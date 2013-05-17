package test;

import settings.Settings;
import util.FolderListener;
import util.FolderWatcher;
import util.Log;
import util.RepeatedThread;

public class FolderTest implements FolderListener {
	private FolderWatcher f;

	public FolderTest() {
		f = new FolderWatcher(Settings.RESSOURCE_FOLDER);
		f.addFolderListener(this);
		new Thread(new Runnable() {

			@Override
			public void run() {
				try {
					f.listCurrentFiles();
					Thread.sleep(20000);
					RepeatedThread.stopAll();
				} catch (InterruptedException e) {
					e.printStackTrace();
				}

			}
		}).start();
	}

	@Override
	public void added(String s) {
		Log.log(s + " has been added!");
		f.listCurrentFiles();
	}

	@Override
	public void changed(String s) {
		Log.log(s + " has been changed!");
		f.listCurrentFiles();
	}

	@Override
	public void removed(String s) {
		Log.log(s + " has been removed! ");
		f.listCurrentFiles();
	}

	public static void main(String[] args) {
		new FolderTest();
	}
}
