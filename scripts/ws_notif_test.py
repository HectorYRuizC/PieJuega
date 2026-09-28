# -*- coding: utf-8 -*-
"""Prueba aislada: topic personal de notificaciones por WebSocket."""
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
    else:
        header += bytes([0x80 | 126]) + struct.pack(">H", length)
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
    return read_exact(length).decode("utf-8", "replace") if length else b""

_, b = api("POST", "/api/auth/login", body={"identifier": "luis@piejuega.dev", "password": "User1234*"})
luis = json.loads(b)
_, b = api("POST", "/api/auth/login", body={"identifier": "marcela@piejuega.dev", "password": "User1234*"})
marcela = json.loads(b)

host, port = "localhost", 8080
s = socket.create_connection((host, port), timeout=10)
print("upgrade:", http_upgrade(s, host, port, "/ws"))
send_frame(s, f"CONNECT\naccept-version:1.2\nhost:localhost\nAuthorization:Bearer {luis['accessToken']}\n\n\x00")
print("CONNECT ->", recv_frame(s, timeout=5).split("\n")[0])

uid = luis["user"]["id"]
send_frame(s, f"SUBSCRIBE\nid:sub-notif\ndestination:/topic/notifications/users/{uid}\n\n\x00")
print(f"SUSCRITO a /topic/notifications/users/{uid}")

# marcela crea un equipo incluyendo a luis -> TEAM_ADDED para luis
_, players = api("GET", "/api/teams/players?query=&city=Barranquilla", token=marcela["accessToken"])
pid = {p["username"]: p["id"] for p in json.loads(players)}
st, _ = api("POST", "/api/teams", token=marcela["accessToken"], body={
    "name": "NotifTest " + str(int(time.time())), "city": "Barranquilla",
    "format": "FIVE", "formation": "1-2-1",
    "members": [{"userId": pid.get("Luis Martinez"), "squadRole": "STARTER",
                 "position": "GK", "slotIndex": 0, "captain": False}]})
print("crear equipo ->", st)

got = None
deadline = time.time() + 8
while time.time() < deadline and got is None:
    try:
        frame = recv_frame(s, timeout=3)
    except socket.timeout:
        break
    except ConnectionError as ce:
        print("conexion cerrada:", ce)
        break
    print("frame:", frame.split("\n")[0], "| dest:", [l for l in frame.split("\n") if l.startswith("destination")])
    if frame.startswith("MESSAGE") and "TEAM_ADDED" in frame:
        got = frame
print("\nTEAM_ADDED por websocket:", "RECIBIDO" if got else "NO RECIBIDO")
send_frame(s, "DISCONNECT\n\n\x00")
s.close()
raise SystemExit(0 if got else 1)
