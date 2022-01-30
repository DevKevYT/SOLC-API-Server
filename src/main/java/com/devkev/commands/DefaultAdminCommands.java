package com.devkev.commands;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.util.ArrayList;

import com.devkev.devscript.raw.ApplicationBuilder;
import com.devkev.devscript.raw.Block;
import com.devkev.devscript.raw.Command;
import com.devkev.devscript.raw.DataType;
import com.devkev.devscript.raw.Library;
import com.devkev.devscript.raw.Process;
import com.devkev.devscript.raw.Process.HookedLibrary;
import com.devkev.main.HookManager;
import com.devkev.main.Main;
import com.devkev.main.ServerFileManager;

public class DefaultAdminCommands extends Library {

	HookManager application;
	
	public DefaultAdminCommands(HookManager application) {
		super("Default admin commands");
		this.application = application;
	}

	@Override
	public Command[] createLib() {
		return new Command[] {
				
				new Command("help", "", "Gibt diese Seite aus.") {

					@Override
					public Object execute(Object[] arg0, Process arg1, Block arg2) throws Exception {
						
						for(HookedLibrary lib : arg1.getLibraries()) {
							for(Command c : lib.commands) {
								String args = "";
								for(DataType s : c.arguments) {
									args += "<" + s.type.typeName + "> ";
								}
								//String.format("%1$"+20+ "s%2", c.name + args, c.description);
								System.out.println(String.format("%1$20s %2$20s", c.name + " " + args, c.description));
							}
						}
						
						return null;
					}
					
				},
				
				new Command("uninstall-list", "", "Listet alle Installierten Dateien auf.") {
					@Override
					public Object execute(Object[] arg0, Process arg1, Block arg2) throws Exception {
						
						Main.logger.log("Folgende Dateien sind installiert: ", true);
						
						for(File f : ServerFileManager.getInstalledFiles()) {
							Main.logger.log(f.getAbsolutePath(), true);
						}
						return null;
					}
				},
				
				new Command("uninstall-delete", "", "Löscht alle Programm Dateien inklusive des aktuellen Hooks.") {
					@Override
					public Object execute(Object[] arg0, Process arg1, Block arg2) throws Exception {
						
						for(File f : ServerFileManager.getInstalledFiles()) {
							Main.logger.log("Lösche " + f.getAbsolutePath(), true);
							f.delete();
						}
						return null;
					}
				},
				
				new Command("hook-reload", "", "Startet den Server neu") {
					@Override
					public Object execute(Object[] arg0, Process arg1, Block arg2) throws Exception {
						if(application.application == null) {
							Main.logger.log("Kein Hook geladen. Führe 'update' aus um den neuesten herunterzuladen.", true);
							return null;
						}
						
						Main.logger.log("Lade hook neu aus " + Main.config.getHookFile());
						application.detatchHook();
						application.loadHook(Main.config.getHookFile());
						return null;
					}
				},
				
				new Command("hook-info", "", "Gibt Informationen über die aktuelle Server Version aus") {
					@Override
					public Object execute(Object[] arg0, Process arg1, Block arg2) throws Exception {
						if(application.application == null) {
							Main.logger.log("Kein Hook geladen. Führe 'update' aus um den neuesten herunterzuladen.");
							return null;
						}
						Main.logger.log("Version: " + application.application.hookVersion.toString() , true);
						return null;
					}
				},
				
				new Command("update", "", "Prüft auf eine neue Server Version und updated den Server.") {

					@Override
					public Object execute(Object[] arg0, Process arg1, Block arg2) throws Exception {
						Main.logger.log("Überprüfe auf neue Version ... unter https://api.github.com/repos/DevKevYT/Excel-CellColor-Server/releases/latest", true);
						
						if(application.downloadUpdate()) {
							Main.logger.log("Server auf dem neuesten Stand.", true);
						}
						return null;
					}
				},
				
				new Command("read-log", "string ...", "Usage: 'read-log help' for more info") {
					@Override
					public Object execute(Object[] arg0, Process arg1, Block arg2) throws Exception {
						int lines = 10;
						String filter = "";
						
						if(arg0.length == 0) {
							arg1.log(description, true);
							return null;
						}
						if(arg0.length >= 1) {
							if(arg0[0].toString().equals("help")) {
								System.out.println("Usage:\nread-log <filter[all|error|info]> <last <integer> | *>\n\nBeispiele:\nread-log all\nread-log error last 5\nread-log all last 10");
								
								return null;
							}
							if(arg0[0].toString().equals("error")) {
								filter = "ERROR";
							} else if(arg0[0].toString().equals("info")) {
								filter = "INFO";
							} else filter = "";
						}
						
						if(arg0.length == 3) {
							if(arg0[1].toString().equals("last")) {
								if(ApplicationBuilder.testForWholeNumber(arg0[2].toString())) {
									lines = Integer.valueOf(arg0[2].toString());
									if(lines > 0) {
										System.out.println("Zeige die letzten " + lines + " log einträge\n");
									} else {
										arg1.log(description, true);
										return null;
									}
								} else {
									arg1.log(description, true);
									return null;
								}
							} else {
								arg1.log(description, true);
								return null;
							}
						}
						
						ArrayList<String> flines = new ArrayList<String>();
						BufferedReader reader = new BufferedReader(new FileReader(Main.logger.logFile));
						String line = reader.readLine();
						while(line != null) {
							flines.add(line);
							line = reader.readLine();
						}
						if(lines <= 0) {
							for(int i = 0; i < flines.size(); i++) {
								if(!filter.isEmpty()) {
									if(flines.get(i).startsWith(filter)) {
										System.out.println("[" + (i+1) + "] " + flines.get(i));
									}
								} else System.out.println("[" + (i+1) + "] " + flines.get(i));
							}
						} else {
							for(int i = (flines.size() - lines) < 0 ? 0 : flines.size() - lines; i < flines.size(); i++) {
								if(!filter.isEmpty()) {
									if(flines.get(i).startsWith(filter)) {
										System.out.println("[" + (i+1) + "] " + flines.get(i));
									}
								} else System.out.println("[" + (i+1) + "] " + flines.get(i));
							}
						}
						reader.close();
						return null;
					}
				},
				
				new Command("exit", "", "Stoppt den Server") {
					@Override
					public Object execute(Object[] arg0, Process arg1, Block arg2) throws Exception {
						application.detatchHook();
						System.exit(-1);
						return null;
					}
				}
				
//				new Command("~", "string ...", "Führt einen Shell Befehl aus (output only) (': cat instancelog.log' um die Logdatei anzuzeigen)") {
//					@Override
//					public Object execute(Object[] arg0, Process arg1, Block arg2) throws Exception {
//						System.out.println("Press [ENTER] to interrupt.\n");
//						String all = "";
//						for(Object s : arg0) {
//							all += " " + s.toString();
//						}
//						Process sub = new Process(true);
//						sub.addOutput(new Output() {
//							
//							@Override
//							public void warning(String arg0) {
//								System.err.println("[~] " + arg0);
//							}
//							
//							@Override
//							public void log(String arg0, boolean arg1) {
//								System.out.println("[~] " + arg0);
//							}
//							
//							@Override
//							public void error(String arg0) {
//								System.err.println("[~] " + arg0);
//							}
//						});
//						Thread interrupt = new Thread(new Runnable() {
//							@Override
//							public void run() {
//								Scanner b = new Scanner(System.in);
//								b.nextLine();
//								sub.kill(sub.getMain(), "Interrupted");
//								sub.getLibraries().get(0).lib.scriptExit(sub, 1, "");
//								b.close();
//							}
//						}, "shell interrupt");
//						interrupt.setDaemon(true);
//						interrupt.start();
//						
//						sub.setApplicationListener(new ApplicationListener() {
//							@Override
//							public void done(int arg0) {
//								System.out.println("Done");
//							}
//						});
//						sub.execute("exec <" + all + ">", false);
//						System.out.println("Finished");
//						return null;
//					}
//				}
		};
	}

	@Override
	public void scriptExit(Process arg0, int arg1, String arg2) {
	}

	@Override
	public void scriptImport(Process arg0) {
		
	}
}
