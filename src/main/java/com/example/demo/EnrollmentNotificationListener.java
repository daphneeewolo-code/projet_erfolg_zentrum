package com.example.demo;

import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Component;
import org.springframework.transaction.event.TransactionPhase;
import org.springframework.transaction.event.TransactionalEventListener;

@Component
public class EnrollmentNotificationListener {

	private final EnrollmentNotificationService notificationService;

	public EnrollmentNotificationListener(EnrollmentNotificationService notificationService) {
		this.notificationService = notificationService;
	}

	@Async
	@TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
	public void onEnrollmentRegistered(EnrollmentRegistered event) {
		notificationService.send(event.enrollmentId());
	}
}
