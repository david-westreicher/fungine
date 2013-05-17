package util;

import java.io.File;
import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class FolderWatcher extends RepeatedThread {
	private static final int MAX_COMPARE = 100;
	private File folder;
	private Map<String, Date> files;
	private List<FolderListener> listeners = new ArrayList<FolderListener>();

	public FolderWatcher(String file) {
		super(1000, Thread.MIN_PRIORITY, "FolderWatcherThread");
		folder = new File(file);
		if (!folder.exists()) {
			Log.err(this, "WARNING!!! Folder " + folder + " doesn't exist");
		}
		files = getHashMap(folder);
	}

	private void getHashMap(String str, File folder,
			Map<String, Date> savedFiles) {
		File[] files = folder.listFiles();
		for (File f : files) {
			String path = str + f.getName();
			if (f.isDirectory())
				getHashMap(path + File.separator, f, savedFiles);
			else
				savedFiles.put(path, new Date(f.lastModified()));
		}
	}

	private Map<String, Date> getHashMap(File folder) {
		Map<String, Date> files = new HashMap<String, Date>();
		getHashMap("", folder, files);
		return files;
	}

	@Override
	protected void executeRepeatedly() {
		check();
	}

	private void check() {
		Map<String, Date> newFiles = getHashMap(folder);
		Map<String, Date> tmp = new HashMap<String, Date>(files);
		files = new HashMap<String, Date>(newFiles);
		compare(tmp, newFiles);
	}

	private void compare(Map<String, Date> files1, Map<String, Date> files2) {
		int counter = 0;
		for (String s : files1.keySet()) {
			if (counter++ > MAX_COMPARE) {
				waitSomeTime();
				counter = 0;
			}
			Date inFirst = files1.get(s);
			Date inSec = files2.get(s);
			if (inSec == null)
				removed(s);
			else {
				if (inSec.compareTo(inFirst) > 0)
					changed(s);
				files2.remove(s);
			}
		}
		for (String s : files2.keySet()) {
			added(s);
		}
	}

	private void added(String s) {
		for (FolderListener f : listeners) {
			f.added(s);
		}
	}

	private void changed(String s) {
		for (FolderListener f : listeners) {
			f.changed(s);
		}
	}

	private void removed(String s) {
		for (FolderListener f : listeners) {
			f.removed(s);
		}
	}

	public void addFolderListener(FolderListener f) {
		listeners.add(f);
	}

	public void removeFolderListener(FolderListener f) {
		listeners.remove(f);
	}

	public void listCurrentFiles() {
		Log.log(this, "Files in " + folder.getAbsolutePath() + ":");
		for (String f : files.keySet()) {
			Log.log("\t" + f);
		}
	}
}
