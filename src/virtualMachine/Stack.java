package virtualMachine;

import java.nio.ByteBuffer;
import java.util.NoSuchElementException;

public class Stack {
	private final ByteBuffer buf;

	public Stack(int capacity) {
		buf = ByteBuffer.allocate(capacity);
	}

	public void pushGlobal(int size, String name) {
		MVAtomC.getInstance().getGlobalVars().put(name, buf.position());
		for (int i = 0; i < size; i++) {
			buf.position(buf.position() + 1);
		}

	}

	public void push(int i) {
		buf.putInt(i);
	}

	public void push(double d) {
		buf.putDouble(d);
	}

	public void push(char c) {
		buf.putChar(c);
	}

	public byte[] pop(int size) {
		byte[] bytes = new byte[size];
		for (int i = 0; i < size; i++) {
			if (buf.position() == 0) {
				throw new NoSuchElementException();
			}
			buf.position(buf.position() - 1);
			bytes[i] = buf.get(buf.position());
		}
		return MVAtomC.reverse(bytes);
	}

	public void replaceByes(int addres, byte[] val) {
		for (int i = 0; i < val.length; i++) {
			buf.put(addres++, val[i]);
		}

	}

	public byte[] readBytes(int addres, int nr) {
		byte[] val = new byte[nr];
		for (int i = 0; i < val.length; i++) {
			val[i] = buf.get(addres++);
		}
		return val;

	}

	public void push(byte[] readBytes) {
		for (int i = 0; i < readBytes.length; i++) {
			buf.put(readBytes[i]);
		}

	}

}
