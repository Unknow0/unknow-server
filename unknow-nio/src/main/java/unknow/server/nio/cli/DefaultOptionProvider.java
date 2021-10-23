/**
 * 
 */
package unknow.server.nio.cli;

import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.Reader;
import java.nio.charset.StandardCharsets;
import java.util.Properties;

import picocli.CommandLine.IDefaultValueProvider;
import picocli.CommandLine.Model.ArgSpec;

/**
 * @author unknow
 */
public class DefaultOptionProvider implements IDefaultValueProvider {
	private final Properties prop;

	/**
	 * create new DefaultOptionProvider
	 * 
	 * @throws IOException in case of error
	 */
	public DefaultOptionProvider() throws IOException {
		prop = new Properties();
		try (InputStream is = this.getClass().getResourceAsStream("/nioserver.properties")) {
			if (is == null)
				return;
			try (Reader r = new InputStreamReader(is, StandardCharsets.UTF_8)) {
				prop.load(r);
			}
		}
	}

	@Override
	public String defaultValue(ArgSpec arg) throws Exception {
		return prop.getProperty(arg.command().qualifiedName(".") + "." + arg.descriptionKey());
	}
}
