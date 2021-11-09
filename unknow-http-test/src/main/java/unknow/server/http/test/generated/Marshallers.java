package unknow.server.http.test.generated;

import java.io.IOException;
import java.io.Writer;
import unknow.server.http.test.xml.Mixed;
import unknow.server.http.test.xml.Root;
import unknow.server.jaxws.Envelope;
import unknow.server.jaxws.Marshaller;
import unknow.server.jaxws.MarshallerRegistry;
import unknow.server.jaxws.XMLNsCollector;
import unknow.server.jaxws.XMLOutput;
import unknow.server.jaxws.XMLWriter;

public final class Marshallers {

	private static final MarshallerRegistry R = new MarshallerRegistry();

	static {
		R.register(Mixed.class, (m, t, w) -> {
			w.startElement("elem", "");
			w.text(t.getElem());
			w.endElement("elem", "");
			w.text(Integer.toString(t.getValue()));
		});
		R.register(Root.class, (m, t, w) -> {
			w.attribute("value", "", Integer.toString(t.getValue()));
			w.startElement("elem", "webservice.unknow");
			w.text(t.getElem());
			w.endElement("elem", "webservice.unknow");
		});
	}

	public static final void marshall(Envelope e, Writer w) throws IOException {
		XMLNsCollector c = new XMLNsCollector();
		Marshaller.ENVELOPE.marshall(R, e, c);
		try (XMLWriter out = new XMLOutput(w, c.buildNsMapping())) {
			Marshaller.ENVELOPE.marshall(R, e, out);
		}
	}

	@SuppressWarnings("unchecked")
	public static final void marshall(Object e, Writer w) throws IOException {
		Marshaller<Object> m = R.get((Class<Object>) e.getClass());
		XMLNsCollector c = new XMLNsCollector();
		m.marshall(R, e, c);
		try (XMLWriter out = new XMLOutput(w, c.buildNsMapping())) {
			m.marshall(R, e, out);
		}
	}
}
