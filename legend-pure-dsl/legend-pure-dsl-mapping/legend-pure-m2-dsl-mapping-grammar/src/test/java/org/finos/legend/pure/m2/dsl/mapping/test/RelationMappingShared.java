// Copyright 2024 Goldman Sachs
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

package org.finos.legend.pure.m2.dsl.mapping.test;

public class RelationMappingShared
{
    public static final String RELATION_MAPPING_CLASS_SOURCE = "###Pure\n" +
            "Class my::Person\n" +
            "{\n" +
            "  firstName: String[1];\n" +
            "  age: Integer[1];\n" +
            "  firmId: Integer[1];\n" +
            "  address: my::Address[1];\n" +
            "}\n" +
            "\n" +
            "Class my::Firm\n" +
            "{\n" +
            "  id: Integer[1];\n" +
            "  legalName: String[1];\n" +
            "  clientNames: String[*];\n" +
            "}\n" +
            "\n" +
            "Class my::Address\n" +
            "{\n" +
            "  city: String[1];\n" +
            "}\n" +
            "\n" +
            "Association my::Person_Firm\n" +
            "{\n" +
            "  employees: my::Person[*];\n" +
            "  firm: my::Firm[1];\n" +
            "}\n";

    public static final String RELATION_MAPPING_FUNCTION_SOURCE = "###Pure\n" +
            "import meta::pure::metamodel::relation::*;\n" +
            "function my::personFunction(): Relation<Any>[1]\n" +
            "{\n" +
            "  1->cast(@Relation<(FIRSTNAME:String, AGE:Integer, FIRMID:Integer, CITY:String)>);\n" +
            "}\n" +
            "function my::firmFunction(): Relation<Any>[1]\n" +
            "{\n" +
            "  1->cast(@Relation<(ID:Integer, LEGALNAME:String)>);\n" +
            "}\n" +
            "function my::integerFunction(): Integer[1]\n" +
            "{\n" +
            "  1;\n" +
            "}\n" +
            "native function filter<T>(rel:Relation<T>[1], f:Function<{T[1]->Boolean[1]}>[1]):Relation<T>[1];\n";
}
