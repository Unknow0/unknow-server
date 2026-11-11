package unknow.server.util.data;

import java.util.ListIterator;

public abstract class AbstractArrayIt<T> implements ListIterator<T> {
	protected final int o;
	protected final int l;
	protected int i;

	protected AbstractArrayIt(int o, int l, int i) {
		this.o = o;
		this.l = l;
		this.i = i;
	}

	@Override
	public boolean hasNext() {
		return i < l;
	}

	@Override
	public int nextIndex() {
		return i - o;
	}

	@Override
	public int previousIndex() {
		return i - 1 - o;
	}

	@Override
	public boolean hasPrevious() {
		return i > 0;
	}

	@Override
	public void remove() {
		throw new UnsupportedOperationException();
	}

	@Override
	public void set(T e) {
		throw new UnsupportedOperationException();
	}

	@Override
	public void add(T e) {
		throw new UnsupportedOperationException();
	}
}
