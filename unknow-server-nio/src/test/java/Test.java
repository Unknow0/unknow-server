//import java.io.EOFException;
//import java.net.InetSocketAddress;
//import java.nio.ByteBuffer;
//import java.nio.channels.ServerSocketChannel;
//import java.nio.channels.SocketChannel;
//import java.nio.file.Files;
//import java.nio.file.Paths;
//import java.security.KeyStore;
//import java.util.Arrays;
//import java.util.List;
//
//import javax.net.ssl.KeyManagerFactory;
//import javax.net.ssl.SSLContext;
//import javax.net.ssl.SSLEngine;
//import javax.net.ssl.SSLEngineResult;
//import javax.net.ssl.SSLEngineResult.HandshakeStatus;
//import javax.net.ssl.SSLEngineResult.Status;
//import javax.net.ssl.SSLParameters;
//import javax.net.ssl.TrustManagerFactory;
//
//import unknow.server.nio.NIOConnectionSSL;
//import unknow.server.nio.NIOServer;
//import unknow.server.nio.NIOServerListener;
//import unknow.server.nio.NIOWorker;
//import unknow.server.nio.NIOWorkers;
//
//public class Test {
//
//	public static void main(String[] arg) throws Exception {
//		KeyStore ks = KeyStore.getInstance("JKS");
//		ks.load(Files.newInputStream(Paths.get("../store.jks")), null);
//
//		KeyManagerFactory manager = KeyManagerFactory.getInstance("SunX509");
//		manager.init(ks, "123456".toCharArray());
//
//		TrustManagerFactory trust = TrustManagerFactory.getInstance("SunX509");
//		trust.init(ks);
//
//		SSLContext sslContext = SSLContext.getInstance("TLS");
//		sslContext.init(manager.getKeyManagers(), trust.getTrustManagers(), null);
//
//		NIOWorkers workers = new NIOWorker(0, NIOServerListener.NOP, 100);
//		NIOServer nioServer = new NIOServer(workers, NIOServerListener.NOP);
//		nioServer.bind(new InetSocketAddress("0.0.0.0", 54321), key -> new NIOConnectionSSL(key, sslContext));
//		nioServer.start();
//		nioServer.await();
//
//		ServerSocketChannel open = ServerSocketChannel.open();
//		open.configureBlocking(true);
//		open.bind(new InetSocketAddress("0.0.0.0", 54321));
//
//		SocketChannel accept = open.accept();
//		accept.configureBlocking(false);
//
//		InetSocketAddress a = (InetSocketAddress) accept.getRemoteAddress();
//		SSLEngine sslEngine = sslContext.createSSLEngine(a.getHostString(), a.getPort());
//		sslEngine.setUseClientMode(false);
////		sslEngine.setNeedClientAuth(true);
//		SSLParameters params = new SSLParameters();
//		params.setApplicationProtocols(new String[] { "h2" });
//		sslEngine.setSSLParameters(params);
//
//		ByteBuffer app = ByteBuffer.allocate(16704);
//		ByteBuffer rawIn = ByteBuffer.allocate(16709);
//		ByteBuffer rawOut = ByteBuffer.allocate(16709);
//		sslEngine.beginHandshake();
//		System.out.println(sslEngine.getSession().getApplicationBufferSize());
//		System.out.println(sslEngine.getSession().getPacketBufferSize());
//		HandshakeStatus hs = sslEngine.getHandshakeStatus();
//		while (hs != HandshakeStatus.FINISHED && hs != HandshakeStatus.NOT_HANDSHAKING) {
//			System.out.println("> " + hs);
//			SSLEngineResult r;
//			if (hs == HandshakeStatus.NEED_WRAP) {
//				r = sslEngine.wrap(app, rawOut);
//				System.out.println(">> " + r.getStatus());
//				rawOut.flip();
//				if (r.getStatus() == Status.BUFFER_OVERFLOW) {
//					ByteBuffer b = ByteBuffer.allocate(rawOut.capacity() << 2);
//					rawOut = b.put(rawOut);
//					continue;
//				}
//				// TODO check status
//				accept.write(rawOut);
//				rawOut.compact();
//				hs = r.getHandshakeStatus();
//			} else if (hs == HandshakeStatus.NEED_UNWRAP /*|| r.getHandshakeStatus() == HandshakeStatus.NEED_UNWRAP_AGAIN*/) {
//				do {
//					if (accept.read(rawIn) < 0)
//						throw new EOFException();
//					rawIn.flip();
//					r = sslEngine.unwrap(rawIn, app);
//					System.out.println(">> " + r.getStatus());
//					rawIn.compact();
//				} while (r.getStatus() == Status.BUFFER_UNDERFLOW);
//				// TODO check status
//				hs = r.getHandshakeStatus();
//			} else if (hs == HandshakeStatus.NEED_TASK) {
//				Runnable task;
//				while ((task = sslEngine.getDelegatedTask()) != null)
//					task.run();
//				System.out.println(">> done");
//				hs = sslEngine.getHandshakeStatus();
//			}
//		}
//
//		System.out.println("ALPN: " + sslEngine.getApplicationProtocol());
//
//		app.put("test".getBytes());
//		app.flip();
//		SSLEngineResult r = sslEngine.wrap(app, rawOut);
//		rawOut.flip();
//		System.out.println(r);
//		System.out.println(rawOut);
//		while (rawOut.remaining() > 0)
//			accept.write(rawOut);
//		app.clear();
//		sslEngine.closeOutbound();
//
//		int l;
//		while ((l = accept.read(rawIn)) >= 0) {
//			if (l == 0)
//				continue;
//
//			rawIn.flip();
//			System.out.println(rawIn);
//			r = sslEngine.unwrap(rawIn, app);
//			System.out.println(r);
//			rawIn.compact();
//		}
//		System.out.println(new String(app.array(), 0, app.position()));
//	}
//}
