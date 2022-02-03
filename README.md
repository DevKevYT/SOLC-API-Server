# SOL-Connect API
Ein Java Server der Funktionen für die SOl-Connect App als Pseudo API bereitstellt.
Versichedene Funktionen können [hier](https://github.com/DevKevYT/Excel-CellColor-Server/edit/main/README.md#server-funktionen) eingesehen werden.

## Projects using this project:
<a>https://github.com/floodoo/SOL-Connect/</a>

## Allgemeine Informationen

Der Server besteht hauptsächlich aus zwei .jar Dateien. Die Installation erstellt 4-5 Dateien.

- `instancelog.log` (Log Datei, automatisch generiert)
- `main.conf` (Konfigurationsdatei, automatisch generiert, veränderbar)
- `Main.jar` (Einstiegspunkt, muss manuell ausgeführt werden)
- `server-hook.jar` (Automatischer download aus GitHub releases)
- `backup.tmp` (Backupdatei bei einem `server-hook.jar` update)

Die `Main.jar` Datei ist das Hauptprogramm welches manuell gepflegt werden muss.<br>
Es stellt grundlegende Administrationsbefehle zur Verfügung und kümmert sich darum,
dass die `server-hook.jar` immer auf dem neuesten Stand ist.<br>
Das Hauptprogramm selber sollte selten Updates benötigen. (-> Sektion [Updating](https://github.com/DevKevYT/SOLC-API-Server/edit/main/README.md#updates))

Die `server-hook.jar` kümmert sich um alles um den Server am laufen zu halten.<br>
Sie ist nicht selbst ausführbar.<br>
Sie lässt sich mittels der Main.jar konfigurieren, ausführen und updaten.

## Installation

Jeder GitHub release beinhaltet zwei Binärdateien:
- `Main.jar` (Das Hauptprogramm wie oben beschrieben)
- `server-hook.jar` (Die Server Datei)

Um den Server zum laufen zu bringen reicht es die `Main.jar` herunterzuladen und auszuführen.
Konfigurationsdateien werden automatisch generiert.
Beim ersten start wird eine Fehlermeldung kommen:
```
.../server-hook.jar nicht gefunden oder ist ein Ordner. Überprüfe main.conf oder führe den Befehl 'update' aus!
```
Hier reicht es den `update` Befehl auszuführen.

Um die `Main.jar` auszuführen folgenden Befehl im Ordner eingeben wo die Main.jar gedownloaded wurde:

```
java -Xmx1G -jar Main.jar
```
## Administrator Konsole
Die administrator Konsole ermöglicht es dir Befehle der `Main.jar` oder auch des Hooks auszuführen.<br>
Du erkennst sie an zwei `>>` ein der Konsole.
<br>
<br>
Um eine Liste von Befehlen zu erhalten, gib `help` ein.
```
>>help
               help  Gibt diese Seite aus.
     uninstall-list  Listet alle Installierten Dateien auf.
   uninstall-delete  Löscht alle Programm Dateien inklusive des aktuellen Hooks.
        hook-reload  Startet den Server neu
           hook-info  Gibt Informationen über die aktuelle Server Version aus
             update  Prüft auf eine neue Server Version und updated den Server.
  read-log <string>  Usage: 'read-log help' for more info
               exit     Stoppt den Server
```

## Server Einstellungen
Diese Sektion beschreibt, welche Möglichkeiten es gibt das Serververhalten einzustellen.

### Konfigurationsdatei main.conf
Folgende Konfigurationen werden automatisch bei der ersten ausführung erstellt:

```
hook-args={
	max-clients=10.0;
	client-timeout=10000.0;
	port=6969;
}
hook-file=hook-files/server-hook.jar;
```

- `hook-file`: Relativer oder absoluter download Pfad für die `server-hook.jar`.
- `hook-args`: Argumente die dem `server-hook.jar` beim laden übergeben werden.
- `max-clients`: Maximale anzahl an Clients, die gleichzeitig mit dem Server verbunden sein dürfen.
- `client-timeout`: Maximale dauer ein Client darf mit dem Server verbunden sein, bevor die Verbindung serverwseitig getrennt wird.
- `port`: Der Port auf dem der Server laufen soll (Optional, standart: 6969)

Um änderungen an der Konfiguration wirksam zu machen gibt es mehrere Möglichkeiten:
- Befehl `hook-reload` eingeben.
- Befehl `exit` eingeben und `Main.jar` neu starten

### Die server-hook.jar selber
Da die Hook Datei nur eine einfache .jar Datei ist die mit einem ClassLoader geladen wird, kannst du ganz einfach selber eine erstellen!

Vorraussetzung ist eine .jar Datei die eine Klasse beinhaltet mit dem Namen `Application`
die von der abstrakten Klasse `com.devkev.main.Hook` erbt.
Jetzt nur noch `server-hook.jar` im angegebenen Pfad in main.conf ersetzen oder einen eigenen erstellen und deine eigenen `hook-args` erstellen.

## Updates
Aktuell ist es nur möglich mittels dem Befehl `update` manuell nach neuen GitHub releases zu checken.
Automatische updates werden bald kommen.
Diese Updates werden aber nur die Hook Datei beeinflussen. Um die `Main.jar` zu updaten musst du 
selber in die Github releases schauen.
Falls eine neue Version für `Main.jar` verfügbar ist, wird es in der Release Beschreibung stehen.

# Datentransfer

Es ist möglich Befehle an den Server zu senden. Das sind einfach Strings die über einen Socket gesendet werden. Maximale Zeichenlänge beträgt 6000.
Clients sind auf einen Befehl pro Verbindung beschränkt. Üblicherweise wäre das ein Befehl mit ein paar Parametern.<br>
`befehl "ein-parameter"`

Bis auf Befehle sollte jeglicher Datentransfer zwischen Server und Client im JSON Format erfolgen.
Standartmäßig sendet der Server Antorten im folgenden Format:

```json
{
  "data": "..",
  "code": 0
}
```
Fehlernachricht:
```json
{
  "error": "Fehler Nachricht",
  "code": 100
}
```
Antwort Codes 0-99 sind lediglich Statusmeldungen.
Codes vom 100-... sind Fehlercodes.
Eine Liste aller möglichen Codes findest du unter server-hook/Codes.java

# SOLC-API Server dependencies:
- <a>https://github.com/DevKevYT/devscript</a> version: '1.9.4'<br>
- <a>https://mvnrepository.com/artifact/org.apache.poi/poi</a> version: '5.1.0'<br>
- <a>https://mvnrepository.com/artifact/org.apache.poi/poi-ooxml</a> version: '5.1.0'<br>
- <a>https://mvnrepository.com/artifact/org.apache.xmlbeans/xmlbeans</a> version: '2.3.0'<br>
- <a>https://mvnrepository.com/artifact/dom4j/dom4j</a> version: '1.6.1'<br>
- <a>https://mvnrepository.com/artifact/org.apache.commons/commons-collections4</a> version: '4.3'<br>
- <a>https://mvnrepository.com/artifact/org.apache.commons/commons-compress</a> version: '1.18'<br>
- <a>https://mvnrepository.com/artifact/org.apache.poi/ooxml-schemas</a> version: '4.1'

