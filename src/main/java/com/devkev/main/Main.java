package com.devkev.main;

/**@author Philipp Gersch*/
public class Main {
	
	public static Logger logger;
	public static SettingsHandler config;
	public static final Version version = Version.of("2.0.0");
	
	public static void main(String[] args) throws IllegalArgumentException, Exception  {
		logger = new Logger();
		
		logger.log("[Version: " + version + "]", true);
		System.out.println();
		
		config = new SettingsHandler();
		new HookManager();
	}
	
}
