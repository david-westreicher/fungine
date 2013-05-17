package manager;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public abstract class Manager<T> implements Manageable {
	public static Map<String, Manageable> manager = new HashMap<String, Manageable>();
	private Map<String, List<T>> savedObjects = new HashMap<String, List<T>>();
	static {
		new ScriptManager();
		new ShaderManager();
	}

	public Manager(String name) {
		manager.put(name, this);
	}

	public void update(String file, T t) {
		List<T> list = savedObjects.get(file);
		if (list == null) {
			list = new ArrayList<T>();
			savedObjects.put(file, list);
		}
		if (!list.contains(t)) {
			list.add(t);
		}
		updateObject(t, file);
	}

	protected abstract void updateObject(T t, String file);

	public void changed(String s) {
		List<T> list = savedObjects.get(s);
		if (list != null) {
			for (T t : list) {
				this.update(s, t);
			}
		}
	}

	public void restart() {
		savedObjects.clear();
	}

	public static void restartManager() {
		for (Manageable m : manager.values())
			m.restart();
	}

	public static Manageable getManager(String name) {
		return manager.get(name);
	}
}
