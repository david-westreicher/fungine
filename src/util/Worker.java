package util;

import java.util.ArrayList;
import java.util.List;

public class Worker extends RepeatedThread {

	private List<Runnable> jobs = new ArrayList<Runnable>();
	private boolean oneJobAtaTime;

	public Worker(long waitTime, String name, boolean oneJobAtaTime) {
		super(waitTime, name);
		this.oneJobAtaTime = oneJobAtaTime;
	}

	public Worker(long waitTime, String name) {
		this(waitTime, name, true);
	}

	@Override
	protected void executeRepeatedly() {
		if (oneJobAtaTime) {
			if (jobs.size() > 0)
				jobs.remove(0).run();
		} else {
			while (jobs.size() > 0)
				jobs.remove(0).run();
		}
	}

	public void addJob(Runnable r) {
		jobs.add(r);
	}
}
