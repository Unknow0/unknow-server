package unknow.server.protobuf;

/**
 * 
 */

import java.io.IOException;

import javax.net.ssl.SSLEngine;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import io.protostuff.CodedInput;
import io.protostuff.LimitedInputStream;
import io.protostuff.LinkedBuffer;
import io.protostuff.Message;
import io.protostuff.Output;
import io.protostuff.ProtobufOutput;
import io.protostuff.ProtostuffOutput;
import io.protostuff.Schema;
import io.protostuff.WriteSession;
import unknow.server.nio.NIOConnectionAbstract;
import unknow.server.nio.NIOConnectionAbstract.Out;
import unknow.server.nio.NIOConnectionHandler;
import unknow.server.util.io.Buffers;

/**
 * @author unknow
 */
public abstract class ProtoStuffConnection<T> implements NIOConnectionHandler {
	private static final Logger logger = LoggerFactory.getLogger(ProtoStuffConnection.class);

	private final Schema<T> schema;
	private final boolean protostuff;

	private Out out;
	private LimitedInputStream lis;
	private CodedInput input;

	protected ProtoStuffConnection(Schema<T> schema, boolean protostuff) {
		this.schema = schema;
		this.protostuff = protostuff;
	}

	@Override
	public final void onInit(NIOConnectionAbstract co, SSLEngine ssl) {
		out = co.getOut();
		lis = new LimitedInputStream(co.getIn(), Integer.MAX_VALUE);
		input = new CodedInput(lis, protostuff);
	}

	protected final <M extends Message<M>> void write(T o) throws IOException {
		LinkedBuffer buffer = LinkedBuffer.allocate();
		Output output = protostuff ? new ProtobufOutput(buffer) : new ProtostuffOutput(buffer);
		schema.writeTo(output, o);
		int size = ((WriteSession) output).getSize();
		ProtobufOutput.writeRawVarInt32Bytes(out, size);
		LinkedBuffer.writeTo(out, buffer);
		out.flush();
	}

	protected final <M extends Message<M>> void writeMessage(M o) throws IOException {
		LinkedBuffer buffer = LinkedBuffer.allocate();
		Output output = protostuff ? new ProtobufOutput(buffer) : new ProtostuffOutput(buffer);
		o.cachedSchema().writeTo(output, o);
		int size = ((WriteSession) output).getSize();
		ProtobufOutput.writeRawVarInt32Bytes(out, size);
		LinkedBuffer.writeTo(out, buffer);
		out.flush();
	}

	@Override
	public final void onWrite() throws InterruptedException, IOException { // ok
	}

	@Override
	public final void onRead(Buffers b) {
		lis.mark(48);
		try {
			int size = input.readUInt32();
			if (b.length() < size) {
				lis.reset();
				return;
			}

			T m = schema.newMessage();
			schema.mergeFrom(input, m);
			process(m);
		} catch (IOException e) {
			logger.warn("", e);
			out.close();
		}
	}

	@Override
	public final void onHandshakeDone(SSLEngine sslEngine) throws InterruptedException { // ok
	}

	@Override
	public final void onFree() throws IOException { // ok
	}

	protected abstract void process(T t) throws IOException;
}