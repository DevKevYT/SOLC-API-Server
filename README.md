# Excel-CellColor-Server
A Java Server which returns all colored cells to an uploaded Excel Sheet.

## Projects using this project:
<a>https://github.com/floodoo/untis_phasierung/</a>

## RUN THE EXCEL SERVER:

You may configure the .jar with two arguments:
- Maximum amount of clients which can be connected at the same time
- Maximal amount of time a client can be connected to the server (Given in milliseconds)

You should only run the .jar file inside a terminal. This way you can cconfigure both arguments as you like.

Example command to run the server:

```java -Xmx1G -jar excelServer.jar 10 10000```

## SERVER FUNCTIONS

In general, it is possible to send commands to the server. These are simple strings sent over a TCP socket.
Clients are resticted to request only one line of String per connection. This would usually be a command with some arguments.
> "do-something"

Data generated and sent back to the requesting client is always in this JSON format:
```json
{
  "message": { 
    "some": "data"
  }
  "errorMessage": "ErrorMessage"
}
```  
Please note that only either the "message" or the "errorMessage" field are present in the JSON. Never both.
Here is a list of the current commands for version 1.1.1:

#### convertxssf
* This commands expects no arguments. When the response "ready" is sent, the server expects the excel file as stream.
When finished, the server will response with a list of cells with their x and y index and their color

## CHANGELOG

### 1.0.1 (OBSOLETE!)
- Fixing a vulnerability
- More detailed logging for better minitoring
- Only errors are logged
- Arguments can become infinite if a value 0 or below 0 is provided.

### 1.1.0
- Logfile creation "instancelog.log".
- Server admin commands: 
    - A list can be called with "help"
    - You can also execute shell commands
- Changed the 'convertxsff' client command flow. `!Aktueller client nicht mehr mit 1.0.1 kompatibel!`

### 1.1.1
- Fixed an issue, where there would always be an error thrown on the first client connection of an instance.
- Various more bugfixes

# Excel Server dependencies:

- <a>https://github.com/DevKevYT/devscript</a> version: '1.9.4'<br>
- <a>https://mvnrepository.com/artifact/org.apache.poi/poi</a> version: '5.1.0'<br>
- <a>https://mvnrepository.com/artifact/org.apache.poi/poi-ooxml</a> version: '5.1.0'<br>
- <a>https://mvnrepository.com/artifact/org.apache.xmlbeans/xmlbeans</a> version: '2.3.0'<br>
- <a>https://mvnrepository.com/artifact/dom4j/dom4j</a> version: '1.6.1'<br>
- <a>https://mvnrepository.com/artifact/org.apache.commons/commons-collections4</a> version: '4.3'<br>
- <a>https://mvnrepository.com/artifact/org.apache.commons/commons-compress</a> version: '1.18'<br>
- <a>https://mvnrepository.com/artifact/org.apache.poi/ooxml-schemas</a> version: '4.1'

