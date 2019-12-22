package virtualMachine;

public class Instr {

	protected Instr(Opcode opcode2) {
		opcode = opcode2;
		val1 = new Register();
		val2 = new Register();

	}

	public enum Opcode {
		ADD_C, ADD_I, ADD_D, AND_A, AND_C, AND_I, AND_D, CALL, CALLEXT, CAST_C_D, CAST_C_I, CAST_D_C, CAST_D_I,
		CAST_I_C, CAST_I_D, DIC_C, DIC_I, DIV_D, DROP, ENTER, EQ_A, EQ_C, EQ_I, EQ_D, GREATER_C, GREATER_I, GREATER_D,
		GREATEREQ_C, GREATEREQ_I, GREATEREQ_D, HALT, INSERT, JF_A, JF_C, JF_I, JF_D, JMP, JT_A, JT_C, JT_I, JT_D,
		LESS_C, LESS_I, LESS_D, LESSEQ_C, LESSEQ_I, LESSEQ_D, LOAD, MUL_C, MUL_I, MUL_D, NEG_C, NEG_I, NEG_D, NOP,
		NOT_A, NOT_C, NOT_I, NOT_D, NOTEQ_A, NOTEQ_C, NOTEQ_I, NOTEQ_D, OFFSET, OR_A, OR_C, OR_I, OR_D, PUSHFPADDR,
		PUSHCT_A, PUSHCT_C, PUSHCT_I, PUSHCT_D, RET, STORE, SUB_C, SUB_I, SUB_D
	}

	public Opcode opcode;
	public Register val1;
	public Register val2;
	public int indexNextInstr;

	class Register {
		int i;
		double d;
		public Instr instrAddres;
		int addres;
	}
}
