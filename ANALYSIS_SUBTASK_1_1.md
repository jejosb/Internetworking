# Subtask 1.1: Codeanalyse - Cookie-Request Ablauf

## Protokoll-Stack Initialisierung

### Im CPClient:
```
1. PhyProtocol phy = new PhyProtocol(clientPort)
   - Erstellt UDP-Socket auf Port des Clients (z.B. 5000)
   
2. CPProtocol cp = new CPProtocol(remoteHost, remotePort, phy)
   - role = CLIENT
   - cookie = -1 (ungültig)
   - PhyConfigCommandServer zeigt auf Command-Server (localhost:2000)
   
3. cp.setCookieServer(remoteHost, remotePort)
   - PhyConfigCookieServer zeigt auf Cookie-Server (localhost:3000)
```

---

## Cookie-Request-Ablauf (requestCookie())

### Detaillierter Workflow:

```
CPClient.send(sentence, null)
    ↓
CPProtocol.send() prüft: if (cookie < 0)
    ↓
CPProtocol.requestCookie() wird aufgerufen
    ↓
1. CPCookieRequestMsg reqMsg = new CPCookieRequestMsg()
   - reqMsg.create(null)
   - Erzeugt Nachricht: "cp cookie_request"
   
2. while(waitForResp && count < 3) {
   
   a) PhyProto.send(reqMsg.getDataBytes(), PhyConfigCookieServer)
      → UDP-Packet an Cookie-Server (localhost:3000)
      → Paket enthält: "cp cookie_request"
      
   b) try {
        Msg in = PhyProto.receive(CP_TIMEOUT = 2000ms)
        
        i)   Socket wartet max. 2 Sekunden auf Response
        
        ii)  CPMsg.parse(in.getData()) wird aufgerufen
             - Identifiziert als CPCookieResponseMsg
             - Parsed: "cp cookie_response ACK <cookie_value>"
             
        iii) if (resMsg instanceof CPCookieResponseMsg)
             → waitForResp = false (Schleife bricht ab)
        
      } catch (SocketTimeoutException) {
        count += 1  // Zähler erhöhen, neuer Versuch
      }
   }
   
3. if (count == 3)
   → throw CookieRequestException()
   
4. if (!((CPCookieResponseMsg) resMsg).getSuccess())
   → throw CookieRequestException()
   
5. this.cookie = ((CPCookieResponseMsg) resMsg).getCookie()
   → Cookie ist jetzt verfügbar!
```

---

## Sequenzdiagramm: Cookie-Request Flow (KORRIGIERT)

```
CPClient    CPProto(Client)   PhyProto(Client)  UDP   CPCookieServer   CPProto(Server)
   |              |                |            |         |                |
   |--send(cmd)-->|                |            |         |                |
   |              |                |            |         |                |
   |              |--check cookie--|            |         |                |
   |              |  (if < 0)      |            |         |                |
   |              |                |            |         |                |
   |              |--requestCookie()            |         |                |
   |              |                |            |         |                |
   |              |--create CPCookieRequestMsg  |         |                |
   |              |                |            |         |                |
   |              |--PhyProto.send->|--UDP----->|         |                |
   |              |                | "cp        |         |                |
   |              |                | cookie_    |         |                |
   |              |                | request"   |         |                |
   |              |                |            |--main()->|                |
   |              |                |            |         |--cp.receive()-->
   |              |                |            |         |                |
   |              |                |            |         |--PhyProto.receive()  
   |              |                |            |         |--CPMsg.parse() |
   |              |                |            |         |--cookie_process|
   |              |                |            |         |  (generates    |
   |              |                |            |         |   random cookie|
   |              |                |            |         |   creates      |
   |              |                |            |         |   response)    |
   |              |                |            |         |                |
   |              |                |            |         |--PhyProto.send()|
   |              |                |<--UDP------|<--      |  response      |
   |              |                |   "cp      |         |                |
   |              |                |   cookie_  |         |                |
   |              |                |   response |         |                |
   |              |<--PhyProto.receive(2000ms)  |         |                |
   |              |   ACK xxx"     |            |         |                |
   |              |                |            |         |                |
   |              |--CPMsg.parse() |            |         |                |
   |              |  ->CPCookieResponseMsg     |         |                |
   |              |                |            |         |                |
   |              |--extract cookie|            |         |                |
   |              |  this.cookie=val           |         |                |
   |              |                |            |         |                |
   |<--return OK--|                |            |         |                |
   |              |                |            |         |                |
```

**Wichtige Korrekturen zum korrigierten Diagramm:**

1. **Client-Seite (Spalten 1-3)**
   - CPClient: Benutzer-Interface (send-Aufruf)
   - CPProto(Client): Cookie-Validierung, requestCookie()-Logik, Parse-Response
   - PhyProto(Client): UDP-Sende/Empfang

2. **Network Layer (Spalte 4)**
   - UDP: Bidirektionale Kommunikation zwischen Client und Server

3. **Server-Seite (Spalten 5-6)**
   - CPCookieServer: Applikation (Mainloop)
   - CPProto(Server): cp.receive() → parse() → cookie_process()
   - Cookie-Generierung innerhalb von cookie_process()

4. **Ablauf-Korrekturen:**
   - ✅ RequestMsg wird vom Client versendet (UDP)
   - ✅ Server empfängt in Mainloop → cp.receive()
   - ✅ cp.receive() → PhyProto.receive() → CPMsg.parse()
   - ✅ CPMsg.parse() identifies CPCookieRequestMsg → routes to cookie_process()
   - ✅ cookie_process() generates random cookie und erstellt response
   - ✅ Response wird via PhyProto.send() zurück an Client gesendet
   - ✅ Client empfängt Response mit 2000ms Timeout
   - ✅ Client parst Response → extrahiert cookie value
   - ✅ Cookie wird in this.cookie gespeichert

5. **KORRIGIERTE FEHLER des ursprünglichen Diagramms:**
   - ❌ Zu viele Akteure (verwirrend)
   - ❌ CPProtocol(Server) als Aktor falsch positioniert
   - ❌ Unklar, wo parse() und cookie_process() stattfinden
   - ❌ Fehler bei Timeout-Angabe (hier: 2000ms für Cookie-Request)

---

## Cookie-Server Verarbeitung (cookie_process)

```
CPCookieServer.receive()
    ↓
PhyProtocol.receive()
    ↓
CPMsg.parse() → CPCookieRequestMsg
    ↓
CPProtocol.cookie_process(CPCookieRequestMsg)
    ↓
1. Generate random cookie: int newCookie = rnd.nextInt(1000000)

2. Create response: 
   CPCookieResponseMsg response = new CPCookieResponseMsg(true)
   response.create(String.valueOf(newCookie))
   → Nachricht: "cp cookie_response ACK <newCookie>"

3. Send back:
   PhyProto.send(response.getDataBytes(), cpmIn.getConfiguration())
   → UDP-Response an Client zurück
```

---

## Exception-Handling

### Erfolgsfall:
- CPCookieRequestMsg wird gesendet
- CPCookieResponseMsg mit "ACK" empfangen
- Cookie wird extrahiert und gespeichert
- `this.cookie` ≥ 0

### Fehlerfälle:
| Fall | Auslöser | Exception |
|------|----------|-----------|
| 3x Timeout | Kein Response nach 2sec | CookieRequestException |
| NAK Response | Server lehnt ab ("NAK") | CookieRequestException |
| Parse Error | Ungültiges Message-Format | Ignoriert (catch-Block) |

---

## Message-Format

### CPCookieRequestMsg:
```
Header: "cp cookie_request"
```

### CPCookieResponseMsg (Success):
```
Header: "cp cookie_response ACK <cookie_value>"
Beispiel: "cp cookie_response ACK 654321"
```

### CPCookieResponseMsg (Failure):
```
Header: "cp cookie_response NAK <reason>"
Beispiel: "cp cookie_response NAK Duplicate client"
```

---

## Wichtige Erkenntnisse

1. **Timeout-Mechanismus**: 3 Versuche mit je 2000ms Timeout
2. **Protokoll-Stack**: Client → CP Layer → Phy Layer → UDP
3. **Zustandsübergang**: 
   - Anfang: `cookie = -1` (ungültig)
   - Nach Erfolg: `cookie = <random_value>` (gültig)
4. **Exception-Propagation**: 
   - CookieRequestException wird geworfen und bubbled up zum Client
   - Client sollte dann erneut versuchen
5. **Parse-Sicherheit**: 
   - Ungültige Messages werden verworfen (continue in Schleife)
   - Schützt vor Injection/Corruption
