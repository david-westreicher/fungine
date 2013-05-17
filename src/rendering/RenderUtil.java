package rendering;

import javax.media.opengl.GL2;

public class RenderUtil {

	public static void drawRec(float[] bbox, GL2 gl) {
		gl.glVertex3f(bbox[0], bbox[1], 0);
		gl.glVertex3f(bbox[2], bbox[1], 0);
		gl.glVertex3f(bbox[2], bbox[3], 0);
		gl.glVertex3f(bbox[0], bbox[3], 0);
	}

	public static void drawSphere(float x, float y, float radius,
			float[] color, GL2 gl) {

		int i;
		int sections = 20; // number of triangles to use to estimate a circle
		// (a higher number yields a more perfect circle)
		float twoPi = 2.0f * (float) Math.PI;

		gl.glBegin(GL2.GL_TRIANGLE_FAN);
		gl.glVertex2d(0.0, 0.0); // origin
		if (color != null)
			gl.glColor4f(color[0], color[1], color[2], 0);
		for (i = 0; i <= sections; i++) {
			// make $section number of circles
			gl.glVertex2d(radius * Math.cos(i * twoPi / sections), radius
					* Math.sin(i * twoPi / sections));
		}
		gl.glEnd();

	}

	public static void drawSphere(float pos[], float radius, float[] color,
			GL2 gl, boolean b) {
		if (color != null)
			gl.glColor3fv(color, 0);
		gl.glPushMatrix();
		gl.glTranslatef(pos[0], pos[1], pos[2]);
		if (b)
			RenderUpdater.glut.glutWireSphere(radius, 5, 5);
		else
			RenderUpdater.glut.glutSolidSphere(radius, 10, 10);
		gl.glPopMatrix();

	}

	public static void drawLinedBox(float[] b, GL2 gl) {
		gl.glVertex3f(b[0], b[2], b[4]);
		gl.glVertex3f(b[0], b[2], b[5]);

		gl.glVertex3f(b[0], b[2], b[5]);
		gl.glVertex3f(b[0], b[3], b[5]);

		gl.glVertex3f(b[0], b[3], b[5]);
		gl.glVertex3f(b[1], b[3], b[5]);

		gl.glVertex3f(b[1], b[3], b[5]);
		gl.glVertex3f(b[1], b[3], b[4]);

		gl.glVertex3f(b[1], b[3], b[4]);
		gl.glVertex3f(b[1], b[2], b[4]);

		gl.glVertex3f(b[1], b[2], b[4]);
		gl.glVertex3f(b[0], b[2], b[4]);

	}

	public static void gluPerspective(GL2 gl, double fovY, double aspect,
			double zNear, double zFar) {
		double fH = Math.tan(fovY / 360 * Math.PI) * zNear;
		double fW = fH * aspect;
		// glu.gluPerspective(fov_y, (float) width / height, ZNear, ZFar);
		gl.glFrustum(-fW, fW, -fH, fH, zNear, zFar);
	}
}
