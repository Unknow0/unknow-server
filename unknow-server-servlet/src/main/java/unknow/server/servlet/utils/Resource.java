/**
 * 
 */
package unknow.server.servlet.utils;

/**
 * @author unknow
 */
public class Resource {
	private final long lastModified;
	private final long size;

	public Resource(long lastModified, long size) {
		this.lastModified = lastModified;
		this.size = size;
	}

	public long getLastModified() {
		return lastModified;
	}

	public long getSize() {
		return size;
	}
}
