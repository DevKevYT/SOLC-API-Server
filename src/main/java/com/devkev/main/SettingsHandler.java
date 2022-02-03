package com.devkev.main;

import java.io.File;

import com.sn1pe2win.config.dataflow.Node;
import com.sn1pe2win.config.dataflow.Variable;

public class SettingsHandler {
	
	public interface ConfigModification {
		public void modify(Node node);
	}
	
	public static final String SETTINGS_FILE_NAME = "main.conf";
	private Node settings;
	public ServerFileManager file;
	
	public SettingsHandler() throws IllegalArgumentException, Exception {
		
		ServerFileManager settingsFile = new ServerFileManager(SETTINGS_FILE_NAME, true);
		if(!settingsFile.exists()) {
			Main.logger.log("Config file not found under " + settingsFile.getAbsolutePath() + " Generating default configuration ...", false);
			generateConfigFile(settingsFile);
		} else {
			try {
				settings = new Node(settingsFile);
			} catch(Exception e) {
				throw new IllegalArgumentException("Failed to load config file from " + settingsFile.getAbsolutePath() + ": " + e.getLocalizedMessage());
			}
		}
		
		file = settingsFile;
	}
	
	public ServerFileManager getHookFile() {
		if(settings.get("hook-file") == Variable.UNKNOWN) {
			return null;
		}
		return new ServerFileManager(settings.get("hook-file").getAsString(), true);
	}
	
	public void setHookFile(ServerFileManager file) {
		settings.addString("hook-file", file.getAbsolutePath());
		settings.save(true);
	}
	
	public void setMaxClientsConfig(int maxClients) {
		settings.get("hook-args").getAsNode().addNumber("max-clients", maxClients);
		settings.save(true);
	}
	
	public int getMaxClientsConfig() {
		return settings.get("hook-args").getAsNode().get("max-clients").getAsInt();
	}
	
	public void setClientTimeoutConfig(int timeoutInMs) {
		settings.get("hook-args").getAsNode().addNumber("client-timeout", timeoutInMs);
		settings.save(true);
	}
	
	public int getClientTimeout() {
		return settings.get("hook-args").getAsNode().get("client-timeout").getAsInt();
	}
	
	public ServerFileManager getConfigFile() {
		return file;
	}
	
	public Node getHookArguments() {
		return settings.getCreateNode("hook-args");
	}
	
	public void modifyConfig(ConfigModification modification) {
		modification.modify(settings);
		settings.save(true);
	}
	
	public Node getConfig() {
		return settings;
	}
	
	private void generateConfigFile(File file) throws IllegalArgumentException, Exception {
		file.createNewFile();
		settings = new Node(file);
		settings.addNode("hook-args").addNumber("max-clients", 10).addNumber("client-timeout", 10000);
		settings.addString("hook-file", "hook-files/" + HookManager.HOOK_FILE_NAME + ".jar");
		settings.save(true);
	}
}
