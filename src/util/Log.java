package util;

import java.awt.Toolkit;
import java.io.PrintStream;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;

public class Log {
	private static Log instance;
	private static List<String> excludedClasses = new ArrayList<String>();
	private static PrintStream realError;

	private Log() {
		Log.log(this, "new log");
		realError = System.err;
		// TODO quick fix disable real error output
		/*
		 * try { System.setErr(new PrintStream(new File("err.txt"))); } catch
		 * (FileNotFoundException e) { e.printStackTrace(); }
		 */
	}

	public static void log(Object o) {
		System.out.println(time() + "Logger: " + o);
	}

	private static String print(Object o) {
		StringBuilder sb = new StringBuilder();
		if (o instanceof int[] || o instanceof Integer[]) {
			Integer[] oi = (Integer[]) o;
			sb.append("[");
			for (int i = 0; i < oi.length; i++) {
				if (i == oi.length - 1) {
					sb.append(oi[i]);
				} else
					sb.append(oi[i] + ", ");
			}
			sb.append("]");
		} else
			sb.append(o.toString());
		return sb.toString();
	}

	public static void log(Object inst, Object... o) {
		log(inst.getClass(), o);
	}

	public static void log(Object inst, float[] o) {
		String className = inst.getClass().getName();
		if (!excludedClasses.contains(className)) {
			System.out.print(time() + className + ": ");
			for (int i = 0; i < o.length; i++)
				System.out.print(o[i] + ((i < o.length - 1) ? ", " : ""));
			System.out.print("\n");
		}
	}

	public static void log(Object inst, int[] o) {
		String className = inst.getClass().getName();
		if (!excludedClasses.contains(className)) {
			System.out.print(time() + className + ": ");
			for (int i = 0; i < o.length; i++)
				System.out.print(o[i] + ((i < o.length - 1) ? ", " : ""));
			System.out.print("\n");
		}
	}

	public static void log(Class<?> clss, Object... o) {
		String className = clss.getName();
		if (!excludedClasses.contains(className)) {
			System.out.print(time() + className + ": ");
			for (int i = 0; i < o.length; i++)
				System.out.print(o[i] + ((i < o.length - 1) ? ", " : ""));
			System.out.print("\n");
		}
	}

	private static String time() {
		return new Date().toLocaleString() + "-";
	}

	public static void err(String string) {
		Toolkit tk = Toolkit.getDefaultToolkit();
		tk.beep();
		realError.println(time() + string);
	}

	public static void err(Object o, String string) {
		err(o.getClass().getName() + ": " + string);
	}

	public static void err(Class<?> clss, String string) {
		err(clss.getName() + ": " + string);
	}

	public static Log getInstance() {
		if (instance == null)
			instance = new Log();
		return instance;
	}

	public void excludeFromLogging(Object o) {
		String name = o.getClass().getName();
		Log.err("excluding class " + name + " from logging");
		excludedClasses.add(name);
	}

	public void excludeFromLogging(Class<?> clss) {
		String name = clss.getName();
		Log.err("excluding class " + name + " from logging");
		excludedClasses.add(name);
	}
}
