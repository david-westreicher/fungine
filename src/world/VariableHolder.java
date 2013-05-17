package world;

import java.util.HashMap;
import java.util.Map;

public class VariableHolder {
	public Map<String, Object> vals = new HashMap<String, Object>();

	public void set(String name, Object val) {
		vals.put(name, val);
	}

	public Map<String, Object> getVars() {
		return vals;
	}

	public Object get(String name) {
		return vals.get(name);
	}

}
