package unknow.server.util.data;

import java.util.Collection;
import java.util.Iterator;
import java.util.List;
import java.util.Set;
import java.util.Spliterator;

public abstract class AbstractImmuableList<T> implements Set<T>, List<T> {
	protected final int o;
	protected final int e;

	protected AbstractImmuableList(int o, int e) {
		this.o = o;
		this.e = e;
	}

	protected void checkIndex(int index) {
		if (index < 0 || index > size())
			throw new IndexOutOfBoundsException(index);
	}

	@Override
	public abstract Spliterator<T> spliterator();

	protected abstract List<T> createSublist(int fromIndex, int toIndex);

	@Override
	public int size() {
		return e - o;
	}

	@Override
	public boolean isEmpty() {
		return e == o;
	}

	@Override
	public List<T> subList(int fromIndex, int toIndex) {
		if (fromIndex == 0 && toIndex == size())
			return this;

		if ((fromIndex | toIndex) < 0 || fromIndex > toIndex || toIndex > size())
			throw new IndexOutOfBoundsException();
		if (fromIndex == toIndex)
			return List.of();
		return createSublist(fromIndex, toIndex);
	}

	@Override
	public boolean containsAll(Collection<?> c) {
		for (Object obj : c) {
			if (!contains(obj))
				return false;
		}
		return true;
	}

	@Override
	public Iterator<T> iterator() {
		return listIterator();
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
}
