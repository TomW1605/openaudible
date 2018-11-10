package org.openaudible;


public class AudibleAccountPrefs {
	public String audibleUser = "";
	public String audiblePassword = "";
	public AudibleRegion audibleRegion = AudibleRegion.AU;
	
	public AudibleRegion getRegion() {
		return audibleRegion;
	}
}

