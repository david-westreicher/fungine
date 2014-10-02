package rendering;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import javax.media.opengl.GL3;

import rendering.VBO.VertexAttribute;
import util.GLUtil;
import util.MathHelper;
import util.Util;
import world.GameObject;

public class DebugRenderer {

	private RenderInformation debugRi;
	private ArrayList<GameObject> objs;

	public DebugRenderer(GL3 gl3) {
		debugRi = getMesh();
		objs = new ArrayList<GameObject>();
		GameObject go = new GameObject(null);
		go.beforeUpdate();
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
		int verticesNum = 100000;
		float[] vers = new float[verticesNum * 3];
		float[] cols = new float[verticesNum * 3];
		float[] pos = new float[3];
		Util.random.setSeed(0);
		for (int i = 0; i < vers.length; i += 3) {
			float[] color = new float[3];
			float hue = (float) i / (vers.length);
			MathHelper.HSLtoRGB(hue, 0.5f, 0.5f, color);
			for (int k = 0; k < 3; k++) {
				pos[k] += Util.random.nextFloat() - 0.5f;
				vers[i + k] = pos[k];
				cols[i + k] = color[k] * 0.8f;
			}
		}
		RenderInformation ri = new RenderInformation();
		ri.addVA(VertexAttribute.POSITION, vers, 3);
		ri.addVA(VertexAttribute.COLOR, cols, 3);
		ri.setDrawType(GL3.GL_TRIANGLE_STRIP);
		return ri;
	}
}
