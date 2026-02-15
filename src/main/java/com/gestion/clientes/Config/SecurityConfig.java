package com.gestion.clientes.Config;

import org.springframework.beans.factory.annotation.Value; // Importante: Importar @Value
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.core.userdetails.User;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.provisioning.InMemoryUserDetailsManager;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.web.cors.CorsConfiguration;
import org.springframework.web.cors.UrlBasedCorsConfigurationSource;
import java.util.List;

@Configuration
@EnableWebSecurity
public class SecurityConfig {

    // Inyectamos el usuario desde application.yml (que a su vez viene de Render)
    @Value("${security.admin.username}")
    private String adminUsername;

    // Inyectamos la contraseña desde application.yml
    @Value("${security.admin.password}")
    private String adminPassword;

    @Bean
    public SecurityFilterChain filterChain(HttpSecurity http) throws Exception {
        http
            // Desactivar CSRF para permitir POST desde React
            .csrf(csrf -> csrf.disable())
            .cors(cors -> cors.configurationSource(corsConfigurationSource())) // Habilitar CORS explícitamente
            .authorizeHttpRequests(auth -> auth
                .requestMatchers("/uploads/**").permitAll() // Permitir ver fotos
                .requestMatchers("/api/productos/**").permitAll() // Permitir ver productos
                .requestMatchers("/api/admin/**").authenticated() // Solo admin puede guardar/borrar
                .anyRequest().permitAll()
            )
            .httpBasic(basic -> {}); // Usar autenticación básica
        
        return http.build();
    }

    // Configuración CORS para que React (puerto 5173) pueda hablar con Spring (8080)
    @Bean
    public UrlBasedCorsConfigurationSource corsConfigurationSource() {
        CorsConfiguration config = new CorsConfiguration();
        config.setAllowedOriginPatterns(List.of("*")); // Permitir cualquier origen (React)
        config.setAllowedMethods(List.of("GET", "POST", "PUT", "DELETE", "OPTIONS"));
        config.setAllowedHeaders(List.of("*"));
        config.setAllowCredentials(true);

        UrlBasedCorsConfigurationSource source = new UrlBasedCorsConfigurationSource();
        source.registerCorsConfiguration("/**", config);
        return source;
    }

    @Bean
    public UserDetailsService userDetailsService() {
        // AQUI ESTÁ LA PROTECCIÓN:
        // Usamos las variables inyectadas en lugar de escribir el texto fijo
        UserDetails admin = User.withUsername(adminUsername)
                .password("{noop}" + adminPassword) // Concatenamos {noop} a la contraseña que venga de Render
                .roles("ADMIN")
                .build();
        return new InMemoryUserDetailsManager(admin);
    }
}