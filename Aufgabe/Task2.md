# Aufgabe 2

Prof. Dr. Marcus Schöller

Nachdem die Implementierung der Client-Seite abgeschlossen ist, musst du in dieser Aufgabe den Command-Server implementieren.

## Teilaufgabe 2.1: Cookie-Server-Implementierung (7 Punkte)
In dieser Aufgabe musst du den Cookie-Server teilweise implementieren. Du wirst dich zunächst auf die Verarbeitung von Cookie-Anfragen konzentrieren. Die Cookie-Validierung wird in Aufgabe 3 hinzugefügt.

1. Erweitere CPProtocol.receive(), um Cookie-Anfragen zu empfangen. Die Cookie-Anfragen sollen in einer dedizierten Methode verarbeitet werden (siehe 2.)

2. Erweitere die CPProtocol-Implementierung mit einer Cookie-Verarbeitungsmethode, die ein Server verwendet, um eingehende CookieRequest-Nachrichten zu verarbeiten. Beachte, dass diese Methode sich von der requestCookie()-Methode unterscheidet, die der Client verwendet, um ein Cookie anzufordern.

    a. Die Zuordnungen von Clients zu Cookies werden in einer Java HashMap gespeichert; siehe Java HashMap (https://docs.oracle.com/javase/9/docs/api/java/util/HashMap.html) dafür. Diese HashMap implementiert ein assoziatives Array. Die PhyConfiguration jedes Clients wird als Schlüssel für die Map verwendet. Der Wert in jedem Eintrag ist das jeweilige Cookie.
    • Es sollen niemals mehr als 20 Einträge in der HashMap sein. Cookie-Anfragen von weiteren Clients sollen abgelehnt werden.

    b. Treffe eine Implementierungsentscheidung zur Verarbeitung vorzeitiger Cookie-Erneuerung. Soll es einem Client erlaubt sein, ein neues Cookie anzufordern, während das alte Cookie noch nicht abgelaufen ist?
    • Dokumentiere deine Entscheidung und Begründung mit einer Erklärung in einem begleitenden Kommentar.
    • Implementiere entsprechend deiner Entscheidung.

    c. Sende eine entsprechende Antwortnachricht an den Client.
