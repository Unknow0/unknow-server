package unknow.server.http.jaxrs.impl;

import java.lang.annotation.Annotation;
import java.lang.reflect.Type;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.time.OffsetDateTime;
import java.time.OffsetTime;
import java.time.ZonedDateTime;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeParseException;

import jakarta.ws.rs.ext.ParamConverter;
import jakarta.ws.rs.ext.ParamConverterProvider;

public class JavaTimeTypesConverter implements ParamConverterProvider {
	private static final ParamConverter<ZonedDateTime> ZONED_DATE_TIME = new ParamConverter<ZonedDateTime>() {
		@Override
		public ZonedDateTime fromString(String value) {
			try {
				return ZonedDateTime.parse(value);
			} catch (DateTimeParseException parseException) {
				throw new IllegalArgumentException(parseException);
			}
		}

		@Override
		public String toString(ZonedDateTime zonedDateTime) {
			return zonedDateTime.toString();
		}

	};

	private static final ParamConverter<LocalDateTime> LOCAL_DATE_TIME = new ParamConverter<LocalDateTime>() {
		@Override
		public LocalDateTime fromString(String value) {
			try {
				return LocalDateTime.parse(value, getFormatter());
			} catch (DateTimeParseException parseException) {
				throw new IllegalArgumentException(parseException);
			}
		}

		@Override
		public String toString(LocalDateTime localDateTime) {
			return getFormatter().format(localDateTime);
		}

		protected DateTimeFormatter getFormatter() {
			return DateTimeFormatter.ISO_LOCAL_DATE_TIME;
		}
	};

	private static final ParamConverter<LocalDate> LOCAL_DATE = new ParamConverter<LocalDate>() {
		@Override
		public LocalDate fromString(String value) {
			try {
				return LocalDate.parse(value);
			} catch (DateTimeParseException parseException) {
				throw new IllegalArgumentException(parseException);
			}
		}

		@Override
		public String toString(LocalDate localDate) {
			return localDate.toString();
		}
	};

	private static final ParamConverter<LocalTime> LOCAL_TIME = new ParamConverter<LocalTime>() {
		@Override
		public LocalTime fromString(String value) {
			try {
				return LocalTime.parse(value);
			} catch (DateTimeParseException parseException) {
				throw new IllegalArgumentException(parseException);
			}
		}

		@Override
		public String toString(LocalTime localTime) {
			return localTime.toString();
		}
	};

	private static final ParamConverter<OffsetDateTime> OFFSET_DATE_TIME = new ParamConverter<OffsetDateTime>() {
		@Override
		public OffsetDateTime fromString(String value) {
			try {
				return OffsetDateTime.parse(value);
			} catch (DateTimeParseException parseException) {
				throw new IllegalArgumentException(parseException);
			}
		}

		@Override
		public String toString(OffsetDateTime offsetDateTime) {
			return offsetDateTime.toString();
		}
	};

	private static final ParamConverter<OffsetTime> OFFSET_TIME = new ParamConverter<OffsetTime>() {
		@Override
		public OffsetTime fromString(String value) {
			try {
				return OffsetTime.parse(value);
			} catch (DateTimeParseException parseException) {
				throw new IllegalArgumentException(parseException);
			}
		}

		@Override
		public String toString(OffsetTime offsetTime) {
			return offsetTime.toString();
		}
	};

	@SuppressWarnings("unchecked")
	@Override
	public <T> ParamConverter<T> getConverter(Class<T> rawType, Type genericType, Annotation[] annotations) {
		if (rawType.equals(LocalDateTime.class))
			return (ParamConverter<T>) LOCAL_DATE_TIME;
		if (rawType.equals(LocalDate.class))
			return (ParamConverter<T>) LOCAL_DATE;
		if (rawType.equals(LocalTime.class))
			return (ParamConverter<T>) LOCAL_TIME;
		if (rawType.equals(OffsetDateTime.class))
			return (ParamConverter<T>) OFFSET_DATE_TIME;
		if (rawType.equals(OffsetTime.class))
			return (ParamConverter<T>) OFFSET_TIME;
		if (rawType.equals(ZonedDateTime.class))
			return (ParamConverter<T>) ZONED_DATE_TIME;
		return null;
	}
}