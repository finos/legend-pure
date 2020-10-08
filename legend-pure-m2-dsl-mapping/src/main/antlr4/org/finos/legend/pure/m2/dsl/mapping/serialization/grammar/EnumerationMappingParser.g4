parser grammar EnumerationMappingParser;

options
{
    tokenVocab = EnumerationMappingLexer;
}

mapping:                    enumSingleEntryMapping (COMMA enumSingleEntryMapping)*
                            EOF
;

enumSingleEntryMapping:     enumName COLON (enumSourceValue | enumMultipleSourceValue)
;

enumMultipleSourceValue:    BRACKET_OPEN enumSourceValue (COMMA enumSourceValue)* BRACKET_CLOSE
;

enumName:                   VALID_STRING
;

enumSourceValue:            STRING | INTEGER | enumReference
;

enumReference: qualifiedName  DOT identifier
;

qualifiedName: packagePath? identifier
;

packagePath: (identifier PATH_SEPARATOR)+
;

identifier:                 VALID_STRING
;

