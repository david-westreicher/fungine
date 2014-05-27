package test;

import javax.vecmath.Vector3f;

import util.Log;
import util.MathHelper;
import util.MathHelper.Tansformation;

public class MathTest {

	public static void main(String[] args) {
		float[][] realPoints = new float[][] { new float[] { 0, 0, 0 },
				new float[] { 0, 0, 1 }, new float[] { 0, 1, 1 } };
		float[][] currPoints = new float[][] { new float[] { 0, 0, 0 },
				new float[] { 0, 1, 0 }, new float[] { 1, 1, 0 } };
		Tansformation t = MathHelper.getTransformation(realPoints, currPoints);
		Log.log(t);
		Vector3f tmp = new Vector3f(0, 1, 1);
		//t.transform(tmp);
		Log.log(tmp);
	}

}
