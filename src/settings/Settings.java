package settings;

import java.io.File;

import com.sun.jna.Platform;

public class Settings {
	public static String RESSOURCE_FOLDER = "ressources" + File.separator;
	public static String MAIN_SCRIPT = "Main.java";
	public static int WIDTH = 640;
	public static int HEIGHT = 800;
	public static boolean STEREO = true;
	public static boolean USE_FULL_SCREEN = false;
	public static String ENGINE_FOLDER = "engine" + File.separator;
	public static boolean VR = STEREO;
	public static boolean USE_BROWSER = false;
	public static boolean SHOW_STATUS = true;
	public static boolean LOW_GRAPHICS = true;
	public static final boolean IS_WINDOWS = Platform.isWindows();
}
