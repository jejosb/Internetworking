# Aufgabe 1

## Teilaufgabe 1.1: Code-Analyse (5 Punkte)
Analysiere den bereitgestellten Quellcode, wie ein Client ein Cookie anfordert und die Antwort erhält.
Du kannst einen Debugger verwenden, um die Ausführung des Codes direkt zu beobachten. Du musst die Initialisierung des Protokollstapels und die Cookie-Anforderungsmethode dokumentieren, bis ein Cookie für den Client verfügbar ist oder die Anfrage abgelehnt wurde. Dokumentiere deine Erkenntnisse und visualisiere sie in einem Sequenzdiagramm. Verwende Klassennamen als Akteure.

## Teilaufgabe 1.2: Vervollständigung der Client-Seite (5 Punkte)
Sobald der Client ein gültiges Cookie hat, kann er Befehlsnachrichten an den Server senden. Implementiere alle Funktionalitäten des Clients, um eine Print-Befehlsnachricht an den Server zu senden und die Antwort vom Server zu empfangen. Code-Kommentare enthalten Hinweise darauf, was wo zu implementieren ist.

1. Vervollständigung der send-Methode in CPProtocol.
    a. Erstelle ein Befehlsnachrichtenobjekt (siehe unten).
    b. Sende den Befehl an den Command-Server. Die PHY-Konfiguration ist als Attribut in CPProtocol gespeichert.

2. Implementierung der receive-Methode in CPProtocol.
    a. Für jede gesendete Befehlsnachricht wartet der Client maximal drei Sekunden auf eine Antwort vom Command-Server. Rufe die entsprechende receive-Methode der PHY-Schicht auf.
    b. Rufe den Nachrichten-Parser auf, um ein CP-Nachrichtenobjekt aus dem empfangenen String-Objekt gemäß der Protokollspezifikation zu erstellen. Wenn der Parser eine Exception wirft, soll die Nachricht verworfen werden.
    Hinweis: Du kannst den receive-Teil der requestCookie()-Methode als Vorlage verwenden.
    c. Überprüfe, dass die Antwort zur Befehlsnachricht passt, indem du die Nachrichten-ID der empfangenen Nachricht mit der ID der gesendeten Nachricht vergleichst.
    d. Überprüfe, ob der Command-Server den Befehl akzeptiert hat.
    e. Gib dem Client eine entsprechende Rückmeldung.

3. Erstelle und implementiere Nachrichtenklassen zum Erstellen und Parsen von Befehls- und Befehlsantwortnachrichten.
    a. Um die CRC-Prüfsumme zu berechnen, verwende die CRC32-Klasse von Java (https://docs.oracle.com/javase/9/docs/api/java/util/zip/CRC32.html)
    b. Füge Getter/Setter-Methoden hinzu, wie sie für andere Aufgaben benötigt werden.

### Erweiterte Funktion für Teilaufgabe 2
Falls du mit diesem Teil früh fertig bist und deine Implementierung weiter verbessern möchtest, kannst du deine Implementierung auf folgende Weise erweitern:
1. Das Nachrichten-Parsing sollte mit regulären Ausdrücken (RegEx) implementiert werden. Siehe das Java RegEx-Tutorial für Details:
https://docs.oracle.com/javase/tutorial/essential/regex/intro.html
Diese erweiterte Funktion ist nicht verpflichtend zu implementieren und bringt keine Punkte für die praktische Arbeit. Es ist vielmehr Erfahrung, die du daraus gewinnst J.
