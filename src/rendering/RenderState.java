package rendering;

public class RenderState {
	public boolean materials = true;
	public boolean depthOnly = false;
	public boolean stereo = false;

	@Override
	public String toString() {
		return "RenderState [materials=" + materials + ", depthOnly="
				+ depthOnly + "]";
	}
}