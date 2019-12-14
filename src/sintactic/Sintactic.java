package sintactic;

import java.io.FileNotFoundException;
import java.util.List;
import java.util.ListIterator;
import java.util.Stack;

import lexical.Atom;
import lexical.Atom.Iduri;
import lexical.Lexical;
import tabelaS.SymbolTable;
import tabelaS.SymbolTable.AtomAttribute;
import tabelaS.SymbolTable.Clas;
import tabelaS.SymbolTable.EnumType;
import tabelaS.SymbolTable.RetVal;
import tabelaS.SymbolTable.Type;

public class Sintactic {

	/*
	 * SECVENTA ( a b … ) Se implementeaza testand pe rand fiecare componenta a sa
	 * si returnand true doar daca toate sunt indeplinite.
	 * 
	 * ALTERNATIVA ( a | b ) Se implementeaza testand pe rand fiecare componenta si
	 * returnand true la prima adevarata. Daca niciuna nu este adevarata, in final
	 * se returneaza false.
	 * 
	 * 
	 * Optionalitatea ( a? ) Deoarece in acest caz a poate sa existe sau nu, se
	 * incearca sa se consume acesta, dar fara sa se tina cont de rezultatul
	 * incercarii.
	 * 
	 * 
	 * Repetitia optionala ( a* ) Se include testarea lui a intr-o bucla care
	 * dureaza atata timp cat s-a putut consuma a.
	 * 
	 * 
	 * . Prezenta ε ca membru intr-o alternativa face ca acea alternativa sa devina
	 * optionala: „α | ε” = „α?”.
	 */
	// BEFORE calling a method call nextAtom
	// assume on each method start (start of method) that nextAtom was called
	// each method on return succes has called nextAtom !!
	// each method on return false has NOT called nextAtom !!

	private List<Atom> atoms;
	private Atom currentAtom;
	private ListIterator<Atom> atomIterator;
	Stack<ListIterator<Atom>> STACKIterator = new Stack<ListIterator<Atom>>();
	Stack<Atom> STACKAtom = new Stack<Atom>();
	Stack<StringBuilder> STACKERRORS = new Stack<StringBuilder>();
	StringBuilder errorReporter = new StringBuilder();

	public Sintactic(List<Atom> atoms) {
		this.atoms = atoms;

	}

	public void compile() {
		atomIterator = atoms.listIterator();
		System.out.println("Start sintax check...");

		consumeProgram();

		if (errorReporter.length() > 0) {
			System.out.println("Sintax check...FAILED");
			System.out.println(errorReporter.toString());
		} else
			System.out.println("Sintax check...PASSED");
	}

	private boolean consumeProgram() {

		nextAtom();
		while (currentAtom().type != Iduri.END) {
			saveAtom();
			if (consumeDeclStruct()) {
				popAtomFromStack();
				continue;
			}
			restoreSaveAtom();
			if (consumeDeclFunc()) {
				popAtomFromStack();
				continue;
			}
			restoreSaveAtom();
			if (consumeDeclVar()) {
				popAtomFromStack();
				continue;
			}
			break;
		}

		if (currentAtom().type == Iduri.END)
			return true;
		else {
			errorReporter.append(String.format(prefix, this.currentLine(), "Sintax check premature abord.\n"));
			return false;
		}
	}

	private boolean consumeDeclStruct() {
		String tokenName;
		if (currentAtomType() != Iduri.STRUCT)
			return false;
		nextAtom();

		if (currentAtomType() != Iduri.ID) {
			errorReporter.append(String.format(prefix, this.currentLine(), "Invalid id provided"));
			gotoNextDelimitator();
			return false;
		}
		tokenName = currentAtom().text;

		nextAtom();

		if (currentAtomType() != Iduri.LACC) {
			errorReporter.append(String.format(prefix, this.currentLine(), "Missing open decl str ("));
			gotoNextDelimitator();
			return false;
		}

		SymbolTable.getInstance().crtStruct = SymbolTable.getInstance().addStructSymbol(tokenName);

		nextAtom();

		while (consumeDeclVar()) {

		}

		if (currentAtomType() != Iduri.RACC) {
			errorReporter.append(String.format(prefix, this.currentLine(), "Missing close }"));
			gotoNextDelimitator();
			return false;
		}
		nextAtom();

		if (currentAtomType() != Iduri.SEMICOLON) {
			errorReporter.append(String.format(prefix, this.currentLine(), "DeclStruct Missing ; Assuming it"));
			gotoNextDelimitator();
			return false;
		}
		nextAtom();

		SymbolTable.getInstance().crtStruct = null;
		return true;
	}

	private boolean consumeDeclVar() {
		SymbolTable.Type type = new SymbolTable.Type();
		String tokenName;
		if (!consumeTypeBase(type))
			return false;

		if (currentAtomType() != Iduri.ID) {
			errorReporter.append(String.format(prefix, this.currentLine(), "Invalid id provided"));
			gotoNextDelimitator();
			return false;
		}
		tokenName = currentAtom().text;
		nextAtom();

		if (!consumeArrayDecl(type))
			type.nrElements = -1;
		SymbolTable.getInstance().addVar(tokenName, type);
		while (true) {

			if (currentAtomType() == Iduri.COMMA) {
				nextAtom();

				if (currentAtomType() != Iduri.ID) {
					errorReporter.append(String.format(prefix, this.currentLine(), "Invalid id provided"));
					gotoNextDelimitator();
					return false;
				}
				tokenName = currentAtom().text;
				nextAtom();
				type = type.clone();
				if (!consumeArrayDecl(type))
					type.nrElements = -1;
				SymbolTable.getInstance().addVar(tokenName, type);
				continue;
			}
			break;
		}

		if (currentAtomType() != Iduri.SEMICOLON) {
			errorReporter.append(String.format(prefix, this.currentLine(), "Decl Var Missing ; Assuming it "));
			gotoNextDelimitator();
			return false;
		}
		nextAtom();

		return true;
	}

	private boolean consumeTypeBase(SymbolTable.Type type) {
		if (currentAtomType() == Iduri.INT || currentAtomType() == Iduri.DOUBLE || currentAtomType() == Iduri.CHAR) {
			type.determineTypeBase(currentAtomType());
			nextAtom();
			return true;
		} else if (currentAtomType() == Iduri.STRUCT) {

			nextAtom();
			if (currentAtom.type != Iduri.ID) {
				errorReporter.append(String.format(prefix, this.currentLine(), "Invalid type"));
				gotoNextDelimitator();
				return false;
			}

			type.determineTypeBase(currentAtomType(), currentAtom().text);

			nextAtom();
			return true;

		} else
			return false;
	}

	private boolean consumeArrayDecl(SymbolTable.Type type) {
		if (currentAtomType() != Iduri.LBRACKET)
			return false;
		nextAtom();
		RetVal rv = new RetVal();
		if (consumeExpr(rv)) {
			if (!rv.isCtVal)
				throw new RuntimeException("the array size is not a constant");
			if (rv.type.typeBase != EnumType.TB_INT)
				throw new RuntimeException("the array size is not an integer");
			type.nrElements = rv.ctVal.i.intValue();
		} else {
			type.nrElements = 0;
		}

		if (currentAtomType() != Iduri.RBRACKET) {
			errorReporter.append(String.format(prefix, this.currentLine(), "Missing close ]"));
			gotoNextDelimitator();
			return false;
		}
		nextAtom();
		return true;
	}

	private boolean consumeTypeName(SymbolTable.Type retType) {

		if (!consumeTypeBase(retType))
			return false;
		if (!consumeArrayDecl(retType))
			retType.nrElements = 0;
		return true;
	}

	private boolean consumeDeclFunc() {
		SymbolTable.Type type = new SymbolTable.Type();
		if (consumeTypeBase(type)) {

			if (currentAtomType() == Iduri.MUL) {
				nextAtom();
				type.nrElements = 0;
			} else
				type.nrElements = -1;

		} else if (currentAtomType() == Iduri.VOID) {
			type.typeBase = EnumType.TB_VOID;
			nextAtom();
		} else {
			return false;
		}

		if (currentAtomType() != Iduri.ID) {
			errorReporter.append(String.format(prefix, this.currentLine(), "Invalid id provided"));
			gotoNextDelimitator();
			return false;
		}
		String tokenName = currentAtom().text;
		nextAtom();

		if (currentAtomType() != Iduri.LPAR) {
			errorReporter.append(String.format(prefix, this.currentLine(), "Missing func open ("));
			gotoNextDelimitator();
			return false;
		}
		SymbolTable.getInstance().crtFunc = SymbolTable.getInstance().addFuncSymbol(tokenName, type);
		nextAtom();

		if (consumeFuncArg()) {
			while (true) {
				if (currentAtomType() == Iduri.COMMA) {
					nextAtom();
					if (!consumeFuncArg()) {
						errorReporter.append(String.format(prefix, this.currentLine(), "Missing fct arg"));
						gotoNextDelimitator();
						return false;
					}
					continue;
				}
				break;
			}
		}

		if (currentAtomType() != Iduri.RPAR) {
			errorReporter.append(String.format(prefix, this.currentLine(), "Missing close )"));
			gotoNextDelimitator();
			return false;
		}
		SymbolTable.getInstance().crtDepth--;
		nextAtom();

		if (!consumeStmCompound()) {
			errorReporter.append(String.format(prefix, this.currentLine(), "Missing declFunct stmCOmpound"));
			gotoNextDelimitator();
			return false;
		}
		SymbolTable.getInstance().deleteSymbolsAfter(SymbolTable.getInstance().crtFunc);
		SymbolTable.getInstance().crtFunc = null;
		return true;
	}

	private boolean consumeFuncArg() {
		SymbolTable.Type type = new SymbolTable.Type();
		if (!consumeTypeBase(type))
			return false;
		if (currentAtomType() != Iduri.ID) {
			errorReporter.append(String.format(prefix, this.currentLine(), "fct arg Invalid id provided"));
			gotoNextDelimitator();
			return false;
		}
		String tokenName = currentAtom().text;
		nextAtom();

		if (!consumeArrayDecl(type))
			type.nrElements = -1;
		SymbolTable.getInstance().addFcArg(tokenName, type);

		return true;
	}

	private boolean consumeStm() {
		saveAtom();
		if (consumeStmCompound()) {
			popAtomFromStack();
			return true;
		}
		restoreSaveAtom();

		if (consumeStmIfElse()) {
			popAtomFromStack();
			return true;
		}

		restoreSaveAtom();
		if (consumeStmWhile()) {
			popAtomFromStack();
			return true;
		}

		restoreSaveAtom();
		if (consumeStmFor())
			return true;

		restoreSaveAtom();
		if (consumeStmBreak()) {
			popAtomFromStack();
			return true;
		}

		restoreSaveAtom();
		if (consumeStmReturn()) {
			popAtomFromStack();
			return true;
		}

		restoreSaveAtom();
		if (consumeStmSemicolon()) {
			popAtomFromStack();
			return true;
		}

		restoreAtom();
		return false;

	}

	private boolean consumeStmSemicolon() {

		RetVal rv = new RetVal();
		consumeExpr(rv);

		if (currentAtomType() != Iduri.SEMICOLON) {
			errorReporter.append(String.format(prefix, this.currentLine(), "? Missing ; Assuming it"));
			gotoNextDelimitator();
			return false;
		}

		nextAtom();
		return true;
	}

	private boolean consumeStmReturn() {
		if (currentAtomType() != Iduri.RETURN)
			return false;

		nextAtom();

		RetVal rv = new RetVal();
		consumeExpr(rv);
		{
			if (SymbolTable.getInstance().crtFunc.type.typeBase == EnumType.TB_VOID)
				throw new RuntimeException("a void function cannot return a value");
			Type.cast(SymbolTable.getInstance().crtFunc.type, rv.type);
		}
		if (currentAtomType() != Iduri.SEMICOLON) {
			errorReporter.append(String.format(prefix, this.currentLine(), " return Missing ; Assuming it"));
			gotoNextDelimitator();
			return false;
		}
		nextAtom();
		return true;
	}

	private boolean consumeStmBreak() {
		if (currentAtomType() != Iduri.BREAK)
			return false;

		nextAtom();
		if (currentAtomType() != Iduri.SEMICOLON) {
			errorReporter.append(String.format(prefix, this.currentLine(), " break Missing ; Assuming it"));
			gotoNextDelimitator();
			return false;
		}
		nextAtom();
		return true;
	}

	private boolean consumeStmFor() {
		if (currentAtomType() != Iduri.FOR)
			return false;

		nextAtom();
		if (currentAtomType() != Iduri.LPAR) {
			errorReporter.append(String.format(prefix, this.currentLine(), " stm for Missing open ("));
			gotoNextDelimitator();
			return false;
		}
		nextAtom();

		RetVal rv1 = new RetVal();
		consumeExpr(rv1);

		if (currentAtomType() != Iduri.SEMICOLON) {
			errorReporter.append(String.format(prefix, this.currentLine(), "Missing ; Assuming it"));
			gotoNextDelimitator();
			return false;
		}
		nextAtom();

		RetVal rv2 = new RetVal();
		consumeExpr(rv2);
		{

			if (rv2.type.typeBase == EnumType.TB_STRUCT)
				throw new RuntimeException("a structure cannot be logically tested");
		}
		if (currentAtomType() != Iduri.SEMICOLON) {
			errorReporter.append(String.format(prefix, this.currentLine(), "Missing ; Assuming it"));
			gotoNextDelimitator();
			return false;
		}
		nextAtom();

		RetVal rv3 = new RetVal();
		consumeExpr(rv3);

		if (currentAtomType() != Iduri.RPAR) {
			errorReporter.append(String.format(prefix, this.currentLine(), "Missing close )"));
			gotoNextDelimitator();
			return false;
		}
		nextAtom();

		if (!consumeStm()) {
			errorReporter.append(String.format(prefix, this.currentLine(), "stm  for Missing stm ("));
			gotoNextDelimitator();
			return false;
		}
		return true;
	}

	private boolean consumeStmWhile() {
		if (currentAtomType() != Iduri.WHILE)
			return false;
		nextAtom();

		if (currentAtomType() != Iduri.LPAR) {
			errorReporter.append(String.format(prefix, this.currentLine(), "Missing open while ("));
			gotoNextDelimitator();
			return false;
		}
		nextAtom();
		RetVal rv = new RetVal();
		if (!consumeExpr(rv)) {
			errorReporter.append(String.format(prefix, this.currentLine(), "stm Missing expr ("));
			gotoNextDelimitator();
			return false;
		} else if (rv.type.typeBase == EnumType.TB_STRUCT)
			throw new RuntimeException("a structure cannot be logically tested");

		if (currentAtomType() != Iduri.RPAR) {
			errorReporter.append(String.format(prefix, this.currentLine(), "Missing close )"));
			gotoNextDelimitator();
			return false;
		}
		nextAtom();

		if (!consumeStm()) {
			errorReporter.append(String.format(prefix, this.currentLine(), "stm  while Missing stm ("));
			gotoNextDelimitator();
			return false;
		}
		return true;
	}

	private boolean consumeStmIfElse() {
		if (currentAtomType() != Iduri.IF)
			return false;
		nextAtom();

		if (currentAtomType() != Iduri.LPAR) {
			errorReporter.append(String.format(prefix, this.currentLine(), "Missing open stm ("));
			gotoNextDelimitator();
			return false;
		}
		nextAtom();

		RetVal rv = new RetVal();
		if (!consumeExpr(rv)) {
			errorReporter.append(String.format(prefix, this.currentLine(), "stm Missing expr ("));
			gotoNextDelimitator();
			return false;
		} else if (rv.type.typeBase == EnumType.TB_STRUCT)
			throw new RuntimeException("a structure cannot be logically tested");

		if (currentAtomType() != Iduri.RPAR) {
			errorReporter.append(String.format(prefix, this.currentLine(), "Missing close stm )"));
			gotoNextDelimitator();
			return false;
		}
		nextAtom();

		if (!consumeStm()) {
			errorReporter.append(String.format(prefix, this.currentLine(), " missing Stm if stm"));
			gotoNextDelimitator();
			return false;
		}

		if (currentAtomType() == Iduri.ELSE) {
			nextAtom();
			if (!consumeStm()) {
				errorReporter.append(String.format(prefix, this.currentLine(), "Missing Stm if else stm "));
				gotoNextDelimitator();
				return false;
			}
		}

		return true;

	}

	private boolean consumeStmCompound() {

		AtomAttribute last = SymbolTable.getInstance().getLast();
		if (currentAtomType() != Iduri.LACC)
			return false;
		SymbolTable.getInstance().crtDepth++;
		nextAtom();
		while (true) {
			if (consumeDeclVar()) {
				continue;
			}
			if (consumeStm()) {
				continue;
			} else {

				break;
			}
		}

		if (currentAtomType() != Iduri.RACC) {
			errorReporter.append(String.format(prefix, this.currentLine(), "StmCompound missing }"));
			gotoNextDelimitator();
			return false;
		}
		SymbolTable.getInstance().crtDepth--;
		SymbolTable.getInstance().deleteSymbolsAfter(last);
		nextAtom();
		return true;
	}

	private boolean consumeExpr(RetVal rv) {
		if (consumeExprAssign(rv)) {
			return true;
		} else
			return false;
	}

	private boolean consumeExprAssign(RetVal rv) {
		saveAtom();
		if (consumeExprAssignS(rv)) {
			popAtomFromStack();
			return true;
		}
		restoreSaveAtom();
		if (consumeExprOr(rv)) {
			popAtomFromStack();
			return true;
		}
		restoreAtom();
		return false;

	}

	private boolean consumeExprAssignS(RetVal rv) {
		if (!consumeExprUnary(rv))
			return false;
		if (currentAtomType() != Iduri.ASSIGN) {
			errorReporter.append(String.format(prefix, this.currentLine(), "Missing = "));
			gotoNextDelimitator();
			return false;
		}
		nextAtom();
		RetVal rve = new RetVal();
		if (!consumeExprAssign(rve)) {
			errorReporter.append(String.format(prefix, this.currentLine(), "Missing = right side operator "));
			gotoNextDelimitator();
			return false;
		}
		{
			if (!rv.isLVal)
				throw new RuntimeException("cannot assign to a non-lval");
			if (rv.type.nrElements > -1 || rve.type.nrElements > -1)
				throw new RuntimeException("the arrays cannot be assigned");
			Type.cast(rv.type, rve.type);
			rv.isCtVal = rv.isLVal = false;
		}
		return true;
	}

	private boolean consumeExprOr(RetVal rv) {
		if (!consumeExprAnd(rv))
			return false;
		if (!consumeExprOrResolved(rv)) {
			errorReporter.append(String.format(prefix, this.currentLine(), "Invalid OR Expr\n"));
			gotoNextDelimitator();
			return false;
		}
		return true;
	}

	private boolean consumeExprOrResolved(RetVal rv) {
		boolean er = false;
		if (currentAtomType() == Iduri.OR) {
			saveAtom();
			nextAtom();
			RetVal rve = new RetVal();
			if (!consumeExprAnd(rve)) {
				errorReporter.append(String.format(prefix, this.currentLine(), "Invalid Expr afrer OR op\n"));
				gotoNextDelimitator();
				er = true;
			}
			{
				if (rv.type.typeBase == EnumType.TB_STRUCT || rve.type.typeBase == EnumType.TB_STRUCT)
					throw new RuntimeException("a structure cannot be logically tested");
				rv.type = Type.createType(EnumType.TB_INT, -1);
				rv.isCtVal = rv.isLVal = false;
			}
			if (!consumeExprOrResolved(rv)) {
				errorReporter.append(String.format(prefix, this.currentLine(), "Invalid ExprOrResolved \n"));
				gotoNextDelimitator();
				er = true;
			}
			popAtomFromStack();
		}
		if (er)
			restoreAtom();
		return true;
	}

	private boolean consumeExprAnd(RetVal rv) {
		if (!consumeExprEq(rv))
			return false;
		if (!consumeExprAndResolved(rv)) {
			errorReporter.append(String.format(prefix, this.currentLine(), "Invalid And Expr\n"));
			gotoNextDelimitator();
			return false;
		}
		return true;
	}

	private boolean consumeExprAndResolved(RetVal rv) {
		boolean er = false;
		if (currentAtomType() == Iduri.AND) {
			saveAtom();
			nextAtom();
			RetVal rve = new RetVal();
			if (!consumeExprEq(rve)) {
				errorReporter.append(String.format(prefix, this.currentLine(), "Invalid Expr afrer AND op\n"));
				gotoNextDelimitator();
				er = true;
			}
			{
				if (rv.type.typeBase == EnumType.TB_STRUCT || rve.type.typeBase == EnumType.TB_STRUCT)
					throw new RuntimeException("a structure cannot be logically tested");
				rv.type = Type.createType(EnumType.TB_INT, -1);
				rv.isCtVal = rv.isLVal = false;
			}
			if (!consumeExprAndResolved(rv)) {
				errorReporter.append(String.format(prefix, this.currentLine(), "Invalid ExprAndResolved \n"));
				gotoNextDelimitator();
				er = true;
			}
			popAtomFromStack();
		}
		if (er)
			restoreAtom();
		return true;
	}

	private boolean consumeExprEq(RetVal rv) {
		if (!consumeExpRel(rv))
			return false;
		if (!consumeExprEqReolved(rv)) {
			errorReporter.append(String.format(prefix, this.currentLine(), "Invalid Eq Expr\n"));
			gotoNextDelimitator();
			return false;
		}
		return true;
	}

	private boolean consumeExprEqReolved(RetVal rv) {
		boolean er = false;
		if (currentAtomType() == Iduri.EQUAL || currentAtomType() == Iduri.NOTEQ) {
			Iduri tkop = currentAtomType();
			saveAtom();
			nextAtom();
			RetVal rve = new RetVal();
			if (!consumeExpRel(rve)) {
				errorReporter.append(String.format(prefix, this.currentLine(), "Invalid Expr afrer ==!= op\n"));
				gotoNextDelimitator();
				er = true;
			}
			{
				if (rv.type.typeBase == EnumType.TB_STRUCT || rve.type.typeBase == EnumType.TB_STRUCT)
					throw new RuntimeException("a structure cannot be compared");
				rv.type = Type.createType(EnumType.TB_INT, -1);
				rv.isCtVal = rv.isLVal = false;

			}
			if (!consumeExprEqReolved(rv)) {
				errorReporter.append(String.format(prefix, this.currentLine(), "Invalid ExpEqResolved \n"));
				gotoNextDelimitator();
				er = true;
			}
			popAtomFromStack();
		}
		if (er)

			restoreAtom();
		return true;

	}

	private boolean consumeExpRel(RetVal rv) {
		if (!consumeExprAdd(rv))
			return false;
		if (!consumeExpRelResolved(rv)) {
			errorReporter.append(String.format(prefix, this.currentLine(), "Invalid REl Expr\n"));
			gotoNextDelimitator();
			return false;
		}
		return true;
	}

	private boolean consumeExpRelResolved(RetVal rv) {
		boolean er = false;
		if (currentAtomType() == Iduri.LESS || currentAtomType() == Iduri.GREATER || currentAtomType() == Iduri.LESSEQ
				|| currentAtomType() == Iduri.GREATEREQ) {
			Iduri tkop = currentAtomType();
			saveAtom();
			nextAtom();
			RetVal rve = new RetVal();
			if (!consumeExprAdd(rve)) {
				errorReporter.append(String.format(prefix, this.currentLine(), "Invalid Expr afrer ><== op\n"));
				gotoNextDelimitator();
				er = true;
			}
			{
				if (rv.type.nrElements > -1 || rve.type.nrElements > -1)
					throw new RuntimeException("an array cannot be compared");
				if (rv.type.typeBase == EnumType.TB_STRUCT || rve.type.typeBase == EnumType.TB_STRUCT)
					throw new RuntimeException("a structure cannot be compared");
				rv.type = Type.createType(EnumType.TB_INT, -1);
				rv.isCtVal = rv.isLVal = false;
			}
			if (!consumeExpRelResolved(rv)) {
				errorReporter.append(String.format(prefix, this.currentLine(), "Invalid ExprRelResolved \n"));
				gotoNextDelimitator();
				er = true;
			}
			popAtomFromStack();
		}
		if (er)
			restoreAtom();

		return true;
	}

	private boolean consumeExprAdd(RetVal rv) {
		if (!consumeExprMul(rv))
			return false;
		if (!consumeExprAddResolved(rv)) {
			errorReporter.append(String.format(prefix, this.currentLine(), "Invalid Add Expr\n"));
			gotoNextDelimitator();
			return false;
		}
		return true;
	}

	private boolean consumeExprAddResolved(RetVal rv) {
		boolean er = false;
		if (currentAtomType() == Iduri.ADD || currentAtomType() == Iduri.SUB) {
			Iduri tkop = currentAtomType();
			nextAtom();
			saveAtom();
			RetVal rve = new RetVal();
			if (!consumeExprMul(rve)) {
				errorReporter.append(String.format(prefix, this.currentLine(), "Invalid Expr afrer +- op\n"));
				gotoNextDelimitator();
				er = true;
			}
			{
				if (rv.type.nrElements > -1 || rve.type.nrElements > -1)
					throw new RuntimeException("an array cannot be added or subtracted");
				if (rv.type.typeBase == EnumType.TB_STRUCT || rve.type.typeBase == EnumType.TB_STRUCT)
					throw new RuntimeException("a structure cannot be added or subtracted");
				rv.type = Type.getArithType(rv.type, rve.type);
				rv.isCtVal = rv.isLVal = false;
			}
			if (!consumeExprAddResolved(rv)) {
				errorReporter.append(String.format(prefix, this.currentLine(), "Invalid ExprAddResolved \n"));
				gotoNextDelimitator();
				er = true;
			}
			popAtomFromStack();
		}
		if (er)

			restoreAtom();

		return true;

	}

	private boolean consumeExprMul(RetVal rv) {
		if (!consumeExprCast(rv))
			return false;
		if (!consumeExprMulResolved(rv)) {
			errorReporter.append(String.format(prefix, this.currentLine(), "Invalid Mul Expr\n"));
			gotoNextDelimitator();
			return false;
		}
		return true;
	}

	private boolean consumeExprMulResolved(RetVal rv) {
		boolean er = false;
		if (currentAtomType() == Iduri.MUL || currentAtomType() == Iduri.DIV) {
			Iduri tkop = currentAtomType();
			saveAtom();
			nextAtom();
			RetVal rve = new RetVal();
			if (!consumeExprCast(rve)) {
				errorReporter.append(String.format(prefix, this.currentLine(), "Invalid Expr afrer */ op\n"));
				gotoNextDelimitator();
				er = true;
			}
			{
				if (rv.type.nrElements > -1 || rve.type.nrElements > -1)
					throw new RuntimeException("an array cannot be multiplied or divided");
				if (rv.type.typeBase == EnumType.TB_STRUCT || rve.type.typeBase == EnumType.TB_STRUCT)
					throw new RuntimeException("a structure cannot be multiplied or divided");
				rv.type = Type.getArithType(rv.type, rve.type);
				rv.isCtVal = rv.isLVal = false;
			}
			if (!consumeExprMulResolved(rv)) {
				errorReporter.append(String.format(prefix, this.currentLine(), "Invalid ExprMulResolved \n"));
				gotoNextDelimitator();
				er = true;
			}

			popAtomFromStack();
		}
		if (er)
			restoreAtom();
		return true;
	}

	private boolean consumeExprCast(RetVal rv) {

		saveAtom();
		if (consumeExprCastS(rv)) {
			popAtomFromStack();
			return true;
		}
		restoreSaveAtom();
		if (consumeExprUnary(rv)) {
			popAtomFromStack();
			return true;
		}
		restoreAtom();
		return false;
	}

	private boolean consumeExprCastS(RetVal rv) {
		if (currentAtomType() != Iduri.LPAR)
			return false;

		nextAtom();
		SymbolTable.Type type = new Type();
		if (!consumeTypeName(type)) {
			errorReporter.append(String.format(prefix, this.currentLine(), "Cast type wrong name\n"));
			gotoNextDelimitator();
			return false;
		}

		if (currentAtomType() != Iduri.RPAR) {
			errorReporter.append(String.format(prefix, this.currentLine(), "Cast type mising\n"));
			gotoNextDelimitator();
			return false;
		}

		nextAtom();
		RetVal rve = new RetVal();
		if (!consumeExprCast(rve)) {
			errorReporter.append(String.format(prefix, this.currentLine(), "Wrong Expr to cast\n"));
			gotoNextDelimitator();
			return false;
		}
		{
			Type.cast(type, rve.type);
			rv.type = type;
			rv.isCtVal = rv.isLVal = false;
		}
		return true;
	}

	private boolean consumeExprUnary(RetVal rv) {
		saveAtom();
		if (consumeExprUnaryL(rv)) {
			popAtomFromStack();
			return true;
		}

		restoreSaveAtom();
		if (consumeExprPostfix(rv)) {
			popAtomFromStack();
			return true;
		}

		restoreAtom();
		return false;
	}

	private boolean consumeExprUnaryL(RetVal rv) {
		if (currentAtomType() == Iduri.SUB || currentAtomType() == Iduri.NOT) {
			Iduri tkop = currentAtomType();
			nextAtom();

			if (!consumeExprUnary(rv)) {
				errorReporter.append(String.format(prefix, this.currentLine(), "UNary Invalid ExprPostfix\n"));
				gotoNextDelimitator();
				return false;
			}
			{
				if (tkop == Iduri.SUB) {
					if (rv.type.nrElements >= 0)
						throw new RuntimeException("unary '-' cannot be applied to an array");
					if (rv.type.typeBase == EnumType.TB_STRUCT)
						throw new RuntimeException("unary '-' cannot be applied to a struct");
				} else { // NOT
					if (rv.type.typeBase == EnumType.TB_STRUCT)
						throw new RuntimeException("'!' cannot be applied to a struct");
					rv.type = Type.createType(EnumType.TB_INT, -1);
				}
				rv.isCtVal = rv.isLVal = false;
			}
			return true;
		}
		return false;
	}

	private boolean consumeExprPostfix(RetVal rv) {

		if (!consumeExprPrimary(rv))
			return false;

		if (!consumeExprPostfixResolved(rv)) {
			errorReporter.append(String.format(prefix, this.currentLine(), "Invalid ExprPrimary\n"));
			gotoNextDelimitator();
			return false;
		}

		return true;
	}

	private boolean consumeExprPostfixResolved(RetVal rv) {

		saveAtom();
		if (consumeExprPostfixResolvedArray(rv)) {
			popAtomFromStack();
			return true;
		}

		restoreSaveAtom();
		if (consumeExprPostfixResolvedStructField(rv)) {
			popAtomFromStack();
			return true;
		}
		restoreAtom();
		return true;

	}

	private boolean consumeExprPostfixResolvedArray(RetVal rv) {
		if (currentAtomType() != Iduri.LBRACKET)
			return false;
		nextAtom();
		RetVal rve = new RetVal();
		if (!consumeExpr(rve)) {
			errorReporter.append(String.format(prefix, this.currentLine(), "Invalid array index ID\n"));
			gotoNextDelimitator();
			return false;
		}
		{
			if (rv.type.nrElements < 0)
				throw new RuntimeException("only an array can be indexed");
			Type typeInt = Type.createType(EnumType.TB_INT, -1);

			Type.cast(typeInt, rve.type);
			rv.type = rv.type; // WTF ?
			rv.type.nrElements = -1;
			rv.isLVal = true;
			rv.isCtVal = false;

		}
		if (currentAtomType() != Iduri.RBRACKET) {
			errorReporter.append(String.format(prefix, this.currentLine(), "Missing close ]\n"));
			gotoNextDelimitator();
			return false;
		}
		nextAtom();

		if (!consumeExprPostfixResolved(rv)) {
			errorReporter.append(String.format(prefix, this.currentLine(), "Invalid ExprPostfixResolved\n"));
			gotoNextDelimitator();
			return false;
		}
		return true;
	}

	private boolean consumeExprPostfixResolvedStructField(RetVal rv) {
		if (currentAtomType() != Iduri.DOT)
			return false;
		nextAtom();
		if (currentAtomType() != Iduri.ID) {
			errorReporter.append(String.format(prefix, this.currentLine(), "Invalid Struct ID field\n"));
			gotoNextDelimitator();
			return false;
		}
		String tokenName = currentAtom().text;
		{
			AtomAttribute sStruct = rv.type.s;
			AtomAttribute sMember = sStruct.findMember(tokenName);
			if (sMember == null)
				throw new RuntimeException(
						String.format("struct %s does not have a member %s", sStruct.name, tokenName));
			rv.type = sMember.type;
			rv.isLVal = true;
			rv.isCtVal = false;
		}

		nextAtom();

		if (!consumeExprPostfixResolved(rv)) {
			errorReporter.append(String.format(prefix, this.currentLine(), "Invalid ExprPostfixResolved\n"));
			gotoNextDelimitator();
			return false;
		}
		return true;
	}

	private boolean consumeExprPrimary(RetVal rv) {

		saveAtom();
		if (consumeExprPrimaryFctCall(rv)) {
			popAtomFromStack();
			return true;
		}
		restoreAtom();

		if (currentAtomType() == Iduri.CT_INT) {
			rv.makePrimitiv(EnumType.TB_INT, currentAtom().intreg, -1);
			nextAtom();
			return true;
		} else if (currentAtomType() == Iduri.CT_REAL) {
			rv.makePrimitiv(EnumType.TB_DOUBLE, currentAtom().real, -1);
			nextAtom();
			return true;
		} else if (currentAtomType() == Iduri.CT_CHAR) {
			rv.makePrimitiv(EnumType.TB_CHAR, currentAtom().text, -1);
			nextAtom();
			return true;
		} else if (currentAtomType() == Iduri.CT_SRING) {
			rv.makePrimitiv(EnumType.TB_CHAR, currentAtom().text, 0);
			nextAtom();
			return true;
		}

		saveAtom();
		if (consumeExprPrimaryParntesis(rv)) {
			popAtomFromStack();
			return true;
		}
		restoreAtom();

		return false;

	}

	private boolean consumeExprPrimaryFctCall(RetVal rv) {
		if (currentAtomType() != Iduri.ID)
			return false;
		String tkName = currentAtom().text;
		AtomAttribute s = rv.fromSymbolName(tkName);
		nextAtom();

		if (currentAtomType() == Iduri.LPAR) {
			int i = 0;

			AtomAttribute crtDefArg = s.getArgByIndex(i);
			s.isFct();

			nextAtom();
			RetVal arg = new RetVal();
			if (consumeExpr(arg)) {
				{
					if (s.getArgByIndex(i) == null)// do compare method
						throw new RuntimeException("too many arguments in call" + tkName);
					Type.cast(crtDefArg.type, arg.type);
					crtDefArg = s.getArgByIndex(i++);
				}
				while (true) {
					if (currentAtomType() == Iduri.COMMA) {
						nextAtom();
						arg = new RetVal();
						if (!consumeExpr(arg)) {
							{
								if (s.getArgByIndex(i) == null)// do compare method
									throw new RuntimeException("too many arguments in call" + tkName);
								Type.cast(crtDefArg.type, arg.type);
								crtDefArg = s.getArgByIndex(i++);
							}
							errorReporter.append(
									String.format(prefix, this.currentLine(), "Missing Fct arg after COMMA )\n"));
							gotoNextDelimitator();
							return false;
						}
						continue;
					}
					break;
				}
			}

			if (currentAtomType() != Iduri.RPAR) {
				errorReporter.append(String.format(prefix, this.currentLine(), "Missing Fct )\n"));
				gotoNextDelimitator();
				return false;
			}
			{
				if (i < s.nrOfArgs())// do compare method
					throw new RuntimeException("too few arguments in call" + tkName);
				rv.type = s.type;
				rv.isCtVal = rv.isLVal = false;
			}
			nextAtom();
		} else {
			if (s.cls == Clas.CLS_FUNC || s.cls == Clas.CLS_EXTFUNC)
				throw new RuntimeException("missing call for function " + tkName);
		}
		return true;
	}

	private boolean consumeExprPrimaryParntesis(RetVal rv) {

		if (currentAtomType() != Iduri.LPAR)
			return false;
		nextAtom();

		if (!consumeExpr(rv)) {
			errorReporter.append(String.format(prefix, this.currentLine(), "No expression in parantesis )\n"));
			gotoNextDelimitator();
			return false;
		}

		if (currentAtomType() != Iduri.RPAR) {
			errorReporter.append(String.format(prefix, this.currentLine(), "Missing close )\n"));
			gotoNextDelimitator();
			return false;
		}

		nextAtom();
		return true;
	}

	public static void main(String[] args) {

		Sintactic sintactic;
		Lexical alex;
		for (int i = 0; i <= 9; i++) {

			alex = new Lexical("CCodeTest/" + i + ".c");
			try {
				alex.compile();
				alex.hasErrors();
				SymbolTable.getInstance().initExtFct("methods.h");
				sintactic = new Sintactic(alex.getAtoms());
				sintactic.compile();
				SymbolTable.getInstance().printSymbolTable();

			} catch (FileNotFoundException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			}
		}
	}

	private void saveAtom() {
		ListIterator<Atom> tmp = atoms.listIterator(atomIterator.nextIndex());
		StringBuilder errTMp = new StringBuilder(errorReporter.toString());
		STACKIterator.push(tmp);
		STACKAtom.push(currentAtom);
		STACKERRORS.push(errTMp);
	}

	private void restoreAtom() {
		atomIterator = STACKIterator.pop();
		currentAtom = STACKAtom.pop();
		errorReporter = STACKERRORS.pop();
	}

	private void popAtomFromStack() {
		STACKIterator.pop();
		STACKAtom.pop();
	}

	private int currentLine() {
		return currentAtom.myLine;
	}

	private Iduri currentAtomType() {
		return currentAtom.type;
	}

	private Atom currentAtom() {
		return currentAtom;
	}

	private void nextAtom() {
		currentAtom = atomIterator.next();
		System.out.println(currentAtom.toString());
	}

	private void restoreSaveAtom() {
		restoreAtom();
		saveAtom();
	}

	private void gotoNextDelimitator() {

	}

	private void gotoNextDelimitator(Atom.Iduri id) {
		while (currentAtomType() != id)
			nextAtom();

	}

	private String prefix = "Sintax error at line [%1$s]:%2$s.\n";

}
