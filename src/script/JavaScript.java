package script;

import game.Game;
import io.IO;

import java.io.File;
import java.net.MalformedURLException;
import java.net.URL;
import java.net.URLClassLoader;
import java.util.ArrayList;
import java.util.Collection;
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
	private static Map<String, List<RuntimeScript>> allScripts = new HashMap<String, List<RuntimeScript>>();

	public interface RuntimeScript {
		public void update(List<GameObject> go);

		public void init(GameObjectType gameObjectType);

		public void exit();

		public String getGameObjectType();
	}

	/*
	 * public static RuntimeScript getScript(String name, GameObjectType goType)
	 * { if (name == null) return null; RuntimeScript ret =
	 * allScripts.get("scripts/" + name); if (ret == null) compile(name,
	 * goType); return ret; }
	 */

	private static void compile(final String name) {
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
				final List<RuntimeScript> rts = new ArrayList<RuntimeScript>();
				final List<GameObjectType> gots = new ArrayList<GameObjectType>();
				for (GameObjectType got : GameObjectType.getTypes()) {
					if (name.equals(got.getRuntimeScript())) {
						rts.add((RuntimeScript) cls.newInstance());
						gots.add(got);
					}
				}

				final List<RuntimeScript> oldScripts = allScripts
						.get("scripts/" + name);

				Game.INSTANCE.loop.mechanics.addRunnable(new Runnable() {
					@Override
					public void run() {
						if (oldScripts != null)
							for (RuntimeScript rs : oldScripts)
								rs.exit();
						for (int i = 0; i < gots.size(); i++)
							rts.get(i).init(gots.get(i));
						allScripts.put("scripts/" + name, rts);
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
		if (!s.contains("scripts/games"))
			compile(s.substring("scripts/".length()));
	}

	public static void reset() {
		for (List<RuntimeScript> listRS : allScripts.values())
			for (RuntimeScript rs : listRS)
				rs.exit();
		allScripts.clear();
	}

	public static Collection<RuntimeScript> getScripts() {
		List<RuntimeScript> all = new ArrayList<RuntimeScript>();
		for (List<RuntimeScript> list : allScripts.values()) {
			all.addAll(list);
		}
		return all;
	}

	public static void compileIfNew(String runtimeScript) {
		if (allScripts.get("scripts/" + runtimeScript) == null)
			scriptChanged("scripts/" + runtimeScript);
	}

}
