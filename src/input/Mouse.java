package input;

public class Mouse {

	public int[] pos = new int[2];
	public boolean[] down = new boolean[15];
	public int wheel = 0;

	public void set(int x, int y) {
		pos[0] = x;
		pos[1] = y;
	}

	public void reset() {
		wheel = 0;
	}

}
