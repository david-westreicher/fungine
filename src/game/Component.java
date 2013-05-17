package game;

import java.util.HashMap;
import java.util.Map;

public abstract class Component<T> {
	private Map<String, T> components = new HashMap<String, T>();
	private Component<?> parent = null;

	public Component() {
	}

	public Component(Component<?> parent) {
		this.parent = parent;
	}

	public Component<?> getParent() {
		return parent;
	}

	public void addComponent(String name, T c) {
		components.put(name, c);
	}

	public T getComponent(String name) {
		return components.get(name);
	}

	public void clear() {
		components.clear();
	}

}
