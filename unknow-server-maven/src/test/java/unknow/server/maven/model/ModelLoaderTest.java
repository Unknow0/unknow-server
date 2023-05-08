/**
 * 
 */
package unknow.server.maven.model;

import static org.junit.jupiter.api.Assertions.assertEquals;

import java.util.Arrays;
import java.util.List;
import java.util.stream.Stream;

import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;

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
}
