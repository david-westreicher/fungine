package rendering;

import game.Game;
import game.GameLoop;
import game.Updatable;

import java.awt.Cursor;
import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import javax.media.opengl.GL;
import javax.media.opengl.GL2;
import javax.media.opengl.GLAutoDrawable;
import javax.media.opengl.GLEventListener;
import javax.media.opengl.GLException;
import javax.media.opengl.fixedfunc.GLMatrixFunc;
import javax.media.opengl.glu.GLU;
import javax.vecmath.Matrix3f;
import javax.vecmath.Matrix4f;
import javax.vecmath.Point3f;
import javax.vecmath.Vector3f;

import manager.UberManager;
import settings.Settings;
import util.Log;
import util.Material;
import util.Util;
import vr.Rift;
import world.Camera;
import world.GameObject;
import world.GameObjectType;
import world.PointLight;
import browser.AwesomiumWrapper;
import browser.Browser;

import com.jogamp.common.nio.Buffers;
import com.jogamp.opengl.util.awt.Screenshot;
import com.jogamp.opengl.util.gl2.GLUT;
import com.jogamp.opengl.util.texture.Texture;
import com.jogamp.opengl.util.texture.TextureIO;

public class RenderUpdater implements Updatable, GLEventListener {
	protected static List<Integer[]> fobs = new ArrayList<Integer[]>();
	protected static final boolean USE_OBJECT_INTERP = false;
	protected static final boolean SMOOTHSTEP_INTERP = false;
	public static float ZFAR_DISTANCE = 100;// TODO changed from 5000
	protected static final float ZNEAR = 0.01f;
	protected static final float DEBUG_SIZE = 250f;
	public static float EYE_GAP = 0.23f;
	public static float ZFar;
	public static float ZNear;
	public static double FOV_Y = 111;
	private OpenGLRendering renderer;
	public static GL2 gl;
	public static GLUT glut = new GLUT();
	public final static GLU glu = new GLU();
	protected static float interp;
	private static Vector3f tmpVector3f = new Vector3f();
	private static Vector3f tmp2Vector3f = new Vector3f();
	protected List<String> renderStrings = new ArrayList<String>();
	private Cursor hiddenCursor;
	public static GameObject cgo = null;
	public int width;
	public int height;
	protected Map<String, List<GameObject>> renderObjs;
	protected Camera cam = Game.INSTANCE.cam;
	private List<String> excludedGameObjects = new ArrayList<String>();
	private float debugAngle;
	public static Browser browser = new AwesomiumWrapper();
	private static List<Runnable> queue = new ArrayList<Runnable>();
	private static List<Runnable> contextExecutions = new ArrayList<Runnable>();
	public RenderState renderState = new RenderState();
	private float[] projectionMatrix = new float[16];
	private float[] modelViewMatrix = new float[16];
	private Point3f p = new Point3f(0, 0, 0);
	private Matrix4f modelView = new Matrix4f();
	private Matrix4f projection = new Matrix4f();
	private boolean takeScreen = false;

	public RenderUpdater() {
		renderer = new OpenGLRendering(this);
		hiddenCursor = Util.getHiddenCursor();
	}

	@Override
	public void update(float interp) {
		this.interp = interp;
		renderer.display();
	}

	public void setFOV(double fov) {
		FOV_Y = fov;
		executeInOpenGLContext(new Runnable() {

			@Override
			public void run() {
				RenderUpdater.this.setProjection(width, height);
			}
		});
	}

	@Override
	public void display(GLAutoDrawable arg0) {
		gl = arg0.getGL().getGL2();
		synchronized (queue) {
			if (queue.size() > 0) {
				queue.remove(0).run();
			}
		}
		synchronized (contextExecutions) {
			for (Runnable r : contextExecutions) {
				r.run();
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

		if (Settings.STEREO)
			gl.glViewport(0, 0, width, height);
		endOrthoRender();

		gl.glFlush();
		// arg0.swapBuffers();
	}

	protected void setupLook(GameObject go) {
		float pos[] = interp(go.pos, go.oldPos, interp);
		setupLook(pos, go.rotationMatrix);
	}

	protected void setupLook(GameObject go, Matrix3f rot) {
		float pos[] = interp(go.pos, go.oldPos, interp);
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

	private float[] interp(float[] pos, float[] oldPos, float interp) {
		float res[] = new float[3];
		if (SMOOTHSTEP_INTERP)
			interp = ((interp) * (interp) * (3 - 2 * (interp)));
		for (int i = 0; i < 3; i++)
			res[i] = pos[i] * interp + (oldPos[i] * (1 - interp));
		return res;
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

	protected void startOrthoRenderOffset() {
		gl.glMatrixMode(GL2.GL_PROJECTION);
		gl.glPushMatrix();
		gl.glLoadIdentity();
		glu.gluOrtho2D(width, width * 2, height, 0);
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
		renderStrings.add("Textures to load:  "
				+ UberManager.getTexturesToLoad());
		renderStrings.add("Process Queue   :  " + queue.size());
		renderStrings.add("Render-FPS: "
				+ Util.roundDigits(loop.currentFPS.fps, 1));
		renderStrings.add("Tick-FPS  :  "
				+ Util.roundDigits(loop.currentTick.fps, 1));
		renderStrings.add("TpT       :  " + loop.timePerTick + "ms");
		renderStrings.add("#Objects  :  " + Game.INSTANCE.world.getObjectNum());
		renderStrings.add("#Chunks   :  " + VoxelWorldRenderer.VISIBLE_CHUNKS);
		if (!browser.isDummy())
			renderStrings.add("BerkFPS   :  "
					+ Util.roundDigits(browser.getFPS(), 1));

		int i = 1;
		for (String s : renderStrings) {
			gl.glRasterPos2f(width * (Settings.STEREO ? 2 : 1) - 200, 15 * i++);
			glut.glutBitmapString(GLUT.BITMAP_8_BY_13, s);
		}
		renderStrings.clear();
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

		/*
		 * for (List<GameObject> list : renderObjs.values()) { for (GameObject
		 * go : list) { Point3f pos = project(go.pos); if
		 * (isLegitProjection(go.pos, cam)) { gl.glPushMatrix();
		 * gl.glTranslatef(pos.x, pos.y, 0); gl.glScalef(20, 20, 1);
		 * gl.glColor4f(1, 0, 0, 1); gl.glBegin(GL2.GL_LINE_LOOP); {
		 * gl.glVertex3f(-1, -1, 0); gl.glVertex3f(1, -1, 0); gl.glVertex3f(1,
		 * 1, 0); gl.glVertex3f(-1, 1, 0); } gl.glEnd(); gl.glPopMatrix(); } } }
		 * endOrthoRender();
		 */
		/*
		 * int num = 50; int size = 100; gl.glPushMatrix(); {
		 * gl.glTranslatef(-num * size / 2, 0, -num * size / 2);
		 * gl.glBegin(GL2.GL_LINES); for (int i = 0; i < num; i++) {
		 * gl.glVertex3f(i * size, 0, 0); gl.glVertex3f(i * size, 0, size *
		 * size); gl.glVertex3f(0, 0, i * size); gl.glVertex3f(size * size, 0, i
		 * * size); } gl.glEnd(); } gl.glPopMatrix();
		 */
		gl.glBegin(GL2.GL_LINES);
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
		// gl.glPolygonMode(GL2.GL_FRONT_AND_BACK, GL2.GL_FILL);

		/*
		 * gl.glPushMatrix(); gl.glTranslatef(cam.pos[0], cam.pos[1], 0);
		 * gl.glScalef(cam.zoom, cam.zoom, 1); gl.glTranslatef(-width / 2,
		 * -height / 2, 0); // visible cam gl.glBegin(GL.GL_LINE_LOOP);
		 * gl.glVertex3f(0, 0, 0); gl.glVertex3f(width, 0, 0);
		 * gl.glVertex3f(width, height, 0); gl.glVertex3f(0, height, 0);
		 * gl.glEnd(); gl.glPopMatrix();
		 */
	}

	private void saveModelViewProjectionMatrix() {
		gl.glGetFloatv(GL2.GL_MODELVIEW_MATRIX, modelViewMatrix, 0);
		gl.glGetFloatv(GLMatrixFunc.GL_PROJECTION_MATRIX, projectionMatrix, 0);
		modelView.set(modelViewMatrix);
		projection.set(projectionMatrix);
		modelView.transpose();
		projection.transpose();
	}

	private boolean isLegitProjection(float[] pos, Camera cam) {
		tmpVector3f.set(pos);
		tmp2Vector3f.set(cam.pos);
		tmpVector3f.sub(tmp2Vector3f);
		tmp2Vector3f.set(0, 0, -1);
		cam.rotationMatrix.transform(tmp2Vector3f);
		return tmpVector3f.dot(tmp2Vector3f) > 0;
	}

	private Point3f project(float[] point) {
		p.set(point);
		modelView.transform(p);
		projection.transform(p);
		p.x = (p.x / p.z + 1) * width / 2;
		p.y = (1 - p.y / p.z) * height / 2;
		return p;
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
				renderer.draw(gl, objs, interp);
			else
				for (GameObject go : objs) {
					cgo = go;
					/*
					 * gl.glPushMatrix(); transform(goType, go);
					 * 
					 * gl.glMatrixMode(GL.GL_TEXTURE);
					 * gl.glActiveTexture(GL.GL_TEXTURE7); gl.glPushMatrix();
					 * transform(goType, go);
					 * gl.glActiveTexture(GL.GL_TEXTURE0);
					 */
					renderer.draw(gl);
					/*
					 * gl.glPopMatrix(); gl.glMatrixMode(GL2.GL_MODELVIEW);
					 * gl.glPopMatrix();
					 */
				}
			renderer.end(gl);
		}
	}

	private void transform(GameObjectType goType, GameObject go) {
		if (USE_OBJECT_INTERP) {
			// interp
			float xSpeed = go.pos[0] - go.oldPos[0];
			float ySpeed = go.pos[1] - go.oldPos[1];
			float zSpeed = go.pos[2] - go.oldPos[2];
			// TODO new interp rotation float rotSpeed =
			// go.rotation - go.oldRotation;
			gl.glTranslatef(go.pos[0] + xSpeed * interp, go.pos[1] + ySpeed
					* interp, go.pos[2] + zSpeed * interp);
		} else
			gl.glTranslatef(go.pos[0], go.pos[1], go.pos[2]);
		/*
		 * gl.glRotatef(toDegree(go.angle), go.rotation[0], go.rotation[1],
		 * go.rotation[2]);
		 */
		if (goType.shape == null || !Game.INSTANCE.hasPhysics()) {
			gl.glRotatef(toDegree(go.rotation[0]), -1, 0, 0);
			gl.glRotatef(toDegree(go.rotation[1]), 0, -1, 0);
			gl.glRotatef(toDegree(go.rotation[2]), 0, 0, -1);
		} else {
			// interpolationQuat.set(go.oldQuat);
			// interpolationQuat.interpolate(go.quat, interp);
			// interpolationAxisAngle.set(interpolationQuat);
			// gl.glRotatef(
			// toDegree(interpolationAxisAngle.angle),
			// interpolationAxisAngle.x,
			// interpolationAxisAngle.y,
			// interpolationAxisAngle.z);
			gl.glRotatef(toDegree(go.angle), go.rotation[0], go.rotation[1],
					go.rotation[2]);
		}
		gl.glScalef(go.size[0], go.size[1], go.size[2]);

	}

	private float toDegree(float f) {
		return (float) (f * 180 / Math.PI);
	}

	protected void renderObjects(boolean depthOnly) {
		renderState.depthOnly = depthOnly;
		if (depthOnly) {
			gl.glColorMask(false, false, false, false);
		}
		// render this shit
		renderObjects(renderObjs);

		if (depthOnly)
			gl.glColorMask(true, true, true, true);
		renderState.depthOnly = false;
	}

	public void renderObjects(Map<String, List<GameObject>> renderObjs) {
		for (String type : renderObjs.keySet()) {
			if (!excludedGameObjects.contains(type))
				renderObjects(type, renderObjs);
		}
	}

	protected void renderObjects() {
		renderObjects(false);
	}

	@Override
	public void dispose(GLAutoDrawable arg0) {
		gl = arg0.getGL().getGL2();
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
		gl.glEnable(GL2.GL_LINE_SMOOTH);
		gl.glHint(GL2.GL_LINE_SMOOTH_HINT, GL2.GL_NICEST);
		gl.glHint(GL2.GL_PERSPECTIVE_CORRECTION_HINT, GL2.GL_NICEST);
		gl.glEnable(GL2.GL_DEPTH_TEST);
		gl.glDepthFunc(GL2.GL_LEQUAL);
		// culling
		// gl.glDisable(GL2.GL_CULL_FACE);
		gl.glFrontFace(GL2.GL_CCW);
		gl.glEnable(GL2.GL_CULL_FACE);
		gl.glCullFace(GL2.GL_BACK);
		// point cloud rendering
		// gl.glEnable(GL2.GL_POINT_SMOOTH);
		gl.glEnable(GL2.GL_VERTEX_PROGRAM_POINT_SIZE);
		gl.glPointSize(10);
		UberManager.initializeShaders();
	}

	public void setProjection(int width, int height) {
		setProjection(width, height, FOV_Y, 0);
	}

	public void setProjection(int width, int height, double fov_y,
			float translation) {
		this.width = width;
		this.height = height;
		// float z = ((float) height / 2)
		// / (float) Math.tan(fov_y * Math.PI / 360);
		// float zoom = Game.INSTANCE.cam.zoom * z;
		// TODO maybe need gl.glViewport(0, 0, width, height);
		gl.glMatrixMode(GL2.GL_PROJECTION);
		gl.glLoadIdentity();
		ZNear = ZNEAR;// Math.max(zoom - 500, 0);
		ZFar = ZNEAR + ZFAR_DISTANCE;
		// RenderUtil.gluPerspective(gl, fov_y, (float) width / height, ZNear,
		// ZFar);
		if (translation != 0)
			gl.glTranslatef(translation, 0, 0);
		glu.gluPerspective(fov_y, (float) width / height, ZNear, ZFar);
		// gl.glScalef(1, -1, 1);
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
		browser.dispose(gl);
		glu.destroy();
		renderer.dispose();
	}

	public synchronized static void executeInOpenGLContext(Runnable runnable) {
		synchronized (contextExecutions) {
			contextExecutions.add(runnable);
		}
	}

	public synchronized static void queue(Runnable runnable) {
		synchronized (queue) {
			queue.add(runnable);
		}
	}

	public static void createCallList(final Runnable r,
			final CallBack<Integer> c) {
		executeInOpenGLContext(new Runnable() {

			@Override
			public void run() {
				int num = gl.glGenLists(1);
				gl.glNewList(num, GL2.GL_COMPILE);
				r.run();
				gl.glEndList();
				c.returnVar(num);
			}
		});
	}

	protected void setUpTranslate(GameObject go, float zOrdering) {
		float xSpeed = go.pos[0] - go.oldPos[0];
		float ySpeed = go.pos[1] - go.oldPos[1];
		float zSpeed = go.pos[2] - go.oldPos[2];
		// TODO new rotation interp float rotSpeed = go.rotation -
		// go.oldRotation;
		// gl.glTranslatef(go.pos[0] + xSpeed * interp, go.pos[1] + ySpeed
		// * interp, zOrdering + go.pos[2] + zSpeed * interp);
		// //gl.glRotatef(go.rotation + rotSpeed * interp, 0, 0, 1);
		// gl.glRotatef(go.zrotation, 1, 0, 0);
	}

	protected void setUpTransform(GameObject go, float zOrdering) {
		setUpTranslate(go, zOrdering);
		gl.glScalef(go.size[0], go.size[1], go.size[2]);
	}

	public void excludeGameObjectFromRendering(String string) {
		excludedGameObjects.add(string);
	}

	public void includeGameObjectFromRendering(String lightObjectTypeName) {
		excludedGameObjects.remove(lightObjectTypeName);
	}

	public void renderExcludedObjects() {
		for (String type : excludedGameObjects) {
			this.renderObjects(type, renderObjs);
		}
	}

	protected void createTex(int fobNum, int width, int height) {
		int[] fboId = new int[1];
		int[] texId = new int[1];
		// int[] depId = new int[1];
		gl.glGenFramebuffers(1, fboId, 0);
		gl.glGenTextures(1, texId, 0);
		// gl.glGenRenderbuffers(1, depId, 0);

		// gl.glBindRenderbuffer(GL2.GL_RENDERBUFFER, depId[0]);
		// gl.glRenderbufferStorage(GL2.GL_RENDERBUFFER, GL2.GL_DEPTH_COMPONENT,
		// width, height);

		gl.glBindTexture(GL.GL_TEXTURE_2D, texId[0]);
		gl.glTexParameteri(GL.GL_TEXTURE_2D, GL.GL_TEXTURE_MIN_FILTER,
				GL.GL_LINEAR);
		gl.glTexParameteri(GL.GL_TEXTURE_2D, GL.GL_TEXTURE_MAG_FILTER,
				GL.GL_LINEAR);
		gl.glTexParameteri(GL.GL_TEXTURE_2D, GL.GL_TEXTURE_WRAP_S, GL2.GL_CLAMP);
		gl.glTexParameteri(GL.GL_TEXTURE_2D, GL.GL_TEXTURE_WRAP_T, GL2.GL_CLAMP);
		gl.glTexImage2D(GL.GL_TEXTURE_2D, 0, GL.GL_RGBA, width, height, 0,
				GL.GL_RGBA, GL.GL_UNSIGNED_BYTE,
				Buffers.newDirectByteBuffer(width * height * 4));

		gl.glBindFramebuffer(GL2.GL_FRAMEBUFFER, fboId[0]);
		gl.glFramebufferTexture2D(GL2.GL_FRAMEBUFFER, GL2.GL_COLOR_ATTACHMENT0,
				GL.GL_TEXTURE_2D, texId[0], 0);
		// gl.glFramebufferRenderbuffer(GL2.GL_FRAMEBUFFER,
		// GL2.GL_DEPTH_ATTACHMENT, GL2.GL_RENDERBUFFER, depId[0]);

		gl.glBindTexture(GL.GL_TEXTURE_2D, 0);
		Integer[] fob = new Integer[] { texId[0], fboId[0] };
		if (fobs.size() < fobNum)
			fobs.set(fobNum, fob);
		else
			fobs.add(fob);
		int status = gl.glCheckFramebufferStatus(GL2.GL_FRAMEBUFFER);
		if (status == GL2.GL_FRAMEBUFFER_COMPLETE) {
			gl.glBindFramebuffer(GL2.GL_FRAMEBUFFER, 0);
			Log.log(this, "Frame buffer object successfully created");
		} else {
			throw new IllegalStateException("Frame Buffer Oject not created.");
		}
	}

	protected void createTex(int fobNum) {
		createTex(fobNum, Settings.WIDTH, Settings.HEIGHT);
	}

	protected void createShadowFob(int fobNum, int width, int height) {
		int[] fboId = new int[1];
		int[] texId = new int[1];
		gl.glGenFramebuffers(1, fboId, 0);
		gl.glGenTextures(1, texId, 0);

		gl.glBindTexture(GL.GL_TEXTURE_2D, texId[0]);
		gl.glTexParameteri(GL.GL_TEXTURE_2D, GL.GL_TEXTURE_MIN_FILTER,
				GL.GL_LINEAR);// before:GL_NEAREST
		gl.glTexParameteri(GL.GL_TEXTURE_2D, GL.GL_TEXTURE_MAG_FILTER,
				GL.GL_LINEAR);
		gl.glTexParameteri(GL.GL_TEXTURE_2D, GL2.GL_TEXTURE_COMPARE_MODE,
				GL2.GL_COMPARE_R_TO_TEXTURE);
		gl.glTexParameteri(GL.GL_TEXTURE_2D, GL2.GL_TEXTURE_COMPARE_FUNC,
				GL.GL_LEQUAL);
		gl.glTexParameteri(GL.GL_TEXTURE_2D, GL2.GL_DEPTH_TEXTURE_MODE,
				GL2.GL_INTENSITY);
		gl.glTexParameterf(GL.GL_TEXTURE_2D, GL.GL_TEXTURE_WRAP_S, GL2.GL_CLAMP);
		gl.glTexParameterf(GL.GL_TEXTURE_2D, GL.GL_TEXTURE_WRAP_T, GL2.GL_CLAMP);

		gl.glTexImage2D(GL2.GL_TEXTURE_2D, 0, GL2.GL_DEPTH_COMPONENT32F, width,
				height, 0, GL2.GL_DEPTH_COMPONENT, GL.GL_UNSIGNED_BYTE,
				Buffers.newDirectByteBuffer(width * height * 4));

		gl.glBindTexture(GL.GL_TEXTURE_2D, 0);
		gl.glBindFramebuffer(GL2.GL_FRAMEBUFFER, fboId[0]);

		gl.glDrawBuffer(GL2.GL_NONE);
		gl.glReadBuffer(GL2.GL_NONE);

		gl.glFramebufferTexture2D(GL2.GL_FRAMEBUFFER, GL2.GL_DEPTH_ATTACHMENT,
				GL.GL_TEXTURE_2D, texId[0], 0);

		Integer[] fob = new Integer[] { texId[0], fboId[0] };
		if (fobs.size() < fobNum)
			fobs.set(fobNum, fob);
		else
			fobs.add(fob);
		int status = gl.glCheckFramebufferStatus(GL2.GL_FRAMEBUFFER);
		if (status == GL2.GL_FRAMEBUFFER_COMPLETE) {
			gl.glBindFramebuffer(GL2.GL_FRAMEBUFFER, 0);
			Log.log(this, "Frame buffer object successfully created");
		} else {
			throw new IllegalStateException("Frame Buffer Oject not created.");
		}
	}

	protected Texture createCubeMap(String img) {
		gl.glEnable(GL2.GL_TEXTURE_CUBE_MAP_SEAMLESS);
		Texture cubeMapTex = TextureIO.newTexture(GL.GL_TEXTURE_CUBE_MAP);
		// String[] shortCuts = new String[] { "east.bmp", "west.bmp", "up.bmp",
		// "down.bmp", "north.bmp", "south.bmp" };
		// String[] shortCuts = new String[] { "r.jpg", "l.jpg", "u.jpg",
		// "d.jpg",
		// "f.jpg", "b.jpg" };
		// String[] shortCuts = new String[] { "+Z.tga", "-Z.tga", "+Y.tga",
		// "-Y.tga", "+X.tga", "-X.tga" };
		String[] shortCuts = new String[] { "", "", "", "", "", "" };
		Log.log(this, "create cubemap: " + img);
		try {
			cubeMapTex.updateImage(
					gl,
					TextureIO.newTextureData(gl.getGLProfile(), new File(img
							+ shortCuts[0]), false, null),
					GL.GL_TEXTURE_CUBE_MAP_POSITIVE_X);
			cubeMapTex.updateImage(
					gl,
					TextureIO.newTextureData(gl.getGLProfile(), new File(img
							+ shortCuts[1]), false, null),
					GL.GL_TEXTURE_CUBE_MAP_NEGATIVE_X);
			cubeMapTex.updateImage(
					gl,
					TextureIO.newTextureData(gl.getGLProfile(), new File(img
							+ shortCuts[2]), false, null),
					GL.GL_TEXTURE_CUBE_MAP_POSITIVE_Y);
			cubeMapTex.updateImage(
					gl,
					TextureIO.newTextureData(gl.getGLProfile(), new File(img
							+ shortCuts[3]), false, null),
					GL.GL_TEXTURE_CUBE_MAP_NEGATIVE_Y);
			cubeMapTex.updateImage(
					gl,
					TextureIO.newTextureData(gl.getGLProfile(), new File(img
							+ shortCuts[4]), false, null),
					GL.GL_TEXTURE_CUBE_MAP_POSITIVE_Z);
			cubeMapTex.updateImage(
					gl,
					TextureIO.newTextureData(gl.getGLProfile(), new File(img
							+ shortCuts[5]), false, null),
					GL.GL_TEXTURE_CUBE_MAP_NEGATIVE_Z);
			return cubeMapTex;
		} catch (GLException e) {
			e.printStackTrace();
		} catch (IOException e) {
			e.printStackTrace();
		}
		return null;
	}

	public Map<String, Object> getSettings() {
		Map<String, Object> settings = new HashMap<String, Object>();
		settings.put("isWireframe", Game.WIREFRAME);
		settings.put("isdDebug", DeferredRenderer.DEBUG);
		settings.put("isDepthFirst", DeferredRenderer.DEPTH_FIRST);
		settings.put("isDof", DeferredRenderer.DOF);
		settings.put("isSSAO", DeferredRenderer.SSAO);
		settings.put("blur", DeferredRenderer.BLUR);
		settings.put("ambient", DeferredRenderer.AMBIENT);
		settings.put("tFPS", GameLoop.TICKS_PER_SECOND);
		settings.put("ssao", DeferredRenderer.SSAO_STRENGTH);
		settings.put("fov", FOV_Y);
		settings.put("eyegap", EYE_GAP);
		return settings;
	}

	public void initShaderUniforms() {
	}

	public void endShaderUniforms() {
	}

	public static Browser getBrowser() {
		return browser;
	}
	/*
	 * private Vector3f computeTranslation(Camera cam, float size) {
	 * tmpVector3f.set(size, 0, 0); cam.rotationMatrix.transform(tmpVector3f);
	 * return (Vector3f) tmpVector3f.clone(); }
	 * 
	 * private void translate(Camera cam, Vector3f translation) { cam.pos[0] +=
	 * translation.x; cam.pos[1] += translation.y; cam.pos[2] += translation.z;
	 * cam.oldPos[0] += translation.x; cam.oldPos[1] += translation.y;
	 * cam.oldPos[2] += translation.z; }
	 * 
	 * private void renderCrosshair() { gl.glColor3f(1, 0, 0);
	 * gl.glBegin(GL2.GL_LINES); gl.glVertex2i(width / 2, height / 2 - 5);
	 * gl.glVertex2i(width / 2, height / 2 + 5); gl.glEnd(); }
	 */
}
