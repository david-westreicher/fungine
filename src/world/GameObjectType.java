package world;

import java.util.Collection;
import java.util.HashMap;
import java.util.Map;

import physics.AbstractCollisionShape;
import rendering.GameObjectRenderer;
import rendering.RenderState;
import script.GameScript;

public class GameObjectType extends VariableHolder {
	private static Map<String, GameObjectType> allTypes = new HashMap<String, GameObjectType>();

	public GameObjectRenderer renderer = null;
	public GameScript script = null;
	public AbstractCollisionShape shape = null;
	public String name;

	public float shininess = (float) (Math.random() * 2000);
	public float reflective = 0;
	public boolean airShader = false;
	public RenderState renderState = new RenderState();

	public GameObjectType(String name) {
		allTypes.put(name, this);
		this.name = name;
	}

	public static GameObjectType getType(String name) {
		return allTypes.get(name);
	}

	@Override
	public String toString() {
		return "GameObjectType [renderer=" + renderer + ", script=" + script
				+ ", shape=" + shape + ", name=" + name + ", shininess="
				+ shininess + "]";
	}

	public static Collection<GameObjectType> getTypes() {
		return allTypes.values();
	}

}
