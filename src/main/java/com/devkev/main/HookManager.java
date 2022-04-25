package com.devkev.main;

import java.io.BufferedInputStream;
import java.io.BufferedReader;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.URL;
import java.net.URLClassLoader;
import java.nio.file.Files;
import java.nio.file.StandardCopyOption;
import java.util.Scanner;

import com.devkev.commands.DefaultAdminCommands;
import com.devkev.devscript.raw.Library;
import com.devkev.devscript.raw.Output;
import com.devkev.devscript.raw.Process;
import com.google.gson.JsonElement;
import com.google.gson.JsonParser;

public class HookManager {
	
	public static final String HOOK_FILE_NAME = "server-hook";
	
	public Hook application;
	
	Version latestVersion;
	
	public HookManager() {
		
		ServerFileManager defaultHook = Main.config.getHookFile();
		if(defaultHook == null) {
			defaultHook = new ServerFileManager("hook-files/server.jar", true);
			Main.config.setHookFile(defaultHook);
			
			Main.logger.log("Hook Datei in der Config nicht gefunden. Erstellen der Default Datei: " + defaultHook.getAbsolutePath());
		}
		if(defaultHook.exists() && !defaultHook.isDirectory()) {
			loadHook(defaultHook);
		} else {
			Main.logger.logError("------------------\n" + defaultHook.getAbsolutePath() + " nicht gefunden oder ist ein Ordner.\nÜberprüfe auf neue Version ...!\n------------------", true);
			try {
				if(downloadUpdate()) {
					Main.logger.log("Server auf dem neuesten Stand.", true);
				} else {
					Main.logger.logError("Update fehlgeschlagen. Bitte versuche es erneut oder installiere die hook Datei manuell!");
				}
			} catch (IOException e) {
				Main.logger.logError("Update fehlgeschlagen. Bitte versuche es erneut oder installiere die hook Datei manuell!");
				e.printStackTrace();
			}
		}
		
		new Thread(new Runnable() {
			@Override
			public void run() {
				Scanner scanner = new Scanner(System.in);
				final Process executor = new Process(true);
				executor.clearLibraries();
				executor.includeLibrary(new DefaultAdminCommands(HookManager.this));
				if(application != null) {
					Library lib = application.adminCommands();
					if(lib != null) {
						executor.includeLibrary(lib);
					}
				}
				executor.addOutput(new Output() {
					@Override
					public void warning(String arg0) {
						System.out.println(arg0);
						Main.logger.log("[CMD: '" + arg0 + "']");
					}
					
					@Override
					public void log(String arg0, boolean arg1) {
						System.out.println(arg0);
						Main.logger.log("[CMD: '" + arg0 + "']");
					}
					
					@Override
					public void error(String arg0) {
						System.err.println(arg0);
						Main.logger.logError("[CMD: '" + arg0 + "']");
					}
				});
				System.out.println("\n###\nWillkommen in der Admin Konsole.\nGib 'help' ein für eine Liste von Befehlen\n###\n");
				
				while(true) {
					try {
						System.out.print(">>");
						String command = scanner.nextLine();
						executor.execute(command, false);
					} catch(Exception e) {
						Main.logger.logError("Console not available");
						scanner.close();
						break;
					}
				}
			}
			
		}, "Admin Console").start();
		
	}
	
	public boolean downloadUpdate() throws IOException {
		URL obj;
		HttpURLConnection con;
		try {
			obj = new URL("https://api.github.com/repos/DevKevYT/Excel-CellColor-Server/releases/latest");
			con = (HttpURLConnection) obj.openConnection();
			con.setRequestMethod("GET");
		} catch (Exception e) {
			Main.logger.logError("Failed to check for newer Versions: " + e.getLocalizedMessage());
			return false;
		}
		
		int responseCode = 404;
		String inputLine;
		StringBuilder responseCollector = new StringBuilder();
		
		responseCode = con.getResponseCode();
		InputStream stream;
		if(responseCode >= 200 && responseCode < 400) stream = con.getInputStream();
		else stream = con.getErrorStream();
		BufferedReader in = new BufferedReader(new InputStreamReader(stream));
		while ((inputLine = in.readLine()) != null) responseCollector.append(inputLine);
		in.close();
		
		JsonElement parsed = JsonParser.parseString(responseCollector.toString());
		//String downloadUrl = parsed.getAsJsonObject().getAsJsonArray("assets").get(0).getAsJsonObject().getAsJsonPrimitive("browser_download_url").getAsString();
		for(JsonElement element : parsed.getAsJsonObject().getAsJsonArray("assets")) {
			String releaseURL = element.getAsJsonObject().getAsJsonPrimitive("browser_download_url").getAsString();
			
			if(releaseURL.endsWith(HOOK_FILE_NAME + ".jar")) {
				Version downloadVersion = Version.of(releaseURL.substring(releaseURL.indexOf("/download/") + 10, releaseURL.lastIndexOf("/")));
				if(application != null) {
					
					if(Version.isOlder(application.hookVersion, downloadVersion)) {
						
						Main.logger.log("Eine neue Version ist verfügbar: " + application.hookVersion + " -> " + downloadVersion, true);
						ServerFileManager downloaded = downloadLatest(releaseURL);
						Main.logger.log("server.jar heruntergeladen. Versuche zu hooken ...", true);
						loadHook(downloaded);
						Main.logger.log("Server auf Version " + application.hookVersion + " geupdated!", true);
					} else {
						return true;
					}
				} else {
					ServerFileManager downloaded = downloadLatest(releaseURL);
					Main.logger.log("server.jar heruntergeladen. Versuche zu hooken ...", true);
					loadHook(downloaded);
				}
				
				return false;
			}
		}
		Main.logger.logError("Konnte keinen gültigen release finden. Bitte überprüfe den \"Latest release\" unter https://github.com/DevKevYT/Excel-CellColor-Server/releases", true);
		return false;
	}
	
	public void detatchHook() {
		if(application != null) {
			try {
				application.shutdown();
			} catch(Exception e) {}
		}
		if(application.loader != null) {
			try {
				application.loader.close();
			} catch (IOException e) {
				e.printStackTrace();
			}
		}
		application = null;
	}
	
	public ServerFileManager downloadLatest(String downloadUrl) throws IOException {
		
		if(application != null) {
			ServerFileManager file = application.file;
			Main.logger.log("Erstelle backup ...", true);
			File bckup = new File(file.getParentFile().getAbsolutePath() + "/backup.tmp");
			Files.copy(file.toPath(), bckup.toPath(), StandardCopyOption.REPLACE_EXISTING);
			file.delete();
		}
		
		ServerFileManager downloadFile = Main.config.getHookFile();
		
		if(downloadFile.isDirectory()) {
			downloadFile.mkdir();
			downloadFile = new ServerFileManager(Main.config.getHookFile().getAbsolutePath() + HOOK_FILE_NAME + ".jar", true);
			Main.config.setHookFile(downloadFile);
		} 
		
		try {
			downloadFile.createNewFile();							
		} catch(Exception e) {
			downloadFile.getParentFile().mkdirs();
			downloadFile.createNewFile();	
		}
		
		Main.logger.log("Downloade server ... unter " + downloadFile.getAbsolutePath(), true);
		
		try (BufferedInputStream inputStream = new BufferedInputStream(new URL(downloadUrl).openStream());
		  FileOutputStream fileOutputStream = new FileOutputStream(downloadFile)) {
		    byte dataBuffer[] = new byte[1024];
		    int bytesRead;
		    while ((bytesRead = inputStream.read(dataBuffer, 0, 1024)) != -1) {
		        fileOutputStream.write(dataBuffer, 0, bytesRead);
		    }
		} catch (IOException e) {
		    e.printStackTrace();
		}
		return downloadFile;
	}
	
	//Hook Datei MINOR Versionen müssen in der Version der Main MINOR Version entsprechen
	//Main Minor Versionen werden geändert, wenn sie nicht mit älteren Hooks kompatibel sind
	public void loadHook(ServerFileManager file) {
		
		Hook application;
		
		try {
			Main.logger.log("Verifying file " + file.getAbsolutePath() + " ...", true);
			java.net.URI uri = file.toURI();
			java.net.URL url = uri.toURL();
			
			@SuppressWarnings("resource")
			URLClassLoader loader = new java.net.URLClassLoader(new java.net.URL[] {url});
			Class<?> pluginClass;
			try  {
				pluginClass = loader.loadClass("Application");
			} catch(ClassNotFoundException e) {
				Main.logger.logError("------------------\nKlasse 'Application extends Hook' nicht in .jar gefunden.\nStelle sicher, dass diese Date als Hook verifiziert werden kann.\nFühre den Befehl 'update' aus um die neueste Server Version herunterzuladen!\n------------------", true);
				loader.close();
				return;
			}
			
			Main.config = new SettingsHandler();
			application = (Hook) pluginClass.getConstructor().newInstance();
			
			if(application.hookVersion.MINOR != Main.version.MINOR) {
				Main.logger.logError("------------------\nHook (Version " + application.hookVersion + ") ist nicht kompatibel mit Main Programm (Version " + Main.version + ").\nLade die neueste \"Main.jar\" unter https://github.com/DevKevYT/Excel-CellColor-Server/releases herunter!\n------------------", true);
				loader.close();
				return;
			}
			
			if(this.application != null) {
				this.application.shutdown();
			}
			
			application.loader = loader;
			application.file = file;
			application.init(Main.config.getHookArguments());
			Main.logger.log("Setze Hook auf Version: " + application.hookVersion + " ...", true);
			
			this.application = application;
			this.application.listen();
		} catch(Exception e) {
			Main.logger.logError("Failed to load: " + e.getLocalizedMessage(), true);
			if(this.application != null) {
				Main.logger.log("Falling back to older version ...");
			}
			e.printStackTrace();
			return;
		}
	}
	
	public void hookApplication() {
		
		application.listen();
	}
}
