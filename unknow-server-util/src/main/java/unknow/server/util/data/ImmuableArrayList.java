package unknow.server.util.data;

import java.util.Arrays;
import java.util.List;
import java.util.ListIterator;
import java.util.Objects;
import java.util.Set;
import java.util.Spliterator;
import java.util.stream.Stream;

public class ImmuableArrayList<T> extends AbstractImmuableList<T> implements Set<T>, List<T> {
	private final T[] t;

	public ImmuableArrayList(T[] t) {
		this(t, 0, t.length);
	}

	public ImmuableArrayList(T[] t, int o, int e) {
		super(o, e);
		this.t = t;
	}

	@Override
	public boolean contains(Object obj) {
		for (int i = o; i < e; i++) {
			if (Objects.equals(t[i], obj))
				return true;
		}
		return false;
	}

	@Override
	public Object[] toArray() {
		return Arrays.copyOfRange(t, o, e);
	}

	@SuppressWarnings("unchecked")
	@Override
	public <E> E[] toArray(E[] a) {
		int size = size();
		if (a.length < size)
			return Arrays.copyOf(t, size, (Class<? extends E[]>) a.getClass());
		System.arraycopy(t, 0, a, 0, size);
		if (a.length > size)
			a[size] = null;
		return a;
	}

	@Override
	public T get(int index) {
		checkIndex(index);
		return t[index + o];
	}

	@Override
	public int indexOf(Object obj) {
		for (int i = o; i < e; i++) {
			if (Objects.equals(t[i], obj))
				return i - o;
		}
		return -1;
	}

	@Override
	public int lastIndexOf(Object obj) {
		int i = e;
		while (i >= o && !Objects.equals(t[--i], obj))
			;
		return i - o;
	}

	@Override
	public ListIterator<T> listIterator() {
		return new ArrayIt<>(t, o, size(), 0);
	}

	@Override
	public ListIterator<T> listIterator(int index) {
		checkIndex(index);
		return new ArrayIt<>(t, o, size(), index);
	}

	@Override
	protected List<T> createSublist(int fromIndex, int toIndex) {
		return new ImmuableArrayList<>(t, fromIndex + o, toIndex + o);
	}

	@Override
	public Stream<T> stream() {
		return Arrays.stream(t, o, e);
	}

	@Override
	public Spliterator<T> spliterator() {
		return Arrays.spliterator(t, o, e);
	}
}
