import java.io.IOException;
import java.io.InputStream;
import java.io.StringWriter;

import javax.xml.parsers.ParserConfigurationException;

import org.apache.xmlbeans.XmlException;
import org.apache.xmlbeans.XmlObject;
import org.xml.sax.InputSource;
import org.xml.sax.SAXException;

import unknow.sax.SaxParser;
import unknow.server.http.test.generated.Marshallers;
import unknow.server.http.test.generated.WsServlet;
import unknow.server.jaxws.Envelope;

/**
 * 
 */

/**
 * @author unknow
 */
public class XmlParseTest {
	private static final WsServlet WS = new WsServlet();

	public static XmlObject parseXmlBeans(InputStream is) throws XmlException, IOException {
		return XmlObject.Factory.parse(is);
	}

	public static Envelope parseUnknow(InputStream is) throws ParserConfigurationException, SAXException, IOException {
		return SaxParser.parse(WS, new InputSource(is));

	}

	private static final int l = 10000;
	public static Envelope e;
	public static XmlObject o;

	public static void main(String[] arg) throws Exception {
		long start = System.currentTimeMillis();
		for (int i = 0; i < l; i++) {
			try (InputStream is = XmlParseTest.class.getResourceAsStream("/test.xml")) {
				e = parseUnknow(is);
			}
		}
		System.out.println("unknow:   " + (System.currentTimeMillis() - start));
		start = System.currentTimeMillis();
		for (int i = 0; i < l; i++)
			Marshallers.marshall(e, new StringWriter());
		System.out.println("unknow:   " + (System.currentTimeMillis() - start));

		start = System.currentTimeMillis();
		for (int i = 0; i < l; i++) {
			try (InputStream is = XmlParseTest.class.getResourceAsStream("/test.xml")) {
				o = parseXmlBeans(is);
			}
		}
		System.out.println("xmlBeans: " + (System.currentTimeMillis() - start));
		start = System.currentTimeMillis();
		for (int i = 0; i < l; i++)
			o.save(new StringWriter());
		System.out.println("xmlBeans: " + (System.currentTimeMillis() - start));
	}

}
