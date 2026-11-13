package com.example.demo;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Index;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;

import java.time.Instant;
import java.time.LocalDate;

@Entity
@Table(
		name = "enrollments",
		uniqueConstraints = @UniqueConstraint(name = "uk_enrollment_number", columnNames = "registration_number"),
		indexes = {
				@Index(name = "idx_enrollment_email", columnList = "email"),
				@Index(name = "idx_enrollment_created", columnList = "created_at")
		}
)
public class Enrollment {

	@Id
	@GeneratedValue(strategy = GenerationType.IDENTITY)
	private Long id;

	@Column(name = "registration_number", nullable = false, unique = true, length = 32)
	private String registrationNumber;

	@Column(name = "first_name", nullable = false, length = 100)
	private String firstName;

	@Column(name = "last_name", nullable = false, length = 100)
	private String lastName;

	@Column(nullable = false, unique = true, length = 254)
	private String email;

	@Column(nullable = false, length = 30)
	private String phone;

	@Column(nullable = false, length = 120)
	private String course;

	@Column(nullable = false, length = 80)
	private String level;

	@Column(length = 80)
	private String centre;

	private LocalDate birthDate;

	@Column(length = 300)
	private String address;

	@Enumerated(EnumType.STRING)
	@Column(name = "enrollment_status", nullable = false, length = 20)
	private EnrollmentStatus enrollmentStatus = EnrollmentStatus.CONFIRMED;

	@Column(nullable = false, length = 20)
	private String emailStatus = "QUEUED";

	@Column(length = 500)
	private String emailError;

	@Column(nullable = false, updatable = false)
	private Instant createdAt;

	protected Enrollment() {
	}

	public Enrollment(String registrationNumber, String firstName, String lastName, String email,
			String phone, String course, String level, String centre, LocalDate birthDate, String address,
			EnrollmentStatus enrollmentStatus) {
		this.registrationNumber = registrationNumber;
		this.firstName = firstName;
		this.lastName = lastName;
		this.email = email;
		this.phone = phone;
		this.course = course;
		this.level = level;
		this.centre = centre;
		this.birthDate = birthDate;
		this.address = address;
		this.enrollmentStatus = enrollmentStatus;
		this.createdAt = Instant.now();
	}

	public Long getId() {
		return id;
	}

	public String getRegistrationNumber() {
		return registrationNumber;
	}

	public String getFirstName() {
		return firstName;
	}

	public String getLastName() {
		return lastName;
	}

	public String getEmail() {
		return email;
	}

	public String getPhone() {
		return phone;
	}

	public String getCourse() {
		return course;
	}

	public String getLevel() {
		return level;
	}

	public String getCentre() {
		return centre;
	}

	public LocalDate getBirthDate() {
		return birthDate;
	}

	public String getAddress() {
		return address;
	}

	public EnrollmentStatus getEnrollmentStatus() {
		return enrollmentStatus;
	}

	public String getEmailStatus() {
		return emailStatus;
	}

	public String getEmailError() {
		return emailError;
	}

	public Instant getCreatedAt() {
		return createdAt;
	}

	public void setEmailStatus(String emailStatus, String emailError) {
		this.emailStatus = emailStatus;
		this.emailError = emailError;
	}
}
