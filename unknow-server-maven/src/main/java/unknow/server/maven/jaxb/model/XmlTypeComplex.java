/**
 * 
 */
package unknow.server.maven.jaxb.model;

import java.util.List;

import javax.xml.namespace.QName;

import unknow.server.maven.model.ClassModel;
import unknow.server.maven.model.TypeModel;

/**
 * @author unknow
 */
public class XmlTypeComplex implements XmlType {
	private final QName qname;
	private final ClassModel c;
	private final Factory factory;
	private final List<XmlElement> attrs;
	private final XmlElements elements;
	private final XmlElement value;

	/**
	 * create new XmlTypeComplex
	 * 
	 * @param qname
	 * @param type
	 */
	public XmlTypeComplex(QName qname, ClassModel type, Factory factory, List<XmlElement> attrs, XmlElements elements, XmlElement value) {
		this.qname = qname;
		this.c = type;
		this.factory = factory;
		this.attrs = attrs;
		this.elements = elements;
		this.value = value;
	}

	@Override
	public QName qname() {
		return qname;
	}

	@Override
	public TypeModel type() {
		return c;
	}

	public Factory factory() {
		return factory;
	}

	public List<XmlElement> getAttributes() {
		return attrs;
	}

	public XmlElements getElements() {
		return elements;
	}

	public XmlElement getValue() {
		return value;
	}

	public static class Factory {
		public final ClassModel clazz;
		public final String method;

		public Factory(ClassModel clazz, String method) {
			this.clazz = clazz;
			this.method = method;
		}
	}
}
