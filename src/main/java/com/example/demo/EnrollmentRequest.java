package com.example.demo;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Past;
import jakarta.validation.constraints.Size;

import java.time.LocalDate;

public record EnrollmentRequest(
		@NotBlank(message = "Le prénom est obligatoire.")
		@Size(max = 100, message = "Le prénom ne peut pas dépasser 100 caractères.")
		String firstName,

		@NotBlank(message = "Le nom est obligatoire.")
		@Size(max = 100, message = "Le nom ne peut pas dépasser 100 caractères.")
		String lastName,

		@NotBlank(message = "L’adresse e-mail est obligatoire.")
		@Email(message = "Saisissez une adresse e-mail valide.")
		@Size(max = 254)
		String email,

		@NotBlank(message = "Le téléphone est obligatoire.")
		@Pattern(regexp = "^\\+?[0-9][0-9\\s().-]{6,19}$", message = "Saisissez un numéro de téléphone valide.")
		String phone,

		@NotBlank(message = "La filière est obligatoire.")
		@Size(max = 120)
		String course,

		@NotBlank(message = "Le niveau est obligatoire.")
		@Size(max = 80)
		String level,

		@NotBlank(message = "Le centre est obligatoire.")
		@Size(max = 80)
		String centre,

		@Past(message = "La date de naissance doit être dans le passé.")
		LocalDate birthDate,

		@Size(max = 300, message = "L’adresse ne peut pas dépasser 300 caractères.")
		String address
) {
}
