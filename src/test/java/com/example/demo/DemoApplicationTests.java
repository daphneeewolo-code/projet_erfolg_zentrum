package com.example.demo;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest(properties = {
		"app.admin.password=TestAdminPassword123!",
		"app.enrollment.capacity=2",
		"spring.datasource.url=jdbc:h2:mem:erfolg-test;DB_CLOSE_DELAY=-1"
})
@AutoConfigureMockMvc
class DemoApplicationTests {

	@Autowired
	private MockMvc mockMvc;

	@Autowired
	private EnrollmentService enrollmentService;

	@Autowired
	private EnrollmentRepository enrollmentRepository;

	@Autowired
	private EnrollmentPdfService pdfService;

	@BeforeEach
	void cleanDatabase() {
		enrollmentRepository.deleteAll();
	}

	@Test
	void contextLoads() {
	}

	@Test
	void registrationCreatesUniqueNumberAndPdf() {
		assertEquals(java.util.List.of("Cours allemand"), enrollmentService.courses());
		Enrollment enrollment = enrollmentService.register(request("student@example.com"));

		assertTrue(enrollment.getRegistrationNumber().matches("EZ-\\d{4}-[A-F0-9]{8}"));
		assertEquals("Campus de Ange Raphael", enrollment.getCentre());
		assertTrue(new String(pdfService.generate(enrollment), 0, 4).equals("%PDF"));
		assertTrue(enrollmentRepository.existsByEmailIgnoreCase("STUDENT@example.com"));
	}

	@Test
	void publicConfigurationContainsRequestedCoursesLevelsAndCentres() throws Exception {
		mockMvc.perform(get("/api/config"))
				.andExpect(status().isOk())
				.andExpect(jsonPath("$.courses").isArray())
				.andExpect(jsonPath("$.courses.length()").value(1))
				.andExpect(jsonPath("$.courses[0]").value("Cours allemand"))
				.andExpect(jsonPath("$.levels[0]").value("A1"))
				.andExpect(jsonPath("$.levels[1]").value("A2"))
				.andExpect(jsonPath("$.levels[2]").value("B1"))
				.andExpect(jsonPath("$.levels[3]").value("B2"))
				.andExpect(jsonPath("$.levels[4]").value("C1"))
				.andExpect(jsonPath("$.centres[0]").value("Campus de Ange Raphael"))
				.andExpect(jsonPath("$.centres[1]").value("Campus de Bessengue"))
				.andExpect(jsonPath("$.centres[2]").value("autres"))
				.andExpect(jsonPath("$.capacityPerGroup").value(2));
	}

	@Test
	void registrationRejectsLevelsAndCentresOutsideConfiguredOptions() {
		assertThrows(IllegalArgumentException.class,
				() -> enrollmentService.register(request("student@example.com", "Débutant", "Douala")));
		assertThrows(IllegalArgumentException.class,
				() -> enrollmentService.register(request("student@example.com", "A1", "Douala")));
		assertThrows(IllegalArgumentException.class,
				() -> enrollmentService.register(request(
						"student@example.com", "Autre formation", "A1", "Campus de Ange Raphael")));
	}

	@Test
	void duplicateEmailIsRejectedWithoutSavingAnotherEnrollment() {
		enrollmentService.register(request("student@example.com"));

		assertThrows(DuplicateEnrollmentException.class,
				() -> enrollmentService.register(request(" STUDENT@example.com ")));
		assertTrue(enrollmentRepository.count() == 1);
	}

	@Test
	void registrationsBeyondGroupCapacityAreAddedToWaitlist() {
		Enrollment first = enrollmentService.register(request("first@example.com"));
		Enrollment second = enrollmentService.register(request("second@example.com"));
		Enrollment waiting = enrollmentService.register(request("waiting@example.com"));
		Enrollment otherCentre = enrollmentService.register(
				request("other-centre@example.com", "A1", "Campus de Bessengue"));

		assertEquals(EnrollmentStatus.CONFIRMED, first.getEnrollmentStatus());
		assertEquals(EnrollmentStatus.CONFIRMED, second.getEnrollmentStatus());
		assertEquals(EnrollmentStatus.WAITLISTED, waiting.getEnrollmentStatus());
		assertEquals(EnrollmentStatus.CONFIRMED, otherCentre.getEnrollmentStatus());
		assertEquals(2, enrollmentRepository.countByCourseAndLevelAndCentreAndEnrollmentStatus(
				"Cours allemand", "A1", "Campus de Ange Raphael", EnrollmentStatus.CONFIRMED));
	}

	@Test
	@WithMockUser(roles = "ADMIN")
	void waitlistedRegistrationIsReturnedAndFilterableInAdminApi() throws Exception {
		enrollmentService.register(request("seat-one@example.com"));
		enrollmentService.register(request("seat-two@example.com"));

		mockMvc.perform(post("/api/enrollments")
					.with(csrf())
					.contentType(MediaType.APPLICATION_JSON)
					.content("""
							{"firstName":"Amina","lastName":"Njoya","email":"waiting@example.com",
							"phone":"+237 699 000 113","course":"Cours allemand","level":"A1",
							"centre":"Campus de Ange Raphael"}
							"""))
				.andExpect(status().isCreated())
				.andExpect(jsonPath("$.enrollmentStatus").value("WAITLISTED"));

		mockMvc.perform(get("/api/admin/enrollments").param("enrollmentStatus", "WAITLISTED"))
				.andExpect(status().isOk())
				.andExpect(jsonPath("$.totalElements").value(1))
				.andExpect(jsonPath("$.content[0].enrollmentStatus").value("WAITLISTED"));
	}

	@Test
	void adminApiRequiresAuthentication() throws Exception {
		mockMvc.perform(get("/api/admin/dashboard"))
				.andExpect(status().isForbidden());
	}

	@Test
	@WithMockUser(roles = "ADMIN")
	void adminApiIsAvailableToAdministrators() throws Exception {
		mockMvc.perform(get("/api/admin/dashboard"))
				.andExpect(status().isOk());
	}

	@Test
	void unknownRoutesAreDeniedByDefault() throws Exception {
		mockMvc.perform(get("/api/not-configured"))
				.andExpect(status().isForbidden());
	}

	@Test
	void enrollmentCreationRequiresCsrfToken() throws Exception {
		mockMvc.perform(post("/api/enrollments")
						.contentType(MediaType.APPLICATION_JSON)
						.content("{}"))
				.andExpect(status().isForbidden());
	}

	@Test
	void invalidAdminCredentialsReturnUnauthorized() throws Exception {
		mockMvc.perform(post("/api/admin/login")
						.with(csrf())
						.contentType(MediaType.APPLICATION_JSON)
						.content("""
								{"username":"invalid-user","password":"invalid-password"}
								"""))
				.andExpect(status().isUnauthorized());
	}

	private EnrollmentRequest request(String email) {
		return request(email, "A1", "Campus de Ange Raphael");
	}

	private EnrollmentRequest request(String email, String level, String centre) {
		return request(email, "Cours allemand", level, centre);
	}

	private EnrollmentRequest request(String email, String course, String level, String centre) {
		return new EnrollmentRequest(
				"Amina", "Njoya", email, "+237 699 000 111",
				course, level, centre, null, "Douala");
	}
}
