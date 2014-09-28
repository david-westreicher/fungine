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
import javax.media.opengl.GL2GL3;
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
import util.GLUtil;
import util.Log;
import util.MathHelper;
import util.Util;
import vr.Rift;
import world.Camera;
import world.GameObject;
import world.GameObjectType;
import world.PointLight;
import browser.AwesomiumWrapper;

import com.jogamp.opengl.util.awt.Screenshot;
import com.jogamp.opengl.util.gl2.GLUT;

public class RenderUpdater implements Updatable, GLEventListener {
	public interface Renderer {
		public void renderObjects(GL3 gl, GLUtil glutil,
				Map<String, List<GameObject>> renderObjs);

		void dispose(GL2GL3 gl);

	}

	private static final float ZNEAR = 0.01f;
	private static final float DEBUG_SIZE = 250f;
	private static final AwesomiumWrapper browser = new AwesomiumWrapper();
	private static final List<GLRunnable> queue = new ArrayList<GLRunnable>();
	private static final List<GLRunnable> contextExecutions = new ArrayList<GLRunnable>();
	private static final float ZFAR_DISTANCE = 1000;
	private List<float[][]> debugLines = new LinkedList<float[][]>();
	protected boolean takeScreen = false;
	private FPSRenderer fpsRenderer;
	public Renderer objectsRenderer = null;
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
	public static float FOV_Y = 69;
	public static float INTERP;
	public static GLUT glut = new GLUT();
	public static float EYE_GAP = 0.23f;
	public static boolean WIREFRAME = false;
	public final static GLU glu = new GLU();
	public final static GLUtil glutil = new GLUtil();
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

	public void setFOV(float fov) {
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
		glutil.glMatrixMode(GL2.GL_MODELVIEW);
		glutil.glLoadIdentity();

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
			// if (Game.DEBUG || !Settings.SHOW_STATUS)
			// renderDebug();

		}

		startOrthoRender(Settings.STEREO);
		if (Settings.STEREO)
			gl.glViewport(0, 0, width * 2, height);

		if (Settings.USE_BROWSER)
			browser.render(gl, glutil);
		// renderCrosshair();
		// if (!Settings.SHOW_STATUS)
		// renderText();
		GameLoop loop = Game.INSTANCE.loop;
		fpsRenderer.render(gl, glutil, textures, width, loop.timePerRender,
				loop.timePerTick);

		if (Settings.STEREO)
			gl.glViewport(0, 0, width, height);
		endOrthoRender();

		glutil.checkSanity();

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
		glutil.gluLookAt(pos[0], pos[1], pos[2], pos[0] + tmpVector3f.x, pos[1]
				+ tmpVector3f.y, pos[2] + tmpVector3f.z, tmp2Vector3f.x,
				tmp2Vector3f.y, tmp2Vector3f.z);
	}

	public void endOrthoRender() {
		glutil.glPopMatrix();
		glutil.glMatrixMode(GL2.GL_PROJECTION);
		glutil.glPopMatrix();
		glutil.glMatrixMode(GL2.GL_MODELVIEW);
	}

	protected void startOrthoRender(boolean stereo) {
		glutil.glMatrixMode(GL2.GL_PROJECTION);
		glutil.glPushMatrix();
		glutil.glLoadIdentity();
		glutil.gluOrtho2D(0, width * (stereo ? 2 : 1), height, 0);
		glutil.glMatrixMode(GL2.GL_MODELVIEW);
		glutil.glPushMatrix();
		glutil.glLoadIdentity();
	}

	public void startOrthoRender() {
		startOrthoRender(false);
	}

	private void renderText() {
		gl.glColor3f(1, 0, 0);
		gl.glPixelZoom(((float) width / Settings.WIDTH),
				((float) height / Settings.HEIGHT));
		GameLoop loop = Game.INSTANCE.loop;
		// text
		int i = 0;
		int x = width * (Settings.STEREO ? 2 : 1) / 2 - 200;
		int startY = height / 2 - 100;
		renderString("Render-FPS: " + Util.roundDigits(loop.currentFPS.fps, 1),
				x, startY - 15 * i++);
		renderString(
				"Tick-FPS  :  " + Util.roundDigits(loop.currentTick.fps, 1), x,
				startY - 15 * i++);
		renderString("TpT       :  " + loop.timePerTick + "ms", x, startY - 15
				* i++);
		renderString("#Objects  :  " + Game.INSTANCE.world.getObjectNum(), x,
				startY - 15 * i++);
		renderString("Textures to load:  " + UberManager.getTexturesToLoad(),
				x, startY - 15 * i++);

	}

	private void renderString(String string, int posX, int posY) {
		// gl.glRasterPos2f(posX, posY);
		gl.glRasterPos2i(0, 0);
		gl.glBitmap(0, 0, 0, 0, posX, posY, null);
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

	protected void renderObjects() {
		if (objectsRenderer != null)
			try {
				objectsRenderer.renderObjects(gl3, glutil, renderObjs);
			} catch (Exception e) {
				e.printStackTrace();
			}
	}

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
		Log.log(this, "gl version: " + gl);
		Log.log(this, "dimensions: " + width, height);
		Log.log(this,
				"GL_ARB_gpu_shader5: "
						+ (gl.isExtensionAvailable("GL_ARB_gpu_shader5") ? "available"
								: "missing"));

		gl.glClearColor(1, 1, 1, 1);
		gl.glDisable(GL2.GL_LINE_SMOOTH);
		gl.glHint(GL2.GL_LINE_SMOOTH_HINT, GL2.GL_FASTEST);
		// gl.glHint(GL3.GL_GENERATE_MIPMAP_HINT, GL3.GL_NICEST);
		// gl.glHint(GL2.GL_PERSPECTIVE_CORRECTION_HINT, GL2.GL_FASTEST);
		gl.glEnable(GL2.GL_DEPTH_TEST);
		gl.glDepthFunc(GL2.GL_LEQUAL);
		// culling
		// gl.glDisable(GL2.GL_CULL_FACE);
		gl.glLineWidth(4);
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
		browser.init(textures, arg0.getGL().getGL3());
	}

	public void setProjection(int width, int height) {
		setProjection(width, height, FOV_Y, 0);
	}

	public void setProjection(int width, int height, float fov_y,
			float translation) {
		this.width = width;
		this.height = height;
		glutil.glMatrixMode(GL2.GL_PROJECTION);
		glutil.glLoadIdentity();
		zNear = ZNEAR;
		zFar = ZNEAR + ZFAR_DISTANCE;
		if (translation != 0)
			glutil.glTranslatef(translation, 0, 0);
		glutil.gluPerspective(fov_y, (float) width / height, zNear, zFar);
		glutil.glMatrixMode(GL2.GL_MODELVIEW);
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

	public void initShaderUniforms() {
	}

	public void endShaderUniforms() {
	}

	public static AwesomiumWrapper getBrowser() {
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

	public void setObjectsRenderer(final Renderer r) {
		Log.log(this, "setting object renderer to "
				+ r.getClass().getSimpleName());
		executeInOpenGLContext(new GLRunnable() {
			@Override
			public void run(GL2 gl) {
				if (objectsRenderer != null)
					objectsRenderer.dispose(gl3);
				objectsRenderer = r;
			}
		});
	}
}
