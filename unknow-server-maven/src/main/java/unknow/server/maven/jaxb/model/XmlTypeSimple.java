/**
 * 
 */
package unknow.server.maven.jaxb.model;

import java.util.Set;

import javax.xml.namespace.QName;

import unknow.server.maven.model.TypeModel;

/**
 * @author unknow
 */
public class XmlTypeSimple implements XmlType {
	private final QName qname;
	private final TypeModel type;

	/**
	 * create new XmlTypeSimple
	 * 
	 * @param qname the qname
	 * @param type the java type
	 */
	public XmlTypeSimple(QName qname, TypeModel type) {
		this.qname = qname;
		this.type = type;
	}

	@Override
	public QName name() {
		return qname;
	}

	@Override
	public TypeModel type() {
		return type;
	}

	@Override
	public String toString() {
		return type.name();
	}

	@Override
	public void toString(StringBuilder sb, Set<XmlType> saw) {
		sb.append(toString());
	}
}
