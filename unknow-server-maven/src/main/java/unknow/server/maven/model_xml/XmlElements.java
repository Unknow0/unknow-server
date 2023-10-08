/**
 * 
 */
package unknow.server.maven.model_xml;

import java.util.Iterator;
import java.util.List;

/**
 * @author unknow
 */
public class XmlElements implements Iterable<XmlElement> {
	private final XmlGroup group;
	private final List<XmlElement> elements;

	/**
	 * create new XmlElements
	 * 
	 * @param group
	 * @param elements
	 */
	public XmlElements(XmlGroup group, List<XmlElement> elements) {
		this.group = group;
		this.elements = elements;
	}

	public XmlGroup group() {
		return group;
	}

	@Override
	public Iterator<XmlElement> iterator() {
		return elements.iterator();
	}

	public static enum XmlGroup {
		SEQUENCE, ALL, CHOICE
	}

}
