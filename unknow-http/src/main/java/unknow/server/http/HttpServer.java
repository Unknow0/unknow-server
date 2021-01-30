/**
 * 
 */
package unknow.server.http;

import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

import picocli.CommandLine;
import picocli.CommandLine.Command;
import picocli.CommandLine.Option;
import unknow.server.nio.Handler;
import unknow.server.nio.HandlerFactory;
import unknow.server.nio.cli.NIOServerCli;

/**
 * @author unknow
 */
@Command(name = "httpd")
public class HttpServer extends NIOServerCli {
	@Option(names = "--path", description = "base path for this server")
	public Path path = Paths.get(".");

	public ExecutorService executor = Executors.newCachedThreadPool();

	public static void main(String[] arg) {
		HttpServer c = new HttpServer();
		c.handler = new HandlerFactory() {
			@Override
			protected Handler create() {
				return new HttpHandler(c.executor, new DefaultHttpProcessor(c.path));
			}
		};
		System.exit(new CommandLine(c).execute(arg));
	}
}

/*
 * Bootstrapring
 * 
 * generate ServletContext for each servlet * generate ServletConfig * create servlet & init
 * 
 * load mapping & filter
 * 
 * process
 */