
/**
 * 
 */
package unknow.server.jaxb;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

import java.io.StringReader;

import javax.xml.stream.XMLInputFactory;
import javax.xml.stream.XMLStreamException;
import javax.xml.stream.XMLStreamReader;

import org.junit.jupiter.api.Test;

import jakarta.xml.bind.JAXBContext;
import jakarta.xml.bind.JAXBException;

/**
 * @author unknow
 */
public class ContextFactoryTest {
	private static final XMLInputFactory f = XMLInputFactory.newInstance();

	@Test
	void test() throws JAXBException {
		JAXBContext.newInstance(O.class);
		assertThrows(JAXBException.class, () -> JAXBContext.newInstance(ContextFactoryTest.class));
	}

	@Test
	void h() throws XMLStreamException {
		XMLStreamReader r = f.createXMLStreamReader(new StringReader("<o a='4'>test</o>"));
		r.nextTag();
		O o = new OHandler().read(r, null, null);
		assertEquals(4, o.a);
		assertEquals("test", o.v);
	}
}
