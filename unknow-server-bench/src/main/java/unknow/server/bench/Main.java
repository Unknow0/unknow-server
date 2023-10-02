package unknow.server.bench;

import org.openjdk.jmh.runner.Runner;
import org.openjdk.jmh.runner.options.Options;
import org.openjdk.jmh.runner.options.OptionsBuilder;

public class Main {
	public static void main(String[] args) throws Exception {
		Options options = new OptionsBuilder().forks(1).measurementIterations(10).build();
		new Runner(options).run();
	}
}
