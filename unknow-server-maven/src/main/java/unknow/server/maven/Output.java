/**
 * 
 */
package unknow.server.maven;

import java.io.BufferedWriter;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;

import com.github.javaparser.ast.CompilationUnit;
import com.github.javaparser.ast.body.ClassOrInterfaceDeclaration;
import com.github.javaparser.printer.configuration.DefaultConfigurationOption;
import com.github.javaparser.printer.configuration.DefaultPrinterConfiguration;
import com.github.javaparser.printer.configuration.DefaultPrinterConfiguration.ConfigOption;
import com.github.javaparser.printer.configuration.Indentation;
import com.github.javaparser.printer.configuration.Indentation.IndentType;
import com.github.javaparser.printer.configuration.PrinterConfiguration;

/**
 * @author unknow
 */
public class Output {
	private final Path out;
	private final PrinterConfiguration pp;

	public Output(String output, String packageName) throws IOException {
		pp = new DefaultPrinterConfiguration()
				.addOption(new DefaultConfigurationOption(ConfigOption.INDENTATION, new Indentation(IndentType.TABS, 1)))
				.addOption(new DefaultConfigurationOption(ConfigOption.ORDER_IMPORTS, true))
				.addOption(new DefaultConfigurationOption(ConfigOption.SPACE_AROUND_OPERATORS, true));

		Path file = Paths.get(output);
		if (packageName != null)
			file = file.resolve(packageName.replace('.', '/'));
		out = file;
		Files.createDirectories(out);
	}

	public void save(CompilationUnit cu) throws IOException {
		ClassOrInterfaceDeclaration cl = cu.findFirst(ClassOrInterfaceDeclaration.class, c -> c.isPublic()).orElse(cu.findFirst(ClassOrInterfaceDeclaration.class).orElse(null));
		try (BufferedWriter w = Files.newBufferedWriter(out.resolve(cl.getNameAsString() + ".java"), StandardCharsets.UTF_8)) {
			w.write(cu.toString(pp));
		}
	}
}
