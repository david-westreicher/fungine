package game;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import physics.AbstractPhysics;
import script.JavaScript;
import script.JavaScript.GotScript;
import util.WorkerPool.WorkerImpl;
import world.GameObject;

public class GameMechanics implements Updatable {

	public AbstractPhysics physics;
	private boolean restartPhysics = false;
	private List<Runnable> runnables = new ArrayList<Runnable>();
	private static final WorkerImpl beforeUpdater = new WorkerImpl() {

		@Override
		public void update(List<GameObject> gos, int subListStart,
				int subListEnd) {
			for (int i = subListStart; i < subListEnd; i++)
				gos.get(i).beforeUpdate();
		}
	};
	private static final WorkerImpl bboxUpdater = new WorkerImpl() {

		@Override
		public void update(List<GameObject> gos, int subListStart,
				int subListEnd) {
			for (int i = subListStart; i < subListEnd; i++)
				gos.get(i).updateBbox();
		}
	};

	@Override
	public void dispose() {
		if (physics != null)
			physics.dispose();
	}

	@Override
	public void update(float interp) {
		for (Runnable r : runnables)
			r.run();

		List<GameObject> objs = Game.INSTANCE.world.getAllObjects();
		Map<String, List<GameObject>> gotObjs = Game.INSTANCE.world
				.getAllObjectsTypes();
		runnables.clear();

		Game.INSTANCE.input.update();

		Game.workerPool.execute(objs, beforeUpdater);

		for (GotScript rs : JavaScript.getScripts())
			rs.update();

		if (physics != null) {
			if (restartPhysics) {
				physics.restart();
				restartPhysics = false;
			}
			physics.update(gotObjs);
		}
		if (Game.DEBUG)
			Game.workerPool.execute(objs, bboxUpdater);

		Game.INSTANCE.input.mouse.reset();
	}

	public void restart() {
		restartPhysics = true;
	}

	public void addRunnable(Runnable runnable) {
		runnables.add(runnable);
	}
}
