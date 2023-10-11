package unknow.server.bench;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.io.Reader;
import java.io.StringReader;

import org.openjdk.jmh.annotations.Benchmark;

import jakarta.xml.bind.JAXBContext;
import jakarta.xml.bind.JAXBException;
import unknow.server.http.test.xml.Complex;

public class XmlBench {
	private static final ClassLoader cl = XmlBench.class.getClassLoader();
	private static final Class<?>[] CLASS = { Complex.class };
	static final JAXBContext UNKNOW;
	static final JAXBContext JAXB;
	static final JAXBContext MOXY;
	static final String XML;
	static {
		try {
			UNKNOW = new unknow.server.http.test.generated.JaxbContextFactory().createContext(CLASS, null);
			JAXB = new org.glassfish.jaxb.runtime.v2.JAXBContextFactory().createContext(CLASS, null);
			MOXY = new org.eclipse.persistence.jaxb.XMLBindingContextFactory().createContext(CLASS, null);
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

	private static void bench(JAXBContext c) throws JAXBException, IOException {
		Object o;
		try (Reader r = new StringReader(XML)) {
			o = c.createUnmarshaller().unmarshal(r);
		}
		if (!(o instanceof Complex))
			throw new RuntimeException(o.getClass().getName());
		c.createMarshaller().marshal(o, DUMP);
	}

	@Benchmark
	public void unknow() throws JAXBException, IOException {
		bench(UNKNOW);
	}

	@Benchmark
	public void reference() throws JAXBException, IOException {
		bench(JAXB);
	}

	@Benchmark
	public void moxy() throws JAXBException, IOException {
		bench(MOXY);
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
