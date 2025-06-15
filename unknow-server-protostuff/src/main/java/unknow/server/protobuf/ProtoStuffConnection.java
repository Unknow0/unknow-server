package unknow.server.protobuf;

/**
 * 
 */

import java.io.IOException;
import java.nio.ByteBuffer;

import javax.net.ssl.SSLEngine;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import io.protostuff.CodedInput;
import io.protostuff.LinkedBuffer;
import io.protostuff.Message;
import io.protostuff.Output;
import io.protostuff.ProtobufOutput;
import io.protostuff.ProtostuffOutput;
import io.protostuff.Schema;
import io.protostuff.WriteSession;
import unknow.server.nio.NIOConnection;
import unknow.server.nio.NIOConnection.Out;
import unknow.server.nio.NIOConnectionHandler;
import unknow.server.util.io.ByteBufferInputStream;
import unknow.server.util.io.LimitedInputStream;

/**
 * @author unknow
 */
public abstract class ProtoStuffConnection<T> implements NIOConnectionHandler {
	private static final Logger logger = LoggerFactory.getLogger(ProtoStuffConnection.class);

	private final Schema<T> schema;
	private final boolean protostuff;

	private Out out;
	private ByteBufferInputStream in;
	private LimitedInputStream lis;
	private CodedInput input;

	protected ProtoStuffConnection(Schema<T> schema, boolean protostuff) {
		this.schema = schema;
		this.protostuff = protostuff;
		this.in = new ByteBufferInputStream();
	}

	@Override
	public final void init(NIOConnection co, long now, SSLEngine ssl) {
		out = co.getOut();
		lis = new LimitedInputStream(in);
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
	public final void onRead(ByteBuffer b, long now) throws IOException {
		in.addBuffer(b);
		lis.mark(48);
		try {
			int size = input.readUInt32();
			if (b.remaining() < size) {
				lis.reset();
				return;
			}

			lis.setLimit(size);
			T m = schema.newMessage();
			schema.mergeFrom(input, m);
			process(m);
		} catch (IOException e) {
			logger.warn("", e);
			out.close();
		}
	}

	protected abstract void process(T t) throws IOException;
}