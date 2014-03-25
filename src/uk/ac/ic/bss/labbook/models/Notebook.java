package uk.ac.ic.bss.labbook.models;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.util.Arrays;
import java.util.Properties;

public class Notebook {

	static enum Field { name, format, lastModified, tags };

	public String id;
	public File folder;

	public File properties;
	public File thumbs;
	public File picdir;
	public File bmpdir;
	public File screenshots;
	public File txtdir;
	public File audioMemo;
	public File videos;
	public File docs;

	protected Notebook(String id, File folder) {
		if (id == null || id.length() == 0) {
			throw new IllegalArgumentException("Invalid ID");
		}
		if (!folder.exists()) {
			throw new IllegalArgumentException("Folder does not exist");
		}

		this.id = id;
		this.folder = folder;

		properties = new File(folder, "properties.txt");
		thumbs = new File(folder, "Thumbs");
		picdir = new File(folder, "Picdir");
		bmpdir = new File(folder, "Bmpdir");
		screenshots = new File(folder, "Screenshots");
		txtdir = new File(folder, "Txtdir");
		audioMemo = new File(folder, "AudioMemo");
		videos = new File(folder, "Videos");
		docs = new File(folder, "Docs");

		thumbs.mkdir();
		picdir.mkdir();
		bmpdir.mkdir();
		screenshots.mkdir();
		txtdir.mkdir();
		audioMemo.mkdir();
		videos.mkdir();
		docs.mkdir();
	}

	public void setName(String name) {
		setProperty(properties, Field.name, name);
	}

	public String getName() {
		return getProperty(properties, Field.name);
	}

	public void setLastModified() {
		setProperty(properties, Field.lastModified, String.valueOf(getLastModified()));
	}

	public long getLastModified() {
		return lastModified(-1, folder);
	}

	public void delete() {
		delete(folder);
	}

	public String[] getTags() {
		String tags = getProperty(properties, Field.tags);
		return tags == null ? new String[0] : tags.split(" ");
	}

	public void addTag(String tag) {
		if (!Arrays.asList(getTags()).contains(tag)) {
			String tags = getProperty(properties, Field.tags);
			setProperty(properties, Field.tags, tags == null ? tag : String.format("%s %s", tags, tag));
		}
	}

	public void removeTag(String tag) {
		String[] tags = getTags();
		StringBuilder _tags = new StringBuilder();
		if (Arrays.asList(tags).contains(tag)) {
			for (String _tag : tags) {
				if (!_tag.equals(tag)) {
					_tags.append(_tag).append(" ");
				}
			}
		}
		setProperty(properties, Field.tags, _tags.toString().trim());
	}

	//utility methods

	protected static void setProperty(File file, Field field, String value) {
		try {
			Properties properties = new Properties();
			if (file.exists()) {
				properties.load(new FileInputStream(file));
			}
			if (value.length() == 0) {
				properties.remove(field.name());
			} else {
				properties.setProperty(field.name(), value);
			}
			properties.store(new FileOutputStream(file), null);
		} catch (IOException e) {
			throw new RuntimeException(e);
		}
	}

	protected static String getProperty(File file, Field field) {
		try {
			Properties properties = new Properties();
			properties.load(new FileInputStream(file));
			return properties.getProperty(field.name());
		} catch (IOException e) {
			throw new RuntimeException(e);
		}
	}

	protected static long lastModified(long modified, File d) {
		for (File f : d.listFiles()) {
			modified = Math.max(modified, f.isDirectory() ? lastModified(modified, f) : f.lastModified());
		}
		return modified;
	}

	protected static void delete(File file) {
		if (!file.delete()) {
			for (File child : file.listFiles()) {
				delete(child);
				file.delete();
			}
		}
	}

}
