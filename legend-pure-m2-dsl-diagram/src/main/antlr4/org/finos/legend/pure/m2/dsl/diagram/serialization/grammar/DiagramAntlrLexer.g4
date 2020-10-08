lexer grammar DiagramAntlrLexer;

import M4Fragment;

DIAGRAM: 'Diagram';

TYPE_VIEW : 'TypeView';
ASSOCIATION_VIEW : 'AssociationView';
PROPERTY_VIEW : 'PropertyView';
GENERALIZATION_VIEW : 'GeneralizationView';
GEOMETRY : 'Geometry';
TYPE : 'type'(Assign);
ASSOCIATION : 'association'(Assign);
PROPERTY : 'property'(Assign);
STEREOTYPES_VISIBLE : 'stereotypesVisible'(Assign);
ATTRIBUTES_VISIBLE : 'attributesVisible'(Assign);
ATTRIBUTE_STEREOTYPES_VISIBLE : 'attributeStereotypesVisible'(Assign);
ATTRIBUTE_TYPES_VISIBLE : 'attributeTypesVisible'(Assign);
NAME_VISIBLE : 'nameVisible'(Assign);
COLOR : 'color'(Assign);
LINE_WIDTH : 'lineWidth'(Assign);
LINE_STYLE : 'lineStyle'(Assign);
POSITION : 'position'(Assign);
POINTS : 'points'(Assign);
WIDTH : 'width'(Assign);
HEIGHT : 'height'(Assign);
LABEL : 'label'(Assign);
SOURCE : 'source'(Assign);
TARGET : 'target'(Assign);
PROP_POSITION : 'propertyPosition'(Assign);
MULT_POSITION : 'multiplicityPosition'(Assign);
SOURCE_PROP_POSITION : 'sourcePropertyPosition'(Assign);
SOURCE_MULT_POSITION : 'sourceMultiplicityPosition'(Assign);
TARGET_PROP_POSITION : 'targetPropertyPosition'(Assign);
TARGET_MULT_POSITION : 'targetMultiplicityPosition'(Assign);
IMPORT : 'import';

CURLY_BRACKET_OPEN: '{';
CURLY_BRACKET_CLOSE: '}';
BRACKET_OPEN: '[';
BRACKET_CLOSE: ']';
GROUP_OPEN: '(';
GROUP_CLOSE: ')';

PATH_SEPARATOR: '::';
DOT: '.';
COMMA: ',';
END_LINE: ';';
STRING:   ('\'' ( EscSeq | ~['\r\n] )*  '\'' ) ;
COLOR_STRING : '#'(HexDigit)(HexDigit)(HexDigit)(HexDigit)(HexDigit)(HexDigit);
BOOLEAN: Boolean;
TRUE: True;
FALSE: False;
INTEGER: (Digit)+;
FLOAT : ('+' | '-')? (Digit)* '.' (Digit)+ ( ('e' | 'E') ('+' | '-')? (Digit)+)? ('f' | 'F')?;
VALID_STRING: (Letter | Digit | '_' ) (Letter | Digit | '_' | '$')*;

WHITESPACE:   [ \r\t\n]+    ->  skip ;
COMMENT:      '/*' .*? '*/' -> skip  ;
LINE_COMMENT: '//' ~[\r\n]* -> skip  ;

STAR: '*';