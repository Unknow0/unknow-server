package io.protostuff;

import static org.junit.jupiter.api.Assertions.assertEquals;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Objects;
import java.util.stream.Stream;

import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;

public class JsonXInputTest {
	public static final Stream<Arguments> test() {
		//@formatter:off
		return Stream.of(
				Arguments.of("{\"v\":\"val😜\"}", new Truc("val😜", null, null)),
				Arguments.of("  { \"v\"	:\n\"val\" }", new Truc("val", null, null)),
				Arguments.of("{\"v\":\"val\",\"loop\":{\"v\":\"inside\"}}", new Truc("val", null, new Truc("inside", null, null))),
				Arguments.of("{\"v\":\"val\",\"loop\":null}", new Truc("val", null, null)),
				Arguments.of("{\"v\":\"val\",\"list\":[]}", new Truc("val", null, null)),
				Arguments.of("{\"v\":\"val\",\"list\":[1,3,null]}", new Truc("val", Arrays.asList(1,3), null)),
				Arguments.of("{ \"b\"	:\n\"val\" }", new Truc()),
				Arguments.of("{}", new Truc()),
				Arguments.of("\"v\":\"val\"}", null),
				Arguments.of("{v\":\"val\"}", null),
				Arguments.of("{\"v:\"val\"}", null),
				Arguments.of("{\"v\"\"val\"}", null),
				Arguments.of("{\"v\":val\"}", null),
				Arguments.of("{\"v\":\"val}", null),
				Arguments.of("{\"v\":\"val\"", null)
				); //@formatter:on
	}

	@ParameterizedTest
	@MethodSource
	public void test(String json, Truc result) throws IOException {
		JsonXInput in = new JsonXInput(new ByteArrayInputStream(json.getBytes(StandardCharsets.UTF_8)));
		try {
			in.readStartObject();
			Truc truc = new Truc();
			Truc.getSchema().mergeFrom(in, truc);
			assertEquals(result, truc);
		} catch (JsonInputException e) {
			if (result != null)
				throw e;
		}
	}

	/**
	 * 
	 */
	public static final class Truc implements Message<Truc> {

		public static Schema<Truc> getSchema() {
			return SCHEMA;
		}

		public static Truc getDefaultInstance() {
			return DEFAULT_INSTANCE;
		}

		static final Truc DEFAULT_INSTANCE = new Truc();

		String v;
		List<Integer> list;
		Truc loop;

		public Truc() {
		}

		public Truc(String v, List<Integer> list, Truc loop) {
			this.v = v;
			this.list = list;
			this.loop = loop;
		}

		@Override
		public String toString() {
			return "Truc [v=" + v + ", list=" + list + ", loop=" + loop + "]";
		}

		@Override
		public int hashCode() {
			return Objects.hash(list, loop, v);
		}

		@Override
		public boolean equals(Object obj) {
			if (this == obj)
				return true;
			if (obj == null)
				return false;
			if (getClass() != obj.getClass())
				return false;
			Truc other = (Truc) obj;
			return Objects.equals(list, other.list) && Objects.equals(loop, other.loop) && Objects.equals(v, other.v);
		}

		@Override
		public Schema<Truc> cachedSchema() {
			return SCHEMA;
		}

		static final Schema<Truc> SCHEMA = new Schema<Truc>() {
			// schema methods
			@Override
			public Truc newMessage() {
				return new Truc();
			}

			@Override
			public Class<Truc> typeClass() {
				return Truc.class;
			}

			@Override
			public String messageName() {
				return Truc.class.getSimpleName();
			}

			@Override
			public String messageFullName() {
				return Truc.class.getName();
			}

			@Override
			public boolean isInitialized(Truc message) {
				return true;
			}

			@Override
			public void mergeFrom(Input input, Truc message) throws IOException {
				for (int number = input.readFieldNumber(this);; number = input.readFieldNumber(this)) {
					switch (number) {
						case 0:
							return;
						case 1:
							message.v = input.readString();
							break;
						case 2:
							if (message.list == null)
								message.list = new ArrayList<>();
							message.list.add(input.readInt32());
							break;
						case 3:
							message.loop = input.mergeObject(message.loop, Truc.getSchema());
							break;
						default:
							input.handleUnknownField(number, this);
					}
				}
			}

			@Override
			public void writeTo(Output output, Truc message) throws IOException {
				if (message.v != null)
					output.writeString(1, message.v, false);
			}

			@Override
			public String getFieldName(int number) {
				switch (number) {
					case 1:
						return "v";
					case 2:
						return "list";
					case 3:
						return "loop";
					default:
						return null;
				}
			}

			@Override
			public int getFieldNumber(String name) {
				return fieldMap.getOrDefault(name, 0);
			}

			final java.util.HashMap<String, Integer> fieldMap = new java.util.HashMap<String, Integer>();
			{
				fieldMap.put("v", 1);
				fieldMap.put("list", 2);
				fieldMap.put("loop", 3);
			}
		};

	}
}
