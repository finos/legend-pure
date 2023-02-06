lexer grammar MappingLexer;

import M4Fragment;

COLON : ':';
COMMA : ',';
PATH_SEPARATOR: PathSeparator;
BRACKET_OPEN: '[';
BRACKET_CLOSE: ']';
GROUP_OPEN: '(';
GROUP_CLOSE: ')';
CURLY_BRACKET_OPEN : '{' -> pushMode (ISLAND_BLOCK);
CURLY_BRACKET_CLOSE: '}';
CARET_OPEN: '<';
CARET_CLOSE: '>';
IMPORT: 'import';
MAPPING : 'Mapping';
INCLUDE : 'include';
TESTS: 'MappingTests';
TEST_QUERY:  '~query';
TEST_INPUT_DATA: '~inputData';
TEST_ASSERT: '~assert';
ARROW : '->';
STAR: '*';
END_LINE: ';';
EXTEND : 'extends';
VALID_STRING: ValidString;
WHITESPACE:   Whitespace    ->  skip ;
COMMENT:      Comment -> skip  ;
LINE_COMMENT: LineComment -> skip  ;

STRING:   String;

mode ISLAND_BLOCK;
INNER_CURLY_BRACKET_OPEN : '{' -> pushMode (ISLAND_BLOCK);
CONTENT: (~[{}])+;
INNER_CURLY_BRACKET_CLOSE: '}' -> popMode;