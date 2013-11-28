package rendering;

public class NiceRenderer extends RenderUpdater {

	public NiceRenderer() {
		super.executeInOpenGLContext(new Runnable() {
			@Override
			public void run() {
				textures.createGBuffer(gl, "gBuffer");
			}
		});
	}

	public void renderObjects() {
		// super.renderObjs.get(arg0);
	}
}
