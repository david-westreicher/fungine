package manager;

import game.Game;
import io.IO;

import java.io.BufferedReader;
import java.io.IOException;

import javax.script.Compilable;
import javax.script.ScriptEngine;
import javax.script.ScriptEngineManager;
import javax.script.ScriptException;

import script.GameScript;
import util.Factory;
import util.Log;

public class ScriptManager extends Manager<GameScript> {

	public ScriptManager() {
		super("script");
	}

	@Override
	protected void updateObject(GameScript gs, String file) {
		try {
			ScriptEngineManager manager = new ScriptEngineManager();
			ScriptEngine engine = manager.getEngineByName("JavaScript");
			// engine put....
			engine.put("log", Log.getInstance());
			engine.put("game", Game.INSTANCE);
			engine.put("factory", Factory.INSTANCE);
			Compilable compEngine = (Compilable) engine;
			BufferedReader br = IO.read(file);
			gs.script = compEngine.compile(br);
			br.close();
		} catch (ScriptException e) {
			gs.script = null;
		} catch (IOException e) {
			e.printStackTrace();
		}
	}
}
