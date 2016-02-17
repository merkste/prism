/* Generated By:JavaCC: Do not edit this line. PrismParserConstants.java */
package parser;


/**
 * Token literal values and constants.
 * Generated by org.javacc.parser.OtherFilesGen#start()
 */
public interface PrismParserConstants {

  /** End of File. */
  int EOF = 0;
  /** RegularExpression Id. */
  int WHITESPACE = 1;
  /** RegularExpression Id. */
  int COMMENT = 2;
  /** RegularExpression Id. */
  int A = 3;
  /** RegularExpression Id. */
  int BOOL = 4;
  /** RegularExpression Id. */
  int CLOCK = 5;
  /** RegularExpression Id. */
  int CONST = 6;
  /** RegularExpression Id. */
  int CTMC = 7;
  /** RegularExpression Id. */
  int C = 8;
  /** RegularExpression Id. */
  int DOUBLE = 9;
  /** RegularExpression Id. */
  int DTMC = 10;
  /** RegularExpression Id. */
  int E = 11;
  /** RegularExpression Id. */
  int ENDINIT = 12;
  /** RegularExpression Id. */
  int ENDINVARIANT = 13;
  /** RegularExpression Id. */
  int ENDMODULE = 14;
  /** RegularExpression Id. */
  int ENDREWARDS = 15;
  /** RegularExpression Id. */
  int ENDSYSTEM = 16;
  /** RegularExpression Id. */
  int FALSE = 17;
  /** RegularExpression Id. */
  int FORMULA = 18;
  /** RegularExpression Id. */
  int FILTER = 19;
  /** RegularExpression Id. */
  int FUNC = 20;
  /** RegularExpression Id. */
  int F = 21;
  /** RegularExpression Id. */
  int GLOBAL = 22;
  /** RegularExpression Id. */
  int G = 23;
  /** RegularExpression Id. */
  int INIT = 24;
  /** RegularExpression Id. */
  int INVARIANT = 25;
  /** RegularExpression Id. */
  int I = 26;
  /** RegularExpression Id. */
  int INT = 27;
  /** RegularExpression Id. */
  int LABEL = 28;
  /** RegularExpression Id. */
  int MAX = 29;
  /** RegularExpression Id. */
  int MDP = 30;
  /** RegularExpression Id. */
  int MIN = 31;
  /** RegularExpression Id. */
  int MODULE = 32;
  /** RegularExpression Id. */
  int X = 33;
  /** RegularExpression Id. */
  int NONDETERMINISTIC = 34;
  /** RegularExpression Id. */
  int PMAX = 35;
  /** RegularExpression Id. */
  int PMIN = 36;
  /** RegularExpression Id. */
  int P = 37;
  /** RegularExpression Id. */
  int PROBABILISTIC = 38;
  /** RegularExpression Id. */
  int PROB = 39;
  /** RegularExpression Id. */
  int PTA = 40;
  /** RegularExpression Id. */
  int RATE = 41;
  /** RegularExpression Id. */
  int REWARDS = 42;
  /** RegularExpression Id. */
  int RMAX = 43;
  /** RegularExpression Id. */
  int RMIN = 44;
  /** RegularExpression Id. */
  int R = 45;
  /** RegularExpression Id. */
  int S = 46;
  /** RegularExpression Id. */
  int STOCHASTIC = 47;
  /** RegularExpression Id. */
  int SYSTEM = 48;
  /** RegularExpression Id. */
  int TRUE = 49;
  /** RegularExpression Id. */
  int U = 50;
  /** RegularExpression Id. */
  int VIEW = 51;
  /** RegularExpression Id. */
  int W = 52;
  /** RegularExpression Id. */
  int NOT = 53;
  /** RegularExpression Id. */
  int AND = 54;
  /** RegularExpression Id. */
  int OR = 55;
  /** RegularExpression Id. */
  int IMPLIES = 56;
  /** RegularExpression Id. */
  int IFF = 57;
  /** RegularExpression Id. */
  int RARROW = 58;
  /** RegularExpression Id. */
  int COLON = 59;
  /** RegularExpression Id. */
  int SEMICOLON = 60;
  /** RegularExpression Id. */
  int COMMA = 61;
  /** RegularExpression Id. */
  int DOTS = 62;
  /** RegularExpression Id. */
  int LPARENTH = 63;
  /** RegularExpression Id. */
  int RPARENTH = 64;
  /** RegularExpression Id. */
  int LBRACKET = 65;
  /** RegularExpression Id. */
  int RBRACKET = 66;
  /** RegularExpression Id. */
  int DLBRACKET = 67;
  /** RegularExpression Id. */
  int DRBRACKET = 68;
  /** RegularExpression Id. */
  int LBRACE = 69;
  /** RegularExpression Id. */
  int RBRACE = 70;
  /** RegularExpression Id. */
  int EQ = 71;
  /** RegularExpression Id. */
  int NE = 72;
  /** RegularExpression Id. */
  int LT = 73;
  /** RegularExpression Id. */
  int GT = 74;
  /** RegularExpression Id. */
  int DLT = 75;
  /** RegularExpression Id. */
  int DGT = 76;
  /** RegularExpression Id. */
  int LE = 77;
  /** RegularExpression Id. */
  int GE = 78;
  /** RegularExpression Id. */
  int PLUS = 79;
  /** RegularExpression Id. */
  int MINUS = 80;
  /** RegularExpression Id. */
  int TIMES = 81;
  /** RegularExpression Id. */
  int DIVIDE = 82;
  /** RegularExpression Id. */
  int PRIME = 83;
  /** RegularExpression Id. */
  int RENAME = 84;
  /** RegularExpression Id. */
  int QMARK = 85;
  /** RegularExpression Id. */
  int CARET = 86;
  /** RegularExpression Id. */
  int REG_INT = 87;
  /** RegularExpression Id. */
  int REG_DOUBLE = 88;
  /** RegularExpression Id. */
  int REG_IDENTPRIME = 89;
  /** RegularExpression Id. */
  int REG_IDENT = 90;
  /** RegularExpression Id. */
  int REG_QUOTED_IDENT = 91;
  /** RegularExpression Id. */
  int REG_QUOTED_STRING = 92;
  /** RegularExpression Id. */
  int PREPROC = 93;
  /** RegularExpression Id. */
  int LEXICAL_ERROR = 94;

  /** Lexical state. */
  int DEFAULT = 0;

  /** Literal token values. */
  String[] tokenImage = {
    "<EOF>",
    "<WHITESPACE>",
    "<COMMENT>",
    "\"A\"",
    "\"bool\"",
    "\"clock\"",
    "\"const\"",
    "\"ctmc\"",
    "\"C\"",
    "\"double\"",
    "\"dtmc\"",
    "\"E\"",
    "\"endinit\"",
    "\"endinvariant\"",
    "\"endmodule\"",
    "\"endrewards\"",
    "\"endsystem\"",
    "\"false\"",
    "\"formula\"",
    "\"filter\"",
    "\"func\"",
    "\"F\"",
    "\"global\"",
    "\"G\"",
    "\"init\"",
    "\"invariant\"",
    "\"I\"",
    "\"int\"",
    "\"label\"",
    "\"max\"",
    "\"mdp\"",
    "\"min\"",
    "\"module\"",
    "\"X\"",
    "\"nondeterministic\"",
    "\"Pmax\"",
    "\"Pmin\"",
    "\"P\"",
    "\"probabilistic\"",
    "\"prob\"",
    "\"pta\"",
    "\"rate\"",
    "\"rewards\"",
    "\"Rmax\"",
    "\"Rmin\"",
    "\"R\"",
    "\"S\"",
    "\"stochastic\"",
    "\"system\"",
    "\"true\"",
    "\"U\"",
    "\"view\"",
    "\"W\"",
    "\"!\"",
    "\"&\"",
    "\"|\"",
    "\"=>\"",
    "\"<=>\"",
    "\"->\"",
    "\":\"",
    "\";\"",
    "\",\"",
    "\"..\"",
    "\"(\"",
    "\")\"",
    "\"[\"",
    "\"]\"",
    "\"[[\"",
    "\"]]\"",
    "\"{\"",
    "\"}\"",
    "\"=\"",
    "\"!=\"",
    "\"<\"",
    "\">\"",
    "\"<<\"",
    "\">>\"",
    "\"<=\"",
    "\">=\"",
    "\"+\"",
    "\"-\"",
    "\"*\"",
    "\"/\"",
    "\"\\\'\"",
    "\"<-\"",
    "\"?\"",
    "\"^\"",
    "<REG_INT>",
    "<REG_DOUBLE>",
    "<REG_IDENTPRIME>",
    "<REG_IDENT>",
    "<REG_QUOTED_IDENT>",
    "<REG_QUOTED_STRING>",
    "<PREPROC>",
    "<LEXICAL_ERROR>",
  };

}
