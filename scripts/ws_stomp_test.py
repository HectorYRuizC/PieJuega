# -*- coding: utf-8 -*-
"""Prueba E2E de chat en tiempo real: carlos se suscribe por WebSocket a la sala
del equipo y andrea publica un mensaje via REST -> carlos debe recibirlo por WS."""
import base64, json, os, socket, struct, time, urllib.request, urllib.error

BASE = "http://localhost:8080"

def api(method, path, token=None, body=None):
    headers = {"Content-Type": "application/json"}
    if token:
        headers["Authorization"] = "Bearer " + token
    data = json.dumps(body).encode() if body else None
    req = urllib.request.Request(BASE + path, data=data, headers=headers, method=method)
    try:
        with urllib.request.urlopen(req, timeout=15) as r:
            return r.status, r.read()
    except urllib.error.HTTPError as e:
        return e.code, e.read()

def http_upgrade(sock, host, port, path):
    key = base64.b64encode(os.urandom(16)).decode()
    req = (f"GET {path} HTTP/1.1\r\nHost: {host}:{port}\r\nUpgrade: websocket\r\n"
           f"Connection: Upgrade\r\nSec-WebSocket-Key: {key}\r\nSec-WebSocket-Version: 13\r\n\r\n")
    sock.sendall(req.encode())
    resp = b""
    while b"\r\n\r\n" not in resp:
        chunk = sock.recv(4096)
        if not chunk:
            break
        resp += chunk
    return resp.split(b"\r\n")[0].decode()

def send_frame(sock, payload, opcode=1):
    data = payload.encode()
    mask = os.urandom(4)
    header = bytes([0x80 | opcode])
    length = len(data)
    if length < 126:
        header += bytes([0x80 | length])
    elif length < 65536:
        header += bytes([0x80 | 126]) + struct.pack(">H", length)
    else:
        header += bytes([0x80 | 127]) + struct.pack(">Q", length)
    masked = bytes(b ^ mask[i % 4] for i, b in enumerate(data))
    sock.sendall(header + mask + masked)

def recv_frame(sock, timeout=5):
    sock.settimeout(timeout)
    def read_exact(n):
        buf = b""
        while len(buf) < n:
            chunk = sock.recv(n - len(buf))
            if not chunk:
                raise ConnectionError("cerrado")
            buf += chunk
        return buf
    b1, b2 = read_exact(2)
    length = b2 & 0x7F
    if length == 126:
        length = struct.unpack(">H", read_exact(2))[0]
    elif length == 127:
        length = struct.unpack(">Q", read_exact(8))[0]
    return read_exact(length).decode("utf-8", "replace") if length else b""

_, b = api("POST", "/api/auth/login", body={"identifier": "carlos@piejuega.dev", "password": "User1234*"})
carlos = json.loads(b)
_, b = api("POST", "/api/auth/login", body={"identifier": "andrea@piejuega.dev", "password": "User1234*"})
andrea = json.loads(b)

# sala donde ambos son miembros (equipo Las Aguilas)
_, b = api("GET", "/api/chat/conversations", token=carlos["accessToken"])
convs_c = json.loads(b)
_, b = api("GET", "/api/chat/conversations", token=andrea["accessToken"])
convs_a = json.loads(b)
room_ids_c = {c["roomId"] for c in convs_c}
room_ids_a = {c["roomId"] for c in convs_a}
shared = sorted(room_ids_c & room_ids_a)
room = shared[0]
print(f"sala compartida: {room}")

host, port = "localhost", 8080
s = socket.create_connection((host, port), timeout=10)
print("upgrade:", http_upgrade(s, host, port, "/ws"))

send_frame(s, f"CONNECT\naccept-version:1.2\nhost:localhost\nAuthorization:Bearer {carlos['accessToken']}\n\n\x00")
frame = recv_frame(s, timeout=5)
print("CONNECT ->", frame.split("\n")[0])
assert frame.startswith("CONNECTED"), frame

# carlos se suscribe a la sala compartida
send_frame(s, f"SUBSCRIBE\nid:sub-0\ndestination:/topic/chat/rooms/{room}\n\n\x00")

# andrea publica un mensaje por REST
marker = "mensaje-rt-" + str(int(time.time()))
st, b = api("POST", f"/api/chat/rooms/{room}/messages", token=andrea["accessToken"],
            body={"content": marker, "messageType": "TEXT"})
print("REST mensaje andrea ->", st)
assert st in (200, 201)

# carlos debe recibir el MESSAGE por el websocket
received = None
deadline = time.time() + 8
while time.time() < deadline and received is None:
    try:
        frame = recv_frame(s, timeout=3)
    except socket.timeout:
        break
    if frame.startswith("MESSAGE") and marker in frame:
        received = frame
if received:
    print("WS MESSAGE recibido en tiempo real:", marker)
else:
    print("WS MESSAGE NO recibido")

# notificaciones personales: andrea crea equipo incluyendo a carlos -> TEAM_ADDED
# (se prueba ANTES de la suscripcion rechazada: el ERROR cierra la sesion STOMP)
send_frame(s, f"SUBSCRIBE\nid:sub-2\ndestination:/topic/notifications/users/{carlos['user']['id']}\n\n\x00")
_, players = api("GET", "/api/teams/players?query=&city=Barranquilla", token=andrea["accessToken"])
pid = {p["username"]: p["id"] for p in json.loads(players)}
carlos_uid = pid.get("Carlos Perez")
if carlos_uid:
    suffix = str(int(time.time()))
    st, _ = api("POST", "/api/teams", token=andrea["accessToken"], body={
        "name": "RT " + suffix, "city": "Barranquilla", "format": "FIVE",
        "formation": "1-2-1",
        "members": [{"userId": carlos_uid, "squadRole": "STARTER",
                     "position": "GK", "slotIndex": 0, "captain": False}]})
    print("crear equipo (dispara TEAM_ADDED) ->", st)
else:
    print("no se encontro a carlos en la busqueda de jugadores")

notif_ok = None
deadline = time.time() + 6
while time.time() < deadline and notif_ok is None:
    try:
        frame = recv_frame(s, timeout=3)
    except (socket.timeout, ConnectionError):
        break
    if frame.startswith("ERROR"):
        notif_ok = False
        break
    if frame.startswith("MESSAGE") and "/topic/notifications/users/" + str(carlos["user"]["id"]) in frame:
        notif_ok = True
print("topic notificaciones personales ->", "RECIBE eventos" if notif_ok else ("RECHAZADO" if notif_ok is False else "sin eventos de prueba"))

# suscripcion no autorizada -> ERROR (sala existente donde carlos NO es miembro);
# va al final porque un ERROR termina la sesion STOMP
import subprocess
out = subprocess.run(["docker", "exec", "piejuega_postgres", "psql", "-U", "piejuega",
                      "-d", "piejuega_db", "-t", "-A", "-c",
                      "SELECT id FROM chat_rooms ORDER BY id;"],
                     capture_output=True, text=True).stdout.split()
foreign = next(int(rid) for rid in out if int(rid) not in room_ids_c)
print(f"sala ajena elegida: {foreign}")
send_frame(s, f"SUBSCRIBE\nid:sub-1\ndestination:/topic/chat/rooms/{foreign}\n\n\x00")
unauth = None
try:
    frame = recv_frame(s, timeout=4)
    if frame.startswith("ERROR") and "No tienes acceso" in frame:
        unauth = True
except (socket.timeout, ConnectionError):
    pass
print("SUBSCRIBE sala ajena ->", "RECHAZADA correctamente" if unauth else "sin ERROR")

send_frame(s, "DISCONNECT\n\n\x00")
s.close()

print("\nRESULTADO WS:", "OK" if (received and unauth and notif_ok) else "FALLA")
raise SystemExit(0 if (received and unauth and notif_ok) else 1)
