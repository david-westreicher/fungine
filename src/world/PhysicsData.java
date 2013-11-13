package world;

public class PhysicsData {

	public float mass = 1;
	public float[] force = new float[3];
	public float friction = 0;
	public boolean fixed = false;
	public LocomotionData locomotion = null;

	public void setForce(float x, float y, float z) {
		force[0] = x;
		force[1] = y;
		force[2] = z;
	}

	public void resetForce() {
		for (int i = 0; i < 3; i++)
			force[i] = 0;
	}

	public void setLocomoted(GameObject body, Joint jA, Joint jB, Joint jKneeA,
			Joint jKneeB, GameObject jFootA, GameObject jFootB,
			Joint jHeelA, Joint jHeelB, GameObject floor) {
		locomotion = new LocomotionData(body, jA, jB, jKneeA, jKneeB, jFootA,
				jFootB, jHeelA, jHeelB, floor);
	}
}
