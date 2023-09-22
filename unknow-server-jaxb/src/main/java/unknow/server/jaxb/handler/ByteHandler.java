/**
 * 
 */
package unknow.server.jaxb.handler;

/**
 * @author unknow
 */
public class ByteHandler extends XmlDefaultHandler<Byte> {

	public static final ByteHandler INSTANCE = new ByteHandler();

	private ByteHandler() {
	}

	@Override
	public String toString(Byte t) {
		return Byte.toString(t);
	}

	@Override
	public Byte toObject(String s) {
		return Byte.parseByte(s);
	}

}
