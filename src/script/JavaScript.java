package script;

import game.Game;
import io.IO;

import java.io.File;
import java.net.MalformedURLException;
import java.net.URL;
import java.net.URLClassLoader;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import javax.tools.JavaCompiler;
import javax.tools.ToolProvider;

import settings.Settings;
import util.Log;
import world.GameObject;
import world.GameObjectType;

public class JavaScript {
	private static Map<String, RuntimeScript> allScripts = new HashMap<String, RuntimeScript>();

	public interface RuntimeScript {
		public void update(List<GameObject> go);

		public void init(GameObjectType gameObjectType);

		public void exit();
	}

	public static RuntimeScript getScript(String name, GameObjectType goType) {
		if (name == null)
			return null;
		RuntimeScript ret = allScripts.get("scripts/" + name);
		if (ret == null)
			compile(name, goType);
		return ret;
	}

	private static void compile(String name, final GameObjectType goType) {
		String fileToCompile = Settings.RESSOURCE_FOLDER + "scripts/";
		// TODO copy file into temp file for compiling with different name
		String newFile = Settings.RESSOURCE_FOLDER.replaceAll(File.separator,
				"") + name.replace(".java", "") + ".java";
		IO.copyFile(new File(fileToCompile + name), new File(fileToCompile
				+ newFile));
		replaceClassName(fileToCompile + newFile, name.replace(".java", ""),
				newFile.replace(".java", ""));
		JavaCompiler compiler = ToolProvider.getSystemJavaCompiler();
		int compilationResult = compiler.run(null, null, null, fileToCompile
				+ newFile);
		if (compilationResult == 0) {
			Log.log(JavaScript.class, "Compilation is successful");
			try {
				URL u = new File(fileToCompile).toURI().toURL();
				URLClassLoader classLoader = new URLClassLoader(
						new URL[] { u }, RuntimeScript.class.getClassLoader());
				Class<?> cls = classLoader.loadClass(newFile.replace(".java",
						""));
				Object inst = cls.newInstance();
				Log.log(JavaScript.class,
						Arrays.toString(inst.getClass().getInterfaces()));
				if (inst instanceof RuntimeScript)
					Log.log(JavaScript.class, "instance of RuntimeSCript");
				if (inst instanceof Object)
					Log.log(JavaScript.class, "instance of Object");
				final RuntimeScript object = (RuntimeScript) inst;
				allScripts.put("scripts/" + name, object);
				Game.INSTANCE.loop.mechanics.addRunnable(new Runnable() {
					@Override
					public void run() {
						object.init(goType);
					}
				});
			} catch (MalformedURLException e) {
				e.printStackTrace();
			} catch (InstantiationException e) {
				e.printStackTrace();
			} catch (IllegalAccessException e) {
				e.printStackTrace();
			} catch (ClassNotFoundException e) {
				e.printStackTrace();
			}
		} else {
			Log.err(JavaScript.class, "Compilation Failed");
		}
	}

	private static void replaceClassName(String file, final String name,
			final String newFile) {
		IO.replace(file, new IO.LineReplacer() {

			@Override
			public String replace(String line) {
				if (line.contains("public class " + name)) {
					line = line.replaceAll(name, newFile);
				}
				return line;
			}
		});
	}

	public static void scriptChanged(String s) {
		RuntimeScript rt = allScripts.remove(s);
		if (rt != null)
			rt.exit();
	}

	public static void reset() {
		for (RuntimeScript r : allScripts.values())
			r.exit();
		allScripts.clear();
	}

}
