/**
 * 
 */
package unknow.server.maven;

import java.util.Collection;

import com.github.javaparser.JavaParser;
import com.github.javaparser.ParseResult;
import com.github.javaparser.ParserConfiguration;
import com.github.javaparser.ast.CompilationUnit;
import com.github.javaparser.symbolsolver.JavaSymbolSolver;
import com.github.javaparser.symbolsolver.resolution.typesolvers.CombinedTypeSolver;
import com.github.javaparser.symbolsolver.resolution.typesolvers.JavaParserTypeSolver;
import com.github.javaparser.symbolsolver.resolution.typesolvers.ReflectionTypeSolver;

import unknow.server.maven.model.ClassModel;
import unknow.server.maven.model.MethodModel;
import unknow.server.maven.model.ModelLoader;
import unknow.server.maven.model.TypeModel;
import unknow.server.maven.model.jvm.JvmModelLoader;

/**
 * @author unknow
 */
public class Main {
	public static void main(String[] arg) {
		ModelLoader loader = JvmModelLoader.GLOBAL;

		CombinedTypeSolver resolver = new CombinedTypeSolver(new ReflectionTypeSolver(false), new JavaParserTypeSolver("src/test/java"));
		JavaSymbolSolver javaSymbolSolver = new JavaSymbolSolver(resolver);
		JavaParser parser = new JavaParser(new ParserConfiguration().setStoreTokens(true).setSymbolResolver(javaSymbolSolver));
		ParseResult<CompilationUnit> parse = parser.parse("public class T<A> extends java.util.ArrayList<String> {public A a;}");

		CompilationUnit cu = parse.getResult().get();
		System.out.println(cu.getTypes().get(0).asClassOrInterfaceDeclaration().getExtendedTypes().get(0).resolve().describe());

		TypeModel col = loader.get(Collection.class.getName());
		TypeModel t = loader.get(StringList.class.getName());
//		assertTrue(col.isAssignableFrom(t));
//		System.out.println(T.class.getSuperclass());
//		System.out.println(T.class.getGenericSuperclass().getTypeName());

		ClassModel superType = t.asClass().superType();
		System.out.println(superType.field("a").type());
		System.out.println(superType.superType());
		MethodModel m = superType.methods().iterator().next();
		System.out.println(m.type().name());
		System.out.println(m.parameter(0).type().name());
	}

	public static class G<A> {
		A a;

		A m(A a) {
			return a;
		}
	}

	public static class StringList extends G<String> {
	}

	public static class IntList extends G<Integer> {
	}
}
