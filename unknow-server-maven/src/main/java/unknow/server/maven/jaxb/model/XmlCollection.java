/**
 * 
 */
package unknow.server.maven.jaxb.model;

import java.util.HashSet;
import java.util.Set;

import javax.xml.namespace.QName;

import unknow.server.maven.model.TypeModel;

/**
 * @author unknow
 */
public class XmlCollection implements XmlType {
	private final TypeModel type;
	private final XmlType component;

	public XmlCollection(TypeModel type, XmlType component) {
		this.type = type;
		this.component = component;
	}

	public XmlType component() {
		return component;
	}

	@Override
	public QName name() {
		return component.name();
	}

	@Override
	public TypeModel type() {
		return type;
	}

	@Override
	public String toString() {
		StringBuilder sb = new StringBuilder();
		toString(sb, new HashSet<>());
		return sb.toString();
	}

	@Override
	public void toString(StringBuilder sb, Set<XmlType> saw) {
		component.toString(sb.append("List "), saw);
	}
}
