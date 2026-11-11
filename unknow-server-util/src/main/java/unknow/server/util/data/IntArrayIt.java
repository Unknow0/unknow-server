/**
 * 
 */
package unknow.server.util.data;

import java.util.ListIterator;
import java.util.NoSuchElementException;

/**
 * simple iterator over an int array
 */
public class IntArrayIt extends AbstractArrayIt<Integer> implements ListIterator<Integer> {
	private final int[] data;

	/**
	 * new iterator on array
	 * @param data the array
	 */
	public IntArrayIt(int[] data) {
		this(data, 0, data.length, 0);
	}

	/**
	 * new iterator on array
	 * @param data the array
	 * @param o start array index
	 * @param l array length length
	 * @param i start index
	 */
	public IntArrayIt(int[] data, int o, int l, int i) {
		super(o, l, i);
		this.data = data;
	}

	@Override
	public Integer next() {
		if (i == l)
			throw new NoSuchElementException();
		return data[o + i++];
	}

	@Override
	public Integer previous() {
		if (i == 0)
			throw new NoSuchElementException();
		return data[--i + o];
	}
}