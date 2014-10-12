package rendering;

import javax.media.opengl.GL2;
import javax.media.opengl.GL3;

import node.NodeVar.VarConnection;
import rendering.nodes.DrawQuad;
import rendering.nodes.Number;
import rendering.nodes.RenderGraph;
import rendering.nodes.ShaderConstVec;
import rendering.nodes.TextureNode;
import rendering.nodes.UniformVec;
import rendering.util.GLRunnable;
import util.Log;
import world.Camera;
import world.GameObject;
import browser.AwesomiumHelper;
import browser.AwesomiumWrapper;

import com.google.gson.Gson;

public class NiceRenderer extends RenderUpdater {

	private static final int[] gbufferDrawBuffer = new int[] {
			GL3.GL_COLOR_ATTACHMENT0, GL3.GL_COLOR_ATTACHMENT1,
			GL3.GL_COLOR_ATTACHMENT2 };
	private RenderGraph g;

	public NiceRenderer() {
		AwesomiumWrapper.ENGINE_GUI_FILE = "gui/graph/graph.html";
		AwesomiumWrapper.onLoadGUI = new Runnable() {

			@Override
			public void run() {
				Log.log(this, "sending Objects to JS");
				Gson gson = new Gson();
				try {
					String objectString = gson.toJson(g);
					Log.log(this, "\n", objectString);
					AwesomiumHelper.executeJavascript("window.initGraph("
							+ objectString + ")");
				} catch (IllegalArgumentException e) {
					e.printStackTrace();
				}
			}
		};
		super.executeInOpenGLContext(new GLRunnable() {

			@Override
			public void run(GL2 gl) {
				textures.createGBuffer(gl, "gBuffer");
				g = new RenderGraph();
				TextureNode t = new TextureNode();
				DrawQuad d = new DrawQuad();
				Number width = new Number(5.0f);
				Number height = new Number(10.0f);
				UniformVec v = new UniformVec("uniformColor", 1, 1, 0);
				ShaderConstVec sv = new ShaderConstVec("constColor", 1, 1, 0);
				g.addNode(width);
				g.addNode(height);
				g.addNode(sv);
				g.addNode(v);
				g.addNode(t);
				g.addNode(d);
				g.addConnection(new VarConnection(sv.outConstVec, t.color));
				g.addConnection(new VarConnection(width.outNum, d.inWidth,
						Float.class));
				g.addConnection(new VarConnection(height.outNum, d.inHeight,
						Float.class));
				g.init();
			}
		});
	}

	public void renderObjects() {
		// * render into gbuffer
		// * * for every gameobjecttype:
		// * * * activate got shader, in=list of gameobjects, out: drawn gbuffer
		// * * * * for every object of gameobject
		// * * * * * render gameobject
		// * * * * * * if emmisive -> render to light buffer
		// * * * deactivate got shader
		// * render lights
		// * * activate deferred light shader wo. shadows
		// * * * for every non-shadow-light:
		// * * * * render light
		// * * deactivate shader
		// * * for every shadow-light:
		// * * * for every gameobjecttype:
		// * * * * activate got vertex shader
		// * * * * * render all objects into shadowmap
		// * * * * deactivate vertex shader
		// * * * activate deferred light shader with shadows
		// * * * * render light
		// * * * deactivate shader
		// * ssao
		// * mix results so far
		// * * add albedo + lights + ssao
		// * postrocessing
		// * * DOF
		// * * WARP
		// * * TONEMAP
		// draw();
		g.draw(gl);
		// g.changed("Number2", "number", Game.INSTANCE.loop.tick / 100.0f);
	}

	private void draw() {
		// renderIntoGBuffer();
		for (String type : renderObjs.keySet()) {
			if (type.equals(Camera.CAM_OBJECT_TYPE_NAME))
				continue;
			for (GameObject go : renderObjs.get(type)) {
			}
		}
	}

	private void renderIntoGBuffer() {
		bindGBuffer();
		gl3.glClear(GL3.GL_COLOR_BUFFER_BIT | GL3.GL_DEPTH_BUFFER_BIT);
		// draw all objects gbuffer style
		gl3.glBindFramebuffer(GL3.GL_FRAMEBUFFER, 0);
	}

	private void bindGBuffer() {
		gl3.glBindFramebuffer(GL3.GL_FRAMEBUFFER,
				textures.getTextureInformation("gBuffer")[0]);
		gl3.glFramebufferTexture2D(GL3.GL_FRAMEBUFFER,
				GL3.GL_COLOR_ATTACHMENT0, GL3.GL_TEXTURE_2D,
				textures.getTextureInformation("gBuffer")[1], 0);
		gl3.glFramebufferTexture2D(GL3.GL_FRAMEBUFFER,
				GL3.GL_COLOR_ATTACHMENT1, GL3.GL_TEXTURE_2D,
				textures.getTextureInformation("gBuffer")[2], 0);
		gl3.glFramebufferTexture2D(GL3.GL_FRAMEBUFFER,
				GL3.GL_COLOR_ATTACHMENT2, GL3.GL_TEXTURE_2D,
				textures.getTextureInformation("gBuffer")[3], 0);
		gl3.glDrawBuffers(3, gbufferDrawBuffer, 0);
	}

	@Override
	public void initShaderUniforms() {

	}

	@Override
	public void endShaderUniforms() {

	}
}
