package world;

import org.ode4j.ode.DBody;

public class Joint extends GameObject {
	public static final String JOINT_OBJECT_TYPE_NAME = "Joint";
	public Type jointType = Type.BALL;
	public GameObject[] body = new GameObject[2];
	public float[] hingeAxis;
	public float[] hingeAnchor;
	public float[] ballAnchor;
	public float[] sliderAxis;
	public float sliderLength;
	public Float maxStop = null;
	public Float minStop = null;
	public float angleRate;

	public static enum Type {
		BALL, HINGE, SLIDER, CONTACT, UNIVERSAL, HINGE2, FIXED, DUMMY
	};

	public Joint() {
		super(JOINT_OBJECT_TYPE_NAME);
		size[0] = size[1] = size[2] = 1;
		color[0] = color[1] = color[2] = 1;
	}

	public void setJointType(String t) {
		jointType = Type.valueOf(t);
	}

	public void setHingeAxis(float x, float y, float z) {
		hingeAxis = new float[] { x, y, z };
	}

	public void setHingeAnchor(float x, float y, float z) {
		hingeAnchor = new float[] { x, y, z };
	}

	public void setBallAnchor(float x, float y, float z) {
		ballAnchor = new float[] { x, y, z };
	}

	public void setSliderAxis(float x, float y, float z) {
		sliderAxis = new float[] { x, y, z };
	}

	public void setMinStop(float x) {
		minStop = x;
	}

	public void setMaxStop(float x) {
		maxStop = x;
	}

}
