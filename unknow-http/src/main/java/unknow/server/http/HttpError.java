/**
 * 
 */
package unknow.server.http;

import java.nio.charset.StandardCharsets;

/**
 * @author unknow
 */
@SuppressWarnings("javadoc")
public final class HttpError {
	private static final byte[] PROTOCOL = "HTTP/1.1 ".getBytes(StandardCharsets.US_ASCII);
	private static final byte[] EMPTY_ENDING = "\r\nContent-Length: 0\r\n\r\n".getBytes(StandardCharsets.US_ASCII);

	public static final HttpError CONTINUE = new HttpError(100, "Continue");
	public static final HttpError SWITCHING_PROTOCOL = new HttpError(101, "Switching Protocols");

	public static final HttpError OK = new HttpError(200, "Ok");
	public static final HttpError CREATED = new HttpError(201, "Created");
	public static final HttpError ACCEPTED = new HttpError(202, "Accepted");
	public static final HttpError NON_AUTHORITATIVE = new HttpError(203, "Non-Authoritative Information");
	public static final HttpError NO_CONTENT = new HttpError(204, "No Content");
	public static final HttpError RESET_CONTENT = new HttpError(205, "Reset Content");
	public static final HttpError PATIIAL_CONTENT = new HttpError(206, "Partial Content");

	public static final HttpError MULTIPLE_CHOICES = new HttpError(300, "Multiple Choices");
	public static final HttpError MOVED_PERMANENTLY = new HttpError(301, "Moved Permanently");
	public static final HttpError FOUND = new HttpError(302, "Found");
	public static final HttpError SEE_OTHER = new HttpError(303, "See Other");
	public static final HttpError NOT_MODIFIED = new HttpError(304, "Not Modified");
	public static final HttpError USE_PROXY = new HttpError(305, "Use Proxy");
	public static final HttpError SWITCH_PROXY = new HttpError(306, "Switch Proxy");
	public static final HttpError TEMPORARY_REDIRECT = new HttpError(307, "Temporary Redirect");
	public static final HttpError PERMANENT_REDIRECT = new HttpError(308, "Permanent Redirect");
	public static final HttpError TOO_MANY_REDIRECT = new HttpError(310, "Too many Redirects");

	public static final HttpError BAD_REQUEST = new HttpError(400, "Bad Request");
	public static final HttpError UNAUTHORIZED = new HttpError(401, "Unauthorized");
	public static final HttpError PAYMENT_REQUIRED = new HttpError(402, "Payment Required");
	public static final HttpError FORBIDEN = new HttpError(403, "Forbidden");
	public static final HttpError NOT_FOUND = new HttpError(404, "Not Found");
	public static final HttpError METHOD_NOT_ALLOWED = new HttpError(405, "Method Not Allowed");
	public static final HttpError NOT_ACCEPTABLE = new HttpError(406, "Not Acceptable");
	public static final HttpError PROXY_AUTH_REQUIRED = new HttpError(407, "Proxy Authentication Required");
	public static final HttpError REQUEST_TIMEOUT = new HttpError(408, "Request Time-out");
	public static final HttpError CONFLICT = new HttpError(409, "Conflict");
	public static final HttpError GONE = new HttpError(410, "Gone");
	public static final HttpError LENGTH_REQUIRED = new HttpError(411, "Length Required");
	public static final HttpError PRECONDITION_FAILED = new HttpError(412, "Precondition Failed");
	public static final HttpError ENTITY_TOO_LARGE = new HttpError(413, "Request Entity Too Large");
	public static final HttpError URI_TOO_LONG = new HttpError(414, "URI Too Long");
	public static final HttpError UNSUPORTED_MEDIA = new HttpError(415, "Unsupported Media Type");
	public static final HttpError RANGE_UNSATIFIABLE = new HttpError(416, "Requested range unsatisfiable");
	public static final HttpError EXPECTATION_FAILED = new HttpError(417, "Expectation failed");
	public static final HttpError HEADER_TOO_LARGE = new HttpError(431, "Request Header Fields Too Large");
	public static final HttpError SERVER_ERROR = new HttpError(500, "Server Error");

	public final int code;
	public final String message;
	public final byte[] encoded;
	private byte[] empty;

	private HttpError(int code, String message) {
		this.code = code;
		this.message = message;
		this.encoded = encodeStatusLine(code, message);
	}

	public byte[] empty() {
		if (empty == null)
			empty = encodeEmptyReponse(code, message);
		return empty;
	}

	public static byte[] encodeStatusLine(int code, String message) {
		byte[] bytes = message.getBytes();
		int i = PROTOCOL.length;
		byte[] b = new byte[i + 4 + bytes.length + 2];
		System.arraycopy(PROTOCOL, 0, b, 0, i);
		b[i++] = (byte) ('0' + code / 100);
		b[i++] = (byte) ('0' + (code / 10 % 10));
		b[i++] = (byte) ('0' + (code % 10));
		b[i++] = ' ';
		System.arraycopy(bytes, 0, b, i, bytes.length);
		i += bytes.length;
		b[i++] = '\r';
		b[i] = '\n';
		return b;
	}

	public static byte[] encodeEmptyReponse(int code, String message) {
		byte[] bytes = message.getBytes();
		int i = PROTOCOL.length;
		byte[] b = new byte[i + 4 + bytes.length + EMPTY_ENDING.length];
		System.arraycopy(PROTOCOL, 0, b, 0, i);
		b[i++] = (byte) ('0' + code / 100);
		b[i++] = (byte) ('0' + (code / 10 % 10));
		b[i++] = (byte) ('0' + (code % 10));
		b[i++] = ' ';
		System.arraycopy(bytes, 0, b, i, bytes.length);
		System.arraycopy(EMPTY_ENDING, 0, b, i + bytes.length, EMPTY_ENDING.length);
		return b;
	}

	/**
	 * @param status
	 * @return
	 */
	public static HttpError fromStatus(int status) {
		switch (status) {
			default:
				return null;
			case 100:
				return CONTINUE;
			case 101:
				return SWITCHING_PROTOCOL;
			case 200:
				return OK;
			case 201:
				return CREATED;
			case 202:
				return ACCEPTED;
			case 203:
				return NON_AUTHORITATIVE;
			case 204:
				return NO_CONTENT;
			case 205:
				return RESET_CONTENT;
			case 206:
				return PATIIAL_CONTENT;
			case 300:
				return MULTIPLE_CHOICES;
			case 301:
				return MOVED_PERMANENTLY;
			case 302:
				return FOUND;
			case 303:
				return SEE_OTHER;
			case 304:
				return NOT_MODIFIED;
			case 305:
				return USE_PROXY;
			case 306:
				return SWITCH_PROXY;
			case 307:
				return TEMPORARY_REDIRECT;
			case 308:
				return PERMANENT_REDIRECT;
			case 310:
				return TOO_MANY_REDIRECT;
			case 400:
				return BAD_REQUEST;
			case 401:
				return UNAUTHORIZED;
			case 402:
				return PAYMENT_REQUIRED;
			case 403:
				return FORBIDEN;
			case 404:
				return NOT_FOUND;
			case 405:
				return METHOD_NOT_ALLOWED;
			case 406:
				return NOT_ACCEPTABLE;
			case 407:
				return PROXY_AUTH_REQUIRED;
			case 408:
				return REQUEST_TIMEOUT;
			case 409:
				return CONFLICT;
			case 410:
				return GONE;
			case 411:
				return LENGTH_REQUIRED;
			case 412:
				return PRECONDITION_FAILED;
			case 413:
				return ENTITY_TOO_LARGE;
			case 414:
				return URI_TOO_LONG;
			case 415:
				return UNSUPORTED_MEDIA;
			case 416:
				return RANGE_UNSATIFIABLE;
			case 417:
				return EXPECTATION_FAILED;
			case 431:
				return HEADER_TOO_LARGE;
			case 500:
				return SERVER_ERROR;
		}
	}

	@Override
	public String toString() {
		return code + " " + message;
	}
}