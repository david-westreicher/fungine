package rendering;

import java.nio.FloatBuffer;
import java.util.ArrayList;
import java.util.List;

import javax.media.opengl.GL2;
import javax.vecmath.Matrix4f;

import manager.UberManager;
import shader.Shader;
import shader.ShaderScript;
import util.Log;

public class TestSkinningRenderer extends RenderUpdater {
	public static class MatrixStack {
		private List<Matrix4f> matrices;

		public MatrixStack() {
			this.matrices = new ArrayList<Matrix4f>();
			matrices.add(new Matrix4f());
			matrices.get(0).setIdentity();
		}

		public void push() {
			Matrix4f clone = (Matrix4f) get().clone();
			matrices.add(clone);
		}

		public Matrix4f get() {
			return matrices.get(matrices.size() - 1);
		}

		public Matrix4f pop() {
			return matrices.remove(matrices.size() - 1);
		}

		public void reset() {
			while (matrices.size() > 1)
				matrices.remove(0);
			matrices.get(0).setIdentity();
		}
	}

	public static class Bone {
		private static int NUM;
		public Bone parent;
		public List<Bone> children;
		public float angle;
		public float length;
		public String name;
		private int index;
		private Matrix4f matrix = new Matrix4f();
		private static Matrix4f translation = new Matrix4f();
		private static Matrix4f tmpMatrix = new Matrix4f();
		private static MatrixStack stack = new MatrixStack();

		public Bone() {
			this((float) (Math.PI / 2), 1, null);
		}

		public Bone(float a, float l, Bone parent) {
			this.angle = a;
			this.length = l;
			this.parent = parent;
			this.children = new ArrayList<Bone>();
			this.name = "bone " + NUM++;
			matrix.setIdentity();
		}

		public Bone addChild(float a, float l) {
			Bone child = new Bone(a, l, this);
			children.add(child);
			return child;
		}

		@Override
		public String toString() {
			return "\n" + toString(0).toString();
		}

		public StringBuilder toString(int level) {
			StringBuilder sb = new StringBuilder();
			for (int i = 0; i < level; i++)
				sb.append("\t");
			sb.append("Bone [hasParent=" + (parent != null) + ", angle="
					+ angle + ", length=" + length + ", name=" + name + "]");
			for (Bone child : children) {
				sb.append("\n");
				sb.append(child.toString(level + 1));
			}
			return sb;
		}

		public void draw() {
			ShaderScript.setUniform("boneIndex", index);
			gl.glBegin(GL2.GL_QUADS);
			gl.glColor3f(1 - index, index, 0);
			gl.glVertex3f(0.05f, 0, 0);
			gl.glVertex3f(-0.05f, 0, 0);
			gl.glColor3f(1 - index, index, 0);
			gl.glVertex3f(-0.05f, length, 0);
			gl.glVertex3f(0.05f, length, 0);
			gl.glEnd();
		}

		public List<Bone> getBoneList() {
			List<Bone> list = new ArrayList<Bone>();
			addToBoneList(list);
			return list;
		}

		private void addToBoneList(List<Bone> list) {
			this.index = list.size();
			list.add(this);
			for (Bone b : children)
				b.addToBoneList(list);
		}

		public void calcucalteMatrices() {
			stack.reset();
			// translate whole object
			// translate(stack.get(), -0.5f);
			calculateMatrix();
		}

		private void calculateMatrix() {
			stack.push();
			Matrix4f m = stack.get();
			if (parent != null)
				translate(m, parent.length);

			rotate(m);
			for (Bone child : children)
				child.calculateMatrix();
			matrix.set(m);
			stack.pop();
		}

		private void translate(Matrix4f m, float length) {
			translation.setIdentity();
			translation.m13 = length;
			m.mul(translation);
		}

		private void rotate(Matrix4f m) {
			tmpMatrix.rotZ((float) (angle));
			m.mul(tmpMatrix);
		}

		public Matrix4f getBoneMatrix() {
			return matrix;
		}

	}

	private List<Bone> children = new ArrayList<Bone>();
	private ShaderScript skinning;
	public static FloatBuffer bonesUniform;
	private MatrixStack matrixStack;
	private List<Bone> bones;
	private ShaderScript transformSkinning;

	public TestSkinningRenderer() {
		bones = createBoneList();
		bonesUniform = FloatBuffer.allocate(16 * bones.size());
		matrixStack = new MatrixStack();
		Log.log(this, bones.get(0));
	}

	private List<Bone> createBoneList() {
		Bone b = new Bone(0, 1f, null);
		children.add(b);
		Bone child;
		children.add(child = b.addChild(0, 1f));
		children.add(child.addChild(0, 1f));
		return b.getBoneList();
	}

	@Override
	protected void renderObjects() {
		if (!UberManager.areShaderInitialized(Shader.values()))
			return;
		gl.glColor4f(1, 1, 1, 1);
		calculateBones();
		transformSkinning = UberManager.getShader(Shader.TRANSFORM_SKINNING);
		transformSkinning.execute(gl);
		super.renderObjects("Box", renderObjs);
		transformSkinning.end(gl);
		skinning = UberManager.getShader(Shader.SKINNING);
		skinning.execute(gl);
		ShaderScript.setUniformMatrix4("bones", bonesUniform, true);
		gl.glPolygonMode(GL2.GL_FRONT_AND_BACK, GL2.GL_LINE);
		gl.glDisable(GL2.GL_DEPTH_TEST);
		gl.glDisable(GL2.GL_CULL_FACE);
		gl.glPushMatrix();
		for (Bone b : bones)
			b.draw();
		gl.glPopMatrix();
		gl.glPolygonMode(GL2.GL_FRONT_AND_BACK, GL2.GL_FILL);
		gl.glEnable(GL2.GL_DEPTH_TEST);
		gl.glEnable(GL2.GL_CULL_FACE);
		skinning.end(gl);

		for (Bone child : children)
			child.angle += 0.0002f;
	}

	private void calculateBones() {
		bones.get(0).calcucalteMatrices();
		bonesUniform.rewind();
		for (Bone b : bones)
			for (int i = 0; i < 16; i++)
				bonesUniform.put(b.getBoneMatrix().getElement(i / 4, i % 4));
		bonesUniform.rewind();
	}
}
