package tabelaS;

import java.util.HashMap;

import lexical.Atom.Iduri;

public class SymbolTable {
	public enum EnumType {TB_INT,TB_DOUBLE,TB_CHAR,TB_STRUCT,TB_VOID};
	public enum Clas {CLS_VAR,CLS_FUNC,CLS_EXTFUNC,CLS_STRUCT};
	public enum Mem {MEM_GLOBAL,MEM_ARG,MEM_LOCAL};
	private HashMap<String,AtomAttribute> symbols;
	
	public SymbolTable() {
		symbols = new HashMap<String, SymbolTable.AtomAttribute>();
	}
	
	public AtomAttribute findSymbol(String token) {
		return symbols.get(token);
	}
	
	public static class AtomAttribute{
		public String name;
		public Clas cls;
		public Mem mem;
		public Type type;
		public int depth;
		public Args args;
		public Members members;
		
	}
	
	public static class Type {
		
		public EnumType typeBase;
		public AtomAttribute s;
		public int nrElements;
		
		public void determineTypeBase(Iduri currentAtomType) {
			switch(currentAtomType) {
				case INT: typeBase = EnumType.TB_INT;break;
				case DOUBLE: typeBase = EnumType.TB_DOUBLE;break;
				case CHAR: typeBase = EnumType.TB_CHAR;break;
				case STRUCT: typeBase = EnumType.TB_STRUCT;break;
				case VOID: typeBase = EnumType.TB_VOID;break;
				default:
					break;
			}
			
		}

		public void determineTypeBase(Iduri currentAtomType, AtomAttribute s2) {
			switch(currentAtomType) {
				case INT: typeBase = EnumType.TB_INT;break;
				case DOUBLE: typeBase = EnumType.TB_DOUBLE;break;
				case CHAR: typeBase = EnumType.TB_CHAR;break;
				case STRUCT: typeBase = EnumType.TB_STRUCT;break;
				case VOID: typeBase = EnumType.TB_VOID;break;
				default:
					break;
			}
		
			s=s2;
		}
	}
	
	private class Args {
		
		
		
	}
	
	private class Members{
		
		
		
	}
	
	
}
