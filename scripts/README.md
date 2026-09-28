# Scripts de desarrollo y pruebas — PieJuega

Herramientas para sembrar datos de prueba y verificar el backend end-to-end
contra `http://localhost:8080`.

## Prerequisitos

1. PostgreSQL levantado con Docker:
   ```bash
   docker compose up -d postgres
   ```
2. Backend corriendo (o dejalo que lo arranques tú con `mvnw spring-boot:run`).
3. Python 3.10+ en el PATH.

## api_smoke_test.py — suite E2E (90+ pruebas)

```bash
python scripts/api_smoke_test.py
```

- **Idempotente:** si los usuarios ya existen hace login; los equipos/torneos
  creados usan un sufijo por corrida para no duplicar.
- Cubre: auth (login, refresh, logout, recuperación), perfil de usuario,
  canchas y disponibilidad, reservas (conflictos, permisos, cancelación),
  flujo admin completo (aprobar/rechazar reservas y torneos, CRUD de canchas),
  equipos, chat REST, notificaciones y las reglas de seguridad
  (un usuario normal nunca debe poder usar endpoints de admin).
- Guarda los tokens vigentes en `scripts/.smoke_tokens.json` (ignorado por git).

## ws_stomp_test.py — chat en tiempo real (WebSocket + STOMP)

```bash
python scripts/ws_stomp_test.py
```

Verifica, igual que hace la app Flutter (`stomp_dart_client` contra `/ws`):

1. `CONNECT` exige `Authorization: Bearer <jwt>` (sin token → ERROR).
2. Un mensaje publicado por REST llega **en vivo** a los suscriptores de la sala.
3. El topic personal `/topic/notifications/users/{id}` entrega eventos
   (prueba con un `TEAM_ADDED` real).
4. Suscribirse a una sala ajena es rechazado (ERROR de sesión).
   Se deja al final porque un ERROR termina la sesión STOMP.

## ws_notif_test.py — prueba aislada de notificaciones push-web

```bash
python scripts/ws_notif_test.py
```

Solo el topic personal de notificaciones: útil para depurar el delivery.

## Usuarios de prueba

| Usuario | Email | Contraseña |
|---|---|---|
| Admin | `admin@piejuega.dev` | `Admin1234*` |
| Usuarios | `carlos|andrea|luis|marcela|pedro@piejuega.dev` | `User1234*` |

El rol `ROLE_ADMIN` se asigna por SQL en la primera corrida (el endpoint de
registro nunca crea admins). Si recreas la BD, vuelve a correr la suite para
restaurar todos los datos.

## Limitaciones conocidas del entorno local

- **Cloudinary sin configurar** → `POST /api/media/images` responde 503.
- **Sin SMTP real** → el código de recuperación por email no llega
  (el flujo queda verificado hasta la generación del código, que se guarda
  hasheado). El reset **por teléfono** sí es verificable de punta a punta con
  un ID token real de Firebase (SMS).
