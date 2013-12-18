package rendering;

import game.Game;
import game.GameLoop;
import game.Updatable;

import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;

import javax.media.opengl.GL2;
import javax.media.opengl.GL3;
import javax.media.opengl.GLAutoDrawable;
import javax.media.opengl.GLEventListener;
import javax.media.opengl.GLException;
import javax.media.opengl.GLProfile;
import javax.media.opengl.glu.GLU;
import javax.vecmath.Matrix3f;
import javax.vecmath.Vector3f;

import manager.UberManager;
import rendering.material.Material;
import settings.Settings;
import util.Log;
import util.MathHelper;
import util.Util;
import vr.Rift;
import world.Camera;
import world.GameObject;
import world.GameObjectType;
import world.PointLight;
import browser.AwesomiumWrapper;
import browser.Browser;

import com.jogamp.opengl.util.awt.Screenshot;
import com.jogamp.opengl.util.gl2.GLUT;

public abstract class RenderUpdater implements Updatable, GLEventListener {
	private static final float ZNEAR = 0.01f;
	private static final float DEBUG_SIZE = 250f;
	private static final Browser browser = new AwesomiumWrapper();
	private static final List<GLRunnable> queue = new ArrayList<GLRunnable>();
	private static final List<GLRunnable> contextExecutions = new ArrayList<GLRunnable>();
	private static final float ZFAR_DISTANCE = 100;
	private List<float[][]> debugLines = new LinkedList<float[][]>();
	private boolean takeScreen = false;
	private FPSRenderer fpsRenderer;
	private float debugAngle;
	private OpenGLRendering renderer;
	protected static final boolean SMOOTHSTEP_INTERP = false;
	protected static float zFar;
	protected static float zNear;
	protected Map<String, List<GameObject>> renderObjs;
	protected Camera cam = Game.INSTANCE.cam;
	protected TextureHelper textures = new TextureHelper();
	protected GL2 gl;
	protected GL3 gl3;
	public static double FOV_Y = 69;
	public static float INTERP;
	public static GLUT glut = new GLUT();
	public static float EYE_GAP = 0.23f;
	public static boolean WIREFRAME = false;
	public final static GLU glu = new GLU();
	public int width;
	public int height;
	public RenderState renderState = new RenderState();
	private static Vector3f tmpVector3f = new Vector3f();
	private static Vector3f tmp2Vector3f = new Vector3f();

	public RenderUpdater() {
		renderer = new OpenGLRendering(this);
	}

	@Override
	public void update(float interp) {
		INTERP = interp;
		renderer.display();
	}

	public void setFOV(double fov) {
		FOV_Y = fov;
		executeInOpenGLContext(new GLRunnable() {

			@Override
			public void run(GL2 gl) {
				RenderUpdater.this.setProjection(width, height);
			}
		});
	}

	@Override
	public void display(GLAutoDrawable arg0) {
		gl = arg0.getGL().getGL2();
		if (arg0.getGL().isGL3())
			gl3 = arg0.getGL().getGL3();
		synchronized (queue) {
			if (queue.size() > 0) {
				queue.remove(0).run(gl);
			}
		}
		synchronized (contextExecutions) {
			for (GLRunnable r : contextExecutions) {
				r.run(gl);
			}
			contextExecutions.clear();
		}
		// long startTime = 0;
		// if(Game.INSTANCE.loop.tick%60==0)
		// startTime = System.nanoTime();
		gl.glClear(GL2.GL_COLOR_BUFFER_BIT | GL2.GL_DEPTH_BUFFER_BIT);
		// if(Game.INSTANCE.loop.tick%60==0)
		// Log.log(this, System.nanoTime() - startTime);
		gl.glMatrixMode(GL2.GL_MODELVIEW);
		gl.glLoadIdentity();

		// renderOBJECTS

		if (!Game.INSTANCE.loop.isPausing()) {
			renderObjs = Game.INSTANCE.world.getVisibleObjects();
			gl.glEnable(GL2.GL_DEPTH_TEST);
			// CAMERA
			if (Settings.STEREO) {
				if (Settings.VR) {
					cam.setRotation(Game.vr.getRotation());
					cam.setRotation(Game.vr.getMatrix());
				}
				gl.glTranslatef(Rift.getDip(), 0, 0);
				setProjection(width, height, Rift.getFOV(), Rift.getH());
				setupLook(cam, Settings.VR ? Game.vr.getMatrix()
						: cam.rotationMatrix);
				renderObjects();
				renderState.stereo = true;
				gl.glLoadIdentity();
				gl.glTranslatef(-Rift.getDip(), 0, 0);
				setProjection(width, height, Rift.getFOV(), -Rift.getH());
				setupLook(cam, Settings.VR ? Game.vr.getMatrix()
						: cam.rotationMatrix);
				renderObjects();
				renderState.stereo = false;
			} else {
				setupLook(cam);
				renderObjects();
			}

			if (takeScreen) {
				Log.log(this, "taking screenshot");
				try {
					Screenshot.writeToFile(Util.generateScreenshotFile(),
							Settings.STEREO ? width * 2 : width, height);
				} catch (GLException e) {
					e.printStackTrace();
				} catch (IOException e) {
					e.printStackTrace();
				}
				takeScreen = false;
			}

			gl.glDisable(GL2.GL_DEPTH_TEST);
			if (Game.DEBUG || !Settings.SHOW_STATUS)
				renderDebug();

		}

		startOrthoRender(Settings.STEREO);
		if (Settings.STEREO)
			gl.glViewport(0, 0, width * 2, height);

		if (Settings.USE_BROWSER)
			browser.render(gl);
		// renderCrosshair();
		if (!Settings.SHOW_STATUS)
			renderText();
		GameLoop loop = Game.INSTANCE.loop;
		fpsRenderer.render(gl, textures, width, loop.timePerRender,
				loop.timePerTick);

		if (Settings.STEREO)
			gl.glViewport(0, 0, width, height);
		endOrthoRender();

		// if (Settings.IS_WINDOWS)
		// gl.glFlush();
		// else
		// use glFinish() for faulty linux driver
		// https://github.com/ValveSoftware/Source-1-Games/issues/765
		// gl.glFinish();
	}

	protected void setupLook(GameObject go) {
		float pos[] = MathHelper.interp(go.pos, go.oldPos, INTERP,
				SMOOTHSTEP_INTERP);
		setupLook(pos, go.rotationMatrix);
	}

	protected void setupLook(GameObject go, Matrix3f rot) {
		float pos[] = MathHelper.interp(go.pos, go.oldPos, INTERP,
				SMOOTHSTEP_INTERP);
		setupLook(pos, rot);
	}

	protected void setupLook(float[] pos, Matrix3f rotationMatrix) {
		tmpVector3f.set(0, 0, -1);
		tmp2Vector3f.set(0, 1, 0);
		rotationMatrix.transform(tmpVector3f);
		rotationMatrix.transform(tmp2Vector3f);
		glu.gluLookAt(pos[0], pos[1], pos[2], pos[0] + tmpVector3f.x, pos[1]
				+ tmpVector3f.y, pos[2] + tmpVector3f.z, tmp2Vector3f.x,
				tmp2Vector3f.y, tmp2Vector3f.z);
	}

	protected void endOrthoRender() {
		gl.glPopMatrix();
		gl.glMatrixMode(GL2.GL_PROJECTION);
		gl.glPopMatrix();
		gl.glMatrixMode(GL2.GL_MODELVIEW);
	}

	protected void startOrthoRender(boolean stereo) {
		gl.glMatrixMode(GL2.GL_PROJECTION);
		gl.glPushMatrix();
		gl.glLoadIdentity();
		glu.gluOrtho2D(0, width * (stereo ? 2 : 1), height, 0);
		gl.glMatrixMode(GL2.GL_MODELVIEW);
		gl.glPushMatrix();
		gl.glLoadIdentity();
	}

	protected void startOrthoRender() {
		startOrthoRender(false);
	}

	private void renderText() {
		gl.glColor3f(1, 0, 0);
		GameLoop loop = Game.INSTANCE.loop;
		// text
		int i = 1;
		int x = width * (Settings.STEREO ? 2 : 1) - 200;
		renderString("Render-FPS: " + Util.roundDigits(loop.currentFPS.fps, 1),
				x, 15 * i++ + 80);
		renderString(
				"Tick-FPS  :  " + Util.roundDigits(loop.currentTick.fps, 1), x,
				15 * i++ + 80);
		renderString("TpT       :  " + loop.timePerTick + "ms", x,
				15 * i++ + 80);
		renderString("#Objects  :  " + Game.INSTANCE.world.getObjectNum(), x,
				15 * i++ + 80);
		renderString("Textures to load:  " + UberManager.getTexturesToLoad(),
				x, 15 * i++ + 80);

	}

	private void renderString(String string, int posX, int posY) {
		gl.glRasterPos2f(posX, posY);
		glut.glutBitmapString(GLUT.BITMAP_8_BY_13, string);
	}

	private void renderDebug() {
		// bboxes
		gl.glColor4f(0.5f, 0.5f, 0.5f, 1);
		// gl.glPolygonMode(GL2.GL_FRONT_AND_BACK, GL2.GL_LINE);
		for (List<GameObject> list : renderObjs.values()) {
			for (GameObject go : list) {
				if ((Game.DEBUG || go.marked)) {
					gl.glColor3fv(go.color, 0);
					gl.glBegin(GL2.GL_LINES);
					RenderUtil.drawLinedBox(go.bbox, gl);
					gl.glEnd();
					if (go instanceof PointLight) {
						PointLight l = (PointLight) go;
						RenderUtil.drawSphere(go.pos, l.radius, l.color, gl,
								true);
					}
					// draw wireframe of object into center
					debugAngle += 0.01f;
					startOrthoRender();
					gl.glPushMatrix();
					gl.glTranslatef(400, DEBUG_SIZE / 2, 0);
					gl.glScalef(DEBUG_SIZE, -DEBUG_SIZE, 1);
					gl.glRotatef(debugAngle, 0, 1, 0);
					GameObjectRenderer objectRenderer = GameObjectType
							.getType(go.getType()).renderer;
					if (objectRenderer == null)
						return;
					gl.glPolygonMode(GL2.GL_FRONT_AND_BACK, GL2.GL_LINE);
					objectRenderer.drawSimple(gl);
					gl.glPolygonMode(GL2.GL_FRONT_AND_BACK, GL2.GL_FILL);
					gl.glPopMatrix();
					gl.glPushMatrix();
					List<Material> mats = objectRenderer.getMaterials();
					gl.glColor4f(1, 1, 1, 1);
					gl.glDisable(GL2.GL_CULL_FACE);
					gl.glEnable(GL2.GL_TEXTURE_2D);
					gl.glTranslatef(400, DEBUG_SIZE / 2, 0);
					if (mats != null && mats.size() > 0) {
						for (int i = 0; i < Math.min(3, mats.size()); i++) {
							Material mat = mats.get(i);
							if (mat != null) {
								if (mat.texture != null) {
									gl.glTranslatef(DEBUG_SIZE, 0, 0);
									Util.drawTexture(gl, mat.texture,
											DEBUG_SIZE / 2, DEBUG_SIZE / 2);
								}
								if (mat.normalMap != null) {
									gl.glTranslatef(DEBUG_SIZE, 0, 0);
									Util.drawTexture(gl, mat.normalMap,
											DEBUG_SIZE / 2, DEBUG_SIZE / 2);
								}
							}
						}
					}
					gl.glDisable(GL2.GL_TEXTURE_2D);
					gl.glEnable(GL2.GL_CULL_FACE);
					gl.glPopMatrix();
					endOrthoRender();
				}
			}
		}

		gl.glBegin(GL2.GL_LINES);
		gl.glColor4f(1, 0, 0, 1);
		for (float line[][] : debugLines) {
			gl.glVertex3fv(line[0], 0);
			gl.glVertex3fv(line[1], 0);
		}
		{
			gl.glColor4f(1, 0, 0, 1);
			gl.glVertex3f(0, 0, 0);
			gl.glVertex3f(1, 0, 0);
			gl.glColor4f(0, 1, 0, 1);
			gl.glVertex3f(0, 0, 0);
			gl.glVertex3f(0, 1, 0);
			gl.glColor4f(0, 0, 1, 1);
			gl.glVertex3f(0, 0, 0);
			gl.glVertex3f(0, 0, 1);
		}
		gl.glEnd();
	}

	protected abstract void renderObjects();

	@Override
	public void dispose(GLAutoDrawable arg0) {
		gl = arg0.getGL().getGL2();
		UberManager.clearNow(gl);
		textures.dispose(gl);
		Log.log(this, "gl dispose");
	}

	@Override
	public void init(GLAutoDrawable arg0) {
		gl = arg0.getGL().getGL2();
		Log.log(this, "dimensions: " + width, height);
		Log.log(this,
				"GL_ARB_gpu_shader5: "
						+ (gl.isExtensionAvailable("GL_ARB_gpu_shader5") ? "available"
								: "missing"));

		gl.glClearColor(0, 0, 0, 0);
		gl.glDisable(GL2.GL_LINE_SMOOTH);
		gl.glHint(GL2.GL_LINE_SMOOTH_HINT, GL2.GL_FASTEST);
		gl.glHint(GL2.GL_PERSPECTIVE_CORRECTION_HINT, GL2.GL_FASTEST);
		gl.glEnable(GL2.GL_DEPTH_TEST);
		gl.glDepthFunc(GL2.GL_LEQUAL);
		// culling
		// gl.glDisable(GL2.GL_CULL_FACE);
		gl.glFrontFace(GL2.GL_CCW);
		gl.glEnable(GL2.GL_CULL_FACE);
		gl.glCullFace(GL2.GL_BACK);
		// point cloud rendering
		// gl.glEnable(GL2.GL_POINT_SMOOTH);
		// gl.glEnable(GL2.GL_VERTEX_PROGRAM_POINT_SIZE);
		// gl.glPointSize(10);
		// if (!Settings.LOW_GRAPHICS)
		// UberManager.initializeShaders(gl);

		fpsRenderer = new FPSRenderer(textures, gl);
	}

	public void setProjection(int width, int height) {
		setProjection(width, height, FOV_Y, 0);
	}

	public void setProjection(int width, int height, double fov_y,
			float translation) {
		this.width = width;
		this.height = height;
		gl.glMatrixMode(GL2.GL_PROJECTION);
		gl.glLoadIdentity();
		zNear = ZNEAR;
		zFar = ZNEAR + ZFAR_DISTANCE;
		if (translation != 0)
			gl.glTranslatef(translation, 0, 0);
		RenderUtil.gluPerspective(gl, fov_y, (float) width / height, zNear,
				zFar);
		gl.glMatrixMode(GL2.GL_MODELVIEW);
	}

	@Override
	public void reshape(GLAutoDrawable arg0, int x, int y, int width, int height) {
		gl = arg0.getGL().getGL2();
		Log.log(this, "reshape:[" + width + "," + height + "]");
		setProjection(width / (Settings.STEREO ? 2 : 1), height);
	}

	@Override
	public void dispose() {
		Log.log(this, "dispose");
		queue.clear();
		contextExecutions.clear();
		browser.dispose(gl);
		glu.destroy();
		renderer.dispose();
	}

	public synchronized static void executeInOpenGLContext(GLRunnable runnable) {
		synchronized (contextExecutions) {
			contextExecutions.add(runnable);
		}
	}

	public synchronized static void queue(GLRunnable runnable) {
		synchronized (queue) {
			queue.add(runnable);
		}
	}

	public Map<String, Object> getSettings() {
		Map<String, Object> settings = new HashMap<String, Object>();
		settings.put("isWireframe", WIREFRAME);
		settings.put("tFPS", GameLoop.TICKS_PER_SECOND);
		settings.put("fov", FOV_Y);
		settings.put("eyegap", EYE_GAP);
		return settings;
	}

	public abstract void initShaderUniforms();

	public abstract void endShaderUniforms();

	public static Browser getBrowser() {
		return browser;
	}

	public void addDebugLine(float from[], float to[]) {
		debugLines.add(0, new float[][] { from, to });
		for (int i = 1000; i < debugLines.size(); i++) {
			debugLines.remove(i);
		}
	}

	public void addDebugLine(double from[], double to[]) {
		float[] newFrom = new float[] { from.length };
		float[] newTo = new float[] { to.length };
		addDebugLine(newFrom, newTo);
	}

	public void clearDebugLines() {
		debugLines.clear();
	}

	public static GLProfile getGLProfile() {
		if (Game.INSTANCE.loop == null || Game.INSTANCE.loop.renderer == null
				|| Game.INSTANCE.loop.renderer.gl == null)
			return null;
		return Game.INSTANCE.loop.renderer.gl.getGLProfile();
	}
}
