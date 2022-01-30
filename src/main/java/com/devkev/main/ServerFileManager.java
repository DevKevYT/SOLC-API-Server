package com.devkev.main;

import java.io.File;
import java.util.ArrayList;

public class ServerFileManager extends File {
	
	private static final long serialVersionUID = 1L;
	private static final ArrayList<File> installedFiles = new ArrayList<File>();
	
	public ServerFileManager(String pathname, boolean installedFile) {
		super(pathname);
		
		if(installedFile) {
			addInstalledFile(this);
		}
	}

	private void addInstalledFile(File file) {
		if(file.isDirectory()) return;
		
		for(File f : installedFiles) {
			if(f.getAbsolutePath().equals(file.getAbsolutePath())) {
				return;
			}
		}
		installedFiles.add(file);
	}
	
	public static File[] getInstalledFiles() {
		return installedFiles.toArray(new File[installedFiles.size()]);
	}
}
