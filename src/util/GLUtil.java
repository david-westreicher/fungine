package util;

import java.nio.FloatBuffer;

import javax.media.opengl.GL2;
import javax.vecmath.Matrix3f;
import javax.vecmath.Matrix4f;
import javax.vecmath.Vector3f;

public class GLUtil {

	private static Matrix4f tmpM = new Matrix4f();
	private static FloatBuffer tmpFB = FloatBuffer.allocate(16);
	private static float[] tmpArr = new float[16];
	private int currentMatrixStack;
	private static Matrix4f[][] stacks = new Matrix4f[3][10];
	private static int[] stackPointers = new int[3];

	public GLUtil() {
		currentMatrixStack = 0;
		for (int i = 0; i < stacks.length; i++)
			for (int j = 0; j < stacks[0].length; j++) {
				stacks[i][j] = new Matrix4f();
				stacks[i][j].setIdentity();
			}
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
		stacks[currentMatrixStack][stackPointers[currentMatrixStack]]
				.setIdentity();
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
		Matrix4f current = stacks[currentMatrixStack][stackPointers[currentMatrixStack]];
		current.mul(frustumMatrix);
	}

	public void glTranslatef(float x, float y, float z) {
		Matrix4f translationMatrix = tmpM;
		translationMatrix.setIdentity();
		translationMatrix.setColumn(3, x, y, z, 1);
		Matrix4f current = stacks[currentMatrixStack][stackPointers[currentMatrixStack]];
		current.mul(translationMatrix);
	}

	public void glPushMatrix() {
		Matrix4f current = stacks[currentMatrixStack][stackPointers[currentMatrixStack]];
		stackPointers[currentMatrixStack]++;
		stacks[currentMatrixStack][stackPointers[currentMatrixStack]]
				.set(current);
	}

	public void glPopMatrix() {
		stackPointers[currentMatrixStack]--;
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
		Matrix4f current = stacks[currentMatrixStack][stackPointers[currentMatrixStack]];
		current.mul(orthoMatrix);
	}

	public void checkSanity() {
		for (int i = 0; i < stacks.length; i++)
			if (stackPointers[i] != 0)
				Log.err(this, "Stack " + i + " has not size 1 but "
						+ stackPointers[i]);
	}

	private static final Vector3f f = new Vector3f();
	private static final Vector3f up = new Vector3f();
	private static final Vector3f s = new Vector3f();
	private static final Vector3f u = new Vector3f();

	// TODO optimize
	public void gluLookAt(float eyeX, float eyeY, float eyeZ, float centerX,
			float centerY, float centerZ, float upX, float upY, float upZ) {
		f.set(centerX - eyeX, centerY - eyeY, centerZ - eyeZ);
		up.set(upX, upY, upZ);
		f.normalize();
		up.normalize();
		s.cross(f, up);
		u.cross(s, f);
		Matrix4f m = tmpM;
		m.setRow(0, s.x, s.y, s.z, 0);
		m.setRow(1, u.x, u.y, u.z, 0);
		m.setRow(2, -f.x, -f.y, -f.z, 0);
		m.setRow(3, 0, 0, 0, 1);
		Matrix4f current = stacks[currentMatrixStack][stackPointers[currentMatrixStack]];
		current.mul(m);
		glTranslatef(-eyeX, -eyeY, -eyeZ);
	}

	public FloatBuffer getModelViewProjection() {
		// Model view projection
		Matrix4f mvp = tmpM;
		mvp.set(stacks[0][stackPointers[0]]);
		mvp.mul(stacks[1][stackPointers[1]]);
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
		Matrix4f current = stacks[currentMatrixStack][stackPointers[currentMatrixStack]];
		current.mul(scaleMatrix);
	}

	public void multiply(Matrix3f rotationMatrix) {
		Matrix4f current = stacks[currentMatrixStack][stackPointers[currentMatrixStack]];
		tmpM.setIdentity();
		tmpM.set(rotationMatrix);
		current.mul(tmpM);
	}

	public FloatBuffer getMatrix(int mode) {
		// TODO optimize
		FloatBuffer fb = FloatBuffer.allocate(16);
		mode = (mode == GL2.GL_PROJECTION ? 0 : 1);
		set(fb, stacks[mode][stackPointers[mode]]);
		return fb;
	}
}
