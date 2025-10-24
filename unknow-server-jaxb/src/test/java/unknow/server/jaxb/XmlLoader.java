package unknow.server.jaxb;

import java.util.Arrays;
import java.util.Collection;

public class XmlLoader implements XmlHandlerLoader {

	@Override
	public String contextPath() {
		return "unknow.server.jaxb";
	}

	@Override
	public Collection<XmlHandler<?>> handlers() {
		return Arrays.asList(OHandler.INSTANCE);
	}

}
