lexer grammar RelationalLexer;

import M3Lexer;

AND : 'and' ;
OR : 'or' ;
CONSTRAINT: 'constraint' ;
ASSOCIATION_MAPPING: 'AssociationMapping' ;
BUSINESS_MILESTONING : 'business' ;
BUSINESS_MILESTONING_FROM : 'BUS_FROM' ;
BUSINESS_MILESTONING_THRU : 'BUS_THRU' ;
OUT_IS_INCLUSIVE : 'OUT_IS_INCLUSIVE' ;
THRU_IS_INCLUSIVE : 'THRU_IS_INCLUSIVE' ;
INFINITY_DATE : 'INFINITY_DATE' ;
BUS_SNAPSHOT_DATE : 'BUS_SNAPSHOT_DATE';
PROCESSING_MILESTONING : 'processing';
PROCESSING_MILESTONING_IN : 'PROCESSING_IN';
PROCESSING_MILESTONING_OUT : 'PROCESSING_OUT';
DATABASE: 'Database';
DISTINCTCMD : '~distinct';
ENUMERATION_MAPPING: 'EnumerationMapping' ;
INLINE: 'Inline' ;

FILTER : 'Filter' ;
FILTERCMD : '~filter';
GROUPBYCMD : '~groupBy';
GROUP : '~groupBy';
INCLUDE : 'include' ;
ISNULL : 'is null';
ISNOTNULL : 'is not null';
JOIN : 'Join' ;
MILESTONING : 'milestoning' ;
MULTIGRAINFILTER : 'MultiGrainFilter' ;
MAINTABLECMD : '~mainTable';
NOT_EQUAL_2: '<>';
NOT_NULL : 'NOT NULL';
OTHERWISE: 'Otherwise' ;
PRIMARYKEY : 'PRIMARY KEY';
PRIMARYKEYCMD : '~primaryKey';
INTEGER: ('+' | '-')? (Digit)+;
FLOAT: ('+' | '-')? (Float)+;
QUOTED_STRING:   ('"' ( EscSeq | ~["\r\n] )*  '"' ) ;
SCOPE : 'scope' ;
SCHEMA: 'Schema';
TABLE : 'Table' ;
TARGET : '{target}' ;
VIEW : 'View' ;

