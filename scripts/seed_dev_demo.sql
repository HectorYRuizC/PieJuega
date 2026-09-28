-- Datos de demostración para la base local de PieJuega.
-- Se puede ejecutar varias veces sin duplicar los registros marcados como Demo.
-- No modifica cuentas ni registros creados por usuarios.
BEGIN;

DO $seed$
DECLARE
    owner_sabanalarga bigint;
    owner_barranquilla bigint;
    field_sabanalarga bigint;
    field_sabanalarga_2 bigint;
    field_barranquilla bigint;
    team_sabanalarga bigint;
    team_barranquilla bigint;
    room_community bigint;
    room_sabanalarga bigint;
    room_barranquilla bigint;
    tournament_sabanalarga bigint;
    tournament_barranquilla bigint;
    reservation_sabanalarga bigint;
    reservation_barranquilla bigint;
BEGIN
    SELECT id INTO owner_sabanalarga
    FROM users
    WHERE city_code = '05628' AND email NOT LIKE 'demo.%@piejuega.test'
    ORDER BY id LIMIT 1;

    SELECT id INTO owner_barranquilla
    FROM users
    WHERE city_code = '08001' AND email NOT LIKE 'demo.%@piejuega.test'
    ORDER BY id LIMIT 1;

    IF owner_sabanalarga IS NULL OR owner_barranquilla IS NULL THEN
        RAISE EXCEPTION 'Se requieren usuarios existentes en Sabanalarga (05628) y Barranquilla (08001)';
    END IF;

    -- Los perfiles Demo aparecen en la búsqueda de jugadores, pero no tienen
    -- credenciales de acceso. Las cuentas existentes conservan sus datos.
    INSERT INTO users (username, email, password, auth_provider, verified, active,
                       city, city_code, department)
    VALUES
      ('Demo Alex Portero', 'demo.alex@piejuega.test', '!DEMO_NO_LOGIN!', 'LOCAL', true, true, 'Sabanalarga', '05628', 'Antioquia'),
      ('Demo Camila Defensa', 'demo.camila@piejuega.test', '!DEMO_NO_LOGIN!', 'LOCAL', true, true, 'Sabanalarga', '05628', 'Antioquia'),
      ('Demo David Defensa', 'demo.david@piejuega.test', '!DEMO_NO_LOGIN!', 'LOCAL', true, true, 'Sabanalarga', '05628', 'Antioquia'),
      ('Demo Lina Volante', 'demo.lina@piejuega.test', '!DEMO_NO_LOGIN!', 'LOCAL', true, true, 'Sabanalarga', '05628', 'Antioquia'),
      ('Demo Mateo Portero', 'demo.mateo@piejuega.test', '!DEMO_NO_LOGIN!', 'LOCAL', true, true, 'Barranquilla', '08001', 'Atlántico'),
      ('Demo Sara Defensa', 'demo.sara@piejuega.test', '!DEMO_NO_LOGIN!', 'LOCAL', true, true, 'Barranquilla', '08001', 'Atlántico'),
      ('Demo Juan Defensa', 'demo.juan@piejuega.test', '!DEMO_NO_LOGIN!', 'LOCAL', true, true, 'Barranquilla', '08001', 'Atlántico'),
      ('Demo Valeria Volante', 'demo.valeria@piejuega.test', '!DEMO_NO_LOGIN!', 'LOCAL', true, true, 'Barranquilla', '08001', 'Atlántico')
    ON CONFLICT (email) DO NOTHING;

    INSERT INTO user_roles (user_id, role_id)
    SELECT u.id, r.id FROM users u CROSS JOIN roles r
    WHERE u.email LIKE 'demo.%@piejuega.test' AND r.name = 'ROLE_USER'
    ON CONFLICT DO NOTHING;

    INSERT INTO football_fields
      (name, address, city, city_code, description, format, rating,
       price_per_hour, opening_time, closing_time, slot_duration_minutes,
       active, created_at)
    SELECT 'Demo Arena Sabanalarga', 'Zona centro, Sabanalarga', 'Sabanalarga', '05628',
           'Cancha de prueba para reservas, favoritos y torneos de fútbol 5.',
           'FIVE', 4.7, 80000, '08:00', '22:00', 60, true, now()
    WHERE NOT EXISTS (SELECT 1 FROM football_fields WHERE name = 'Demo Arena Sabanalarga' AND city_code = '05628');

    INSERT INTO football_fields
      (name, address, city, city_code, description, format, rating,
       price_per_hour, opening_time, closing_time, slot_duration_minutes,
       active, created_at)
    SELECT 'Demo Cancha La Loma', 'Sector deportivo, Sabanalarga', 'Sabanalarga', '05628',
           'Otra opción de prueba para comparar disponibilidad y precios.',
           'SEVEN', 4.5, 110000, '08:00', '22:00', 60, true, now()
    WHERE NOT EXISTS (SELECT 1 FROM football_fields WHERE name = 'Demo Cancha La Loma' AND city_code = '05628');

    SELECT id INTO field_sabanalarga FROM football_fields
    WHERE name = 'Demo Arena Sabanalarga' AND city_code = '05628' ORDER BY id LIMIT 1;
    SELECT id INTO field_sabanalarga_2 FROM football_fields
    WHERE name = 'Demo Cancha La Loma' AND city_code = '05628' ORDER BY id LIMIT 1;
    SELECT id INTO field_barranquilla FROM football_fields
    WHERE name = 'La Bombonera' AND city_code = '08001' ORDER BY id LIMIT 1;

    IF field_barranquilla IS NULL THEN
        RAISE EXCEPTION 'No se encontró la cancha base La Bombonera';
    END IF;

    INSERT INTO field_open_days (field_id, day_of_week)
    SELECT f.id, d.day_name
    FROM football_fields f
    CROSS JOIN unnest(ARRAY['MONDAY','TUESDAY','WEDNESDAY','THURSDAY','FRIDAY','SATURDAY','SUNDAY']) AS d(day_name)
    WHERE f.id IN (field_sabanalarga, field_sabanalarga_2)
    ON CONFLICT DO NOTHING;

    INSERT INTO field_features (field_id, feature)
    SELECT f.id, feature.name
    FROM football_fields f
    CROSS JOIN unnest(ARRAY['Grama sintética','Iluminación','Camerinos','Parqueadero']) AS feature(name)
    WHERE f.id IN (field_sabanalarga, field_sabanalarga_2)
    ON CONFLICT DO NOTHING;

    INSERT INTO field_favorites (user_id, field_id, created_at)
    VALUES (owner_sabanalarga, field_sabanalarga, now()),
           (owner_sabanalarga, field_sabanalarga_2, now()),
           (owner_barranquilla, field_barranquilla, now())
    ON CONFLICT (user_id, field_id) DO NOTHING;

    SELECT id INTO room_community FROM chat_rooms
    WHERE name = 'Comunidad PieJuega' ORDER BY id LIMIT 1;
    IF room_community IS NULL THEN
        INSERT INTO chat_rooms (name, category, created_by, created_at, updated_at)
        VALUES ('Comunidad PieJuega', 'Comunidad', owner_sabanalarga, now(), now())
        RETURNING id INTO room_community;
    END IF;

    INSERT INTO chat_rooms (name, category, created_by, created_at, updated_at)
    SELECT 'Demo Titanes de Sabanalarga', 'Equipo', owner_sabanalarga, now(), now()
    WHERE NOT EXISTS (SELECT 1 FROM chat_rooms WHERE name = 'Demo Titanes de Sabanalarga' AND created_by = owner_sabanalarga);
    INSERT INTO chat_rooms (name, category, created_by, created_at, updated_at)
    SELECT 'Demo Tiburones del Norte', 'Equipo', owner_barranquilla, now(), now()
    WHERE NOT EXISTS (SELECT 1 FROM chat_rooms WHERE name = 'Demo Tiburones del Norte' AND created_by = owner_barranquilla);

    SELECT id INTO room_sabanalarga FROM chat_rooms
    WHERE name = 'Demo Titanes de Sabanalarga' AND created_by = owner_sabanalarga ORDER BY id LIMIT 1;
    SELECT id INTO room_barranquilla FROM chat_rooms
    WHERE name = 'Demo Tiburones del Norte' AND created_by = owner_barranquilla ORDER BY id LIMIT 1;

    INSERT INTO football_teams
      (name, description, city, primary_color, secondary_color, format,
       formation, owner_id, chat_room_id, active, created_at)
    SELECT 'Demo Titanes de Sabanalarga', 'Equipo de prueba de fútbol 5.',
           'Sabanalarga', '#267A78', '#ECFFFC', 'FIVE', '1-2-1',
           owner_sabanalarga, room_sabanalarga, true, now()
    WHERE NOT EXISTS (SELECT 1 FROM football_teams WHERE name = 'Demo Titanes de Sabanalarga' AND owner_id = owner_sabanalarga);
    INSERT INTO football_teams
      (name, description, city, primary_color, secondary_color, format,
       formation, owner_id, chat_room_id, active, created_at)
    SELECT 'Demo Tiburones del Norte', 'Equipo de prueba de fútbol 5.',
           'Barranquilla', '#155356', '#40E0D0', 'FIVE', '1-2-1',
           owner_barranquilla, room_barranquilla, true, now()
    WHERE NOT EXISTS (SELECT 1 FROM football_teams WHERE name = 'Demo Tiburones del Norte' AND owner_id = owner_barranquilla);

    SELECT id INTO team_sabanalarga FROM football_teams
    WHERE name = 'Demo Titanes de Sabanalarga' AND owner_id = owner_sabanalarga ORDER BY id LIMIT 1;
    SELECT id INTO team_barranquilla FROM football_teams
    WHERE name = 'Demo Tiburones del Norte' AND owner_id = owner_barranquilla ORDER BY id LIMIT 1;

    INSERT INTO team_members (team_id, user_id, squad_role, position, slot_index, captain)
    VALUES (team_sabanalarga, owner_sabanalarga, 'STARTER', 'FW', 4, true),
           (team_barranquilla, owner_barranquilla, 'STARTER', 'FW', 4, true)
    ON CONFLICT (team_id, user_id) DO NOTHING;

    INSERT INTO team_members (team_id, user_id, squad_role, position, slot_index, captain)
    SELECT CASE WHEN assignment.city = '05628' THEN team_sabanalarga ELSE team_barranquilla END,
           u.id, 'STARTER', assignment.position, assignment.slot_index, false
    FROM (VALUES
      ('demo.alex@piejuega.test', '05628', 'GK', 0),
      ('demo.camila@piejuega.test', '05628', 'DF', 1),
      ('demo.david@piejuega.test', '05628', 'DF', 2),
      ('demo.lina@piejuega.test', '05628', 'MF', 3),
      ('demo.mateo@piejuega.test', '08001', 'GK', 0),
      ('demo.sara@piejuega.test', '08001', 'DF', 1),
      ('demo.juan@piejuega.test', '08001', 'DF', 2),
      ('demo.valeria@piejuega.test', '08001', 'MF', 3)
    ) AS assignment(email, city, position, slot_index)
    JOIN users u ON u.email = assignment.email
    ON CONFLICT (team_id, user_id) DO NOTHING;

    INSERT INTO chat_room_members (room_id, user_id, joined_at, last_read_message_id)
    SELECT room_community, u.id, now(), 0 FROM users u
    WHERE u.id IN (owner_sabanalarga, owner_barranquilla)
       OR u.email LIKE 'demo.%@piejuega.test'
    ON CONFLICT (room_id, user_id) DO NOTHING;

    INSERT INTO chat_room_members (room_id, user_id, joined_at, last_read_message_id)
    SELECT CASE WHEN tm.team_id = team_sabanalarga THEN room_sabanalarga ELSE room_barranquilla END,
           tm.user_id, now(), 0
    FROM team_members tm WHERE tm.team_id IN (team_sabanalarga, team_barranquilla)
    ON CONFLICT (room_id, user_id) DO NOTHING;

    INSERT INTO chat_messages (room_id, sender_id, content, message_type, sent_at)
    SELECT sample.room_id, sample.sender_id, sample.content, 'TEXT', now() - sample.age
    FROM (VALUES
      (room_community, owner_sabanalarga, 'Demo: ¿quién se apunta a un partido esta semana?', interval '2 hours'),
      (room_community, owner_barranquilla, 'Demo: ¡yo me apunto! Revisemos las canchas disponibles.', interval '1 hour'),
      (room_sabanalarga, owner_sabanalarga, 'Demo: bienvenidos al equipo. Organicemos la próxima fecha.', interval '50 minutes'),
      (room_barranquilla, owner_barranquilla, 'Demo: ya tenemos plantilla completa para el torneo.', interval '35 minutes')
    ) AS sample(room_id, sender_id, content, age)
    WHERE NOT EXISTS (SELECT 1 FROM chat_messages m WHERE m.room_id = sample.room_id AND m.content = sample.content)
    ;

    INSERT INTO tournaments
      (name, description, rules, format, field_id, creator_id, approved_by_id,
       starts_at, registration_deadline, max_teams, entry_fee, prize, status,
       created_at, updated_at, version)
    SELECT 'Demo Copa Sabanalarga', 'Torneo de prueba para equipos de fútbol 5.',
           'Partidos de 40 minutos. Presentarse 20 minutos antes.', 'FIVE',
           field_sabanalarga, owner_sabanalarga, owner_sabanalarga,
           current_date + interval '14 days 10 hours', current_date + interval '11 days',
           8, 60000, 'Trofeo y medallas', 'OPEN_REGISTRATION', now(), now(), 0
    WHERE NOT EXISTS (SELECT 1 FROM tournaments WHERE name = 'Demo Copa Sabanalarga');

    INSERT INTO tournaments
      (name, description, rules, format, field_id, creator_id, approved_by_id,
       starts_at, registration_deadline, max_teams, entry_fee, prize, status,
       created_at, updated_at, version)
    SELECT 'Demo Copa Barranquilla', 'Torneo de prueba para equipos de fútbol 5.',
           'Eliminación directa y juego limpio.', 'FIVE', field_barranquilla,
           owner_barranquilla, owner_barranquilla,
           current_date + interval '21 days 18 hours', current_date + interval '18 days',
           8, 80000, 'Trofeo y bono deportivo', 'OPEN_REGISTRATION', now(), now(), 0
    WHERE NOT EXISTS (SELECT 1 FROM tournaments WHERE name = 'Demo Copa Barranquilla');

    INSERT INTO tournaments
      (name, description, rules, format, field_id, creator_id, starts_at,
       registration_deadline, max_teams, entry_fee, prize, status,
       created_at, updated_at, version)
    SELECT 'Demo Torneo en revisión', 'Propuesta de prueba para la vista administrativa.',
           'Reglamento pendiente de aprobación.', 'SEVEN', field_sabanalarga_2,
           owner_sabanalarga, current_date + interval '28 days 17 hours',
           current_date + interval '25 days', 8, 50000, 'Medallas',
           'PENDING_APPROVAL', now(), now(), 0
    WHERE NOT EXISTS (SELECT 1 FROM tournaments WHERE name = 'Demo Torneo en revisión');

    SELECT id INTO tournament_sabanalarga FROM tournaments
    WHERE name = 'Demo Copa Sabanalarga' ORDER BY id LIMIT 1;
    SELECT id INTO tournament_barranquilla FROM tournaments
    WHERE name = 'Demo Copa Barranquilla' ORDER BY id LIMIT 1;

    INSERT INTO tournament_registrations
      (tournament_id, team_id, registered_by_id, created_at)
    VALUES (tournament_sabanalarga, team_sabanalarga, owner_sabanalarga, now()),
           (tournament_barranquilla, team_barranquilla, owner_barranquilla, now())
    ON CONFLICT (tournament_id, team_id) DO NOTHING;

    INSERT INTO reservations
      (field_id, user_id, start_at, end_at, contact_name, contact_phone,
       total_price, payment_method, note, status, created_at, updated_at, version)
    SELECT sample.field_id, sample.user_id, sample.start_at,
           sample.start_at + interval '1 hour', u.username, '3000000000',
           f.price_per_hour, sample.payment_method, sample.note, sample.status,
           now(), now(), 0
    FROM (VALUES
      (field_sabanalarga, owner_sabanalarga, current_date + interval '3 days 18 hours', 'NEQUI', 'Demo: partido entre amigos', 'APPROVED'),
      (field_sabanalarga_2, owner_sabanalarga, current_date + interval '5 days 19 hours', 'CASH', 'Demo: reserva pendiente de revisión', 'PENDING'),
      (field_barranquilla, owner_barranquilla, current_date + interval '4 days 19 hours', 'CASH', 'Demo: entrenamiento de equipo', 'APPROVED'),
      (field_barranquilla, owner_barranquilla, current_date + interval '6 days 20 hours', 'NEQUI', 'Demo: solicitud por revisar', 'PENDING'),
      (field_sabanalarga, owner_barranquilla, current_date + interval '8 days 18 hours', 'OTHER', 'Demo: reserva rechazada', 'REJECTED')
    ) AS sample(field_id, user_id, start_at, payment_method, note, status)
    JOIN users u ON u.id = sample.user_id
    JOIN football_fields f ON f.id = sample.field_id
    WHERE NOT EXISTS (SELECT 1 FROM reservations r WHERE r.note = sample.note AND r.user_id = sample.user_id);

    SELECT id INTO reservation_sabanalarga FROM reservations
    WHERE user_id = owner_sabanalarga AND note = 'Demo: partido entre amigos' ORDER BY id LIMIT 1;
    SELECT id INTO reservation_barranquilla FROM reservations
    WHERE user_id = owner_barranquilla AND note = 'Demo: entrenamiento de equipo' ORDER BY id LIMIT 1;

    INSERT INTO notifications
      (recipient_id, type, title, body, destination, source_id, created_at)
    SELECT sample.recipient_id, sample.type, sample.title, sample.body,
           sample.destination, sample.source_id, now() - sample.age
    FROM (VALUES
      (owner_sabanalarga, 'RESERVATION_APPROVED', 'Demo: reserva aprobada', 'Tu partido en Demo Arena Sabanalarga está confirmado.', '/reservations/' || reservation_sabanalarga, reservation_sabanalarga, interval '20 minutes'),
      (owner_sabanalarga, 'TOURNAMENT_REGISTRATION', 'Demo: equipo inscrito', 'Titanes ya está inscrito en la Copa Sabanalarga.', '/tournaments/' || tournament_sabanalarga, tournament_sabanalarga, interval '1 hour'),
      (owner_sabanalarga, 'CHAT_MESSAGE', 'Demo: mensaje nuevo', 'Tienes un mensaje en la comunidad.', '/teamChat/' || room_community, room_community, interval '2 hours'),
      (owner_barranquilla, 'RESERVATION_APPROVED', 'Demo: reserva aprobada', 'La Bombonera confirmó tu entrenamiento.', '/reservations/' || reservation_barranquilla, reservation_barranquilla, interval '30 minutes'),
      (owner_barranquilla, 'TOURNAMENT_REGISTRATION', 'Demo: equipo inscrito', 'Tiburones ya está inscrito en la Copa Barranquilla.', '/tournaments/' || tournament_barranquilla, tournament_barranquilla, interval '90 minutes')
    ) AS sample(recipient_id, type, title, body, destination, source_id, age)
    WHERE NOT EXISTS (
      SELECT 1 FROM notifications n
      WHERE n.recipient_id = sample.recipient_id
        AND n.type = sample.type AND n.title = sample.title
        AND n.source_id = sample.source_id
    );
END
$seed$;

COMMIT;
