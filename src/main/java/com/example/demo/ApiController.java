package com.example.demo;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.http.ContentDisposition;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContext;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.web.context.SecurityContextRepository;
import org.springframework.security.web.csrf.CsrfToken;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.nio.charset.StandardCharsets;
import java.time.LocalDate;
import java.util.Base64;
import java.util.LinkedHashMap;
import java.util.Map;

@RestController
@RequestMapping("/api")
public class ApiController {

	private final EnrollmentService enrollmentService;
	private final AuthenticationManager authenticationManager;
	private final SecurityContextRepository securityContextRepository;

	public ApiController(
			EnrollmentService enrollmentService,
			AuthenticationManager authenticationManager,
			SecurityContextRepository securityContextRepository) {
		this.enrollmentService = enrollmentService;
		this.authenticationManager = authenticationManager;
		this.securityContextRepository = securityContextRepository;
	}

	@GetMapping("/csrf")
	public Map<String, String> csrf(CsrfToken csrfToken) {
		return Map.of("token", csrfToken.getToken());
	}

	@GetMapping("/config")
	public Map<String, Object> publicConfiguration() {
		Map<String, Object> config = new LinkedHashMap<>();
		config.put("schoolName", "Erfolg Zentrum");
		config.put("courses", enrollmentService.courses());
		config.put("levels", enrollmentService.levels());
		config.put("centres", enrollmentService.centres());
		config.put("capacityPerGroup", enrollmentService.capacityPerGroup());
		config.put("admissionsOpen", enrollmentService.isEnrollmentOpen());
		config.put("openingDate", enrollmentService.enrollmentOpeningDate());
		config.put("openingMarginDays", enrollmentService.openingMarginDays());
		return config;
	}

	@PostMapping("/enrollments")
	public ResponseEntity<EnrollmentReceipt> register(@Valid @RequestBody EnrollmentRequest request) {
		Enrollment enrollment = enrollmentService.register(request);
		EnrollmentReceipt receipt = new EnrollmentReceipt(
				enrollment.getId(),
				enrollment.getRegistrationNumber(),
				enrollment.getFirstName(),
				enrollment.getCourse(),
				enrollment.getEnrollmentStatus(),
				enrollment.getCreatedAt().toString(),
				Base64.getEncoder().encodeToString(enrollmentService.generatePdf(enrollment)));
		return ResponseEntity.status(HttpStatus.CREATED).body(receipt);
	}

	@PostMapping("/admin/login")
	public Map<String, String> login(
			@Valid @RequestBody LoginRequest request,
			HttpServletRequest httpRequest,
			HttpServletResponse httpResponse) {
		Authentication authentication = authenticationManager.authenticate(
				UsernamePasswordAuthenticationToken.unauthenticated(request.username(), request.password()));
		httpRequest.getSession(true);
		httpRequest.changeSessionId();
		SecurityContext context = SecurityContextHolder.createEmptyContext();
		context.setAuthentication(authentication);
		SecurityContextHolder.setContext(context);
		securityContextRepository.saveContext(context, httpRequest, httpResponse);
		return Map.of("username", authentication.getName());
	}

	@PostMapping("/admin/logout")
	public ResponseEntity<Void> logout(HttpServletRequest request, HttpServletResponse response) {
		SecurityContextHolder.clearContext();
		var session = request.getSession(false);
		if (session != null) {
			session.invalidate();
		}
		response.setStatus(HttpStatus.NO_CONTENT.value());
		return ResponseEntity.noContent().build();
	}

	@GetMapping("/admin/me")
	public Map<String, String> currentAdministrator(Authentication authentication) {
		return Map.of("username", authentication.getName());
	}

	@GetMapping("/admin/dashboard")
	public Dashboard dashboard() {
		return new Dashboard(
				enrollmentService.countAll(),
				enrollmentService.countThisMonth(),
				enrollmentService.countEmailFailures(),
				enrollmentService.countCourses());
	}

	@GetMapping("/admin/enrollments")
	public Page<EnrollmentView> enrollments(
			@RequestParam(required = false) String q,
			@RequestParam(required = false) String course,
			@RequestParam(required = false) EnrollmentStatus enrollmentStatus,
			@RequestParam(required = false) String emailStatus,
			@RequestParam(required = false) LocalDate from,
			@RequestParam(required = false) LocalDate until,
			@RequestParam(defaultValue = "0") int page,
			@RequestParam(defaultValue = "10") int size) {
		Page<Enrollment> result = enrollmentService.search(
				q, course, enrollmentStatus, emailStatus, from, until,
				PageRequest.of(Math.max(0, page), Math.max(1, Math.min(size, 100)),
						Sort.by(Sort.Direction.DESC, "createdAt")));
		return result.map(EnrollmentView::from);
	}

	@GetMapping("/admin/enrollments/{id}")
	public EnrollmentView enrollment(@PathVariable long id) {
		return EnrollmentView.from(enrollmentService.get(id));
	}

	@GetMapping("/admin/enrollments/{id}/pdf")
	public ResponseEntity<byte[]> enrollmentPdf(@PathVariable long id) {
		Enrollment enrollment = enrollmentService.get(id);
		return ResponseEntity.ok()
				.contentType(MediaType.APPLICATION_PDF)
				.header(HttpHeaders.CONTENT_DISPOSITION,
						ContentDisposition.attachment()
								.filename(enrollment.getRegistrationNumber() + ".pdf", StandardCharsets.UTF_8)
								.build().toString())
				.body(enrollmentService.generatePdf(enrollment));
	}

	@PostMapping("/admin/enrollments/{id}/resend")
	public Map<String, String> resendEmail(@PathVariable long id) {
		enrollmentService.queueEmail(id);
		return Map.of("status", "QUEUED", "message", "La fiche a été ajoutée à la file d’envoi.");
	}

	@GetMapping(value = "/admin/export.csv", produces = "text/csv")
	public ResponseEntity<byte[]> exportCsv(
			@RequestParam(required = false) String q,
			@RequestParam(required = false) String course,
			@RequestParam(required = false) EnrollmentStatus enrollmentStatus,
			@RequestParam(required = false) String emailStatus,
			@RequestParam(required = false) LocalDate from,
			@RequestParam(required = false) LocalDate until) {
		StringBuilder csv = new StringBuilder("\uFEFFNuméro;Prénom;Nom;E-mail;Téléphone;Formation;Niveau;Centre;Statut;Date d’inscription;État e-mail\n");
		int page = 0;
		Page<Enrollment> results;
		do {
			results = enrollmentService.search(q, course, enrollmentStatus, emailStatus, from, until,
					PageRequest.of(page++, 500, Sort.by(Sort.Direction.DESC, "createdAt")));
			results.getContent().forEach(enrollment -> csv.append(csvRow(
					enrollment.getRegistrationNumber(),
					enrollment.getFirstName(),
					enrollment.getLastName(),
					enrollment.getEmail(),
					enrollment.getPhone(),
					enrollment.getCourse(),
					enrollment.getLevel(),
					enrollment.getCentre(),
					enrollment.getEnrollmentStatus().name(),
					enrollment.getCreatedAt().toString(),
					enrollment.getEmailStatus())));
		}
		while (results.hasNext());

		return ResponseEntity.ok()
				.contentType(new MediaType("text", "csv", StandardCharsets.UTF_8))
				.header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=\"inscriptions-erfolg.csv\"")
				.body(csv.toString().getBytes(StandardCharsets.UTF_8));
	}

	@ExceptionHandler(MethodArgumentNotValidException.class)
	public ResponseEntity<Map<String, String>> invalidRequest(MethodArgumentNotValidException exception) {
		String message = exception.getBindingResult().getFieldErrors().stream()
				.map(error -> error.getDefaultMessage())
				.findFirst()
				.orElse("Vérifiez les informations saisies.");
		return ResponseEntity.badRequest().body(Map.of("message", message));
	}

	@ExceptionHandler(DuplicateEnrollmentException.class)
	public ResponseEntity<Map<String, String>> duplicateEnrollment(DuplicateEnrollmentException exception) {
		return ResponseEntity.status(HttpStatus.CONFLICT).body(Map.of("message", exception.getMessage()));
	}

	@ExceptionHandler(EnrollmentNotFoundException.class)
	public ResponseEntity<Map<String, String>> notFound(EnrollmentNotFoundException exception) {
		return ResponseEntity.status(HttpStatus.NOT_FOUND).body(Map.of("message", exception.getMessage()));
	}

	@ExceptionHandler(IllegalArgumentException.class)
	public ResponseEntity<Map<String, String>> invalidValue(IllegalArgumentException exception) {
		return ResponseEntity.badRequest().body(Map.of("message", exception.getMessage()));
	}

	@ExceptionHandler(BadCredentialsException.class)
	public ResponseEntity<Map<String, String>> invalidCredentials() {
		return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
				.body(Map.of("message", "Identifiants incorrects."));
	}

	private String csvRow(String... values) {
		return String.join(";", java.util.Arrays.stream(values)
				.map(this::csvValue)
				.toList()) + "\r\n";
	}

	private String csvValue(String value) {
		String safeValue = value != null && value.matches("^\\s*[=+@-].*") ? "'" + value : value;
		if (safeValue == null) {
			safeValue = "";
		}
		return "\"" + safeValue.replace("\"", "\"\"") + "\"";
	}

	public record LoginRequest(@NotBlank String username, @NotBlank String password) {
	}

	public record EnrollmentReceipt(
			Long id,
			String registrationNumber,
			String firstName,
			String course,
			EnrollmentStatus enrollmentStatus,
			String createdAt,
			String pdfBase64) {
	}

	public record Dashboard(long total, long thisMonth, long failedEmails, long courses) {
	}

	public record EnrollmentView(
			Long id,
			String registrationNumber,
			String firstName,
			String lastName,
			String email,
			String phone,
			String course,
			String level,
			String centre,
			EnrollmentStatus enrollmentStatus,
			LocalDate birthDate,
			String address,
			String emailStatus,
			String emailError,
			String createdAt) {

		static EnrollmentView from(Enrollment enrollment) {
			return new EnrollmentView(
					enrollment.getId(),
					enrollment.getRegistrationNumber(),
					enrollment.getFirstName(),
					enrollment.getLastName(),
					enrollment.getEmail(),
					enrollment.getPhone(),
					enrollment.getCourse(),
					enrollment.getLevel(),
					enrollment.getCentre(),
				enrollment.getEnrollmentStatus(),
					enrollment.getBirthDate(),
					enrollment.getAddress(),
					enrollment.getEmailStatus(),
					enrollment.getEmailError(),
					enrollment.getCreatedAt().toString());
		}
	}
}
