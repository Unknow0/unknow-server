/**
 * 
 */
package unknow.server.maven.jaxb.model;

import java.util.ArrayDeque;
import java.util.Arrays;
import java.util.Collections;
import java.util.Comparator;
import java.util.Deque;
import java.util.Iterator;
import java.util.List;
import java.util.NoSuchElementException;

/**
 * @author unknow
 */
public interface XmlElements extends Iterable<XmlElements> {

	XmlGroup group();

	Iterable<XmlElement> childs();

	String firstName();

	void sort(Comparator<XmlElements> cmp);

	public static class XmlSimpleElements implements XmlElements {
		private final XmlElement e;

		public XmlSimpleElements(XmlElement e) {
			this.e = e;
		}

		public XmlElement element() {
			return e;
		}

		@Override
		public String firstName() {
			String n = e.getter();
			return Character.toLowerCase(n.charAt(3)) + n.substring(4);
		}

		@Override
		public XmlGroup group() {
			return XmlGroup.SIMPLE;
		}

		@Override
		public void sort(Comparator<XmlElements> cmp) {
			// noting to sort
		}

		@Override
		public Iterator<XmlElements> iterator() {
			return Collections.emptyIterator();
		}

		@Override
		public Iterable<XmlElement> childs() {
			return Arrays.asList(e);
		}

		@Override
		public String toString() {
			return e.toString();
		}
	}

	public static class XmlGroupElements implements XmlElements {
		private final XmlGroup group;
		private final List<XmlElements> elements;
		private final Iterable<XmlElement> it;

		/**
		 * create new XmlElements
		 * 
		 * @param group
		 * @param elements
		 */
		public XmlGroupElements(XmlGroup group, List<XmlElements> elements) {
			this.group = group;
			this.elements = elements;
			this.it = () -> new ChildIt(elements.iterator());
		}

		@Override
		public XmlGroup group() {
			return group;
		}

		@Override
		public String firstName() {
			return elements.get(0).firstName();
		}

		@Override
		public void sort(Comparator<XmlElements> cmp) {
			elements.sort(cmp);
			for (XmlElements e : elements)
				e.sort(cmp);
		}

		@Override
		public Iterator<XmlElements> iterator() {
			return elements.iterator();
		}

		@Override
		public Iterable<XmlElement> childs() {
			return it;
		}

		@Override
		public String toString() {
			return group.toString() + elements;
		}
	}

	public static class ChildIt implements Iterator<XmlElement> {
		private final Deque<Iterator<XmlElements>> q = new ArrayDeque<>();
		private Iterator<XmlElements> it;

		public ChildIt(Iterator<XmlElements> it) {
			this.it = it;
		}

		@Override
		public boolean hasNext() {
			return it.hasNext() || !q.isEmpty();
		}

		@Override
		public XmlElement next() {
			if (!it.hasNext() && !q.isEmpty())
				it = q.removeLast();

			while (it.hasNext()) {
				XmlElements next = it.next();
				if (next.group() == XmlGroup.SIMPLE)
					return next.childs().iterator().next();
				if (it.hasNext())
					q.addLast(it);
				it = next.iterator();
			}
			throw new NoSuchElementException();
		}
	}

	public enum XmlGroup {
		SIMPLE, SEQUENCE, ALL, CHOICE;

		@Override
		public String toString() {
			switch (this) {
				case SIMPLE:
					return "simple";
				case SEQUENCE:
					return "sequence";
				case ALL:
					return "all";
				case CHOICE:
					return "choice";
			}
			return "ERROR";
		}
	}

}
