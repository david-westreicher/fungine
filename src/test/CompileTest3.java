package test;

import java.io.File;
import java.lang.reflect.Method;
import java.net.URI;
import java.net.URL;
import java.net.URLClassLoader;
import java.util.Arrays;
import java.util.List;

import javax.tools.JavaCompiler;
import javax.tools.JavaFileObject;
import javax.tools.SimpleJavaFileObject;
import javax.tools.StandardJavaFileManager;
import javax.tools.StandardLocation;
import javax.tools.ToolProvider;

public class CompileTest3 {

	private static final File TEMP_PATH = new File("/tmp/");

	public static void main(String[] args) throws Exception {
		String thisPackage = CompileTest3.class.getPackage().getName();
		String className = thisPackage + ".Formula";
		String body = "package " + thisPackage + ";   "
				+ "public class Formula {         "
				+ "    double calculateFails() {  "
				+ "        return 42;             "
				+ "    }                          "
				+ "}                              ";

		compile(className, body, TEMP_PATH);

		loadClass(className, TEMP_PATH);

	}

	private static void loadClass(String className, File path) throws Exception {
		// com/stackoverflow/Test1.class
		String idem = CompileTest3.class.getCanonicalName().replace(".", "/")
				+ ".class";
		// The URL
		String classFile = CompileTest3.class.getClassLoader()
				.getResource(idem).toString();
		// Remove the class name
		String repository = classFile.substring(0,
				classFile.length() - idem.length());

		URL location = new URL(repository);
		URLClassLoader loader = new URLClassLoader(new URL[] { path.toURL(),
				location }, null);

		Class<?> formulaClass = loader.loadClass(className);

		Class<CompileTest3> test1 = (Class<CompileTest3>) loader
				.loadClass(CompileTest3.class.getCanonicalName());

		assert test1.getClassLoader().equals(formulaClass.getClassLoader());

		Method compute = test1.getDeclaredMethod("compute", Class.class);
		compute.invoke(test1, formulaClass);

		// loader.close();
	}

	public static void compute(Class<?> formulaClass) throws Exception {
		Method calculateFails = formulaClass
				.getDeclaredMethod("calculateFails");
		// next line throws exception:
		double valueFails = (Double) calculateFails.invoke(formulaClass
				.newInstance());
		System.out.println("HOORAY! valueFails = " + valueFails);
	}

	private static void compile(String className, String body, File path)
			throws Exception {
		List<JavaSourceFromString> sourceCode = Arrays
				.asList(new JavaSourceFromString(className, body));

		JavaCompiler compiler = ToolProvider.getSystemJavaCompiler();
		StandardJavaFileManager fileManager = compiler.getStandardFileManager(
				null, null, null);
		fileManager.setLocation(StandardLocation.CLASS_OUTPUT,
				Arrays.asList(path));
		boolean ok = compiler.getTask(null, fileManager, null, null, null,
				sourceCode).call();

		System.out.println("compilation ok = " + ok);
	}

	public static class JavaSourceFromString extends SimpleJavaFileObject {
		final String code;

		JavaSourceFromString(String name, String code) {
			super(URI.create("string:///" + name.replace('.', '/')
					+ JavaFileObject.Kind.SOURCE.extension),
					JavaFileObject.Kind.SOURCE);
			this.code = code;
		}

		@Override
		public CharSequence getCharContent(boolean ignoreEncodingErrors) {
			return code;
		}
	}
}