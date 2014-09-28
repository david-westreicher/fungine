package world;

import java.util.Collection;
import java.util.HashMap;
import java.util.Map;

import javax.media.opengl.GL2;

import physics.AbstractCollisionShape;
import rendering.GLRunnable;
import rendering.RenderInformation;
import rendering.RenderState;
import rendering.RenderUpdater;
import script.JavaScript;

public class GameObjectType extends VariableHolder {
	private static Map<String, GameObjectType> allTypes = new HashMap<String, GameObjectType>();

	private String runtimeScript = null;
	public AbstractCollisionShape shape = null;
	public String name;
	public float shininess = (float) (Math.random() * 2000);
	public float reflective = 0;
	public boolean airShader = false;
	public RenderState renderState = new RenderState();
	public RenderInformation renderInformation = null;

	public GameObjectType(String name) {
		allTypes.put(name, this);
		this.name = name;
	}

	public static GameObjectType getType(String name) {
		return allTypes.get(name);
	}

	@Override
	public String toString() {
		return "GameObjectType [renderer=" + renderInformation + ", shape="
				+ shape + ", name=" + name + ", shininess=" + shininess + "]";
	}

	public static Collection<GameObjectType> getTypes() {
		return allTypes.values();
	}

	public String getRuntimeScript() {
		return runtimeScript;
	}

	public void setRuntimeScript(String runtimeScript) {
		this.runtimeScript = runtimeScript;
		JavaScript.loadIfNew(runtimeScript);
	}

	public void setRenderInformation(final RenderInformation ri) {
		RenderUpdater.executeInOpenGLContext(new GLRunnable() {

			@Override
			public void run(GL2 gl) {
				if (renderInformation != null)
					renderInformation.dispose(gl);
				renderInformation = ri;
			}
		});
	}

}
