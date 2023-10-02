package unknow.server.bench;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.io.Reader;
import java.io.StringReader;

import org.apache.xmlbeans.XmlException;
import org.apache.xmlbeans.XmlObject;
import org.openjdk.jmh.annotations.Benchmark;

import jakarta.xml.bind.JAXBContext;
import jakarta.xml.bind.JAXBException;
import unknow.server.http.test.xml.Complex;

public class XmlBench {
	private static final ClassLoader cl = XmlBench.class.getClassLoader();
	private static final Class<?>[] CLASS = { Complex.class };
	static final JAXBContext JAXB;
	static final JAXBContext UNKNOW;
	static final String XML;
	static {
		try {
			JAXB = new org.glassfish.jaxb.runtime.v2.JAXBContextFactory().createContext(CLASS, null);
			UNKNOW = new unknow.server.http.test.generated.JaxbContextFactory().createContext(CLASS, null);
		} catch (JAXBException e) {
			throw new RuntimeException(e);
		}

		try (ByteArrayOutputStream out = new ByteArrayOutputStream(); InputStream is = cl.getResourceAsStream("complex.xml")) {
			byte[] b = new byte[4096];
			int l = 0;
			while ((l = is.read(b)) != -1)
				out.write(b, 0, l);
			XML = out.toString("utf8");
		} catch (IOException e) {
			throw new RuntimeException(e);
		}
	}

	@Benchmark
	public void jaxb() throws JAXBException, IOException {
		Object o;
		try (Reader r = new StringReader(XML)) {
			o = JAXB.createUnmarshaller().unmarshal(r);
		}
		if (!(o instanceof Complex))
			throw new RuntimeException(o.getClass().getName());
		JAXB.createMarshaller().marshal(o, DUMP);
	}

	@Benchmark
	public void unknow() throws JAXBException, IOException {
		Object o;
		try (Reader r = new StringReader(XML)) {
			o = UNKNOW.createUnmarshaller().unmarshal(r);
		}
		if (!(o instanceof Complex))
			throw new RuntimeException(o.getClass().getName());
		UNKNOW.createMarshaller().marshal(o, DUMP);
	}

	@Benchmark
	public void xmlbeans() throws IOException, XmlException {
		XmlObject o;
		try (Reader r = new StringReader(XML)) {
			o = XmlObject.Factory.parse(r);
		}
		o.save(DUMP);
	}

	private static final OutputStream DUMP = new OutputStream() {
		@Override
		public void write(int b) throws IOException { // OK
		}

		@Override
		public void write(byte[] b) throws IOException { // OK
		}

		@Override
		public void write(byte[] b, int off, int len) throws IOException { // OK
		}
	};
}
