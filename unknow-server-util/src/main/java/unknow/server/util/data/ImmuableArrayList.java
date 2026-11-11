package unknow.server.util.data;

import java.util.Arrays;
import java.util.Collection;
import java.util.Iterator;
import java.util.List;
import java.util.ListIterator;
import java.util.Objects;
import java.util.Set;
import java.util.Spliterator;
import java.util.stream.Stream;

public class ImmuableArrayList<T> implements Set<T>, List<T> {
	private final T[] t;
	private final int o;
	private final int e;

	public ImmuableArrayList(T[] t) {
		this(t, 0, t.length);
	}

	public ImmuableArrayList(T[] t, int o, int e) {
		this.t = t;
		this.o = o;
		this.e = e;
	}

	@Override
	public int size() {
		return e - o;
	}

	@Override
	public boolean isEmpty() {
		return e == o;
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
	public Iterator<T> iterator() {
		return listIterator();
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
	public boolean add(T e) {
		throw new UnsupportedOperationException();
	}

	@Override
	public boolean remove(Object o) {
		throw new UnsupportedOperationException();
	}

	@Override
	public boolean containsAll(Collection<?> c) {
		for (Object o : c) {
			if (!contains(o))
				return false;
		}
		return true;
	}

	@Override
	public boolean addAll(Collection<? extends T> c) {
		throw new UnsupportedOperationException();
	}

	@Override
	public boolean removeAll(Collection<?> c) {
		throw new UnsupportedOperationException();
	}

	@Override
	public boolean retainAll(Collection<?> c) {
		throw new UnsupportedOperationException();
	}

	@Override
	public void clear() {
		throw new UnsupportedOperationException();
	}

	@Override
	public boolean addAll(int index, Collection<? extends T> c) {
		throw new UnsupportedOperationException();
	}

	@Override
	public T get(int index) {
		if (index < 0 || index > size())
			throw new IndexOutOfBoundsException();
		return t[index + o];
	}

	@Override
	public T set(int index, T element) {
		throw new UnsupportedOperationException();
	}

	@Override
	public void add(int index, T element) {
		throw new UnsupportedOperationException();
	}

	@Override
	public T remove(int index) {
		throw new UnsupportedOperationException();
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
		if (index < 0 || index > size())
			throw new IndexOutOfBoundsException();
		return new ArrayIt<>(t, o, size(), index);
	}

	@Override
	public List<T> subList(int fromIndex, int toIndex) {
		if (fromIndex == 0 && toIndex == size())
			return this;

		if ((fromIndex | toIndex) < 0 || fromIndex > toIndex || toIndex > size())
			throw new IndexOutOfBoundsException();
		if (fromIndex == toIndex)
			return List.of();
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
