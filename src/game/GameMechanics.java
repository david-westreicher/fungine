package game;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import physics.AbstractPhysics;
import script.JavaScript;
import script.JavaScript.RuntimeScript;
import world.GameObject;

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
				.getAllObjectsTypes();
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

		for (RuntimeScript rs : JavaScript.getScripts()) {
			String typeName = rs.getGameObjectType();
			rs.update(typeName == null ? null : objs.get(typeName));
		}
		/*
		 * try { Script.executeFunction(Settings.MAIN_SCRIPT, "update",
		 * Game.INSTANCE); } catch (ScriptException e1) {
		 * Log.err(e1.getFileName()); e1.printStackTrace(); } catch
		 * (NoSuchMethodException e1) { e1.printStackTrace(); }
		 */
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
