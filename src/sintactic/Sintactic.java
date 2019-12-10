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
import tabelaS.SymbolTable.EnumType;
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
	SymbolTable symbolsTable;

	public Sintactic(SymbolTable table, List<Atom> atoms) {
		this.atoms = atoms;
		symbolsTable = table;
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

		symbolsTable.crtStruct = symbolsTable.addStructSymbol(tokenName);

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

		symbolsTable.crtStruct = null;
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
		symbolsTable.addVar(tokenName, type);
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
				symbolsTable.addVar(tokenName, type);
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

			type.determineTypeBase(symbolsTable, currentAtomType(), currentAtom().text);

			nextAtom();
			return true;

		} else
			return false;
	}

	private boolean consumeArrayDecl(SymbolTable.Type type) {
		if (currentAtomType() != Iduri.LBRACKET)
			return false;
		nextAtom();
		if (consumeExpr()) {
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
		symbolsTable.crtFunc = symbolsTable.addFuncSymbol(tokenName, type);
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
		symbolsTable.crtDepth--;
		nextAtom();

		if (!consumeStmCompound()) {
			errorReporter.append(String.format(prefix, this.currentLine(), "Missing declFunct stmCOmpound"));
			gotoNextDelimitator();
			return false;
		}
		symbolsTable.deleteSymbolsAfter(symbolsTable.crtFunc);
		symbolsTable.crtFunc = null;
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
		symbolsTable.addFcArg(tokenName, type);

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

		consumeExpr();

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

		consumeExpr();

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

		consumeExpr();

		if (currentAtomType() != Iduri.SEMICOLON) {
			errorReporter.append(String.format(prefix, this.currentLine(), "Missing ; Assuming it"));
			gotoNextDelimitator();
			return false;
		}
		nextAtom();

		consumeExpr();

		if (currentAtomType() != Iduri.SEMICOLON) {
			errorReporter.append(String.format(prefix, this.currentLine(), "Missing ; Assuming it"));
			gotoNextDelimitator();
			return false;
		}
		nextAtom();

		consumeExpr();

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

		if (!consumeExpr()) {
			errorReporter.append(String.format(prefix, this.currentLine(), "stm Missing expr ("));
			gotoNextDelimitator();
			return false;
		}

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

		if (!consumeExpr()) {
			errorReporter.append(String.format(prefix, this.currentLine(), "stm Missing expr ("));
			gotoNextDelimitator();
			return false;
		}

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

		AtomAttribute last = symbolsTable.getLast();
		if (currentAtomType() != Iduri.LACC)
			return false;
		symbolsTable.crtDepth++;
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
		symbolsTable.crtDepth--;
		symbolsTable.deleteSymbolsAfter(last);
		nextAtom();
		return true;
	}

	private boolean consumeExpr() {
		if (consumeExprAssign()) {
			return true;
		} else
			return false;
	}

	private boolean consumeExprAssign() {
		saveAtom();
		if (consumeExprAssignS()) {
			popAtomFromStack();
			return true;
		}
		restoreSaveAtom();
		if (consumeExprOr()) {
			popAtomFromStack();
			return true;
		}
		restoreAtom();
		return false;

	}

	private boolean consumeExprAssignS() {
		if (!consumeExprUnary())
			return false;
		if (currentAtomType() != Iduri.ASSIGN) {
			errorReporter.append(String.format(prefix, this.currentLine(), "Missing = "));
			gotoNextDelimitator();
			return false;
		}
		nextAtom();
		if (!consumeExprAssign()) {
			errorReporter.append(String.format(prefix, this.currentLine(), "Missing = right side operator "));
			gotoNextDelimitator();
			return false;
		}
		return true;
	}

	private boolean consumeExprOr() {
		if (!consumeExprAnd())
			return false;
		if (!consumeExprOrResolved()) {
			errorReporter.append(String.format(prefix, this.currentLine(), "Invalid OR Expr\n"));
			gotoNextDelimitator();
			return false;
		}
		return true;
	}

	private boolean consumeExprOrResolved() {
		boolean er = false;
		if (currentAtomType() == Iduri.OR) {
			saveAtom();
			nextAtom();
			if (!consumeExprAnd()) {
				errorReporter.append(String.format(prefix, this.currentLine(), "Invalid Expr afrer OR op\n"));
				gotoNextDelimitator();
				er = true;
			} else if (!consumeExprOrResolved()) {
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

	private boolean consumeExprAnd() {
		if (!consumeExprEq())
			return false;
		if (!consumeExprAndResolved()) {
			errorReporter.append(String.format(prefix, this.currentLine(), "Invalid And Expr\n"));
			gotoNextDelimitator();
			return false;
		}
		return true;
	}

	private boolean consumeExprAndResolved() {
		boolean er = false;
		if (currentAtomType() == Iduri.AND) {
			saveAtom();
			nextAtom();
			if (!consumeExprEq()) {
				errorReporter.append(String.format(prefix, this.currentLine(), "Invalid Expr afrer AND op\n"));
				gotoNextDelimitator();
				er = true;
			} else if (!consumeExprAndResolved()) {
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

	private boolean consumeExprEq() {
		if (!consumeExpRel())
			return false;
		if (!consumeExprEqReolved()) {
			errorReporter.append(String.format(prefix, this.currentLine(), "Invalid Eq Expr\n"));
			gotoNextDelimitator();
			return false;
		}
		return true;
	}

	private boolean consumeExprEqReolved() {
		boolean er = false;
		if (currentAtomType() == Iduri.EQUAL || currentAtomType() == Iduri.NOTEQ) {
			saveAtom();
			nextAtom();
			if (!consumeExpRel()) {
				errorReporter.append(String.format(prefix, this.currentLine(), "Invalid Expr afrer ==!= op\n"));
				gotoNextDelimitator();
				er = true;
			} else if (!consumeExprEqReolved()) {
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

	private boolean consumeExpRel() {
		if (!consumeExprAdd())
			return false;
		if (!consumeExpRelResolved()) {
			errorReporter.append(String.format(prefix, this.currentLine(), "Invalid REl Expr\n"));
			gotoNextDelimitator();
			return false;
		}
		return true;
	}

	private boolean consumeExpRelResolved() {
		boolean er = false;
		if (currentAtomType() == Iduri.LESS || currentAtomType() == Iduri.GREATER || currentAtomType() == Iduri.LESSEQ
				|| currentAtomType() == Iduri.GREATEREQ) {
			saveAtom();
			nextAtom();
			if (!consumeExprAdd()) {
				errorReporter.append(String.format(prefix, this.currentLine(), "Invalid Expr afrer ><== op\n"));
				gotoNextDelimitator();
				er = true;
			} else if (!consumeExpRelResolved()) {
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

	private boolean consumeExprAdd() {
		if (!consumeExprMul())
			return false;
		if (!consumeExprAddResolved()) {
			errorReporter.append(String.format(prefix, this.currentLine(), "Invalid Add Expr\n"));
			gotoNextDelimitator();
			return false;
		}
		return true;
	}

	private boolean consumeExprAddResolved() {
		boolean er = false;
		if (currentAtomType() == Iduri.ADD || currentAtomType() == Iduri.SUB) {
			nextAtom();
			saveAtom();
			if (!consumeExprMul()) {
				errorReporter.append(String.format(prefix, this.currentLine(), "Invalid Expr afrer +- op\n"));
				gotoNextDelimitator();
				er = true;
			} else if (!consumeExprAddResolved()) {
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

	private boolean consumeExprMul() {
		if (!consumeExprCast())
			return false;
		if (!consumeExprMulResolved()) {
			errorReporter.append(String.format(prefix, this.currentLine(), "Invalid Mul Expr\n"));
			gotoNextDelimitator();
			return false;
		}
		return true;
	}

	private boolean consumeExprMulResolved() {
		boolean er = false;
		if (currentAtomType() == Iduri.MUL || currentAtomType() == Iduri.DIV) {
			saveAtom();
			nextAtom();
			if (!consumeExprCast()) {
				errorReporter.append(String.format(prefix, this.currentLine(), "Invalid Expr afrer */ op\n"));
				gotoNextDelimitator();
				er = true;
			} else if (!consumeExprMulResolved()) {
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

	private boolean consumeExprCast() {

		saveAtom();
		if (consumeExprCastS()) {
			popAtomFromStack();
			return true;
		}
		restoreSaveAtom();
		if (consumeExprUnary()) {
			popAtomFromStack();
			return true;
		}
		restoreAtom();
		return false;
	}

	private boolean consumeExprCastS() {
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
		if (!consumeExprCast()) {
			errorReporter.append(String.format(prefix, this.currentLine(), "Wrong Expr to cast\n"));
			gotoNextDelimitator();
			return false;
		}
		return true;
	}

	private boolean consumeExprUnary() {
		saveAtom();

		if (consumeExprUnaryL()) {
			popAtomFromStack();
			return true;
		}

		restoreSaveAtom();
		if (consumeExprPostfix()) {
			popAtomFromStack();
			return true;
		}

		restoreAtom();
		return false;
	}

	private boolean consumeExprUnaryL() {
		if (currentAtomType() == Iduri.SUB || currentAtomType() == Iduri.NOT) {
			nextAtom();

			if (!consumeExprUnary()) {
				errorReporter.append(String.format(prefix, this.currentLine(), "UNary Invalid ExprPostfix\n"));
				gotoNextDelimitator();
				return false;
			}
			return true;
		}
		return false;
	}

	private boolean consumeExprPostfix() {

		if (!consumeExprPrimary())
			return false;

		if (!consumeExprPostfixResolved()) {
			errorReporter.append(String.format(prefix, this.currentLine(), "Invalid ExprPrimary\n"));
			gotoNextDelimitator();
			return false;
		}

		return true;
	}

	private boolean consumeExprPostfixResolved() {

		saveAtom();
		if (consumeExprPostfixResolvedArray()) {
			popAtomFromStack();
			return true;
		}

		restoreSaveAtom();
		if (consumeExprPostfixResolvedStructField()) {
			popAtomFromStack();
			return true;
		}
		restoreAtom();
		return true;

	}

	private boolean consumeExprPostfixResolvedArray() {
		if (currentAtomType() != Iduri.LBRACKET)
			return false;
		nextAtom();

		if (!consumeExpr()) {
			errorReporter.append(String.format(prefix, this.currentLine(), "Invalid array index ID\n"));
			gotoNextDelimitator();
			return false;
		}

		if (currentAtomType() != Iduri.RBRACKET) {
			errorReporter.append(String.format(prefix, this.currentLine(), "Missing close ]\n"));
			gotoNextDelimitator();
			return false;
		}
		nextAtom();

		if (!consumeExprPostfixResolved()) {
			errorReporter.append(String.format(prefix, this.currentLine(), "Invalid ExprPostfixResolved\n"));
			gotoNextDelimitator();
			return false;
		}
		return true;
	}

	private boolean consumeExprPostfixResolvedStructField() {
		if (currentAtomType() != Iduri.DOT)
			return false;
		nextAtom();
		if (currentAtomType() != Iduri.ID) {
			errorReporter.append(String.format(prefix, this.currentLine(), "Invalid Struct ID field\n"));
			gotoNextDelimitator();
			return false;
		}
		nextAtom();

		if (!consumeExprPostfixResolved()) {
			errorReporter.append(String.format(prefix, this.currentLine(), "Invalid ExprPostfixResolved\n"));
			gotoNextDelimitator();
			return false;
		}
		return true;
	}

	private boolean consumeExprPrimary() {

		saveAtom();
		if (consumeExprPrimaryFctCall()) {
			popAtomFromStack();
			return true;
		}
		restoreAtom();

		if (currentAtomType() == Iduri.CT_INT) {
			nextAtom();
			return true;
		} else if (currentAtomType() == Iduri.CT_REAL) {
			nextAtom();
			return true;
		} else if (currentAtomType() == Iduri.CT_CHAR) {
			nextAtom();
			return true;
		} else if (currentAtomType() == Iduri.CT_SRING) {
			nextAtom();
			return true;
		}

		saveAtom();
		if (consumeExprPrimaryParntesis()) {
			popAtomFromStack();
			return true;
		}
		restoreAtom();

		return false;

	}

	public boolean consumeExprPrimaryFctCall() {
		if (currentAtomType() != Iduri.ID)
			return false;
		nextAtom();

		if (currentAtomType() == Iduri.LPAR) {
			nextAtom();

			if (consumeExpr()) {
				while (true) {
					if (currentAtomType() == Iduri.COMMA) {
						nextAtom();
						if (!consumeExpr()) {
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
			nextAtom();
		}
		return true;
	}

	public boolean consumeExprPrimaryParntesis() {

		if (currentAtomType() != Iduri.LPAR)
			return false;
		nextAtom();
		if (!consumeExpr()) {
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
		SymbolTable table;
		for (int i = 0; i <= 9; i++) {

			alex = new Lexical("CCodeTest/" + i + ".c");
			try {
				alex.compile();
				alex.hasErrors();
				table = new SymbolTable();
				sintactic = new Sintactic(table, alex.getAtoms());
				sintactic.compile();
				table.printSymbolTable();

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
