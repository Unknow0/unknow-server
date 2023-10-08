/**
 * 
 */
package unknow.server.maven.model;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.Arrays;
import java.util.List;
import java.util.stream.Stream;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;

import com.github.javaparser.JavaParser;
import com.github.javaparser.ParserConfiguration;
import com.github.javaparser.ast.CompilationUnit;
import com.github.javaparser.ast.body.ClassOrInterfaceDeclaration;
import com.github.javaparser.resolution.TypeSolver;
import com.github.javaparser.symbolsolver.JavaSymbolSolver;
import com.github.javaparser.symbolsolver.resolution.typesolvers.ReflectionTypeSolver;

import unknow.server.maven.model.ast.AstClass;
import unknow.server.maven.model.jvm.JvmClass;
import unknow.server.maven.model.jvm.JvmModelLoader;

/**
 * @author unknow
 */
public class ModelLoaderTest {

	public static final Stream<Arguments> input() {
		return Stream.of(Arguments.of("String", Arrays.asList("String")), Arguments.of("Collection<String>", Arrays.asList("Collection", "String")),
				Arguments.of("Map<String,String>", Arrays.asList("Map", "String", "String")),
				Arguments.of("Collection<List<String>>", Arrays.asList("Collection", "List<String>")),
				Arguments.of("Map<List<String>,String>", Arrays.asList("Map", "List<String>", "String")),
				Arguments.of("Map<String,List<String>>", Arrays.asList("Map", "String", "List<String>")), Arguments.of("A<B,C,D<E>>", Arrays.asList("A", "B", "C", "D<E>")),
				Arguments.of("L<>", Arrays.asList("L", "")));
	}

	@ParameterizedTest(name = "{0}")
	@MethodSource("input")
	public void testParse(String clazz, List<String> result) {
		assertEquals(result, ModelLoader.parse(clazz));
	}

	@Test
	public void testCollection() {
		ModelLoader loader = JvmModelLoader.GLOBAL;

		TypeModel col = loader.get(G.class.getName());
		ClassModel slist = loader.get(StringList.class.getName()).asClass();
		assertTrue(col.isAssignableFrom(slist));
		assertEquals("java.lang.String", slist.superType().parameter(0).type().name());
		assertEquals("java.lang.String", slist.superType().field("a").type().name());

		ClassModel ilist = loader.get(IntList.class.getName()).asClass();
		assertTrue(col.isAssignableFrom(ilist));
		assertEquals("java.lang.Integer", ilist.superType().parameter(0).type().name());
		MethodModel m = ilist.superType().method("m", loader.get("java.lang.Integer")).orElse(null);
		assertEquals("java.lang.Integer", m.type().name());
		assertEquals("java.lang.Integer", m.parameter(0).type().name());
	}

	public static final Stream<Arguments> testClassName() {
		Class<?> cl = ModelLoaderTest.class;

		TypeSolver resolver = new ReflectionTypeSolver(false);
		JavaSymbolSolver javaSymbolSolver = new JavaSymbolSolver(resolver);
		JavaParser parser = new JavaParser(new ParserConfiguration().setStoreTokens(true).setSymbolResolver(javaSymbolSolver));
		CompilationUnit cu = parser.parse("package " + cl.getPackageName() + ";"
				+ " public class " + cl.getSimpleName() + "{"
				+ " public static class G<A> {}"
				+ " public static class StringList extends G<String> {}"
				+ "}").getResult().orElse(null);

		ClassOrInterfaceDeclaration ast = cu.findFirst(ClassOrInterfaceDeclaration.class, c -> "StringList".equals(c.getNameAsString())).orElse(null);
		AstClass astList = new AstClass(JvmModelLoader.GLOBAL, null, ast, new TypeModel[0]);

		JvmClass jvmList = new JvmClass(JvmModelLoader.GLOBAL, StringList.class, new TypeModel[0]);
		return Stream.of(Arguments.of(jvmList), Arguments.of(astList));

	}

	@ParameterizedTest()
	@MethodSource()
	public void testClassName(ClassModel list) {

		assertEquals(this.getClass().getName() + "$StringList", list.name());
		assertEquals(this.getClass().getName() + "$G", list.superType().name());
		assertEquals(this.getClass().getName() + "$G<java.lang.String>", list.superType().genericName());

	}

	public static class G<A> {
		A a;

		A m(A a) {
			return a;
		}
	}

	public static class StringList extends G<String> { // ok
	}

	public static class IntList extends G<Integer> { // ok
	}
}
