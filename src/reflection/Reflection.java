package reflection;

import game.Game;

import java.lang.reflect.Field;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.json.simple.JSONArray;
import org.json.simple.JSONValue;

import util.Log;

public class Reflection {
	private static Map<String, Field> fieldMap = new HashMap<String, Field>();
	private static Map<String, Method> methodMap = new HashMap<String, Method>();

	public static void execute(String code) {
		Log.log(Reflection.class, code);
		String[] split = code.split("\\.");
		// Log.log(this, "splitted", Arrays.toString(split));
		List<String> fields = new ArrayList<String>();
		boolean next = false;
		for (String s : split) {
			String lastChar = s.substring(s.length() - 1);
			// Log.log(this, s, next);
			if (next) {
				String current = fields.get(fields.size() - 1);
				current += "." + s;
				fields.set(fields.size() - 1, current);
				next = false;
			} else {
				fields.add(s);
			}
			try {
				Integer.parseInt(lastChar);
				next = true;
			} catch (NumberFormatException e) {
			}
		}
		// Log.log(this, "splitted", fields);
		split = new String[fields.size()];
		for (int i = 0; i < fields.size(); i++)
			split[i] = fields.get(i);
		execute(split, Game.INSTANCE, 0);
	}

	private static void execute(String[] objects, Object o, int level) {
		// Log.log(this, "execute", Arrays.toString(objects), o, level);
		if (level >= objects.length)
			return;
		if (objects[level].contains("=")) {
			int fieldIndex = objects[level].indexOf('=');
			String fieldName = objects[level].substring(0, fieldIndex);
			String value = objects[level].substring(fieldIndex + 1);
			for (int i = level + 1; i < objects.length; i++) {
				value += "." + objects[i];
			}
			Object newObject = JSONValue.parse(value);
			// Log.log(this, "setting field", fieldName, "on", o.getClass()
			// .getSimpleName(), "to", newObject, newObject.getClass()
			// .getName());
			if (newObject instanceof Double)
				newObject = ((Double) newObject).floatValue();
			Log.log(Reflection.class, "accessing field", fieldName,
					o.getClass());
			try {
				Field field = null;
				Class<? extends Object> clss = o.getClass();
				if (clss == null || fieldName == null)
					return;
				while (field == null) {
					try {
						field = clss.getDeclaredField(fieldName);
					} catch (NoSuchFieldException e) {
						Log.log(Reflection.class, fieldName
								+ " not found in "
								+ clss.getSimpleName()
								+ " looking into "
								+ ((clss.getSuperclass() != null) ? clss
										.getSuperclass().getSimpleName() : ""));
						clss = clss.getSuperclass();
					} catch (NullPointerException e) {
						return;
					}
				}
				field.setAccessible(true);
				field.set(o, newObject);
			} catch (SecurityException e) {
				e.printStackTrace();
			} catch (IllegalArgumentException e) {
				e.printStackTrace();
			} catch (IllegalAccessException e) {
				e.printStackTrace();
			}
		} else if (objects[level].endsWith(")")) {
			// method
			int methodNameIndex = objects[level].indexOf('(');
			String methodName = objects[level].substring(0, methodNameIndex);
			// Log.log(this, "methodName:", methodName);

			JSONArray jsonArgs = (JSONArray) JSONValue.parse("["
					+ objects[level].substring(methodNameIndex + 1,
							objects[level].length() - 1) + "]");
			// Log.log(this, "argsJson:", jsonArgs);
			Object[] args = new Object[0];
			if (jsonArgs != null && jsonArgs.size() > 0) {
				args = new Object[jsonArgs.size()];
			}
			Log.log(Reflection.class, "looking for " + methodName + " with "
					+ args.length + " args");
			Method rightMethod = methodMap.get(methodName + args.length);
			Class<? extends Object> clss = o.getClass();
			while (rightMethod == null) {
				if (clss == null || clss.getDeclaredMethods() == null)
					return;
				for (Method m : clss.getDeclaredMethods())
					// Log.log(this, "method", m, m.getParameterTypes().length,
					// args.length, o.getClass().getSimpleName());
					if (m.getName().equals(methodName)
							&& m.getParameterTypes().length == args.length) {
						rightMethod = m;
						rightMethod.setAccessible(true);
						methodMap.put(methodName + args.length, rightMethod);
						break;
					}
				Log.log(Reflection.class,
						methodName
								+ " not found in "
								+ clss.getSimpleName()
								+ " looking into "
								+ (clss.getSuperclass() != null ? clss
										.getSuperclass().getSimpleName()
										: "no supper class"));
				clss = clss.getSuperclass();
			}
			if (rightMethod != null)
				try {
					Object result = null;
					if (args.length == 0) {
						result = rightMethod.invoke(o);
					} else {
						for (int i = 0; i < jsonArgs.size(); i++) {
							Object jsonObject = jsonArgs.get(i);
							Log.log(Reflection.class, "args", i, jsonObject,
									jsonObject.getClass());
							if (jsonObject instanceof Long) {
								args[i] = ((Long) jsonObject).intValue();
							} else if (jsonObject instanceof Double) {
								args[i] = ((Double) jsonObject).floatValue();
							} else
								args[i] = rightMethod.getParameterTypes()[i]
										.cast(jsonObject);
						}
						result = rightMethod.invoke(o, args);
					}
					Log.log(Reflection.class, "invoked", rightMethod, "on ", o
							.getClass().getSimpleName(), result);
					if (result != null) {
						execute(objects, result, level + 1);
					}
				} catch (IllegalAccessException e) {
					e.printStackTrace();
				} catch (IllegalArgumentException e) {
					e.printStackTrace();
				} catch (InvocationTargetException e) {
					e.printStackTrace();
				}

		} else {
			Log.log(Reflection.class, "getting field", objects[level], "on ", o
					.getClass().getSimpleName());
			try {
				String fieldName = collectString(objects, level);
				Field field = fieldMap.get(fieldName);
				if (field == null) {
					Class<? extends Object> currentClass = o.getClass();
					while (field == null)
						try {
							field = currentClass
									.getDeclaredField(objects[level]);
						} catch (NoSuchFieldException e) {
							currentClass = currentClass.getSuperclass();
						}
					field.setAccessible(true);
					fieldMap.put(fieldName, field);
				}
				Object newObject = field.get(o);
				execute(objects, newObject, level + 1);
			} catch (IllegalArgumentException e) {
				e.printStackTrace();
			} catch (IllegalAccessException e) {
				e.printStackTrace();
			} catch (SecurityException e) {
				e.printStackTrace();
			}
		}
	}

	private static String collectString(String[] objects, int level) {
		StringBuilder sb = new StringBuilder();
		int index = 0;
		while (index <= level) {
			sb.append(objects[index++]);
			sb.append(".");
		}
		return sb.toString();
	}
}
