grammar TreePath;

treepath: root EOF
;

root: qualifiedClassName ('[' valueSpecifications ']')? alias? stereoTypes? taggedValues?
classBody
;

classBody:
'{'
    simpleProperties?
    (newComplexProperty | complexProperty )*
'}'
;

simpleProperties: ALL | ((INCLUDE | EXCLUDE) ('[' simpleProperty (',' simpleProperty)* ']'))
;

simpleProperty: property stereoTypes? taggedValues?
;

complexProperty: property ('[' valueSpecifications ']')? alias? stereoTypes? taggedValues? classBody?
;

newComplexProperty: '>' property '[' valueSpecifications ']' alias? stereoTypes? taggedValues? classBody?
;

valueSpecifications: valueSpecification (';' valueSpecification)*
;

valueSpecification: '$'* .*?
;

property: Identifier ('(' (parameterType (',' parameterType)*)? ')')*
;

parameterType: literalType | enumType
;

qualifiedClassName: (packagePath)? Identifier (typeParameters)?
;

stereoTypes: '<<' stereoType ( ',' stereoType)* '>>'
;

stereoType:  profile '.' Identifier
;

taggedValues: '{' taggedValue (',' taggedValue)* '}'
;

taggedValue: profile '.' Identifier '=' STRING
;

profile: packagePath? Identifier
;

packagePath: (Identifier '::')+
;

literalType: primitve multiplicity
;

primitve: 'String' | 'Number' | 'Integer' | 'Float' | 'Decimal' | 'Boolean' | 'Date' | 'StrictDate' | 'DateTime' | 'StrictTime'
;

enumType: qualifiedClassName multiplicity
;

multiplicity: '[' ( (fromMultiplicity '..')? toMultiplicity )  ']'
;

fromMultiplicity: INTEGER;

toMultiplicity: INTEGER | '*';

typeParameters: '<' Identifier (',' Identifier)*  '>'
;

Identifier: LETTER (LETTER|DIGIT|'_')*
;

alias : 'as' qualifiedClassName;

ALL: '*';

INCLUDE: '+';

EXCLUDE: '-';

STRING:   '"' ( EscapeSequence | ~('\\'|'"') )* '"'
               |  '\'' ( EscapeSequence | ~('\\'|'\'') )* '\'';
BOOLEAN: 'true' | 'false';
INTEGER: (DIGIT)+;
FLOAT: (DIGIT)* '.' (DIGIT)+ (('e' | 'E') ('+' | '-')? (DIGIT)+)? ('f' | 'F')?;
DECIMAL: ((DIGIT)* '.' (DIGIT)+ | (DIGIT)+) (('e' | 'E') (('+' | '-'))? (DIGIT)+)? ('d' | 'D');
DATE: '%' ('-')? (DIGIT)+ ('-'(DIGIT)+ ('-'(DIGIT)+ ('T' DATE_TIME (DATE_TZ)?)?)?)?;
STRICTTIME: '%' DATE_TIME;
DATE_TIME: (DIGIT)+ (':'(DIGIT)+ (':'(DIGIT)+ ('.'(DIGIT)+)?)?)?;
DATE_TZ: ('+'|'-')(DIGIT)(DIGIT)(DIGIT)(DIGIT);

WHITESPACE:   [ \r\t\n]+    -> skip ;
COMMENT:      '/*' .*? '*/' -> skip ;
LINE_COMMENT: '//' ~[\r\n]* -> skip ;

fragment
EscapeSequence
    :   '\\' ('b' | 't'| 'n' | 'f' | 'r' | '"' | '\'' | '\\');

fragment 
LETTER : [A-Za-z];

fragment 
DIGIT : [0-9];
