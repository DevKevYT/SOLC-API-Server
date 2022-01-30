package com.devkev.main;

import java.net.URLClassLoader;

import com.devkev.devscript.raw.Library;
import com.sn1pe2win.config.dataflow.Node;

public abstract class Hook {
	
	public URLClassLoader loader;
	public ServerFileManager file;
	public final Version hookVersion;
	
	public Hook(Version hookVersion) {
		this.hookVersion = hookVersion;
	}
	
	public abstract void init(Node arguments);
	
	public abstract void shutdown();
	
	public abstract void listen();
	
	public abstract Library adminCommands();
}
