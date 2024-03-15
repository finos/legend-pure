lexer grammar TopAntlrLexer;

import M4Fragment;

CODE_BLOCK_START: Separator ValidString;

NON_HASH:                           ~[#];
HASH:                               '#';

fragment Separator : '\n###';