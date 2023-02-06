parser grammar DiagramAntlrParser;

options
{
    tokenVocab = DiagramAntlrLexer;
}

imports: import_statement*
;

definition:         imports
                    (diagram)*
                     EOF
;

diagram:            DIAGRAM qualifiedDiagramName (geometry)?
                    CURLY_BRACKET_OPEN
                        (typeView | associationView | propertyView | generalizationView)*
                    CURLY_BRACKET_CLOSE
;

typeView:           TYPE_VIEW diagramIdentifier
                        GROUP_OPEN
                            typeViewProperty (COMMA typeViewProperty)*
                        GROUP_CLOSE
;

associationView:    ASSOCIATION_VIEW diagramIdentifier
                        GROUP_OPEN
                            associationViewProperty (COMMA associationViewProperty)*
                        GROUP_CLOSE
;

propertyView:       PROPERTY_VIEW diagramIdentifier
                        GROUP_OPEN
                            propertyViewProperty (COMMA propertyViewProperty)*
                        GROUP_CLOSE
;

generalizationView: GENERALIZATION_VIEW diagramIdentifier
                        GROUP_OPEN
                            generalizationViewProperty (COMMA generalizationViewProperty)*
                        GROUP_CLOSE
;

typeViewProperty:
                TYPE                            qualifiedDiagramName
            |   STEREOTYPES_VISIBLE             BOOLEAN
            |   ATTRIBUTES_VISIBLE              BOOLEAN
            |   ATTRIBUTE_STEREOTYPES_VISIBLE   BOOLEAN
            |   ATTRIBUTE_TYPES_VISIBLE         BOOLEAN
            |   COLOR                           COLOR_STRING
            |   LINE_WIDTH                      FLOAT
            |   POSITION                        GROUP_OPEN FLOAT COMMA FLOAT GROUP_CLOSE
            |   WIDTH                           FLOAT
            |   HEIGHT                          FLOAT
;

associationViewProperty:
                ASSOCIATION                     qualifiedDiagramName
            |   STEREOTYPES_VISIBLE             BOOLEAN
            |   NAME_VISIBLE                    BOOLEAN
            |   COLOR                           COLOR_STRING
            |   LINE_WIDTH                      FLOAT
            |   LABEL                           STRING
            |   LINE_STYLE                      diagramIdentifier
            |   POINTS                          BRACKET_OPEN ( GROUP_OPEN FLOAT COMMA FLOAT GROUP_CLOSE (COMMA GROUP_OPEN FLOAT COMMA FLOAT GROUP_CLOSE)+ )? BRACKET_CLOSE
            |   SOURCE                          diagramIdentifier
            |   TARGET                          diagramIdentifier
            |   SOURCE_PROP_POSITION            GROUP_OPEN FLOAT COMMA FLOAT GROUP_CLOSE
            |   SOURCE_MULT_POSITION            GROUP_OPEN FLOAT COMMA FLOAT GROUP_CLOSE
            |   TARGET_PROP_POSITION            GROUP_OPEN FLOAT COMMA FLOAT GROUP_CLOSE
            |   TARGET_MULT_POSITION            GROUP_OPEN FLOAT COMMA FLOAT GROUP_CLOSE
;

propertyViewProperty:
                PROPERTY                        qualifiedDiagramName DOT diagramIdentifier
            |   STEREOTYPES_VISIBLE             BOOLEAN
            |   NAME_VISIBLE                    BOOLEAN
            |   COLOR                           COLOR_STRING
            |   LINE_WIDTH                      FLOAT
            |   LABEL                           STRING
            |   LINE_STYLE                      diagramIdentifier
            |   POINTS                          BRACKET_OPEN ( GROUP_OPEN FLOAT COMMA FLOAT GROUP_CLOSE (COMMA GROUP_OPEN FLOAT COMMA FLOAT GROUP_CLOSE)+ )? BRACKET_CLOSE
            |   SOURCE                          diagramIdentifier
            |   TARGET                          diagramIdentifier
            |   PROP_POSITION                   GROUP_OPEN FLOAT COMMA FLOAT GROUP_CLOSE
            |   MULT_POSITION                   GROUP_OPEN FLOAT COMMA FLOAT GROUP_CLOSE
;

generalizationViewProperty:
                COLOR                           COLOR_STRING
            |   LINE_WIDTH                      FLOAT
            |   LABEL                           STRING
            |   LINE_STYLE                      diagramIdentifier
            |   POINTS                          BRACKET_OPEN ( GROUP_OPEN FLOAT COMMA FLOAT GROUP_CLOSE (COMMA GROUP_OPEN FLOAT COMMA FLOAT GROUP_CLOSE)+ )? BRACKET_CLOSE
            |   SOURCE                          diagramIdentifier
            |   TARGET                          diagramIdentifier
;

geometry: GROUP_OPEN (widthFirst | heightFirst) GROUP_CLOSE
;

widthFirst: WIDTH FLOAT COMMA HEIGHT FLOAT
;

heightFirst: HEIGHT FLOAT COMMA WIDTH FLOAT
;

import_statement: IMPORT packagePath STAR END_LINE
;

packagePath: (diagramIdentifier PATH_SEPARATOR)+
;

diagramIdentifier:  VALID_STRING | DIAGRAM | TYPE_VIEW | ASSOCIATION_VIEW | PROPERTY_VIEW | GENERALIZATION_VIEW | GEOMETRY
;

qualifiedDiagramName: diagramPackagePath? diagramIdentifier
;

diagramPackagePath: (diagramIdentifier PATH_SEPARATOR)+
;