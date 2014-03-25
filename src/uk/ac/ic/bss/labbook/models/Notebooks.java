package uk.ac.ic.bss.labbook.models;

import java.io.File;
import java.util.Arrays;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

import uk.ac.ic.bss.labbook.LabBook;
import uk.ac.ic.bss.labbook.models.Notebook.Field;

public class Notebooks {

	private static final int FORMAT = 1;

	protected static Map<String, Notebook> notebooks = new HashMap<String, Notebook>();
	public static Notebook get(File folder) {
		if (!folder.exists()) {
			throw new IllegalArgumentException("Folder does not exist");
		}
		if (notebooks.containsKey(folder) && !isNotebook(folder)) {
			//notebook may have been deleted since cached
			notebooks.remove(folder);
			return null;
		}
		Notebook notebook = notebooks.get(folder.getName());
		if (notebook == null) {
			notebook = new Notebook(folder.getName(), folder);
			notebooks.put(folder.getName(), notebook);
		}
		return notebook;
	}

	public static boolean isNotebook(File folder) {
		return new File(folder, "properties.txt").exists();
	}

	public static Notebook create(File folder) {
		if (folder.exists()) {
			throw new IllegalArgumentException("Folder already exists");
		}

		Set<String> names = new HashSet<String>();
		for (File file : folder.getParentFile().listFiles()) {
			names.add(get(file).getName());
		}
		String name;
		int i = 0;
		do {
			name = String.format("Experiment %s", i++);
		} while (names.contains(name));
		folder.mkdir();

		Notebook notebook = get(folder);
		notebook.setName(name);
		Notebook.setProperty(notebook.properties, Field.format, String.valueOf(FORMAT));
		return notebook;
	}

	public static String[] getTags() {
		Set<String> tags = new HashSet<String>();
		for (File folder : LabBook.labBookDir.listFiles()) {
			tags.addAll(Arrays.asList(get(folder).getTags()));
		}
		return tags.toArray(new String[0]);
	}

}
