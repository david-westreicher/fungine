package game;

import input.Input;
import rendering.util.NEWTWindow;
import script.JavaScript;
import settings.Settings;
import util.Factory;
import util.FolderWatcher;
import util.Log;
import util.Stoppable;
import util.Util;
import util.WorkerPool;
import vr.VRFactory;
import world.Camera;
import world.World;

//TODO use data-oriented approach for gamebojects
//TODO refactor drawTexture
//TODO use array textures for rgb/normal/spec
//TODO send all go's data to GPU once after gamemechanics (currently send every frame -.- )
//TODO use glMapBuffer/glMapBufferRange to update per frame vbo's (go's data), maybe with doublebuffering?
//TODO use VAO(binds all the buffers, attribPointer, divisor) for renderinformation
//TODO folderwatcher allocates too many string :(
//TODO rename repeatedrunnable/repreatedthread
//TODO nicer way to handle textures?
//TODO implement simple file-modify listener (listener for ubermanager, shaderutil, javascript)
//TODO use fbx-conv to load models/bones
//TODO cleanup renderupdater
//TODO remove engine/img -> generate on startup
//TODO update camera every frame / use relative mouse position, just recenter if outside a specific area
//TODO remove startOrthoRenderer
//TODO switch to gradle build
//TODO update awesomium to 1.7 (needs c++ jna)
public class Game {
	public static Game INSTANCE;
	public GameLoop loop = new GameLoop();
	public World world;
	public Factory factory = Factory.INSTANCE;
	public Input input = new Input();
	public static boolean DEBUG = false;
	public Camera cam = new Camera();
	public boolean exitFlag = false;
	public boolean fullscreenFlag = Settings.USE_FULL_SCREEN;
	public static WorkerPool workerPool;
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
		workerPool = new WorkerPool();
		workerPool.start();
		loop.startPause();
		loop.start();
	}

	public void restart() {
		Log.log(this, "Restarting!");
		// TODO restart the whole shit
		Util.sleep(10);
		loop.startPause();
		loop.exit();
		JavaScript.reset();
		Util.sleep(100);
		start();
	}

	public void start() {
		world = new World();
		world.add(cam);
		JavaScript.execute(Settings.MAIN_SCRIPT, true);
		if (Settings.LOW_GRAPHICS)
			Util.sleep(1000);
		loop.endPause();
	}

	public void exit() {
		Log.log(this, "game exit");
		// loop.startPause();
		loop.exit();
		workerPool.dispose();
		Stoppable.stopAll();
	}

	public void hideMouse(boolean b) {
		NEWTWindow.hideMouse(b);
	}

	public void centerMouse() {
		NEWTWindow.centerMouse();
	}

	public void log(Object o) {
		Log.log(this, o);
	}

	public int getWidth() {
		return loop.renderer.width;
	}

	public int getHeight() {
		return loop.renderer.height;
	}

	public static Game getInstance() {
		return INSTANCE;
	}

	public void jsTest() {
		Log.log(this, "jsTest");
	}
}
