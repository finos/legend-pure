// Copyright 2026 Goldman Sachs
//
// Licensed under the Apache License, Version 2.0 (the "License");
// you may not use this file except in compliance with the License.
// You may obtain a copy of the License at
//
//      http://www.apache.org/licenses/LICENSE-2.0
//
// Unless required by applicable law or agreed to in writing, software
// distributed under the License is distributed on an "AS IS" BASIS,
// WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
// See the License for the specific language governing permissions and
// limitations under the License.

//! Parser for the M3 metamodel serialization format (`m3.pure`).
//!
//! The `m3.pure` file uses a specialised instance-graph serialization
//! (not normal Pure syntax). Each top-level declaration is:
//!
//! ```text
//! ^ClassifierPath Name @ParentPath { property: value, ... }
//! ```
//!
//! This module parses that format and **directly allocates** [`Element`]
//! instances into the bootstrap [`Arena`] — no intermediate AST.
//!
//! # Extracted data
//!
//! | Classifier tail | → Element variant | Fields populated |
//! |---|---|---|
//! | `Class` | [`Element::Class`] | properties (name, type, multiplicity), generalizations, type parameters |
//! | `Enumeration` | [`Element::Enumeration`] | enum values |
//! | `Profile` | [`Element::Profile`] | stereotypes, tags |
//! | `PackageableMultiplicity` | [`Element::PackageableMultiplicity`] | lower/upper bounds |
//!
//! Everything else (classifierGenericType, multiplicityArguments, etc.)
//! is skipped — the parser just balances braces and brackets.

use crate::source_info::SourceInfo;
use smol_str::SmolStr;

use crate::arena::Arena;
use crate::bootstrap::BOOTSTRAP_CHUNK_ID;
use crate::ids::ElementId;
use crate::model::{Element, ElementNode};
use crate::nodes::class::{Class, Property};
use crate::nodes::enumeration::{EnumValue, Enumeration};
use crate::nodes::profile::Profile;
use crate::types::Multiplicity;

/// Registration info for one M3 element that was parsed and allocated.
///
/// The caller uses this to wire the element into the package tree
/// via `model.register_element(package_id, element_id)`.
#[derive(Debug, Clone)]
pub struct M3Registration {
    /// The `ElementId` of the allocated element in the bootstrap chunk.
    pub element_id: ElementId,
    /// Package path segments (e.g., `["meta", "pure", "metamodel", "type"]`).
    pub package_segments: Vec<SmolStr>,
}

/// Parses the m3.pure serialization and directly allocates elements
/// into the given arenas.
///
/// Returns a list of [`M3Registration`]s for the caller to wire into
/// the package tree.
///
/// # Panics
///
/// Panics on malformed input — the m3.pure file is a known-good input
/// from the Java codebase, so we treat parse failures as bugs.
pub fn parse_m3_into_chunk(
    source: &str,
    nodes: &mut Arena<ElementNode>,
    elements: &mut Arena<Element>,
) -> Vec<M3Registration> {
    let mut parser = M3Parser::new(source, nodes, elements);
    parser.parse_file();
    parser.registrations
}

#[derive(Debug, Clone, PartialEq)]
enum Token {
    Caret,  // ^
    Dot,    // .
    LBrack, // [
    RBrack, // ]
    LBrace, // {
    RBrace, // }
    Colon,  // :
    Comma,  // ,
    At,     // @
    Ident(SmolStr),
    StringLit(SmolStr),
    IntLit(i64),
    Eof,
}

struct Tokenizer {
    chars: Vec<char>,
    pos: usize,
}

impl Tokenizer {
    fn new(source: &str) -> Self {
        Self {
            chars: source.chars().collect(),
            pos: 0,
        }
    }

    fn peek_char(&self) -> Option<char> {
        self.chars.get(self.pos).copied()
    }

    fn advance_char(&mut self) -> Option<char> {
        let ch = self.chars.get(self.pos).copied();
        if ch.is_some() {
            self.pos += 1;
        }
        ch
    }

    fn skip_whitespace_and_comments(&mut self) {
        loop {
            // Skip whitespace
            while let Some(ch) = self.peek_char() {
                if ch.is_ascii_whitespace() {
                    self.advance_char();
                } else {
                    break;
                }
            }
            // Skip `// ...` line comments
            if self.pos + 1 < self.chars.len()
                && self.chars[self.pos] == '/'
                && self.chars[self.pos + 1] == '/'
            {
                while let Some(ch) = self.advance_char() {
                    if ch == '\n' {
                        break;
                    }
                }
                continue;
            }
            break;
        }
    }

    fn next_token(&mut self) -> Token {
        self.skip_whitespace_and_comments();

        let Some(ch) = self.advance_char() else {
            return Token::Eof;
        };

        match ch {
            '^' => Token::Caret,
            '.' => Token::Dot,
            '[' => Token::LBrack,
            ']' => Token::RBrack,
            '{' => Token::LBrace,
            '}' => Token::RBrace,
            ':' => Token::Colon,
            ',' => Token::Comma,
            '@' => Token::At,
            '\'' => {
                // String literal: 'content'
                let mut s = String::new();
                loop {
                    match self.advance_char() {
                        Some('\'') | None => break,
                        Some(c) => s.push(c),
                    }
                }
                Token::StringLit(SmolStr::new(&s))
            }
            c if c.is_ascii_digit() || c == '-' => {
                // Integer literal
                let mut s = String::new();
                s.push(c);
                while let Some(ch) = self.peek_char() {
                    if ch.is_ascii_digit() {
                        s.push(ch);
                        self.advance_char();
                    } else {
                        break;
                    }
                }
                Token::IntLit(s.parse().unwrap_or(0))
            }
            c if c.is_alphanumeric() || c == '_' => {
                // Identifier
                let mut s = String::new();
                s.push(c);
                while let Some(ch) = self.peek_char() {
                    if ch.is_alphanumeric() || ch == '_' {
                        s.push(ch);
                        self.advance_char();
                    } else {
                        break;
                    }
                }
                Token::Ident(SmolStr::new(&s))
            }
            _ => {
                // Skip unexpected chars and try next
                self.next_token()
            }
        }
    }
}

/// Synthetic source info for bootstrap elements (no real file position).
fn synthetic_source() -> SourceInfo {
    SourceInfo {
        source: SmolStr::new("<m3.pure>"),
        start_line: 0,
        start_column: 0,
        end_line: 0,
        end_column: 0,
    }
}

struct M3Parser<'a> {
    tokens: Vec<Token>,
    pos: usize,
    nodes: &'a mut Arena<ElementNode>,
    elements: &'a mut Arena<Element>,
    registrations: Vec<M3Registration>,
}

impl<'a> M3Parser<'a> {
    fn new(
        source: &str,
        nodes: &'a mut Arena<ElementNode>,
        elements: &'a mut Arena<Element>,
    ) -> Self {
        // Pre-tokenize the entire file
        let mut tokenizer = Tokenizer::new(source);
        let mut tokens = Vec::new();
        loop {
            let tok = tokenizer.next_token();
            if tok == Token::Eof {
                tokens.push(Token::Eof);
                break;
            }
            tokens.push(tok);
        }

        Self {
            tokens,
            pos: 0,
            nodes,
            elements,
            registrations: Vec::new(),
        }
    }

    fn peek(&self) -> &Token {
        self.tokens.get(self.pos).unwrap_or(&Token::Eof)
    }

    fn advance(&mut self) -> Token {
        let tok = self.tokens.get(self.pos).cloned().unwrap_or(Token::Eof);
        if tok != Token::Eof {
            self.pos += 1;
        }
        tok
    }

    fn expect_ident(&mut self) -> SmolStr {
        match self.advance() {
            Token::Ident(s) => s,
            other => panic!(
                "m3_parser: expected Ident, got {other:?} at pos {}",
                self.pos
            ),
        }
    }

    fn at(&self, tok: &Token) -> bool {
        std::mem::discriminant(self.peek()) == std::mem::discriminant(tok)
    }

    fn eat(&mut self, expected: &Token) -> bool {
        if self.at(expected) {
            self.advance();
            true
        } else {
            false
        }
    }

    fn expect(&mut self, expected: &Token) {
        assert!(
            self.eat(expected),
            "m3_parser: expected {expected:?}, got {:?} at pos {}",
            self.peek(),
            self.pos
        );
    }

    fn parse_file(&mut self) {
        loop {
            match self.peek() {
                Token::Eof => break,
                Token::Caret => self.parse_top_level_instance(),
                _ => {
                    // Skip unexpected tokens
                    self.advance();
                }
            }
        }
    }

    fn parse_top_level_instance(&mut self) {
        self.expect(&Token::Caret);

        // Parse classifier path — the tail identifier tells us the type
        let classifier_tail = self.parse_classifier_path();

        // Parse instance name
        let name = self.expect_ident();

        // Parse optional @parent_path
        let package_segments = if self.eat(&Token::At) {
            self.parse_package_path()
        } else {
            vec![]
        };

        // Parse body (if present)
        match classifier_tail.as_str() {
            "Class" => {
                // `Any` and `Nil` already exist as the top/bottom-type
                // bootstrap classes (slots 0 and 1) and are registered in
                // `meta::pure::metamodel::type` via `M3_ALIASES`. m3.pure
                // re-declares them; allocating a second slot here would
                // create a duplicate registration that wins
                // `resolve_by_path` over the bootstrap alias and breaks
                // `is_metatype_carrier` on FQN round-trip.
                // Skip the body so the bootstrap slot stays canonical.
                let is_metamodel_type_pkg = package_segments.iter().map(SmolStr::as_str).eq([
                    "meta",
                    "pure",
                    "metamodel",
                    "type",
                ]
                .iter()
                .copied());
                if is_metamodel_type_pkg && (name.as_str() == "Any" || name.as_str() == "Nil") {
                    if self.at(&Token::LBrace) {
                        self.skip_balanced_braces();
                    }
                } else {
                    self.parse_class_body(&name, &package_segments);
                }
            }
            "Enumeration" => self.parse_enumeration_body(&name, &package_segments),
            "Profile" => self.parse_profile_body(&name, &package_segments),
            "PackageableMultiplicity" => {
                self.parse_multiplicity_body(&name, &package_segments);
            }
            "Package" => {
                // Skip package bodies — we handle packages via the @path
                if self.at(&Token::LBrace) {
                    self.skip_balanced_braces();
                }
            }
            "PrimitiveType" => {
                // The 11 standard primitives are already in bootstrap slots 0-12.
                // Any additional PrimitiveTypes (e.g., LatestDate) must be allocated.
                const BOOTSTRAP_PRIMS: &[&str] = &[
                    "String",
                    "Boolean",
                    "Byte",
                    "StrictTime",
                    "Number",
                    "Integer",
                    "Float",
                    "Decimal",
                    "Date",
                    "StrictDate",
                    "DateTime",
                ];
                if BOOTSTRAP_PRIMS.contains(&name.as_str()) {
                    if self.at(&Token::LBrace) {
                        self.skip_balanced_braces();
                    }
                } else {
                    // Non-bootstrap primitive — parse body to extract supertype
                    self.parse_primitive_type_body(&name, &package_segments);
                }
            }
            _ => {
                // Unknown classifier — skip body if present
                if self.at(&Token::LBrace) {
                    self.skip_balanced_braces();
                }
            }
        }
    }

    /// Parses a classifier path like `Root.children[...].children[Class]`
    /// or just `Package`. Returns the tail identifier (e.g., "Class", "Package").
    fn parse_classifier_path(&mut self) -> SmolStr {
        let mut tail = self.expect_ident();

        // Follow `.children[X]` or `.properties[X]` chains
        while self.at(&Token::Dot) {
            self.advance(); // .
            let segment = self.expect_ident();
            if self.at(&Token::LBrack) {
                self.advance(); // [
                tail = self.expect_ident();
                self.expect(&Token::RBrack);
            } else {
                tail = segment;
            }
        }

        tail
    }

    /// Parses a path like `Root.children[meta].children[pure].children[metamodel].children`
    /// and extracts the package segments: `["meta", "pure", "metamodel"]`.
    fn parse_package_path(&mut self) -> Vec<SmolStr> {
        let mut segments = Vec::new();

        // Start: expect "Root" or first identifier
        let _root = self.expect_ident();

        // Parse .children[X] chains
        while self.at(&Token::Dot) {
            self.advance(); // .
            let _accessor = self.expect_ident(); // "children" or "properties" etc.
            if self.at(&Token::LBrack) {
                self.advance(); // [
                let seg = self.expect_ident();
                segments.push(seg);
                self.expect(&Token::RBrack);
            }
        }

        segments
    }

    fn alloc_element(
        &mut self,
        name: &SmolStr,
        package_segments: &[SmolStr],
        element: Element,
    ) -> ElementId {
        let idx = self.nodes.alloc(ElementNode {
            name: name.clone(),
            source_info: synthetic_source(),
            name_source_info: synthetic_source(),
            parent_package: crate::ids::PackageId(0), // placeholder — wired later
        });
        let elem_idx = self.elements.alloc(element);
        debug_assert_eq!(idx, elem_idx, "node and element arenas out of sync");

        let element_id = ElementId::InstanceId {
            chunk_id: BOOTSTRAP_CHUNK_ID,
            local_idx: idx,
        };

        self.registrations.push(M3Registration {
            element_id,
            package_segments: package_segments.to_vec(),
        });

        element_id
    }

    fn parse_class_body(&mut self, name: &SmolStr, package_segments: &[SmolStr]) {
        use crate::nodes::class::TypeParameter;
        let mut properties = Vec::new();
        let mut super_types = Vec::new();
        let mut type_parameters: Vec<TypeParameter> = Vec::new();
        let mut multiplicity_parameters: Vec<SmolStr> = Vec::new();

        if !self.at(&Token::LBrace) {
            // No body — allocate empty class
            self.alloc_element(
                name,
                package_segments,
                Element::Class(Class {
                    type_parameters: vec![],
                    multiplicity_parameters: Vec::new(),
                    type_variable_parameters: vec![],
                    super_types: vec![],
                    properties: vec![],
                    qualified_properties: vec![],
                    constraints: vec![],
                    stereotypes: vec![],
                    tagged_values: vec![],
                    original_milestoned_properties: vec![],
                }),
            );
            return;
        }

        self.advance(); // {

        // Parse body assignments
        loop {
            match self.peek() {
                Token::RBrace | Token::Eof => break,
                Token::Caret => {
                    // Inline instance — could be inside properties list,
                    // but at top-level of the body it's unexpected.
                    // Skip it.
                    self.skip_inline_instance();
                }
                _ => {
                    // Parse: property_path ':' value
                    let prop_path = self.parse_property_path_tail();

                    if !self.eat(&Token::Colon) {
                        // Malformed — skip to comma or closing brace
                        self.skip_to_comma_or_brace();
                        continue;
                    }

                    match prop_path.as_str() {
                        "properties" => {
                            // Class.properties[properties] : [...]
                            properties = self.parse_property_list();
                        }
                        "generalizations" => {
                            // Type.properties[generalizations] : [...]
                            // In m3.pure, generalizations stored on a type include entries
                            // for ALL subclasses, not just this class itself. We filter by
                            // `specific` to only keep entries for the current class.
                            super_types = self.parse_generalizations(name);
                        }
                        "typeParameters" => {
                            // Class.properties[typeParameters] : [...]
                            // Captures both the parameter names and
                            // their variance flags
                            // (`contravariant: true` /
                            // `covariant: true`). m3.pure declares
                            // Property's U and Column's U as
                            // contravariant via this metamodel-level
                            // form.
                            let (params, variances) = self.parse_type_parameters();
                            type_parameters = params
                                .into_iter()
                                .zip(variances)
                                .map(|(name, variance)| TypeParameter::new(name, variance))
                                .collect();
                        }
                        "multiplicityParameters" => {
                            // Class.properties[multiplicityParameters] :
                            // ^InstanceValue{values: ['m'], …} — extract
                            // the names from the values slot. Without
                            // this, `Property<U,V|m>` lost its `m`
                            // parameter and `subtype_view`'s mult
                            // substitution couldn't fire on the
                            // generalization, leaving downstream
                            // `eval(Property<…>)` unable to bind the
                            // FunctionType slot's `m` (1,628 cases on
                            // platform pre-fix; this lifts the lid on
                            // the residual ones).
                            multiplicity_parameters =
                                self.parse_multiplicity_parameters_instance_value();
                        }
                        _ => {
                            // Skip the value
                            self.skip_value();
                        }
                    }
                }
            }

            // Expect comma between assignments
            self.eat(&Token::Comma);
        }

        self.eat(&Token::RBrace);

        self.alloc_element(
            name,
            package_segments,
            Element::Class(Class {
                type_parameters,
                multiplicity_parameters,
                type_variable_parameters: vec![],
                super_types,
                properties,
                qualified_properties: vec![],
                constraints: vec![],
                stereotypes: vec![],
                tagged_values: vec![],
                original_milestoned_properties: vec![],
            }),
        );
    }

    /// Parses the multiplicityParameters InstanceValue shape:
    /// `^InstanceValue { values: ['m', 'n', …], multiplicity: …, genericType: … }`
    /// and returns the parameter names.
    ///
    /// Tolerant of malformed input — returns whatever names were
    /// successfully parsed.
    fn parse_multiplicity_parameters_instance_value(&mut self) -> Vec<SmolStr> {
        let mut names = Vec::new();
        if !self.at(&Token::Caret) {
            self.skip_value();
            return names;
        }
        self.advance(); // ^
        let _classifier = self.parse_classifier_path();

        if !self.eat(&Token::LBrace) {
            return names;
        }
        loop {
            match self.peek() {
                Token::RBrace | Token::Eof => break,
                _ => {
                    let path_tail = self.parse_property_path_tail();
                    if !self.eat(&Token::Colon) {
                        self.skip_to_comma_or_brace();
                        continue;
                    }
                    if path_tail.as_str() == "values" {
                        // values is `['m', 'n', …]`
                        if self.eat(&Token::LBrack) {
                            loop {
                                match self.peek() {
                                    Token::RBrack | Token::Eof => break,
                                    Token::StringLit(s) => {
                                        names.push(SmolStr::new(s.as_str()));
                                        self.advance();
                                    }
                                    _ => {
                                        self.advance();
                                    }
                                }
                                self.eat(&Token::Comma);
                            }
                            self.eat(&Token::RBrack);
                        } else if let Token::StringLit(s) = self.peek() {
                            names.push(SmolStr::new(s.as_str()));
                            self.advance();
                        } else {
                            self.skip_value();
                        }
                    } else {
                        self.skip_value();
                    }
                }
            }
            self.eat(&Token::Comma);
        }
        self.eat(&Token::RBrace);
        names
    }

    /// Parses a non-bootstrap `PrimitiveType` body from m3.pure.
    ///
    /// Extracts the supertype from its generalizations and allocates it as a
    /// `PrimitiveType` element. The supertype is resolved later during the
    /// compilation pipeline when package registration wires up the references.
    fn parse_primitive_type_body(&mut self, name: &SmolStr, package_segments: &[SmolStr]) {
        let mut super_type_name: Option<SmolStr> = None;

        if self.eat(&Token::LBrace) {
            loop {
                match self.peek() {
                    Token::RBrace | Token::Eof => break,
                    Token::Caret => {
                        self.skip_inline_instance();
                    }
                    _ => {
                        let prop_path = self.parse_property_path_tail();
                        if !self.eat(&Token::Colon) {
                            self.skip_to_comma_or_brace();
                            continue;
                        }

                        if prop_path.as_str() == "generalizations" {
                            let supers = self.parse_generalizations(name);
                            if let Some(crate::types::TypeExpr::Generic(s)) = supers.first() {
                                super_type_name = Some(s.clone());
                            }
                        } else {
                            self.skip_value();
                        }
                    }
                }
                self.eat(&Token::Comma);
            }
            self.eat(&Token::RBrace);
        }

        // Look up the supertype ElementId from the bootstrap primitives
        let super_type = super_type_name.and_then(|sn| {
            crate::bootstrap::BOOTSTRAP_PRIMITIVES
                .iter()
                .find(|(n, _, _)| *n == sn.as_str())
                .map(|(_, id, _)| *id)
        });

        self.alloc_element(
            name,
            package_segments,
            Element::PrimitiveType(crate::types::PrimitiveType {
                super_type,
                super_type_value_arguments: Vec::new(),
                type_variable_parameters: Vec::new(),
                constraints: Vec::new(),
            }),
        );
    }

    /// Parses a list of `^Property` instances inside `[...]`.
    fn parse_property_list(&mut self) -> Vec<Property> {
        let mut props = Vec::new();

        if self.eat(&Token::LBrack) {
            // Array of properties
            loop {
                match self.peek() {
                    Token::RBrack | Token::Eof => break,
                    Token::Caret => {
                        props.push(self.parse_property_instance());
                    }
                    _ => {
                        self.advance();
                    }
                }
                self.eat(&Token::Comma);
            }
            self.eat(&Token::RBrack);
        } else if self.at(&Token::Caret) {
            // Single property (not in array)
            props.push(self.parse_property_instance());
        } else {
            self.skip_value();
        }

        props
    }

    /// Parses a single `^...Property name { ... }` instance.
    fn parse_property_instance(&mut self) -> Property {
        self.expect(&Token::Caret);
        let _classifier = self.parse_classifier_path();
        let prop_name = self.expect_ident();

        let mut multiplicity = Multiplicity::PureOne;
        let mut type_expr_opt: Option<crate::types::TypeExpr> = None;

        if self.eat(&Token::LBrace) {
            loop {
                match self.peek() {
                    Token::RBrace | Token::Eof => break,
                    Token::Caret => {
                        self.skip_inline_instance();
                    }
                    _ => {
                        let path_tail = self.parse_property_path_tail();
                        if !self.eat(&Token::Colon) {
                            self.skip_to_comma_or_brace();
                            continue;
                        }

                        match path_tail.as_str() {
                            "name" => {
                                // ModelElement.properties[name] : 'xxx'
                                // We already have the name from the instance header
                                self.skip_value();
                            }
                            "multiplicity" => {
                                // Extract multiplicity from a reference like:
                                // Root...multiplicity.children[PureOne]
                                multiplicity = self.parse_multiplicity_ref();
                            }
                            "genericType" => {
                                // Parse the full inline TypeExpr so
                                // parametric property types like
                                // `Property<U, V>[*]` and inline-FunctionType
                                // shapes survive the bootstrap pass.
                                type_expr_opt = Some(self.parse_generic_type_full());
                            }
                            _ => {
                                self.skip_value();
                            }
                        }
                    }
                }
                self.eat(&Token::Comma);
            }
            self.eat(&Token::RBrace);
        }

        // `parse_generic_type_full` encodes the right shape internally:
        // `Generic(name)` for bare references, the pack-typeexpr sentinel
        // for parametric Class refs, or `TypeExpr::FunctionType{…}` for
        // inline FunctionType (m3.pure's `Function<{T->X}>`-style typeArgs).
        let type_expr =
            type_expr_opt.unwrap_or(crate::types::TypeExpr::Generic(SmolStr::new("Any")));

        Property {
            name: SmolStr::new(&prop_name),
            source_info: synthetic_source(),
            type_expr,
            multiplicity,
            aggregation: None,
            default_value: None,
            stereotypes: vec![],
            tagged_values: vec![],
        }
    }

    /// Parses the generalizations list and extracts super-type names.
    ///
    /// In m3.pure, `Type.properties[generalizations]` stores Generalization
    /// instances for ALL subclasses (e.g., Class, `PrimitiveType`), not just
    /// the current class. Each Generalization has:
    /// - `general.rawType` — the supertype
    /// - `specific` — the actual subtype this applies to
    ///
    /// We filter by `specific` to only keep entries where the subtype matches
    /// the class we are currently parsing (`class_name`).
    fn parse_generalizations(&mut self, class_name: &str) -> Vec<crate::types::TypeExpr> {
        let mut supers = Vec::new();

        if self.eat(&Token::LBrack) {
            loop {
                match self.peek() {
                    Token::RBrack | Token::Eof => break,
                    Token::Caret => {
                        // ^Generalization { general: ^GenericType { rawType: SuperClass, typeArguments: …, multiplicityArguments: … }, specific: SubClass }
                        if let Some((te, specific)) = self.parse_generalization_instance() {
                            // Only keep this generalization if `specific` matches
                            // the class we are currently parsing.
                            if specific.as_deref() == Some(class_name) || specific.is_none() {
                                supers.push(te);
                            }
                        }
                    }
                    _ => {
                        self.advance();
                    }
                }
                self.eat(&Token::Comma);
            }
            self.eat(&Token::RBrack);
        } else if self.at(&Token::Caret) {
            // Single generalization (not in array)
            if let Some((te, specific)) = self.parse_generalization_instance()
                && (specific.as_deref() == Some(class_name) || specific.is_none())
            {
                supers.push(te);
            }
        } else {
            self.skip_value();
        }

        supers
    }

    /// Parses a `^Generalization { general: ^GenericType{rawType: X, typeArguments: …, multiplicityArguments: …}, specific: Y }`
    /// and returns `(general_typeexpr, specific_name)`.
    ///
    /// The `general_typeexpr` is the *full* parametric form of the
    /// supertype — `Generic(name)` for plain references, or the
    /// sentinel `Named { ANY_ID, type_arguments, multiplicity_arguments,
    /// value_arguments: [String(rawType)] }` form when typeArguments /
    /// multiplicityArguments are present (`resolve_m3_supertypes`
    /// rewrites the element id during bootstrap finalisation —
    /// preserving the args). Without this, `Property` lost its
    /// `Function<{U[1]->V[m]}>` generalization shape and
    /// `bind_type`'s subtype-walk had no FunctionType slot to extract
    /// T/V/m/n from when an `eval` overload bound against a
    /// `Property<...>` arg.
    ///
    /// The `specific` is the simple name of the subclass extracted from the
    /// path reference (e.g., `Root.children[...].children[Class]` → `"Class"`).
    fn parse_generalization_instance(
        &mut self,
    ) -> Option<(crate::types::TypeExpr, Option<SmolStr>)> {
        self.expect(&Token::Caret);
        let _classifier = self.parse_classifier_path();

        // May or may not have a name
        if self.at(&Token::Ident(SmolStr::default()))
            && !self.at(&Token::LBrace)
            && let Token::Ident(_) = self.peek()
            && !self.at(&Token::LBrace)
        {
            // Check if next token after ident is { — if so, it's a body
            // Otherwise it might be something else
        }

        let mut general_type: Option<crate::types::TypeExpr> = None;
        let mut specific_type = None;

        if self.eat(&Token::LBrace) {
            loop {
                match self.peek() {
                    Token::RBrace | Token::Eof => break,
                    Token::Caret => {
                        self.skip_inline_instance();
                    }
                    _ => {
                        let path_tail = self.parse_property_path_tail();
                        if !self.eat(&Token::Colon) {
                            self.skip_to_comma_or_brace();
                            continue;
                        }

                        match path_tail.as_str() {
                            "general" => {
                                // Parametric-aware: `parse_generic_type_full`
                                // now returns the right TypeExpr shape
                                // (Generic for plain refs, pack-typeexpr
                                // for parametric Class refs, FunctionType
                                // for inline `^FunctionType{...}` —
                                // critical for Property's
                                // `Function<{U[1]->V[m]}>` generalization).
                                general_type = Some(self.parse_generic_type_full());
                            }
                            "specific" => {
                                // The value is a path reference like:
                                // Root.children[meta]...children[Class]
                                // Extract the last segment as the specific class name.
                                specific_type = Some(self.parse_classifier_path());
                            }
                            _ => {
                                self.skip_value();
                            }
                        }
                    }
                }
                self.eat(&Token::Comma);
            }
            self.eat(&Token::RBrace);
        }

        general_type.map(|g| (g, specific_type))
    }

    /// Parses a type parameters list: `[^TypeParameter{name:'T'}, ...]`.
    /// Returns parallel vectors of names and variances. Variance comes
    /// from the optional `contravariant: true` / `covariant: true`
    /// fields on the TypeParameter instance — m3.pure declares
    /// `Property<U[contravariant], V>`,
    /// `Column<U[contravariant], V>`, and
    /// `NewPropertyRouteNodeFunctionDefinition<U[contravariant], V>`
    /// via this metamodel-level form. (The class-level `<-T>` /
    /// `<+T>` prefix syntax is a separate path used only by path.pure.)
    fn parse_type_parameters(&mut self) -> (Vec<SmolStr>, Vec<crate::nodes::class::Variance>) {
        let mut params = Vec::new();
        let mut variances = Vec::new();

        if self.eat(&Token::LBrack) {
            loop {
                match self.peek() {
                    Token::RBrack | Token::Eof => break,
                    Token::Caret => {
                        if let Some((name, variance)) = self.parse_type_parameter_instance() {
                            params.push(name);
                            variances.push(variance);
                        }
                    }
                    _ => {
                        self.advance();
                    }
                }
                self.eat(&Token::Comma);
            }
            self.eat(&Token::RBrack);
        } else if self.at(&Token::Caret) {
            if let Some((name, variance)) = self.parse_type_parameter_instance() {
                params.push(name);
                variances.push(variance);
            }
        } else {
            self.skip_value();
        }

        (params, variances)
    }

    /// Parses `^TypeParameter{name:'T', contravariant:true, ...}` and
    /// returns `(name, variance)`. Variance defaults to `Invariant`
    /// when neither `contravariant` nor `covariant` flags are present.
    fn parse_type_parameter_instance(
        &mut self,
    ) -> Option<(SmolStr, crate::nodes::class::Variance)> {
        use crate::nodes::class::Variance;
        self.expect(&Token::Caret);
        let _classifier = self.parse_classifier_path();

        let mut name = None;
        let mut variance = Variance::Invariant;

        if self.eat(&Token::LBrace) {
            loop {
                match self.peek() {
                    Token::RBrace | Token::Eof => break,
                    _ => {
                        let path_tail = self.parse_property_path_tail();
                        if !self.eat(&Token::Colon) {
                            self.skip_to_comma_or_brace();
                            continue;
                        }

                        match path_tail.as_str() {
                            "name" => {
                                if let Token::StringLit(s) = self.advance() {
                                    name = Some(s);
                                }
                            }
                            "contravariant" => {
                                // The lexer maps the literal `true` /
                                // `false` to Ident; check the spelling
                                // explicitly. Treat any non-`true`
                                // value as the absence of the flag.
                                if matches!(&self.advance(), Token::Ident(s) if s.as_str() == "true")
                                {
                                    variance = Variance::Contravariant;
                                }
                            }
                            "covariant" => {
                                if matches!(&self.advance(), Token::Ident(s) if s.as_str() == "true")
                                {
                                    variance = Variance::Covariant;
                                }
                            }
                            _ => self.skip_value(),
                        }
                    }
                }
                self.eat(&Token::Comma);
            }
            self.eat(&Token::RBrace);
        }

        name.map(|n| (n, variance))
    }

    /// Extracts the raw type name from a `^GenericType{rawType: X}` or
    /// a simple element reference `Root.children[...].children[X]`.
    /// Parses an inline `^GenericType { rawType: …, typeArguments: […],
    /// multiplicityArguments: … }` OR `^FunctionType { parameters: […],
    /// returnType: …, returnMultiplicity: … }` and returns a `TypeExpr`.
    ///
    /// Three result shapes:
    ///
    /// 1. **`Generic(name)`** — for plain references (no type/mult
    ///    arguments) and for the `typeParameter` short-circuit form.
    /// 2. **Pack-typeexpr** (`Named { ANY_ID, type_arguments,
    ///    multiplicity_arguments, value_arguments: [String(rawType)] }`)
    ///    — for parametric Class references; the resolver rewrites the
    ///    element id later.
    /// 3. **`TypeExpr::FunctionType { parameters, return_type,
    ///    return_multiplicity }`** — when the classifier is FunctionType
    ///    OR when an outer GenericType's `rawType` is itself an inline
    ///    `^FunctionType{...}` (the m3.pure shape for Property's
    ///    `Function<{U[1]->V[m]}>` generalization). Critical for
    ///    `bind_type_with_mode`'s Function-vs-Property subtype-walk to
    ///    extract T/V/m/n at platform `getProperty('a')->toOne()->eval(…)`
    ///    sites.
    ///
    /// Used by property-instance parsing, generalization parsing, and
    /// `parse_type_argument_list` to feed `resolve_m3_supertypes`.
    fn parse_generic_type_full(&mut self) -> crate::types::TypeExpr {
        if !self.at(&Token::Caret) {
            // Direct element reference: Root.children[...].children[Type]
            return crate::types::TypeExpr::Generic(self.parse_element_ref_tail());
        }

        self.advance(); // ^
        let classifier = self.parse_classifier_path();

        // `^FunctionType{...}` directly — m3.pure uses this when the
        // typeArgument slot is itself a FunctionType (no surrounding
        // GenericType wrapper).
        if classifier.as_str() == "FunctionType" {
            return self.parse_function_type_body();
        }

        // Default: ^GenericType{rawType: …, typeArguments: […], multiplicityArguments: …}
        // GenericType can also be `{typeParameter: ^TypeParameter{name:'T'}}` — the
        // latter is a forward reference to one of the surrounding class's
        // declared type parameters and survives as `Generic("T")`.
        let mut raw_type_name = SmolStr::new("Any");
        let mut raw_type_inline_te: Option<crate::types::TypeExpr> = None;
        let mut type_arguments: Vec<crate::types::TypeExpr> = Vec::new();
        let mut multiplicity_arguments: Vec<Multiplicity> = Vec::new();
        let mut type_parameter_name: Option<SmolStr> = None;

        if self.eat(&Token::LBrace) {
            loop {
                match self.peek() {
                    Token::RBrace | Token::Eof => break,
                    _ => {
                        let path_tail = self.parse_property_path_tail();
                        if !self.eat(&Token::Colon) {
                            self.skip_to_comma_or_brace();
                            continue;
                        }

                        match path_tail.as_str() {
                            "rawType" => {
                                // rawType can be either:
                                //   - a direct element ref (Root.children[…].children[Foo])
                                //   - an inline `^FunctionType{...}` (m3.pure's
                                //     `Function<{->Z[y]}>`-style typeArgs)
                                //   - an inline `^GenericType{...}` (rare)
                                // Recurse so the FunctionType case produces a
                                // real `TypeExpr::FunctionType` rather than the
                                // previous "Any" fallback.
                                if self.at(&Token::Caret) {
                                    raw_type_inline_te = Some(self.parse_generic_type_full());
                                } else {
                                    raw_type_name = self.parse_element_ref_tail();
                                }
                            }
                            "typeArguments" => {
                                type_arguments = self.parse_type_argument_list();
                            }
                            "multiplicityArguments" => {
                                multiplicity_arguments = self.parse_multiplicity_argument_list();
                            }
                            "typeParameter" => {
                                type_parameter_name = Some(self.parse_type_parameter_name());
                            }
                            _ => {
                                self.skip_value();
                            }
                        }
                    }
                }
                self.eat(&Token::Comma);
            }
            self.eat(&Token::RBrace);
        }

        // `typeParameter` form short-circuits to the parameter name.
        if let Some(name) = type_parameter_name {
            return crate::types::TypeExpr::Generic(name);
        }
        // If rawType was a nested inline (typically FunctionType), the
        // outer GenericType is just a wrapper — the inner TypeExpr IS
        // the type. M3 typically puts no extra type/mult args on this
        // outer GenericType, but if there are some we discard them to
        // match Java's M3 representation (a GenericType with rawType =
        // FunctionType doesn't have meaningful typeArguments; the
        // FunctionType IS the type).
        if let Some(te) = raw_type_inline_te {
            return te;
        }
        self.build_pack_typeexpr(raw_type_name, type_arguments, multiplicity_arguments)
    }

    /// Parses the body of an `^FunctionType { parameters: [...],
    /// returnType: ..., returnMultiplicity: ... }` instance (after the
    /// `^FunctionType` classifier has been consumed) and returns a
    /// `TypeExpr::FunctionType`.
    ///
    /// Each entry in `parameters` is a `^VariableExpression { name,
    /// genericType: ^GenericType{...}, multiplicity: ... }` — we
    /// extract the genericType and multiplicity and discard the name
    /// (our `TypeExpr::FunctionType.parameters` is
    /// `Vec<(TypeExpr, Multiplicity)>`).
    fn parse_function_type_body(&mut self) -> crate::types::TypeExpr {
        let mut parameters: Vec<(crate::types::TypeExpr, Multiplicity)> = Vec::new();
        let mut return_type = crate::types::TypeExpr::Generic(SmolStr::new("Any"));
        let mut return_multiplicity = Multiplicity::PureOne;

        if !self.eat(&Token::LBrace) {
            return crate::types::TypeExpr::FunctionType {
                parameters,
                return_type: Box::new(return_type),
                return_multiplicity,
            };
        }

        loop {
            match self.peek() {
                Token::RBrace | Token::Eof => break,
                _ => {
                    let path_tail = self.parse_property_path_tail();
                    if !self.eat(&Token::Colon) {
                        self.skip_to_comma_or_brace();
                        continue;
                    }
                    match path_tail.as_str() {
                        "parameters" => {
                            parameters = self.parse_function_type_parameters();
                        }
                        "returnType" => {
                            return_type = self.parse_generic_type_full();
                        }
                        "returnMultiplicity" => {
                            return_multiplicity = self.parse_multiplicity_value();
                        }
                        _ => {
                            self.skip_value();
                        }
                    }
                }
            }
            self.eat(&Token::Comma);
        }
        self.eat(&Token::RBrace);

        crate::types::TypeExpr::FunctionType {
            parameters,
            return_type: Box::new(return_type),
            return_multiplicity,
        }
    }

    /// Parses a FunctionType `parameters: [...]` list. Each entry is a
    /// `^VariableExpression { name, genericType: ^GenericType{...},
    /// multiplicity: ^Multiplicity{...} | <ref> }`. Discards the name
    /// (our internal FunctionType doesn't carry param names).
    fn parse_function_type_parameters(&mut self) -> Vec<(crate::types::TypeExpr, Multiplicity)> {
        let mut out = Vec::new();
        if self.at(&Token::Caret) {
            // Single VariableExpression (no surrounding brackets).
            if let Some(pair) = self.parse_variable_expression_typeslot() {
                out.push(pair);
            }
            return out;
        }
        if !self.eat(&Token::LBrack) {
            self.skip_value();
            return out;
        }
        loop {
            match self.peek() {
                Token::RBrack | Token::Eof => break,
                Token::Caret => {
                    if let Some(pair) = self.parse_variable_expression_typeslot() {
                        out.push(pair);
                    }
                }
                _ => {
                    self.advance();
                }
            }
            self.eat(&Token::Comma);
        }
        self.eat(&Token::RBrack);
        out
    }

    /// Parses a single `^VariableExpression { name, genericType: …,
    /// multiplicity: … }` and returns the `(TypeExpr, Multiplicity)`
    /// pair from its `genericType` and `multiplicity` slots. Returns
    /// `None` when the inline shape can't be parsed.
    fn parse_variable_expression_typeslot(
        &mut self,
    ) -> Option<(crate::types::TypeExpr, Multiplicity)> {
        if !self.at(&Token::Caret) {
            return None;
        }
        self.advance(); // ^
        let _classifier = self.parse_classifier_path();

        let mut generic_type = crate::types::TypeExpr::Generic(SmolStr::new("Any"));
        let mut multiplicity = Multiplicity::PureOne;

        if self.eat(&Token::LBrace) {
            loop {
                match self.peek() {
                    Token::RBrace | Token::Eof => break,
                    _ => {
                        let path_tail = self.parse_property_path_tail();
                        if !self.eat(&Token::Colon) {
                            self.skip_to_comma_or_brace();
                            continue;
                        }
                        match path_tail.as_str() {
                            "genericType" => {
                                generic_type = self.parse_generic_type_full();
                            }
                            "multiplicity" => {
                                multiplicity = self.parse_multiplicity_value();
                            }
                            _ => {
                                self.skip_value();
                            }
                        }
                    }
                }
                self.eat(&Token::Comma);
            }
            self.eat(&Token::RBrace);
        }

        Some((generic_type, multiplicity))
    }

    /// Parses a Multiplicity value — either a direct ref like
    /// `Root.children[multiplicity].children[PureOne]` or an inline
    /// `^Multiplicity{...}` instance. Defaults to `PureOne` when the
    /// shape isn't recognised.
    fn parse_multiplicity_value(&mut self) -> Multiplicity {
        if self.at(&Token::Caret) {
            // Inline Multiplicity instance — skip and default. The
            // platform mostly uses direct refs.
            self.skip_inline_instance();
            return Multiplicity::PureOne;
        }
        self.parse_multiplicity_ref()
    }

    /// Parses `typeArguments: [^GenericType{…}, ^GenericType{…}, …]` and
    /// returns each entry as a `TypeExpr`. Class refs become
    /// `TypeExpr::Generic(class_name)` (resolved later by
    /// `resolve_m3_supertypes`); type-parameter refs become
    /// `TypeExpr::Generic(param_name)`.
    fn parse_type_argument_list(&mut self) -> Vec<crate::types::TypeExpr> {
        let mut out = Vec::new();
        // Single ^GenericType / ^FunctionType (no surrounding brackets).
        if self.at(&Token::Caret) {
            out.push(self.parse_generic_type_full());
            return out;
        }
        if !self.eat(&Token::LBrack) {
            self.skip_value();
            return out;
        }
        loop {
            match self.peek() {
                Token::RBrack | Token::Eof => break,
                Token::Caret => {
                    out.push(self.parse_generic_type_full());
                }
                _ => {
                    self.skip_value();
                }
            }
            self.eat(&Token::Comma);
        }
        self.eat(&Token::RBrack);
        out
    }

    /// Parses `multiplicityArguments: …` — either a single
    /// `Root.children[multiplicity].children[ZeroMany]` ref or a
    /// bracketed list. Inline multiplicity instances are tolerated
    /// (default `PureOne`).
    fn parse_multiplicity_argument_list(&mut self) -> Vec<Multiplicity> {
        let mut out = Vec::new();
        if self.eat(&Token::LBrack) {
            loop {
                match self.peek() {
                    Token::RBrack | Token::Eof => break,
                    Token::Caret => {
                        // Inline ^Multiplicity{...}; not used by m3.pure today.
                        self.skip_inline_instance();
                        out.push(Multiplicity::PureOne);
                    }
                    _ => {
                        out.push(self.parse_multiplicity_ref());
                    }
                }
                self.eat(&Token::Comma);
            }
            self.eat(&Token::RBrack);
        } else {
            out.push(self.parse_multiplicity_ref());
        }
        out
    }

    /// Parses `^TypeParameter { name: 'T', … }` and returns just the name.
    fn parse_type_parameter_name(&mut self) -> SmolStr {
        if !self.eat(&Token::Caret) {
            self.skip_value();
            return SmolStr::new("");
        }
        let _classifier = self.parse_classifier_path();
        let mut name = SmolStr::new("");
        if self.eat(&Token::LBrace) {
            loop {
                match self.peek() {
                    Token::RBrace | Token::Eof => break,
                    _ => {
                        let path_tail = self.parse_property_path_tail();
                        if !self.eat(&Token::Colon) {
                            self.skip_to_comma_or_brace();
                            continue;
                        }
                        if path_tail.as_str() == "name" {
                            name = self.parse_string_literal();
                        } else {
                            self.skip_value();
                        }
                    }
                }
                self.eat(&Token::Comma);
            }
            self.eat(&Token::RBrace);
        }
        name
    }

    /// Folds a `(rawType_name, type_arguments, multiplicity_arguments)`
    /// triple into a single `TypeExpr`. With no args, it stays
    /// `Generic(name)` so the existing forward-ref resolution in
    /// `resolve_m3_supertypes` handles it. With args, it becomes a
    /// `Named` whose `element` is a placeholder (`Any`) — the resolver
    /// detects the marker via `value_arguments[0] == String(rawType)`
    /// and rewrites the element while preserving the type/mult args.
    fn build_pack_typeexpr(
        &self,
        raw: SmolStr,
        type_arguments: Vec<crate::types::TypeExpr>,
        multiplicity_arguments: Vec<Multiplicity>,
    ) -> crate::types::TypeExpr {
        if type_arguments.is_empty() && multiplicity_arguments.is_empty() {
            crate::types::TypeExpr::Generic(raw)
        } else {
            // Sidecar inline encoding: ANY_ID + value_arguments[0] = String(raw_name).
            // resolve_m3_supertypes detects this pattern and rewrites
            // element to the resolved class id.
            crate::types::TypeExpr::Named {
                element: crate::bootstrap::ANY_ID,
                type_arguments,
                multiplicity_arguments,
                value_arguments: vec![crate::types::ConstValue::String(raw.to_string())],
                source_info: None,
            }
        }
    }

    /// Parses a string literal token, returning its content. Falls back
    /// to an empty `SmolStr` if the next token isn't a string.
    fn parse_string_literal(&mut self) -> SmolStr {
        if let Token::StringLit(s) = self.peek().clone() {
            self.advance();
            s
        } else {
            self.skip_value();
            SmolStr::new("")
        }
    }

    /// Parses a multiplicity reference like `Root...multiplicity.children[PureOne]`
    /// and returns the corresponding `Multiplicity` variant.
    fn parse_multiplicity_ref(&mut self) -> Multiplicity {
        if self.at(&Token::Caret) {
            // Inline multiplicity instance — skip and default to PureOne
            self.skip_inline_instance();
            return Multiplicity::PureOne;
        }

        let name = self.parse_element_ref_tail();
        match name.as_str() {
            "ZeroOne" => Multiplicity::ZeroOrOne,
            "ZeroMany" => Multiplicity::ZeroOrMany,
            "OneMany" => Multiplicity::OneOrMany,
            _ => Multiplicity::PureOne,
        }
    }

    fn parse_enumeration_body(&mut self, name: &SmolStr, package_segments: &[SmolStr]) {
        let mut values = Vec::new();

        if !self.at(&Token::LBrace) {
            self.alloc_element(
                name,
                package_segments,
                Element::Enumeration(Enumeration {
                    values: vec![],
                    stereotypes: vec![],
                    tagged_values: vec![],
                }),
            );
            return;
        }

        self.advance(); // {

        loop {
            match self.peek() {
                Token::RBrace | Token::Eof => break,
                _ => {
                    let path_tail = self.parse_property_path_tail();
                    if !self.eat(&Token::Colon) {
                        self.skip_to_comma_or_brace();
                        continue;
                    }

                    if path_tail.as_str() == "values" {
                        values = self.parse_enum_values();
                    } else {
                        self.skip_value();
                    }
                }
            }
            self.eat(&Token::Comma);
        }

        self.eat(&Token::RBrace);

        self.alloc_element(
            name,
            package_segments,
            Element::Enumeration(Enumeration {
                values,
                stereotypes: vec![],
                tagged_values: vec![],
            }),
        );
    }

    /// Parses `[^EnumType Value1 {...}, ^EnumType Value2 {...}]`.
    fn parse_enum_values(&mut self) -> Vec<EnumValue> {
        let mut values = Vec::new();

        if self.eat(&Token::LBrack) {
            loop {
                match self.peek() {
                    Token::RBrack | Token::Eof => break,
                    Token::Caret => {
                        self.advance(); // ^
                        let _classifier = self.parse_classifier_path();
                        let value_name = self.expect_ident();

                        // Skip body
                        if self.at(&Token::LBrace) {
                            self.skip_balanced_braces();
                        }

                        values.push(EnumValue {
                            name: value_name,
                            source_info: synthetic_source(),
                            stereotypes: vec![],
                            tagged_values: vec![],
                        });
                    }
                    _ => {
                        self.advance();
                    }
                }
                self.eat(&Token::Comma);
            }
            self.eat(&Token::RBrack);
        } else {
            self.skip_value();
        }

        values
    }

    fn parse_profile_body(&mut self, name: &SmolStr, package_segments: &[SmolStr]) {
        let mut stereotypes = Vec::new();
        let mut tags = Vec::new();

        if !self.at(&Token::LBrace) {
            self.alloc_element(
                name,
                package_segments,
                Element::Profile(Profile {
                    stereotypes: vec![],
                    tags: vec![],
                }),
            );
            return;
        }

        self.advance(); // {

        loop {
            match self.peek() {
                Token::RBrace | Token::Eof => break,
                _ => {
                    let path_tail = self.parse_property_path_tail();
                    if !self.eat(&Token::Colon) {
                        self.skip_to_comma_or_brace();
                        continue;
                    }

                    match path_tail.as_str() {
                        "p_stereotypes" => {
                            stereotypes = self.parse_annotation_list();
                        }
                        "p_tags" => {
                            tags = self.parse_annotation_list();
                        }
                        _ => {
                            self.skip_value();
                        }
                    }
                }
            }
            self.eat(&Token::Comma);
        }

        self.eat(&Token::RBrace);

        // m3 bootstrap parser doesn't carry per-name source info — the
        // IDE can't navigate to declarations inside m3-bootstrapped
        // Profiles (e.g. `meta::pure::profiles::test`) without these.
        // Wrap each name with the bootstrap-source sentinel so the
        // shape matches parser-loaded profiles; the reference index
        // recognises the sentinel and skips emitting entries for it.
        use crate::nodes::profile::bootstrap_spanned_name;
        self.alloc_element(
            name,
            package_segments,
            Element::Profile(Profile {
                stereotypes: stereotypes
                    .into_iter()
                    .map(bootstrap_spanned_name)
                    .collect(),
                tags: tags.into_iter().map(bootstrap_spanned_name).collect(),
            }),
        );
    }

    /// Parses `[^Stereotype name {value: 'x'}, ...]` into a list of names.
    fn parse_annotation_list(&mut self) -> Vec<SmolStr> {
        let mut names = Vec::new();

        if self.eat(&Token::LBrack) {
            loop {
                match self.peek() {
                    Token::RBrack | Token::Eof => break,
                    Token::Caret => {
                        self.advance(); // ^
                        let _classifier = self.parse_classifier_path();
                        let anno_name = self.expect_ident();

                        // Skip body
                        if self.at(&Token::LBrace) {
                            self.skip_balanced_braces();
                        }

                        names.push(anno_name);
                    }
                    _ => {
                        self.advance();
                    }
                }
                self.eat(&Token::Comma);
            }
            self.eat(&Token::RBrack);
        } else {
            self.skip_value();
        }

        names
    }

    fn parse_multiplicity_body(&mut self, name: &SmolStr, package_segments: &[SmolStr]) {
        // Map well-known multiplicity names to Multiplicity variants
        let mult = match name.as_str() {
            "PureZero" => Multiplicity::Range {
                lower: 0,
                upper: Some(0),
            },
            "ZeroMany" => Multiplicity::ZeroOrMany,
            "ZeroOne" => Multiplicity::ZeroOrOne,
            "OneMany" => Multiplicity::OneOrMany,
            _ => Multiplicity::PureOne,
        };

        // Skip body
        if self.at(&Token::LBrace) {
            self.skip_balanced_braces();
        }

        self.alloc_element(
            name,
            package_segments,
            Element::PackageableMultiplicity(mult),
        );
    }

    /// Parses a property-assignment path like
    /// `Root.children[...].children[Class].properties[properties]`
    /// and returns the tail bracket content (e.g., `"properties"`).
    fn parse_property_path_tail(&mut self) -> SmolStr {
        let mut tail = self.expect_ident();

        while self.at(&Token::Dot) {
            self.advance(); // .
            let segment = self.expect_ident();
            if self.at(&Token::LBrack) {
                self.advance(); // [
                tail = self.expect_ident();
                self.expect(&Token::RBrack);
            } else {
                tail = segment;
            }
        }

        tail
    }

    /// Parses an element reference path and returns the last bracket content.
    /// e.g., `Root.children[meta].children[...].children[PureOne]` → `"PureOne"`.
    fn parse_element_ref_tail(&mut self) -> SmolStr {
        let mut tail = self.expect_ident();

        while self.at(&Token::Dot) {
            self.advance(); // .
            let segment = self.expect_ident();
            if self.at(&Token::LBrack) {
                self.advance(); // [
                tail = self.expect_ident();
                self.expect(&Token::RBrack);
            } else {
                tail = segment;
            }
        }

        tail
    }

    /// Skips a balanced `{ ... }` block.
    fn skip_balanced_braces(&mut self) {
        self.expect(&Token::LBrace);
        let mut depth = 1u32;
        loop {
            match self.advance() {
                Token::LBrace => depth += 1,
                Token::RBrace => {
                    depth -= 1;
                    if depth == 0 {
                        return;
                    }
                }
                Token::Eof => return,
                _ => {}
            }
        }
    }

    /// Skips a value in a property assignment.
    fn skip_value(&mut self) {
        match self.peek() {
            Token::LBrack => {
                // Array value — skip balanced brackets
                self.advance();
                let mut depth = 1u32;
                loop {
                    match self.advance() {
                        Token::LBrack => depth += 1,
                        Token::RBrack => {
                            depth -= 1;
                            if depth == 0 {
                                return;
                            }
                        }
                        Token::Eof => return,
                        _ => {}
                    }
                }
            }
            Token::Caret => {
                self.skip_inline_instance();
            }
            Token::Ident(_) => {
                // Element reference — consume path
                let _ = self.parse_element_ref_tail();
            }
            _ => {
                self.advance();
            }
        }
    }

    /// Skips an inline `^ClassifierPath Name? { body }` instance.
    fn skip_inline_instance(&mut self) {
        self.advance(); // ^
        let _ = self.parse_classifier_path();

        // Optional name
        if let Token::Ident(_) = self.peek() {
            // Check if this is a name followed by { or if it's something else
            let saved_pos = self.pos;
            self.advance(); // consume potential name
            if self.at(&Token::LBrace) {
                self.skip_balanced_braces();
                return;
            }
            // Not a body — restore position
            self.pos = saved_pos;
        }

        if self.at(&Token::LBrace) {
            self.skip_balanced_braces();
        }
    }

    /// Advances until we find a comma or closing brace at the current depth.
    fn skip_to_comma_or_brace(&mut self) {
        let mut depth = 0u32;
        loop {
            match self.peek() {
                Token::Eof => return,
                Token::Comma | Token::RBrace if depth == 0 => return,
                Token::LBrace | Token::LBrack => {
                    depth += 1;
                    self.advance();
                }
                Token::RBrace | Token::RBrack => {
                    depth = depth.saturating_sub(1);
                    self.advance();
                }
                _ => {
                    self.advance();
                }
            }
        }
    }
}

#[cfg(test)]
mod tests {
    use super::*;

    fn parse_fragment(source: &str) -> (Arena<ElementNode>, Arena<Element>, Vec<M3Registration>) {
        let mut nodes = Arena::new();
        let mut elements = Arena::new();
        let regs = parse_m3_into_chunk(source, &mut nodes, &mut elements);
        (nodes, elements, regs)
    }

    #[test]
    fn parse_profile_with_stereotypes() {
        let src = r"
^Root.children[meta].children[pure].children[metamodel].children[extension].children[Profile] ProtocolInfo @Root.children[meta].children[pure].children[metamodel].children
{
    Root.children[meta].children[pure].children[metamodel].children[PackageableElement].properties[package] : Root.children[meta].children[pure].children[metamodel],
    Root.children[meta].children[pure].children[metamodel].children[ModelElement].properties[name] : 'ProtocolInfo',
    Root.children[meta].children[pure].children[metamodel].children[extension].children[Profile].properties[p_stereotypes] : [
        ^Root.children[meta].children[pure].children[metamodel].children[extension].children[Stereotype] inferred
        {
            Root.children[meta].children[pure].children[metamodel].children[extension].children[Annotation].properties[profile] : Root.children[meta].children[pure].children[metamodel].children[ProtocolInfo],
            Root.children[meta].children[pure].children[metamodel].children[extension].children[Annotation].properties[value] : 'inferred'
        },
        ^Root.children[meta].children[pure].children[metamodel].children[extension].children[Stereotype] excluded
        {
            Root.children[meta].children[pure].children[metamodel].children[extension].children[Annotation].properties[profile] : Root.children[meta].children[pure].children[metamodel].children[ProtocolInfo],
            Root.children[meta].children[pure].children[metamodel].children[extension].children[Annotation].properties[value] : 'excluded'
        }
    ]
}
        ";

        let (nodes, elements, regs) = parse_fragment(src);
        assert_eq!(regs.len(), 1);
        assert_eq!(nodes.get(0).name, "ProtocolInfo");

        if let Element::Profile(p) = elements.get(0) {
            let names: Vec<&str> = p.stereotypes.iter().map(|s| s.value.as_str()).collect();
            assert_eq!(names, vec!["inferred", "excluded"]);
            assert!(p.tags.is_empty());
        } else {
            panic!("Expected Profile, got {:?}", elements.get(0));
        }
    }

    #[test]
    fn parse_enumeration_with_values() {
        let src = r"
^Root.children[meta].children[pure].children[metamodel].children[type].children[Enumeration] AggregationKind @Root.children[meta].children[pure].children[metamodel].children[function].children[property].children
{
    Root.children[meta].children[pure].children[metamodel].children[type].children[Enumeration].properties[values]:
             [
                 ^Root.children[meta].children[pure].children[metamodel].children[function].children[property].children[AggregationKind] None {Root.children[meta].children[pure].children[metamodel].children[type].children[Enum].properties[name] : 'None'},
                 ^Root.children[meta].children[pure].children[metamodel].children[function].children[property].children[AggregationKind] Shared {Root.children[meta].children[pure].children[metamodel].children[type].children[Enum].properties[name] : 'Shared'},
                 ^Root.children[meta].children[pure].children[metamodel].children[function].children[property].children[AggregationKind] Composite {Root.children[meta].children[pure].children[metamodel].children[type].children[Enum].properties[name] : 'Composite'}
             ],
    Root.children[meta].children[pure].children[metamodel].children[ModelElement].properties[name]:'AggregationKind'
}
        ";

        let (_nodes, elements, regs) = parse_fragment(src);
        assert_eq!(regs.len(), 1);

        if let Element::Enumeration(e) = elements.get(0) {
            let names: Vec<&str> = e.values.iter().map(|v| v.name.as_str()).collect();
            assert_eq!(names, vec!["None", "Shared", "Composite"]);
        } else {
            panic!("Expected Enumeration");
        }
    }

    #[test]
    fn parse_package_skipped() {
        let src = r"
^Package meta @Root.children
{
    Root.children[meta].children[pure].children[metamodel].children[ModelElement].properties[name] : 'meta',
    Package.properties[children] : [],
    Root.children[meta].children[pure].children[metamodel].children[PackageableElement].properties[package] : Root
}
        ";

        let (_nodes, _elements, regs) = parse_fragment(src);
        // Packages don't produce elements
        assert_eq!(regs.len(), 0);
    }

    #[test]
    fn parse_real_m3_pure() {
        let source = include_str!(
            "../../../../legend-pure-core/legend-pure-m3-core/src/main/resources/platform/pure/grammar/m3.pure"
        );

        let (nodes, elements, regs) = parse_fragment(source);

        // Should have parsed a significant number of elements
        assert!(
            regs.len() > 80,
            "expected > 80 M3 elements from m3.pure, got {}",
            regs.len()
        );

        // Count registrations with and without package segments
        let with_pkg = regs
            .iter()
            .filter(|r| !r.package_segments.is_empty())
            .count();
        eprintln!(
            "{} of {} registrations have package segments",
            with_pkg,
            regs.len()
        );

        // Verify ProtocolInfo profile has stereotypes
        let protocol_idx = (0..nodes.len())
            .find(|&i| nodes.get(i).name == "ProtocolInfo")
            .expect("ProtocolInfo should exist");
        if let Element::Profile(p) = elements.get(protocol_idx) {
            let names: Vec<&str> = p.stereotypes.iter().map(|s| s.value.as_str()).collect();
            assert_eq!(
                names,
                vec!["inferred", "excluded"],
                "ProtocolInfo should have stereotypes inferred and excluded"
            );
        } else {
            panic!("ProtocolInfo should be a Profile");
        }

        // Verify AggregationKind enumeration has values
        let agg_idx = (0..nodes.len())
            .find(|&i| nodes.get(i).name == "AggregationKind")
            .expect("AggregationKind should exist");
        if let Element::Enumeration(e) = elements.get(agg_idx) {
            let names: Vec<&str> = e.values.iter().map(|v| v.name.as_str()).collect();
            assert_eq!(names, vec!["None", "Shared", "Composite"]);
        } else {
            panic!("AggregationKind should be an Enumeration");
        }

        // Verify PureOne multiplicity exists
        let pure_one_idx = (0..nodes.len())
            .find(|&i| nodes.get(i).name == "PureOne")
            .expect("PureOne should exist");
        assert!(
            matches!(
                elements.get(pure_one_idx),
                Element::PackageableMultiplicity(Multiplicity::PureOne)
            ),
            "PureOne should be PackageableMultiplicity(PureOne)"
        );

        eprintln!("Parsed {} M3 elements from m3.pure", regs.len());
    }
}
