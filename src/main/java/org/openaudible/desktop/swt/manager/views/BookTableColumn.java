package org.openaudible.desktop.swt.manager.views;

public enum BookTableColumn {
	File, Title, Author, Narrated_By, Time, Purchased, Released, Task;
	static int widths[] = {22, 300, 200, 200, 80, 150, 150, 260};
	
	// HasAAX, HasMP3,
	public static int[] getWidths() {
		return widths;
	}
}

