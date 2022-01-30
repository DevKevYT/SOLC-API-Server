package com.devkev.main;

import com.devkev.devscript.raw.ApplicationBuilder;

public class Version {
	
	/**Increased when: Versions that are incompatible with previous versions*/
	public int MAJOR;
	/**Increased when: Versions that are compatible with previous versions*/
	public int MINOR;
	/**Increased when: Just bug fixed and minor adjustments*/
	public int PATCH;
	
	private Version(int major, int minor, int patch) {
		this.MAJOR = major;
		this.MINOR = minor;
		this.PATCH = patch;
	}
	
	/**Compares the major version*/
	public static boolean compatible(Version v1, Version v2) {
		return v1.MAJOR == v2.MAJOR;
	}
	
	/**@return true if version1 is older than version2*/
	public static boolean isOlder(Version v1, Version v2) {
		if(v1.MAJOR >= v2.MAJOR) {
			if(v1.MINOR >= v2.MINOR) {
				if(v1.PATCH >= v2.PATCH) {
					return false;
				}
			}
		}
		return true;
	}
	
	/**A version should be notet like this: x.x.x
	 * Example: 1.2.3 or 1.2 or 1*/
	public static Version of(String version) {
		String[] subVersions = version.split("\\.");
		if(subVersions.length == 0) return new Version(1, 0, 0);
		else {
			int major = 1;
			int minor = 0;
			int patch = 0;
			for(int i = 0; i < (subVersions.length >= 3 ? 3 : subVersions.length); i++) {
				if(ApplicationBuilder.testForWholeNumber(subVersions[i])) {
					if(i == 0) major = Integer.valueOf(subVersions[i]);
					else if(i == 1) minor = Integer.valueOf(subVersions[i]);
					else if(i == 2) patch = Integer.valueOf(subVersions[i]);
				}
			}
			return new Version(major, minor, patch);
		}
	}
	
	public String toString() {
		return MAJOR + "." + MINOR + "." + PATCH;
	}
}
