package uk.ac.ic.bss.labbook.backend;

public class FileEntry {
	public String path;
	public String fileName;
	public boolean is_dir;
	public FileEntry(String path, String fileName, boolean is_dir) {
		this.path = path;
		this.fileName = fileName;
		this.is_dir = is_dir;
	}
	@Override
	public String toString() {
		return fileName;
	}
}

