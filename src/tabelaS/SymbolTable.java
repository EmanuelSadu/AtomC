package tabelaS;

import java.util.LinkedHashMap;
import java.util.Map;

import lexical.Atom.Iduri;

public class SymbolTable {

	private static SymbolTable instance;
	public AtomAttribute crtStruct;
	public AtomAttribute crtFunc;
	public int crtDepth = 0;
	private LinkedHashMap<String, AtomAttribute> symbols;

	public enum EnumType {
		TB_CHAR, TB_INT, TB_DOUBLE, TB_STRUCT, TB_VOID
	};

	public enum Clas {
		CLS_VAR, CLS_FUNC, CLS_EXTFUNC, CLS_STRUCT
	};

	public enum Mem {
		MEM_GLOBAL, MEM_ARG, MEM_LOCAL
	};

	public void tearDown() {
		instance = null;
	}

	public static SymbolTable getInstance() {
		if (instance == null) {
			instance = new SymbolTable();
		}
		return instance;
	}

	private SymbolTable() {
		symbols = new LinkedHashMap<String, SymbolTable.AtomAttribute>();
	}

	public void initExtFct(String extFctHeaderFile) {

		AtomAttribute getS = addExtFunc("get_s", Type.createType(EnumType.TB_VOID, -1));
		getS.addArg("s", Type.createType(EnumType.TB_CHAR, 0));

		AtomAttribute putS = addExtFunc("put_s", Type.createType(EnumType.TB_VOID, -1));
		putS.addArg("s", Type.createType(EnumType.TB_CHAR, 0));

		AtomAttribute putI = addExtFunc("put_i", Type.createType(EnumType.TB_VOID, -1));
		putI.addArg("i", Type.createType(EnumType.TB_INT, -1));

		AtomAttribute getI = addExtFunc("get_i", Type.createType(EnumType.TB_INT, -1));

		AtomAttribute putD = addExtFunc("put_d", Type.createType(EnumType.TB_VOID, -1));
		putD.addArg("d", Type.createType(EnumType.TB_DOUBLE, -1));

		AtomAttribute getD = addExtFunc("get_d", Type.createType(EnumType.TB_DOUBLE, -1));
		// putI.addArg("i", Type.createType(EnumType.TB_CHAR, -1));

		AtomAttribute putC = addExtFunc("put_c", Type.createType(EnumType.TB_CHAR, -1));
		putC.addArg("c", Type.createType(EnumType.TB_CHAR, -1));

		AtomAttribute getC = addExtFunc("get_c", Type.createType(EnumType.TB_CHAR, -1));
		// putI.addArg("i", Type.createType(EnumType.TB_CHAR, -1));

		AtomAttribute seconds = addExtFunc("seconds", Type.createType(EnumType.TB_DOUBLE, -1));
		// putI.addArg("i", Type.createType(EnumType.TB_CHAR, -1));

	}

	public AtomAttribute findSymbol(String token) {
		return symbols.get(token);
	}

	public AtomAttribute requireSymbol(String token) {
		AtomAttribute s = symbols.get(token);
		if (s == null)
			throw new RuntimeException("Require symbol is null");
		return s;
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

	public AtomAttribute addExtFunc(String token, Type type) {
		AtomAttribute symbol = addSymbol(token, Clas.CLS_EXTFUNC);
		symbol.args = new Args();
		symbols.put(token, symbol);
		symbol.type = type;
		return symbol;
	}

	public void addFcArg(String tokenName, Type type) {

		AtomAttribute symbol = new AtomAttribute(tokenName, Clas.CLS_VAR);
		symbol.mem = Mem.MEM_ARG;
		symbol.type = type;
		symbols.put(tokenName, symbol);

		type = type.clone();
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

	public void deleteSymbolsAfter(AtomAttribute last) {
		LinkedHashMap<String, AtomAttribute> newSymbols = new LinkedHashMap<String, AtomAttribute>();
		for (Map.Entry<String, AtomAttribute> entry : symbols.entrySet()) {

			newSymbols.put(entry.getKey(), entry.getValue());
			if (entry.getKey().equals(last.name))
				break;
		}
		symbols = newSymbols;
	}

	public AtomAttribute getLast() {

		AtomAttribute last = null;
		for (Map.Entry<String, AtomAttribute> entry : symbols.entrySet()) {
			last = entry.getValue();
		}
		return last;
	}

	public static class AtomAttribute {

		public String name;
		public Clas cls;
		public Mem mem;
		public Type type;
		public int depth;
		public Args args;
		public Members members;

		// VM
		int addr; // vm: the memory address for global symbols
		int offset; // vm: the stack offset for local symbols

		public AtomAttribute(String token, Clas clsStruct) {
			this.name = token;
			this.cls = clsStruct;
		}

		public AtomAttribute findMember(String tokenName) {
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

		public void addArg(String tokenName, Type createType) {
			AtomAttribute a = new AtomAttribute(tokenName, SymbolTable.Clas.CLS_VAR);
			a.depth = SymbolTable.getInstance().crtDepth;
			a.type = createType;
			args.args.put(tokenName, a);
		}

		public AtomAttribute getFirstArg() {

			for (Map.Entry<String, AtomAttribute> entry : args.args.entrySet()) {
				return entry.getValue();
			}
			return null;
		}

		public AtomAttribute getLastArg() {

			AtomAttribute last = null;
			for (Map.Entry<String, AtomAttribute> entry : args.args.entrySet()) {
				last = entry.getValue();
			}
			return last;
		}

		public AtomAttribute getArgByIndex(int i) {

			AtomAttribute last = null;
			for (Map.Entry<String, AtomAttribute> entry : args.args.entrySet()) {
				last = entry.getValue();
				if (i == 0)
					return last;
				else
					i--;
			}
			return null;
		}

		public void isFct() {
			if (this.cls != Clas.CLS_FUNC && this.cls != Clas.CLS_EXTFUNC)
				throw new RuntimeException("call of the non-function " + this.name);

		}

		public int nrOfArgs() {
			return args.args.size();
		}

		public AtomAttribute findSymbol(String tokenName) {
			// TODO Auto-generated method stub
			return null;
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
				this.typeBase = EnumType.TB_INT;
				break;
			case DOUBLE:
				this.typeBase = EnumType.TB_DOUBLE;
				break;
			case CHAR:
				this.typeBase = EnumType.TB_CHAR;
				break;
			case STRUCT:
				this.typeBase = EnumType.TB_STRUCT;
				break;
			case VOID:
				this.typeBase = EnumType.TB_VOID;
				break;
			default:
				break;
			}

		}

		public void determineTypeBase(Iduri currentAtomType, String token) {

			AtomAttribute s = SymbolTable.getInstance().findSymbol(token);
			if (s == null)
				throw new RuntimeException("Undefined Symbol " + token);

			if (s.cls != Clas.CLS_STRUCT)
				throw new RuntimeException("Symbol not a struct");

			typeBase = EnumType.TB_STRUCT;
			this.s = s;
		}

		public static void cast(Type dst, Type src) {
			if (src.nrElements > -1) {
				if (dst.nrElements > -1) {
					if (src.typeBase != dst.typeBase)
						throw new RuntimeException("an array cannot be converted to an array of another type");
				} else {
					throw new RuntimeException("a.n array cannot be converted to a non-array");
				}
			} else {
				if (dst.nrElements > -1) {
					throw new RuntimeException("a non-array cannot be converted to an array");
				}
			}
			switch (src.typeBase) {
			case TB_CHAR:
			case TB_INT:
			case TB_DOUBLE:

				switch (dst.typeBase) {
				case TB_CHAR:
				case TB_INT:
				case TB_DOUBLE:
					return;
				default:
					break;
				}
			case TB_STRUCT:
				if (dst.typeBase == EnumType.TB_STRUCT) {
					if (src.s != dst.s)
						throw new RuntimeException("a structure cannot be converted to another one");
					return;
				}
			default:
				break;
			}
			throw new RuntimeException("incompatible types");
		}

		@Override
		public Type clone() {

			Type type = new Type();
			type.nrElements = this.nrElements;
			type.s = this.s;
			type.typeBase = this.typeBase;
			return type;
		}

		public static Type createType(EnumType enumType, int nrElems) {
			Type type = new Type(enumType);
			type.nrElements = nrElems;
			return type;
		}

		public static Type getArithType(Type s1, Type s2) {
			Type a;
			if (s1.typeBase == s2.typeBase)
				a = createType(s1.typeBase, -1);
			else if (s1.typeBase.ordinal() > s2.typeBase.ordinal())
				a = createType(s1.typeBase, -1);
			else
				a = createType(s2.typeBase, -1);
			return a;
		}
	}

	private class Args {
		private LinkedHashMap<String, AtomAttribute> args;

		public Args() {
			args = new LinkedHashMap<String, SymbolTable.AtomAttribute>();
		}
	}

	private class Members {

		private LinkedHashMap<String, AtomAttribute> members;

		public Members() {
			members = new LinkedHashMap<String, SymbolTable.AtomAttribute>();
		}
	}

	public static class CtVal {
		public Long i; // int, char
		public Double d; // double
		public String str;

		public CtVal(Object val) {
			if (val instanceof String)
				str = (String) val;
			else if (val instanceof Long)
				i = (Long) val;
			else if (val instanceof Integer) {
				i = ((Integer) val).longValue();

			} else
				d = (Double) val;
		}

		private CtVal() {
			// TODO Auto-generated constructor stub
		}

		@Override
		protected CtVal clone() {
			CtVal n = new CtVal();
			n.d = d;
			n.i = i;
			n.str = str;
			return n;

		}

	}

	public static class RetVal {
		public Type type; // type of the result
		public boolean isLVal; // if it is a LVal
		public boolean isCtVal; // if it is a constant value
		public CtVal ctVal; // the constat value

		public void makePrimitiv(EnumType type, Object val, int nrElements) {
			this.type = Type.createType(type, nrElements);
			this.ctVal = new CtVal(val);
			this.isCtVal = true;
			this.isLVal = false;
		}

		public AtomAttribute fromSymbolName(String tkName) {

			AtomAttribute s = SymbolTable.getInstance().findSymbol(tkName);
			if (s == null)
				throw new RuntimeException("undefined symbol " + tkName);
			this.type = s.type.clone();
			this.isCtVal = false;
			this.isLVal = true;
			return s;
		}

		@Override
		public RetVal clone() {

			RetVal newRetVal = new RetVal();
			newRetVal.isCtVal = isCtVal;
			newRetVal.isLVal = isLVal;
			newRetVal.type = type.clone();
			newRetVal.ctVal = ctVal.clone();
			return newRetVal;
		}
	}

	public void printSymbolTable() {

		for (Map.Entry<String, AtomAttribute> entry : symbols.entrySet()) {
			AtomAttribute symbol = entry.getValue();
			if (null != symbol.type)
				System.out.println(symbol.type.typeBase);
			System.out.println(entry.getKey() + " = " + symbol.cls + " " + symbol.mem + " ");
		}

	}
}
