package test;

import game.Game;
import io.IO;

import java.io.File;
import java.lang.reflect.Field;
import java.util.HashMap;

import settings.Settings;
import util.Log;
import util.Util;

public class OpenGLTest {
	static {
		if (!Settings.IS_WINDOWS)
			// fix for jdk1.6 on linux
			System.setProperty("java.awt.headless", "false");
	}

	public static void main(String[] args) {
		Log.getInstance();
		if (args.length > 0) {
			Settings.RESSOURCE_FOLDER = args[0] + File.separator;
			Log.log(OpenGLTest.class, "Ressource folder is: "
					+ Settings.RESSOURCE_FOLDER);
		}
		if (args.length > 1)
			try {
				readSettings(args[1]);
			} catch (NumberFormatException e) {
				e.printStackTrace();
			} catch (IllegalArgumentException e) {
				e.printStackTrace();
			} catch (IllegalAccessException e) {
				e.printStackTrace();
			}

		// Log.getInstance().excludeFromLogging(Reflection.class);
		// Log.getInstance().excludeFromLogging(AwesomiumHelper.class);
		Game g = new Game();
		Util.sleep(500);
		g.start();
	}

	private static void readSettings(String file) throws NumberFormatException,
			IllegalArgumentException, IllegalAccessException {
		File f = new File(file);
		if (f.exists()) {
			Log.log(OpenGLTest.class, "Reading config file: " + file);
			String strCfg = IO.readToString(f);
			HashMap<String, String> varVals = new HashMap<String, String>();
			for (String s : strCfg.split("\\r|\\n")) {
				String[] varVal = s.split("=");
				varVals.put(varVal[0], varVal[1]);
			}
			for (Field field : Settings.class.getFields()) {
				String val = varVals.get(field.getName());
				if (val != null) {
					if (field.getType().equals(int.class))
						field.setInt(field, Integer.parseInt(val));
					if (field.getType().equals(boolean.class))
						field.setBoolean(field, Boolean.parseBoolean(val));
				}
			}
			for (Field field : Settings.class.getFields()) {
				String log = field.getName() + "=";
				if (field.getType().equals(int.class))
					log += field.getInt(field);
				if (field.getType().equals(boolean.class))
					log += field.getBoolean(field);
				if (field.getType().equals(String.class))
					log += (String) field.get(field);
				Log.log(OpenGLTest.class, log);
			}
		} else {
			Log.err("Cant find settings file:" + file);
		}
	}
}
