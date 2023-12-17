package unknow.server.nio;

import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.Reader;
import java.net.InetSocketAddress;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;
import java.util.Properties;

import org.apache.commons.cli.CommandLine;
import org.apache.commons.cli.DefaultParser;
import org.apache.commons.cli.HelpFormatter;
import org.apache.commons.cli.Option;
import org.apache.commons.cli.Options;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import unknow.server.nio.NIOWorkers.RoundRobin;

public class NIOServerBuilder {
	private static final Logger logger = LoggerFactory.getLogger(NIOServerBuilder.class);

	private final Option help = Option.builder("h").longOpt("help").desc("show this help").build();
	private final List<Opt> options = new ArrayList<>();

	private Opt iothread;
	private Opt select;
	private Opt listener;
	private Opt shutdown;

	public NIOServerBuilder() {
		iothread = withOpt("iothread").withCli(Option.builder().longOpt("iothread").argName("num").hasArg().type(Integer.class).desc("number of io thread to use").build())
				.withValue(Integer.toString(Runtime.getRuntime().availableProcessors()));
		select = withOpt("select").withCli(Option.builder().longOpt("select").argName("num").hasArg().type(Integer.class).desc("timeout on Selector.select").build())
				.withValue("200");
		listener = withOpt("listener").withCli(Option.builder().longOpt("listener").argName("NOP|LOG").hasArg().type(String.class).desc("set the listener").build())
				.withValue("NOP");
		shutdown = withOpt("shutdown").withCli(
				Option.builder().longOpt("shutdown").argName("port|addr:port").hasArg().type(Integer.class).desc("addr:port to gracefuly shutdown the server").build());
	}

	public Opt withOpt(String name) {
		Opt o = new Opt(name);
		options.add(o);
		return o;
	}

	protected void beforeParse() {
		// for override
	}

	public static int parseInt(CommandLine cli, Opt o, int min) {
		try {
			int i = Integer.parseInt(o.value(cli));
			if (i < min)
				throw new IllegalArgumentException(o.name() + " should be >= " + min);
			return i;
		} catch (@SuppressWarnings("unused") NumberFormatException e) {
			throw new IllegalArgumentException(o.name() + " invalid value " + o.value(cli));
		}
	}

	public static InetSocketAddress parseAddr(CommandLine cli, Opt o, String defaultHost) {
		String host = defaultHost;
		String a = o.value(cli);
		if (a == null)
			return null;
		int i = a.indexOf(':');
		if (i == 0)
			a = a.substring(1);
		else if (i > 0) {
			host = a.substring(0, i);
			a = a.substring(i + 1);
		}
		try {
			int port = Integer.parseInt(a);
			if (host == null || host.isEmpty())
				return new InetSocketAddress(port);
			return new InetSocketAddress(host, port);
		} catch (@SuppressWarnings("unused") NumberFormatException e) {
			throw new IllegalArgumentException(o.name() + " invalid value " + o.value(cli));
		}
	}

	@SuppressWarnings("unused")
	protected void process(NIOServer server, CommandLine cli) throws Exception {
		// for override
	}

	public final NIOServer build(String... arg) throws Exception {
		beforeParse();
		readProperties();
		Options opts = new Options();
		opts.addOption(help);
		for (Opt o : options) {
			if (o.cli != null)
				opts.addOption(o.cli);
		}

		CommandLine cli = new DefaultParser().parse(opts, arg);
		if (cli.hasOption(help)) {
			HelpFormatter helpFormatter = new HelpFormatter();
			helpFormatter.setWidth(100);
			helpFormatter.printHelp("nioserver", opts);
			System.exit(2);
		}

		NIOServerListener l = getListener(listener.value(cli));

		NIOWorkers workers = createWorkers(parseInt(cli, iothread, 0), parseInt(cli, select, 0), l);

		NIOServer server = new NIOServer(workers, l);
		process(server, cli);
		InetSocketAddress addr = parseAddr(cli, shutdown, "127.0.0.1");
		if (addr != null)
			server.bind(addr, () -> new ShutdownConnection(server));
		return server;
	}

	private NIOWorkers createWorkers(int i, int selectTime, NIOServerListener l) throws IOException {
		if (i == 1)
			return new NIOWorker(0, l, selectTime);
		NIOWorker[] w = new NIOWorker[i];
		while (i > 0)
			w[--i] = new NIOWorker(i, l, selectTime);
		return new RoundRobin(w);
	}

	private NIOServerListener getListener(String l) {
		if ("NOP".equals(l))
			return NIOServerListener.NOP;
		if ("LOG".equals(l))
			return NIOServerListener.LOG;
		throw new IllegalArgumentException("listener should be one of NOP, LOG");
	}

	private void readProperties() {
		try (InputStream is = this.getClass().getResourceAsStream("/META-INF/nioserver.properties")) {
			if (is == null)
				return;
			Properties prop = new Properties();
			try (Reader r = new InputStreamReader(is, StandardCharsets.UTF_8)) {
				prop.load(r);
			}
			for (Opt o : options) {
				String v = (String) prop.get(o.name);
				if (v != null)
					o.value = v;
			}
		} catch (IOException e) {
			logger.warn("Failed to read nioserver.properties", e);
		}
	}

	public static class Opt {
		private final String name;
		private Option cli;
		private String value;

		public Opt(String name) {
			this.name = name;
		}

		public Opt withValue(String value) {
			if (cli != null)
				cli.setDescription(cli.getDescription() + ", default to: " + value);
			this.value = value;
			return this;
		}

		public Opt withCli(Option cli) {
			this.cli = cli;
			return this;
		}

		public String name() {
			return name;
		}

		public String value(CommandLine c) {
			return cli != null ? c.getOptionValue(cli, value) : value;
		}
	}
}
