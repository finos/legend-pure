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

singlePropertyMapping:                      singleLocalPropertyMapping | singleNonLocalPropertyMapping
;

singleLocalPropertyMapping:                 PLUS qualifiedName COLON qualifiedName multiplicity relationFunctionPropertyMapping
;

singleNonLocalPropertyMapping:              qualifiedName
                                            (
                                                relationFunctionPropertyMapping
                                                | inlineRelationFunctionEmbeddedPropertyMapping
                                                | relationFunctionEmbeddedPropertyMapping
                                            )
;

relationFunctionPropertyMapping:            COLON (transformer)? columnName
;

transformer:                                ENUMERATION_MAPPING identifier COLON
;

// -------------------------------------- EMBEDDED PROPERTY MAPPING --------------------------------------

relationFunctionEmbeddedPropertyMapping:    GROUP_OPEN
                                                (singlePropertyMapping (COMMA singlePropertyMapping)*)?
                                            GROUP_CLOSE
;

inlineRelationFunctionEmbeddedPropertyMapping:  GROUP_OPEN GROUP_CLOSE INLINE BRACKET_OPEN identifier BRACKET_CLOSE
;
