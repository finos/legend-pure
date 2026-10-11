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

package org.finos.legend.pure.rust;

/**
 * One node of the Java graph, described for the Rust side in a single
 * call. Plain fields read by JNI; the kind constants are mirrored in
 * {@code crates/jni/src/java_node.rs}.
 */
final class NodeDescription
{
    static final int BOOLEAN = 0;
    static final int INTEGER = 1;
    static final int FLOAT = 2;
    static final int DECIMAL = 3;
    static final int STRING = 4;
    static final int DATE = 5;
    static final int LATEST_DATE = 6;
    static final int STRICT_TIME = 7;
    static final int BYTE = 8;
    static final int ENUM_VALUE = 10;
    static final int PACKAGEABLE_ELEMENT = 11;
    static final int NODE = 12;
    static final int RUST_OBJECT = 13;

    int kind;
    long id;
    String classifier;
    String name;
    String userPath;
    String payload;
    int year;
    int month;
    int day;
    int hour;
    int minute;
    int second;
    /**
     * 0 year, 1 month, 2 day, 3 hour, 4 minute, 5 second, 6 subsecond.
     */
    int precision;
    String subsecond;
    /**
     * For a lambda value the engine created for one evaluation (a lambda with
     * the scope it was evaluated in): the bridge id of the definition it
     * stands for, the same for every evaluation of that lambda; -1 otherwise.
     */
    long definition = -1;
    /**
     * Source span, when the node has one ({@code sourceId == null} otherwise):
     * the anchor line/column plus the end position, as {@code SourceInformation}
     * reports them.
     */
    String sourceId;
    int line;
    int column;
    int endLine;
    int endColumn;
}
