lexer grammar M4AntlrLexer;

import M4Fragment;

STRING : String;
DATE : Date;
STRICTTIME : StrictTime;
SELF : 'self';
COMMA: ',';
BRACKET_OPEN: '[';
BRACKET_CLOSE: ']';
CURLY_BRACKET_OPEN: '{';
CURLY_BRACKET_CLOSE: '}';
CARET: '^';
COLON: ':';
ENDLINE: ';';
DOT: '.';
BOOLEAN: Boolean;
TRUE: True;
FALSE: False;
INTEGER: ('+' | '-')? Integer;
FLOAT: ('+' | '-')? Float;
VALID_STRING: ValidString;
VALID_STRING_EXT: '?['(Letter | Digit | '_' | '.' | '/')+;
AT: '@';
SOURCE_INFO_CLOSE: ']?';

WHITESPACE:   Whitespace        -> skip  ;
COMMENT:      Comment           -> skip  ;
LINE_COMMENT: LineComment       -> skip  ;