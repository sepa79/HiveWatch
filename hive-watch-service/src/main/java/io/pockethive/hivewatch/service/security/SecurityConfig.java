package io.pockethive.hivewatch.service.security;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpMethod;
import org.springframework.security.config.Customizer;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.AnonymousAuthenticationFilter;
import org.springframework.security.web.authentication.HttpStatusEntryPoint;
import org.springframework.security.oauth2.server.resource.web.authentication.BearerTokenAuthenticationFilter;

import static org.springframework.http.HttpStatus.UNAUTHORIZED;

@Configuration
public class SecurityConfig {
    private final HiveWatchAuthProperties authProperties;
    private final DevHeaderAuthenticationFilter devHeaderAuthenticationFilter;
    private final JwtDatabaseUserFilter jwtDatabaseUserFilter;

    public SecurityConfig(
            HiveWatchAuthProperties authProperties,
            DevHeaderAuthenticationFilter devHeaderAuthenticationFilter,
            JwtDatabaseUserFilter jwtDatabaseUserFilter
    ) {
        this.authProperties = authProperties;
        this.devHeaderAuthenticationFilter = devHeaderAuthenticationFilter;
        this.jwtDatabaseUserFilter = jwtDatabaseUserFilter;
    }

    @Bean
    public SecurityFilterChain securityFilterChain(HttpSecurity http) throws Exception {
        http.csrf(csrf -> csrf.disable());
        http.sessionManagement(sm -> sm.sessionCreationPolicy(SessionCreationPolicy.STATELESS));

        http.exceptionHandling(eh -> eh.authenticationEntryPoint(new HttpStatusEntryPoint(UNAUTHORIZED)));

        http.authorizeHttpRequests(auth -> auth
                .requestMatchers(HttpMethod.GET, "/", "/index.html", "/assets/**", "/favicon.ico", "/logo.svg", "/error").permitAll()
                .requestMatchers(HttpMethod.GET, "/actuator/health", "/actuator/info").permitAll()
                .requestMatchers("/api/v1/admin/**").hasRole("ADMIN")
                .requestMatchers("/api/v1/**").authenticated()
                .anyRequest().permitAll()
        );

        if (authProperties.mode() == AuthMode.DEV_HEADER) {
            http.addFilterBefore(devHeaderAuthenticationFilter, AnonymousAuthenticationFilter.class);
        } else if (authProperties.mode() == AuthMode.OIDC_JWT) {
            http.oauth2ResourceServer(oauth -> oauth.jwt(Customizer.withDefaults()));
            http.addFilterAfter(jwtDatabaseUserFilter, BearerTokenAuthenticationFilter.class);
        } else {
            throw new IllegalStateException("Unsupported auth mode: " + authProperties.mode());
        }

        return http.build();
    }
}
