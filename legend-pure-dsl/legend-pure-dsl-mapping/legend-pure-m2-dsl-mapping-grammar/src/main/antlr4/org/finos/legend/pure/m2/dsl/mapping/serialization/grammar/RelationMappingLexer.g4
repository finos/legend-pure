lexer grammar RelationMappingLexer;

import M3CoreLexer;

RELATION_FUNCTION : '~func' ;
INLINE :            'Inline' ;

WHITESPACE:   Whitespace    ->  skip ;
COMMENT:      Comment       -> skip  ;
LINE_COMMENT: LineComment   -> skip  ;
