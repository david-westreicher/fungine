package util;

public class InstantWorker extends Stoppable {

	public interface AbstractWork {

		void work();

		void post();

	}

	public InstantWorker() {
		super("InstantWorker");
	}

	private AbstractWork aw;

	public void run() {
		while (running) {
			synchronized (this) {
				try {
					this.wait();
				} catch (InterruptedException e) {
					e.printStackTrace();
				}
			}
			if (aw != null) {
				aw.work();
				aw.post();
			}
		}
	}

	public void work(AbstractWork aw) {
		this.aw = aw;
		synchronized (this) {
			this.notify();
		}
	}

}
