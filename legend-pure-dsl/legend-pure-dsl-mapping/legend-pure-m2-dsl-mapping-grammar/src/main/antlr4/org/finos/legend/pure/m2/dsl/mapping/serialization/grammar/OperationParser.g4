parser grammar OperationParser;

options {
    tokenVocab = OperationLexer;
}

mapping:            functionPath (parameters | mergeParameters) (END_LINE)?
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

setParameter:       BRACKET_OPEN (VALID_STRING (COMMA VALID_STRING)*)? BRACKET_CLOSE
;

lambdaElement:      INNER_CURLY_BRACKET_OPEN | CONTENT | INNER_CURLY_BRACKET_CLOSE
;

mergeParameters:         GROUP_OPEN setParameter COMMA validationLambdaInstance GROUP_CLOSE
;

validationLambdaInstance:        CURLY_BRACKET_OPEN
                            (lambdaElement)*
;

