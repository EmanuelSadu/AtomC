package lexical;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;

import lexical.Atom.Iduri;
import lexical.Atom.IntType;

public class Lexical {

	private List<Atom> atoms = new ArrayList<Atom>();
	private File sourceFile;
	private List<String> formatParams = Arrays
			.asList(new String[] { "\\a", "\\b", "\\f", "\\n", "\\r", "\\t", "\\v", "\\\\", "\\'", "\\\"", "\\?" });
	private List<String> validHexa = Arrays.asList(new String[] { "0", "1", "2", "3", "4", "5", "6", "7", "8", "9", "a",
			"A", "b", "B", "c", "C", "d", "D", "e", "E", "f", "F" });
	public int line = 1;

	public Lexical(String source) {
		sourceFile = new File(source);
	}

	public List<Atom> getAtoms() {
		return Collections.unmodifiableList(atoms);
	}

	public void printAtoms() {
		System.out.println("Lines: " + line);
		for (Atom at : atoms)
			System.out.println(at.toString());

	}

	public boolean hasErrors() {
		boolean anyErr = false;

		for (Atom at : atoms) {
			if (at.isWrong()) {
				anyErr = true;
				at.errorMessage();
			}
		}
		return anyErr;
	}

	public void compile() throws FileNotFoundException {

		Atom tmp = new Atom(line);
		FileReader fr = new FileReader(sourceFile); // Creation of File Reader object
		System.out.println("Lexical compile of " + sourceFile);
		try (BufferedReader br = new BufferedReader(fr)) { // Creation of BufferedReader object
			int c = 0;
			while ((c = br.read()) != -1) // Read char by Char
			{
				Character character = (char) c;
				if (character == Character.LINE_SEPARATOR) {
					line++;
				}

				tmp = determinAtom(tmp, character);
			}
			// if(tmp.type)
			atoms.add(new Atom(line));
		} catch (Exception e) {
			e.printStackTrace();
		}
	}

	private Atom determinAtom(Atom atom, Character c) {

		switch (atom.type) {

		case ID:
			if (Character.isAlphabetic(c) || c == '_' || Character.isDigit(c)) {
				atom.text += c;
				if (isKeyWord(atom)) {
					atoms.add(atom);
					atom = new Atom(line);
				}
				break;
			} else {
				atoms.add(atom);
				atom = determinAtom(new Atom(line), c);
				break;
			}

		case CT_CHAR:

			if (atom.expectingEscape == true) {
				if (!formatParams.contains("\\" + c)) // ERROR HERE
					atom.setError("Invalid char formater");

				atom.text += formatString("\\" + c);
				atom.expectingEscape = false;
				break;
			}
			if (c == '\\' && atom.expectingEscape == false) {
				atom.expectingEscape = true;
				break;
			}

			if (c == '\'') {
				atoms.add(atom);
				atom = new Atom(line);
				break;
			}
			if (atom.text.length() > 1)
				atom.setError("Invalid char");

			atom.text += c;
			break;

		case CT_SRING:
			if (atom.expectingEscape == true) {
				if (!formatParams.contains("\\" + c)) // ERROR HERE
					atom.setError("Invalid string formater");
				atom.text += formatString("\\" + c);
				atom.expectingEscape = false;
				break;
			}
			if (c == '\\' && atom.expectingEscape == false) {
				atom.expectingEscape = true;
				break;
			}

			if (c == '\"') {
				atoms.add(atom);
				atom = new Atom(line);
				break;
			}

			atom.text += c;
			break;

		case CT_INT:
			if (atom.intType == null) {
				if (c == 'x') {
					atom.intType = IntType.HEXA;
					atom.text += c;
					break;
				} else if (c == '.') {
					atom.type = Iduri.CT_REAL;
					atom.intType = IntType.REAL;
					atom.text += c;
					break;
				} else if (c == 'e' || c == 'E') {
					atom.type = Iduri.CT_REAL;
					atom.intType = IntType.REAL;
					atom.exponentPrsent = true;
					atom.text += c;
					break;
				} else if (Character.isDigit(c)) {
					if (Integer.parseInt("" + c) <= 7) {
						if (atom.text.startsWith("0")) {
							atom.intType = IntType.OCTAL;
							atom.text += c;
						} else {
							atom.intType = IntType.ZECIMAL;
							atom.text += c;
						}
					} else {
						atom.intType = IntType.ZECIMAL;
						atom.text += c;
					}
					break;
				}
				atom.intType = IntType.ZECIMAL;
				atoms.add(atom);
				atom = determinAtom(new Atom(line), c);
				break;
			}

			if (atom.intType == IntType.ZECIMAL) {

				if (c == '.') {
					atom.type = Iduri.CT_REAL;
					atom.intType = IntType.REAL;
					atom.text += c;
					break;
				}
				if (c == 'e' || c == 'E') {
					atom.type = Iduri.CT_REAL;
					atom.intType = IntType.REAL;
					atom.exponentPrsent = true;
					atom.text += c;
					break;
				}

				if (!Character.isDigit(c)) {
					atoms.add(atom);
					atom = determinAtom(new Atom(line), c);
					break;
				}
			}

			if (atom.intType == IntType.HEXA) {
				if (!validHexa.contains("" + c)) {
					atoms.add(atom);
					atom = determinAtom(new Atom(line), c);
					break;
				}
				atom.text += c;
				;
				break;
			}

			if (atom.intType == IntType.OCTAL) {

				if (!Character.isDigit(c)) {
					atoms.add(atom);
					atom = determinAtom(new Atom(line), c);
					break;
				}

				if (Integer.parseInt("" + c) > 7) {
					if (atom.text.startsWith("0")) {
						atom.setError("Invalid number, assumed Octal");
					} else
						atom.intType = IntType.ZECIMAL;
				}
				atom.text += c;
				break;

			}
			atom.text += c;
			break;

		case CT_REAL:

			if (c == '.') {
				atom.intType = IntType.REAL;
				atom.text += c;
				break;
			}
			if (c == 'e' || c == 'E') {
				if (atom.exponentPrsent == true) {
					atom.setError("Invalid Real number");
				}
				atom.exponentPrsent = true;
				atom.intType = IntType.REAL;
				atom.exponentPrsent = true;
				atom.text += c;
				break;
			}

			if (c == '+' || c == '-') {
				if (atom.text.endsWith("e") || atom.text.endsWith("E")) {
					atom.text += c;
					break;
				} else {
					atoms.add(atom);
					atom = determinAtom(new Atom(line), c);
					break;
				}
			}
			if (!Character.isDigit(c)) {
				atoms.add(atom);
				atom = determinAtom(new Atom(line), c);
				break;
			}
			atom.text += c;
			break;

		case END:

			if (c == Character.LINE_SEPARATOR)
				atom.myLine++;

			if (Character.isWhitespace(c))
				break;

			// SPECIL

			if (c == '/') {
				atom.type = Iduri.DIV;
				break;
			}

			if (c == '\'') {
				atom.type = Iduri.CT_CHAR;
				break;
			}
			if (c == '\"') {
				atom.type = Iduri.CT_SRING;
				break;
			}

			if (Character.isAlphabetic(c) || c == '_') {
				atom.type = Iduri.ID;
				atom.text += c;
				break;
			} else if (Character.isDigit(c)) {
				atom.type = Iduri.CT_INT;
				atom.text += c;
			}

		// DELIMITATORI
		{
			if (c == ';') {
				atom.text += ';';
				atom.type = Iduri.SEMICOLON;
				atoms.add(atom);
				atom = new Atom(line);
				break;
			}
			if (c == ',') {
				atom.text += ',';
				atom.type = Iduri.COMMA;
				atoms.add(atom);
				atom = new Atom(line);
				break;
			}
			if (c == '(') {
				atom.text += '(';
				atom.type = Iduri.LPAR;
				atoms.add(atom);
				atom = new Atom(line);
				break;
			}
			if (c == ')') {
				atom.text += ')';
				atom.type = Iduri.RPAR;
				atoms.add(atom);
				atom = new Atom(line);
				break;
			}
			if (c == '[') {
				atom.text += '[';
				atom.type = Iduri.LBRACKET;
				atoms.add(atom);
				atom = new Atom(line);
				break;
			}
			if (c == ']') {
				atom.text += ']';
				atom.type = Iduri.RBRACKET;
				atoms.add(atom);
				atom = new Atom(line);
				break;
			}
			if (c == '{') {
				atom.text += '{';
				atom.type = Iduri.LACC;
				atoms.add(atom);
				atom = new Atom(line);
				break;
			}
			if (c == '}') {
				atom.text += '}';
				atom.type = Iduri.RACC;
				atoms.add(atom);
				atom = new Atom(line);
				break;
			}

		}
		// operatori
		{
			if (c == '+') {
				atom.text += '+';
				atom.type = Iduri.ADD;
				atoms.add(atom);
				atom = new Atom(line);
				break;
			}
			if (c == '-') {
				atom.text += '-';
				atom.type = Iduri.SUB;
				atoms.add(atom);
				atom = new Atom(line);
				break;
			}
			if (c == '*') {
				atom.text += '*';
				atom.type = Iduri.MUL;
				atoms.add(atom);
				atom = new Atom(line);
				break;
			}

			if (c == '!') {
				atom.text += '!';
				atom.type = Iduri.NOT;
				break;
			}
			if (c == '=') {
				atom.text += '=';
				atom.type = Iduri.ASSIGN;
				break;
			}
			if (c == '<') {
				atom.text += '<';
				atom.type = Iduri.LESS;

				break;
			}
			if (c == '>') {
				atom.text += '>';
				atom.type = Iduri.GREATER;
				break;
			}

			if (c == '&') {
				atom.text += '&';
				atom.type = Iduri.ANDBIT;
				break;
			}
			if (c == '|') {
				atom.text += '|';
				atom.type = Iduri.ORBIT;

				break;
			}
			if (c == '>') {
				atom.text += '>';
				atom.type = Iduri.GREATER;
				break;
			}

			if (c == '.') {
				atom.text += '.';
				atom.type = Iduri.DOT;
				atoms.add(atom);
				atom = new Atom(line);
				break;
			}
		}
			break;

		case ASSIGN:
			if (c == '=') {
				atom.text = "==";
				atom.type = Iduri.EQUAL;
				atoms.add(atom);
				atom = new Atom(line);
				break;
			} else {
				atoms.add(atom);
				atom = determinAtom(new Atom(line), c);
				break;
			}
		case NOTEQ:
			if (c == '=') {
				atom.text = "!=";
				atom.type = Iduri.NOTEQ;
				atoms.add(atom);
				atom = new Atom(line);
				break;
			} else {
				atoms.add(atom);
				atom = determinAtom(new Atom(line), c);
				break;
			}
		case ANDBIT:
			if (c == '&') {
				atom.text = "&&";
				atom.type = Iduri.AND;
				atoms.add(atom);
				atom = new Atom(line);
				break;
			} else {
				atoms.add(atom);
				atom = determinAtom(new Atom(line), c);
				break;
			}
		case ORBIT:
			if (c == '|') {
				atom.text = "||";
				atom.type = Iduri.OR;
				atoms.add(atom);
				atom = new Atom(line);
				break;
			} else {
				atoms.add(atom);
				atom = determinAtom(new Atom(line), c);
				break;
			}

		case LESS:
			if (c == '=') {
				atom.text = "<=";
				atom.type = Iduri.LESSEQ;
				atoms.add(atom);
				atom = new Atom(line);
				break;
			} else {
				atoms.add(atom);
				atom = determinAtom(new Atom(line), c);
				break;
			}
		case GREATER:
			if (c == '=') {
				atom.text = ">=";
				atom.type = Iduri.GREATEREQ;
				atoms.add(atom);
				atom = new Atom(line);
				break;
			} else {
				atoms.add(atom);
				atom = determinAtom(new Atom(line), c);
				break;
			}
		case DIV:
			if (c == '*') {
				atom.type = Iduri.COMMENT;
				break;
			} else if (c == '/') {
				atom.type = Iduri.LINECOMMENT;
				break;
			} else {
				atom.text += '/';
				atoms.add(atom);
				atom = determinAtom(new Atom(line), c);
				break;
			}

		case COMMENT:
			atom.text += c;
			if (atom.text.endsWith("*/")) {
				atom.text.replaceAll("\\*\\/", "");
				// atoms.add(atom);
				atom = new Atom(line);
				break;
			}
			break;

		case LINECOMMENT:

			if (c == Character.LINE_SEPARATOR || c == null) {
				// atoms.add(atom);
				atom = new Atom(line);
				break;
			}
			atom.text += c;
			break;
		default:
			break;

		}

		return atom;
	}

	private static String formatString(String end) {

		int c;
		switch (end) {

		case "\\'":
			c = 0x27;
			break;
		case "\\\"":
			c = 0x22;
			break;

		case "\\?":
			c = 0x3f;
			break;

		case "\\\\":
			c = 0x5c;
			break;
		case "\\a":
			c = 0x07;
			break;

		case "\\b":
			c = 0x08;
		case "\\f":
			c = 0x0c;
			break;

		case "\\n":
			c = 0x0a;
			break;
		case "\\r":
			c = 0x0d;
			break;
		case "\\t":
			c = 0x09;
			break;

		case "\\v":
			c = 0x0b;
			break;
		default:
			// System.out.print("blsfaf");
			return end;
		}

		return "" + (char) c;

	}

	public static boolean isKeyWord(Atom atom) {

		switch (atom.text) {

		case "break":
			atom.type = Iduri.BREAK;
			break;
		case "char":
			atom.type = Iduri.CHAR;
			break;
		case "double":
			atom.type = Iduri.DOUBLE;
			break;
		case "else":
			atom.type = Iduri.ELSE;
			break;
		case "for":
			atom.type = Iduri.FOR;
			break;
		case "while":
			atom.type = Iduri.WHILE;
			break;
		case "if":
			atom.type = Iduri.IF;
			break;
		case "int":
			atom.type = Iduri.INT;
			break;
		case "return":
			atom.type = Iduri.RETURN;
			break;
		case "struct":
			atom.type = Iduri.STRUCT;
			break;
		case "void":
			atom.type = Iduri.VOID;
			break;

		default:
			return false;
		}

		return !(atom.type.equals(Iduri.ID));

	}

	public static void main(String[] args) {

		for (int i = 0; i <= 9; i++) {

			Lexical alex = new Lexical("CCodeTest/" + i + ".c");
			try {
				alex.compile();
				alex.printAtoms();
			} catch (FileNotFoundException e) {

				e.printStackTrace();
			}

		}
	}

}
