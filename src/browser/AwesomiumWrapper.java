package browser;

import game.Game;

import java.io.File;
import java.nio.ByteBuffer;

import javax.media.opengl.GL;
import javax.media.opengl.GL2GL3;
import javax.media.opengl.GL3;

import rendering.util.RenderUtil;
import rendering.util.TextureHelper;
import settings.Settings;
import util.GLUtil;
import util.Log;

import com.google.gson.Gson;
import com.jogamp.newt.event.KeyEvent;
import com.jogamp.newt.event.MouseEvent;
import com.jogamp.opengl.util.texture.Texture;

public class AwesomiumWrapper {
	private static final boolean ENGINE_GUI = true;
	public static final String BROWSER_TEXTURE = "browserTexture";
	public static String ENGINE_GUI_FILE = "gui/gui.html";
	public static Runnable onLoadGUI;
	private TextureHelper textures;

	public AwesomiumWrapper() {
		AwesomiumHelper.init(!ENGINE_GUI);
		onLoadGUI = new Runnable() {
			@Override
			public void run() {
				sendObjectsToJS();
			}
		};
	}

	public void mouseMoved(int x, int y) {
		AwesomiumHelper.mouseMoved(x, y);
	}

	public void mouseButton(int i, boolean down) {
		// Log.log(this, "mouse down:" + down + ", #" + i);
		AwesomiumHelper.mouseButton(i, down);
	}

	public void mouseWheel(MouseEvent e) {
	}

	public void keyEvent(KeyEvent e, boolean b) {
	}

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

	public void restoreSite() {
	}

	public void keyTyped(KeyEvent e) {
		AwesomiumHelper.keyTyped(e);
	}

	public boolean isDummy() {
		return false;
	}

	public void render(GL2GL3 gl, GLUtil glutil) {
		int texture = textures.getTextureInformation(BROWSER_TEXTURE)[0];
		ByteBuffer buffer = AwesomiumHelper.getBuffer();
		gl.glBindTexture(GL3.GL_TEXTURE_2D, texture);
		gl.glTexImage2D(GL.GL_TEXTURE_2D, 0, GL3.GL_RGBA8, Settings.WIDTH,
				Settings.HEIGHT, 0, GL3.GL_BGRA, GL3.GL_UNSIGNED_BYTE, buffer);
		gl.glBindTexture(GL3.GL_TEXTURE_2D, 0);
		RenderUtil.drawTexture(gl, glutil, Game.INSTANCE.getWidth() / 2,
				Game.INSTANCE.getHeight() / 2, 0, Game.INSTANCE.getWidth(),
				Game.INSTANCE.getHeight(), texture, 0, 1);
	}

	public void dispose(GL2GL3 gl) {
		AwesomiumHelper.dispose();
	}

	public void init(TextureHelper textures, GL2GL3 gl3) {
		this.textures = textures;
		textures.createTex(gl3, BROWSER_TEXTURE, Settings.WIDTH,
				Settings.HEIGHT, true, GL3.GL_CLAMP_TO_EDGE, false, false);
	}

	public Texture getTexture() {
		return new Texture(textures.getTextureInformation(BROWSER_TEXTURE)[0]);
	}
}
