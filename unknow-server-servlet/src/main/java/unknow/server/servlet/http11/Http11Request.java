package unknow.server.servlet.http11;

import java.io.InputStream;

import jakarta.servlet.DispatcherType;
import jakarta.servlet.ServletInputStream;
import unknow.server.servlet.impl.ServletRequestImpl;

public class Http11Request extends ServletRequestImpl {
	private final InputStream content;

	public Http11Request(Http11Processor co, DispatcherType type) {
		super(co.co(), type);
		this.content = co.content();
	}

	@Override
	protected ServletInputStream createInput() {
		String tr = getHeader("transfer-encoding");
		if ("chunked".equalsIgnoreCase(tr))
			return new ChunckedInputStream(content);
		long l = getContentLengthLong();
		if (l > 0)
			return new LengthInputStream(content, l);
		return EmptyInputStream.INSTANCE;
	}

}
