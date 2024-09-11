/**
 * 
 */
package unknow.server.maven.jaxb.model;

import java.util.HashSet;
import java.util.List;
import java.util.Set;

import javax.xml.namespace.QName;

import unknow.server.maven.model.BeanProperty;
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
	private final BeanProperty otherAttrs;

	/**
	 * create new XmlTypeComplex
	 * 
	 * @param qname
	 * @param type
	 */
	public XmlTypeComplex(QName qname, ClassModel type, Factory factory, List<XmlElement> attrs, XmlElements elements, XmlElement value, BeanProperty otherAttrs) {
		this.qname = qname;
		this.c = type;
		this.factory = factory;
		this.attrs = attrs;
		this.elements = elements;
		this.value = value;
		this.otherAttrs = otherAttrs;
	}

	@Override
	public QName name() {
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

	public boolean hasElements() {
		return elements != null && !elements.isEmpty();
	}

	public XmlElement getValue() {
		return value;
	}

	public BeanProperty getOtherAttrs() {
		return otherAttrs;
	}

	public static class Factory {
		public final ClassModel clazz;
		public final String method;

		public Factory(ClassModel clazz, String method) {
			this.clazz = clazz;
			this.method = method;
		}
	}

	@Override
	public String toString() {
		StringBuilder sb = new StringBuilder();
		toString(sb, new HashSet<>());
		return sb.toString();
	}

	@Override
	public void toString(StringBuilder sb, Set<XmlType> saw) {
		sb.append(c.name());
		if (!saw.add(this))
			return;

		sb.append("{attrs: [");
		for (XmlElement e : attrs)
			e.toString(sb, saw);
		sb.append("], elems: ");
		elements.toString(sb, saw);
		if (value != null)
			value.toString(sb.append(", value: "), saw);
		sb.append('}');
	}
}
