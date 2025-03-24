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
import org.finos.legend.pure.m3.serialization.compiler.reference.ReferenceIdProvider;
import org.finos.legend.pure.m3.serialization.compiler.reference.ReferenceIdResolver;
import org.finos.legend.pure.m3.serialization.compiler.reference.v1.TestReferenceIdExtensionV1;
import org.junit.Assert;
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
        ReferenceIdProvider provider = extension.newProvider(processorSupport);
        ReferenceIdResolver resolver = extension.newResolver(processorSupport);

        Assert.assertEquals(path, provider.getReferenceId(testDB));
        Assert.assertSame(path, testDB, resolver.resolveReference(path));

        testDB._schemas().forEach(schema ->
        {
            String schemaId = provider.getReferenceId(schema);
            Assert.assertEquals(path + ".schemas['" + schema._name() + "']", schemaId);
            Assert.assertSame(schemaId, schema, resolver.resolveReference(schemaId));

            schema._tables().forEach(table ->
            {
                String tableId = provider.getReferenceId(table);
                Assert.assertEquals(path + ".schemas['" + schema._name() + "'].tables['" + table._name() + "']", tableId);
                Assert.assertSame(tableId, table, resolver.resolveReference(tableId));

                MutableSet<Column> primaryKeyColumns = Sets.mutable.withAll(table._primaryKey());
                primaryKeyColumns.forEach(column ->
                {
                    String columnId = provider.getReferenceId(column);
                    Assert.assertEquals(path + ".schemas['" + schema._name() + "'].tables['" + table._name() + "'].primaryKey['" + column._name() + "']", columnId);
                    Assert.assertSame(columnId, column, resolver.resolveReference(columnId));
                });

                int[] counter = {0};
                table._columns().forEach(column ->
                {
                    int index = counter[0]++;
                    if (!primaryKeyColumns.contains(column))
                    {
                        String columnId = provider.getReferenceId(column);
                        Assert.assertEquals(path + ".schemas['" + schema._name() + "'].tables['" + table._name() + "'].columns[" + index + "]", columnId);
                        Assert.assertSame(columnId, column, resolver.resolveReference(columnId));
                    }
                });
            });

            schema._views().forEach(view ->
            {
                String viewId = provider.getReferenceId(view);
                Assert.assertEquals(path + ".schemas['" + schema._name() + "'].views['" + view._name() + "']", viewId);
                Assert.assertSame(viewId, view, resolver.resolveReference(viewId));

                FilterMapping filter = view._filter();
                if (filter != null)
                {
                    String filterId = provider.getReferenceId(filter);
                    Assert.assertEquals(path + ".schemas['" + schema._name() + "'].views['" + view._name() + "'].filter", filterId);
                    Assert.assertSame(filterId, filter, resolver.resolveReference(filterId));
                }

                GroupByMapping groupBy = view._groupBy();
                if (groupBy != null)
                {
                    String groupById = provider.getReferenceId(groupBy);
                    Assert.assertEquals(path + ".schemas['" + schema._name() + "'].views['" + view._name() + "'].groupBy", groupById);
                    Assert.assertSame(groupById, groupBy, resolver.resolveReference(groupById));
                }

                MutableSet<Column> primaryKeyColumns = Sets.mutable.withAll(view._primaryKey());
                primaryKeyColumns.forEach(column ->
                {
                    String columnId = provider.getReferenceId(column);
                    Assert.assertEquals(path + ".schemas['" + schema._name() + "'].views['" + view._name() + "'].primaryKey['" + column._name() + "']", columnId);
                    Assert.assertSame(columnId, column, resolver.resolveReference(columnId));
                });

                int[] counter = {0};
                view._columns().forEach(column ->
                {
                    int index = counter[0]++;
                    if (!primaryKeyColumns.contains(column))
                    {
                        String columnId = provider.getReferenceId(column);
                        Assert.assertEquals(path + ".schemas['" + schema._name() + "'].views['" + view._name() + "'].columns[" + index + "]", columnId);
                        Assert.assertSame(columnId, column, resolver.resolveReference(columnId));
                    }
                });
            });
        });

        testDB._joins().forEach(join ->
        {
            String joinId = provider.getReferenceId(join);
            Assert.assertEquals(path + ".joins['" + join._name() + "']", joinId);
            Assert.assertSame(joinId, join, resolver.resolveReference(joinId));
        });

        testDB._filters().forEach(filter ->
        {
            String filterId = provider.getReferenceId(filter);
            Assert.assertEquals(path + ".filters['" + filter._name() + "']", filterId);
            Assert.assertSame(filterId, filter, resolver.resolveReference(filterId));
        });
    }

    @Test
    public void testRelationalMapping()
    {
        String path = "test::relational::TestMapping";
        Mapping mapping = getCoreInstance(path);

        ReferenceIdProvider provider = extension.newProvider(processorSupport);
        ReferenceIdResolver resolver = extension.newResolver(processorSupport);

        Assert.assertEquals(path, provider.getReferenceId(mapping));
        Assert.assertSame(path, mapping, resolver.resolveReference(path));

        mapping._classMappings().forEach(classMapping ->
        {
            String classMappingId = provider.getReferenceId(classMapping);
            Assert.assertEquals(path + ".classMappings[id='" + classMapping._id() + "']", classMappingId);
            Assert.assertSame(classMappingId, classMapping, resolver.resolveReference(classMappingId));

            int[] counter = {0};
            ((RelationalInstanceSetImplementation) classMapping)._propertyMappings().forEach(propertyMapping ->
            {
                String propertyMappingId = provider.getReferenceId(propertyMapping);
                Assert.assertEquals(path + ".classMappings[id='" + classMapping._id() + "'].propertyMappings[" + counter[0]++ + "]", propertyMappingId);
                Assert.assertSame(propertyMappingId, propertyMapping, resolver.resolveReference(propertyMappingId));
            });
        });

        mapping._associationMappings().forEach(assocMapping ->
        {
            String assocMappingId = provider.getReferenceId(assocMapping);
            Assert.assertEquals(path + ".associationMappings[id='" + assocMapping._id() + "']", assocMappingId);
            Assert.assertSame(assocMappingId, assocMapping, resolver.resolveReference(assocMappingId));

            int[] counter = {0};
            assocMapping._propertyMappings().forEach(propertyMapping ->
            {
                String propertyMappingId = provider.getReferenceId(propertyMapping);
                Assert.assertEquals(path + ".associationMappings[id='" + assocMapping._id() + "'].propertyMappings[" + counter[0]++ + "]", propertyMappingId);
                Assert.assertSame(propertyMappingId, propertyMapping, resolver.resolveReference(propertyMappingId));
            });
        });

        mapping._enumerationMappings().forEach(enumMapping ->
        {
            String enumMappingId = provider.getReferenceId(enumMapping);
            Assert.assertEquals(path + ".enumerationMappings['" + enumMapping._name() + "']", enumMappingId);
            Assert.assertSame(enumMappingId, enumMapping, resolver.resolveReference(enumMappingId));
            int[] counter = {0};
            enumMapping._enumValueMappings().forEach(enumValueMapping ->
            {
                String enumValueMappingId = provider.getReferenceId(enumValueMapping);
                Assert.assertEquals(path + ".enumerationMappings['" + enumMapping._name() + "'].enumValueMappings[" + counter[0]++ + "]", enumValueMappingId);
                Assert.assertSame(enumValueMappingId, enumValueMapping, resolver.resolveReference(enumValueMappingId));
            });
        });
    }
}
