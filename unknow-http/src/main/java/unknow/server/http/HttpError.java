/**
 * 
 */
package unknow.server.http;

import java.nio.charset.StandardCharsets;

/**
 * @author unknow
 */
public enum HttpError {
	BAD_REQUEST("HTTP/1.1 400 Bad Request\r\nContent-Length: 0\r\n\r\n".getBytes(StandardCharsets.US_ASCII)),
	NOT_FOUND("HTTP/1.1 404 Not Found\r\nContent-Length: 0\r\n\r\n".getBytes(StandardCharsets.US_ASCII)),
	URI_TOO_LONG("HTTP/1.1 414 URI Too Long\r\nContent-Length: 0\r\n\r\n".getBytes(StandardCharsets.US_ASCII)),
	HEADER_TOO_LARGE("HTTP/1.1 431 Request Header Fields Too Large\r\nContent-Length: 0\r\n\r\n".getBytes(StandardCharsets.US_ASCII)),
	SERVER_ERROR("HTTP/1.1 500 Server Error\r\nContent-Length: 0\r\n\r\n".getBytes(StandardCharsets.US_ASCII));

	public final byte[] data;

	private HttpError(byte[] data) {
		this.data = data;
	}
}
