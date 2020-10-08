parser grammar NavigationParser;

options
{
    tokenVocab = NavigationLexer;
}

definition: SEPARATOR genericType (propertyWithParameters)* (name)?
        EOF
;

propertyWithParameters: SEPARATOR VALID_STRING (GROUP_OPEN (parameter (COMMA parameter)*)? GROUP_CLOSE)?
;

parameter: scalar | collection
;

collection: BRACKET_OPEN (scalar (COMMA scalar)*)? BRACKET_CLOSE
;

scalar: atomic | enumStub
;

enumStub : VALID_STRING DOT VALID_STRING
;

atomic: BOOLEAN | INTEGER | FLOAT | STRING | DATE | LATEST_DATE
;

name: EXCLAMATION VALID_STRING
;

genericType: path? identifier
;

path: (identifier PATH_SEPARATOR)+
;

identifier: VALID_STRING | VALID_STRING_TYPE
;