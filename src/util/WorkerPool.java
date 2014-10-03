package util;

import java.util.ArrayList;
import java.util.List;

import world.GameObject;

public class WorkerPool {

	public static interface WorkerImpl {

		void update(List<GameObject> gos, int subListStart, int subListEnd);

	}

	private ArrayList<Worker> workers;
	private int workersFinished;

	public void start() {
		int numOfThreads = Runtime.getRuntime().availableProcessors();
		workers = new ArrayList<Worker>();
		for (int i = 0; i < numOfThreads; i++)
			workers.add(new Worker(this, i));
		for (Worker w : workers)
			w.start();
	}

	public void execute(List<GameObject> objs, WorkerImpl wi) {
		int size = objs.size();
		if (size < 1000) {
			wi.update(objs, 0, objs.size());
			return;
		}
		int perWorkerSize = size / workers.size();
		int subListStart = 0;
		workersFinished = 0;
		for (int i = 0; i < workers.size(); i++) {
			Worker w = workers.get(i);
			if (i == workers.size() - 1)
				w.execute(objs, wi, subListStart, size);
			else {
				w.execute(objs, wi, subListStart, subListStart + perWorkerSize);
				subListStart += perWorkerSize;
			}
		}
		try {
			synchronized (this) {
				if (workersFinished < workers.size())
					this.wait();
			}
		} catch (InterruptedException e) {
		}
	}

	public synchronized void workerFinished() {
		synchronized (this) {
			workersFinished++;
			if (workersFinished == workers.size()) {
				this.notify();
			}
		}
	}

	public void dispose() {
		for (Worker w : workers)
			w.dispose();
		synchronized (this) {
			this.notify();
		}
	}

	private static class Worker extends Thread {
		private boolean exit;
		private WorkerPool workerPool;
		private List<GameObject> gos;
		private WorkerImpl wi;
		private int subListEnd;
		private int subListStart;

		public Worker(WorkerPool workerPool, int id) {
			super("WorkerThread " + id);
			this.workerPool = workerPool;
		}

		public void run() {
			while (!exit) {
				synchronized (this) {
					if (wi == null)
						try {
							this.wait();
						} catch (InterruptedException e) {
							exit = true;
						}
				}
				if (wi != null)
					wi.update(gos, subListStart, subListEnd);
				wi = null;
				workerPool.workerFinished();
			}
		}

		public void execute(List<GameObject> subList, WorkerImpl wi,
				int subListStart, int subListEnd) {
			this.gos = subList;
			this.subListStart = subListStart;
			this.subListEnd = subListEnd;
			synchronized (this) {
				this.wi = wi;
				this.notify();
			}
		}

		public void dispose() {
			exit = true;
			this.interrupt();
		}
	}
}
