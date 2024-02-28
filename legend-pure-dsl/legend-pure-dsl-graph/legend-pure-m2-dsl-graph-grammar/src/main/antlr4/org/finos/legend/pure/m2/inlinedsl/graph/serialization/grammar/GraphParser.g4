parser grammar GraphParser;

options
{
    tokenVocab = GraphLexer;
}

identifier: VALID_STRING
;

qualifiedName: packagePath? identifier
;

packagePath: (identifier PATH_SEPARATOR)+
;

definition: CURLY_BRACKET_OPEN
                qualifiedName
                graphDefinition
             CURLY_BRACKET_CLOSE
             EOF
;

graphDefinition:  CURLY_BRACKET_OPEN
                    graphPaths
                  CURLY_BRACKET_CLOSE
;

graphPaths: graphPath (COMMA graphPath)*
;

graphPath: alias? identifier propertyParameters? subtype? graphDefinition?
;

alias: STRING COLON
;

propertyParameters: GROUP_OPEN (parameter (COMMA parameter)*)? GROUP_CLOSE
;

subtype: SUBTYPE_START qualifiedName GROUP_CLOSE
;

parameter: scalarParameter | collectionParameter
;

scalarParameter: LATEST_DATE | instanceLiteral | variable | enumReference
;

collectionParameter: BRACKET_OPEN
                        (scalarParameter (COMMA scalarParameter)*)?
                     BRACKET_CLOSE
;

instanceLiteral: instanceLiteralToken | (MINUS INTEGER) | (MINUS FLOAT) | (MINUS DECIMAL) | (PLUS INTEGER) | (PLUS FLOAT) | (PLUS DECIMAL)
;

instanceLiteralToken: STRING | INTEGER | FLOAT | DECIMAL | DATE | BOOLEAN
;

variable: DOLLAR identifier
;

enumReference: qualifiedName DOT identifier
;
