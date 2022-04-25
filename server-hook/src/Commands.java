

import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.apache.poi.ss.usermodel.Cell;
import org.apache.poi.ss.usermodel.Color;
import org.apache.poi.ss.usermodel.Row;
import org.apache.poi.ss.usermodel.Sheet;
import org.apache.poi.ss.usermodel.Workbook;
import org.apache.poi.xssf.usermodel.XSSFColor;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;

import com.devkev.api.Gateway;
import com.devkev.api.Property;
import com.devkev.api.Response;
import com.devkev.devscript.raw.ApplicationBuilder;
import com.devkev.devscript.raw.Block;
import com.devkev.devscript.raw.Command;
import com.devkev.devscript.raw.Library;
import com.devkev.devscript.raw.Process;
import com.devkev.main.Main;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.sn1pe2win.config.dataflow.Node;
import com.sn1pe2win.config.dataflow.Variable;


//DONE Last downloaded
//TODOm Client command list (Admin command)
//TODO Admin commands to delet phaseplans etc.
//TODO delete phase
//TODO command to list config content 
//TODO Session id zu logs + Zeile + Klasse zu logs
//TODO Phasierung löschen wenn nach x Tagen kein Download gemacht wurde.

//TODO handle unreloaded config
public class Commands extends Library {
	
	public static final int MAX_CELL_ENTRIES = 500;
	public static final String versionJson;
	
	static {
		versionJson = generateJSON(Codes.CODE_SUCCESS, "{\"displayValue\": \"" + Application.VERSION.toString() + 
				"\", \"version\": {\"major\": " + Application.VERSION.MAJOR 
				+ ", \"minor\": " + Application.VERSION.MINOR 
				+ ", \"patch\": " + Application.VERSION.PATCH + "}}");
	}
	
	public Commands() {
		super("Commands");
	}
	
	/*
	 * Server Antwort:
	 * 	Erfolgreich:
	 * 		{
	 * 			"data": "some data"
	 * 			"code": 0
	 * 		}
	 * 	Fehlerhaft:
	 * 		{
	 * 			"error": "Some error message",
	 * 			"code": ERROR_CODE
	 * 		}
	 * 
	 * Codes müssen nicht zwingend Fehlercodes sein. Wenn jedoch "error" in der Serverantwort steckt ist er sicher ein Error.
	 */
	//Generiert eine antwort JSON gemäß des oben genannten Protokolls
	public static String generateJSON(int code, String data) {
		return "{\"data\": " + data + ",\"code\": " + code + "}";
	}
	
	//Generiert eine antwort error JSON gemäß des oben genannten Protokolls
	public static final String generateErrorJSON(int code, String errorMessage) {
		return "{\"error\": \"" + errorMessage + "\",\"code\": " + code + "}";
	}
	
	final SimpleDateFormat format = new SimpleDateFormat("YYYYMMdd");
	
	@Override
	public Command[] createLib() {
		return new Command[] {
				
			new Command("version", "", "Gibt die Serverversion zurück. > 2.1.5") {
				@Override
				public Object execute(Object[] arg0, Process arg1, Block arg2) throws Exception {
					Connection c = (Connection) arg1.getVariable("connection", arg1.getMain());
					try {
						c.sendMessage(versionJson);
					} catch (IOException e) {
						Main.logger.logError("Failed to send 'ready-to-recieve' message to client: " + e.getLocalizedMessage());
					}
					return null;
				}
			},
			
			new Command("download-file", "string", "download-file <klasseid> Stellt die Date als Stream bereit. Wenn eine Nachricht mit code 2 vom Server kommt muss der Befehl 'ready-to-recieve' zurückgesendet werden. Dann kann die Datei empfangen werden") {
				@Override
				public Object execute(Object[] arg0, Process arg1, Block arg2) throws Exception {
					Connection c = (Connection) arg1.getVariable("connection", arg1.getMain());
					
					Main.config.modifyConfig(node -> {
						Variable entry = node.getCreateNode("fileroute").get(arg0[0].toString());
						if(entry != Variable.UNKNOWN) {
							Variable filepath = entry.getAsNode().get("file");
							if(filepath != Variable.UNKNOWN) {
								File sheet = new File(filepath.getAsString());
								if(sheet.exists()) {
									
									try {
										c.sendMessage(generateJSON(Codes.CODE_SEND_READY, "\"ready-to-send\""));
									} catch (IOException e) {
										Main.logger.logError("Failed to send 'ready-to-recieve' message to client: " + e.getLocalizedMessage());
										return;
									}
									//Warte auf Client Bestätigung
									try {
										String approve = c.read();
										
										if(approve.equals("ready-to-recieve")) {
											try (FileInputStream fileReadStream = new FileInputStream(sheet)) {
									            int read;
									            byte[] bytes = new byte[8192];
									            while ((read = fileReadStream.read(bytes)) != -1) {
									            	c.client.getOutputStream().write(bytes, 0, read);
									            }
									            
									            c.status = Codes.CODE_SUCCESS;
									            c.terminateConnection();
											} catch (FileNotFoundException e1) {
									        	Main.logger.logError("Failed to find file to pull download data: " + filepath.getAsString() + ": " + e1.getLocalizedMessage());
											} catch (IOException e1) {
												Main.logger.logError("An IO Exception occurred for download file: " + filepath.getAsString() + ": " + e1.getLocalizedMessage());
											}
										}
									} catch (IOException e) {
										Main.logger.logError("Failed to recieve download approve from client");
										return;
									}
								} else {
									c.status = Codes.CODE_FILE_MISSING;
									arg1.error("Sheet for subject " + arg0[0].toString() + " not found");
								}
							} else {
								//Corrupted. Delete
								c.status = Codes.CODE_ENTRY_MISSING;
								arg1.error("Subject " +  arg0[0].toString() + " not found");
								
								entry.delete();
								File f = new File("uploaded-sheets/" + arg0[0].toString() + "/");
								if(f.exists()) f.delete();
							}
						} else {
							c.status = Codes.CODE_ENTRY_MISSING;
							arg1.error("Subject not found");
						}
					});
					return null;
				}
			},
			
			new Command("phase-status", "string", "Hier sind die Berechtigungen eher entspannt. Die ID ist die ID der Klasse von der die Informationen geholt soll.") {
				@Override
				public Object execute(Object[] arg0, Process arg1, Block arg2) throws Exception {
					Connection c = (Connection) arg1.getVariable("connection", arg1.getMain());
					
					//Suche nach der Klasse
					Main.config.modifyConfig(node -> {
						Variable entry = node.getCreateNode("fileroute").get(arg0[0].toString());
						if(entry != Variable.UNKNOWN) {
							Variable startDate = entry.getAsNode().get("start");
							Variable endDate = entry.getAsNode().get("end");
							Variable created = entry.getAsNode().get("created");
							Variable owner = entry.getAsNode().get("owner");
							Variable ownerdn = entry.getAsNode().get("ownerdn");
							
							String latestDownload = String.valueOf(System.currentTimeMillis());
							entry.getAsNode().addString("last-download", latestDownload);
							
							if(ownerdn == Variable.UNKNOWN) {
								entry.getAsNode().addString("ownerdn", "unknown-older-version");
								ownerdn = entry.getAsNode().get("ownerdn");
							}
							if(startDate == Variable.UNKNOWN || endDate == Variable.UNKNOWN || created == Variable.UNKNOWN || owner == Variable.UNKNOWN
									|| !startDate.isNumber() || !endDate.isNumber() || !owner.isNumber()) {
								//Corrupted. Delete
								entry.delete();
								Main.logger.logError("Deleting corrupted subject entry: " + arg0[0].toString());
								c.status = Codes.CODE_ENTRY_MISSING;
								arg1.error("Subject not found.");
							} else {
								try {
									c.sendMessage(generateJSON(Codes.CODE_SUCCESS,
											"{\"startDate\": " + startDate.getAsInt() 
											+ ",\"endDate\": " + endDate.getAsInt() 
											+ ",\"created\": \"" + created.getAsString() 
											+ "\" ,\"fileowner\": " + owner.getAsInt() 
											+ ",\"ownerDisplayName\": \"" + ownerdn.getAsString() + "\"}"));
									c.status = Codes.CODE_SUCCESS;
								} catch (IOException e) {
									Main.logger.logError("Failed to send success response to client: " + e.getLocalizedMessage());
								}
							}
						} else {
							c.status = Codes.CODE_ENTRY_MISSING;
							arg1.error("Subject not found");
						}
					});
					return null;
				}
			},
			
			new Command("upload-file", "string string string string string", 
					"uploadfile <sessionID> <Bearertoken> <klasseID> <startDate> <endDate> "
					+ "Die sessionID ist die SessionID einer gültigen Session. Die ID der anzufragenden Person. Achtung nutzt ein eingeloggter Client diesen Befehl wird er ausgeloggt!"
					+ "Start und Enddate sind im standart Untis API Format anzugeben: YYYYMMDD") {
				@Override
				public Object execute(Object[] arg0, Process arg1, Block arg2) throws Exception {
					Connection c = (Connection) arg1.getVariable("connection", arg1.getMain());
					//TODO KlasseID statt personID.
					final String sessionId = arg0[0].toString();
					final String token = arg0[1].toString();
					final int klasseId;
					final int startDate;
					final int endDate;
					
					if(ApplicationBuilder.testForWholeNumber(arg0[3].toString()) || arg0[3].toString().length() != 8) {
						startDate = Integer.valueOf(arg0[3].toString());
					} else {
						c.status = Codes.CODE_ARGUMENT_PARSE_ERROR;
						arg1.error("Invalid startDate format (Argument 4)");
						return null;
					}
					
					if(ApplicationBuilder.testForWholeNumber(arg0[4].toString()) || arg0[4].toString().length() != 8) {
						endDate = Integer.valueOf(arg0[4].toString());
					} else {
						c.status = Codes.CODE_ARGUMENT_PARSE_ERROR;
						arg1.error("Invalid endDate format (Argument 5)");
						return null;
					}
					
					if(ApplicationBuilder.testForWholeNumber(arg0[2].toString())) {
						klasseId = Integer.valueOf(arg0[2].toString());
					} else {
						c.status = Codes.CODE_ARGUMENT_PARSE_ERROR;
						arg1.error("Invalid Person ID (Argument 2)");
						return null;
					}
					
					//Schritt 1: Verifiziere die SessionID:
					Calendar calendar = Calendar.getInstance();
					String testDate = calendar.get(Calendar.YEAR) + "" + 
							(calendar.get(Calendar.MONTH) <= 9 ? "0" + (calendar.get(Calendar.MONTH)+1) : (calendar.get(Calendar.MONTH)+1))
							+ (calendar.get(Calendar.DAY_OF_MONTH) <= 9 ? "0" + calendar.get(Calendar.DAY_OF_MONTH) : calendar.get(Calendar.DAY_OF_MONTH));
					
					Response<JsonObject> response = Gateway.POST(
							"https://hepta.webuntis.com/WebUntis/jsonrpc.do?school=bbs1-mainz", 
							"{\"jsonrpc\": 2.0,\"method\": \"getTimetable\",\"params\": {\"startDate\":\"" + testDate + "\",\"endDate\":\"" + testDate + "\" },\"id\": \"sol-connect\"}",
							Property.of("Cookie", "JSESSIONID=" + sessionId));
					
					if(!response.containsPayload()) {
						c.status = Codes.CODE_MISSING_PAYLOAD;
						arg1.error("Missing API payload. API down? (Http: " + response.httpStatus + ", error message: " + response.errorMessage + ", error code: " + response.errorCode + ")");
						return null;
					} else {
						if(response.errorCode == -8520) {
							c.status = Codes.CODE_NOT_AUTHENTICATED;
							arg1.error("Session authentification failed");
							return null;
						} 
					}
					
					try {
						Gateway.POST(
								"https://hepta.webuntis.com/WebUntis/jsonrpc.do?school=bbs1-mainz", 
								"{\"jsonrpc\": \"2.0\",\"method\":\"logout\",\"id\": \"sol-connect\"}", 
								Property.of("Cookie", "JSESSIONID=" + sessionId));
					} catch(Exception e) {
						Main.logger.log("Failed to log user out");
					}
					
					//Schritt 2: Verifiziere Rechte
					Response<JsonObject> data = Gateway.GET("https://hepta.webuntis.com/WebUntis/api/rest/view/v1/app/data", Property.of("Authorization", "Bearer " + token));
					
					if(!data.containsPayload()) {
						
						c.status = Codes.CODE_MISSING_PAYLOAD;
						arg1.error("Missing API payload. API down? (Http: " + data.httpStatus + ", error message: " + data.errorMessage + ", error code: " + data.errorCode + ")");
						return null;
					} else {
						if(data.httpStatus == 401) {
							c.status = Codes.CODE_NOT_AUTHENTICATED;
							arg1.error("Invalid Token");
							return null;
						} else if(data.httpStatus != 200) {
							c.status = Codes.CODE_HTTP_ERROR;
							arg1.error("http error: " + data.httpStatus);
							return null;
						}
						if(data.getResponseData().getAsJsonObject("user").getAsJsonObject("person") != null) {
							int uploaderId = data.getResponseData().getAsJsonObject("user").getAsJsonObject("person").getAsJsonPrimitive("id").getAsInt();
							String uploaderDisplayName = data.getResponseData().getAsJsonObject("user").getAsJsonPrimitive("name").getAsString();
							
							//Überprüfe Berechtigung
							if(data.getResponseData().getAsJsonObject("user").getAsJsonArray("roles") != null) {
								for(JsonElement e : data.getResponseData().getAsJsonObject("user").getAsJsonArray("roles")) {
									if(e.getAsString().equals("TEACHER")) {
										
										//Überprüfe Dateispeicher und erstelle route in der Konfig ...
										Main.config.modifyConfig(node -> {
											File folder = new File("uploaded-sheets/");
											if(!folder.exists()) folder.mkdir();
											
											Variable entry = node.getCreateNode("fileroute").get(String.valueOf(klasseId));
											File sheetFile = null;
											
											if(entry != Variable.UNKNOWN) {
												sheetFile = new File("uploaded-sheets/" + String.valueOf(klasseId) + "/sheet.xlsx");
												entry.getAsNode().addString("created", String.valueOf(System.currentTimeMillis()));
												entry.getAsNode().addNumber("start", startDate);
												entry.getAsNode().addNumber("end", endDate);
												entry.getAsNode().addNumber("owner", uploaderId);
												entry.getAsNode().addString("ownerdn", uploaderDisplayName);
												if(!sheetFile.exists()) {
													sheetFile = null;
													entry = Variable.UNKNOWN;
												}
											}
											String relativePath = "";
											if(entry == Variable.UNKNOWN) {
												//Erstelle Folder und Date
												File userFolder = new File("uploaded-sheets/" + String.valueOf(klasseId) + "/");
												if(!userFolder.exists()) userFolder.mkdir();
												
												relativePath = "uploaded-sheets/" + String.valueOf(klasseId) + "/sheet.xlsx";
												sheetFile = new File(relativePath);
												Node userStorage = node.get("fileroute").getAsNode().addNode(String.valueOf(klasseId));
												
												userStorage.addString("file", relativePath);
												userStorage.addString("created", String.valueOf(System.currentTimeMillis()));
												userStorage.addNumber("start", startDate);
												userStorage.addNumber("end", endDate);
												userStorage.addNumber("owner", uploaderId);
												userStorage.addString("ownerdn", uploaderDisplayName);
												try {
													sheetFile.createNewFile();
												} catch (IOException exception) {
													Main.logger.logError("Failed to create file for uploaded sheet under: " + relativePath + ": " + exception.getLocalizedMessage());
												}
											} 
											
											//Berechtigung für file Upload!
											c.status = Codes.CODE_SUCCESS;
											try {
												c.sendMessage(generateJSON(Codes.CODE_READY, "\"ready-for-file\""));
											} catch (IOException e2) {
												Main.logger.logError("Failed to send 'ready-for-file' message to client: " + e2.getLocalizedMessage());
												return;
											}
											
											if(sheetFile != null) {
												try (FileOutputStream outputStream = new FileOutputStream(sheetFile, false)) {
										            int read;
										            byte[] bytes = new byte[8192];
										            while ((read = c.client.getInputStream().read(bytes)) != -1) {
										                outputStream.write(bytes, 0, read);
										            }
										        } catch (FileNotFoundException e1) {
										        	Main.logger.logError("Failed to find file to write upload data: " + relativePath + ": " + e1.getLocalizedMessage());
												} catch (IOException e1) {
													Main.logger.logError("An IO Exception occurred for upload file: " + relativePath + ": " + e1.getLocalizedMessage());
												}
											}
										});
										
										return null;
									}
								}
								
								c.status = Codes.CODE_INSUFICCIENT_PERMISSIONS;
								arg1.error("No teacher permission");
								return null;
							}
						}
					}
					
					c.status = Codes.CODE_UNKNOWN_ERROR;
					arg1.error("Unknown Error: " + data.httpStatus);
					return null;
				}
			},
			
			new Command("convertxssf", "", "Erwartet einen Stream.") {
				@Override
				public Object execute(Object[] arg0, Process arg1, Block arg2) throws Exception {
					
					Connection c = (Connection) arg1.getVariable("connection", arg1.getMain());
					
					//Warte auf den Excel stram. Der timeout wird vom observer gehandled
					//Sende ready antwort. Dies signalisiert dass der Server bereit ist die Excel zu empfangen!
					c.sendMessage("{\"message\": \"ready-for-file\"}");
					
					try {
						
						StringBuilder json = new StringBuilder("{\"message\":\"ok\",\"data\":[");
			            Workbook workbook = new XSSFWorkbook(c.client.getInputStream());
			    		Sheet sheet = workbook.getSheetAt(0);
			    		
			    		Map<Integer, List<String>> data = new HashMap<>();
			    		int i = 0;
			    		int cellEntries = 0;
			    		
			    		main: for (Row row : sheet) {
			    			
			    		    data.put(i, new ArrayList<String>());
			    		    for (Cell cell : row) {
			    		    	
			    		    	Color fillColor = cell.getCellStyle().getFillForegroundColorColor();
			    		    	if(fillColor != null) {
				    		    	String hex = ((XSSFColor) fillColor).getARGBHex().substring(1);
				    		    	
				    		    	String cellEntry = "{\"x\":" + cell.getColumnIndex() + ",\"y\":" + cell.getRowIndex() + ",";
				    		    	
				    		    	String colorEntry = "\"c\":{";
				    		    	
				    		    	int colorIndex = 0;
				    		    	for(int rgb : hex2Rgb(hex)) {
				    		    		if(colorIndex == 0) colorEntry += "\"r\":" + rgb + ",";
				    		    		else if(colorIndex == 1) colorEntry += "\"g\":" + rgb + ",";
				    		    		else if(colorIndex == 2) colorEntry += "\"b\":" + rgb;
				    		    		
				    		    		colorIndex++;
				    		    	}
				    		    	colorEntry += "}";
				    		    	
				    		    	cellEntry += colorEntry + "},";
				    		    	json.append(cellEntry);
				    		    	cellEntries++;
				    		    	
				    		    	if(cellEntries > MAX_CELL_ENTRIES) {
				    		    		c.status = 1;
				    		    		Main.logger.logError("Session: " + c.sessionId + ": Exceeded max colored cell entries. Aborting");
				    		    		break main;
				    		    	}
			    		    	}
			    		    }
			    		    i++;
			    		}
			    		
			    		if(i > 0) json.deleteCharAt(json.length()-1);
			    		json.append("]}");
			    		
			    		c.status = Codes.CODE_SUCCESS;
			    		c.sendMessage(json.toString().trim());
			    		workbook.close();
					} catch(Exception e) {
						
						c.status = 1;
						Main.logger.logError("Session: " + c.sessionId + ": Error while converting Excel: " + e.getMessage());
						if(!c.client.isClosed()) 
							c.sendMessage("{\"error\": \"" + e.getMessage() + "\"}");
					}
					return null;
				}
				
				public int[] hex2Rgb(String colorStr) {
				    return new int[] {
				            Integer.valueOf(colorStr.substring(1, 3), 16 ),
				            Integer.valueOf(colorStr.substring(3, 5), 16 ),
				            Integer.valueOf(colorStr.substring(5, 7), 16 )
				    };
				}
			}
		};
	}

	@Override
	public void scriptExit(Process arg0, int arg1, String arg2) {
	}

	@Override
	public void scriptImport(Process arg0) {
	}
}
