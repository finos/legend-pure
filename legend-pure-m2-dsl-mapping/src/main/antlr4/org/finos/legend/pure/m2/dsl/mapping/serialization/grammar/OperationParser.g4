parser grammar OperationParser;

options {
    tokenVocab = OperationLexer;
}

mapping:            functionPath parameters (END_LINE)?
                    EOF
;

parameters:         GROUP_OPEN (VALID_STRING (COMMA VALID_STRING)*)? GROUP_CLOSE
;

functionPath:       qualifiedName
;

qualifiedName:      packagePath? identifier
;

packagePath:        (identifier PATH_SEPARATOR)+
;

identifier:         VALID_STRING
;

