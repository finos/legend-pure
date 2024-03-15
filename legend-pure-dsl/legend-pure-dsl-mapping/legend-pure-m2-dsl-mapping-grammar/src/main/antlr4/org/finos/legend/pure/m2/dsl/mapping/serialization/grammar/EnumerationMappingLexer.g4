lexer grammar EnumerationMappingLexer;

import M4Fragment;

COLON: ':';
PATH_SEPARATOR: '::';
BRACKET_OPEN: '[';
BRACKET_CLOSE: ']';
GROUP_OPEN: '(';
GROUP_CLOSE: ')';
EQUAL: '=';
DOT: '.';
QUOTE: '\'';
COMMA: ',';
INTEGER: ('+' | '-')? (Digit)+;
VALID_STRING: ValidString;
STRING: String;

WHITESPACE:   Whitespace    ->  skip ;
COMMENT:      Comment       -> skip  ;
LINE_COMMENT: LineComment   -> skip  ;
