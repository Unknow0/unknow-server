package unknow.server.util.data;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.Test;

class IntArrayMapTest {
	@Test
	void insertsKeysInSortedOrderAndGrows() {
		IntArrayMap<String> map = new IntArrayMap<>();
		for (int i = 19; i >= 0; i--)
			map.set(i, "v" + i);

		assertEquals(20, map.size());
		assertEquals("v0", map.get(0));
		assertEquals("v10", map.get(10));
		assertEquals("v19", map.get(19));
	}

	@Test
	void setReturnsPreviousValueAndRemoveDeletesKey() {
		IntArrayMap<String> map = new IntArrayMap<>();

		assertEquals(null, map.set(4, "first"));
		assertEquals("first", map.set(4, "second"));
		assertEquals("second", map.remove(4));
		assertFalse(map.contains(4));
		assertEquals(0, map.size());
	}

	@Test
	void keySetContainsAKeyEvenWhenItsValueIsNull() {
		IntArrayMap<String> map = new IntArrayMap<>();
		map.set(12, null);

		assertTrue(map.keySet().contains(12));
	}

	@Test
	void valuesCollectionReportsContainedValues() {
		IntArrayMap<String> map = new IntArrayMap<>();
		map.set(1, "one");
		map.set(2, "two");

		assertTrue(map.values().contains("two"));
		assertFalse(map.values().contains("missing"));
	}
}