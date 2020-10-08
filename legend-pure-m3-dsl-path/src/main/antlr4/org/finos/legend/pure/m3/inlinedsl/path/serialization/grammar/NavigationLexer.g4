lexer grammar NavigationLexer;

import M4Fragment;

SEPARATOR : '/' ;
GROUP_OPEN: '(';
GROUP_CLOSE: ')';
BRACKET_OPEN: '[';
BRACKET_CLOSE: ']';
COMMA : ',' ;
DOT: '.' ;
EXCLAMATION : '!' ;
STRING: String ;
DATE: Date;
LATEST_DATE: '%latest' ;
BOOLEAN: TRUE | FALSE ;
TRUE: True ;
FALSE: False ;
INTEGER: ('+'|'-')? Integer ;
FLOAT: ('+'|'-')? Float ;
VALID_STRING: ValidString ;
VALID_STRING_TYPE: (Letter | Digit | '_' ) (Letter | Digit | '_' | '$' | '<' | '>')* ;
PATH_SEPARATOR: PathSeparator;

WHITESPACE:   Whitespace        -> skip ;
COMMENT:      Comment           -> skip ;
LINE_COMMENT: LineComment       -> skip ;
