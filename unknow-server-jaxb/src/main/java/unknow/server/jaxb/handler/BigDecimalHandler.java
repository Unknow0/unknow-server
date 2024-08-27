/**
 * 
 */
package unknow.server.jaxb.handler;

import java.math.BigDecimal;

import jakarta.xml.bind.JAXBException;
import unknow.server.jaxb.XmlSimpleHandler;

/**
 * @author unknow
 */
public class BigDecimalHandler implements XmlSimpleHandler<BigDecimal> {

	public static final BigDecimalHandler INSTANCE = new BigDecimalHandler();

	private BigDecimalHandler() {
	}

	@Override
	public String toString(BigDecimal t) {
		return t.toString();
	}

	@Override
	public BigDecimal toObject(String s) throws JAXBException {
		return new BigDecimal(s);
	}
}
