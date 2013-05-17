package input;

import game.Game;
import rendering.RenderUpdater;
import settings.Settings;

import com.jogamp.newt.event.KeyEvent;
import com.jogamp.newt.event.KeyListener;
import com.jogamp.newt.event.MouseEvent;
import com.jogamp.newt.event.MouseListener;

public class CanvasListener implements MouseListener, KeyListener {
	private Input input;

	public CanvasListener() {
		input = Game.INSTANCE.input;
	}

	@Override
	public void mouseDragged(MouseEvent e) {
		int x = e.getX();
		int y = e.getY();
		input.mouse.set(x, y);
		RenderUpdater.getBrowser().mouseMoved(x, y);
	}

	@Override
	public void mouseMoved(MouseEvent e) {
		int x = e.getX();
		int y = e.getY();
		input.mouse.set(e.getX(), e.getY());
		RenderUpdater.getBrowser().mouseMoved(x, y);
	}

	@Override
	public void mouseClicked(MouseEvent e) {
	}

	@Override
	public void mouseEntered(MouseEvent e) {
		input.mouse.set(e.getX(), e.getY());

	}

	@Override
	public void mouseExited(MouseEvent e) {
		input.mouse.set(e.getX(), e.getY());
	}

	@Override
	public void mousePressed(MouseEvent e) {
		int buttonNum = e.getButton() - 1;
		input.mouse.down[buttonNum] = true;
		RenderUpdater.getBrowser().mouseButton(buttonNum,
				input.mouse.down[buttonNum]);
	}

	@Override
	public void mouseReleased(MouseEvent e) {
		int buttonNum = e.getButton() - 1;
		input.mouse.down[buttonNum] = false;
		RenderUpdater.getBrowser().mouseButton(buttonNum,
				input.mouse.down[buttonNum]);
	}

	@Override
	public void keyPressed(KeyEvent e) {
		input.keyboard.pressed(e.getKeyChar());
		RenderUpdater.getBrowser().keyEvent(e, true);
	}

	@Override
	public void keyReleased(KeyEvent e) {
		if (e.getKeyCode() == KeyEvent.VK_F1) {
			Settings.USE_BROWSER = !Settings.USE_BROWSER;
			if (Settings.USE_BROWSER) {
				Game.INSTANCE.loop.pauseLogic();
				Game.INSTANCE.hideMouse(false);
				Settings.SHOW_STATUS = false;
				RenderUpdater.getBrowser().debugSite();
			} else {
				Settings.SHOW_STATUS = true;
				Game.INSTANCE.loop.continueLogic();
				RenderUpdater.getBrowser().restoreSite();
			}
		}
		input.keyboard.released(e.getKeyChar());
		RenderUpdater.getBrowser().keyEvent(e, false);
	}

	@Override
	public void keyTyped(KeyEvent e) {
		RenderUpdater.getBrowser().keyTyped(e);
	}

	@Override
	public void mouseWheelMoved(MouseEvent e) {
		input.mouse.wheel = e.getWheelRotation();
		RenderUpdater.getBrowser().mouseWheel(e);
	}

}
