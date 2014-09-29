package util;

import java.nio.FloatBuffer;
import java.util.Stack;

import javax.media.opengl.GL2;
import javax.vecmath.Matrix3f;
import javax.vecmath.Matrix4f;
import javax.vecmath.Vector3f;

public class GLUtil {
	public class MatrixStack {
		public Stack<Matrix4f> stack = new Stack<Matrix4f>();

		public MatrixStack() {
			stack.push(new Matrix4f());
			stack.peek().setIdentity();
		}
	}

	private static Matrix4f tmpM = new Matrix4f();
	private static FloatBuffer tmpFB = FloatBuffer.allocate(16);
	private static float[] tmpArr = new float[16];
	private int currentMatrixStack;
	private static MatrixStack[] stacks = new MatrixStack[3];

	public GLUtil() {
		currentMatrixStack = 0;
		for (int i = 0; i < stacks.length; i++)
			stacks[i] = new MatrixStack();
	}

	public void glMatrixMode(int newMode) {
		switch (newMode) {
		case GL2.GL_PROJECTION:
			currentMatrixStack = 0;
			break;
		case GL2.GL_MODELVIEW:
			currentMatrixStack = 1;
			break;

		default:
			break;
		}
	}

	public void glLoadIdentity() {
		stacks[currentMatrixStack].stack.peek().setIdentity();
	}

	public void glFrustum(float fW, float fH, float near, float far) {
		// gl.glFrustum(-fW, fW, -fH, fH, zNear, zFar);
		// left right bottom top near far
		// float left = -fW;
		// float right = fW;
		// float bottom = -fH;
		// float top = fH;
		// float A = (right + left) / (right - left);
		// float B = (top + bottom) / (top - bottom);
		// float C = -(far + near) / (far - near);
		// float D = -(far * near * 2) / (far - near);
		float A = 0;
		float B = 0;
		float C = -(far + near) / (far - near);
		float D = -(far * near * 2) / (far - near);
		float E = near / fW;
		float F = near / fH;
		Matrix4f frustumMatrix = tmpM;
		frustumMatrix.setRow(0, E, 0, A, 0);
		frustumMatrix.setRow(1, 0, F, B, 0);
		frustumMatrix.setRow(2, 0, 0, C, D);
		frustumMatrix.setRow(3, 0, 0, -1, 0);
		Matrix4f current = stacks[currentMatrixStack].stack.peek();
		current.mul(frustumMatrix);
	}

	public void glTranslatef(float x, float y, float z) {
		Matrix4f translationMatrix = tmpM;
		translationMatrix.setIdentity();
		translationMatrix.setColumn(3, x, y, z, 1);
		Matrix4f current = stacks[currentMatrixStack].stack.peek();
		current.mul(translationMatrix);
	}

	public void glPushMatrix() {
		Matrix4f current = stacks[currentMatrixStack].stack.peek();
		Matrix4f newMatrix = (Matrix4f) current.clone();
		stacks[currentMatrixStack].stack.push(newMatrix);
	}

	public void glPopMatrix() {
		stacks[currentMatrixStack].stack.pop();
	}

	public void gluOrtho2D(float left, float right, float bottom, float top) {
		// http://pyopengl.sourceforge.net/documentation/manual-3.0/gluOrtho2D.html
		glOrtho(left, right, bottom, top, -1, 1);
	}

	public void glOrtho(float left, float right, float bottom, float top,
			float near, float far) {
		// http://pyopengl.sourceforge.net/documentation/manual-3.0/glOrtho.html
		Matrix4f orthoMatrix = tmpM;
		float x = -(right + left) / (right - left);
		float y = -(top + bottom) / (top - bottom);
		float z = -(far + near) / (far - near);
		orthoMatrix.setRow(0, 2.0f / (right - left), 0, 0, x);
		orthoMatrix.setRow(1, 0, 2.0f / (top - bottom), 0, y);
		orthoMatrix.setRow(2, 0, 0, -2.0f / (far - near), z);
		orthoMatrix.setRow(3, 0, 0, 0, 1);
		Matrix4f current = stacks[currentMatrixStack].stack.peek();
		current.mul(orthoMatrix);
	}

	public void checkSanity() {
		for (int i = 0; i < stacks.length; i++)
			if (stacks[i].stack.size() != 1)
				Log.err(this, "Stack " + i + " has not size 1 but "
						+ stacks[i].stack.size());
	}

	// TODO optimize
	public void gluLookAt(float eyeX, float eyeY, float eyeZ, float centerX,
			float centerY, float centerZ, float upX, float upY, float upZ) {
		Vector3f f = new Vector3f(centerX - eyeX, centerY - eyeY, centerZ
				- eyeZ);
		f.normalize();
		Vector3f up = new Vector3f(upX, upY, upZ);
		up.normalize();
		Vector3f s = new Vector3f();
		s.cross(f, up);
		Vector3f u = new Vector3f();
		u.cross(s, f);
		Matrix4f m = tmpM;
		m.setRow(0, s.x, s.y, s.z, 0);
		m.setRow(1, u.x, u.y, u.z, 0);
		m.setRow(2, -f.x, -f.y, -f.z, 0);
		m.setRow(3, 0, 0, 0, 1);
		Matrix4f current = stacks[currentMatrixStack].stack.peek();
		current.mul(m);
		glTranslatef(-eyeX, -eyeY, -eyeZ);
	}

	public FloatBuffer getModelViewProjection() {
		// Model view projection
		Matrix4f mvp = tmpM;
		mvp.set(stacks[0].stack.peek());
		mvp.mul(stacks[1].stack.peek());
		set(tmpFB, mvp);
		return tmpFB;
	}

	private void set(FloatBuffer tmpFB, Matrix4f m) {
		tmpFB.put(m.m00);
		tmpFB.put(m.m01);
		tmpFB.put(m.m02);
		tmpFB.put(m.m03);
		tmpFB.put(m.m10);
		tmpFB.put(m.m11);
		tmpFB.put(m.m12);
		tmpFB.put(m.m13);
		tmpFB.put(m.m20);
		tmpFB.put(m.m21);
		tmpFB.put(m.m22);
		tmpFB.put(m.m23);
		tmpFB.put(m.m30);
		tmpFB.put(m.m31);
		tmpFB.put(m.m32);
		tmpFB.put(m.m33);
		tmpFB.rewind();
	}

	private void set(float[] tmpArr, Matrix4f m) {
		tmpArr[0] = m.m00;
		tmpArr[1] = m.m01;
		tmpArr[2] = m.m02;
		tmpArr[3] = m.m03;
		tmpArr[4] = m.m10;
		tmpArr[5] = m.m11;
		tmpArr[6] = m.m12;
		tmpArr[7] = m.m13;
		tmpArr[8] = m.m20;
		tmpArr[9] = m.m21;
		tmpArr[10] = m.m22;
		tmpArr[11] = m.m23;
		tmpArr[12] = m.m30;
		tmpArr[13] = m.m31;
		tmpArr[14] = m.m32;
		tmpArr[15] = m.m33;
	}

	public void gluPerspective(float fovY, float aspect, float zNear, float zFar) {
		float fH = (float) (Math.tan(fovY / 360 * Math.PI) * zNear);
		float fW = fH * aspect;
		// glu.gluPerspective(fov_y, (float) width / height, ZNear, ZFar);
		this.glFrustum(fW, fH, zNear, zFar);
	}

	public void scale(float x, float y, float z) {
		Matrix4f scaleMatrix = tmpM;
		scaleMatrix.setIdentity();
		scaleMatrix.m00 = x;
		scaleMatrix.m11 = y;
		scaleMatrix.m22 = z;
		Matrix4f current = stacks[currentMatrixStack].stack.peek();
		current.mul(scaleMatrix);
	}

	public void multiply(Matrix3f rotationMatrix) {
		Matrix4f current = stacks[currentMatrixStack].stack.peek();
		tmpM.setIdentity();
		tmpM.set(rotationMatrix);
		current.mul(tmpM);
	}

}
