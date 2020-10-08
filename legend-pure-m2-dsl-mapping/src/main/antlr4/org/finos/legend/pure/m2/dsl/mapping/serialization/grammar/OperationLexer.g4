lexer grammar OperationLexer;

import M4Fragment;

GROUP_OPEN: '(';
GROUP_CLOSE: ')';
COMMA: ',';
PATH_SEPARATOR: '::';
END_LINE: ';';
VALID_STRING: ValidString;

WHITESPACE:   Whitespace    ->  skip ;
COMMENT:      Comment       -> skip  ;
LINE_COMMENT: LineComment   -> skip  ;
