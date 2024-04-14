package unknow.server.protobuf;

/**
 * 
 */

import java.io.IOException;

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

	protected Out out;
	private LimitedInputStream in;
	private CodedInput input;

	protected ProtoStuffConnection(Schema<T> schema, boolean protostuff) {
		this.schema = schema;
		this.protostuff = protostuff;
	}

	@Override
	protected final void onInit() {
		out = getOut();
		in = new LimitedInputStream(getIn(), Integer.MAX_VALUE);
		input = new CodedInput(in, protostuff);
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
		in.mark(48);
		try {
			int size = input.readUInt32();
			if (pendingRead.length() < size) {
				in.reset();
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