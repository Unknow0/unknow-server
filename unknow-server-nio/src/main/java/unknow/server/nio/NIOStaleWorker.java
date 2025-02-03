package unknow.server.nio;

import java.util.concurrent.ConcurrentHashMap;

public class NIOStaleWorker implements Runnable {
	private final Thread t;
	private final ConcurrentHashMap<NIOConnectionAbstract, Object> connections;

	public NIOStaleWorker() {
		this.t = new Thread(this, "NIOStaleWorker");
		this.connections = new ConcurrentHashMap<>();
	}

	public void start() {
		t.start();
	}

	public void add(NIOConnectionAbstract co) {
		connections.put(co, this);
	}

	public void remove(NIOConnectionAbstract co) {
		connections.remove(co);
	}

	public void stop() {
		t.interrupt();
	}

	@Override
	public void run() {
		try {
			while (!Thread.interrupted()) {
				Thread.sleep(500);
				checkConnections(false);
			}
		} catch (@SuppressWarnings("unused") InterruptedException e) { // ok
		}

		try {
			while (!connections.isEmpty()) {
				Thread.sleep(100);
				checkConnections(true);
			}
		} catch (@SuppressWarnings("unused") InterruptedException e) { // ok
		}
	}

	private final void checkConnections(boolean close) {
		if (connections.isEmpty())
			return;
		long now = System.currentTimeMillis();
		for (NIOConnectionAbstract co : connections.keySet()) {
			if (!co.key.isValid() || co.closed(now, close))
				co.close();
		}
//		NIOConnectionAbstract first = connections.poll();
//		if (first == null)
//			return;
//
//		long now = System.currentTimeMillis();
//		int i = 0;
//		NIOConnectionAbstract co = first;
//		do {
//			if (!co.key.isValid() || co.closed(now, close))
//				co.close();
//			else
//				connections.offer(co);
//			co = connections.poll();
//		} while (i++ < 1000 && co != null && first != co);
	}
}
