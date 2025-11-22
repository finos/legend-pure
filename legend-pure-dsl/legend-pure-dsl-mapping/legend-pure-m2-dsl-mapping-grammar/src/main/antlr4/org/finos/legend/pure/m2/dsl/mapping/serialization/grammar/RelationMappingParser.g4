parser grammar RelationMappingParser;

import M3CoreParser;

options
{
    tokenVocab = RelationMappingLexer;
}

mapping:                                    RELATION_FUNCTION (functionDescriptor | qualifiedName)
                                            (singlePropertyMapping (COMMA singlePropertyMapping)*)?
                                            EOF
;

singlePropertyMapping:                      ((PLUS qualifiedName COLON qualifiedName multiplicity) | qualifiedName) COLON columnName
;
