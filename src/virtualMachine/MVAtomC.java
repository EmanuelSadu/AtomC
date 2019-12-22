package virtualMachine;

import java.nio.ByteBuffer;
import java.util.HashMap;
import java.util.LinkedList;

import external.ExtFunc;
import external.PutI;
import virtualMachine.Instr.Opcode;

public class MVAtomC {

	private static MVAtomC instance;

	private Stack SP;
	private LinkedList<Instr> Instructions;
	private HashMap<String, Integer> globalVars;

	private MVAtomC() {
		SP = new Stack(32 * 1024);
		Instructions = new LinkedList<Instr>();
		setGlobalVars(new HashMap<String, Integer>());
	}

	public static MVAtomC getInstance() {
		if (instance == null)
			instance = new MVAtomC();
		return instance;
	}

	public void pusha(int index) {
		SP.push(index);
	}

	public int popa() {
		ByteBuffer wrapped = ByteBuffer.wrap(SP.pop(Integer.BYTES)); // big-endian by default
		int addres = wrapped.getInt();
		return addres;
	}

	public void pushd(Double d) {
		SP.push(d);
	}

	public double popd() {

		ByteBuffer wrapped = ByteBuffer.wrap(SP.pop(Double.BYTES)); // big-endian by default
		double res = wrapped.getDouble();
		return res;
	}

	public void pushi(Integer i) {
		SP.push(i);
	}

	public int popi() {
		ByteBuffer wrapped = ByteBuffer.wrap(SP.pop(Integer.BYTES)); // big-endian by default
		int res = wrapped.getInt();
		return res;
	}

	public void pushc(Character c) {
		SP.push(c);
	}

	public char popc() {
		ByteBuffer wrapped = ByteBuffer.wrap(SP.pop(Character.BYTES)); // big-endian by default
		char res = wrapped.getChar();
		return res;
	}

	public void allocGlobal(int size, String name) {
		SP.pushGlobal(size, name);
	}

	private void replaceByes(int addres, byte[] val) {
		SP.replaceByes(addres, val);

	}

	public Instr createInstr(Instr.Opcode opcode) {
		Instr instr = new Instr(opcode);
		return instr;
	}

	public void insertInstrAfter(Instr after, Instr i) {

	}

	public void addInstr(Instr.Opcode opcode) {
		Instr i = new Instr(opcode);
		Instructions.add(i);
		i.indexNextInstr = Instructions.size();
	}

	private void addInstrExternal(Opcode callext, ExtFunc extFunc) {
		Instr i = new Instr(callext);
		Instructions.add(i);
		i.extFunc = extFunc;
		i.indexNextInstr = Instructions.size();
	}

	public void addInstrAfter(Instr after, Instr.Opcode opcode) {
		Instr i = createInstr(opcode);
		insertInstrAfter(after, i);
	}

	public Instr addInstrA(Instr.Opcode opcode, int address) {
		Instr i = createInstr(opcode);
		i.val1.addres = address;
		Instructions.add(i);
		i.indexNextInstr = Instructions.size();
		return i;
	}

	public void addInstrInstrA(Instr.Opcode opcode, Instr address) {
		Instr i = createInstr(opcode);
		i.val1.instrAddres = address;
		Instructions.add(i);
		i.indexNextInstr = Instructions.size();
	}

	public void addInstrI(Instr.Opcode opcode, int val) {
		Instr i = createInstr(opcode);
		i.val1.i = val;
		Instructions.add(i);
		i.indexNextInstr = Instructions.size();
	}

	public void addInstrII(Instr.Opcode opcode, int val1, int val2) {
		Instr i = createInstr(opcode);
		i.val1.i = val1;
		i.val2.i = val2;
		Instructions.add(i);
		i.indexNextInstr = Instructions.size();
	}

	// Internals;
	private char cVal1, cVal2;
	private int iVal1, iVal2;
	private double dVal1, dVal2;
	private int addres1, addres2;
	private Instr instrAdress;
	private Instr FP;

	void mvTest() {
		Instr L1;
		allocGlobal(Long.BYTES, "a");
		allocGlobal(Integer.BYTES, "v");
		addInstrA(Opcode.PUSHCT_A, getGlobalVars().get("v"));
		addInstrI(Opcode.PUSHCT_I, 3);
		addInstrI(Opcode.STORE, Integer.BYTES);
		L1 = addInstrA(Opcode.PUSHCT_A, getGlobalVars().get("v"));
		addInstrI(Opcode.LOAD, Integer.BYTES);
		addInstrExternal(Opcode.CALLEXT, new PutI());
		addInstrA(Opcode.PUSHCT_A, getGlobalVars().get("v"));
		addInstrA(Opcode.PUSHCT_A, getGlobalVars().get("v"));
		addInstrI(Opcode.LOAD, Integer.BYTES);
		addInstrI(Opcode.PUSHCT_I, 1);
		addInstr(Opcode.SUB_I);
		addInstrI(Opcode.STORE, Integer.BYTES);
		addInstrA(Opcode.PUSHCT_A, getGlobalVars().get("v"));
		addInstrI(Opcode.LOAD, Integer.BYTES);
		addInstrInstrA(Opcode.JT_I, L1);
		addInstr(Opcode.HALT);

	}

	public void run() {

		int instrIndex = 0;
		Instr IP = Instructions.get(instrIndex++);

		while (true) {

			// System.out.printf("%p/%d\t",IP,SP-stack);
			switch (IP.opcode) {

			// ADD
			case ADD_D:
				dVal1 = popd();
				dVal2 = popd();
				System.out.printf("ADD_D\t(%g-%g . %g)\n", dVal2, dVal1, dVal2 + dVal1);
				pushd(dVal2 + dVal1);
				IP = Instructions.get(instrIndex++);
				break;
			case ADD_I:
				iVal1 = popi();
				iVal2 = popi();
				System.out.printf("ADD_I\t(%g-%g . %g)\n", iVal2, iVal1, iVal2 - iVal1);
				pushi(iVal2 + iVal1);
				IP = Instructions.get(instrIndex++);
				break;
			case ADD_C:
				cVal1 = popc();
				cVal2 = popc();
				System.out.printf("ADD_C\t(%g-%g . %g)\n", cVal2, cVal1, cVal2 + cVal1);
				pushi(cVal2 + cVal1);
				IP = Instructions.get(instrIndex++);
				break;

			// AND
			case AND_D:
				dVal1 = popd();
				dVal2 = popd();
				// System.out.printf("AND_D\t(%g-%g . %g)\n", dVal2, dVal1, (dVal2 & dVal1));
				// pushd((dVal2 & dVal1));
				IP = Instructions.get(instrIndex++);
				break;
			case AND_I:
				iVal1 = popi();
				iVal2 = popi();
				System.out.printf("AND_I\t(%g-%g . %g)\n", iVal2, iVal1, iVal2 & iVal1);
				pushi(iVal2 & iVal1);
				IP = Instructions.get(instrIndex++);
				break;
			case AND_C:
				cVal1 = popc();
				cVal2 = popc();
				System.out.printf("AND_C\t(%g-%g . %g)\n", cVal2, cVal1, cVal2 & cVal1);
				pushi(cVal2 & cVal1);
				IP = Instructions.get(instrIndex++);
				break;
			case AND_A:
				addres1 = popa();
				addres2 = popa();
				System.out.printf("AND_A\t(%g-%g . %g)\n", addres2, addres1, addres2 & addres1);
				pushi(addres1 & addres2);
				IP = Instructions.get(instrIndex++);
				break;

			case CALL:
				instrAdress = IP.val1.instrAddres;
				System.out.printf("CALL\t%p\n", instrAdress.opcode);
				pusha(instrIndex++);
				IP = instrAdress;
				break;

			case CALLEXT:
				System.out.printf("CALLEXT\t%d\n", IP.val1.addres);
				IP.extFunc.run();
				// (*(void(*)())IP.val1.addr)();
				IP = Instructions.get(instrIndex++);
				break;

			// CAST
			case CAST_C_D:
				cVal1 = popc();
				dVal1 = (double) cVal1;
				System.out.printf("CAST_C_D\t(%d . %g)\n", cVal1, dVal1);
				pushd(dVal1);
				IP = Instructions.get(instrIndex++);
				break;
			case CAST_C_I:
				cVal1 = popc();
				iVal1 = (int) cVal1;
				System.out.printf("CAST_C_I\t(%d . %g)\n", cVal1, iVal1);
				pushi(iVal1);
				IP = Instructions.get(instrIndex++);
				break;
			case CAST_D_C:
				dVal1 = popd();
				cVal1 = (char) dVal1;
				System.out.printf("CAST_D_C\t(%d . %g)\n", dVal1, cVal1);
				pushc(cVal1);
				IP = Instructions.get(instrIndex++);
				break;
			case CAST_D_I:
				dVal1 = popd();
				iVal1 = (int) dVal1;
				System.out.printf("CAST_D_I\t(%d . %g)\n", dVal1, iVal1);
				pushi(iVal1);
				IP = Instructions.get(instrIndex++);
				break;
			case CAST_I_C:
				iVal1 = popi();
				cVal1 = (char) iVal1;
				System.out.printf("CAST_I_C\t(%d . %g)\n", iVal1, dVal1);
				pushc(cVal1);
				IP = Instructions.get(instrIndex++);
				break;
			case CAST_I_D:
				iVal1 = popi();
				dVal1 = (double) iVal1;
				System.out.printf("CAST_I_D\t(%d . %g)\n", iVal1, dVal1);
				pushd(dVal1);
				IP = Instructions.get(instrIndex++);
				break;

			case DIV_D:
				dVal1 = popd();
				dVal2 = popd();
				System.out.printf("DIV_D\t(%g-%g . %g)\n", dVal2, dVal1, dVal2 / dVal1);
				pushd(dVal2 / dVal1);
				IP = Instructions.get(instrIndex++);
				break;

			case DIV_I:
				iVal1 = popi();
				iVal2 = popi();
				System.out.printf("DIV_I\t(%g-%g . %g)\n", iVal2, iVal1, iVal2 / iVal1);
				pushi(iVal2 / iVal1);
				IP = Instructions.get(instrIndex++);
				break;
			case DIV_C:
				cVal1 = popc();
				cVal2 = popc();
				System.out.printf("DIV_C\t(%g-%g . %g)\n", cVal2, cVal1, cVal2 / cVal1);
				pushi(cVal2 / cVal1);
				IP = Instructions.get(instrIndex++);
				break;

			case DROP:
				iVal1 = IP.val1.i;
				System.out.printf("DROP\t%d\n", iVal1);
				SP.pop(iVal1);
				IP = Instructions.get(instrIndex++);
				break;

			case ENTER:
				iVal1 = IP.val1.i;
				System.out.printf("ENTER\t%d\n", iVal1);
				// pusha(FP);
				// FP = SP;
				// SP += iVal1;
				IP = Instructions.get(instrIndex++);
				break;

			case EQ_A:
				addres1 = popa();
				addres2 = popa();
				System.out.printf("EQ_A\t(%g==%g . %d)\n", addres2, addres1, addres2 == addres1);
				if (addres1 == addres2)
					pushi(1);
				else
					pushi(0);
				IP = Instructions.get(instrIndex++);
				break;

			case EQ_D:
				dVal1 = popd();
				dVal2 = popd();
				System.out.printf("EQ_D\t(%g==%g . %d)\n", dVal2, dVal1, dVal2 == dVal1);
				if (dVal1 == dVal2)
					pushi(1);
				else
					pushi(0);
				IP = Instructions.get(instrIndex++);
				break;

			case EQ_I:
				iVal1 = popi();
				iVal2 = popi();
				System.out.printf("EQ_I\t(%g==%g . %d)\n", iVal2, iVal1, iVal2 == iVal1);
				if (iVal1 == iVal2)
					pushi(1);
				else
					pushi(0);
				IP = Instructions.get(instrIndex++);
				break;

			case EQ_C:
				cVal1 = popc();
				cVal2 = popc();
				System.out.printf("EQ_C\t(%g==%g . %d)\n", cVal2, cVal1, cVal2 == cVal1);
				if (cVal1 == cVal2)
					pushi(1);
				else
					pushi(0);
				IP = Instructions.get(instrIndex++);
				break;

			case GREATER_D:
				dVal1 = popd();
				dVal2 = popd();
				System.out.printf("GREATER_D\t(%g==%g . %d)\n", dVal2, dVal1, dVal2 < dVal1);
				if (dVal1 > dVal2)
					pushi(1);
				else
					pushi(0);
				IP = Instructions.get(instrIndex++);
				break;

			case GREATER_I:
				iVal1 = popi();
				iVal2 = popi();
				System.out.printf("GREATER_I\t(%g==%g . %d)\n", iVal2, iVal1, iVal2 < iVal1);
				if (iVal1 > iVal2)
					pushi(1);
				else
					pushi(0);
				IP = Instructions.get(instrIndex++);
				break;

			case GREATER_C:
				cVal1 = popc();
				cVal2 = popc();
				System.out.printf("GREATER_C\t(%g==%g . %d)\n", cVal2, cVal1, cVal2 < cVal1);
				if (cVal1 > cVal2)
					pushi(1);
				else
					pushi(0);
				IP = Instructions.get(instrIndex++);
				break;

			case GREATEREQ_D:
				dVal1 = popd();
				dVal2 = popd();
				System.out.printf("GREATEREQ_D\t(%g==%g . %d)\n", dVal2, dVal1, dVal2 <= dVal1);
				if (dVal1 >= dVal2)
					pushi(1);
				else
					pushi(0);
				IP = Instructions.get(instrIndex++);
				break;

			case GREATEREQ_I:
				iVal1 = popi();
				iVal2 = popi();
				System.out.printf("GREATEREQ_I\t(%g==%g . %d)\n", iVal2, iVal1, iVal2 <= iVal1);
				if (iVal1 >= iVal2)
					pushi(1);
				else
					pushi(0);
				IP = Instructions.get(instrIndex++);
				break;

			case GREATEREQ_C:
				cVal1 = popc();
				cVal2 = popc();
				System.out.printf("GREATEREQ_C\t(%g==%g . %d)\n", cVal2, cVal1, cVal2 <= cVal1);
				if (cVal1 >= cVal2)
					pushi(1);
				else
					pushi(0);
				IP = Instructions.get(instrIndex++);
				break;

			case HALT:
				System.out.printf("HALT\n");
				return;

			case INSERT:
				iVal1 = IP.val1.i; // iDst iVal2=IP.args[1].i; // nBytes
				System.out.printf("INSERT\t%d,%d\n", iVal1, iVal2);
				// if (SP + iVal2 > stackAfter)
				// err("out of stack");
				// memmove(SP - iVal1 + iVal2, SP - iVal1, iVal1); // make room
				// memmove(SP - iVal1, SP + iVal2, iVal2); // dup SP+=iVal2;
				IP = Instructions.get(instrIndex++);
				break;

			case JF_A:
				addres1 = popa();
				System.out.printf("JF\t\t(%d)\n", addres1);
				if (addres1 == 0) {
					IP = IP.val1.instrAddres;
					instrIndex = IP.indexNextInstr;
				} else
					IP = Instructions.get(instrIndex++);
				break;

			case JF_I:
				iVal1 = popi();
				System.out.printf("JF\t\t(%d)\n", iVal1);
				if (iVal1 == 0) {
					IP = IP.val1.instrAddres;
					instrIndex = IP.indexNextInstr;
				} else
					IP = Instructions.get(instrIndex++);
				break;

			case JF_D:
				dVal1 = popd();
				System.out.printf("JF\t\t(%d)\n", dVal1);
				if (dVal1 == 0) {
					IP = IP.val1.instrAddres;
					instrIndex = IP.indexNextInstr;
				} else
					IP = Instructions.get(instrIndex++);
				break;

			case JF_C:
				cVal1 = popc();
				System.out.printf("JF\t\t(%d)\n", cVal1);
				if (cVal1 == 0) {
					IP = IP.val1.instrAddres;
					instrIndex = IP.indexNextInstr;
				} else
					IP = Instructions.get(instrIndex++);
				break;

			case JMP:
				IP = IP.val1.instrAddres;
				instrIndex = IP.indexNextInstr;
				break;

			case JT_A:
				addres1 = popa();
				System.out.printf("JT\t\t(%d)\n", addres1);
				if (addres1 != 0) {
					IP = IP.val1.instrAddres;
					instrIndex = IP.indexNextInstr;
				} else
					IP = Instructions.get(instrIndex++);
				break;

			case JT_I:
				iVal1 = popi();
				System.out.printf("JT\t\t(%d)\n", iVal1);
				if (iVal1 != 0) {
					IP = IP.val1.instrAddres;
					instrIndex = IP.indexNextInstr;
				} else
					IP = Instructions.get(instrIndex++);
				break;

			case JT_D:
				dVal1 = popd();
				System.out.printf("JT\t\t(%d)\n", dVal1);
				if (dVal1 != 0) {
					IP = IP.val1.instrAddres;
					instrIndex = IP.indexNextInstr;
				} else
					IP = Instructions.get(instrIndex++);
				break;

			case JT_C:
				cVal1 = popc();
				System.out.printf("JT\t\t(%d)\n", cVal1);
				if (cVal1 != 0) {
					IP = IP.val1.instrAddres;
					instrIndex = IP.indexNextInstr;
				} else
					IP = Instructions.get(instrIndex++);
				break;

			case LESS_D:
				dVal1 = popd();
				dVal2 = popd();
				System.out.printf("LESS_D\t(%g==%g . %d)\n", dVal2, dVal1, dVal2 > dVal1);
				if (dVal1 < dVal2)
					pushi(1);
				else
					pushi(0);
				IP = Instructions.get(instrIndex++);
				break;

			case LESS_I:
				iVal1 = popi();
				iVal2 = popi();
				System.out.printf("LESS_I\t(%g==%g . %d)\n", iVal2, iVal1, iVal2 > iVal1);
				if (iVal1 < iVal2)
					pushi(1);
				else
					pushi(0);
				IP = Instructions.get(instrIndex++);
				break;

			case LESS_C:
				cVal1 = popc();
				cVal2 = popc();
				System.out.printf("LESS_C\t(%g==%g . %d)\n", cVal2, cVal1, cVal2 > cVal1);
				if (cVal1 < cVal2)
					pushi(1);
				else
					pushi(0);
				IP = Instructions.get(instrIndex++);
				break;

			case LESSEQ_D:
				dVal1 = popd();
				dVal2 = popd();
				System.out.printf("LESSEQ_D\t(%g==%g . %d)\n", dVal2, dVal1, dVal2 >= dVal1);
				if (dVal1 <= dVal2)
					pushi(1);
				else
					pushi(0);
				IP = Instructions.get(instrIndex++);
				break;

			case LESSEQ_I:
				iVal1 = popi();
				iVal2 = popi();
				System.out.printf("LESSEQ_I\t(%g==%g . %d)\n", iVal2, iVal1, iVal2 >= iVal1);
				if (iVal1 <= iVal2)
					pushi(1);
				else
					pushi(0);
				IP = Instructions.get(instrIndex++);
				break;

			case LESSEQ_C:
				cVal1 = popc();
				cVal2 = popc();
				System.out.printf("LESSEQ_C\t(%g==%g . %d)\n", cVal2, cVal1, cVal2 >= cVal1);
				if (cVal1 <= cVal2)
					pushi(1);
				else
					pushi(0);
				IP = Instructions.get(instrIndex++);
				break;

			case LOAD:
				iVal1 = IP.val1.i; // nr of bytes
				addres1 = popa();
				System.out.printf("LOAD\t%d\t(%d)\n", iVal1, addres1);
				SP.push(SP.readBytes(addres1, iVal1));
				// memcpy(SP, addres, iVal1);
				// SP += iVal1;
				IP = Instructions.get(instrIndex++);
				break;

			case MUL_D:
				dVal1 = popd();
				dVal2 = popd();
				System.out.printf("MUL_D\t(%g-%g . %g)\n", dVal2, dVal1, dVal2 * dVal1);
				pushd(dVal2 * dVal1);
				IP = Instructions.get(instrIndex++);
				break;

			case MUL_I:
				iVal1 = popi();
				iVal2 = popi();
				System.out.printf("MUL_I\t(%g-%g . %g)\n", iVal2, iVal1, iVal2 * iVal1);
				pushi(iVal2 * iVal1);
				IP = Instructions.get(instrIndex++);
				break;
			case MUL_C:
				cVal1 = popc();
				cVal2 = popc();
				System.out.printf("MUL_C\t(%g-%g . %g)\n", cVal2, cVal1, cVal2 * cVal1);
				pushi(cVal2 * cVal1);
				IP = Instructions.get(instrIndex++);
				break;

			case OFFSET:
				iVal1 = popi();
				addres1 = popa();
				System.out.printf("OFFSET\t(%p+%d . %p)\n", addres1, iVal1, addres1 + iVal1);
				pusha(addres1 + iVal1);
				IP = Instructions.get(instrIndex++);
				break;

			case PUSHFPADDR:
				iVal1 = IP.val1.i;
				// System.out.printf("PUSHFPADDR\t%d\t(%p)\n", iVal1, FP + iVal1);
				// pusha(FP + iVal1);
				IP = Instructions.get(instrIndex++);
				break;

			case PUSHCT_A:
				addres1 = IP.val1.addres;
				System.out.printf("PUSHCT_A\t%d\n", addres1);
				pusha(addres1);
				IP = Instructions.get(instrIndex++);
				break;

			case PUSHCT_I:
				iVal1 = IP.val1.i;
				System.out.printf("PUSHCT_I\t%d\n", iVal1);
				pushi(iVal1);
				IP = Instructions.get(instrIndex++);
				break;

			case PUSHCT_D:
				dVal1 = IP.val1.d;
				System.out.printf("PUSHCT_D\t%d\n", iVal1);
				pushd(dVal1);
				IP = Instructions.get(instrIndex++);
				break;

			case PUSHCT_C:
				iVal1 = IP.val1.i;
				System.out.printf("PUSHCT_I\t%p\n", iVal1);
				pusha(iVal1);
				IP = Instructions.get(instrIndex++);
				break;
			/*
			 * case RET: iVal1=IP.val1.i; // sizeArgs iVal2=IP.args[1].i; // sizeof(retType)
			 * System.out.printf("RET\t%d,%d\n",iVal1,iVal2); oldSP=SP; SP=FP; FP=popa();
			 * IP=popa(); if(SP-iVal1<stack)err("not enough stack bytes"); SP-=iVal1;
			 * memmove(SP,oldSP-iVal2,iVal2); SP+=iVal2; break;
			 */

			case STORE:
				iVal1 = IP.val1.i;
				byte[] val = SP.pop(IP.val1.i);
				addres1 = popa();

				System.out.printf("STORE\t%d\t(%d)\n", iVal1, addres1);

				replaceByes(addres1, val);
				IP = Instructions.get(instrIndex++);
				break;

			case SUB_D:
				dVal1 = popd();
				dVal2 = popd();
				System.out.printf("SUB_D\t(%g-%g . %g)\n", dVal2, dVal1, dVal2 - dVal1);
				pushd(dVal2 - dVal1);
				IP = Instructions.get(instrIndex++);
				break;
			case SUB_I:
				iVal1 = popi();
				iVal2 = popi();
				System.out.printf("SUB_I\t(%d-%d . %d)\n", iVal2, iVal1, iVal2 - iVal1);
				pushi(iVal2 - iVal1);
				IP = Instructions.get(instrIndex++);
				break;
			case SUB_C:
				iVal1 = popi();
				iVal2 = popi();
				System.out.printf("SUB_C\t(%d-%d . %d)\n", iVal2, iVal1, iVal2 - iVal1);
				pushi(iVal2 - iVal1);
				IP = Instructions.get(instrIndex++);
				break;
			default:
				// err("invalid opcode: %d", IP.opcode);

			}

		}
	}

	public static void main(String[] args) {
		MVAtomC.getInstance();
		MVAtomC.getInstance().mvTest();
		MVAtomC.getInstance().run();
	}

	static byte[] reverse(byte[] array) {
		for (int i = 0; i < array.length / 2; i++) {
			byte temp = array[i];
			array[i] = array[array.length - i - 1];
			array[array.length - i - 1] = temp;
		}
		return array;

	}

	public HashMap<String, Integer> getGlobalVars() {
		return globalVars;
	}

	public void setGlobalVars(HashMap<String, Integer> globalVars) {
		this.globalVars = globalVars;
	}
}
