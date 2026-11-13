package com.example.demo;

import java.util.List;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpMethod;
import org.springframework.security.config.Customizer;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.config.annotation.authentication.configuration.AuthenticationConfiguration;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configurers.AbstractHttpConfigurer;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.core.userdetails.User;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.crypto.factory.PasswordEncoderFactories;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.provisioning.InMemoryUserDetailsManager;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.context.HttpSessionSecurityContextRepository;
import org.springframework.security.web.context.SecurityContextRepository;
import org.springframework.security.web.csrf.CookieCsrfTokenRepository;
import org.springframework.security.web.csrf.CsrfTokenRequestAttributeHandler;
import org.springframework.web.cors.CorsConfiguration;
import org.springframework.web.cors.CorsConfigurationSource;
import org.springframework.web.cors.UrlBasedCorsConfigurationSource;

@Configuration
public class SecurityConfiguration {

	@Bean
	SecurityFilterChain securityFilterChain(
			HttpSecurity http,
			@Value("${app.security.csrf-cookie.same-site:lax}") String csrfCookieSameSite,
			@Value("${app.security.csrf-cookie.secure:false}") boolean csrfCookieSecure) throws Exception {
		CookieCsrfTokenRepository csrfTokenRepository = CookieCsrfTokenRepository.withHttpOnlyFalse();
		csrfTokenRepository.setCookieCustomizer(cookie -> cookie
				.sameSite(csrfCookieSameSite)
				.secure(csrfCookieSecure));
		http
				.cors(Customizer.withDefaults())
				.csrf(csrf -> csrf
						.csrfTokenRepository(csrfTokenRepository)
						.csrfTokenRequestHandler(new CsrfTokenRequestAttributeHandler()))
				.authorizeHttpRequests(authorize -> authorize
						.requestMatchers("/", "/index.html", "/admin.html", "/assets/**")
						.permitAll()
						.requestMatchers(HttpMethod.GET, "/api/csrf", "/api/config")
						.permitAll()
						.requestMatchers(HttpMethod.POST, "/api/enrollments", "/api/admin/login")
						.permitAll()
						.requestMatchers("/api/admin/**").hasRole("ADMIN")
						.anyRequest().denyAll())
				.formLogin(AbstractHttpConfigurer::disable)
				.httpBasic(AbstractHttpConfigurer::disable)
				.logout(AbstractHttpConfigurer::disable)
				.sessionManagement(session -> session.sessionCreationPolicy(SessionCreationPolicy.IF_REQUIRED));
		return http.build();
	}

	@Bean
	CorsConfigurationSource corsConfigurationSource(
			@Value("${app.cors.allowed-origin:}") String allowedOrigin) {
		CorsConfiguration configuration = new CorsConfiguration();
		if (!allowedOrigin.isBlank()) {
			configuration.setAllowedOrigins(List.of(allowedOrigin));
			configuration.setAllowedMethods(List.of("GET", "POST", "OPTIONS"));
			configuration.setAllowedHeaders(List.of("Content-Type", "X-XSRF-TOKEN"));
			configuration.setAllowCredentials(true);
		}
		UrlBasedCorsConfigurationSource source = new UrlBasedCorsConfigurationSource();
		source.registerCorsConfiguration("/api/**", configuration);
		return source;
	}

	@Bean
	UserDetailsService userDetailsService(
			PasswordEncoder passwordEncoder,
			@Value("${app.admin.username}") String username,
			@Value("${app.admin.password}") String password) {
		if (password.isBlank()) {
			throw new IllegalStateException("Configurez ADMIN_PASSWORD avant de démarrer l’application.");
		}
		return new InMemoryUserDetailsManager(User.withUsername(username)
				.password(passwordEncoder.encode(password))
				.roles("ADMIN")
				.build());
	}

	@Bean
	PasswordEncoder passwordEncoder() {
		return PasswordEncoderFactories.createDelegatingPasswordEncoder();
	}

	@Bean
	AuthenticationManager authenticationManager(AuthenticationConfiguration configuration) throws Exception {
		return configuration.getAuthenticationManager();
	}

	@Bean
	SecurityContextRepository securityContextRepository() {
		return new HttpSessionSecurityContextRepository();
	}
}
