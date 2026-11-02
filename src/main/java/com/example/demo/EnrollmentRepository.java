package com.example.demo;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.LocalDateTime;
import java.time.Instant;
import java.util.Optional;

public interface EnrollmentRepository extends JpaRepository<Enrollment, Long> {

	boolean existsByEmailIgnoreCase(String email);

	Optional<Enrollment> findByRegistrationNumber(String registrationNumber);

	@Query("""
			select e from Enrollment e
			where (:term is null
				or lower(e.firstName) like lower(concat('%', :term, '%'))
				or lower(e.lastName) like lower(concat('%', :term, '%'))
				or lower(e.email) like lower(concat('%', :term, '%'))
				or lower(e.registrationNumber) like lower(concat('%', :term, '%')))
			and (:course is null or e.course = :course)
			and (:emailStatus is null or e.emailStatus = :emailStatus)
			and (:from is null or e.createdAt >= :from)
			and (:until is null or e.createdAt < :until)
			""")
	Page<Enrollment> search(
			@Param("term") String term,
			@Param("course") String course,
			@Param("emailStatus") String emailStatus,
			@Param("from") LocalDateTime from,
			@Param("until") LocalDateTime until,
			Pageable pageable);

	long countByCreatedAtAfter(Instant instant);

	long countByEmailStatus(String emailStatus);
}
