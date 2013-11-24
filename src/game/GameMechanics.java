package game;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import javax.script.ScriptException;

import physics.AbstractPhysics;
import script.JavaScript;
import script.JavaScript.RuntimeScript;
import script.Script;
import settings.Settings;
import util.Log;
import world.GameObject;
import world.GameObjectType;

public class GameMechanics implements Updatable {

	public AbstractPhysics physics;
	private boolean restartPhysics = false;
	private List<Runnable> runnables = new ArrayList<Runnable>();

	public GameMechanics() {
	}

	@Override
	public void dispose() {
		if (physics != null)
			physics.dispose();
	}

	@Override
	public void update(float interp) {

		Map<String, List<GameObject>> objs = Game.INSTANCE.world
				.getAllObjects();
		final int tick = Game.INSTANCE.loop.tick;
		for (Runnable r : runnables)
			r.run();
		runnables.clear();

		Game.INSTANCE.input.update();

		for (String type : objs.keySet()) {
			for (GameObject go : objs.get(type)) {
				go.beforeUpdate();
			}
		}

		for (String type : objs.keySet()) {
			GameObjectType goType = GameObjectType.getType(type);
			RuntimeScript rt = JavaScript.getScript(goType.runtimeScript,
					goType);
			if (rt != null)
				rt.update(objs.get(type));
			/*
			 * GameScript script = goType.script; CompiledScript cScript = null;
			 * if (script != null) { cScript = script.script;
			 * cScript.getEngine().put("objects", objs.get(type)); try {
			 * cScript.eval(); } catch (ScriptException e) { Log.err(this,
			 * goType.name); System.err.println(e.getMessage());
			 * e.printStackTrace(); } }
			 */
		}

		try {
			Script.executeFunction(Settings.MAIN_SCRIPT, "update",
					Game.INSTANCE);
		} catch (ScriptException e1) {
			Log.err(e1.getFileName());
			e1.printStackTrace();
		} catch (NoSuchMethodException e1) {
			e1.printStackTrace();
		}
		if (physics != null) {
			if (restartPhysics) {
				physics.restart();
				restartPhysics = false;
			}
			physics.update(objs);
		}
		if (Game.DEBUG)
			for (String type : objs.keySet()) {
				for (GameObject go : objs.get(type)) {
					go.updateBbox();
				}
			}

		Game.INSTANCE.input.mouse.reset();
	}

	public void restart() {
		restartPhysics = true;
	}

	public void addRunnable(Runnable runnable) {
		runnables.add(runnable);
	}
}
