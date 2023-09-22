/**
 * 
 */
package unknow.server.maven.jaxb.model;

import javax.xml.namespace.QName;

import unknow.server.maven.model.TypeModel;

/**
 * @author unknow
 */
public class XmlElement {
	private final XmlLoader loader;
	private final QName qname;
	private final TypeModel type;
	private final String getter;
	private final String setter;

	/**
	 * create new XmlElement
	 * 
	 * @param qname
	 * @param type
	 * @param getter
	 * @param setter
	 */
	public XmlElement(XmlLoader loader, QName qname, TypeModel type, String getter, String setter) {
		this.loader = loader;
		this.qname = qname;
		this.type = type;
		this.getter = getter;
		this.setter = setter;
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
		return loader.add(type);
	}

	public TypeModel type() {
		return type;
	}

	public String getter() {
		return getter;
	}

	public String setter() {
		return setter;
	}
}
