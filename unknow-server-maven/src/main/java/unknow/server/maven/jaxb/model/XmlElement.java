/**
 * 
 */
package unknow.server.maven.jaxb.model;

import java.util.HashSet;
import java.util.Set;
import java.util.function.Supplier;

import javax.xml.namespace.QName;

import unknow.server.maven.model.BeanProperty;
import unknow.server.maven.model.TypeModel;

/**
 * @author unknow
 */
public class XmlElement {
	private final QName qname;
	private final BeanProperty b;
	private final Supplier<XmlType> xmlType;

	/**
	 * create new XmlElement
	 * 
	 * @param qname the name
	 * @param b the property
	 * @param xmlType xmlType builder
	 */
	public XmlElement(QName qname, BeanProperty b, Supplier<XmlType> xmlType) {
		this.qname = qname;
		this.b = b;
		this.xmlType = xmlType;
	}

	public String ns() {
		return qname.getNamespaceURI();
	}

	public String name() {
		return qname.getLocalPart();
	}

	public QName qname() {
		return qname;
	}

	public XmlType xmlType() {
		return xmlType.get();
	}

	public TypeModel type() {
		return b.type();
	}

	public String getter() {
		return b.getter().name();
	}

	public String setter() {
		return b.setter().name();
	}

	public BeanProperty prop() {
		return b;
	}

	@Override
	public String toString() {
		StringBuilder sb = new StringBuilder();
		toString(sb, new HashSet<>());
		return sb.toString();
	}

	public void toString(StringBuilder sb, Set<XmlType> saw) {
		if (qname != null)
			sb.append(qname).append(':');
		xmlType.get().toString(sb, saw);
	}
}
