parser grammar TopAntlrParser;

options
{
    tokenVocab = TopAntlrLexer;
}

definition:     (top)*
                EOF
;

top:            CODE_BLOCK_START (CODE)*
;