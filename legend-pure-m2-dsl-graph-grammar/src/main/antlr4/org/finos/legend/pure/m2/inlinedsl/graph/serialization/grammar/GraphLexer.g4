lexer grammar GraphLexer;

import M4Fragment;

BRACKET_OPEN : '[';
BRACKET_CLOSE : ']';

CURLY_BRACKET_OPEN : '{';
CURLY_BRACKET_CLOSE : '}';

GROUP_OPEN : '(';
GROUP_CLOSE : ')';

COMMA : ',';
COLON : ':';
DOLLAR : '$';
DOT : '.';

PLUS : '+';
MINUS : '-';

SUBTYPE_START : '->subType(@';

STRING : String;
INTEGER : Integer;
FLOAT : Float;
DECIMAL : Decimal;
BOOLEAN: TRUE | FALSE;
TRUE: True;
FALSE: False;
DATE: Date;
LATEST_DATE: '%latest';

VALID_STRING: ValidString;
PATH_SEPARATOR: PathSeparator;

WHITESPACE:   Whitespace        -> skip;
COMMENT:      Comment           -> skip;
LINE_COMMENT: LineComment       -> skip;