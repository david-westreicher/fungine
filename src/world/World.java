package world;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class World {
	private List<GameObject> gameObjects = new ArrayList<GameObject>();
	private Map<String, List<GameObject>> visibleObjs = new HashMap<String, List<GameObject>>();
	private Map<String, List<GameObject>> allObjs = new HashMap<String, List<GameObject>>();

	public World() {

	}

	public void add(GameObject go) {
		gameObjects.add(go);
		add(go, visibleObjs);
		add(go, allObjs);
	}

	public void remove(GameObject go) {
		gameObjects.remove(go);
		remove(go, visibleObjs);
		remove(go, allObjs);
	}

	private void add(GameObject go, Map<String, List<GameObject>> mapList) {
		List<GameObject> list = mapList.get(go.getType());
		if (list == null) {
			list = new ArrayList<GameObject>();
			mapList.put(go.getType(), list);
		}
		list.add(go);
	}

	private void remove(GameObject go, Map<String, List<GameObject>> mapList) {
		List<GameObject> list = mapList.get(go.getType());
		if (list != null)
			list.remove(go);
	}

	public void clear() {
		gameObjects.clear();
		visibleObjs.clear();
		allObjs.clear();
	}

	public Map<String, List<GameObject>> getVisibleObjects() {
		// TODO add only visibles
		return visibleObjs;
	}

	public Map<String, List<GameObject>> getAllObjectsTypes() {
		return allObjs;
	}

	public int getObjectNum() {
		return gameObjects.size();
	}

	public void mark(int i) {
		for (int k = 0; k < gameObjects.size(); k++) {
			gameObjects.get(k).marked = k == i;
		}
	}

	public List<GameObject> getAllObjects() {
		return gameObjects;
	}
}
