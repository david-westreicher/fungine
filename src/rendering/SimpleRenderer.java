package rendering;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import javax.media.opengl.GL;
import javax.media.opengl.GL2;

import util.MathHelper;
import world.GameObject;
import world.GameObjectType;

public class SimpleRenderer extends RenderUpdater {
	private static final boolean USE_OBJECT_INTERP = false;
	private List<String> excludedGameObjects = new ArrayList<String>();

	@Override
	protected void renderObjects() {
		renderObjects(renderObjs);
	}

	@Override
	public void initShaderUniforms() {
	}

	@Override
	public void endShaderUniforms() {
	}

	public void renderObjects(Map<String, List<GameObject>> renderObjs) {
		for (String type : renderObjs.keySet()) {
			if (!excludedGameObjects.contains(type))
				renderObjects(type, renderObjs);
		}
	}

	protected void renderObjects(String type,
			Map<String, List<GameObject>> renderObjs) {
		GameObjectType goType = GameObjectType.getType(type);
		if (goType == null)
			return;
		GameObjectRenderer renderer = goType.renderer;
		if (renderer == null)
			return;
		List<GameObject> objs = renderObjs.get(type);
		if (objs != null) {
			renderer.init(gl);
			if (renderer.isSimple())
				renderer.draw(gl, objs, INTERP);
			else
				for (GameObject go : objs) {
					gl.glColor3f(go.color[0], go.color[1], go.color[2]);
					gl.glPushMatrix();
					transform(goType, go);

					gl.glMatrixMode(GL.GL_TEXTURE);
					gl.glActiveTexture(GL.GL_TEXTURE7);
					gl.glPushMatrix();
					transform(goType, go);
					gl.glActiveTexture(GL.GL_TEXTURE0);

					renderer.drawSimple(gl);

					gl.glPopMatrix();
					gl.glMatrixMode(GL2.GL_MODELVIEW);
					gl.glPopMatrix();

				}
			renderer.end(gl);
		}
	}

	private void transform(GameObjectType goType, GameObject go) {
		if (USE_OBJECT_INTERP) {
			float interp[] = MathHelper.interp(go.pos, go.oldPos, INTERP,
					SMOOTHSTEP_INTERP);
			gl.glTranslatef(interp[0], interp[1], interp[2]);
		} else
			gl.glTranslatef(go.pos[0], go.pos[1], go.pos[2]);
		if (goType.shape == null) {
			gl.glRotatef(MathHelper.toDegree(go.rotation[0]), -1, 0, 0);
			gl.glRotatef(MathHelper.toDegree(go.rotation[1]), 0, -1, 0);
			gl.glRotatef(MathHelper.toDegree(go.rotation[2]), 0, 0, -1);
		} else {
			gl.glMultMatrixf(MathHelper.to4x4Matrix(go.rotationMatrix), 0);
		}
		gl.glScalef(go.size[0], go.size[1], go.size[2]);
	}

	public void excludeGameObjectFromRendering(String string) {
		excludedGameObjects.add(string);
	}

	public void includeGameObjectFromRendering(String lightObjectTypeName) {
		excludedGameObjects.remove(lightObjectTypeName);
	}
}
