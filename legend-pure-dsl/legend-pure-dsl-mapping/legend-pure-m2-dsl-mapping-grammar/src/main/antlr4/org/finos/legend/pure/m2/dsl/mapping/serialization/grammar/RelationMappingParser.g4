parser grammar RelationMappingParser;

import M3CoreParser;

options
{
    tokenVocab = RelationMappingLexer;
}

mapping:                                    relationSource
                                            (singlePropertyMapping (COMMA singlePropertyMapping)*)?
                                            EOF
;

// ~func references an existing Pure function by descriptor or qualified name.
// ~src takes an inline zero-arg Pure expression that evaluates to a Relation —
// the graph builder wraps it in a synthetic `{| <expr>}` lambda so the rest of
// the pipeline can treat both forms uniformly.
relationSource:                             RELATION_FUNCTION (functionDescriptor | qualifiedName)
                                            | MAPPING_SRC      combinedExpression
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

// Property RHS — bare columnName (legacy, lowered to `$src.<col>`) or a
// full Pure expression over `$src`.  columnName is listed first so a single
// bare identifier (which matches both alternatives) commits to the legacy
// path; anything starting with `$` or containing operators / function calls
// falls through to combinedExpression.
relationFunctionPropertyMapping:            COLON (transformer)? (columnName | combinedExpression)
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
