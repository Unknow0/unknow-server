package unknow.server.maven.jaxb.model;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.lang.reflect.Modifier;
import java.util.Iterator;

import javax.xml.namespace.QName;

import org.junit.jupiter.api.Test;

import jakarta.xml.bind.annotation.XmlElement;
import unknow.server.maven.jaxb.model.XmlElements.XmlGroup;
import unknow.server.maven.model.PrimitiveModel;
import unknow.server.maven.model.jvm.JvmModelLoader;
import unknow.server.maven.model.simple.SimpleAnnotationArray;
import unknow.server.maven.model.simple.SimpleClass;

public class XmlLoaderTest {

	@Test
	public void testXmlElem() {
		SimpleClass clazz = new SimpleClass("Test", Modifier.PUBLIC);

		clazz.withMethod("getA", Modifier.PUBLIC, PrimitiveModel.INT).withAnnotation(XmlElement.class);
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
		Iterator<XmlElements> it = elements.iterator();
		assertTrue(it.hasNext());
		XmlElements next = it.next();
		assertEquals(XmlGroup.SIMPLE, next.group());
		assertEquals(new QName("", "a"), next.childs().iterator().next().qname());
	}

	@Test
	public void testXmlChoice() {
		SimpleClass clazz = new SimpleClass("Test", Modifier.PUBLIC);

		SimpleAnnotationArray a = clazz.withMethod("getA", Modifier.PUBLIC, PrimitiveModel.INT).withAnnotation(jakarta.xml.bind.annotation.XmlElements.class)
				.withArray("value");
		a.withAnnotation(XmlElement.class).withClass("type", PrimitiveModel.INT, JvmModelLoader.GLOBAL.get(XmlElement.DEFAULT.class.getName()));
		a.withAnnotation(XmlElement.class).withClass("type", PrimitiveModel.DOUBLE, JvmModelLoader.GLOBAL.get(XmlElement.DEFAULT.class.getName()));
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
		Iterator<XmlElements> it = elements.iterator();
		assertTrue(it.hasNext());
		XmlElements next = it.next();
		assertEquals(XmlGroup.CHOICE, next.group());
		assertEquals(new QName("", "a"), next.childs().iterator().next().qname());
	}
}
