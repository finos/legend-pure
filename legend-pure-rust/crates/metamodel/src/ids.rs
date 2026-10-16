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

//! Typed index wrappers for the Pure semantic graph.
//!
//! Every node in the graph is referenced by a lightweight, `Copy` index.
//! The segmented [`ElementId`] enables zero-rewrite chunk merging — when a
//! new chunk is added, all existing IDs remain valid.

use std::fmt;

/// A reference to any element in the Pure model graph.
///
/// In M3, `Package extends PackageableElement` — packages are elements.
/// This enum captures both:
///
/// - **`InstanceId`**: an element stored in a chunk arena, addressed by
///   `(chunk_id, local_idx)`. Enables O(1) model merging.
/// - **`Package`**: a package in the global `global_packages` arena,
///   addressed by [`PackageId`]. Packages span chunks.
#[derive(Debug, Clone, Copy, PartialEq, Eq, Hash)]
pub enum ElementId {
    /// An element instance in a chunk arena.
    InstanceId {
        /// Which chunk owns this element (0 = bootstrap).
        chunk_id: u16,
        /// Index within the chunk's `nodes` and `elements` arenas.
        local_idx: u32,
    },
    /// A package in the global package arena.
    Package(PackageId),
}

impl fmt::Display for ElementId {
    fn fmt(&self, f: &mut fmt::Formatter<'_>) -> fmt::Result {
        match self {
            Self::InstanceId {
                chunk_id,
                local_idx,
            } => write!(f, "{chunk_id}:{local_idx}"),
            Self::Package(pkg_id) => write!(f, "pkg:{}", pkg_id.0),
        }
    }
}

impl ElementId {
    /// Returns the `local_idx` for an `InstanceId` variant.
    ///
    /// # Panics
    ///
    /// Panics if called on a `Package` variant.
    #[must_use]
    pub fn local_idx(self) -> u32 {
        match self {
            Self::InstanceId { local_idx, .. } => local_idx,
            Self::Package(_) => panic!("local_idx called on Package element {self}"),
        }
    }

    /// Returns `true` if this is a `Package` variant.
    #[must_use]
    pub fn is_package(self) -> bool {
        matches!(self, Self::Package(_))
    }

    /// Returns the `PackageId` if this is a `Package` variant.
    #[must_use]
    pub fn package_id(self) -> Option<PackageId> {
        match self {
            Self::Package(id) => Some(id),
            Self::InstanceId { .. } => None,
        }
    }
}

/// An index into the global package arena on [`PureModel`](crate::model::PureModel).
///
/// Unlike [`ElementId`], packages are not chunked — they live in a single
/// global arena because a package like `meta::pure` can span multiple chunks.
#[derive(Debug, Clone, Copy, PartialEq, Eq, Hash)]
pub struct PackageId(pub u32);

impl fmt::Display for PackageId {
    fn fmt(&self, f: &mut fmt::Formatter<'_>) -> fmt::Result {
        write!(f, "pkg:{}", self.0)
    }
}

/// An index into the relation type interner on `PureModel`.
///
/// Relation types are anonymous structural types (column bags) that are
/// interned for deduplication. This ID is used by `TypeExpr::Relation`.
#[derive(Debug, Clone, Copy, PartialEq, Eq, Hash)]
pub struct RelationId(pub u32);

impl fmt::Display for RelationId {
    fn fmt(&self, f: &mut fmt::Formatter<'_>) -> fmt::Result {
        write!(f, "rel:{}", self.0)
    }
}

#[cfg(test)]
mod tests {
    use super::*;

    #[test]
    fn element_id_is_copy_and_eq() {
        let a = ElementId::InstanceId {
            chunk_id: 0,
            local_idx: 5,
        };
        let b = a;
        assert_eq!(a, b);
        assert_eq!(a.to_string(), "0:5");
    }

    #[test]
    fn element_id_hash_works() {
        use std::collections::HashSet;
        let mut set = HashSet::new();
        set.insert(ElementId::InstanceId {
            chunk_id: 0,
            local_idx: 0,
        });
        set.insert(ElementId::InstanceId {
            chunk_id: 0,
            local_idx: 1,
        });
        set.insert(ElementId::InstanceId {
            chunk_id: 0,
            local_idx: 0,
        });
        assert_eq!(set.len(), 2);
    }

    #[test]
    fn element_id_different_chunks_not_equal() {
        let a = ElementId::InstanceId {
            chunk_id: 0,
            local_idx: 0,
        };
        let b = ElementId::InstanceId {
            chunk_id: 1,
            local_idx: 0,
        };
        assert_ne!(a, b);
    }

    #[test]
    fn element_id_package_variant() {
        let a = ElementId::Package(PackageId(7));
        let b = a;
        assert_eq!(a, b);
        assert_eq!(a.to_string(), "pkg:7");
        assert_ne!(
            a,
            ElementId::InstanceId {
                chunk_id: 0,
                local_idx: 7
            }
        );
    }

    #[test]
    fn package_id_is_copy_and_eq() {
        let a = PackageId(3);
        let b = a;
        assert_eq!(a, b);
        assert_eq!(a.to_string(), "pkg:3");
    }

    #[test]
    fn relation_id_display() {
        let r = RelationId(42);
        assert_eq!(r.to_string(), "rel:42");
    }
}
