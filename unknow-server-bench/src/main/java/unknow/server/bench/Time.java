package unknow.server.bench;

import org.openjdk.jmh.annotations.Benchmark;

public class Time {

	@Benchmark
	public long nanoTime() {
		return System.nanoTime();
	}

	@Benchmark
	public long currentMillies() {
		return System.currentTimeMillis();
	}
}
