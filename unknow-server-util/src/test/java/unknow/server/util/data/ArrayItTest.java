package unknow.server.util.data;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.NoSuchElementException;

import org.junit.jupiter.api.Test;

class ArrayItTest {
	@Test
	void iteratesOverArrayInOrder() {
		ArrayIt<String> it = new ArrayIt<>(new String[] { "one", "two", "three" });

		assertTrue(it.hasNext());
		assertEquals("one", it.next());
		assertEquals("two", it.next());
		assertEquals("three", it.next());
		assertFalse(it.hasNext());
		assertThrows(NoSuchElementException.class, it::next);
	}

	@Test
	void emptyArrayHasNoElements() {
		ArrayIt<Object> it = new ArrayIt<>(new Object[0]);

		assertFalse(it.hasNext());
		assertThrows(NoSuchElementException.class, it::next);
	}
}