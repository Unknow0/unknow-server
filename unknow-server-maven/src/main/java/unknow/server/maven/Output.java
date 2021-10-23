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
import com.github.javaparser.printer.PrettyPrinterConfiguration;
import com.github.javaparser.printer.PrettyPrinterConfiguration.IndentType;

/**
 * @author unknow
 */
public class Output {
	private final Path out;
	private final PrettyPrinterConfiguration pp;

	public Output(String output, String packageName) throws IOException {
		pp = new PrettyPrinterConfiguration();
		pp.setIndentType(IndentType.TABS).setIndentSize(1);
		pp.setOrderImports(true).setSpaceAroundOperators(true);

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
