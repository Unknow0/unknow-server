package unknow.server.servlet.http2;

import java.io.IOException;

public interface Http2FlowControl {

	int flowRead();

	void flowRead(int v) throws IOException;

	int flowWrite();

	void flowWrite(int v);
}
