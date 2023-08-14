///**
// * 
// */
//package unknow.server.http.jaxrs;
//
//import static org.junit.jupiter.api.Assertions.assertEquals;
//
//import java.util.Arrays;
//import java.util.List;
//import java.util.stream.Stream;
//
//import org.junit.jupiter.params.ParameterizedTest;
//import org.junit.jupiter.params.provider.Arguments;
//import org.junit.jupiter.params.provider.MethodSource;
//
///**
// * @author unknow
// */
//public class JaxRsPatternServletTest {
//
//	public static final Stream<Arguments> test() {
//		return Stream.of(
//				Arguments.of(Arrays.asList("path"), "/path"),
//				Arguments.of(Arrays.asList("path", "toto"), "/path/toto"));
//	}
//
//	@ParameterizedTest
//	@MethodSource
//	public void test(List<String> expected, String path) {
//		assertEquals(expected, JaxRsPatternServlet.splitPath(path));
//	}
//}
