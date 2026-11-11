package io.github.lucasolimpio.conformia.shared.security;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.annotation.Order;
import org.springframework.http.HttpStatus;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.CredentialsExpiredException;
import org.springframework.security.authentication.ProviderManager;
import org.springframework.security.authentication.dao.DaoAuthenticationProvider;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.access.intercept.AuthorizationFilter;
import org.springframework.security.web.authentication.logout.HttpStatusReturningLogoutSuccessHandler;

/**
 * Duas cadeias de segurança, a mesma sessão:
 * 1. /api/** (Flutter) — respostas em JSON, login por POST /api/auth/login.
 * 2. Todo o resto (navegador) — telas Thymeleaf, login por formulário.
 */
@Configuration
@EnableWebSecurity
public class SecurityConfig {

    @Bean
    @Order(1)
    public SecurityFilterChain api(HttpSecurity http) throws Exception {
        http
                .securityMatcher("/api/**")
                .authorizeHttpRequests(auth -> auth
                        .requestMatchers("/api/auth/login").permitAll()
                        .requestMatchers("/api/usuarios/**").hasRole("GERENTE")
                        .anyRequest().authenticated())
                // CSRF desligado só na API: o ataque depende do navegador enviar o cookie
                // de outro site. Aqui o cookie é SameSite=Lax (não vai em POST de outro site),
                // os endpoints recebem JSON e não há CORS liberado.
                .csrf(csrf -> csrf.disable())
                .exceptionHandling(ex -> ex
                        .authenticationEntryPoint((request, response, e) -> RespostaJson.escrever(response,
                                401, "NAO_AUTENTICADO", "Faça login para continuar."))
                        .accessDeniedHandler((request, response, e) -> RespostaJson.escrever(response,
                                403, "ACESSO_NEGADO", "Seu perfil não permite esta ação.")))
                .logout(logout -> logout
                        .logoutUrl("/api/auth/logout")
                        .logoutSuccessHandler(new HttpStatusReturningLogoutSuccessHandler(HttpStatus.NO_CONTENT)))
                .addFilterAfter(new TrocaSenhaObrigatoriaFilter(), AuthorizationFilter.class);
        return http.build();
    }

    @Bean
    @Order(2)
    public SecurityFilterChain web(HttpSecurity http, LoginSucessoHandler loginSucesso) throws Exception {
        http
                .authorizeHttpRequests(auth -> auth
                        .requestMatchers("/login", "/css/**", "/js/**", "/error").permitAll()
                        .requestMatchers("/usuarios/**").hasRole("GERENTE")
                        .anyRequest().authenticated())
                .formLogin(form -> form
                        .loginPage("/login")
                        .usernameParameter("email")
                        .passwordParameter("senha")
                        .successHandler(loginSucesso)
                        // Erro genérico para não revelar se o e-mail existe ou está inativo.
                        // Senha temporária vencida só é avisada depois que a senha conferiu.
                        .failureHandler((request, response, erro) -> response.sendRedirect(
                                request.getContextPath() + (erro instanceof CredentialsExpiredException
                                        ? "/login?expirada" : "/login?erro")))
                        .permitAll())
                .logout(logout -> logout
                        .logoutSuccessUrl("/login?saiu")
                        .permitAll())
                .addFilterAfter(new TrocaSenhaObrigatoriaFilter(), AuthorizationFilter.class);
        return http.build();
    }

    /**
     * Quem confere e-mail + senha. O formulário web usa por baixo dos panos;
     * o AuthApiController chama direto no login da API.
     */
    @Bean
    public AuthenticationManager authenticationManager(UserDetailsService userDetailsService,
                                                       PasswordEncoder passwordEncoder) {
        DaoAuthenticationProvider provider = new DaoAuthenticationProvider(userDetailsService);
        provider.setPasswordEncoder(passwordEncoder);
        return new ProviderManager(provider);
    }

    @Bean
    public PasswordEncoder passwordEncoder() {
        return new BCryptPasswordEncoder();
    }
}
