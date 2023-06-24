/**
 * 
 */
package unknow.server.http.jaxrs.impl;

import java.lang.annotation.Annotation;
import java.lang.reflect.Type;

import jakarta.ws.rs.ext.ParamConverter;
import jakarta.ws.rs.ext.ParamConverterProvider;

public final class DefaultConvert implements ParamConverterProvider {
	public static final ParamConverter<String> STRING = new ParamConverter<>() {
		@Override
		public String fromString(String value) {
			return value;
		}

		@Override
		public String toString(String value) {
			return value;
		}
	};
	public static final ParamConverter<Boolean> BOOLEAN = new ParamConverter<>() {
		@Override
		public Boolean fromString(String value) {
			return Boolean.parseBoolean(value);
		}

		@Override
		public String toString(Boolean value) {
			return value.toString();
		}
	};
	public static final ParamConverter<Byte> BYTE = new ParamConverter<>() {
		@Override
		public Byte fromString(String value) {
			return Byte.parseByte(value);
		}

		@Override
		public String toString(Byte value) {
			return value.toString();
		}
	};
	public static final ParamConverter<Short> SHORT = new ParamConverter<>() {
		@Override
		public Short fromString(String value) {
			return Short.parseShort(value);
		}

		@Override
		public String toString(Short value) {
			return value.toString();
		}
	};
	public static final ParamConverter<Integer> INTEGER = new ParamConverter<>() {
		@Override
		public Integer fromString(String value) {
			return Integer.parseInt(value);
		}

		@Override
		public String toString(Integer value) {
			return value.toString();
		}
	};
	public static final ParamConverter<Long> LONG = new ParamConverter<>() {
		@Override
		public Long fromString(String value) {
			return Long.parseLong(value);
		}

		@Override
		public String toString(Long value) {
			return value.toString();
		}
	};
	public static final ParamConverter<Float> FLOAT = new ParamConverter<>() {
		@Override
		public Float fromString(String value) {
			return Float.parseFloat(value);
		}

		@Override
		public String toString(Float value) {
			return value.toString();
		}
	};
	public static final ParamConverter<Double> DOUBLE = new ParamConverter<>() {
		@Override
		public Double fromString(String value) {
			return Double.parseDouble(value);
		}

		@Override
		public String toString(Double value) {
			return value.toString();
		}
	};

	@SuppressWarnings("unchecked")
	@Override
	public <T> ParamConverter<T> getConverter(Class<T> rawType, Type genericType, Annotation[] annotations) {
		if (rawType == String.class)
			return (ParamConverter<T>) STRING;
		if (rawType == Boolean.class || rawType == boolean.class)
			return (ParamConverter<T>) BOOLEAN;
		if (rawType == Byte.class || rawType == byte.class)
			return (ParamConverter<T>) BYTE;
		if (rawType == Short.class || rawType == short.class)
			return (ParamConverter<T>) SHORT;
		if (rawType == Integer.class || rawType == int.class)
			return (ParamConverter<T>) INTEGER;
		if (rawType == Long.class || rawType == long.class)
			return (ParamConverter<T>) LONG;
		if (rawType == Float.class || rawType == float.class)
			return (ParamConverter<T>) FLOAT;
		if (rawType == Double.class || rawType == double.class)
			return (ParamConverter<T>) DOUBLE;
		return null;
	}
}