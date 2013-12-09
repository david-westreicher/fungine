package game;

import java.io.File;

import javax.media.opengl.GL2;
import javax.script.ScriptException;

import manager.UberManager;
import rendering.GLRunnable;
import rendering.RenderUpdater;
import rendering.model.ModelRenderer;
import script.JavaScript;
import script.Script;
import settings.Settings;
import util.Factory;
import util.FolderListener;
import util.Log;
import util.Util;
import world.GameObjectType;
import world.World;

public class GameWatcher implements FolderListener {

	private Game game;

	public GameWatcher(Game g) {
		this.game = g;
	}

	@Override
	public void added(String s) {
	}

	@Override
	public void changed(String s) {
		Log.log(this, s + " changed");
		if (s.equals(Settings.INIT_SCRIPT)) {
			game.restart();
		} else if (s.equals(Settings.MAIN_SCRIPT)) {
			Script.scripts.remove(Settings.MAIN_SCRIPT);
			GameLoop gl = game.loop;
			gl.startPause();
			Util.sleep(10);
			JavaScript.reset();
			Game.INSTANCE.world = new World();
			Game.INSTANCE.world.add(Game.INSTANCE.cam);
			try {
				Script.executeFunction(Settings.MAIN_SCRIPT, "init",
						Game.INSTANCE, Factory.INSTANCE);
			} catch (ScriptException e) {
				e.printStackTrace();
			} catch (NoSuchMethodException e) {
				e.printStackTrace();
			}
			gl.endPause();
		} else if (s.equals(Settings.OBJECTS_XML)) {
			GameLoop gl = game.loop;
			UberManager.clear();
			Util.sleep(10);
			gl.startPause();
			Util.sleep(10);
			game.parseXML();
			gl.endPause();
		} else {
			String folder = s.split(File.separator.equals("\\") ? "\\\\"
					: File.separator)[0];
			s = s.replace("\\", "/");
			Log.log(this, folder);
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
				for (final GameObjectType go : GameObjectType.getTypes()) {
					if (go.renderer != null && go.renderer.getName().equals(s)) {
						final ModelRenderer newModel = new ModelRenderer(s,
								false);
						RenderUpdater.executeInOpenGLContext(new GLRunnable() {
							@Override
							public void run(GL2 gl) {
								go.renderer = newModel;
							}
						});
					}
				}
				gl.endPause();
			}
		}
	}

	@Override
	public void removed(String s) {

	}

}
