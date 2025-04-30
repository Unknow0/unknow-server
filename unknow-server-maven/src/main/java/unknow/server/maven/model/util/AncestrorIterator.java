/**
 * 
 */
package unknow.server.maven.model.util;

import java.util.HashSet;
import java.util.Iterator;
import java.util.NoSuchElementException;
import java.util.Queue;
import java.util.Set;
import java.util.concurrent.LinkedBlockingQueue;

import unknow.server.maven.model.ClassModel;

/**
 * iterate over the ancestror of a class. <br>
 * (iterate over interface first, then super class)
 * 
 * <pre>
 * <code>
 * class A extends B implements I1, I2
 * interface I1 extends I3</code>
 * </pre>
 * 
 * iteration over A will give : <code>[A, I1, I3, I2, B]</code>
 * 
 * @author unknow
 */
public class AncestrorIterator implements Iterator<ClassModel> {
	private final Queue<ClassModel> queue;
	private final Set<String> saw;

	/**
	 * create new AncestrorIterator
	 * 
	 * @param clazz the root class
	 */
	public AncestrorIterator(ClassModel clazz) {
		this.queue = new LinkedBlockingQueue<>();
		this.queue.add(clazz);
		this.saw = new HashSet<>();
		this.saw.add(clazz.toString());
	}

	@Override
	public boolean hasNext() {
		return !queue.isEmpty();
	}

	@Override
	public ClassModel next() {
		if (queue.isEmpty())
			throw new NoSuchElementException();
		ClassModel poll = queue.poll();
		ClassModel s = poll.superType();
		if (s != null && !"java.lang.Object".equals(s.name()) && saw.add(s.toString()))
			queue.add(s);

		for (ClassModel i : poll.interfaces()) {
			if (saw.add(i.toString()))
				queue.add(i);
		}
		return poll;
	}
}