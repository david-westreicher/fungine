package game;

import java.io.File;

import manager.UberManager;
import script.JavaScript;
import settings.Settings;
import util.FolderListener;
import util.Log;
import util.Util;

public class GameWatcher implements FolderListener {

	private Game game;

	public GameWatcher(Game g) {
		this.game = g;
	}

	@Override
	public void added(String s) {
	}

	// TODO update to engine overhaul (main_script is a java file)
	@Override
	public void changed(String s) {
		Log.log(this, s + " changed");
		if (s.equals("scripts/" + Settings.MAIN_SCRIPT)) {
			GameLoop gl = game.loop;
			gl.startPause();
			Util.sleep(10);
			JavaScript.reset();
			Game.INSTANCE.world.clear();
			Game.INSTANCE.world.add(Game.INSTANCE.cam);
			JavaScript.execute(Settings.MAIN_SCRIPT, false);
			gl.endPause();
		} else {
			String folder = s.split(File.separator.equals("\\") ? "\\\\"
					: File.separator)[0];
			s = s.replace("\\", "/");
			// Log.log(this, folder);
			if (folder.equals("scripts")) {
				JavaScript.scriptChanged(s);
				// game.getManager("script").changed(s);
			} else if (folder.equals("img")) {
				UberManager.textureChanged(s);
			} else if (folder.equals("shader")) {
				UberManager.shaderChanged(s);
				// game.getManager("shader").changed(s);
			} else if (folder.equals("obj")) {
				GameLoop gl = game.loop;
				gl.startPause();
				Util.sleep(10);
				// TODO find gameobjectype renderInformation and update
				gl.endPause();
			}
		}
	}

	@Override
	public void removed(String s) {

	}

}
