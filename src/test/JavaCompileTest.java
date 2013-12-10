package test;

import java.io.File;
import java.net.MalformedURLException;
import java.net.URL;
import java.net.URLClassLoader;

import javax.tools.JavaCompiler;
import javax.tools.ToolProvider;

public class JavaCompileTest {

	public static void main(String[] args) {
		String fileToCompile = "games/empty/";

		JavaCompiler compiler = ToolProvider.getSystemJavaCompiler();
		int compilationResult = compiler.run(null, null, null, fileToCompile
				+ "scripts/Main.java");
		if (compilationResult == 0) {
			System.out.println("Compilation is successful");
		} else {
			System.out.println("Compilation Failed");
		}
		try {
			URL u = new File(fileToCompile + "scripts/").toURI().toURL();
			System.out.println(u);
			URLClassLoader classLoader = new URLClassLoader(new URL[] { u },
					JavaCompileTest.class.getClassLoader());
			Class<?> cls = classLoader.loadClass("Main");
			Object instance = cls.newInstance();
			System.out.println(instance);
		} catch (MalformedURLException e) {
			e.printStackTrace();
		} catch (InstantiationException e) {
			e.printStackTrace();
		} catch (IllegalAccessException e) {
			e.printStackTrace();
		} catch (ClassNotFoundException e) {
			e.printStackTrace();
		}
	}
}
