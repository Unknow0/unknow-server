package unknow.server.protobuf;

/**
 * 
 */

import java.io.IOException;
import java.nio.channels.SelectionKey;

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
import unknow.server.nio.NIOConnection;

/**
 * @author unknow
 */
public abstract class ProtoStuffConnection<T> extends NIOConnection {
	private static final Logger logger = LoggerFactory.getLogger(ProtoStuffConnection.class);

	private final Schema<T> schema;
	private final boolean protostuff;

	private LimitedInputStream lis;
	private CodedInput input;

	protected ProtoStuffConnection(SelectionKey key, Schema<T> schema, boolean protostuff) {
		super(key);
		this.schema = schema;
		this.protostuff = protostuff;
	}

	@Override
	protected final void onInit() {
		lis = new LimitedInputStream(getIn(), Integer.MAX_VALUE);
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
	public final void onRead() {
		lis.mark(48);
		try {
			int size = input.readUInt32();
			if (pendingRead().length() < size) {
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

	protected abstract void process(T t) throws IOException;
}