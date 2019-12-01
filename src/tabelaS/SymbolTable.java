package tabelaS;

import java.util.HashMap;

import lexical.Atom.Iduri;
import tabelaS.SymbolTable.AtomAttribute;
import tabelaS.SymbolTable.Clas;

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
	
	public void addSymbol(String token) {
	
		AtomAttribute symbol = findSymbol(token);
		if(symbol != null)
			throw new RuntimeException("Symbol redefinition");
		symbol = new AtomAttribute(token,Clas.CLS_STRUCT);
		symbols.put(token, symbol);
	}
	
	public static class AtomAttribute{
		
		public String name;
		public Clas cls;
		public Mem mem;
		public Type type;
		public int depth;
		public Args args;
		public Members members;
		
		
		public AtomAttribute(String token, Clas clsStruct) {
			this.name=token;
			this.cls=clsStruct;
		}
		
	}
	
	public static class Type {
		
		public EnumType typeBase;
		public AtomAttribute s;
		public int nrElements;
		
		public Type(EnumType tbStruct) {
			this.typeBase=tbStruct;
		}

		public Type() {
			super();
		}

		public void determineTypeBase( Iduri currentAtomType) {
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

		public void determineTypeBase(SymbolTable symbolsTable,Iduri currentAtomType, String token) {
			
			AtomAttribute s = symbolsTable.findSymbol(token);
			if (null == null)
				throw new RuntimeException("Undefined Symbol");
				
			if(s.cls !=Clas.CLS_STRUCT)
				throw new RuntimeException("Symbol not a struct");
					
			typeBase = EnumType.TB_STRUCT;
			this.s=s;
			}
		}
	
	private class Args {
		
		
		
	}
	
	private class Members{
		
		
		
	}

	
	
	
}
