/**
 * 
 */
package unknow.server.maven.model_xml;

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
}
