lexer grammar TopAntlrLexer;

import M4Fragment;

CODE_BLOCK_START: Separator ValidString;
CODE : ~[];

fragment Separator : '\n###';