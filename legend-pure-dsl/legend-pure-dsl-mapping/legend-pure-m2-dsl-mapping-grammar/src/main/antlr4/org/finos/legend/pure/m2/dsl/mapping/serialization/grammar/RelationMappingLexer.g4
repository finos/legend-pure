lexer grammar RelationMappingLexer;

import M3CoreLexer;

RELATION_FUNCTION : '~func' ;

WHITESPACE:   Whitespace    ->  skip ;
COMMENT:      Comment       -> skip  ;
LINE_COMMENT: LineComment   -> skip  ;
