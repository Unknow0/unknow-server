/**
 * 
 */
package unknow.server.http.utils;

import java.util.Collection;
import java.util.Collections;
import java.util.Iterator;
import java.util.NoSuchElementException;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.TimeUnit;

/**
 * @author unknow
 */
public class EmptyQueue<T> implements BlockingQueue<T> {
	private static final Object[] EMPTY = new Object[0];

	@Override
	public int size() {
		return 0;
	}

	@Override
	public boolean isEmpty() {
		return true;
	}

	@Override
	public boolean contains(Object o) {
		return false;
	}

	@Override
	public Iterator<T> iterator() {
		return Collections.emptyIterator();
	}

	@Override
	public Object[] toArray() {
		return EMPTY;
	}

	@Override
	public <R> R[] toArray(R[] a) {
		return a;
	}

	@Override
	public boolean remove(Object o) {
		return false;
	}

	@Override
	public boolean containsAll(Collection<?> c) {
		return false;
	}

	@Override
	public boolean addAll(Collection<? extends T> c) {
		return false;
	}

	@Override
	public boolean removeAll(Collection<?> c) {
		return false;
	}

	@Override
	public boolean retainAll(Collection<?> c) {
		return false;
	}

	@Override
	public void clear() {
	}

	@Override
	public boolean add(T e) {
		throw new IllegalStateException();
	}

	@Override
	public boolean offer(T e) {
		return false;
	}

	@Override
	public T remove() {
		throw new NoSuchElementException();
	}

	@Override
	public T poll() {
		return null;
	}

	@Override
	public T element() {
		throw new NoSuchElementException();
	}

	@Override
	public T peek() {
		return null;
	}

	@Override
	public void put(T e) throws InterruptedException {
	}

	@Override
	public boolean offer(T e, long timeout, TimeUnit unit) throws InterruptedException {
		return false;
	}

	@Override
	public T take() throws InterruptedException {
		return null;
	}

	@Override
	public T poll(long timeout, TimeUnit unit) throws InterruptedException {
		return null;
	}

	@Override
	public int remainingCapacity() {
		return 0;
	}

	@Override
	public int drainTo(Collection<? super T> c) {
		return 0;
	}

	@Override
	public int drainTo(Collection<? super T> c, int maxElements) {
		return 0;
	}
}
