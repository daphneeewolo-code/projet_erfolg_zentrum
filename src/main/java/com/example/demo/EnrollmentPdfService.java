package com.example.demo;

import com.lowagie.text.Document;
import com.lowagie.text.DocumentException;
import com.lowagie.text.Element;
import com.lowagie.text.Font;
import com.lowagie.text.FontFactory;
import com.lowagie.text.PageSize;
import com.lowagie.text.Paragraph;
import com.lowagie.text.Phrase;
import com.lowagie.text.Rectangle;
import com.lowagie.text.pdf.PdfPCell;
import com.lowagie.text.pdf.PdfPTable;
import com.lowagie.text.pdf.PdfWriter;
import org.springframework.stereotype.Service;

import java.io.ByteArrayOutputStream;
import java.text.Normalizer;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;

@Service
public class EnrollmentPdfService {

	private static final DateTimeFormatter DATE_FORMAT =
			DateTimeFormatter.ofPattern("dd MMMM yyyy 'à' HH:mm").withZone(ZoneId.of("Africa/Douala"));

	public byte[] generate(Enrollment enrollment) {
		ByteArrayOutputStream output = new ByteArrayOutputStream();
		Document document = new Document(PageSize.A4, 48, 48, 52, 48);
		try {
			PdfWriter.getInstance(document, output);
			document.open();

			Font brand = FontFactory.getFont(FontFactory.HELVETICA_BOLD, 18);
			Font title = FontFactory.getFont(FontFactory.HELVETICA_BOLD, 23);
			Font label = FontFactory.getFont(FontFactory.HELVETICA, 10);
			Font value = FontFactory.getFont(FontFactory.HELVETICA_BOLD, 11);

			Paragraph header = new Paragraph("ERFOLG ZENTRUM  |  DOUALA", brand);
			header.setAlignment(Element.ALIGN_CENTER);
			document.add(header);

			Paragraph subtitle = new Paragraph(
					enrollment.getEnrollmentStatus() == EnrollmentStatus.WAITLISTED
							? "DEMANDE EN LISTE D'ATTENTE" : "FICHE D'INSCRIPTION", title);
			subtitle.setAlignment(Element.ALIGN_CENTER);
			subtitle.setSpacingBefore(34);
			subtitle.setSpacingAfter(10);
			document.add(subtitle);

			Paragraph number = new Paragraph(enrollment.getRegistrationNumber(), value);
			number.setAlignment(Element.ALIGN_CENTER);
			number.setSpacingAfter(30);
			document.add(number);

			PdfPTable table = new PdfPTable(2);
			table.setWidthPercentage(100);
			table.setWidths(new float[]{1, 2});
			addRow(table, "Prénom", enrollment.getFirstName(), label, value);
			addRow(table, "Nom", enrollment.getLastName(), label, value);
			addRow(table, "Adresse e-mail", enrollment.getEmail(), label, value);
			addRow(table, "Téléphone", enrollment.getPhone(), label, value);
			addRow(table, "Formation", enrollment.getCourse(), label, value);
			addRow(table, "Niveau", enrollment.getLevel(), label, value);
			addRow(table, "Centre", enrollment.getCentre() == null ? "Non renseigné" : enrollment.getCentre(), label, value);
			addRow(table, "Date de naissance", enrollment.getBirthDate() == null ? "Non renseignée" : enrollment.getBirthDate().toString(), label, value);
			addRow(table, "Adresse", enrollment.getAddress() == null || enrollment.getAddress().isBlank() ? "Non renseignée" : enrollment.getAddress(), label, value);
			addRow(table, "Inscription reçue le", DATE_FORMAT.format(enrollment.getCreatedAt()), label, value);
			document.add(table);

			Paragraph note = new Paragraph(
					"Conservez cette fiche. Présentez votre numéro d'inscription lors de vos échanges avec notre équipe.",
					FontFactory.getFont(FontFactory.HELVETICA, 10));
			note.setSpacingBefore(34);
			document.add(note);

			Paragraph footer = new Paragraph("Erfolg Zentrum · Douala, Cameroun",
					FontFactory.getFont(FontFactory.HELVETICA_OBLIQUE, 9));
			footer.setAlignment(Element.ALIGN_CENTER);
			footer.setSpacingBefore(52);
			document.add(footer);
			document.close();
			return output.toByteArray();
		}
		catch (DocumentException exception) {
			throw new IllegalStateException("Impossible de générer la fiche PDF.", exception);
		}
	}

	private void addRow(PdfPTable table, String name, String content, Font label, Font value) {
		PdfPCell labelCell = new PdfPCell(new Phrase(toPdfText(name), label));
		labelCell.setBorder(Rectangle.BOTTOM);
		labelCell.setPadding(12);
		table.addCell(labelCell);
		PdfPCell valueCell = new PdfPCell(new Phrase(toPdfText(content), value));
		valueCell.setBorder(Rectangle.BOTTOM);
		valueCell.setPadding(12);
		table.addCell(valueCell);
	}

	private String toPdfText(String text) {
		return Normalizer.normalize(text, Normalizer.Form.NFD)
				.replaceAll("\\p{M}", "")
				.replaceAll("[^\\x20-\\x7E]", "?");
	}
}
