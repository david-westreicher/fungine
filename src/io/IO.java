package io;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.lang.reflect.Type;

import settings.Settings;
import util.Log;
import util.Worker;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;

public class IO {

	public interface LineComparer {

		void compareLine(String line, StringBuilder sb);

	}

	private static Worker ioWorker;

	public static BufferedReader read(String file) {
		return read(Settings.RESSOURCE_FOLDER, file);
	}

	public static BufferedReader read(String dir, String file) {
		BufferedReader in = null;
		file = dir + file;
		try {
			in = new BufferedReader(new FileReader(file));
		} catch (FileNotFoundException e) {
			Log.err(IO.class, "couldn't find: " + file);
			return null;
		}
		return in;
	}

	public static String readToString(String file) {
		return readToString(Settings.RESSOURCE_FOLDER, file, null);
	}

	public static String readToString(String dir, String file) {
		return readToString(dir, file, null);
	}

	public static String readToString(String dir, String file, LineComparer lc) {
		BufferedReader br = read(dir, file);
		return readToString(br, lc);
	}

	private static String readToString(BufferedReader br, LineComparer lc) {
		StringBuilder sb = new StringBuilder();
		String line;
		try {
			while ((line = br.readLine()) != null) {
				if (lc != null)
					lc.compareLine(line, sb);
				sb.append(line);
				sb.append("\n");
			}
			br.close();
		} catch (IOException e) {
			e.printStackTrace();
		}
		return sb.toString();
	}

	public static <T> T[] readFromJson(String file, Type t) {
		BufferedReader br = read(file);
		if (br == null)
			return (T[]) new Object[0];
		T result[] = new Gson().fromJson(br, t);
		try {
			br.close();
		} catch (IOException e) {
			e.printStackTrace();
		}
		return result;
	}

	public static <T> void writeToJson(String file, T[] players) {
		String json = new GsonBuilder().setPrettyPrinting().create()
				.toJson(players);
		write(file, json);
	}

	private static void write(String file, String json) {
		file = Settings.RESSOURCE_FOLDER + file;
		BufferedWriter out;
		try {
			out = new BufferedWriter(new FileWriter(file));
			out.write(json);
			out.close();
		} catch (IOException e) {
			Log.err(IO.class, "couldn't write to file " + file);
		}
	}

	public static void queue(Runnable r) {
		if (ioWorker == null) {
			ioWorker = new Worker(200, "IOWorker");
			ioWorker.start();
		}
		ioWorker.addJob(r);
	}

	public static BufferedWriter getWriteBuffer(String file) throws IOException {
		File f = new File(Settings.RESSOURCE_FOLDER + file);
		f.createNewFile();
		BufferedWriter bw = new BufferedWriter(new FileWriter(f));
		return bw;
	}

	public static String readToString(File f) {
		return readToString(read(f.getPath(), ""), null);
	}
}
