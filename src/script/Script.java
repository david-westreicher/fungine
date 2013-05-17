package script;

import game.Game;
import io.IO;

import java.io.BufferedReader;
import java.io.IOException;
import java.util.HashMap;
import java.util.Map;

import javax.script.Compilable;
import javax.script.CompiledScript;
import javax.script.Invocable;
import javax.script.ScriptEngine;
import javax.script.ScriptEngineManager;
import javax.script.ScriptException;

import util.Factory;
import util.Log;

public class Script {

	public static Map<String, Invocable> scripts = new HashMap<String, Invocable>();
	private static ScriptEngine engine;

	public static void execute(String script, Object... vars)
			throws ScriptException {
		ScriptEngine engine = getEngine();
		for (int i = 0; i < vars.length; i += 2)
			engine.put((String) vars[i], vars[i + 1]);
		BufferedReader br = IO.read(script);
		engine.eval(br);
		try {
			br.close();
		} catch (IOException e) {
			e.printStackTrace();
		}
	}

	public static void executeFunction(String script, String func,
			Object... vars) throws ScriptException, NoSuchMethodException {
		Invocable inv = scripts.get(script);
		if (inv == null) {
			ScriptEngine engine = getEngine();
			engine.put("game", Game.INSTANCE);
			engine.put("factory", Factory.INSTANCE);
			engine.put("log", Log.getInstance());
			BufferedReader br = IO.read(script);
			Object objects = engine.eval(br);
			try {
				br.close();
			} catch (IOException e) {
				e.printStackTrace();
			}
			inv = (Invocable) engine;
			scripts.put(script, inv);
		}
		inv.invokeFunction(func, vars);
	}

	private static ScriptEngine getEngine() {
		if (engine == null) {
			ScriptEngineManager engineMgr = new ScriptEngineManager();
			engine = engineMgr.getEngineByName("JavaScript");
		}
		return engine;
	}

	public static void compile(String scriptFile) throws ScriptException {
		ScriptEngine engine = getEngine();
		BufferedReader br = IO.read(scriptFile);
		CompiledScript script = ((Compilable) engine).compile(br);
		try {
			br.close();
		} catch (IOException e) {
			e.printStackTrace();
		}
		// engine.
	}

	public static void restart() {
		scripts.clear();
	}
}
