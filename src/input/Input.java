package input;

public class Input {

	public Mouse mouse;
	public KeyBoard keyboard;
	private JInputWrapper jinput;

	public Input() {
		mouse = new Mouse();
		keyboard = new KeyBoard();
		// jinput = new JInputWrapper();
	}

	public void update() {
		if (jinput != null)
			jinput.update();
	}

	public float getKey(int player, String name) {
		return jinput.getKey(player, name);
	}

	public float getKey(String name) {
		return getKey(0, name);
	}

}
