package util;

import io.IO;

import java.io.BufferedReader;
import java.io.IOException;

import javax.media.opengl.GL2;

import rendering.GameObjectRenderer;

public class Md5Renderer extends GameObjectRenderer {
	public class Mesh {

	}

	public class Joint {

		private String name;
		private int parent;
		private float[] pos;
		private float[] quaternion;

		public Joint(String name, int parent, float[] pos, float[] quaternion) {
			this.name = name;
			this.parent = parent;
			this.pos = pos;
			this.quaternion = quaternion;
		}

	}

	private Joint[] joints;
	private int currentJoint;
	private Mesh[] meshes;
	private int currentMesh;

	private enum Bracket {
		JOINT, MESH
	};

	public Md5Renderer(String file) {
		Log.log(this, file);
		BufferedReader br = IO.read(file);
		try {
			String line = null;
			Bracket currentBracket = null;
			while ((line = br.readLine()) != null) {
				String split[] = line.trim().split("\\s+");
				// version
				if (split[0].equals("MD5Version") && !split[1].equals("10"))
					Log.err("md5 version " + split[1] + " not supported!");

				if (split[0].equals("numJoints")) {
					int jointNum = Integer.parseInt(split[1]);
					joints = new Joint[jointNum];
					Log.err("Number of joints: " + jointNum);
				}

				if (split[0].equals("numMeshes")) {
					int meshNum = Integer.parseInt(split[1]);
					meshes = new Mesh[meshNum];
					Log.err("Number of meshes: " + meshNum);
				}

				if (currentBracket == Bracket.JOINT) {
					if (currentJoint >= joints.length) {
						Log.err("couldn't parse joint: " + line);
						currentBracket = Bracket.MESH;
					} else {
						String name = split[0].replaceAll("\"", "");
						int parent = Integer.parseInt(split[1]);
						float[] pos = new float[3];
						for (int i = 3; i < 6; i++)
							pos[i - 3] = Float.parseFloat(split[i]);
						float[] quaternion = new float[3];
						for (int i = 8; i < 11; i++)
							quaternion[i - 8] = Float.parseFloat(split[i]);

						/*
						 * Log.err("new joint: " + name + ", " + parent + ", " +
						 * Arrays.toString(pos) + ", " +
						 * Arrays.toString(quaternion));
						 */

						joints[currentJoint++] = new Joint(name, parent, pos,
								quaternion);
					}
				}
				if (split[0].equals("joints")) {
					currentJoint = 0;
					currentBracket = Bracket.JOINT;
				}
				if (split[0].equals("mesh")) {
					currentMesh = 0;
					currentBracket = Bracket.MESH;
				}
			}
			br.close();
		} catch (IOException e) {
			e.printStackTrace();
		}
	}

	@Override
	public void init(GL2 gl) {
		// TODO Auto-generated method stub

	}

	@Override
	public void draw(GL2 gl) {
		// TODO Auto-generated method stub

	}

	@Override
	public void end(GL2 gl) {
		// TODO Auto-generated method stub

	}

	@Override
	public void drawSimple(GL2 gl) {
		// TODO Auto-generated method stub

	}

}
