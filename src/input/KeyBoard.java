package input;

import java.util.ArrayList;
import java.util.List;

public class KeyBoard {
	private List<Character> pressed = new ArrayList<Character>();

	public void pressed(char keyChar) {
		if (!pressed.contains(keyChar))
			pressed.add(keyChar);
	}

	public void released(char keyChar) {
		pressed.remove((Character) keyChar);
	}

	public float isDown(char keyChar) {
		return pressed.contains(keyChar) ? 1.0f : 0.0f;
	}

}
