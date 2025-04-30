/**
 * 
 */
package unknow.server.maven.model.util;

import java.util.Iterator;

import unknow.server.maven.model.ClassModel;

/**
 * @author unknow
 */
public class AncestrorIterable implements Iterable<ClassModel> {
	private final ClassModel clazz;

	/**
	 * create new AncestrorIterable
	 * 
	 * @param clazz the root class
	 */
	public AncestrorIterable(ClassModel clazz) {
		this.clazz = clazz;
	}

	@Override
	public Iterator<ClassModel> iterator() {
		return new AncestrorIterator(clazz);
	}
}
