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
 * call. Plain fields a transport reads; the kind constants are mirrored in
 * {@code crates/jni/src/java_node.rs}.
 */
public final class NodeDescription
{
    public static final int BOOLEAN = 0;
    public static final int INTEGER = 1;
    public static final int FLOAT = 2;
    public static final int DECIMAL = 3;
    public static final int STRING = 4;
    public static final int DATE = 5;
    public static final int LATEST_DATE = 6;
    public static final int STRICT_TIME = 7;
    public static final int BYTE = 8;
    public static final int ENUM_VALUE = 10;
    public static final int PACKAGEABLE_ELEMENT = 11;
    public static final int NODE = 12;
    public static final int RUST_OBJECT = 13;

    public int kind;
    public long id;
    public String classifier;
    public String name;
    public String userPath;
    public String payload;
    public int year;
    public int month;
    public int day;
    public int hour;
    public int minute;
    public int second;
    /**
     * 0 year, 1 month, 2 day, 3 hour, 4 minute, 5 second, 6 subsecond.
     */
    public int precision;
    public String subsecond;
    /**
     * For a lambda value the engine created for one evaluation (a lambda with
     * the scope it was evaluated in): the bridge id of the definition it
     * stands for, the same for every evaluation of that lambda; -1 otherwise.
     */
    public long definition = -1;
    /**
     * Source span, when the node has one ({@code sourceId == null} otherwise):
     * the anchor line/column plus the end position, as {@code SourceInformation}
     * reports them.
     */
    public String sourceId;
    public int line;
    public int column;
    public int endLine;
    public int endColumn;
}
