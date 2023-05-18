/**
 * 
 */
package unknow.server.maven.model;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.stream.Stream;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;

import unknow.server.maven.Test.G;

/**
 * @author unknow
 */
public class ModelLoaderTest {

	public static final Stream<Arguments> input() {
		return Stream.of(
				Arguments.of("String", Arrays.asList("String")),
				Arguments.of("Collection<String>", Arrays.asList("Collection", "String")),
				Arguments.of("Map<String,String>", Arrays.asList("Map", "String", "String")),
				Arguments.of("Collection<List<String>>", Arrays.asList("Collection", "List<String>")),
				Arguments.of("Map<List<String>,String>", Arrays.asList("Map", "List<String>", "String")),
				Arguments.of("Map<String,List<String>>", Arrays.asList("Map", "String", "List<String>")),
				Arguments.of("A<B,C,D<E>>", Arrays.asList("A", "B", "C", "D<E>")),
				Arguments.of("L<>", Arrays.asList("L", "")));
	}

	@ParameterizedTest(name = "{0}")
	@MethodSource("input")
	public void testParse(String clazz, List<String> result) {
		assertEquals(result, ModelLoader.parse(clazz));
	}

	@Test
	public void testCollection() {
		ModelLoader loader = new ModelLoader(Collections.emptyMap());

		TypeModel col = loader.get(G.class.getName());
		ClassModel slist = loader.get(StringList.class.getName()).asClass();
		assertTrue(col.isAssignableFrom(slist));
		assertEquals("java.lang.String", slist.superType().parameter(0).type().name());
		assertEquals("java.lang.String", slist.superType().field("a").type().name());

		ClassModel ilist = loader.get(IntList.class.getName()).asClass();
		assertTrue(col.isAssignableFrom(ilist));
		assertEquals("java.lang.Integer", ilist.superType().parameter(0).type().name());
		MethodModel m = ilist.superType().methods().iterator().next();
		assertEquals("java.lang.Integer", m.type().name());
		assertEquals("java.lang.Integer", m.parameter(0).type().name());
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
