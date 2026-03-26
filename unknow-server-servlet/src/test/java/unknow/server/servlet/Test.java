package unknow.server.servlet;

import java.nio.ByteBuffer;

import unknow.server.servlet.http2.Http2Processor;

public class Test {
	public static void main(String[] arg) {
		ByteBuffer b = ByteBuffer.allocate(1024);
		b.put(Http2Processor.PRI.array());
		b.put(new byte[] { 0, 0, 0 });
		b.flip();
		System.out.println(Http2Processor.PRI.remaining());
		System.out.println(Http2Processor.PRI.mismatch(b));
	}
}
