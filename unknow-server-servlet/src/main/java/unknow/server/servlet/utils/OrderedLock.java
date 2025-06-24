package unknow.server.servlet.utils;

import java.util.concurrent.locks.Condition;
import java.util.concurrent.locks.ReentrantLock;

public class OrderedLock {
	private final ReentrantLock lock;
	private final Condition cond;

	private int id;
	private int nextId;

	public OrderedLock() {
		lock = new ReentrantLock();
		cond = lock.newCondition();
	}

	public boolean isDone() {
		return id == nextId;
	}

	public int nextId() {
		return nextId++;
	}

	public void unlockNext() {
		lock.lock();
		try {
			id++;
			cond.signalAll();
		} finally {
			lock.unlock();
		}
	}

	public void waitUntil(int i) throws InterruptedException {
		lock.lock();
		try {
			while (id < i)
				cond.await();
		} finally {
			lock.unlock();
		}
	}
}