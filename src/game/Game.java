package game;

import input.Input;

import javax.script.ScriptException;

import manager.Manageable;
import manager.Manager;
import manager.SoundManager;
import physics.PhysicsTest;
import rendering.DeferredRenderer;
import rendering.OpenGLRendering;
import rendering.RenderUpdater;
import rendering.SpriteRenderer;
import rendering.TestSkinningRenderer;
import script.Script;
import settings.Settings;
import util.Factory;
import util.FolderWatcher;
import util.Log;
import util.Stoppable;
import util.Util;
import util.XMLToObjectParser;
import vr.VRFactory;
import world.Camera;
import world.World;

public class Game {
	public static boolean WIREFRAME = false;
	public static Game INSTANCE;
	public GameLoop loop = new GameLoop();
	public World world;
	public Factory factory = Factory.INSTANCE;
	public Input input = new Input();
	public static boolean DEBUG = false;
	public Camera cam = new Camera();
	private boolean hasPhysics = false;
	public boolean exitFlag = false;
	public boolean fullscreenFlag = false;
	public static VRFactory.VR vr;

	public Game() {
		INSTANCE = this;
		if (Settings.VR)
			vr = VRFactory.createVR();
		FolderWatcher f = new FolderWatcher(Settings.RESSOURCE_FOLDER);
		f.addFolderListener(new GameWatcher(this));
		f.start();
		FolderWatcher f2 = new FolderWatcher(Settings.ENGINE_FOLDER);
		f2.addFolderListener(new GameWatcher(this));
		f2.start();
		loop.startPause();
		loop.start();
	}

	public void parseXML() {
		new XMLToObjectParser().parse();
	}

	public void restart() {
		Log.log(this, "Restarting!");
		// TODO restart the whole shit
		Manager.restartManager();
		Util.sleep(10);
		loop.startPause();
		loop.exit();
		Script.restart();
		SpriteRenderer.createList();
		Util.sleep(100);
		start();
	}

	public void start() {
		world = new World();
		// world.add(cam);
		parseXML();
		try {
			Script.executeFunction(Settings.INIT_SCRIPT, "init", this,
					Factory.INSTANCE);
		} catch (ScriptException e) {
			loop.startPause();
			System.err.println("Couldn't parse the init script!");
			System.err.println(e.getMessage());
			e.printStackTrace();
		} catch (NoSuchMethodException e) {
			loop.startPause();
			System.err.println("Couldn't parse the init script!");
			System.err.println(e.getMessage());
			e.printStackTrace();
		}
		try {
			Script.executeFunction(Settings.MAIN_SCRIPT, "init", this,
					Factory.INSTANCE);
		} catch (ScriptException e) {
			loop.startPause();
			System.err.println("Couldn't parse the main script!");
			System.err.println(e.getMessage());
			e.printStackTrace();
		} catch (NoSuchMethodException e) {
			loop.startPause();
			System.err.println("Couldn't parse the main script!");
			System.err.println(e.getMessage());
			e.printStackTrace();
		}
		// Util.sleep(1000);
		loop.endPause();
	}

	public void addComponent(String c) {
		c = c.toLowerCase();
		if (c.equals("deferredrenderer")) {
			loop.renderer = new DeferredRenderer();
		} else if (c.equals("skinrenderer")) {
			loop.renderer = new TestSkinningRenderer();
		} else if (c.equals("gamemechanics")) {
			loop.mechanics = new GameMechanics();
		} else if (c.equals("sound")) {
			loop.sound = new SoundManager();
		} else if (c.equals("physics")) {
			hasPhysics = true;
			((GameMechanics) loop.mechanics).physics = new PhysicsTest();
		} else {
			System.err.println("Can't add component: " + c);
		}
	}

	public void exit() {
		Log.log(this, "game exit");
		// loop.startPause();
		loop.exit();
		Stoppable.stopAll();
	}

	public Manageable getManager(String name) {
		return Manager.getManager(name);
	}

	public void hideMouse(boolean b) {
		OpenGLRendering.hideMouse(b);
		/*
		 * if (loop.renderer != null) { if (b) { if (blankCursor == null)
		 * blankCursor = Toolkit.getDefaultToolkit() .createCustomCursor( new
		 * BufferedImage(16, 16, BufferedImage.TYPE_INT_ARGB), new Point(0, 0),
		 * "blank cursor"); Util.c.setCursor(blankCursor); } else
		 * Util.c.setCursor(null); }
		 */
	}

	public void centerMouse() {
		OpenGLRendering.centerMouse();
	}

	public void log(Object o) {
		Log.log(this, o);
	}

	public int getWidth() {
		return ((RenderUpdater) loop.renderer).width;
	}

	public int getHeight() {
		return ((RenderUpdater) loop.renderer).height;
	}

	public boolean hasPhysics() {
		return hasPhysics;
	}

	public static Game getInstance() {
		return INSTANCE;
	}

	public void jsTest() {
		Log.log(this, "jsTest");
	}
}
