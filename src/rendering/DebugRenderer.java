package rendering;

import java.util.ArrayList;

import javax.media.opengl.GL3;

import rendering.VBO.VertexAttribute;
import util.GLUtil;
import util.MathHelper;
import world.GameObject;

public class DebugRenderer {

	private static final int GRID_NUM = 20;
	private static final float GRID_SIZE = 1f;
	private RenderInformation debugRi;
	private ArrayList<GameObject> objs;

	public DebugRenderer(GL3 gl3) {
		debugRi = getMesh();
		objs = new ArrayList<GameObject>();
		GameObject go = new GameObject(null);
		objs.add(go);
	}

	public void render(GL3 gl, GLUtil glutil) {
		// debugRi.render(gl, glutil, gos);
		debugRi.render(gl, glutil, objs);
	}

	public void dispose(GL3 gl) {
		debugRi.dispose(gl);
	}

	private RenderInformation getMesh() {
		ArrayList<Float> vertices = new ArrayList<Float>();
		float translation = -GRID_NUM * GRID_SIZE / 2;
		for (float i = 1; i < GRID_NUM; i++) {
			vertices.add(i * GRID_SIZE + translation);
			vertices.add(0f);
			vertices.add(0f + translation);
			vertices.add(i * GRID_SIZE + translation);
			vertices.add(0f);
			vertices.add(GRID_NUM * GRID_SIZE + translation);
			vertices.add(0f + translation);
			vertices.add(0f);
			vertices.add(i * GRID_SIZE + translation);
			vertices.add(GRID_NUM * GRID_SIZE + translation);
			vertices.add(0f);
			vertices.add(i * GRID_SIZE + translation);
		}
		{
			vertices.add(0f);
			vertices.add(0f);
			vertices.add(0f);
			vertices.add(1f);
			vertices.add(0f);
			vertices.add(0f);

			vertices.add(0f);
			vertices.add(0f);
			vertices.add(0f);
			vertices.add(0f);
			vertices.add(1f);
			vertices.add(0f);

			vertices.add(0f);
			vertices.add(0f);
			vertices.add(0f);
			vertices.add(0f);
			vertices.add(0f);
			vertices.add(1f);
		}
		float[] cols = new float[vertices.size()];
		for (int i = 0; i < cols.length; i++)
			cols[i] = 0.5f;
		for (int i = cols.length - 6 * 3; i < cols.length; i++)
			cols[i] = vertices.get(i);
		RenderInformation ri = new RenderInformation();
		ri.addVA(VertexAttribute.POSITION, MathHelper.toArray(vertices), 3);
		ri.addVA(VertexAttribute.COLOR, cols, 3);
		ri.setDrawType(GL3.GL_LINES);
		return ri;
	}
}
