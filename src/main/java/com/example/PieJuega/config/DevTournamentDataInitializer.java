package com.example.PieJuega.config;

import com.example.PieJuega.model.FootballField;
import com.example.PieJuega.model.Tournament;
import com.example.PieJuega.model.User;
import com.example.PieJuega.repository.FootballFieldRepository;
import com.example.PieJuega.repository.TournamentRepository;
import com.example.PieJuega.repository.UserRepository;
import com.example.PieJuega.util.TournamentStatus;
import lombok.RequiredArgsConstructor;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.context.annotation.Profile;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.List;

@Component
@Profile("dev")
@Order(20)
@RequiredArgsConstructor
public class DevTournamentDataInitializer implements ApplicationRunner {

    private final TournamentRepository tournamentRepository;
    private final FootballFieldRepository fieldRepository;
    private final UserRepository userRepository;

    @Override
    @Transactional
    public void run(ApplicationArguments args) {
        if (tournamentRepository.count() > 0) {
            return;
        }
        List<User> users = userRepository.findAll();
        List<FootballField> fields = fieldRepository.findAll();
        if (users.isEmpty() || fields.isEmpty()) {
            return;
        }

        User creator = users.get(0);
        LocalDateTime firstStart = LocalDateTime.now()
                .plusDays(9)
                .with(LocalTime.of(9, 0));
        LocalDateTime secondStart = LocalDateTime.now()
                .plusDays(16)
                .with(LocalTime.of(15, 0));

        FootballField firstField = fields.get(0);
        FootballField secondField = fields.size() > 1 ? fields.get(1) : firstField;
        tournamentRepository.saveAll(List.of(
                tournament(
                        "Copa PieJuega Caribe",
                        "Una jornada competitiva para medir a los mejores equipos de la comunidad.",
                        firstField,
                        creator,
                        firstStart,
                        16,
                        "Trofeo, medallas y bono deportivo",
                        "Partidos de eliminación directa. Cada equipo debe presentarse 30 minutos antes."
                ),
                tournament(
                        "Reto Nocturno Barranquilla",
                        "Torneo corto bajo las luces para equipos que buscan fútbol intenso y buen ambiente.",
                        secondField,
                        creator,
                        secondStart,
                        8,
                        "Trofeo y reserva gratuita para la final",
                        "Fase de grupos y semifinales. Se aplican reglas FIFA adaptadas al formato."
                )
        ));
    }

    private Tournament tournament(
            String name,
            String description,
            FootballField field,
            User creator,
            LocalDateTime startsAt,
            int maxTeams,
            String prize,
            String rules
    ) {
        return Tournament.builder()
                .name(name)
                .description(description)
                .rules(rules)
                .format(field.getFormat())
                .field(field)
                .creator(creator)
                .approvedBy(creator)
                .startsAt(startsAt)
                .registrationDeadline(startsAt.minusDays(2))
                .maxTeams(maxTeams)
                .entryFee(new BigDecimal("120000"))
                .prize(prize)
                .status(TournamentStatus.OPEN_REGISTRATION)
                .build();
    }
}
