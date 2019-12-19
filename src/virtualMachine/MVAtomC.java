package virtualMachine;

import java.nio.ByteBuffer;

public class MVAtomC {

	private static MVAtomC instance;

	private ByteBuffer SP;
	private char[] globals;
	private int nGlobals;

	private MVAtomC() {
		SP = ByteBuffer.allocate(32 * 1024);
	}

	public static MVAtomC getInstance() {
		if (instance == null)
			instance = new MVAtomC();
		return instance;
	}

	public void pusha() {

	}

	public void popa() {

	}

	public void pushd(Double d) {
		if (SP.remaining() + Double.BYTES > SP.capacity()) {
			throw new RuntimeException("out of stack");
		}
		SP.putDouble(d);
	}

	public double popd() {
		return SP.getDouble();
	}

	public void pushi(Integer i) {

	}

	public double popi() {
		return 1;
	}

	public char allocGlobal(int size) {
		char p = ' ';
		// char p = globals + nGlobals;
		// if (nGlobals + size > GLOBAL_SIZE)
		// err("insufficient globals space");
		nGlobals += size;
		return p;
	}

	public Instr createInstr(Instr.Opcode opcode) {
		Instr instr = new Instr(opcode);
		return instr;
	}

}
