# -*- coding: utf-8 -*-
"""
Suite de pruebas end-to-end + siembra de datos para el backend PieJuega.

Uso:
    python scripts/api_smoke_test.py

Prerequisitos:
    - Backend corriendo en http://localhost:8080
    - PostgreSQL (docker) accesible para promover el usuario admin:
        docker exec piejuega_postgres psql -U piejuega -d piejuega_db ...

El script es idempotente: si los usuarios ya existen, hace login en su lugar.
Al final guarda los tokens en scripts/.smoke_tokens.json para pruebas manuales.
"""
import base64
import io
import json
import subprocess
import sys
import urllib.error
import urllib.request
import uuid
from datetime import datetime, timedelta

BASE = "http://localhost:8080"

ADMIN_EMAIL = "admin@piejuega.dev"
ADMIN_PASS = "Admin1234*"
USERS = [
    ("carlos@piejuega.dev", "Carlos Perez", "573001111101", "User1234*"),
    ("andrea@piejuega.dev", "Andrea Gomez", "573001111102", "User1234*"),
    ("luis@piejuega.dev", "Luis Martinez", "573001111103", "User1234*"),
    ("marcela@piejuega.dev", "Marcela Torres", "573001111104", "User1234*"),
    ("pedro@piejuega.dev", "Pedro Ramirez", "573001111105", "User1234*"),
]

results = []
failures = []


def check(name, ok, detail=""):
    results.append((name, ok, detail))
    if not ok:
        failures.append((name, detail))
    tag = "PASS" if ok else "FAIL"
    print(f"[{tag}] {name}" + (f"  -> {detail}" if (detail and not ok) else ""))


def skip(name, detail=""):
    results.append((name, True, detail))
    print(f"[SKIP] {name}" + (f"  -> {detail}" if detail else ""))


def call(method, path, token=None, body=None, raw_body=None, headers=None):
    url = BASE + path
    data = None
    req_headers = {"Content-Type": "application/json", "Accept": "application/json"}
    if headers:
        req_headers.update(headers)
    if token:
        req_headers["Authorization"] = f"Bearer {token}"
    if body is not None:
        data = json.dumps(body).encode("utf-8")
    elif raw_body is not None:
        data = raw_body
    req = urllib.request.Request(url, data=data, headers=req_headers, method=method)
    try:
        with urllib.request.urlopen(req, timeout=30) as resp:
            text = resp.read().decode("utf-8", "replace")
            payload = json.loads(text) if text.strip() else None
            return resp.status, payload
    except urllib.error.HTTPError as e:
        text = e.read().decode("utf-8", "replace")
        try:
            payload = json.loads(text) if text.strip() else None
        except json.JSONDecodeError:
            payload = text[:300]
        return e.code, payload


def register_or_login(email, username, phone, password):
    st, body = call("POST", "/api/auth/register", body={
        "username": username, "email": email, "phone": phone,
        "city": "Barranquilla", "dateBirth": "15/05/1998",
        "password": password,
    })
    if st == 200:
        mode = "registered"
    elif st == 409:
        mode = "already-exists"
    else:
        return "register-failed", st, body
    st2, body2 = call("POST", "/api/auth/login", body={"identifier": email, "password": password})
    return mode, st2, body2


def psql(sql):
    cmd = ["docker", "exec", "piejuega_postgres", "psql", "-U", "piejuega",
           "-d", "piejuega_db", "-t", "-A", "-c", sql]
    out = subprocess.run(cmd, capture_output=True, text=True)
    if out.returncode != 0:
        return None, out.stderr.strip()
    return out.stdout.strip(), None


def future_slot(days_ahead, hour=15):
    return (datetime.now() + timedelta(days=days_ahead)).replace(
        hour=hour, minute=0, second=0, microsecond=0)


def main():
    print("=" * 70)
    print("PIEJUEGA - SMOKE TEST E2E")
    print("=" * 70)

    # sufijo por corrida: evita duplicar equipos/torneos/canchas de prueba
    run_id = datetime.now().strftime("%H%M")

    # ---------- A. AUTH ----------
    st, _ = call("GET", "/api/locations/cities")
    check("A1 Sin token en endpoint protegido -> rechazado", st in (401, 403), f"status={st}")

    tokens = {}
    ids = {}
    for email, username, phone, pwd in USERS:
        mode, st, body = register_or_login(email, username, phone, pwd)
        check(f"A2 Registro/login {email} ({mode})", st == 200 and body and "accessToken" in str(body)[:200000], f"status={st} body={str(body)[:200]}")
        if st == 200 and isinstance(body, dict) and body.get("accessToken"):
            tokens[email] = body["accessToken"]
            ids[email] = body["user"]["id"]

    # admin: registrar, promover via SQL y login
    mode, st, body = register_or_login(ADMIN_EMAIL, "Admin General", "573001111100", ADMIN_PASS)
    ok = st == 200 and isinstance(body, dict) and "accessToken" in body
    check("A3 Registro/login admin", ok, f"status={st} body={str(body)[:200]}")
    if not ok:
        print("No se pudo continuar sin admin."); return

    row, err = psql(
        "INSERT INTO user_roles (user_id, role_id) "
        f"SELECT u.id, r.id FROM users u, roles r WHERE u.email='{ADMIN_EMAIL}' AND r.name='ROLE_ADMIN' "
        "AND NOT EXISTS (SELECT 1 FROM user_roles ur WHERE ur.user_id=u.id AND ur.role_id=r.id) "
        "RETURNING role_id;"
    )
    check("A4 Promocion admin via SQL", err is None and row != "", f"row={row} err={err}")

    st, body = call("POST", "/api/auth/login", body={"identifier": ADMIN_EMAIL, "password": ADMIN_PASS})
    ok = st == 200 and isinstance(body, dict)
    check("A5 Login admin", ok, f"status={st}")
    tokens[ADMIN_EMAIL] = body["accessToken"]
    ids[ADMIN_EMAIL] = body["user"]["id"]
    admin_refresh = body.get("refreshToken")
    roles = body["user"].get("roles", [])
    check("A6 Token de usuario incluye ROLE_ADMIN", any("ROLE_ADMIN" in str(r) for r in roles), f"roles={roles}")

    carlos, andrea, luis, marcela, pedro = [u[0] for u in USERS]
    t_carlos, t_andrea, t_luis, t_marcela, t_pedro = (tokens[u[0]] for u in USERS)
    t_admin = tokens[ADMIN_EMAIL]

    st, body = call("POST", "/api/auth/login", body={"identifier": carlos, "password": "incorrecta"})
    check("A7 Login con password incorrecta -> 401", st == 401, f"status={st}")

    st, body = call("POST", "/api/auth/refresh", body={"refreshToken": admin_refresh})
    check("A8 Refresh token valido", st == 200 and isinstance(body, dict) and body.get("accessToken"), f"status={st}")

    st, body = call("POST", "/api/auth/logout", body={"refreshToken": admin_refresh})
    check("A9 Logout -> 204", st == 204, f"status={st}")
    st, body = call("POST", "/api/auth/refresh", body={"refreshToken": admin_refresh})
    check("A10 Refresh tras logout -> 401", st == 401, f"status={st}")

    st, body = call("GET", f"/api/auth/phoneExist/573001111101")
    check("A11 phoneExist", st == 200 and isinstance(body, dict) and "authProvider" in str(body), f"status={st} body={body}")

    # ---------- B. PERFIL DE USUARIO ----------
    st, body = call("GET", "/api/user/me", token=t_carlos)
    check("B1 GET /user/me", st == 200 and isinstance(body, dict) and body.get("email") == carlos, f"status={st}")

    st, body = call("PUT", "/api/user/me", token=t_carlos, body={"username": "Carlos Perez Test"})
    check("B2 PUT /user/me (username)", st == 200 and body.get("username") == "Carlos Perez Test", f"status={st} body={str(body)[:200]}")

    st, body = call("PUT", "/api/user/me", token=t_carlos, body={"username": "Carlos Perez"})
    check("B3 Restaurar username", st == 200, f"status={st}")

    st, body = call("PUT", "/api/user/me", token=t_carlos, body={"email": andrea})
    check("B4 Update con email ajeno rechazado", st in (400, 409), f"status={st} (BUG si 500) body={str(body)[:150]}")

    st, body = call("PUT", "/api/user/me/location", token=t_andrea, body={
        "city": "Barranquilla", "cityCode": "08001", "department": "Atlantico",
        "latitude": 10.9639, "longitude": -74.7964})
    check("B5 PUT /user/me/location", st == 200 and (body or {}).get("cityCode") == "08001", f"status={st} body={str(body)[:200]}")

    st, body = call("PUT", "/api/user/me/password", token=t_marcela, body={
        "currentPassword": "User1234*", "newPassword": "NewPass456*", "confirmNewPassword": "NewPass456*"})
    check("B6 Cambio de password", st in (200, 204), f"status={st} body={str(body)[:150]}")
    st, body = call("POST", "/api/auth/login", body={"identifier": marcela, "password": "NewPass456*"})
    check("B7 Login con password nueva", st == 200, f"status={st}")
    if st == 200:
        t_marcela = body["accessToken"]
    st, body = call("PUT", "/api/user/me/password", token=t_marcela, body={
        "currentPassword": "NewPass456*", "newPassword": "User1234*", "confirmNewPassword": "User1234*"})
    check("B8 Revertir password", st in (200, 204), f"status={st}")

    # ---------- C. CANCHAS ----------
    st, body = call("GET", "/api/locations/cities?query=barranquilla", token=t_carlos)
    check("C1 Cities con query", st == 200 and isinstance(body, list) and len(body) > 0, f"status={st}")

    st, fields = call("GET", "/api/fields?city=Barranquilla", token=t_carlos)
    check("C2 GET /fields lista (con ciudad)", st == 200 and isinstance(fields, list) and len(fields) >= 4, f"status={st} n={len(fields) if isinstance(fields, list) else fields}")
    fid1 = fields[0]["id"]
    fid2 = fields[1]["id"]

    st, body = call("GET", "/api/fields", token=t_carlos)
    check("C2b INFO: /fields sin ciudad devuelve vacio", st == 200 and body == [], f"status={st} n={len(body) if isinstance(body, list) else body} (comportamiento: ciudad obligatoria)")

    # "tiburon" sin tilde debe encontrar "El Tiburón" (búsqueda sin acentos)
    st, body = call("GET", "/api/fields?query=Tiburon&city=Barranquilla", token=t_carlos)
    check("C3 Filtro por nombre (sin acentos)", st == 200 and isinstance(body, list) and len(body) == 1 and "Tibur" in body[0]["name"], f"status={st} n={len(body) if isinstance(body, list) else body}")

    st, body = call("GET", "/api/fields?format=FIVE", token=t_carlos)
    check("C4 Filtro por formato", st == 200 and isinstance(body, list) and all(f.get("format") == "FIVE" for f in body), f"status={st}")

    st, body = call("GET", f"/api/fields/{fid1}", token=t_carlos)
    check("C5 Detalle de cancha", st == 200 and body.get("id") == fid1, f"status={st}")

    slot = None
    free_slots = []
    for delta in range(2, 9):
        d = (datetime.now() + timedelta(days=delta)).strftime("%Y-%m-%d")
        st, slots = call("GET", f"/api/fields/{fid1}/availability?date={d}", token=t_carlos)
        if st == 200 and isinstance(slots, list):
            free = [s for s in slots if s.get("available")]
            if free:
                slot = free[0]["startAt"]
                free_slots = [s["startAt"] for s in free]
                check(f"C6 Disponibilidad (+{delta}d, libres {len(free)}/{len(slots)})", True)
                print(f"   slot elegido: {slot}")
                break
    if slot is None:
        check("C6 Disponibilidad", False, "ningun dia con slots libres en los proximos 8 dias")

    # slots alternativos en la segunda cancha (para la reserva de luis)
    slot_f2 = None
    for delta in range(2, 9):
        d = (datetime.now() + timedelta(days=delta)).strftime("%Y-%m-%d")
        st, slots = call("GET", f"/api/fields/{fid2}/availability?date={d}", token=t_carlos)
        if st == 200 and isinstance(slots, list):
            free = [s for s in slots if s.get("available")]
            if free:
                slot_f2 = free[0]["startAt"]
                break

    # ---------- D. RESERVAS (USUARIO) ----------
    st, res = call("POST", "/api/reservations", token=t_carlos, body={
        "fieldId": fid1, "startAt": slot, "contactName": "Carlos Perez",
        "contactPhone": "573001111101", "paymentMethod": "CASH", "note": "Partido amistoso"})
    check("D1 Crear reserva", st in (200, 201) and isinstance(res, dict) and res.get("status") == "PENDING", f"status={st} body={str(res)[:200]}")
    res_carlos_id = res.get("id") if isinstance(res, dict) else None

    st, res = call("POST", "/api/reservations", token=t_luis, body={
        "fieldId": fid1, "startAt": slot, "contactName": "Luis",
        "contactPhone": "573001111103", "paymentMethod": "NEQUI", "note": ""})
    check("D2 Reserva en slot ocupado -> 409", st == 409, f"status={st} body={str(res)[:150]}")

    past = (datetime.now() - timedelta(days=1)).strftime("%Y-%m-%dT10:00:00")
    st, res = call("POST", "/api/reservations", token=t_luis, body={
        "fieldId": fid1, "startAt": past, "contactName": "Luis",
        "contactPhone": "573001111103", "paymentMethod": "CASH"})
    check("D3 Reserva en pasado -> 400", st == 400, f"status={st}")

    slot2 = free_slots[1] if len(free_slots) > 1 else future_slot(4, 16).strftime("%Y-%m-%dT%H:%M:%S")
    st, res = call("POST", "/api/reservations", token=t_andrea, body={
        "fieldId": fid1, "startAt": slot2, "contactName": "Andrea",
        "contactPhone": "573001111102", "paymentMethod": "NEQUI"})
    check("D4 Segunda reserva (otro slot)", st in (200, 201), f"status={st} body={str(res)[:150]}")
    res_andrea_id = res.get("id") if isinstance(res, dict) else None

    st, res = call("POST", "/api/reservations", token=t_luis, body={
        "fieldId": fid2, "startAt": slot_f2, "contactName": "Luis",
        "contactPhone": "573001111103", "paymentMethod": "CASH"})
    check("D5 Tercera reserva (para cancelar)", st in (200, 201), f"status={st} body={str(res)[:150]}")
    res_luis_id = res.get("id") if isinstance(res, dict) else None

    st, mine = call("GET", "/api/reservations/mine", token=t_carlos)
    check("D6 GET /reservations/mine", st == 200 and isinstance(mine, list) and any(r.get("id") == res_carlos_id for r in mine), f"status={st}")

    st, body = call("GET", f"/api/reservations/{res_carlos_id}", token=t_andrea)
    check("D7 Reserva ajena -> 403", st == 403, f"status={st}")

    st, body = call("GET", "/api/admin/reservations", token=t_carlos)
    check("D8 Endpoint admin como usuario normal -> 403", st == 403, f"status={st} (BUG si 200) body={str(body)[:150]}")

    st, body = call("PATCH", f"/api/reservations/{res_luis_id}/cancel", token=t_andrea)
    check("D9 Cancelar reserva ajena -> 403", st == 403, f"status={st}")
    st, body = call("PATCH", f"/api/reservations/{res_luis_id}/cancel", token=t_luis)
    check("D10 Cancelar reserva propia", st == 200 and body.get("status") == "CANCELLED", f"status={st} body={str(body)[:150]}")

    # ---------- E. ADMIN: RESERVAS ----------
    st, body = call("GET", "/api/admin/reservations?status=PENDING", token=t_admin)
    ok = st == 200 and isinstance(body, list) and any(r.get("id") == res_carlos_id for r in body)
    check("E1 Admin lista PENDING", ok, f"status={st} n={len(body) if isinstance(body, list) else body}")

    st, body = call("PATCH", f"/api/admin/reservations/{res_carlos_id}/approve", token=t_admin)
    check("E2 Aprobar reserva", st == 200 and body.get("status") == "APPROVED", f"status={st} body={str(body)[:150]}")

    st, body = call("PATCH", f"/api/admin/reservations/{res_andrea_id}/reject", token=t_admin,
                    body={"reason": "La cancha ya esta reservada para mantenimiento"})
    check("E3 Rechazar reserva con motivo", st == 200 and body.get("status") == "REJECTED", f"status={st} body={str(body)[:150]}")

    st, notifs = call("GET", "/api/notifications", token=t_carlos)
    ok = st == 200 and isinstance(notifs, list) and any(
        n.get("type") == "RESERVATION_APPROVED" or "aprobada" in str(n.get("title", "")).lower() for n in notifs)
    check("E4 Notificacion de aprobacion para carlos", ok, f"status={st} notifs={json.dumps(notifs)[:300] if st==200 else notifs}")

    st, notifs = call("GET", "/api/notifications", token=t_andrea)
    ok = st == 200 and isinstance(notifs, list) and any(
        n.get("type") == "RESERVATION_REJECTED" or "no aprobada" in str(n.get("title", "")).lower() for n in notifs)
    check("E5 Notificacion de rechazo para andrea", ok, f"status={st} notifs={json.dumps(notifs)[:300] if st==200 else notifs}")

    st, det = call("GET", f"/api/reservations/{res_andrea_id}", token=t_andrea)
    check("E5b Motivo de rechazo visible para el dueno", st == 200 and det.get("rejectionReason") == "La cancha ya esta reservada para mantenimiento", f"status={st} body={str(det)[:250]}")

    st, body = call("GET", "/api/notifications/unread-count", token=t_carlos)
    unread_before = body.get("count") if isinstance(body, dict) else None
    check("E6 unread-count > 0", st == 200 and unread_before and unread_before > 0, f"status={st} body={body}")

    st, notifs = call("GET", "/api/notifications", token=t_carlos)
    nid = notifs[0]["id"]
    st, body = call("PATCH", f"/api/notifications/{nid}/read", token=t_carlos)
    check("E7 Marcar una leida", st == 200 and body.get("read") is True, f"status={st}")
    st, _ = call("PATCH", "/api/notifications/read-all", token=t_carlos)
    check("E8 Marcar todas leidas", st == 204, f"status={st}")
    st, body = call("GET", "/api/notifications/unread-count", token=t_carlos)
    check("E9 unread=0 tras read-all", st == 200 and body.get("count") == 0, f"status={st} body={body}")

    # ---------- F. EQUIPOS ----------
    st, players = call("GET", "/api/teams/players?query=&city=Barranquilla", token=t_carlos)
    ok = st == 200 and isinstance(players, list) and len(players) >= 5
    check("F1 Buscar jugadores", ok, f"status={st} n={len(players) if isinstance(players, list) else players}")
    # PlayerSearchResponseDTO expone username (no email)
    pid = {p.get("username"): p.get("id") for p in players} if ok else {}
    uid = {}
    if ok:
        for email, username in [(carlos, "Carlos Perez"), (andrea, "Andrea Gomez"),
                                 (luis, "Luis Martinez"), (marcela, "Marcela Torres"),
                                 (pedro, "Pedro Ramirez"), (ADMIN_EMAIL, "Admin General")]:
            uid[email] = pid.get(username)

    def member(email, slot_index, position="MF", squad="STARTER"):
        return {"userId": uid.get(email), "squadRole": squad, "position": position,
                "slotIndex": slot_index, "captain": False}

    # busqueda desde andrea (para su propio equipo): excluye a andrea, incluye a carlos
    st, players2 = call("GET", "/api/teams/players?query=&city=Barranquilla", token=t_andrea)
    pid2 = {p.get("username"): p.get("id") for p in players2} if st == 200 else {}
    uid2 = {email: pid2.get(username) for email, username in [
        (carlos, "Carlos Perez"), (andrea, "Andrea Gomez"),
        (luis, "Luis Martinez"), (marcela, "Marcela Torres"),
        (pedro, "Pedro Ramirez"), (ADMIN_EMAIL, "Admin General")]}

    def member2(email, slot_index, position="MF", squad="STARTER"):
        return {"userId": uid2.get(email), "squadRole": squad, "position": position,
                "slotIndex": slot_index, "captain": False}

    team_name = f"Los Tiburones FC {run_id}"
    team_payload_carlos = {
        "name": team_name, "description": "Equipo de prueba del barrio norte",
        "city": "Barranquilla", "shieldUrl": None,
        "primaryColor": "#1B5E5A", "secondaryColor": "#F4E9DA",
        "format": "FIVE", "formation": "1-2-1",
        # el buscador excluye al usuario actual: el dueño se auto-agrega al equipo
        "members": [
            member(andrea, 0, "GK"), member(luis, 1, "DF"),
            member(marcela, 2, "MF"), member(pedro, 3, "FW"),
        ],
    }
    st, team = call("POST", "/api/teams", token=t_carlos, body=team_payload_carlos)
    ok = st in (200, 201) and isinstance(team, dict) and team.get("id")
    check("F2 Crear equipo FIVE 1-2-1", ok, f"status={st} body={str(team)[:300]}")
    team_carlos_id = team.get("id") if ok else None
    if ok:
        caps = [m for m in team.get("members", []) if m.get("captain")]
        check("F2b Capitan = dueño del equipo", len(caps) == 1 and caps[0].get("userId") == ids[carlos], f"caps={caps}")
        chat_room_id = team.get("chatRoomId") or (team.get("chatRoom") or {}).get("id")

    st, team_bad = call("POST", "/api/teams", token=t_luis, body={
        "name": "Malos Formatos", "format": "FIVE", "formation": "4-3-3",
        "members": [member(luis, 0, "GK")]})
    check("F3 Formacion invalida -> 400", st == 400, f"status={st} body={str(team_bad)[:150]}")

    st, team2 = call("POST", "/api/teams", token=t_andrea, body={
        "name": f"Las Aguilas {run_id}", "city": "Barranquilla", "format": "FIVE", "formation": "2-1-1",
        # plantilla titular completa (5) para poder inscribirse en torneos;
        # andrea (owner) se auto-agrega como suplente capitana.
        # La busqueda de jugadores excluye al usuario que consulta: se hace como andrea.
        "members": [
            member2(luis, 0, "GK"), member2(marcela, 1, "DF"),
            member2(pedro, 2, "MF"), member2(carlos, 3, "MF"),
            member2(ADMIN_EMAIL, 4, "FW"),
        ]})
    check("F4 Crear segundo equipo (owner auto-agregado)", st in (200, 201), f"status={st} body={str(team2)[:200]}")
    team_andrea_id = team2.get("id") if isinstance(team2, dict) else None

    st, mine = call("GET", "/api/teams/mine", token=t_carlos)
    check("F5 GET /teams/mine", st == 200 and isinstance(mine, list) and any(t.get("id") == team_carlos_id for t in mine), f"status={st}")

    st, body = call("GET", f"/api/teams/{team_carlos_id}", token=t_marcela)
    check("F6 Detalle equipo (miembro)", st == 200 and body.get("id") == team_carlos_id, f"status={st}")

    st, notifs = call("GET", "/api/notifications", token=t_luis)
    ok = st == 200 and isinstance(notifs, list) and any(team_name in json.dumps(n) for n in notifs)
    check("F7 Notificacion TEAM_ADDED a miembros", ok, f"status={st}")

    # ---------- G. CHAT ----------
    st, room = call("POST", "/api/chat/rooms", token=t_carlos, body={
        "name": "Partido viernes", "category": "Amistoso",
        "memberIds": [ids[andrea], ids[luis]]})
    ok = st in (200, 201) and isinstance(room, dict) and room.get("roomId", room.get("id"))
    check("G1 Crear sala de chat", ok, f"status={st} body={str(room)[:250]}")
    chat_room = room.get("roomId", room.get("id")) if ok else None

    st, convs = call("GET", "/api/chat/conversations", token=t_andrea)
    check("G2 Conversaciones de andrea", st == 200 and isinstance(convs, list) and any((c.get("roomId") or c.get("id")) == chat_room for c in convs), f"status={st} n={len(convs) if isinstance(convs, list) else convs}")

    st, msg = call("POST", f"/api/chat/rooms/{chat_room}/messages", token=t_carlos, body={
        "content": "Hola equipo, el viernes a las 7?", "messageType": "TEXT"})
    check("G3 Enviar mensaje (carlos)", st in (200, 201), f"status={st} body={str(msg)[:150]}")

    st, msg2 = call("POST", f"/api/chat/rooms/{chat_room}/messages", token=t_andrea, body={
        "content": "Listo, ahi estare", "messageType": "TEXT"})
    check("G4 Enviar mensaje (andrea)", st in (200, 201), f"status={st} body={str(msg2)[:150]}")

    st, page = call("GET", f"/api/chat/rooms/{chat_room}/messages?size=50", token=t_carlos)
    ok = st == 200 and isinstance(page, dict)
    msgs = page.get("messages", []) if ok else []
    check("G5 Historial de mensajes", ok and len(msgs) >= 2, f"status={st} n={len(msgs)}")

    st, _ = call("PUT", f"/api/chat/rooms/{chat_room}/read", token=t_andrea, body={"lastMessageId": None})
    check("G6 Marcar sala leida", st == 204, f"status={st}")

    st, details = call("GET", f"/api/chat/rooms/{chat_room}", token=t_luis)
    check("G7 Detalles de sala (miembro)", st == 200 and isinstance(details, dict), f"status={st}")

    st, body = call("GET", f"/api/chat/rooms/{chat_room}/messages", token=t_marcela)
    check("G8 Mensajes por no-miembro -> 403", st == 403, f"status={st} (BUG si 200)")

    # chat de equipo (creado automaticamente)
    st, convs = call("GET", "/api/chat/conversations", token=t_carlos)
    team_rooms = [c for c in (convs or []) if team_name in str(c.get("name", ""))]
    check("G9 Sala de chat de equipo creada", st == 200 and len(team_rooms) >= 1, f"status={st} rooms={json.dumps(convs)[:250] if st==200 else convs}")

    # ---------- H. TORNEOS ----------
    start = (datetime.now() + timedelta(days=10)).strftime("%Y-%m-%dT10:00:00")
    deadline = (datetime.now() + timedelta(days=6)).strftime("%Y-%m-%dT23:59:59")
    tour_payload = {
        "name": f"Copa Barrial PieJuega {run_id}", "description": "Torneo de prueba para 8 equipos",
        "rules": "Partidos de 20 minutos, fair play", "format": "FIVE",
        "fieldId": fid1, "startsAt": start, "registrationDeadline": deadline,
        "maxTeams": 8, "entryFee": 150000, "prize": "$1.000.000 + trofeo",
    }
    st, tour = call("POST", "/api/tournaments", token=t_carlos, body=tour_payload)
    ok = st in (200, 201) and isinstance(tour, dict) and tour.get("status") == "PENDING_APPROVAL"
    check("H1 Crear torneo -> PENDING_APPROVAL", ok, f"status={st} body={str(tour)[:250]}")
    tour_id = tour.get("id") if ok else None

    st, body = call("POST", "/api/tournaments", token=t_marcela, body={**tour_payload,
        "name": f"Copa Rechazada {run_id}", "startsAt": (datetime.now() + timedelta(days=12)).strftime("%Y-%m-%dT10:00:00")})
    tour_reject_id = body.get("id") if isinstance(body, dict) else None
    check("H2 Crear segundo torneo", st in (200, 201), f"status={st}")

    st, listing = call("GET", "/api/tournaments", token=t_carlos)
    visible = any(t.get("id") == tour_id for t in (listing or [])) if isinstance(listing, list) else None
    check("H3 Torneo pendiente NO visible en lista publica", visible is False, f"visible={visible} n={len(listing) if isinstance(listing, list) else listing}")

    st, pending = call("GET", "/api/admin/tournaments?status=PENDING_APPROVAL", token=t_admin)
    ok = st == 200 and isinstance(pending, list) and any(t.get("id") == tour_id for t in pending)
    check("H4 Admin ve torneos pendientes", ok, f"status={st} n={len(pending) if isinstance(pending, list) else pending}")

    st, body = call("PATCH", f"/api/admin/tournaments/{tour_id}/approve", token=t_admin)
    check("H5 Admin aprueba torneo", st == 200 and body.get("status") == "OPEN_REGISTRATION", f"status={st} body={str(body)[:150]}")

    st, body = call("PATCH", f"/api/admin/tournaments/{tour_reject_id}/reject", token=t_admin,
                    body={"reason": "Fechas coinciden con otro torneo del complejo"})
    check("H6 Admin rechaza torneo con motivo", st == 200 and body.get("status") == "REJECTED", f"status={st} body={str(body)[:150]}")

    st, body = call("POST", f"/api/tournaments/{tour_id}/teams", token=t_andrea, body={"teamId": team_andrea_id})
    ok = st == 200 and isinstance(body, dict) and str(team_andrea_id) in json.dumps(body)
    check("H7 Inscribir equipo en torneo", ok, f"status={st} body={str(body)[:250]}")

    st, body = call("POST", f"/api/tournaments/{tour_id}/teams", token=t_andrea, body={"teamId": team_andrea_id})
    check("H8 Inscripcion duplicada -> 409", st == 409, f"status={st}")

    st, body = call("POST", f"/api/tournaments/{tour_id}/teams", token=t_marcela, body={"teamId": team_carlos_id})
    check("H9 Inscripcion con equipo ajeno -> 403", st == 403, f"status={st} (verificar regla) body={str(body)[:150]}")

    st, mine = call("GET", "/api/tournaments/mine", token=t_carlos)
    check("H10 GET /tournaments/mine (creador)", st == 200 and isinstance(mine, list) and any(t.get("id") == tour_id for t in mine), f"status={st}")

    st, body = call("DELETE", f"/api/tournaments/{tour_id}/teams/{team_andrea_id}", token=t_andrea)
    check("H11 Retirar equipo del torneo", st == 200, f"status={st} body={str(body)[:150]}")

    # ---------- I. ADMIN: CANCHAS ----------
    field_payload = {
        "name": f"Cancha Prueba Admin {run_id}", "address": "Calle 99 #10-20", "cityCode": "08001",
        "latitude": 10.98, "longitude": -74.81,
        "description": "Cancha creada por la suite de pruebas", "imageUrl": None,
        "format": "SEVEN", "pricePerHour": 120000,
        "openingTime": "15:00:00", "closingTime": "22:00:00",
        "slotDurationMinutes": 60,
        "features": ["Iluminacion", "Parqueadero"],
        "openDays": ["MONDAY", "WEDNESDAY", "FRIDAY", "SATURDAY", "SUNDAY"],
        "active": True,
    }
    st, nf = call("POST", "/api/admin/fields", token=t_admin, body=field_payload)
    ok = st in (200, 201) and isinstance(nf, dict) and nf.get("id")
    check("I1 Admin crea cancha", ok, f"status={st} body={str(nf)[:250]}")
    new_fid = nf.get("id") if ok else None

    st, body = call("GET", "/api/fields?city=Barranquilla", token=t_marcela)
    ok = isinstance(body, list) and any(f.get("id") == new_fid for f in body)
    check("I2 Nueva cancha visible para usuarios", ok, f"status={st}")

    st, body = call("PUT", f"/api/admin/fields/{new_fid}", token=t_admin, body={**field_payload, "pricePerHour": 135000})
    check("I3 Admin actualiza cancha", st == 200 and str(body.get("pricePerHour", "")).rstrip("0.").endswith(("135", "13")), f"status={st} price={body.get('pricePerHour') if isinstance(body, dict) else body}")

    st, body = call("PATCH", f"/api/admin/fields/{new_fid}/active", token=t_admin, body={"active": False})
    ok = st == 200
    if not ok:
        st2, body2 = call("PATCH", f"/api/admin/fields/{new_fid}/active?active=false", token=t_admin)
        ok, st, body = st2 == 200, st2, body2
    check("I4 Admin desactiva cancha", ok, f"status={st} body={str(body)[:150]}")
    st, body = call("GET", "/api/fields?city=Barranquilla", token=t_marcela)
    hidden = not any(f.get("id") == new_fid for f in (body or []))
    check("I5 Cancha inactiva oculta para usuarios", hidden, f"status={st}")
    # reactivar para dejar datos coherentes
    call("PATCH", f"/api/admin/fields/{new_fid}/active", token=t_admin, body={"active": True})

    st, body = call("POST", "/api/admin/fields", token=t_carlos, body=field_payload)
    check("I6 Crear cancha como usuario normal -> 403", st == 403, f"status={st} (BUG si 200)")

    # ---------- J. MEDIA ----------
    # PNG 1x1
    png = base64.b64decode("iVBORw0KGgoAAAANSUhEUgAAAAEAAAABCAYAAAAfFcSJAAAADUlEQVR42mP8z8BQDwAEhQGAhKmMIQAAAABJRU5ErkJggg==")
    boundary = "----SmokeTestBoundary" + uuid.uuid4().hex[:8]
    mp = io.BytesIO()
    mp.write(f"--{boundary}\r\n".encode())
    mp.write('Content-Disposition: form-data; name="file"; filename="pixel.png"\r\n'.encode())
    mp.write("Content-Type: image/png\r\n\r\n".encode())
    mp.write(png)
    mp.write(f"\r\n--{boundary}\r\n".encode())
    mp.write('Content-Disposition: form-data; name="folder"\r\n\r\n'.encode())
    mp.write(b"field_images")
    mp.write(f"\r\n--{boundary}--\r\n".encode())
    mp_body = mp.getvalue()
    hdrs = {"Content-Type": f"multipart/form-data; boundary={boundary}"}

    st, body = call("POST", "/api/media/images", token=t_admin, raw_body=mp_body, headers=hdrs)
    if st == 503:
        skip("J1 Admin sube imagen (field_images)", "503: Cloudinary sin configurar en el entorno local")
    else:
        check("J1 Admin sube imagen (field_images)", st == 200, f"status={st} body={str(body)[:200]}")

    mp2 = io.BytesIO()
    mp2.write(f"--{boundary}\r\n".encode())
    mp2.write('Content-Disposition: form-data; name="file"; filename="pixel.png"\r\n'.encode())
    mp2.write("Content-Type: image/png\r\n\r\n".encode())
    mp2.write(png)
    mp2.write(f"\r\n--{boundary}\r\n".encode())
    mp2.write('Content-Disposition: form-data; name="folder"\r\n\r\n'.encode())
    mp2.write(b"field_images")
    mp2.write(f"\r\n--{boundary}--\r\n".encode())
    st, body = call("POST", "/api/media/images", token=t_carlos, raw_body=mp2.getvalue(), headers=hdrs)
    if st == 503:
        skip("J2 Usuario normal sube imagen de cancha -> 403", "503: Cloudinary sin configurar; el check de rol requiere storage activo")
    else:
        check("J2 Usuario normal sube imagen de cancha -> 403", st == 403, f"status={st} (BUG si 200)")

    # ---------- K. RECUPERACION DE PASSWORD ----------
    st, body = call("POST", "/api/auth/recovery", body={"email": carlos})
    check("K1 Solicitar recuperacion (/recovery)", st == 200, f"status={st} body={str(body)[:200]}")
    st, body = call("POST", "/api/auth/recovety", body={"email": carlos})
    check("K1b Alias antiguo /recovety sigue activo", st == 200, f"status={st}")
    # El codigo se guarda HASHEADO y no hay SMTP local -> no se puede completar el
    # flujo verify/reset en este entorno; se prueba solo la respuesta del endpoint.
    skip("K2-K4 Verificar codigo + reset", "requiere un SMTP real para recibir el codigo (el codigo se guarda hasheado)")
    # Sin idToken de Firebase verificado debe rechazarse (no se puede resetear
    # una cuenta conociendo solo el telefono)
    st, body = call("POST", "/api/auth/resetByPhone", body={
        "phone": "573001111101", "newPassword": "User1234*", "confirmNewPassword": "User1234*"})
    check("K5 resetByPhone sin verificacion -> rechazado", st == 400, f"status={st} (CRITICO si 200/204)")
    st, body = call("POST", "/api/auth/resetByPhone", body={
        "phone": "573001111101", "newPassword": "User1234*", "confirmNewPassword": "User1234*",
        "idToken": "token-falso"})
    check("K6 resetByPhone con token falso -> rechazado", st == 400, f"status={st} (CRITICO si 200/204)")

    # ---------- Resumen ----------
    print()
    print("=" * 70)
    total = len(results)
    passed = sum(1 for _, ok, _ in results if ok)
    print(f"RESULTADO: {passed}/{total} pruebas pasaron")
    if failures:
        print("\nFallos:")
        for name, detail in failures:
            print(f"  - {name}: {detail}")

    # guardar tokens para pruebas manuales
    tok_path = "scripts/.smoke_tokens.json"
    with open(tok_path, "w", encoding="utf-8") as f:
        json.dump({"admin": t_admin, "carlos": t_carlos, "andrea": t_andrea,
                   "luis": t_luis, "marcela": t_marcela,
                   "ids": {k: v for k, v in ids.items()},
                   "team_carlos": team_carlos_id, "team_andrea": team_andrea_id,
                   "chat_room": chat_room, "tournament": tour_id,
                   "field_new": new_fid}, f, ensure_ascii=False, indent=2)
    print(f"\nTokens guardados en {tok_path}")


if __name__ == "__main__":
    main()
