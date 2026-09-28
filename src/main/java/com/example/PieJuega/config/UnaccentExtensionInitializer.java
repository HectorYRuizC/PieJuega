package com.example.PieJuega.config;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.core.annotation.Order;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Component;

/**
 * Habilita la extensión unaccent de PostgreSQL para que las búsquedas
 * (canchas, jugadores) ignoren tildes: "tiburon" encuentra "El Tiburón".
 * El contenedor local de Postgres crea la BD con el usuario dueño, por lo que
 * la extensión (trusted) puede instalarse en el arranque sin pasos manuales.
 */
@Component
@Order(1)
@RequiredArgsConstructor
@Slf4j
public class UnaccentExtensionInitializer implements ApplicationRunner {

    private final JdbcTemplate jdbcTemplate;

    @Override
    public void run(ApplicationArguments args) {
        try {
            jdbcTemplate.execute("CREATE EXTENSION IF NOT EXISTS unaccent");
            log.info("Extensión unaccent verificada para búsqueda sin tildes");
        } catch (Exception exception) {
            log.warn(
                    "No se pudo habilitar unaccent; la búsqueda será sensible a tildes: {}",
                    exception.getMessage()
            );
        }
    }
}
