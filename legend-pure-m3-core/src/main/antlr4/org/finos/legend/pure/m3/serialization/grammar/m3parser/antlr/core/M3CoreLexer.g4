lexer grammar M3CoreLexer;

import M4Fragment;

MAPPING_SRC: '~src';
MAPPING_FILTER: '~filter';
MAPPING_GROUPBY: '~groupBy';
ENUMERATION_MAPPING: 'EnumerationMapping';

AGGREGATION_TYPE: 'composite' | 'shared' | 'none';

DSL_TEXT: '#' .*?  '#';
AS: 'as';
ALL: 'all';
ALL_VERSIONS: 'allVersions';
ALL_VERSIONS_IN_RANGE: 'allVersionsInRange';
ARROW: '->';
CURLY_BRACKET_OPEN: '{';
CURLY_BRACKET_CLOSE: '}';
BRACKET_OPEN: '[';
BRACKET_CLOSE: ']';
GROUP_OPEN: '(';
GROUP_CLOSE: ')';
COLON: ':';
DOT: '.';
DOLLAR: '$';
DOTDOT: '..';
END_LINE: ';';
NEW_SYMBOL: '^';
PIPE: '|';
TILDE: '~';
PATH_SEPARATOR: PathSeparator;

STRING:   String;
BOOLEAN: Boolean;
TRUE: True;
FALSE: False;
INTEGER: Integer;
FLOAT: Float;
DECIMAL: Decimal;
DATE: Date;
STRICTTIME: StrictTime;
LATEST_DATE: '%latest';

AND: '&&';
OR: '||';
NOT: '!';
COMMA: ',';
EQUAL: '=';TEST_EQUAL: '==';
TEST_NOT_EQUAL: '!=';
PERCENT: '%';
NATIVE : 'native';
FUNCTION: 'function';
IMPORT: 'import';
EXTENDS: 'extends';
PROJECTS: 'projects';
CLASS: 'Class';
ASSOCIATION: 'Association';
PROFILE: 'Profile';
ENUM: 'Enum';
MEASURE: 'Measure';
STEREOTYPES: 'stereotypes';
TAGS: 'tags';
LET: 'let';
ENFORCEMENT_LEVEL: 'Error' | 'Warn';
VALID_STRING: ValidString;
FILE_NAME: FileName;
FILE_NAME_END: FileNameEnd;

CAN_AGGREGATE: '~canAggregate';
GROUP_BY_FUNCTIONS: '~groupByFunctions';
AGGREGATE_VALUES: '~aggregateValues';
MAP_FN: '~mapFn';
AGGREGATE_FN: '~aggregateFn';

CONSTRAINT_OWNER: '~owner';
CONSTRAINT_EXTERNAL_ID: '~externalId';
CONSTRAINT_FUNCTION: '~function';
CONSTRAINT_ENFORCEMENT: '~enforcementLevel';
CONSTRAINT_MESSAGE: '~message';

WHITESPACE:   Whitespace        ->  skip ;
COMMENT:      Comment           -> skip  ;
LINE_COMMENT: LineComment       -> skip  ;

AT: '@';
PLUS: '+';
STAR: '*';
MINUS: '-';
DIVIDE: '/';
LESSTHAN: '<';
LESSTHANEQUAL: '<=';
GREATERTHAN: '>';
GREATERTHANEQUAL: '>=';