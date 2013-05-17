package util;

import io.IO;

import java.io.BufferedReader;
import java.io.IOException;
import java.util.Collection;
import java.util.HashMap;
import java.util.Map;

public class MaterialLibrary {
	private Map<String, Material> materials = new HashMap<String, Material>();

	public MaterialLibrary(String file) {
		Log.log(this, "openeing material file: " + file);
		BufferedReader br = IO.read(file);
		if (br != null)
			try {
				String line;
				Material currentMaterial = null;
				while ((line = br.readLine()) != null) {
					line = line.trim();
					String[] split = line.split("\\s+");
					if (line.startsWith("newmtl")) {
						String name = split[1];
						currentMaterial = new Material(name);
						materials.put(name, currentMaterial);
					}
					if (line.startsWith("map_Kd") || line.startsWith("map_Ka")) {
						currentMaterial.texture = "img/" + split[1];
					}

					if (line.startsWith("Kd")) {
						currentMaterial.color = new float[] {
								Float.parseFloat(split[1]),
								Float.parseFloat(split[2]),
								Float.parseFloat(split[3]) };
					}

					if (line.startsWith("map_d")) {
						currentMaterial.maskMap = "img/" + split[1];
					}
					if (line.toLowerCase().startsWith("map_bump")
							|| line.startsWith("bump")) {
						currentMaterial.normalMap = "img/" + split[1];
					}
					if (line.startsWith("spec")) {
						currentMaterial.specMap = "img/" + split[1];
					}
				}
				br.close();
			} catch (IOException e) {
				e.printStackTrace();
			}
	}

	public Material getMaterial(String name) {
		Material m = materials.get(name);
		if (m == null)
			throw new RuntimeException("couldn't find material: " + name);
		return m;
	}

	public Collection<Material> getMaterials() {
		return materials.values();
	}

}