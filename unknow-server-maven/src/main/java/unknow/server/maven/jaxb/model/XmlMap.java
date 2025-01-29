package unknow.server.maven.jaxb.model;

import java.util.Set;

import javax.xml.namespace.QName;

import unknow.server.maven.jaxb.model.XmlTypeComplex.Factory;
import unknow.server.maven.model.ClassModel;
import unknow.server.maven.model.TypeModel;

public class XmlMap implements XmlType {
	private final QName qname;
	private final ClassModel c;
	private final Factory f;

	public XmlMap(QName qname, ClassModel c, Factory f) {
		this.qname = qname;
		this.c = c;
		this.f = f;
	}

	@Override
	public QName name() {
		return qname;
	}

	@Override
	public TypeModel type() {
		return c;
	}

	@Override
	public void toString(StringBuilder sb, Set<XmlType> saw) {
		sb.append(c.name());
		if (!saw.add(this))
			return;
	}
}
