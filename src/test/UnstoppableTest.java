package test;

import javax.media.opengl.GLProfile;

public class UnstoppableTest {

	/**
	 * @param args
	 */
	public static void main(String[] args) {
		GLProfile.getDefault();
		GLProfile.shutdown();
		// System.exit(0);
	}

}
