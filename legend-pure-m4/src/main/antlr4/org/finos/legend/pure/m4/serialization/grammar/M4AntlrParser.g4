parser grammar M4AntlrParser;

options
{
    tokenVocab = M4AntlrLexer;
}

definition :        (metaClass)*
                    EOF
;

metaClass :         CARET instance (newTypeStr)? (sourceInfo)? (AT nameSpace)?
                        CURLY_BRACKET_OPEN properties? (COMMA properties)* CURLY_BRACKET_CLOSE
;

properties :        path COLON rightSide
;

rightSide :         (BRACKET_OPEN (element (COMMA element)*)? BRACKET_CLOSE) | element
;

element :           metaClass | literalElement | instance | SELF
;

literalElement :    STRING | INTEGER | FLOAT | DATE | BOOLEAN
;

sourceInfo :        VALID_STRING_EXT COLON INTEGER COMMA INTEGER COMMA INTEGER COMMA INTEGER COMMA INTEGER COMMA INTEGER SOURCE_INFO_CLOSE
;

path :              name  (classifierOwner)*
;

newTypeStr :        VALID_STRING
;

instance :          name (classifierOwner)*
;

classifierOwner:    DOT key
                     (BRACKET_OPEN keyInArray BRACKET_CLOSE)?
;

nameSpace :         name (classifierOwner)*
;

name :              VALID_STRING
;

key :               VALID_STRING
;

keyInArray :        VALID_STRING
;
