package unknow.server.maven.jaxb.model;

import java.util.Collection;
import java.util.HashSet;
import java.util.Iterator;
import java.util.Set;

import javax.xml.namespace.QName;

import unknow.server.maven.model.TypeModel;

public class XmlChoice implements XmlType {
	private final Collection<XmlElement> choice;

	public XmlChoice(Collection<XmlElement> choice) {
		this.choice = choice;
	}

	public Collection<XmlElement> choice() {
		return choice;
	}

	@Override
	public QName name() {
		return null;
	}

	@Override
	public TypeModel type() {
		return null;
	}

	@Override
	public String toString() {
		StringBuilder sb = new StringBuilder();
		toString(sb, new HashSet<>());
		return sb.toString();
	}

	@Override
	public void toString(StringBuilder sb, Set<XmlType> saw) {
		sb.append("Choice[");
		Iterator<XmlElement> it = choice.iterator();
		while (it.hasNext()) {
			it.next().toString(sb, saw);
			if (it.hasNext())
				sb.append(", ");
		}
		sb.append(']');
	}
}
