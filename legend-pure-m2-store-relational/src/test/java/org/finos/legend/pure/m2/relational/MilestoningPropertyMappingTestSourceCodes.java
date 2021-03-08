// Copyright 2021 Goldman Sachs
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

package org.finos.legend.pure.m2.relational;

public class MilestoningPropertyMappingTestSourceCodes
{
    public static final String MODEL_ID = "model.pure";
    public static final String EMBEDDED_MODEL_ID = "embeddedModel.pure";
    public static final String STORE_ID = "store.pure";
    public static final String MAPPING_ID = "mapping.pure";

    public static final String EXTENDED_MODEL_ID = "extendedModel.pure";
    public static final String EXTENDED_MAPPING_ID = "extendedMapping.pure";

    public static final String BUSINESS_MILESTONING_MODEL_CODE = "###Pure\n" +
            "\n" +
            "Class <<temporal.businesstemporal>> milestoning::A\n" +
            "{\n" +
            "   aId : Integer[1];\n" +
            "}\n";

    public static final String PROCESSING_MILESTONING_MODEL_CODE = "###Pure\n" +
            "\n" +
            "Class <<temporal.processingtemporal>> milestoning::A\n" +
            "{\n" +
            "   aId : Integer[1];\n" +
            "}\n";

    public static final String BI_TEMPORAL_MILESTONING_MODEL_CODE = "###Pure\n" +
            "\n" +
            "Class <<temporal.bitemporal>> milestoning::A\n" +
            "{\n" +
            "   aId : Integer[1];\n" +
            "}\n";

    public static final String NON_MILESTONING_MODEL_CODE = "###Pure\n" +
            "\n" +
            "Class milestoning::A\n" +
            "{\n" +
            "   aId : Integer[1];\n" +
            "}\n";

    public static final String BUSINESS_MILESTONING_EXTENDED_MODEL_CODE = "###Pure\n" +
            "import milestoning::*;\n" +
            "\n" +
            "Class <<temporal.businesstemporal>> milestoning::B extends A\n" +
            "{\n" +
            "}\n" +
            "\n";

    public static final String EMBEDDED_MODEL_CODE = "###Pure\n" +
            "\n" +
            "Class milestoning::B\n" +
            "{\n" +
            "   bId : Integer[1];\n" +
            "   a : milestoning::A[1];\n" +
            "}\n";

    public static final String TEMPORAL_EMBEDDED_MODEL_CODE = "###Pure\n" +
            "\n" +
            "Class <<temporal.businesstemporal>> milestoning::B\n" +
            "{\n" +
            "   bId : Integer[1];\n" +
            "   a : milestoning::A[1];\n" +
            "}\n";

    public static final String MILESTONED_STORE_CODE = "###Relational\n" +
            "\n" +
            "Database milestoning::myDB\n" +
            "(\n" +
            "   Table myTable(\n" +
            "      milestoning(\n" +
            "         processing(PROCESSING_IN=in_z, PROCESSING_OUT=out_z),\n" +
            "         business(BUS_FROM=from_z, BUS_THRU=thru_z)\n" +
            "      )\n" +
            "      aId INT, bId INT, in_z DATE, out_z DATE, from_z DATE, thru_z DATE\n" +
            "   )\n" +
            ")\n";

    public static final String NON_MILESTONED_STORE_CODE = "###Relational\n" +
            "\n" +
            "Database milestoning::myDB\n" +
            "(\n" +
            "   Table myTable(\n" +
            "      aId INT, bId INT, in_z DATE, out_z DATE, from_z DATE, thru_z DATE\n" +
            "   )\n" +
            ")\n";

    public static final String UPDATED_MILESTONED_STORE_CODE = "###Relational\n" +
            "\n" +
            "Database milestoning::myDB\n" +
            "(\n" +
            "   Table myTable(\n" +
            "      milestoning(\n" +
            "         processing(PROCESSING_IN=in_z, PROCESSING_OUT=out_z),\n" +
            "         business(BUS_FROM=from_x, BUS_THRU=thru_x)\n" +
            "      )\n" +
            "      aId INT, bId INT, in_z DATE, out_z DATE, from_x DATE, thru_x DATE\n" +
            "   )\n" +
            ")\n";

    public static final String MAPPING_CODE = "###Mapping\n" +
            "import milestoning::*;\n" +
            "\n" +
            "Mapping milestoning::Amap\n" +
            "(\n" +
            "   A[a] : Relational\n" +
            "   {\n" +
            "      aId : [myDB]myTable.aId\n" +
            "   }\n" +
            ")\n";

    public static final String UPDATED_MAPPING_CODE = "###Mapping\n" +
            "import milestoning::*;\n" +
            "\n" +
            "Mapping milestoning::AMap\n" +
            "(\n" +
            "   A[a1] : Relational\n" +
            "   {\n" +
            "      aId : [myDB]myTable.aId\n" +
            "   }\n" +
            ")\n";

    public static final String EXTENDED_MAPPING_CODE = "###Mapping\n" +
            "import milestoning::*;\n" +
            "\n" +
            "Mapping milestoning::Bmap\n" +
            "(  \n" +
            "   include milestoning::Amap\n" +
            "   \n" +
            "   B[b] extends [a] : Relational\n" +
            "   {\n" +
            "      \n" +
            "   }\n" +
            ")\n";

    public static final String EMBEDDED_MAPPING_CODE = "###Mapping\n" +
            "import milestoning::*;\n" +
            "\n" +
            "Mapping milestoning::Bmap\n" +
            "(  \n" +
            "   B[b] : Relational\n" +
            "   {\n" +
            "      bId : [myDB]myTable.bId,\n" +
            "      a[b_a]( \n" +
            "         aId : [myDB]myTable.aId\n" +
            "      )\n" +
            "   }\n" +
            ")\n";

    public static final String UPDATED_EMBEDDED_MAPPING_CODE = "###Mapping\n" +
            "import milestoning::*;\n" +
            "\n" +
            "Mapping milestoning::Bmap\n" +
            "(  \n" +
            "   B[b] : Relational\n" +
            "   {\n" +
            "      bId : [myDB]myTable.bId,\n" +
            "      a[b_a1]( \n" +
            "         aId : [myDB]myTable.aId\n" +
            "      )\n" +
            "   }\n" +
            ")\n";

    public static final String INLINE_EMBEDDED_MAPPING_CODE = "###Mapping\n" +
            "import milestoning::*;\n" +
            "\n" +
            "Mapping milestoning::Bmap\n" +
            "(\n" +
            "   B[b] : Relational\n" +
            "   {\n" +
            "      bId : [myDB]myTable.bId,\n" +
            "      a() Inline[a]\n" +
            "   }\n" +
            "   \n" +
            "   A[a] : Relational\n" +
            "   {\n" +
            "      aId : [myDB]myTable.aId\n" +
            "   }\n" +
            ")\n";

    public static final String UPDATED_INLINE_EMBEDDED_MAPPING_CODE = "###Mapping\n" +
            "import milestoning::*;\n" +
            "\n" +
            "Mapping milestoning::Bmap\n" +
            "(\n" +
            "   B[b] : Relational\n" +
            "   {\n" +
            "      bId : [myDB]myTable.bId,\n" +
            "      a() Inline[a1]\n" +
            "   }\n" +
            "   \n" +
            "   A[a1] : Relational\n" +
            "   {\n" +
            "      aId : [myDB]myTable.aId\n" +
            "   }\n" +
            ")\n";

    public static final String MAPPING_CODE_WITH_MILESTONING_PROPERTY_EXPLICITLY_MAPPED = "###Mapping\n" +
            "import milestoning::*;\n" +
            "\n" +
            "Mapping milestoning::Amap\n" +
            "(  \n" +
            "   A[a] : Relational\n" +
            "   {\n" +
            "      aId : [myDB]myTable.aId,\n" +
            "      milestoning[a_m]( \n" +
            "         from : [myDB]myTable.from_z,\n" +
            "         thru : [myDB]myTable.thru_z\n" +
            "      )\n" +
            "   }\n" +
            ")\n";

    public static final String EXTENDED_MAPPING_CODE_WITH_MILESTONING_PROPERTY_EXPLICITLY_MAPPED = "###Mapping\n" +
            "import milestoning::*;\n" +
            "\n" +
            "Mapping milestoning::Bmap\n" +
            "(  \n" +
            "   include milestoning::Amap\n" +
            "   \n" +
            "   B[b] extends [a] : Relational\n" +
            "   {\n" +
            "      milestoning[b_m]( \n" +
            "         from : [myDB]myTable.from_z,\n" +
            "         thru : [myDB]myTable.thru_z\n" +
            "      )\n" +
            "   }\n" +
            ")\n";

    public static final String EMBEDDED_MAPPING_CODE_WITH_MILESTONING_PROPERTY_EXPLICITLY_MAPPED = "###Mapping\n" +
            "import milestoning::*;\n" +
            "\n" +
            "Mapping milestoning::Bmap\n" +
            "(  \n" +
            "   B[b] : Relational\n" +
            "   {\n" +
            "      bId : [myDB]myTable.bId,\n" +
            "      a[b_a]( \n" +
            "         aId : [myDB]myTable.aId,\n" +
            "         milestoning[b_a_m](" +
            "            from : [myDB]myTable.from_z,\n" +
            "            thru : [myDB]myTable.thru_z\n" +
            "         )\n" +
            "      )\n" +
            "   }\n" +
            ")\n";

    public static final String INLINE_EMBEDDED_MAPPING_CODE_WITH_MILESTONING_PROPERTY_EXPLICITLY_MAPPED = "###Mapping\n" +
            "import milestoning::*;\n" +
            "\n" +
            "Mapping milestoning::Bmap\n" +
            "(\n" +
            "   B[b] : Relational\n" +
            "   {\n" +
            "      bId : [myDB]myTable.bId,\n" +
            "      a() Inline[a]\n" +
            "   }\n" +
            "   \n" +
            "   A[a] : Relational\n" +
            "   {\n" +
            "      aId : [myDB]myTable.aId,\n" +
            "      milestoning[a_m]( \n" +
            "         from : [myDB]myTable.from_z,\n" +
            "         thru : [myDB]myTable.thru_z\n" +
            "      )\n" +
            "   }\n" +
            ")\n";
}
