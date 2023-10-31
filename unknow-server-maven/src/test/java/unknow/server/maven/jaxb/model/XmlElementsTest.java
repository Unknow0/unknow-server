package unknow.server.maven.jaxb.model;

import static org.junit.jupiter.api.Assertions.assertEquals;

import java.util.Arrays;
import java.util.HashSet;
import java.util.Set;
import java.util.stream.Stream;

import javax.xml.namespace.QName;

import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;

import unknow.server.maven.jaxb.model.XmlElements.XmlGroup;
import unknow.server.maven.jaxb.model.XmlElements.XmlGroupElements;
import unknow.server.maven.jaxb.model.XmlElements.XmlSimpleElements;

public class XmlElementsTest {
	public static final Stream<Arguments> testChild() {
		QName n1 = new QName("", "e1");
		QName n2 = new QName("", "e2");
		QName n3 = new QName("", "e3");
		QName n4 = new QName("", "e4");

		XmlSimpleElements e1 = new XmlSimpleElements(new XmlElement(null, n1, null, null));
		XmlSimpleElements e2 = new XmlSimpleElements(new XmlElement(null, n2, null, null));
		XmlSimpleElements e3 = new XmlSimpleElements(new XmlElement(null, n3, null, null));
		XmlSimpleElements e4 = new XmlSimpleElements(new XmlElement(null, n4, null, null));

		XmlGroupElements g1 = new XmlGroupElements(XmlGroup.ALL, Arrays.asList(e1, e2));
		XmlGroupElements g2 = new XmlGroupElements(XmlGroup.ALL, Arrays.asList(e3, e4));

		return Stream.of(Arguments.of(new HashSet<>(Arrays.asList(n1)), e1), Arguments.of(new HashSet<>(Arrays.asList(n1, n2)), g1),
				Arguments.of(new HashSet<>(Arrays.asList(n1, n2)), new XmlGroupElements(XmlGroup.ALL, Arrays.asList(g1))),
				Arguments.of(new HashSet<>(Arrays.asList(n1, n2, n3)), new XmlGroupElements(XmlGroup.ALL, Arrays.asList(g1, e3))),
				Arguments.of(new HashSet<>(Arrays.asList(n1, n2, n3, n4)), new XmlGroupElements(XmlGroup.ALL, Arrays.asList(g1, g2))));
	}

	@ParameterizedTest
	@MethodSource
	public void testChild(Set<QName> expected, XmlElements elements) {
		Set<QName> set = new HashSet<>();
		for (XmlElement e : elements.childs())
			set.add(e.qname());
		assertEquals(expected, set);
	}
}
