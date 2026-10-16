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

//! The header is the contract: its function declarations, its version
//! macro and its constants agree with the crate, in both directions.

use std::collections::{BTreeMap, BTreeSet};
use std::path::{Path, PathBuf};

use legend_pure_evaluator::bridge::description::kind;
use pure_rust_ffi::EXPORTS;
use pure_rust_ffi::abi::{ABI_VERSION, KIND_MAP, crossing, failure_kind, status};

fn crate_dir() -> PathBuf {
    PathBuf::from(env!("CARGO_MANIFEST_DIR"))
}

fn header() -> String {
    let path = crate_dir().join("include").join("pure_rust.h");
    std::fs::read_to_string(&path).unwrap_or_else(|e| panic!("{}: {e}", path.display()))
}

/// The header without its comments.
fn strip_comments(text: &str) -> String {
    let mut out = String::with_capacity(text.len());
    let mut rest = text;
    while !rest.is_empty() {
        if let Some(stripped) = rest.strip_prefix("/*") {
            let end = stripped.find("*/").expect("an unterminated block comment");
            rest = &stripped[end + 2..];
        } else if let Some(stripped) = rest.strip_prefix("//") {
            let end = stripped.find('\n').unwrap_or(stripped.len());
            rest = &stripped[end..];
        } else {
            let mut chars = rest.chars();
            let c = chars.next().expect("not empty");
            out.push(c);
            rest = chars.as_str();
        }
    }
    out
}

fn is_identifier(c: char) -> bool {
    c.is_ascii_alphanumeric() || c == '_'
}

/// Every `pure_*` identifier the header declares as a function: one
/// followed by `(` that does not open a function-pointer declarator.
fn header_functions() -> BTreeSet<String> {
    let text = strip_comments(&header());
    let bytes = text.as_bytes();
    let mut found = BTreeSet::new();
    let mut i = 0;
    while let Some(at) = text[i..].find("pure_") {
        let start = i + at;
        let preceded_by_identifier = start > 0 && is_identifier(char::from(bytes[start - 1]));
        let mut end = start;
        while end < text.len() && is_identifier(char::from(bytes[end])) {
            end += 1;
        }
        i = end;
        if preceded_by_identifier {
            continue;
        }
        let after = text[end..].trim_start();
        if let Some(args) = after.strip_prefix('(')
            && !args.trim_start().starts_with('*')
        {
            found.insert(text[start..end].to_owned());
        }
    }
    found
}

/// Every `#[unsafe(no_mangle)]` function under `src/`.
fn source_exports() -> BTreeSet<String> {
    fn visit(dir: &Path, found: &mut BTreeSet<String>) {
        for entry in std::fs::read_dir(dir).expect("src") {
            let path = entry.expect("entry").path();
            if path.is_dir() {
                visit(&path, found);
            } else if path.extension().is_some_and(|e| e == "rs") {
                let text = std::fs::read_to_string(&path).expect("source");
                let mut lines = text.lines();
                while let Some(line) = lines.next() {
                    if line.trim() != "#[unsafe(no_mangle)]" {
                        continue;
                    }
                    let signature = lines.next().expect("a function after no_mangle");
                    let name = signature
                        .trim()
                        .strip_prefix("pub ")
                        .map(|s| s.strip_prefix("unsafe ").unwrap_or(s))
                        .and_then(|s| s.strip_prefix("extern \"C\" fn "))
                        .and_then(|s| s.split(['(', '<']).next())
                        .unwrap_or_else(|| panic!("not an export: {signature}"));
                    found.insert(name.to_owned());
                }
            }
        }
    }
    let mut found = BTreeSet::new();
    visit(&crate_dir().join("src"), &mut found);
    found
}

/// Every `PURE_* = n` enumerator of the header, and `PURE_ABI_VERSION`.
fn header_constants() -> BTreeMap<String, i64> {
    let text = strip_comments(&header());
    let mut found = BTreeMap::new();
    for statement in text.split([',', ';', '\n']) {
        let statement = statement.trim();
        if let Some((name, value)) = statement.split_once('=')
            && let Some(name) = name.trim().strip_prefix("PURE_")
            && let Ok(value) = value.trim().parse::<i64>()
        {
            found.insert(format!("PURE_{name}"), value);
        }
    }
    for line in text.lines() {
        if let Some(rest) = line.trim().strip_prefix("#define PURE_ABI_VERSION ") {
            found.insert(
                "PURE_ABI_VERSION".to_owned(),
                rest.trim().parse().expect("a version"),
            );
        }
    }
    found
}

#[test]
fn the_header_declares_exactly_the_exports_the_crate_lists() {
    let listed: BTreeSet<String> = EXPORTS.iter().map(|s| (*s).to_owned()).collect();
    assert_eq!(listed.len(), EXPORTS.len(), "EXPORTS repeats a name");
    assert_eq!(header_functions(), listed, "header against EXPORTS");
}

#[test]
fn the_crate_exports_exactly_the_functions_it_lists() {
    let listed: BTreeSet<String> = EXPORTS.iter().map(|s| (*s).to_owned()).collect();
    assert_eq!(
        source_exports(),
        listed,
        "no_mangle functions against EXPORTS"
    );
}

#[test]
fn the_header_names_every_function_with_the_one_prefix() {
    for name in EXPORTS {
        assert!(name.starts_with("pure_"), "{name}");
    }
}

#[test]
fn the_headers_constants_are_the_crates() {
    let expected: BTreeMap<&str, i64> = [
        ("PURE_ABI_VERSION", i64::from(ABI_VERSION)),
        ("PURE_STATUS_OK", i64::from(status::OK)),
        ("PURE_STATUS_FAILED", i64::from(status::FAILED)),
        ("PURE_STATUS_ABSENT", i64::from(status::ABSENT)),
        ("PURE_KIND_BOOLEAN", i64::from(kind::BOOLEAN)),
        ("PURE_KIND_INTEGER", i64::from(kind::INTEGER)),
        ("PURE_KIND_FLOAT", i64::from(kind::FLOAT)),
        ("PURE_KIND_DECIMAL", i64::from(kind::DECIMAL)),
        ("PURE_KIND_STRING", i64::from(kind::STRING)),
        ("PURE_KIND_DATE", i64::from(kind::DATE)),
        ("PURE_KIND_LATEST_DATE", i64::from(kind::LATEST_DATE)),
        ("PURE_KIND_STRICT_TIME", i64::from(kind::STRICT_TIME)),
        ("PURE_KIND_BYTE", i64::from(kind::BYTE)),
        ("PURE_KIND_ENUM_VALUE", i64::from(kind::ENUM_VALUE)),
        (
            "PURE_KIND_PACKAGEABLE_ELEMENT",
            i64::from(kind::PACKAGEABLE_ELEMENT),
        ),
        ("PURE_KIND_NODE", i64::from(kind::NODE)),
        ("PURE_KIND_RUST_OBJECT", i64::from(kind::RUST_OBJECT)),
        ("PURE_KIND_MAP", i64::from(KIND_MAP)),
        ("PURE_PRECISION_YEAR", 0),
        ("PURE_PRECISION_MONTH", 1),
        ("PURE_PRECISION_DAY", 2),
        ("PURE_PRECISION_HOUR", 3),
        ("PURE_PRECISION_MINUTE", 4),
        ("PURE_PRECISION_SECOND", 5),
        ("PURE_PRECISION_SUBSECOND", 6),
        (
            "PURE_FAILURE_EXECUTION_ERROR",
            i64::from(failure_kind::EXECUTION_ERROR),
        ),
        (
            "PURE_FAILURE_ASSERTION_FAILED",
            i64::from(failure_kind::ASSERTION_FAILED),
        ),
        (
            "PURE_FAILURE_CONSTRAINT_VIOLATION",
            i64::from(failure_kind::CONSTRAINT_VIOLATION),
        ),
        (
            "PURE_FAILURE_FUNCTION_NOT_FOUND",
            i64::from(failure_kind::FUNCTION_NOT_FOUND),
        ),
        (
            "PURE_FAILURE_UNSUPPORTED",
            i64::from(failure_kind::UNSUPPORTED),
        ),
        (
            "PURE_FAILURE_NOT_IMPLEMENTED",
            i64::from(failure_kind::NOT_IMPLEMENTED),
        ),
        ("PURE_FAILURE_PANIC", i64::from(failure_kind::PANIC)),
        ("PURE_CROSSING_ELEMENT", i64::from(crossing::ELEMENT)),
        ("PURE_CROSSING_ENUM_VALUE", i64::from(crossing::ENUM_VALUE)),
        ("PURE_CROSSING_HOST_NODE", i64::from(crossing::HOST_NODE)),
        (
            "PURE_CROSSING_RUST_OBJECT",
            i64::from(crossing::RUST_OBJECT),
        ),
        ("PURE_CROSSING_PRIMITIVE", i64::from(crossing::PRIMITIVE)),
    ]
    .into_iter()
    .collect();
    let expected: BTreeMap<String, i64> = expected
        .into_iter()
        .map(|(k, v)| (k.to_owned(), v))
        .collect();
    assert_eq!(header_constants(), expected);
}
