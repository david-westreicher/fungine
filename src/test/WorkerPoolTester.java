package test;

import java.util.ArrayList;
import java.util.List;

import util.WorkerPool;
import util.WorkerPool.WorkerImpl;
import world.GameObject;

public class WorkerPoolTester {

	public static void main(String[] args) {
		WorkerPool workerPool = new WorkerPool();
		workerPool.start();
		WorkerImpl wi = new WorkerImpl() {

			@Override
			public void update(List<GameObject> gos, int subListStart,
					int subListEnd) {
				for (int i = subListStart; i < subListEnd; i++) {
					gos.get(i).pos[0]++;
				}
			}
		};
		List<GameObject> objs = new ArrayList<GameObject>();
		for (int i = 0; i < 100; i++)
			objs.add(new GameObject(null));
		for (int i = 0; i < 50000; i++) {
			workerPool.execute(objs, wi);
		}
		workerPool.dispose();
	}
}
