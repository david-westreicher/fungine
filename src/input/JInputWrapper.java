package input;

import io.IO;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import net.java.games.input.Component;
import net.java.games.input.Controller;
import net.java.games.input.DirectAndRawInputEnvironmentPlugin;
import util.Log;
import util.RepeatedThread;

public class JInputWrapper {

	public class Player {
		private Map<String, String> stringComponents = new HashMap<String, String>();
		private transient Map<String, Component> components;
		private transient boolean active = true;

		public void init(JInputWrapper wrapper) {
			for (String name : stringComponents.keySet()) {
				String componentName = stringComponents.get(name);
				Component c = wrapper.getComponent(componentName);
				if (c == null) {
					active = false;
					return;
				}
				active = true;
				if (components == null)
					components = new HashMap<String, Component>();
				components.put(name, c);
			}
		}

		public float getValue(String name) {
			return components.get(name).getPollData();
		}

		public void add(String key, String value) {
			stringComponents.put(key, value);
		}

		@Override
		public String toString() {
			return "Player [stringComponents=" + stringComponents + "]";
		}

	}

	private Map<String, Controller> controllers = new HashMap<String, Controller>();
	private List<Controller> usedControllers = new ArrayList<Controller>();
	private List<Controller> toDelete = new ArrayList<Controller>();
	private List<Player> players;
	protected int controllerNum;
	private boolean refreshNextTime = false;
	private Controller[] allControllers;

	public JInputWrapper() {
		new Thread(new Runnable() {

			@Override
			public void run() {
				allControllers = DirectAndRawInputEnvironmentPlugin
						.getDefaultEnvironment().getControllers();
				refreshNextTime = true;
			}
		}).start();
		new RepeatedThread(10000, Thread.MIN_PRIORITY, "JInputWrapper_updater") {

			@Override
			protected void executeRepeatedly() {
				allControllers = new DirectAndRawInputEnvironmentPlugin()
						.getControllers();
				int currentControllerNum = allControllers.length;
				if (currentControllerNum != controllerNum) {
					refreshNextTime = true;
				}
			}
		}.start();
	}

	public void refresh() {
		if (controllers == null)
			controllers = new HashMap<String, Controller>();
		if (usedControllers == null)
			usedControllers = new ArrayList<Controller>();
		if (players == null)
			players = new ArrayList<Player>();
		controllers.clear();
		usedControllers.clear();
		players.clear();
		controllerNum = allControllers.length;
		for (Controller c : allControllers) {
			addController(c);
		}
		Player[] playersArr = IO.readFromJson("controller.cfg", Player[].class);
		if (playersArr != null) {
			Log.log(this, "Successfully loaded controller config: "
					+ playersArr.length + " players");
			for (Player p : playersArr) {
				Log.log(this, p);
				p.init(JInputWrapper.this);
				if (p.active) {
					players.add(p);
				}
			}
		} else {
			Log.err(this, "No controller config file found");
			playersArr = new Player[1];
			playersArr[0] = new Player();
			playersArr[0].add("up", "Logitech RumblePad 2 USB0;Button 0");
			IO.writeToJson("controller.cfg", playersArr);
		}
	}

	public Component getComponent(String string) {
		String[] controllerComponent = string.split(";");
		Log.log(this, "Looking for controller: " + controllerComponent[0]);
		Controller c = controllers.get(controllerComponent[0]);
		if (c != null) {
			for (Component comp : c.getComponents()) {
				// Log.log(this, controllerComponent[1] + "==" +
				// comp.getName());
				if (comp.getName().equals(controllerComponent[1])) {
					Log.log(this, "controller= " + c.getName(),
							"found component:" + controllerComponent[1]);
					if (!usedControllers.contains(c))
						usedControllers.add(c);
					return comp;
				}
			}
		}
		Log.err(this, "couldn't find " + string);
		return null;
	}

	public void update() {
		if (refreshNextTime) {
			refresh();
			refreshNextTime = false;
		}
		toDelete.clear();
		for (Controller c : usedControllers) {
			if (!c.poll()) {
				toDelete.add(c);
			}
		}
		usedControllers.removeAll(toDelete);
	}

	private void addController(Controller c) {
		if (c.getRumblers() != null && c.getRumblers().length > 0)
			Log.log(this, c.getName() + " has rumblers");
		int i = c.getPortNumber();
		String name = c.getName();
		while (controllers.containsKey(name + i)) {
			i++;
		}
		controllers.put(name + i, c);
	}

	public float getKey(int player, String name) {
		if (players == null || player >= players.size())
			return 0;
		Component c = players.get(player).components.get(name);
		return (c == null) ? 0 : c.getPollData();
	}

}