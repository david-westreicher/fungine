package util;

import io.IO;

import java.io.BufferedReader;
import java.io.IOException;

public class WaveFrontObjLoader {
	public WaveFrontObjLoader(String file) {
		Log.log(this, "starting to parse: " + file);
		BufferedReader br = IO.read(file);
		if (br != null) {
			try {
				br.close();
			} catch (IOException e) {
				e.printStackTrace();
			}
		}
	}

}
