package rendering;

import java.util.List;

import javax.media.opengl.GL2;

import util.Material;
import world.GameObject;

public abstract class GameObjectRenderer {

	private boolean isSimple;

	public GameObjectRenderer(boolean simple) {
		isSimple = simple;
	}

	public GameObjectRenderer() {
		this(false);
	}

	public abstract void init(GL2 gl);

	public abstract void draw(GL2 gl);

	public abstract void end(GL2 gl);

	public void draw(GL2 gl, List<GameObject> objs, float interp) {
	}

	public boolean isSimple() {
		return isSimple;
	}

	public abstract void drawSimple(GL2 gl);

	public List<Material> getMaterials() {
		return null;
	}

	public String getName() {
		return this.getClass().getSimpleName();
	}

}
