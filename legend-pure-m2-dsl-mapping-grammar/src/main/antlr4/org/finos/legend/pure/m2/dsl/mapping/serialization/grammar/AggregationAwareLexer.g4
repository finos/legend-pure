lexer grammar AggregationAwareLexer;

import M4Fragment;

COMMA : ',';
COLON : ':';
GROUP_OPEN : '(';
GROUP_CLOSE : ')';
BRACKET_OPEN : '[';
BRACKET_CLOSE : ']';
VIEWS : 'Views';
MODEL_OP : '~modelOperation';
AGG_MAP : '~aggregateMapping';
MAIN_MAP : '~mainMapping';
VALID_STRING: ValidString;

CURLY_BRACKET_OPEN : '{' -> pushMode (ISLAND_BLOCK);

WHITESPACE:   Whitespace    ->  skip ;
COMMENT:      Comment -> skip  ;
LINE_COMMENT: LineComment -> skip  ;

mode ISLAND_BLOCK;
CONTENT: ~('}')+;
CURLY_BRACKET_CLOSE: '}' -> popMode;


