parser grammar MappingParser;

options
{
    tokenVocab = MappingLexer;
}

imports: import_statement*
;

import_statement:       IMPORT packagePath STAR END_LINE
;

definition:             imports
                            (mapping)*
                        EOF
;

mapping:                MAPPING qualifiedName
                            GROUP_OPEN
                                (includeMapping)*
                                (classMapping)*
                                (tests)?
                            GROUP_CLOSE
;

includeMapping:         INCLUDE qualifiedName
                            (BRACKET_OPEN
                                (storeSubPath (COMMA storeSubPath)*)?
                            BRACKET_CLOSE)?
;
classMapping:           (STAR)? qualifiedName
                            (BRACKET_OPEN classMappingId BRACKET_CLOSE)?
                            (EXTEND BRACKET_OPEN superClassMappingId BRACKET_CLOSE)?
                        COLON parserName (classMappingName)? mappingInstance
;
tests:                  TESTS BRACKET_OPEN
                            (test (COMMA test)*)?
                        BRACKET_CLOSE
;
test:                   testName
                            GROUP_OPEN
                                TEST_QUERY COLON testLambda COMMA
                                TEST_INPUT_DATA COLON testInput COMMA
                                TEST_ASSERT COLON testAssert
                            GROUP_CLOSE
;
testAssert:             testLambda|STRING
;
testLambda:            CURLY_BRACKET_OPEN
                             (testLambdaElement)*
;
mappingInstance:        CURLY_BRACKET_OPEN
                            (mappingInstanceElement)*
;
mappingInstanceElement: INNER_CURLY_BRACKET_OPEN | CONTENT | INNER_CURLY_BRACKET_CLOSE
;
classMappingName:       VALID_STRING
;
parserName:             VALID_STRING
;
classMappingId:         VALID_STRING
;
superClassMappingId:    VALID_STRING
;
testName:               VALID_STRING
;
testInput:              BRACKET_OPEN
                            (testInputElement (COMMA testInputElement)*)?
                        BRACKET_CLOSE
;
testInputElement:       CARET_OPEN testInputType COMMA testInputSrc COMMA testInputData CARET_CLOSE
;
testInputType:          VALID_STRING
;
testInputSrc:           qualifiedName
;
testInputData:          STRING
;
testLambdaElement:       INNER_CURLY_BRACKET_OPEN | CONTENT | INNER_CURLY_BRACKET_CLOSE
;
storeSubPath:           sourceStore ARROW targetStore
;
sourceStore:            qualifiedName
;
targetStore:            qualifiedName
;

qualifiedName:          packagePath? identifier
;

packagePath:            (identifier PATH_SEPARATOR)+
;

identifier:             VALID_STRING
;
