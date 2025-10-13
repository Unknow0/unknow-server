package unknow.server.maven.jaxb.model;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertInstanceOf;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.lang.reflect.Modifier;
import java.util.Iterator;

import javax.xml.namespace.QName;

import org.junit.jupiter.api.Test;

import unknow.model.api.PrimitiveModel;
import unknow.model.jvm.JvmModelLoader;
import unknow.model.simple.SimpleAnnotationArray;
import unknow.model.simple.SimpleClass;
import unknow.server.maven.jaxb.model.XmlElements.XmlGroup;

public class XmlLoaderTest {

	@Test
	public void testXmlElem() {
		SimpleClass clazz = new SimpleClass("Test", Modifier.PUBLIC);

		clazz.withMethod("getA", Modifier.PUBLIC, PrimitiveModel.INT).withAnnotation(jakarta.xml.bind.annotation.XmlElement.class);
		clazz.withMethod("setA", Modifier.PUBLIC, PrimitiveModel.VOID).withParam("a", PrimitiveModel.INT);

		XmlLoader xmlLoader = new XmlLoader();
		XmlType t = xmlLoader.add(clazz);

		assertEquals(XmlTypeComplex.class, t.getClass());
		XmlTypeComplex c = (XmlTypeComplex) t;
		assertNull(c.getValue());
		assertTrue(c.getAttributes().isEmpty());

		XmlElements elements = c.getElements();
		assertNotNull(elements);
		assertEquals(XmlGroup.ALL, elements.group());
		Iterator<XmlElement> it = elements.iterator();
		assertTrue(it.hasNext());
		assertEquals(new QName("", "a"), it.next().qname());
	}

	@Test
	public void testXmlChoice() {
		SimpleClass clazz = new SimpleClass("Test", Modifier.PUBLIC);

		SimpleAnnotationArray a = clazz.withMethod("getA", Modifier.PUBLIC, PrimitiveModel.INT).withAnnotation(jakarta.xml.bind.annotation.XmlElements.class)
				.withArray("value");
		a.withAnnotation(jakarta.xml.bind.annotation.XmlElement.class).withClass("type", PrimitiveModel.INT,
				JvmModelLoader.GLOBAL.get(jakarta.xml.bind.annotation.XmlElement.DEFAULT.class.getName()));
		a.withAnnotation(jakarta.xml.bind.annotation.XmlElement.class).withClass("type", PrimitiveModel.DOUBLE,
				JvmModelLoader.GLOBAL.get(jakarta.xml.bind.annotation.XmlElement.DEFAULT.class.getName()));
		clazz.withMethod("setA", Modifier.PUBLIC, PrimitiveModel.VOID).withParam("a", PrimitiveModel.INT);

		XmlLoader xmlLoader = new XmlLoader();
		XmlType t = xmlLoader.add(clazz);

		assertEquals(XmlTypeComplex.class, t.getClass());
		XmlTypeComplex c = (XmlTypeComplex) t;
		assertNull(c.getValue());
		assertTrue(c.getAttributes().isEmpty());

		XmlElements elements = c.getElements();
		assertNotNull(elements);
		assertEquals(XmlGroup.ALL, elements.group());
		Iterator<XmlElement> it = elements.iterator();
		assertTrue(it.hasNext());
		XmlElement next = it.next();
		assertInstanceOf(XmlChoice.class, next.xmlType());
		assertEquals(new QName("", "a"), ((XmlChoice) next.xmlType()).choice().iterator().next().qname());
	}
}
