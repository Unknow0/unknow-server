package unknow.server.bench;

import java.io.BufferedReader;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.Formatter;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;
import java.util.TreeSet;
import java.util.function.Function;

import org.apache.commons.csv.CSVFormat;
import org.apache.commons.csv.CSVParser;
import org.apache.commons.csv.CSVRecord;
import org.apache.commons.math3.distribution.TDistribution;

public class ProcessResult {
	private static final CSVFormat JTL = CSVFormat.newFormat(',').builder().setHeader().setSkipHeaderRecord(true).setQuote('"').build();
	private static final CSVFormat H2 = CSVFormat.newFormat('\t');

	private final Map<String, Map<String, Result>> results = new HashMap<>();
	private final Set<String> tests = new TreeSet<>();

	public static void main(String[] args) throws IOException {
		ProcessResult process = new ProcessResult();
		for (String s : args) {
			Path path = Paths.get(s);
			String string = path.getFileName().toString();
			int i = string.lastIndexOf('.');
			if (i < 0)
				continue;
			String ext = string.substring(i + 1);
			string = string.substring(0, i);
			try (BufferedReader r = Files.newBufferedReader(path)) {
				if ("jtl".equals(ext))
					process.readJtl(r, string);
				else if ("h2".equals(ext))
					process.readH2(r, string);
			}
		}
		try (Formatter f = new Formatter(System.out)) {
			process.printResult(f);
		}
	}

	private void readJtl(BufferedReader r, String n) throws IOException {
		Map<String, Result> stats = results.computeIfAbsent(n, k -> new HashMap<>());
		try (CSVParser parser = CSVParser.parse(r, JTL)) {
			for (CSVRecord l : parser) {
				String name = l.get("label");
				tests.add(name);
				boolean e = !"true".equals(l.get("success"));
				double t = Long.parseLong(l.get("timeStamp")) / 1000.;
				double v = Long.parseLong(l.get("elapsed")) / 1000.;
				double c = Long.parseLong(l.get("Latency")) / 1000.;

				stats.computeIfAbsent(name, k -> new Result()).add(t, v, c, e);
			}
		}
	}

	private void readH2(BufferedReader r, String n) throws IOException {
		Result result = results.computeIfAbsent(n, k -> new HashMap<>()).computeIfAbsent("http2", k -> new Result());
		tests.add("http2");
		try (CSVParser parser = CSVParser.parse(r, H2)) {
			for (CSVRecord l : parser) {
				boolean e = !"200".equals(l.get(1));
				double t = Long.parseLong(l.get(0)) / 1000000.;
				double v = Long.parseLong(l.get(2)) / 1000000.;

				result.add(t, v, 0, e);
			}
		}
	}

	private void printResult(Formatter out) {
		List<String> list = new ArrayList<>(results.keySet());
		list.sort(null);

		Function<Result, String> thrpt = r -> Integer.toString((int) r.thrpt());
		Function<Result, String> lattency = r -> String.format("%.2f ± %.2f", r.latency.avg() * 1000, r.latency.err(.999) * 1000);
		Function<Result, String> error = r -> Long.toString(r.err());

		Map<String, Integer> lengths = new HashMap<>();
		for (String t : tests)
			lengths.put(t, Math.max(10, t.length()));
		computeLength(lengths, thrpt);
		computeLength(lengths, lattency);
		computeLength(lengths, error);

		StringBuilder sb = new StringBuilder("%10s");
		for (String t : tests)
			sb.append(" | %").append(lengths.get(t)).append('s');
		sb.append('\n');
		String fmt = sb.toString();

		Object[] l = new String[tests.size() + 1];
		l[0] = "";
		int i = 0;
		for (String t : tests)
			l[++i] = t;
		out.format(fmt, l);

		out.format("Throughput:\n");
		printTable(out, list, fmt, thrpt);
		out.format("\nLattency (ms before first byte):\n");
		printTable(out, list, fmt, lattency);
		out.format("\nErrors:\n");
		printTable(out, list, fmt, error);
	}

	private void computeLength(Map<String, Integer> lengths, Function<Result, String> v) {
		for (String s : results.keySet()) {
			for (Entry<String, Result> e : results.get(s).entrySet())
				lengths.merge(e.getKey(), v.apply(e.getValue()).length(), Math::max);
		}
	}

	private void printTable(Formatter out, List<String> servers, String fmt, Function<Result, String> v) {
		Object[] l = new String[tests.size() + 1];
		for (String s : servers) {
			l[0] = s;
			int i = 0;
			Map<String, Result> map = results.get(s);
			for (String t : tests) {
				Result r = map.get(t);
				l[++i] = r == null ? "" : v.apply(r);
			}
			out.format(fmt, l);
		}
	}

	private static class Result {
		private final Stat time;
		private final Stat latency;

		private long err;
		private double start = Double.MAX_VALUE;
		private double end = Double.MIN_VALUE;

		public Result() {
			this.time = new Stat();
			this.latency = new Stat();
		}

		public void add(double t, double v, double c, boolean e) {
			start = Math.min(this.start, t);
			end = Math.max(this.end, t + v);

			time.add(v);
			latency.add(c);

			if (e)
				this.err++;
		}

		public long err() {
			return err;
		}

		public double duration() {
			return end - start;
		}

		public double thrpt() {
			return time.cnt() / duration();
		}

		@Override
		public String toString() {
			double d = duration();
			return String.format("%.2f req/sec (lat: %.2f\u00B1%.2f)", time.cnt() / d, latency.avg(), latency.err(.999));

		}
	}

	private static class Stat {
		private long cnt;
		private double sum;
		private double sum2;

		public void add(double v) {
			cnt++;
			sum += v;
			sum2 += v * v;

		}

		public long cnt() {
			return cnt;
		}

		public double avg() {
			return sum / cnt;
		}

		public double sdev() {
			double avg = avg();
			return Math.sqrt(sum2 / cnt - avg * avg);
		}

		public double err(double confidence) {
			if (cnt <= 2)
				return Double.NaN;
			TDistribution tDist = new TDistribution(cnt - 1);
			double a = tDist.inverseCumulativeProbability(1 - (1 - confidence) / 2);
			return a * sdev() / Math.sqrt(cnt);
		}
	}
}
