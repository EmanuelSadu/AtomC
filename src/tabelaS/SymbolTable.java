package tabelaS;

import java.util.HashMap;
import java.util.Map;

import lexical.Atom.Iduri;

public class SymbolTable {

	public AtomAttribute crtStruct;
	public AtomAttribute crtFunc;
	public int crtDepth = 0;

	public enum EnumType {
		TB_INT, TB_DOUBLE, TB_CHAR, TB_STRUCT, TB_VOID
	};

	public enum Clas {
		CLS_VAR, CLS_FUNC, CLS_EXTFUNC, CLS_STRUCT
	};

	public enum Mem {
		MEM_GLOBAL, MEM_ARG, MEM_LOCAL
	};

	private HashMap<String, AtomAttribute> symbols;

	public SymbolTable() {
		symbols = new HashMap<String, SymbolTable.AtomAttribute>();
	}

	public AtomAttribute findSymbol(String token) {
		return symbols.get(token);
	}

	public AtomAttribute addSymbol(String token, Clas cls) {

		AtomAttribute symbol = findSymbol(token);
		if (symbol != null)
			throw new RuntimeException("Symbol redefinition");
		symbol = new AtomAttribute(token, cls);
		symbol.depth = crtDepth;
		symbols.put(token, symbol);
		return symbol;
	}

	public AtomAttribute addStructSymbol(String token) {

		AtomAttribute symbol = findSymbol(token);
		if (symbol != null)
			throw new RuntimeException("Symbol redefinition " + token);
		symbol = new AtomAttribute(token, Clas.CLS_STRUCT);
		symbol.depth = crtDepth;
		symbol.members = new Members();
		symbols.put(token, symbol);
		return symbol;
	}

	public AtomAttribute addFuncSymbol(String token, Type type) {
		AtomAttribute symbol = findSymbol(token);
		if (symbol != null)
			throw new RuntimeException("Symbol redefinition " + token);
		symbol = new AtomAttribute(token, Clas.CLS_FUNC);
		symbol.args = new Args();
		symbols.put(token, symbol);
		symbol.type = type;
		crtDepth++;
		return symbol;
	}

	public void addFcArg(String tokenName, Type type) {

		AtomAttribute symbol = new AtomAttribute(tokenName, Clas.CLS_VAR);
		symbol.mem = Mem.MEM_ARG;
		symbol.type = type;
		symbols.put(tokenName, symbol);

		symbol = new AtomAttribute(tokenName, Clas.CLS_VAR);
		symbol.mem = Mem.MEM_ARG;
		symbol.type = type;
		crtFunc.addArg(symbol);
	}

	public void addVar(String tokenName, Type type) {

		AtomAttribute s = null;
		if (crtStruct != null) {
			if (crtStruct.findMember(tokenName) != null)
				throw new RuntimeException("Symbol redefinition " + tokenName);
			s = crtStruct.addMember(tokenName, Clas.CLS_VAR);

		} else if (crtFunc != null) {
			s = findSymbol(tokenName);
			if (s != null && s.depth == crtDepth)
				throw new RuntimeException("Symbol redefinition " + tokenName);
			s = addSymbol(tokenName, Clas.CLS_VAR);
			s.mem = Mem.MEM_LOCAL;
		} else {
			if (findSymbol(tokenName) != null)
				throw new RuntimeException("Symbol redefinition " + tokenName);
			s = addSymbol(tokenName, Clas.CLS_VAR);
			s.mem = Mem.MEM_GLOBAL;
		}
		s.type = type;
	}

	public void deleteSymbolsAfter() {
		// TODO Auto-generated method stub

	}

	public static class AtomAttribute {

		public String name;
		public Clas cls;
		public Mem mem;
		public Type type;
		public int depth;
		public Args args;
		public Members members;

		public AtomAttribute(String token, Clas clsStruct) {
			this.name = token;
			this.cls = clsStruct;
		}

		public Object findMember(String tokenName) {
			return members.members.get(tokenName);
		}

		public AtomAttribute addMember(String tokenName, Clas clsVar) {
			AtomAttribute symbol = new AtomAttribute(tokenName, cls);
			members.members.put(tokenName, symbol);
			return symbol;
		}

		public AtomAttribute addArg(String tokenName, Clas clsVar) {
			AtomAttribute symbol = new AtomAttribute(tokenName, cls);

			return symbol;
		}

		public void addArg(AtomAttribute symbol) {
			args.args.put(symbol.name, symbol);
		}

	}

	public static class Type {

		public EnumType typeBase;
		public AtomAttribute s;
		public int nrElements;

		public Type(EnumType tbStruct) {
			this.typeBase = tbStruct;
		}

		public Type() {
			super();
		}

		public void determineTypeBase(Iduri currentAtomType) {
			switch (currentAtomType) {
			case INT:
				typeBase = EnumType.TB_INT;
				break;
			case DOUBLE:
				typeBase = EnumType.TB_DOUBLE;
				break;
			case CHAR:
				typeBase = EnumType.TB_CHAR;
				break;
			case STRUCT:
				typeBase = EnumType.TB_STRUCT;
				break;
			case VOID:
				typeBase = EnumType.TB_VOID;
				break;
			default:
				break;
			}

		}

		public void determineTypeBase(SymbolTable symbolsTable, Iduri currentAtomType, String token) {

			AtomAttribute s = symbolsTable.findSymbol(token);
			if (s == null)
				throw new RuntimeException("Undefined Symbol " + token);

			if (s.cls != Clas.CLS_STRUCT)
				throw new RuntimeException("Symbol not a struct");

			typeBase = EnumType.TB_STRUCT;
			this.s = s;
		}
	}

	private class Args {
		private HashMap<String, AtomAttribute> args;

		public Args() {
			args = new HashMap<String, SymbolTable.AtomAttribute>();
		}
	}

	private class Members {

		private HashMap<String, AtomAttribute> members;

		public Members() {
			members = new HashMap<String, SymbolTable.AtomAttribute>();
		}
	}

	public void printSymbolTable() {

		for (Map.Entry<String, AtomAttribute> entry : symbols.entrySet()) {
			AtomAttribute symbol = entry.getValue();
			System.out.println(entry.getKey() + " = " + symbol.cls + " " + symbol.mem + " " + symbol.type.typeBase);
		}

	}

}
