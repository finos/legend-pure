lexer grammar M4Fragment;

M3FRAGMENT : 'M4Fragment';

fragment PathSeparator: '::'
;
fragment String: ('\'' ( EscSeq | ~['\r\n\\] )*  '\'' )
;
fragment Boolean: True | False
;
fragment True: 'true'
;
fragment False: 'false'
;
fragment Integer: (Digit)+
;
fragment Float: (Digit)* '.' (Digit)+ ( ('e' | 'E') ('+' | '-')? (Digit)+)? ('f' | 'F')?
;
fragment Decimal: ((Digit)* '.' (Digit)+ | (Digit)+) ( ('e' | 'E') ('+' | '-')? (Digit)+)? ('d' | 'D')
;
fragment Date: '%' ('-')? (Digit)+ ('-'(Digit)+ ('-'(Digit)+ ('T' DateTime TimeZone?)?)?)?
;
fragment StrictTime: '%' DateTime
;
fragment ValidString: (Letter | Digit | '_' ) (Letter | Digit | '_' | '$')*
;
fragment FileName: '?[' (Letter | Digit | '_' | '.' | '/')+
;
fragment FileNameEnd: ']?'
;
fragment DateTime : (Digit)+ (':'(Digit)+ (':'(Digit)+ ('.'(Digit)+)?)?)?
;
fragment TimeZone: (('+' | '-')(Digit)(Digit)(Digit)(Digit))
;
fragment Assign : ([ \r\t\n])* '='
;
fragment EscSeq
	:	Esc
		( [btnfr"'\\]	// The standard escaped character set such as tab, newline, etc.
		| UnicodeEsc	// A Unicode escape sequence
		| .				// Invalid escape character
		| EOF			// Incomplete at EOF
		)
;
fragment EscAny
	:	Esc .
;
fragment UnicodeEsc
	:	'u' (HexDigit (HexDigit (HexDigit HexDigit?)?)?)?
;
fragment Esc : '\\'
;
fragment Letter : [A-Za-z]
;
fragment Digit : [0-9]
;
fragment HexDigit : [0-9a-fA-F]
;
fragment Whitespace:   [ \r\t\n]+
;
fragment Comment:      '/*' .*? '*/'
;
fragment LineComment: '//' ~[\r\n]*
;