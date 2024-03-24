package unknow.server.bench;

import java.io.PrintStream;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Paths;
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
		Options o = new OptionsBuilder().forks(1).measurementIterations(10).verbosity(VerboseMode.NORMAL).warmupIterations(5).build();

		try (PrintStream w = new PrintStream(Files.newOutputStream(Paths.get("bench.log")), false, StandardCharsets.UTF_8)) {
			for (Class<?> c : Arrays.asList(XmlBench.class)) {
				w.println();
				w.println(c.getSimpleName());
				Collection<RunResult> result = new Runner(new OptionsBuilder().parent(o).include(c.getName()).build()).run();
				ResultFormatFactory.getInstance(ResultFormatType.TEXT, w).writeOut(result);
				w.println();
			}
		}
	}
}
