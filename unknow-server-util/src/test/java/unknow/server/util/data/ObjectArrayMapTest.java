package unknow.server.util.data;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.Comparator;

import org.junit.jupiter.api.Test;

class ObjectArrayMapTest {
	@Test
	void insertsKeysInComparatorOrderAndGrows() {
		ObjectArrayMap<Integer, String> map = new ObjectArrayMap<Integer, String>(Comparator.naturalOrder());
		for (int i = 19; i >= 0; i--)
			map.put(i, "v" + i);

		assertEquals(20, map.size());
		assertEquals("v0", map.get(0));
		assertEquals("v10", map.get(10));
		assertEquals("v19", map.get(19));
	}

	@Test
	void putReturnsPreviousValueWhenReplacing() {
		ObjectArrayMap<String, Integer> map = new ObjectArrayMap<String, Integer>(Comparator.naturalOrder());

		assertEquals(null, map.put("key", 1));
		assertEquals(1, map.put("key", 2));
		assertEquals(2, map.get("key"));
	}

	@Test
	void valuesViewIsAvailableAndContainsMappedValues() {
		ObjectArrayMap<String, Integer> map = new ObjectArrayMap<String, Integer>(Comparator.naturalOrder());
		map.put("one", 1);

		assertNotNull(map.values());
		assertTrue(map.values().contains(1));
	}
}