package com.example.demo;

import org.springframework.context.ApplicationEventPublisher;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.time.ZoneId;
import java.util.List;
import java.util.Locale;
import java.util.UUID;

@Service
public class EnrollmentService {

	private static final List<String> COURSES = List.of("Cours allemand");
	private static final List<String> LEVELS = List.of("A1", "A2", "B1", "B2", "C1");
	private static final List<String> CENTRES = List.of(
			"Campus de Ange Raphael", "Campus de Bessengue", "autres");
	private final int capacityPerGroup;
	private final LocalDate enrollmentOpeningDate;
	private final int openingMarginDays;

	private final EnrollmentRepository repository;
	private final EnrollmentPdfService pdfService;
	private final ApplicationEventPublisher events;
	private final JdbcTemplate jdbcTemplate;

	public EnrollmentService(
			EnrollmentRepository repository,
			EnrollmentPdfService pdfService,
			ApplicationEventPublisher events,
			JdbcTemplate jdbcTemplate,
			@Value("${app.enrollment.capacity:20}") int capacityPerGroup,
			@Value("${app.enrollment.opens-on:}") String enrollmentOpeningDateRaw,
			@Value("${app.enrollment.opening-margin-days:25}") int openingMarginDays) {
		this.repository = repository;
		this.pdfService = pdfService;
		this.events = events;
		this.jdbcTemplate = jdbcTemplate;
		this.capacityPerGroup = capacityPerGroup;
		this.enrollmentOpeningDate = blankToLocalDate(enrollmentOpeningDateRaw);
		this.openingMarginDays = Math.max(0, openingMarginDays);
	}

	@Transactional
	public Enrollment register(EnrollmentRequest request) {
		if (!isEnrollmentOpen()) {
			throw new IllegalStateException("Les inscriptions ne sont pas encore ouvertes.");
		}
		jdbcTemplate.queryForObject(
				"SELECT lock_id FROM enrollment_capacity_lock WHERE lock_id = 1 FOR UPDATE", Integer.class);
		String email = request.email().trim().toLowerCase(Locale.ROOT);
		if (repository.existsByEmailIgnoreCase(email)) {
			throw new DuplicateEnrollmentException("Une inscription existe déjà avec cette adresse e-mail.");
		}
		if (!COURSES.contains(request.course())) {
			throw new IllegalArgumentException("La formation sélectionnée n’est pas disponible.");
		}
		if (!LEVELS.contains(request.level())) {
			throw new IllegalArgumentException("Le niveau sélectionné n’est pas disponible.");
		}
		if (!CENTRES.contains(request.centre())) {
			throw new IllegalArgumentException("Le centre sélectionné n’est pas disponible.");
		}
		if (request.birthDate() != null && request.birthDate().isAfter(LocalDate.now())) {
			throw new IllegalArgumentException("La date de naissance ne peut pas être dans le futur.");
		}

		String registrationNumber = "EZ-" + LocalDate.now().getYear() + "-"
				+ UUID.randomUUID().toString().substring(0, 8).toUpperCase(Locale.ROOT);
		EnrollmentStatus enrollmentStatus = repository.countByCourseAndLevelAndCentreAndEnrollmentStatus(
				request.course(), request.level(), request.centre(), EnrollmentStatus.CONFIRMED) >= capacityPerGroup
						? EnrollmentStatus.WAITLISTED
						: EnrollmentStatus.CONFIRMED;
		Enrollment enrollment = repository.saveAndFlush(new Enrollment(
				registrationNumber,
				request.firstName().trim(),
				request.lastName().trim(),
				email,
				request.phone().trim(),
				request.course(),
				request.level(),
				request.centre(),
				request.birthDate(),
				request.address() == null || request.address().isBlank() ? null : request.address().trim(),
				enrollmentStatus));
		events.publishEvent(new EnrollmentRegistered(enrollment.getId()));
		return enrollment;
	}

	@Transactional(readOnly = true)
	public Page<Enrollment> search(String term, String course, EnrollmentStatus enrollmentStatus, String emailStatus,
			LocalDate from, LocalDate until, Pageable pageable) {
		return repository.search(
				blankToNull(term),
				blankToNull(course),
				enrollmentStatus,
				blankToNull(emailStatus),
				from == null ? null : from.atStartOfDay(),
				until == null ? null : until.plusDays(1).atStartOfDay(),
				pageable);
	}

	@Transactional(readOnly = true)
	public Enrollment get(long id) {
		return repository.findById(id)
				.orElseThrow(() -> new EnrollmentNotFoundException("Inscription introuvable."));
	}

	@Transactional
	public void queueEmail(long id) {
		Enrollment enrollment = get(id);
		enrollment.setEmailStatus("QUEUED", null);
		events.publishEvent(new EnrollmentRegistered(id));
	}

	@Transactional
	public void setEmailStatus(long id, String status, String error) {
		Enrollment enrollment = get(id);
		enrollment.setEmailStatus(status, error);
	}

	@Transactional(readOnly = true)
	public byte[] generatePdf(long id) {
		return pdfService.generate(get(id));
	}

	@Transactional(readOnly = true)
	public List<String> courses() {
		return COURSES;
	}

	@Transactional(readOnly = true)
	public List<String> levels() {
		return LEVELS;
	}

	@Transactional(readOnly = true)
	public List<String> centres() {
		return CENTRES;
	}

	public int capacityPerGroup() {
		return capacityPerGroup;
	}

	public boolean isEnrollmentOpen() {
		return enrollmentOpeningDate == null || !LocalDate.now().isBefore(enrollmentOpeningDate.minusDays(openingMarginDays));
	}

	public LocalDate enrollmentOpeningDate() {
		return enrollmentOpeningDate;
	}

	public int openingMarginDays() {
		return openingMarginDays;
	}

	public long countAll() {
		return repository.count();
	}

	public long countThisMonth() {
		return repository.countByCreatedAtAfter(LocalDate.now(ZoneId.of("Africa/Douala"))
				.withDayOfMonth(1).atStartOfDay(ZoneId.of("Africa/Douala")).toInstant());
	}

	public long countEmailFailures() {
		return repository.countByEmailStatus("FAILED");
	}

	public long countCourses() {
		return COURSES.size();
	}

	public byte[] generatePdf(Enrollment enrollment) {
		return pdfService.generate(enrollment);
	}

	private String blankToNull(String value) {
		return value == null || value.isBlank() ? null : value.trim();
	}

	private LocalDate blankToLocalDate(String value) {
		if (value == null || value.isBlank()) {
			return null;
		}
		try {
			return LocalDate.parse(value);
		} catch (RuntimeException exception) {
			throw new IllegalArgumentException("La date d’ouverture des inscriptions est invalide: " + value, exception);
		}
	}
}
