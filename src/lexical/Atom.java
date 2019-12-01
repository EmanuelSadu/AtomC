package lexical;



public class Atom {

	
	public enum Iduri {START,ERROR,ID,INT,REAL,RANGE,COMMENT,STRING_CONST,CHAR_CONST,PLUS,MINUS,MUL,DIV,
		EQUAL,LPAR,RPAR,LBRACKET,RBRACKET,SEMICOLON,COMMA,OPLE,OP_DIF,OPl,OPHE,OPH,ATR,COLON,DOT,AND,BEGIN,CASE,CHAR,CONST,DO,DOWNTO,ELSE,END,FOR,FUNCTION,IF,INTEGER,MOD,NOT,OF,OR, PROCEDURE,PROGRAM,REPEAT,THEN,UNTIL,VAR,WHILE,ARRAY,RECORD,TO,STEP,OTHERWISE,PRINT,READ, LINECOMMENT, BREAK, VOID, DOUBLE, RETURN,
		STRUCT, RACC, LACC, ADD, SUB,ASSIGN,LESS,GREATER,LESSEQ,GREATEREQ,CT_INT,CT_REAL,CT_CHAR,CT_SRING,NOTEQ, ANDBIT, ORBIT};

	
	public enum IntType {ZECIMAL,OCTAL,HEXA,REAL}
	
	 ; //linia curenta in fisier
	
	
	public Iduri type;
	public IntType intType=null;
	public String text;
	public Integer intreg;
	public Double real;
	public int myLine;
	public boolean expectingEscape = false;
	public boolean error = false;
	public String errorMessage = "";
	public boolean exponentPrsent =false;
	
	public Atom(int line) {
		type =Iduri.END;
		text="";
		myLine = line;
	}
	public Atom(Iduri id,int line) {
		type =id;
		myLine=line;
	}
	@Override
	public String toString() {
		String Er = ((error) ? "\tERROR["+errorMessage+"]" : "");
		String information= "";
		if(intType != null && error==false) {
			if(intType == IntType.REAL) {
				real = Double.parseDouble(text);
				information = " -> "+real;
			}else {
				intreg = Integer.decode(text);
				information = " -> "+intreg;
			}
		}

		return type +"["+myLine+"]"+ Er+ "\n"+"CODE[" +text+"] "+ information ;
	}
	
	public boolean isWrong() {
		return error;
	}
	public void errorMessage() {
		 System.out.println("Error at line "+myLine+" : "+errorMessage);
	}
	public void setError(String string) {
		error = true;
		errorMessage=string;
		
	}

}
