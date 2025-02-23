package unknow.server.bench;

import java.io.BufferedReader;
import java.io.IOException;
import java.nio.file.DirectoryStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Formatter;
import java.util.HashMap;
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
	private static final CSVFormat CSV = CSVFormat.newFormat(' ');
	private static final CSVFormat H2 = CSVFormat.newFormat('\t');

	private static final double MILLI = 1000.;
	private static final double ΜICRO = 1000000.;

	private final String[] servers;
	private final Map<String, Map<String, Result>> results = new HashMap<>();
	private final Set<String> tests = new TreeSet<>();

	public ProcessResult(String[] servers) {
		this.servers = servers;
	}

	private void readCsv(BufferedReader r, String n) throws IOException {
		Map<String, Result> stats = results.computeIfAbsent(n, k -> new HashMap<>());
		try (CSVParser parser = CSVParser.parse(r, CSV)) {
			for (CSVRecord l : parser) {
				try {
					String name = l.get(0);
					if ("duration".equals(name)) {
						stats.computeIfAbsent(l.get(1), k -> new Result()).add(Double.parseDouble(l.get(2)), 0, -1, false);
						continue;
					}

					tests.add(name);

					boolean e = !("missing".equals(name) ? "404" : "200").equals(l.get(1));
					double v = Double.parseDouble(l.get(2));
					double c = Double.parseDouble(l.get(3));

					stats.computeIfAbsent(name, k -> new Result()).add(0, v, c, e);
				} catch (Exception e) {
					throw new IOException("line " + l.getRecordNumber() + " " + e.getMessage());
				}
			}
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

				result.add(t, v, -1, e);
			}
		}
	}

	private void printResult(Formatter out) {
		Function<Result, String> thrpt = r -> Integer.toString((int) r.thrpt());
		Function<Result, String> time = r -> r.time.cnt() == 0 ? "" : String.format("%.2f ± %.2f", r.time.avg(MILLI), r.time.err(MILLI, .999));
		Function<Result, String> lattency = r -> r.latency.cnt() == 0 ? "" : String.format("%.2f ± %.2f", r.latency.avg(ΜICRO), r.latency.err(ΜICRO, .999));
		Function<Result, String> error = r -> Long.toString(r.err());

		Map<String, Integer> lengths = new HashMap<>();
		for (String t : tests)
			lengths.put(t, Math.max(10, t.length()));
		computeLength(lengths, thrpt);
		computeLength(lengths, time);
		computeLength(lengths, lattency);
		computeLength(lengths, error);

		StringBuilder sb = new StringBuilder("%10s");
		for (String t : tests)
			sb.append(" | %").append(lengths.get(t)).append('s');
		sb.append("%n");
		String fmt = sb.toString();

		Object[] l = new String[tests.size() + 1];
		l[0] = "";
		int i = 0;
		for (String t : tests)
			l[++i] = t;
		out.format(fmt, l);

		out.format("Throughput:%n");
		printTable(out, fmt, thrpt);
		out.format("Response time (in ms):%n");
		printTable(out, fmt, time);
		out.format("%nLattency (µs before first byte):%n");
		printTable(out, fmt, lattency);
		out.format("%nErrors:%n");
		printTable(out, fmt, error);
	}

	private void computeLength(Map<String, Integer> lengths, Function<Result, String> v) {
		for (Map<String, Result> s : results.values()) {
			for (Entry<String, Result> e : s.entrySet())
				lengths.merge(e.getKey(), v.apply(e.getValue()).length(), Math::max);
		}
	}

	private void printTable(Formatter out, String fmt, Function<Result, String> v) {
		Object[] l = new String[tests.size() + 1];
		for (String s : servers) {
			l[0] = s;
			int i = 0;
			Map<String, Result> map = results.get(s);
			if (map == null)
				continue;
			for (String t : tests) {
				Result r = map.get(t);
				l[++i] = r == null ? "" : v.apply(r);
			}
			out.format(fmt, l);
		}
	}

	public static void main(String[] args) throws IOException {
		ProcessResult process = new ProcessResult(args);

		for (String s : args) {
			Path path = Paths.get("C:\\Users\\Unknow\\Downloads\\results-unknow", s);
//					"out", s);
			if (Files.exists(path)) {
				try (DirectoryStream<Path> out = Files.newDirectoryStream(path)) {
					for (Path p : out) {
						try (BufferedReader r = Files.newBufferedReader(p)) {
							process.readCsv(r, s);
						} catch (Exception e) {
							System.err.println(p + " " + e.getMessage());
						}
					}
				}
			}

			try (BufferedReader r = Files.newBufferedReader(Paths.get("out", s + ".jtl"))) {
				process.readJtl(r, s);
			} catch (IOException e) {
				System.err.println(e.getMessage());
			}
			try (BufferedReader r = Files.newBufferedReader(Paths.get("out", s + ".h2"))) {
				process.readH2(r, s);
			} catch (IOException e) {
				System.err.println(e.getMessage());
			}
		}
		try (Formatter f = new Formatter(System.out)) {
			process.printResult(f);
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
			if (c >= 0)
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
			return String.format("%.2f req/sec (lat: %.2f\u00B1%.2f)", time.cnt() / d, latency.avg(ΜICRO), latency.err(ΜICRO, .999));

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

		public double avg(double scale) {
			return sum * scale / cnt;
		}

		public double sdev(double scale) {
			double avg = avg(scale);
			scale *= scale;
			return Math.sqrt(sum2 * scale / cnt - avg * avg);
		}

		public double err(double scale, double confidence) {
			if (cnt <= 2)
				return Double.NaN;
			TDistribution tDist = new TDistribution(cnt - 1.);
			double a = tDist.inverseCumulativeProbability(1 - (1 - confidence) / 2);
			return a * sdev(scale) / Math.sqrt(cnt);
		}
	}
}
