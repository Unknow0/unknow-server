package unknow.server.util.data;

import java.util.Arrays;
import java.util.List;
import java.util.ListIterator;
import java.util.Set;
import java.util.Spliterator;
import java.util.stream.Stream;

public class IntImmuableArrayList extends AbstractImmuableList<Integer> implements Set<Integer>, List<Integer> {
	private final int[] t;

	public IntImmuableArrayList(int[] t) {
		this(t, 0, t.length);
	}

	public IntImmuableArrayList(int[] t, int o, int e) {
		super(o, e);
		this.t = t;
	}

	@Override
	public boolean contains(Object obj) {
		if (!(obj instanceof Integer))
			return false;
		int v = (Integer) obj;
		for (int i = o; i < e; i++) {
			if (t[i] == v)
				return true;
		}
		return false;
	}

	@Override
	public Object[] toArray() {
		Integer[] a = new Integer[size()];
		for (int i = 0; i < t.length; i++)
			a[i] = t[i + o];
		return a;
	}

	@SuppressWarnings("unchecked")
	@Override
	public <E> E[] toArray(E[] a) {
		int size = size();
		if (a.length < size)
			a = Arrays.copyOf(a, size);
		for (int i = 0; i < t.length; i++)
			a[i] = (E) (Integer) t[i + o];
		if (a.length > size)
			a[size] = null;
		return a;
	}

	@Override
	public Integer get(int index) {
		checkIndex(index);
		return t[index + o];
	}

	@Override
	public int indexOf(Object obj) {
		if (!(obj instanceof Integer))
			return -1;
		int v = (Integer) obj;
		for (int i = o; i < e; i++) {
			if (t[i] == v)
				return i - o;
		}
		return -1;
	}

	@Override
	public int lastIndexOf(Object obj) {
		if (!(obj instanceof Integer))
			return -1;
		int v = (Integer) obj;
		int i = e;
		while (i >= o && t[--i] != v)
			;
		return i - o;
	}

	@Override
	public ListIterator<Integer> listIterator() {
		return new IntArrayIt(t, o, size(), 0);
	}

	@Override
	public ListIterator<Integer> listIterator(int index) {
		checkIndex(index);
		return new IntArrayIt(t, o, size(), index);
	}

	@Override
	protected List<Integer> createSublist(int fromIndex, int toIndex) {
		return new IntImmuableArrayList(t, fromIndex + o, toIndex + o);
	}

	@Override
	public Stream<Integer> stream() {
		return Arrays.stream(t, o, e).mapToObj(i -> i);
	}

	@Override
	public Spliterator<Integer> spliterator() {
		return Arrays.spliterator(t, o, e);
	}
}
