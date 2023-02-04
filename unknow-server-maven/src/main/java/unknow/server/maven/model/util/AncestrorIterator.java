/**
 * 
 */
package unknow.server.maven.model.util;

import java.util.Iterator;
import java.util.List;
import java.util.Queue;
import java.util.concurrent.LinkedBlockingQueue;

import unknow.server.maven.model.ClassModel;

/**
 * iterate over the ancestror of a class. <br>
 * (iterate over interface first, then super class) <code><pre>
 * class A extends B implements I1, I2
 * interface I1 extends I3</pre></code> iteration over A will give : <code>[A, I1, I3, I2, B]</code>
 * 
 * @author unknow
 */
public class AncestrorIterator implements Iterator<ClassModel> {
	private final Queue<ClassModel> queue;

	public AncestrorIterator(ClassModel clazz) {
		this.queue = new LinkedBlockingQueue<>();
		this.queue.add(clazz);
	}

	@Override
	public boolean hasNext() {
		return !queue.isEmpty();
	}

	@Override
	public ClassModel next() {
		ClassModel poll = queue.poll();
		ClassModel s = poll.superType();
		if (s != null)
			queue.add(s);
		List<ClassModel> interfaces = poll.interfaces();
		for (int i = interfaces.size() - 1; i >= 0; --i)
			queue.add(interfaces.get(i));
		return poll;
	}
}