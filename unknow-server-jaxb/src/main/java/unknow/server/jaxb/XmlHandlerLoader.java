package unknow.server.jaxb;

import java.util.Collection;

public interface XmlHandlerLoader {
	/**
	 * @return the linked context path
	 */
	String contextPath();

	/**
	 * @return the mapped class
	 */
	Collection<XmlHandler<?>> handlers();

}
