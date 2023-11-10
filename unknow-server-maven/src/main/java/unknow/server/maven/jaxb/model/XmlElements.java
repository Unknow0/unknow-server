/**
 * 
 */
package unknow.server.maven.jaxb.model;

import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Set;

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

	@Override
	public String toString() {
		StringBuilder sb = new StringBuilder();
		toString(sb, new HashSet<>());
		return sb.toString();
	}

	public void toString(StringBuilder sb, Set<XmlType> saw) {
		sb.append(group).append('[');
		Iterator<XmlElement> it = elements.iterator();
		while (it.hasNext()) {
			it.next().toString(sb, saw);
			if (it.hasNext())
				sb.append(", ");
		}
		sb.append(']');
	}

	public enum XmlGroup {
		SIMPLE, SEQUENCE, ALL;

		@Override
		public String toString() {
			switch (this) {
				case SIMPLE:
					return "simple";
				case SEQUENCE:
					return "sequence";
				case ALL:
					return "all";
			}
			return "ERROR";
		}
	}

}
