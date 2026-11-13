package com.example.demo;

import jakarta.mail.MessagingException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.mail.MailException;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.mail.javamail.MimeMessageHelper;
import org.springframework.stereotype.Service;

@Service
public class EnrollmentNotificationService {

	private static final Logger logger = LoggerFactory.getLogger(EnrollmentNotificationService.class);

	private final EnrollmentService enrollmentService;
	private final EnrollmentPdfService pdfService;
	private final ObjectProvider<JavaMailSender> mailSenders;
	private final String mailHost;
	private final String fromAddress;

	public EnrollmentNotificationService(
			EnrollmentService enrollmentService,
			EnrollmentPdfService pdfService,
			ObjectProvider<JavaMailSender> mailSenders,
			@Value("${spring.mail.host:}") String mailHost,
			@Value("${app.mail.from}") String fromAddress) {
		this.enrollmentService = enrollmentService;
		this.pdfService = pdfService;
		this.mailSenders = mailSenders;
		this.mailHost = mailHost;
		this.fromAddress = fromAddress;
	}

	public void send(long enrollmentId) {
		Enrollment enrollment = enrollmentService.get(enrollmentId);
		if (mailHost.isBlank() || mailSenders.getIfAvailable() == null) {
			String reason = "Le serveur SMTP n’est pas configuré.";
			enrollmentService.setEmailStatus(enrollmentId, "FAILED", reason);
			logger.error("Email delivery is not configured for enrollment {}", enrollmentId);
			return;
		}

		try {
			JavaMailSender sender = mailSenders.getObject();
			var message = sender.createMimeMessage();
			MimeMessageHelper helper = new MimeMessageHelper(message, true, "UTF-8");
			helper.setFrom(fromAddress);
			helper.setTo(enrollment.getEmail());
			boolean waitlisted = enrollment.getEnrollmentStatus() == EnrollmentStatus.WAITLISTED;
			helper.setSubject((waitlisted ? "Votre demande est en liste d’attente — " : "Votre inscription à Erfolg Zentrum — ")
					+ enrollment.getRegistrationNumber());
			helper.setText("Bonjour " + enrollment.getFirstName() + ",\n\n"
					+ (waitlisted
							? "Votre demande en " + enrollment.getCourse() + " a été ajoutée à la liste d’attente du groupe sélectionné. "
									+ "L’équipe vous contactera si une place se libère. "
							: "Nous avons bien reçu votre inscription en " + enrollment.getCourse() + ". ")
					+ "Vous trouverez votre fiche en pièce jointe.\n\n"
					+ "Votre numéro d’inscription : " + enrollment.getRegistrationNumber()
					+ "\n\nL’équipe Erfolg Zentrum — Douala", false);
			helper.addAttachment(enrollment.getRegistrationNumber() + ".pdf",
					() -> new java.io.ByteArrayInputStream(pdfService.generate(enrollment)));
			sender.send(message);
			enrollmentService.setEmailStatus(enrollmentId, "SENT", null);
		}
		catch (MailException | MessagingException exception) {
			String reason = exception.getMessage() == null ? "Échec de l’envoi e-mail." : exception.getMessage();
			enrollmentService.setEmailStatus(enrollmentId, "FAILED", reason.substring(0, Math.min(500, reason.length())));
			logger.error("Email delivery failed for enrollment {}", enrollmentId, exception);
		}
	}
}
