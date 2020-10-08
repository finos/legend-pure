parser grammar AggregationAwareParser;

options
{
    tokenVocab = AggregationAwareLexer;
}

mapping :                   VIEWS COLON
                                BRACKET_OPEN
                                    aggregationSpecification (COMMA aggregationSpecification)*
                                BRACKET_CLOSE
                                COMMA
                                mainMapping
                            EOF
;

aggregationSpecification :  GROUP_OPEN
                                modelOperation  COMMA   aggregateMapping
                            GROUP_CLOSE
;

modelOperation :            MODEL_OP COLON
                        CURLY_BRACKET_OPEN CONTENT CURLY_BRACKET_CLOSE
;

aggregateMapping :          AGG_MAP COLON parserName
                        CURLY_BRACKET_OPEN CONTENT CURLY_BRACKET_CLOSE
;

mainMapping :                MAIN_MAP COLON parserName
                        CURLY_BRACKET_OPEN CONTENT CURLY_BRACKET_CLOSE
;

parserName : VALID_STRING
;