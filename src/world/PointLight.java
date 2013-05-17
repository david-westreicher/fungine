package world;

public class PointLight extends GameObject {

	public static final String LIGHT_OBJECT_TYPE_NAME = "PointLight";
	public float radius = (float) (Math.random() * 1000 + 1000);
	public boolean shadow = false;
	public boolean fallof = true;

	public void setRadius(float radius) {
		this.radius = radius;
		this.setSize(radius / 20, radius / 20, radius / 20);
	}

	public PointLight() {
		super(LIGHT_OBJECT_TYPE_NAME);
	}

}
