package test;

import java.util.Iterator;
import java.util.Map.Entry;

import javax.script.Bindings;
import javax.script.Compilable;
import javax.script.CompiledScript;
import javax.script.Invocable;
import javax.script.ScriptContext;
import javax.script.ScriptEngine;
import javax.script.ScriptEngineManager;
import javax.script.ScriptException;

public class ScriptPerformance {
	private static final String script1 = "function fac(i){" + "if(i==1)"
			+ "	return 1;" + "else" + "	return i*fac(i-1)" + "}" + "fac(60);";
	private static final String script2 = "function fib(i){" + "if(i<2)"
			+ "	return i;" + "else" + "	return fib(i-1)+fib(i-1)" + "}"
			+ "fib(10);";
	private static final String script3 = "var x=10;function add(n){x+=n;return x;}";
	private static CountMS counter;
	private static String script = script3;
	private static int iter = 100;

	public static void main(String[] args) throws InterruptedException {
		ScriptEngineManager mgr = new ScriptEngineManager();
		ScriptEngine jsEngine = mgr.getEngineByName("JavaScript");
		counter = new CountMS();
		// compile(jsEngine);
		invoke(jsEngine);
		// eval(jsEngine);
	}

	private static void invoke(ScriptEngine jsEngine) {
		System.out.println("invoking");
		try {
			jsEngine.eval(script);
		} catch (ScriptException e) {
			e.printStackTrace();
		}
		printContext(jsEngine);

		Invocable inv = (Invocable) jsEngine;
		counter.reset();
		for (int i = 0; i < iter; i++) {
			try {
				double x = (Double) inv.invokeFunction("add", 1);
				System.out.println(x);
			} catch (ScriptException e) {
				e.printStackTrace();
			} catch (NoSuchMethodException e) {
				e.printStackTrace();
			}
		}
		printContext(jsEngine);
		counter.stop();
	}

	private static void printContext(ScriptEngine jsEngine) {
		Bindings b = jsEngine.getContext().getBindings(
				ScriptContext.ENGINE_SCOPE);
		Iterator<Entry<String, Object>> it = b.entrySet().iterator();
		while (it.hasNext()) {
			Entry<String, Object> curr = it.next();
			System.out.println(curr.getKey() + "," + curr.getValue());
		}
	}

	private static void compile(ScriptEngine jsEngine) {
		System.out.println("compiling");
		Compilable comp = (Compilable) jsEngine;
		try {
			CompiledScript compScript = comp.compile(script);
			counter.reset();
			for (int i = 0; i < iter; i++) {
				try {
					compScript.eval();
				} catch (ScriptException ex) {
					ex.printStackTrace();
				}
			}
			counter.stop();
		} catch (ScriptException e) {
			e.printStackTrace();
		}
	}

	private static void eval(ScriptEngine jsEngine) {
		System.out.println("eval");
		counter.reset();
		for (int i = 0; i < iter; i++) {
			try {
				jsEngine.eval(script);
			} catch (ScriptException ex) {
				ex.printStackTrace();
			}
		}
		counter.stop();
	}

}
