package util;

import game.Game;

import java.awt.Color;
import java.awt.Cursor;
import java.awt.Dimension;
import java.awt.Graphics2D;
import java.awt.GraphicsDevice;
import java.awt.GraphicsEnvironment;
import java.awt.Point;
import java.awt.Toolkit;
import java.awt.event.WindowAdapter;
import java.awt.event.WindowEvent;
import java.awt.image.BufferedImage;
import java.io.File;
import java.io.IOException;
import java.util.GregorianCalendar;

import javax.media.opengl.GL2;
import javax.swing.JFrame;
import javax.vecmath.Matrix3f;

import manager.UberManager;
import settings.Settings;

import com.jogamp.opengl.util.texture.Texture;

public class Util {

	public static JFrame frame;
	private static float[] eyeVector = new float[3];

	public static void sleep(long l) {
		try {
			Thread.sleep(l);
		} catch (InterruptedException e) {
			e.printStackTrace();
		}
	}

	public static JFrame createFrame() {
		// TODO fullscreen :D
		frame = new JFrame("Engine Test");

		GraphicsDevice gd = GraphicsEnvironment.getLocalGraphicsEnvironment()
				.getDefaultScreenDevice();
		if (Settings.USE_FULL_SCREEN && gd.isFullScreenSupported()) {
			Log.log(Util.class, "fullscreen supported");
			frame.setUndecorated(true);
			gd.setFullScreenWindow(frame);
		} else {
			// frame.setSize((Settings.STEREO ? Settings.WIDTH * 2
			// : Settings.WIDTH) + 20, Settings.HEIGHT + 40);
			frame.setLocationRelativeTo(null);
			frame.setResizable(true);
		}

		frame.addWindowListener(new WindowAdapter() {
			public void windowClosing(WindowEvent e) {
				super.windowClosing(e);
				Game.INSTANCE.exit();
				frame.dispose();
				// System.exit(0);
				// frame.setVisible(false);
				// frame.dispose();
				// frame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
				if (System.getProperty("os.name").toLowerCase()
						.contains("windows"))
					new Thread(new Runnable() {
						@Override
						public void run() {
							sleep(2000);
							Log.log(this, "System exit");
							try {
								Runtime.getRuntime().exec(
										"taskkill -IM javaw.exe -F");
							} catch (IOException e) {
								e.printStackTrace();
							}
						}
					}).start();
				else
					new Thread(new Runnable() {
						@Override
						public void run() {
							Util.sleep(2000);
							System.exit(0);
						}
					}).start();
			}
		});
		return frame;
	}

	public static float roundDigits(float i, int j) {
		double ten = Math.pow(10, j);
		return (float) (Math.round(i * ten) / ten);
	}

	public static Cursor getHiddenCursor() {
		Toolkit toolkit = Toolkit.getDefaultToolkit();
		Dimension dim = toolkit.getBestCursorSize(1, 1);
		BufferedImage cursorImg = new BufferedImage(dim.width, dim.height,
				BufferedImage.TYPE_INT_ARGB);
		Graphics2D g2d = cursorImg.createGraphics();
		g2d.setBackground(new Color(0.0f, 0.0f, 0.0f, 0.0f));
		g2d.clearRect(0, 0, dim.width, dim.height);
		g2d.dispose();
		return toolkit.createCustomCursor(cursorImg, new Point(0, 0),
				"hiddenCursor");
	}

	public static Object getFileType(String file) {
		String[] strings = file.split("\\.");
		return strings[strings.length - 1];
	}

	public static void toEuler(float x, float y, float z, float angle,
			float[] out) {
		double s = Math.sin(angle);
		double c = Math.cos(angle);
		double t = 1 - c;
		// if axis is not already normalised then uncomment this
		// double magnitude = Math.sqrt(x*x + y*y + z*z);
		// if (magnitude==0) throw error;
		// x /= magnitude;
		// y /= magnitude;
		// z /= magnitude;
		if ((x * y * t + z * s) > 0.998) { // north pole singularity detected
			out[1] = (float) (2 * Math.atan2(x * Math.sin(angle / 2),
					Math.cos(angle / 2)));
			out[2] = (float) (Math.PI / 2);
			out[0] = 0;
			return;
		}
		if ((x * y * t + z * s) < -0.998) { // south pole singularity detected
			out[1] = (float) (-2 * Math.atan2(x * Math.sin(angle / 2),
					Math.cos(angle / 2)));
			out[2] = (float) (-Math.PI / 2);
			out[0] = 0;
			return;
		}
		out[1] = (float) Math.atan2(y * s - x * z * t, 1 - (y * y + z * z) * t);
		out[2] = (float) Math.asin(x * y * t + z * s);
		out[0] = (float) Math.atan2(x * s - y * z * t, 1 - (x * x + z * z) * t);
	}

	public static void toEuler(Matrix3f m, float[] rotation) {
		if (m.m10 > 0.998) { // singularity at north pole
			rotation[1] = (float) Math.atan2(m.m02, m.m22);
			rotation[2] = (float) (Math.PI / 2);
			rotation[0] = 0;
			return;
		}
		if (m.m10 < -0.998) { // singularity at south pole
			rotation[1] = (float) Math.atan2(m.m02, m.m22);
			rotation[2] = (float) (-Math.PI / 2);
			rotation[0] = 0;
			return;
		}
		rotation[1] = (float) Math.atan2(-m.m20, m.m00);
		rotation[2] = (float) Math.atan2(-m.m12, m.m11);
		rotation[0] = (float) Math.asin(m.m10);
	}

	public static float[] eyeVector(float[] rot) {
		eyeVector[0] = (float) (Math.cos(rot[0]) * Math.cos(rot[1]));
		eyeVector[1] = (float) (Math.sin(rot[0]) * Math.cos(rot[1]));
		eyeVector[2] = (float) (Math.sin(rot[1]));
		return eyeVector;
	}

	public static void drawTexture(GL2 gl, String texture, float w, float h) {
		Texture text = UberManager.getTexture(texture);
		if (text != null) {
			text.bind(gl);
			gl.glBegin(GL2.GL_QUADS);
			gl.glTexCoord2f(0.0f, 0.0f);
			gl.glVertex2f(w, h);
			gl.glTexCoord2f(1.0f, 0.0f);
			gl.glVertex2f(-w, h);
			gl.glTexCoord2f(1.0f, 1.0f);
			gl.glVertex2f(-w, -h);
			gl.glTexCoord2f(0.0f, 1.0f);
			gl.glVertex2f(w, -h);
			gl.glEnd();
		}
	}

	public static File generateScreenshotFile() {
		File scrs = new File("screens");
		if (!scrs.exists())
			scrs.mkdirs();
		File screenshot = null;
		String date = getNiceDate(new GregorianCalendar());
		do {
			screenshot = new File(scrs, date + "-"
					+ (int) (Math.random() * 0xFFFFFF) + ".png");
		} while (screenshot.exists());
		return screenshot;
	}

	private static String getNiceDate(GregorianCalendar d) {
		String date = "";
		date += d.get(GregorianCalendar.YEAR);
		date += fillZero(d.get(GregorianCalendar.MONTH) + 1, 2);
		date += fillZero(d.get(GregorianCalendar.DAY_OF_MONTH), 2);
		date += fillZero(d.get(GregorianCalendar.HOUR_OF_DAY), 2);
		date += fillZero(d.get(GregorianCalendar.MINUTE), 2);
		date += fillZero(d.get(GregorianCalendar.SECOND), 2);
		return date;
	}

	private static String fillZero(int i, int j) {
		String out = Integer.toString(i);
		while (out.length() < j)
			out = "0" + out;
		return out;
	}

	public static void rMatrixToEuler(Matrix3f m, float[] rot) {
		float a = 0, b = 0, c = 0;
		m.transpose();
		if (Math.abs(m.m20) != 1) {
			a = -(float) Math.asin(m.m20);
			double cosa = Math.cos(a);
			b = (float) Math.atan2(m.m21 / cosa, m.m22 / cosa);
			c = (float) Math.atan2(m.m10 / cosa, m.m00 / cosa);
		} else {
			c = 0;
			if (m.m20 == -1) {
				a = (float) (Math.PI / 2);
				b = (float) Math.atan2(m.m01, m.m02);
			} else {
				a = -(float) (Math.PI / 2);
				b = (float) Math.atan2(-m.m01, -m.m02);
			}
		}
		rot[0] = -b;
		rot[1] = -a;
		rot[2] = 0;
		m.transpose();
	}
}
