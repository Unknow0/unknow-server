/**
 * 
 */
package unknow.server.jaxb.handler;

/**
 * @author unknow
 */
public class IntHandler extends XmlDefaultHandler<Integer> {

	public static final IntHandler INSTANCE = new IntHandler();

	private IntHandler() {
	}

	@Override
	public String toString(Integer t) {
		return Integer.toString(t);
	}

	@Override
	public Integer toObject(String s) {
		return Integer.parseInt(s);
	}
}
