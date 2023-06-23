package unknow.server.maven;

import org.junit.jupiter.api.Test;

import com.github.javaparser.JavaParser;
import com.github.javaparser.ParserConfiguration;
import com.github.javaparser.ast.CompilationUnit;
import com.github.javaparser.ast.body.TypeDeclaration;
import com.github.javaparser.resolution.TypeSolver;
import com.github.javaparser.symbolsolver.JavaSymbolSolver;
import com.github.javaparser.symbolsolver.resolution.typesolvers.ReflectionTypeSolver;

public class ParserTest {
	@Test
	public void test() {
		TypeSolver resolver = new ReflectionTypeSolver(false);
		JavaSymbolSolver javaSymbolSolver = new JavaSymbolSolver(resolver);
		JavaParser parser = new JavaParser(new ParserConfiguration().setStoreTokens(true).setSymbolResolver(javaSymbolSolver));

		CompilationUnit cu = parser.parse("package test; import org.junit.jupiter.api.Tag; @Tag(\"bla\") public class T{}").getResult().orElse(null);

		TypeDeclaration<?> t = cu.getType(0);
		System.out.println(t.getAnnotation(0).resolve());
	}
}
