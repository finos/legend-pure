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

package org.finos.legend.pure.m2.dsl.mapping.serialization.compiler.reference.v1;

import org.finos.legend.pure.m3.coreinstance.meta.external.store.model.PureInstanceSetImplementation;
import org.finos.legend.pure.m3.coreinstance.meta.pure.mapping.Mapping;
import org.finos.legend.pure.m3.coreinstance.meta.pure.mapping.MappingClass;
import org.finos.legend.pure.m3.coreinstance.meta.pure.metamodel.function.LambdaFunction;
import org.finos.legend.pure.m3.serialization.compiler.reference.ReferenceIdProvider;
import org.finos.legend.pure.m3.serialization.compiler.reference.ReferenceIdResolver;
import org.finos.legend.pure.m3.serialization.compiler.reference.v1.TestReferenceIdExtensionV1;
import org.junit.Assert;
import org.junit.BeforeClass;
import org.junit.Test;

public class TestReferenceIdExtensionV1Mapping extends TestReferenceIdExtensionV1
{
    @BeforeClass
    public static void extraSource()
    {
        compileTestSource(
                "/ref_test/mapping/test_mapping.pure",
                "import test::model::*;\n" +
                        "\n" +
                        "Class test::model::SourceSimpleClass\n" +
                        "{\n" +
                        "  names : String[*];\n" +
                        "  id : Integer[1];\n" +
                        "  titles : String[*];\n" +
                        "}\n" +
                        "\n" +
                        "Class test::model::SourceLeft\n" +
                        "{\n" +
                        "  names : String[*];\n" +
                        "  id : Integer[1];\n" +
                        "  titles : String[*];\n" +
                        "  family : String[0..1];\n" +
                        "}\n" +
                        "\n" +
                        "Class test::model::SourceRight\n" +
                        "{\n" +
                        "  id : Integer[0..1];\n" +
                        "}\n" +
                        "\n" +
                        "###Mapping\n" +
                        "\n" +
                        "import test::model::*;\n" +
                        "\n" +
                        "Mapping test::mapping::TestMapping\n" +
                        "(\n" +
                        "   SimpleClass : Pure\n" +
                        "   {\n" +
                        "      ~src SourceSimpleClass\n" +
                        "      name : $src.names->joinStrings(' '),\n" +
                        "      id : $src.id,\n" +
                        "      +title : String[0..1] : $src.titles->first()\n" +
                        "   }\n" +
                        "\n" +
                        "   Left : Pure\n" +
                        "   {\n" +
                        "      ~src SourceLeft\n" +
                        "      name : $src.names->joinStrings(' '),\n" +
                        "      +titles : String[*] : $src.titles,\n" +
                        "      +family : String[0..1] : $src.family\n" +
                        "   }\n" +
                        "\n" +
                        "   Right : Pure\n" +
                        "   {\n" +
                        "      ~src SourceRight\n" +
                        "      id : if($src.id->isEmpty(), |0, |$src.id->toOne())\n" +
                        "   }\n" +
                        "\n" +
                        "   SimpleEnumeration: EnumerationMapping simpleEnum\n" +
                        "   {\n" +
                        "      VAL1: 1,\n" +
                        "      VAL2: 2\n" +
                        "   }\n" +
                        "\n" +
                        "   AggregationKind: EnumerationMapping aggKind\n" +
                        "   {\n" +
                        "      None: 'none',\n" +
                        "      Shared: 'shared',\n" +
                        "      Composite: ['composite', 'comp']\n" +
                        "   }\n" +
                        ")\n"
        );
    }

    @Test
    public void testMapping()
    {
        String path = "test::mapping::TestMapping";
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

            PureInstanceSetImplementation pureClassMapping = (PureInstanceSetImplementation) classMapping;

            LambdaFunction<?> filter = pureClassMapping._filter();
            if (filter != null)
            {
                String filterId = provider.getReferenceId(filter);
                Assert.assertEquals(path + ".filter", filterId);
                Assert.assertSame(filterId, filter, resolver.resolveReference(filterId));
            }

            int[] counter = {0};
            pureClassMapping._propertyMappings().forEach(propertyMapping ->
            {
                String propertyMappingId = provider.getReferenceId(propertyMapping);
                Assert.assertEquals(path + ".classMappings[id='" + classMapping._id() + "'].propertyMappings[" + counter[0]++ + "]", propertyMappingId);
                Assert.assertSame(propertyMappingId, propertyMapping, resolver.resolveReference(propertyMappingId));
            });

            MappingClass<?> mappingClass = pureClassMapping._mappingClass();
            if (mappingClass != null)
            {
                String mappingClassId = provider.getReferenceId(mappingClass);
                Assert.assertEquals(path + ".classMappings[id='" + classMapping._id() + "'].mappingClass", mappingClassId);
                Assert.assertSame(mappingClassId, mappingClass, resolver.resolveReference(mappingClassId));
                mappingClass._properties().forEach(property ->
                {
                    String propertyId = provider.getReferenceId(property);
                    Assert.assertEquals(path + ".classMappings[id='" + classMapping._id() + "'].mappingClass.properties['" + property._name() + "']", propertyId);
                    Assert.assertSame(propertyId, property, resolver.resolveReference(propertyId));
                });
            }
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
