/**
 * 
 */
package unknow.server.http.servlet.out;

import java.io.IOException;

/**
 * a servlet output
 * 
 * @author unknow
 */
public interface Output {
	/**
	 * @return true if the content will use the chunked encoding
	 */
	boolean isChuncked();

	/**
	 * flush the buffers
	 * 
	 * @throws IOException on ioexception
	 */
	void flush() throws IOException;

	/**
	 * mark this output as close (don't close the real output)
	 * 
	 * @throws IOException on ioexception
	 */
	void close() throws IOException;

	/**
	 * reset/clear the buffers
	 */
	void resetBuffers();

	/**
	 * set buffers size
	 * 
	 * @param size the new size
	 * @throws IllegalArgumentException if this output doens't use buffers
	 */
	void setBufferSize(int size);

	/**
	 * @return the current buffer size, 0 is this output doesn't use buffers
	 */
	int getBufferSize();
}