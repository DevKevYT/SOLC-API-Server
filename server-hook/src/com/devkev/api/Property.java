package com.devkev.api;

public class Property {
	
	String key;
	String value;
	
	public static Property of(String key, String value) {
		Property p = new Property();
		p.key = key;
		p.value = value;
		return p;
	}
}
