package br.com.fiap.nexusverde.auth;

import static java.nio.charset.StandardCharsets.UTF_8;

import java.security.SecureRandom;
import java.util.List;
import javax.crypto.SecretKey;
import javax.crypto.spec.SecretKeySpec;

import br.com.fiap.nexusverde.web.ApiError;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.nimbusds.jose.jwk.source.ImmutableSecret;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.annotation.Order;
import org.springframework.core.env.Environment;
import org.springframework.core.env.Profiles;
import org.springframework.http.HttpMethod;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.oauth2.jose.jws.MacAlgorithm;
import org.springframework.security.oauth2.jwt.*;
import org.springframework.security.oauth2.server.resource.authentication.JwtAuthenticationConverter;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.AuthenticationEntryPoint;

@Configuration
@EnableWebSecurity
class SecurityConfig {
    @Bean
    PasswordEncoder passwordEncoder() {
        return new BCryptPasswordEncoder();
    }

    @Bean
    SecretKey jwtKey(@Value("${nexus.jwt-secret}") String secret, Environment ambiente) {
        byte[] bytes = secret.getBytes(UTF_8);
        if (bytes.length == 0 && !ambiente.acceptsProfiles(Profiles.of("prod"))) {
            bytes = new byte[32];
            new SecureRandom().nextBytes(bytes);
        }
        if (bytes.length < 32) {
            throw new IllegalStateException("JWT_SECRET deve ter pelo menos 32 bytes.");
        }
        return new SecretKeySpec(bytes, "HmacSHA256");
    }

    @Bean
    JwtEncoder jwtEncoder(SecretKey key) {
        return new NimbusJwtEncoder(new ImmutableSecret<>(key));
    }

    @Bean
    JwtDecoder jwtDecoder(SecretKey key) {
        NimbusJwtDecoder decoder = NimbusJwtDecoder.withSecretKey(key).macAlgorithm(MacAlgorithm.HS256).build();
        decoder.setJwtValidator(JwtValidators.createDefaultWithIssuer(AuthService.EMISSOR));
        return decoder;
    }

    @Bean
    @Order(1)
    SecurityFilterChain api(HttpSecurity http, ObjectMapper json) throws Exception {
        JwtAuthenticationConverter converter = new JwtAuthenticationConverter();
        converter.setJwtGrantedAuthoritiesConverter(jwt ->
                List.of(new SimpleGrantedAuthority("ROLE_" + jwt.getClaimAsString("papel"))));
        AuthenticationEntryPoint sessaoInvalida = (request, response, error) -> {
            response.setHeader("WWW-Authenticate", "Bearer");
            escreverErro(json, response, 401, "Sessão inválida ou expirada.");
        };

        return http.securityMatcher("/api/**")
                .csrf(csrf -> csrf.disable())
                .sessionManagement(session -> session.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
                .authorizeHttpRequests(auth -> auth
                        .requestMatchers(HttpMethod.POST, "/api/auth/login", "/api/auth/register").permitAll()
                        .requestMatchers(HttpMethod.PATCH, "/api/solicitacoes/*/status").hasRole("EQUIPE")
                        .requestMatchers(HttpMethod.POST, "/api/solicitacoes").hasRole("CIDADAO")
                        .requestMatchers(HttpMethod.PUT, "/api/solicitacoes/*").hasRole("CIDADAO")
                        .requestMatchers(HttpMethod.DELETE, "/api/solicitacoes/*").hasRole("CIDADAO")
                        .requestMatchers(HttpMethod.GET, "/api/pontos-agua", "/api/pontos-agua/*",
                                "/api/regioes").hasAnyRole("CIDADAO", "EQUIPE")
                        .requestMatchers("/api/pontos-agua", "/api/pontos-agua/**",
                                "/api/regioes", "/api/regioes/**").hasRole("EQUIPE")
                        .anyRequest().hasAnyRole("CIDADAO", "EQUIPE"))
                .exceptionHandling(errors -> errors
                        .authenticationEntryPoint(sessaoInvalida)
                        .accessDeniedHandler((request, response, error) ->
                                escreverErro(json, response, 403, "Você não tem permissão para esta operação.")))
                .oauth2ResourceServer(oauth -> oauth.authenticationEntryPoint(sessaoInvalida)
                        .jwt(jwt -> jwt.jwtAuthenticationConverter(converter)))
                .build();
    }

    @Bean
    @Order(2)
    SecurityFilterChain painel(HttpSecurity http) throws Exception {
        return http.authorizeHttpRequests(auth -> auth
                        .requestMatchers("/painel/entrar", "/swagger-ui/**", "/swagger-ui.html",
                                "/v3/api-docs/**", "/actuator/health", "/assets/**", "/error").permitAll()
                        .requestMatchers("/painel", "/painel/**").hasRole("EQUIPE")
                        .anyRequest().denyAll())
                .formLogin(form -> form
                        .loginPage("/painel/entrar").loginProcessingUrl("/painel/entrar")
                        .failureUrl("/painel/entrar?erro")
                        .defaultSuccessUrl("/painel", true))
                .logout(logout -> logout.logoutUrl("/painel/sair").logoutSuccessUrl("/painel/entrar"))
                .build();
    }

    private static void escreverErro(ObjectMapper json, HttpServletResponse response,
                                     int status, String mensagem) throws java.io.IOException {
        response.setStatus(status);
        response.setContentType("application/json");
        json.writeValue(response.getOutputStream(), new ApiError(mensagem));
    }
}
