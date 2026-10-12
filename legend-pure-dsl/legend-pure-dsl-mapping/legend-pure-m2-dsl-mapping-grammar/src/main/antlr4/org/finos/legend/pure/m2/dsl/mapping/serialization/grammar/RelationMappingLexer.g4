lexer grammar RelationMappingLexer;

import M3CoreLexer;

RELATION_FUNCTION : '~func' ;
RELATION_PRIMARY_KEY : '~primaryKey' ;
INLINE :            'Inline' ;

WHITESPACE:   Whitespace    ->  skip ;
COMMENT:      Comment       -> skip  ;
LINE_COMMENT: LineComment   -> skip  ;
