package unknow.server.bench;

import java.util.Arrays;
import java.util.Collection;

import org.openjdk.jmh.results.RunResult;
import org.openjdk.jmh.results.format.ResultFormatFactory;
import org.openjdk.jmh.results.format.ResultFormatType;
import org.openjdk.jmh.runner.Runner;
import org.openjdk.jmh.runner.options.Options;
import org.openjdk.jmh.runner.options.OptionsBuilder;
import org.openjdk.jmh.runner.options.VerboseMode;

public class Main {
	public static void main(String[] args) throws Exception {
		Options o = new OptionsBuilder().forks(1).measurementIterations(10).verbosity(VerboseMode.SILENT).warmupIterations(5).build();

		for (Class<?> c : Arrays.asList(XmlBench.class)) {
			System.out.println(c.getSimpleName());
			Collection<RunResult> result = new Runner(new OptionsBuilder().parent(o).include(c.getName()).build()).run();
			ResultFormatFactory.getInstance(ResultFormatType.TEXT, System.out).writeOut(result);
			System.out.println();
		}
	}
}
