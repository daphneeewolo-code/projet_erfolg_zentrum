package com.example.demo;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

@Component
public class AdministratorBootstrap implements ApplicationRunner {

	private final RoleRepository roleRepository;
	private final UserAccountRepository userAccountRepository;
	private final PasswordEncoder passwordEncoder;
	private final String username;
	private final String password;

	public AdministratorBootstrap(
			RoleRepository roleRepository,
			UserAccountRepository userAccountRepository,
			PasswordEncoder passwordEncoder,
			@Value("${app.admin.username}") String username,
			@Value("${app.admin.password}") String password) {
		this.roleRepository = roleRepository;
		this.userAccountRepository = userAccountRepository;
		this.passwordEncoder = passwordEncoder;
		this.username = username;
		this.password = password;
	}

	@Override
	@Transactional
	public void run(ApplicationArguments arguments) {
		if (password.isBlank()) {
			throw new IllegalStateException("Configurez ADMIN_PASSWORD avant de démarrer l’application.");
		}

		String normalizedUsername = username.trim();
		if (normalizedUsername.isEmpty()) {
			throw new IllegalStateException("ADMIN_USERNAME ne peut pas être vide.");
		}

		Role administratorRole = roleRepository.findByName(RoleName.ADMIN)
				.orElseThrow(() -> new IllegalStateException("Le rôle Administrateur est absent du schéma."));
		UserAccount account = userAccountRepository.findByUsernameIgnoreCase(normalizedUsername)
				.orElseGet(() -> new UserAccount(normalizedUsername, passwordEncoder.encode(password)));

		if (!account.hasRole(RoleName.ADMIN)) {
			account.addRole(administratorRole);
			userAccountRepository.save(account);
		}
	}
}
