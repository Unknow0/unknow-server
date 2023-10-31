/**
 * 
 */
package unknow.server.maven.jaxb.model;

import javax.xml.namespace.QName;

import unknow.server.maven.model.BeanProperty;
import unknow.server.maven.model.TypeModel;

/**
 * @author unknow
 */
public class XmlElement {
	private final XmlLoader loader;
	private final QName qname;
	private final TypeModel type;
	private final BeanProperty b;

	/**
	 * create new XmlElement
	 * 
	 * @param qname
	 * @param type
	 * @param getter
	 * @param setter
	 */
	public XmlElement(XmlLoader loader, QName qname, TypeModel type, BeanProperty b) {
		this.loader = loader;
		this.qname = qname;
		this.type = type;
		this.b = b;
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
		return loader.add(b.type());
	}

	public TypeModel type() {
		return type;
	}

	public String getter() {
		return b.getter().name();
	}

	public String setter() {
		return b.setter().name();
	}

	@Override
	public String toString() {
		return qname + " " + type;
	}
}
