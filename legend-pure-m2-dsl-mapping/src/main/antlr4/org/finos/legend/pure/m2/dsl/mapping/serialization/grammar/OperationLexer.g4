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
BRACKET_OPEN: '[';
BRACKET_CLOSE: ']';
CURLY_BRACKET_OPEN : '{' -> pushMode (ISLAND_BLOCK);
CURLY_BRACKET_CLOSE: '}';


mode ISLAND_BLOCK;
INNER_CURLY_BRACKET_OPEN : '{' -> pushMode (ISLAND_BLOCK);
CONTENT: (~[{}])+;
INNER_CURLY_BRACKET_CLOSE: '}' -> popMode;


