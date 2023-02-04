/**
 * 
 */
package unknow.server.http.jaxrs.impl;

import java.time.Instant;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.Date;

import jakarta.ws.rs.ext.RuntimeDelegate.HeaderDelegate;

/**
 * @author unknow
 */
public class DateDelegate implements HeaderDelegate<Date> {

	public static final DateDelegate INSTANCE = new DateDelegate();

	private DateDelegate() {
	}

	@Override
	public Date fromString(String value) {
		return Date.from(Instant.from(DateTimeFormatter.RFC_1123_DATE_TIME.parse(value)));
	}

	@Override
	public String toString(Date c) {
		return c.toInstant().atZone(ZoneId.systemDefault()).format(DateTimeFormatter.RFC_1123_DATE_TIME);
	}

}
