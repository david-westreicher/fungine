package physics;

import java.util.List;
import java.util.Map;

import world.GameObject;

public interface AbstractPhysics {

	public void dispose();

	public void update(Map<String, List<GameObject>> objs);

	public void restart();
}
