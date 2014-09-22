package browser;

import game.Game;

import java.io.File;
import java.nio.ByteBuffer;

import javax.media.opengl.GL2;

import settings.Settings;
import util.Log;

import com.google.gson.Gson;
import com.jogamp.newt.event.KeyEvent;
import com.jogamp.newt.event.MouseEvent;
import com.jogamp.opengl.util.texture.Texture;

public class AwesomiumWrapper extends Browser {
	private static final boolean ENGINE_GUI = true;
	public static String ENGINE_GUI_FILE = "gui/gui.html";
	public static Runnable onLoadGUI;

	public AwesomiumWrapper() {
		AwesomiumHelper.init(!ENGINE_GUI);
		onLoadGUI = new Runnable() {
			@Override
			public void run() {
				sendObjectsToJS();
			}
		};
	}

	@Override
	public void mouseMoved(int x, int y) {
		AwesomiumHelper.mouseMoved(x, y);
	}

	@Override
	public void mouseButton(int i, boolean down) {
		// Log.log(this, "mouse down:" + down + ", #" + i);
		AwesomiumHelper.mouseButton(i, down);
	}

	@Override
	public void mouseWheel(MouseEvent e) {
	}

	@Override
	public void keyEvent(KeyEvent e, boolean b) {
	}

	@Override
	public void debugSite() {
		if (ENGINE_GUI)
			AwesomiumHelper.loadFile(new File(ENGINE_GUI_FILE).getPath(),
					onLoadGUI);
		else {
			AwesomiumHelper.loadUrl("http://www.google.at");
		}
	}

	protected void sendObjectsToJS() {
		Log.log(this, "sending Objects to JS");
		if (Game.INSTANCE.world.getAllObjectsTypes().size() == 0)
			return;
		Gson gson = new Gson();
		try {
			String objectString = gson.toJson(Game.INSTANCE.world
					.getAllObjects());
			AwesomiumHelper.executeJavascript("window.sendReceiveObjects("
					+ objectString + ")");
		} catch (IllegalArgumentException e) {
			e.printStackTrace();
		}
		String settingsString = gson.toJson(Game.INSTANCE.loop.renderer
				.getSettings());
		AwesomiumHelper.executeJavascript("window.sendReceiveSettings("
				+ settingsString + ")");
	}

	@Override
	public void restoreSite() {
	}

	@Override
	public void keyTyped(KeyEvent e) {
		AwesomiumHelper.keyTyped(e);
	}

	@Override
	public Texture getTexture() {
		return null;
	}

	@Override
	public void render(GL2 gl) {
		gl.glPixelZoom(((float) Game.INSTANCE.getWidth() / Settings.WIDTH),
				-((float) Game.INSTANCE.getHeight() / Settings.HEIGHT));
		// ugly hack
		// http://www.gamedev.net/topic/438203-glrasterpos-gldrawpixel-and-discarding-images/
		gl.glRasterPos2i(0, 0);
		gl.glBitmap(0, 0, 0, 0, -Game.INSTANCE.getWidth() / 2,
				Game.INSTANCE.getHeight() / 2, null);
		ByteBuffer buffer = AwesomiumHelper.getBuffer();
		gl.glEnable(GL2.GL_BLEND);
		gl.glBlendFunc(GL2.GL_SRC_ALPHA, GL2.GL_ONE_MINUS_SRC_ALPHA);
		if (buffer != null)
			gl.glDrawPixels(Settings.WIDTH, Settings.HEIGHT, GL2.GL_BGRA,
					GL2.GL_UNSIGNED_BYTE, buffer);
		gl.glDisable(GL2.GL_BLEND);
	}

	@Override
	public void dispose(GL2 gl) {
		AwesomiumHelper.dispose();
	}

	@Override
	public boolean isDummy() {
		return false;
	}

}
