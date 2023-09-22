/**
 * 
 */
package unknow.server.jaxb.handler;

/**
 * @author unknow
 */
public final class StringHandler extends XmlDefaultHandler<String> {

	public static final StringHandler INSTANCE = new StringHandler();

	private StringHandler() {
	}

	@Override
	public final String toString(String t) {
		return t;
	}

	@Override
	public final String toObject(String s) {
		return s;
	}
}
