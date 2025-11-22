// Copyright 2025 Goldman Sachs
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

package org.finos.legend.pure.m2.relational.serialization.compiler.reference.v1;

import org.eclipse.collections.api.factory.Sets;
import org.eclipse.collections.api.set.MutableSet;
import org.finos.legend.pure.m3.coreinstance.meta.pure.mapping.Mapping;
import org.finos.legend.pure.m3.coreinstance.meta.relational.mapping.FilterMapping;
import org.finos.legend.pure.m3.coreinstance.meta.relational.mapping.GroupByMapping;
import org.finos.legend.pure.m3.coreinstance.meta.relational.mapping.RelationalInstanceSetImplementation;
import org.finos.legend.pure.m3.coreinstance.meta.relational.metamodel.Column;
import org.finos.legend.pure.m3.coreinstance.meta.relational.metamodel.Database;
import org.finos.legend.pure.m3.serialization.compiler.reference.v1.TestReferenceIdExtensionV1;
import org.junit.BeforeClass;
import org.junit.Test;

public class TestReferenceIdExtensionV1Relational extends TestReferenceIdExtensionV1
{
    @BeforeClass
    public static void extraSources()
    {
        compileTestSource(
                "/ref_test/relational/test_relational.pure",
                "###Relational\n" +
                        "\n" +
                        "Database test::relational::TestDB\n" +
                        "(\n" +
                        "   Table SIMPLE\n" +
                        "   (\n" +
                        "      id INT PRIMARY KEY,\n" +
                        "      name VARCHAR(256)\n" +
                        "   )\n" +
                        "\n" +
                        "   View POS_SIMPLE\n" +
                        "   (\n" +
                        "      ~filter simple_pos_id\n" +
                        "      name: SIMPLE.name,\n" +
                        "      id: SIMPLE.id\n" +
                        "   )\n" +
                        "\n" +
                        "   View SIMPLE_BY_NAME\n" +
                        "   (\n" +
                        "      ~groupBy (SIMPLE.name)\n" +
                        "      name: SIMPLE.name\n" +
                        "   )\n" +
                        "\n" +
                        "   Schema left_right\n" +
                        "   (\n" +
                        "      Table LEFT\n" +
                        "      (\n" +
                        "         name VARCHAR(256) PRIMARY KEY\n" +
                        "      )\n" +
                        "\n" +
                        "      Table RIGHT\n" +
                        "      (\n" +
                        "         id INT PRIMARY KEY\n" +
                        "      )\n" +
                        "\n" +
                        "      Table LR\n" +
                        "      (\n" +
                        "         left_name VARCHAR(256) PRIMARY KEY,\n" +
                        "         right_id INT PRIMARY KEY\n" +
                        "      )\n" +
                        "   )\n" +
                        "\n" +
                        "   Schema milestoned\n" +
                        "   (\n" +
                        "      Table MS1\n" +
                        "      (\n" +
                        "         milestoning(\n" +
                        "            business(BUS_FROM=from_z, BUS_THRU=thru_z)\n" +
                        "         )\n" +
                        "         id INT,\n" +
                        "         in_z DATE,\n" +
                        "         out_z DATE,\n" +
                        "         from_z DATE,\n" +
                        "         thru_z DATE\n" +
                        "      )\n" +
                        "\n" +
                        "      Table MS2\n" +
                        "      (\n" +
                        "         milestoning(\n" +
                        "            processing(PROCESSING_IN=in_z, PROCESSING_OUT=out_z)\n" +
                        "         )\n" +
                        "         id INT,\n" +
                        "         in_z DATE,\n" +
                        "         out_z DATE,\n" +
                        "         from_z DATE,\n" +
                        "         thru_z DATE\n" +
                        "      )\n" +
                        "\n" +
                        "      Table MS3\n" +
                        "      (\n" +
                        "         milestoning(\n" +
                        "            processing(PROCESSING_IN=in_z, PROCESSING_OUT=out_z),\n" +
                        "            business(BUS_FROM=from_z, BUS_THRU=thru_z)\n" +
                        "         )\n" +
                        "         id INT,\n" +
                        "         in_z DATE,\n" +
                        "         out_z DATE,\n" +
                        "         from_z DATE,\n" +
                        "         thru_z DATE\n" +
                        "      )\n" +
                        "   )\n" +
                        "\n" +
                        "   Join left_lr\n" +
                        "   (\n" +
                        "      left_right.LEFT.name = left_right.LR.left_name\n" +
                        "   )\n" +
                        "\n" +
                        "   Join right_lr\n" +
                        "   (\n" +
                        "      left_right.RIGHT.id = left_right.LR.right_id\n" +
                        "   )\n" +
                        "\n" +
                        "   Filter simple_pos_id\n" +
                        "   (\n" +
                        "      SIMPLE.id > 0\n" +
                        "   )\n" +
                        ")\n"
        );
        compileTestSource(
                "/ref_test/relational/test_relational_mapping.pure",
                "###Mapping\n" +
                        "\n" +
                        "import test::model::*;\n" +
                        "import test::relational::*;\n" +
                        "\n" +
                        "Mapping test::relational::TestMapping\n" +
                        "(\n" +
                        "   SimpleClass: Relational\n" +
                        "   {\n" +
                        "      name: [TestDB]SIMPLE.name,\n" +
                        "      id: [TestDB]SIMPLE.id\n" +
                        "   }\n" +
                        "\n" +
                        "   Left[left]: Relational\n" +
                        "   {\n" +
                        "      name: [TestDB]left_right.LEFT.name\n" +
                        "   }\n" +
                        "\n" +
                        "   Right[right]: Relational\n" +
                        "   {\n" +
                        "      id: [TestDB]left_right.RIGHT.id\n" +
                        "   }\n" +
                        "\n" +
                        "   LeftRight: Relational\n" +
                        "   {\n" +
                        "      AssociationMapping\n" +
                        "      (\n" +
                        "         toLeft[right, left]: [TestDB]@right_lr > [TestDB]@left_lr,\n" +
                        "         toRight[left, right]: [TestDB]@left_lr > [TestDB]@right_lr\n" +
                        "      )\n" +
                        "   }\n" +
                        ")\n"
        );
    }

    @Test
    public void testDatabase()
    {
        String path = "test::relational::TestDB";
        Database testDB = getCoreInstance(path);

        assertRefId(path, testDB);

        testDB._schemas().forEach(schema ->
        {
            String schemaId = assertRefId(path + ".schemas['" + schema._name() + "']", schema);

            schema._tables().forEach(table ->
            {
                String tableId = assertRefId(schemaId + ".tables['" + table._name() + "']", table);

                MutableSet<Column> primaryKeyColumns = Sets.mutable.withAll(table._primaryKey());
                primaryKeyColumns.forEach(pk -> assertRefId(tableId + ".primaryKey['" + pk._name() + "']", pk));

                int[] counter = {0};
                table._columns().forEach(column ->
                {
                    int index = counter[0]++;
                    if (!primaryKeyColumns.contains(column))
                    {
                        assertRefId(tableId + ".columns[" + index + "]", column);
                    }
                });
            });

            schema._views().forEach(view ->
            {
                String viewId = assertRefId(schemaId + ".views['" + view._name() + "']", view);

                FilterMapping filter = view._filter();
                if (filter != null)
                {
                    assertRefId(viewId + ".filter", filter);
                }

                GroupByMapping groupBy = view._groupBy();
                if (groupBy != null)
                {
                    assertRefId(viewId + ".groupBy", groupBy);
                }

                MutableSet<Column> primaryKeyColumns = Sets.mutable.withAll(view._primaryKey());
                primaryKeyColumns.forEach(pk -> assertRefId(viewId + ".primaryKey['" + pk._name() + "']", pk));

                int[] counter = {0};
                view._columns().forEach(column ->
                {
                    int index = counter[0]++;
                    if (!primaryKeyColumns.contains(column))
                    {
                        assertRefId(viewId + ".columns[" + index + "]", column);
                    }
                });
            });
        });

        testDB._joins().forEach(join -> assertRefId(path + ".joins['" + join._name() + "']", join));

        testDB._filters().forEach(filter -> assertRefId(path + ".filters['" + filter._name() + "']", filter));
    }

    @Test
    public void testRelationalMapping()
    {
        String path = "test::relational::TestMapping";
        Mapping mapping = getCoreInstance(path);

        assertRefId(path, mapping);

        mapping._classMappings().forEach(classMapping ->
        {
            String classMappingId = assertRefId(path + ".classMappings[id='" + classMapping._id() + "']", classMapping);

            int[] counter = {0};
            ((RelationalInstanceSetImplementation) classMapping)._propertyMappings().forEach(pm ->
                    assertRefId(classMappingId + ".propertyMappings[" + counter[0]++ + "]", pm));
        });

        mapping._associationMappings().forEach(assocMapping ->
        {
            String assocMappingId = assertRefId(path + ".associationMappings[id='" + assocMapping._id() + "']", assocMapping);

            int[] counter = {0};
            assocMapping._propertyMappings().forEach(pm -> assertRefId(assocMappingId + ".propertyMappings[" + counter[0]++ + "]", pm));
        });

        mapping._enumerationMappings().forEach(enumMapping ->
        {
            String enumMappingId = assertRefId(path + ".enumerationMappings['" + enumMapping._name() + "']", enumMapping);

            int[] counter = {0};
            enumMapping._enumValueMappings().forEach(evm -> assertRefId(enumMappingId + ".enumValueMappings[" + counter[0]++ + "]", evm));
        });
    }
}
