parser grammar RelationalParser;

import M3Parser;

options
{
    tokenVocab = RelationalLexer;
}

definition: DATABASE qualifiedName
            GROUP_OPEN
              include*
              (schema | table | join |filter | multiGrainFilter | view)*
            GROUP_CLOSE
;

include: INCLUDE qualifiedName
;

schema:
    SCHEMA identifier
    GROUP_OPEN
        (table | view )*
    GROUP_CLOSE
;

multiGrainFilter:
    MULTIGRAINFILTER identifier
    GROUP_OPEN
        op_operation
    GROUP_CLOSE
;

filter: FILTER identifier GROUP_OPEN op_operation GROUP_CLOSE
;

op_operation:
    (op_groupOperation | op_atomicOperation) op_boolean_operation_right?
;

op_boolean_operation_right: op_boolean_operator op_operation
;

op_groupOperation: GROUP_OPEN op_operation GROUP_CLOSE
;

op_atomicOperation:
    op_function | colWithDbOrConstant ( (op_operator colWithDbOrConstant) | ISNULL | ISNOTNULL)
;

op_function:
    functionName (GROUP_OPEN ( functionArgument (COMMA functionArgument)* )? GROUP_CLOSE)
;

functionName:
    VALID_STRING
;

functionArgument:
    colWithDbOrConstant | arrayOfFunctionArguments
;

arrayOfFunctionArguments:
    BRACKET_OPEN ( functionArgument (COMMA functionArgument)* )? BRACKET_CLOSE
;

op_boolean_operator: AND | OR
;

op_operator: EQUAL | GREATERTHAN | LESSTHAN | GREATERTHANEQUAL | LESSTHANEQUAL | TEST_NOT_EQUAL | NOT_EQUAL_2
;

colWithDbOrConstant: (database? op_column) | constant
;

table:
    TABLE relationalIdentifier
    GROUP_OPEN
        milestoneSpec?
        columnDefinitions
    GROUP_CLOSE
;

milestoneSpec:
    MILESTONING
    GROUP_OPEN
        milestoningDefinitions?
    GROUP_CLOSE
;

view:
    VIEW relationalIdentifier
    GROUP_OPEN
        (filterViewBlock)?
        (mappingBlockGroupBy)?
        (DISTINCTCMD)?
        viewColumnMappingLines
    GROUP_CLOSE
;

viewColumnMappingLines: viewColumnMappingLine (COMMA viewColumnMappingLine)*
;

viewColumnMappingLine:
    identifier (BRACKET_OPEN identifier BRACKET_CLOSE)? COLON joinColWithDbOrConstant
;

join:
    JOIN identifier
    GROUP_OPEN
        op_operation
    GROUP_CLOSE
;

joinColWithDbOrConstants: joinColWithDbOrConstant? (COMMA joinColWithDbOrConstant)*
;

joinColWithDbOrConstant: (database? ((joinSequence (PIPE op_column)?) | op_column)) | constant
;

database: BRACKET_OPEN qualifiedName BRACKET_CLOSE
;

transformer: ENUMERATION_MAPPING identifier COLON
;

columnDefinitions: columnDefinition (COMMA columnDefinition)*
;
columnDefinition:
    relationalIdentifier
    identifier (GROUP_OPEN INTEGER (COMMA INTEGER)? GROUP_CLOSE)? (PRIMARYKEY | NOT_NULL)?
;

op_column:
    tableAliasColumn
    |
    tableAliasColumnWithScopeInfo
;

tableAliasColumn: TARGET DOT relationalIdentifier PRIMARYKEY?
;

tableAliasColumnWithScopeInfo:
    (relationalIdentifier | AND | OR)
    ((scopeInfo PRIMARYKEY?) | (GROUP_OPEN joinColWithDbOrConstants GROUP_CLOSE))?
;

simpleScopeInfo: relationalIdentifier scopeInfo?
;

scopeInfo:  scopeInfoPart scopeInfoPart?
;

scopeInfoPart: DOT relationalIdentifier
;

constant: (STRING | INTEGER | FLOAT)
;

mapping: classMapping | associationMapping
;

associationMapping:
    ASSOCIATION_MAPPING
        GROUP_OPEN
            propertyMappings
        GROUP_CLOSE
;

classMapping:
    mappingBlock
    (mappingElements)?
    EOF
;

mappingBlock:
    filterMappingBlock?
    DISTINCTCMD?
    mappingBlockGroupBy?
    primaryKey?
    mainTableBlock?
;

filterMappingBlock: FILTERCMD database (filterMappingJoinSequence PIPE database)? identifier
;

filterMappingJoinSequence:
    (GROUP_OPEN identifier GROUP_CLOSE)? oneJoin
    (oneJoinRight)*
;

mappingBlockGroupBy:
    GROUPBYCMD
        GROUP_OPEN
            joinColWithDbOrConstants
        GROUP_CLOSE
;

mainTableBlock: MAINTABLECMD database simpleScopeInfo
;

propertyMappings: mappingElements
;

mappingElements: mappingElement (COMMA mappingElement)*
;

mappingElement: singleMappingLine | scope
;

scope:
    SCOPE
    GROUP_OPEN
        database simpleScopeInfo?
    GROUP_CLOSE
    GROUP_OPEN
        singleMappingLines
    GROUP_CLOSE
;

singleMappingLines: singleMappingLine (COMMA singleMappingLine)*
;

singleMappingLine: plusSingleMappingLine | nonePlusSingleMappingLine
;

plusSingleMappingLine: PLUS identifier localMappingProperty relationalMapping
;
nonePlusSingleMappingLine: identifier sourceAndTargetMappingId? (embeddedMapping | relationalMapping)
;

localMappingProperty: COLON qualifiedName BRACKET_OPEN localMappingPropertyFirstMul (DOTDOT localMappingPropertySecondMul)? BRACKET_CLOSE
;

localMappingPropertyFirstMul: INTEGER | STAR
;

localMappingPropertySecondMul: INTEGER | STAR
;

sourceAndTargetMappingId: BRACKET_OPEN sourceId (COMMA targetId)? BRACKET_CLOSE
;

sourceId: identifier
;

targetId: identifier
;

embeddedMapping:
    GROUP_OPEN
        (primaryKey? singleMappingLines)?
    GROUP_CLOSE
    (otherwiseEmbeddedMapping | inline)?
;

inline: INLINE BRACKET_OPEN identifier BRACKET_CLOSE
;

primaryKey: PRIMARYKEYCMD GROUP_OPEN joinColWithDbOrConstants GROUP_CLOSE
;

otherwiseEmbeddedMapping: OTHERWISE GROUP_OPEN otherwisePropertyMappings GROUP_CLOSE
;

otherwisePropertyMappings: otherwisePropertyMapping (COMMA otherwisePropertyMapping)*
;

otherwisePropertyMapping:
    BRACKET_OPEN identifier BRACKET_CLOSE COLON otherwiseJoin
;

otherwiseJoin: database? joinSequence
;

relationalMapping: COLON (transformer)? joinColWithDbOrConstant
;

filterViewBlock: FILTERCMD (database joinSequence PIPE database)? identifier
;

joinSequence:
    oneJoin
    (oneJoinRight)*
;

oneJoinRight : GREATERTHAN (GROUP_OPEN identifier GROUP_CLOSE)? database?  oneJoin
;

oneJoin: AT identifier
;

milestoningDefinitions: milestoningDefinition (COMMA milestoningDefinition)*
;

milestoningDefinition:
    identifier
    GROUP_OPEN
       milestoningContent
    GROUP_CLOSE
;

milestoningContent: (identifier | EQUAL | COMMA | BUSINESS_MILESTONING_FROM | BUSINESS_MILESTONING_THRU | THRU_IS_INCLUSIVE | INFINITY_DATE | BUS_SNAPSHOT_DATE | PROCESSING_MILESTONING_IN | PROCESSING_MILESTONING_OUT | OUT_IS_INCLUSIVE | DATE | BOOLEAN)*
;

businessMilestoningInnerDefinition: (businessMilestoningFrom | bussinessSnapshotDate | processingMilestoningDefinition)
;

businessMilestoningFrom:    BUSINESS_MILESTONING_FROM EQUAL identifier COMMA BUSINESS_MILESTONING_THRU EQUAL identifier
                                (COMMA ((THRU_IS_INCLUSIVE EQUAL BOOLEAN (COMMA INFINITY_DATE EQUAL DATE)?) | (INFINITY_DATE EQUAL DATE)) )?
;

bussinessSnapshotDate:      BUS_SNAPSHOT_DATE EQUAL identifier
;

processingMilestoningDefinition:        PROCESSING_MILESTONING
                                        GROUP_OPEN
                                            processingMilestoningInnerDefinition
                                        GROUP_CLOSE
;

processingMilestoningInnerDefinition: PROCESSING_MILESTONING_IN EQUAL identifier COMMA
                                      PROCESSING_MILESTONING_OUT EQUAL identifier
                                      (COMMA ((OUT_IS_INCLUSIVE EQUAL BOOLEAN (COMMA INFINITY_DATE EQUAL DATE)?) | (INFINITY_DATE EQUAL DATE)))?
;

relationalIdentifier: identifier | QUOTED_STRING
;

identifier: VALID_STRING | CLASS | FUNCTION | PROFILE | ASSOCIATION | ENUM | MEASURE | STEREOTYPES | TAGS | IMPORT | LET | AGGREGATION_TYPE | PATH_SEPARATOR | AS | ALL | PROJECTS | CONSTRAINT | ENFORCEMENT_LEVEL | ENUMERATION_MAPPING | MILESTONING | PROCESSING_MILESTONING | BUSINESS_MILESTONING
;
