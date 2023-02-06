// Copyright 2020 Goldman Sachs
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

package org.finos.legend.pure.m2.ds.mapping.test;

import org.eclipse.collections.api.block.predicate.Predicate;
import org.eclipse.collections.api.list.ImmutableList;
import org.eclipse.collections.api.list.MutableList;
import org.finos.legend.pure.m3.coreinstance.meta.pure.mapping.EnumValueMapping;
import org.finos.legend.pure.m3.coreinstance.meta.pure.mapping.EnumerationMapping;
import org.finos.legend.pure.m3.coreinstance.meta.pure.metamodel._import.EnumStub;
import org.finos.legend.pure.m3.coreinstance.meta.pure.metamodel.type.Enum;
import org.finos.legend.pure.m3.navigation.PackageableElement.PackageableElement;
import org.finos.legend.pure.m4.coreinstance.CoreInstance;
import org.finos.legend.pure.m4.coreinstance.primitive.IntegerCoreInstance;
import org.finos.legend.pure.m4.exception.PureCompilationException;
import org.junit.After;
import org.junit.Assert;
import org.junit.BeforeClass;
import org.junit.Test;

public class TestEnumerationMapping extends AbstractPureMappingTestWithCoreCompiled
{
    @BeforeClass
    public static void setUp() {
        setUpRuntime();
    }

    @After
    public void cleanRuntime()
    {
        runtime.delete("mapping.pure");
        runtime.delete("model.pure");
    }

    private static Predicate detectByEnumerationMappingName(final String name)
    {
        return new Predicate<EnumerationMapping>()
        {
            @Override
            public boolean accept(EnumerationMapping enumMapping)
            {
                return enumMapping._name().equals(name);
            }
        };
    }

    @Test
    public void testInvalidEnumeration()
    {
        this.runtime.createInMemorySource("mapping.pure",
                "###Mapping\n" +
                        "Mapping test::TestMapping\n" +
                        "(\n" +
                        "  test::TestEnumeration: EnumerationMapping TestMapping\n" +
                        "  {\n" +
                        "    VAL1 : '1',\n" +
                        "    VAL2 : '2'\n" +
                        "  }\n" +
                        ")");
        try
        {
            this.runtime.compile();
            Assert.fail("Expected compilation error");
        }
        catch (Exception e)
        {
            assertPureException(PureCompilationException.class, "test::TestEnumeration has not been defined!", "mapping.pure", 4, 9, 4, 9, 4, 23, e);
        }
    }

    @Test
    public void testValidEnumeration()
    {
        this.runtime.createInMemorySource("model.pure",
                "Enum test::TestEnumeration\n" +
                        "{\n" +
                        "  VAL1, VAL2" +
                        "}");
        String source = "###Mapping\n" +
                "Mapping test::TestMapping\n" +
                "(\n" +
                "  test::TestEnumeration: EnumerationMapping TestMapping\n" +
                "  {\n" +
                "    VAL1 : '1',\n" +
                "    VAL2 : '2'\n" +
                "  }\n" +
                ")";
        this.runtime.createInMemorySource("mapping.pure", source);
        this.runtime.compile();
        assertSetSourceInformation(source, "test::TestEnumeration");
    }

    @Test
    public void testInvalidEnum()
    {
        this.runtime.createInMemorySource("model.pure",
                "Enum test::TestEnumeration\n" +
                        "{\n" +
                        "  VAL1, VAL2" +
                        "}");
        this.runtime.createInMemorySource("mapping.pure",
                "###Mapping\n" +
                        "Mapping test::TestMapping\n" +
                        "(\n" +
                        "  test::TestEnumeration: EnumerationMapping TestMapping\n" +
                        "  {\n" +
                        "    VAL1 : '1',\n" +
                        "    VAL2 : '2',\n" +
                        "    NOT_A_VAL : '3'\n" +
                        "  }\n" +
                        ")");
        try
        {
            this.runtime.compile();
            Assert.fail("Expected compilation error");
        }
        catch (Exception e)
        {
            assertPureException(PureCompilationException.class, "The enum value 'NOT_A_VAL' can't be found in the enumeration test::TestEnumeration", "mapping.pure", 8, 5, 8, 5, 8, 13, e);
        }
    }

    @Test
    public void testValidSimpleEnumToEnumMapping()
    {
        this.runtime.createInMemorySource("model.pure",
                "###Pure\n" +
                        "\n" +
                        "Enum my::SourceEnum\n" +
                        "{\n" +
                        "   A, B\n" +
                        "}\n" +
                        "\n" +
                        "Enum my::TargetEnum\n" +
                        "{\n" +
                        "   X, Y\n" +
                        "}\n" +
                        "\n" +
                        "###Mapping\n" +
                        "import my::*;\n" +
                        "\n" +
                        "Mapping my::TestMapping\n" +
                        "(\n" +
                        "   TargetEnum : EnumerationMapping\n" +
                        "   {\n" +
                        "      X : SourceEnum.A,\n" +
                        "      Y : my::SourceEnum.B\n" +
                        "   }\n" +
                        ")");
        this.runtime.compile();

        org.finos.legend.pure.m3.coreinstance.meta.pure.mapping.Mapping mapping = (org.finos.legend.pure.m3.coreinstance.meta.pure.mapping.Mapping)this.runtime.getCoreInstance("my::TestMapping");
        EnumerationMapping enumerationMapping = mapping._enumerationMappings().getFirst();
        Assert.assertEquals("my::TargetEnum", PackageableElement.getUserPathForPackageableElement(enumerationMapping._enumeration()));

        ImmutableList<EnumValueMapping> enumValueMappings = (ImmutableList<EnumValueMapping>)enumerationMapping._enumValueMappings();
        Assert.assertEquals(2, enumValueMappings.size());

        EnumValueMapping enumValueMapping1 = enumValueMappings.get(0);
        Assert.assertEquals("X", enumValueMapping1._enum()._name());
        ImmutableList<CoreInstance> sourceValuesCoreInstances1 = (ImmutableList<CoreInstance>)enumValueMapping1._sourceValuesCoreInstance();
        Assert.assertEquals(1, sourceValuesCoreInstances1.size());
        Assert.assertTrue(sourceValuesCoreInstances1.get(0) instanceof EnumStub);
        Assert.assertEquals("my::SourceEnum", PackageableElement.getUserPathForPackageableElement(((EnumStub)sourceValuesCoreInstances1.get(0))._enumeration()));
        ImmutableList<CoreInstance> sourceValues1 = (ImmutableList<CoreInstance>)enumValueMapping1._sourceValues();
        Assert.assertEquals(1, sourceValues1.size());
        Assert.assertTrue(sourceValues1.get(0) instanceof Enum);
        Assert.assertEquals("A", ((Enum)sourceValues1.get(0))._name());

        EnumValueMapping enumValueMapping2 = enumValueMappings.get(1);
        Assert.assertEquals("Y", enumValueMapping2._enum()._name());
        ImmutableList<CoreInstance> sourceValuesCoreInstances2 = (ImmutableList<CoreInstance>)enumValueMapping2._sourceValuesCoreInstance();
        Assert.assertEquals(1, sourceValuesCoreInstances2.size());
        Assert.assertTrue(sourceValuesCoreInstances2.get(0) instanceof EnumStub);
        Assert.assertEquals("my::SourceEnum", PackageableElement.getUserPathForPackageableElement(((EnumStub)sourceValuesCoreInstances2.get(0))._enumeration()));
        ImmutableList<CoreInstance> sourceValues2 = (ImmutableList<CoreInstance>)enumValueMapping2._sourceValues();
        Assert.assertEquals(1, sourceValues2.size());
        Assert.assertTrue(sourceValues2.get(0) instanceof Enum);
        Assert.assertEquals("B", ((Enum)sourceValues2.get(0))._name());
    }

    @Test
    public void testInvalidSourceTypes()
    {
        this.runtime.createInMemorySource("model.pure",
                "###Pure\n" +
                        "\n" +
                        "Enum my::SourceEnumA\n" +
                        "{\n" +
                        "   A, B\n" +
                        "}\n" +
                        "\n" +
                        "Enum my::SourceEnumB\n" +
                        "{\n" +
                        "   C, D\n" +
                        "}\n" +
                        "\n" +
                        "Enum my::TargetEnum\n" +
                        "{\n" +
                        "   X, Y, Z\n" +
                        "}\n" +
                        "\n" +
                        "###Mapping\n" +
                        "import my::*;\n" +
                        "\n" +
                        "Mapping my::TestMapping\n" +
                        "(\n" +
                        "   TargetEnum : EnumerationMapping\n" +
                        "   {\n" +
                        "      X : [SourceEnumA.A, 'A', 1],\n" +
                        "      Y : SourceEnumB.C,\n" +
                        "      Z : 'Z'\n" +
                        "   }\n" +
                        ")");
        try
        {
            this.runtime.compile();
            Assert.fail("Expected compilation error");
        }
        catch (Exception e)
        {
            assertPureException(PureCompilationException.class, "Enumeration Mapping 'TargetEnum' has Source Types: 'my::SourceEnumA', 'String', 'Integer', 'my::SourceEnumB'. Only one source Type is allowed for an Enumeration Mapping", "model.pure", 23, 4, 23, 4, 23, 13, e);
        }
    }

    @Test
    public void testValidComplexEnumToEnumMapping()
    {
        this.runtime.createInMemorySource("model.pure",
                "###Pure\n" +
                        "\n" +
                        "Enum my::SourceEnum\n" +
                        "{\n" +
                        "   A, B, C\n" +
                        "}\n" +
                        "\n" +
                        "Enum my::TargetEnum\n" +
                        "{\n" +
                        "   X, Y\n" +
                        "}\n" +
                        "\n" +
                        "###Mapping\n" +
                        "import my::*;\n" +
                        "\n" +
                        "Mapping my::TestMapping\n" +
                        "(\n" +
                        "   TargetEnum : EnumerationMapping\n" +
                        "   {\n" +
                        "      X : SourceEnum.A,\n" +
                        "      Y : [SourceEnum.B, my::SourceEnum.C]\n" +
                        "   }\n" +
                        ")");
        this.runtime.compile();

        org.finos.legend.pure.m3.coreinstance.meta.pure.mapping.Mapping mapping = (org.finos.legend.pure.m3.coreinstance.meta.pure.mapping.Mapping)this.runtime.getCoreInstance("my::TestMapping");
        EnumerationMapping enumerationMapping = mapping._enumerationMappings().getFirst();
        Assert.assertEquals("my::TargetEnum", PackageableElement.getUserPathForPackageableElement(enumerationMapping._enumeration()));

        ImmutableList<EnumValueMapping> enumValueMappings = (ImmutableList<EnumValueMapping>)enumerationMapping._enumValueMappings();
        Assert.assertEquals(2, enumValueMappings.size());

        EnumValueMapping enumValueMapping1 = enumValueMappings.get(0);
        Assert.assertEquals("X", enumValueMapping1._enum()._name());
        ImmutableList<CoreInstance> sourceValuesCoreInstances1 = (ImmutableList<CoreInstance>)enumValueMapping1._sourceValuesCoreInstance();
        Assert.assertEquals(1, sourceValuesCoreInstances1.size());
        Assert.assertTrue(sourceValuesCoreInstances1.get(0) instanceof EnumStub);
        Assert.assertEquals("my::SourceEnum", PackageableElement.getUserPathForPackageableElement(((EnumStub)sourceValuesCoreInstances1.get(0))._enumeration()));
        ImmutableList<CoreInstance> sourceValues1 = (ImmutableList<CoreInstance>)enumValueMapping1._sourceValues();
        Assert.assertEquals(1, sourceValues1.size());
        Assert.assertTrue(sourceValues1.get(0) instanceof Enum);
        Assert.assertEquals("A", ((Enum)sourceValues1.get(0))._name());

        EnumValueMapping enumValueMapping2 = enumValueMappings.get(1);
        Assert.assertEquals("Y", enumValueMapping2._enum()._name());
        ImmutableList<CoreInstance> sourceValuesCoreInstances2 = (ImmutableList<CoreInstance>)enumValueMapping2._sourceValuesCoreInstance();
        Assert.assertEquals(2, sourceValuesCoreInstances2.size());
        Assert.assertTrue(sourceValuesCoreInstances2.get(0) instanceof EnumStub);
        Assert.assertEquals("my::SourceEnum", PackageableElement.getUserPathForPackageableElement(((EnumStub)sourceValuesCoreInstances2.get(0))._enumeration()));
        Assert.assertTrue(sourceValuesCoreInstances2.get(1) instanceof EnumStub);
        Assert.assertEquals("my::SourceEnum", PackageableElement.getUserPathForPackageableElement(((EnumStub)sourceValuesCoreInstances2.get(1))._enumeration()));
        ImmutableList<CoreInstance> sourceValues2 = (ImmutableList<CoreInstance>)enumValueMapping2._sourceValues();
        Assert.assertEquals(2, sourceValues2.size());
        Assert.assertTrue(sourceValues2.get(0) instanceof Enum);
        Assert.assertEquals("B", ((Enum)sourceValues2.get(0))._name());
        Assert.assertTrue(sourceValues2.get(1) instanceof Enum);
        Assert.assertEquals("C", ((Enum)sourceValues2.get(1))._name());
    }

    @Test
    public void testValidHybridEnumToEnumMapping()
    {
        this.runtime.createInMemorySource("model.pure",
                "###Pure\n" +
                        "\n" +
                        "Enum my::SourceEnum1\n" +
                        "{\n" +
                        "   A, B, C\n" +
                        "}\n" +
                        "\n" +
                        "Enum my::SourceEnum2\n" +
                        "{\n" +
                        "   P, Q, R\n" +
                        "}\n" +
                        "\n" +
                        "Enum my::TargetEnum\n" +
                        "{\n" +
                        "   U, V, W, X, Y, Z\n" +
                        "}\n" +
                        "\n" +
                        "###Mapping\n" +
                        "import my::*;\n" +
                        "\n" +
                        "Mapping my::TestMapping\n" +
                        "(\n" +
                        "   TargetEnum : EnumerationMapping enumsATargetEnum\n" +
                        "   {\n" +
                        "      U : my::SourceEnum1.A,\n" +
                        "      V : my::SourceEnum1.A,\n" +
                        "      W : [my::SourceEnum1.A, my::SourceEnum1.B],\n" +
                        "      X : [my::SourceEnum1.A, my::SourceEnum1.B, my::SourceEnum1.C],\n" +
                        "      Y : [my::SourceEnum1.A, my::SourceEnum1.B, my::SourceEnum1.C],\n" +
                        "      Z : my::SourceEnum1.C\n" +
                        "   }\n" +
                        "   TargetEnum : EnumerationMapping enumsBTargetEnum\n" +
                        "   {\n" +
                        "      U : my::SourceEnum2.P,\n" +
                        "      V : my::SourceEnum2.P\n," +
                        "      W : [my::SourceEnum2.P, my::SourceEnum2. Q,my::SourceEnum2.R]\n" +
                        "   }\n" +
                        "   TargetEnum : EnumerationMapping integersTargetEnum2\n" +
                        "   {\n" +
                        "      X : [4,5,6],\n" +
                        "      Y : 3\n" +
                        "   }\n" +
                        "   TargetEnum : EnumerationMapping stringsTargetEnum2\n" +
                        "   {\n" +
                        "      Y : ['One','Two','Three'],\n" +
                        "      Z : 'A'\n" +
                        "   }\n" +
                        ")");
        this.runtime.compile();

        org.finos.legend.pure.m3.coreinstance.meta.pure.mapping.Mapping mapping = (org.finos.legend.pure.m3.coreinstance.meta.pure.mapping.Mapping)this.runtime.getCoreInstance("my::TestMapping");
        EnumerationMapping enumerationMapping = mapping._enumerationMappings().detect(detectByEnumerationMappingName("enumsATargetEnum"));
        Assert.assertEquals("my::TargetEnum", PackageableElement.getUserPathForPackageableElement(enumerationMapping._enumeration()));

        ImmutableList<EnumValueMapping> enumValueMappings = (ImmutableList<EnumValueMapping>)enumerationMapping._enumValueMappings();
        Assert.assertEquals(6, enumValueMappings.size());

        EnumValueMapping enumValueMapping1 = enumValueMappings.get(0);
        Assert.assertEquals("U", enumValueMapping1._enum()._name());
        ImmutableList<CoreInstance> sourceValuesCoreInstances1 = (ImmutableList<CoreInstance>)enumValueMapping1._sourceValuesCoreInstance();
        Assert.assertEquals(1, sourceValuesCoreInstances1.size());
        Assert.assertTrue(sourceValuesCoreInstances1.get(0) instanceof EnumStub);
        Assert.assertEquals("my::SourceEnum1", PackageableElement.getUserPathForPackageableElement(((EnumStub)sourceValuesCoreInstances1.get(0))._enumeration()));
        ImmutableList<CoreInstance> sourceValues1 = (ImmutableList<CoreInstance>)enumValueMapping1._sourceValues();
        Assert.assertEquals(1, sourceValues1.size());
        Assert.assertTrue(sourceValues1.get(0) instanceof Enum);
        Assert.assertEquals("A", ((Enum)sourceValues1.get(0))._name());

        enumValueMappings = (ImmutableList<EnumValueMapping>)mapping._enumerationMappings().detect(detectByEnumerationMappingName("enumsBTargetEnum"))._enumValueMappings();
        EnumValueMapping enumValueMapping2 = enumValueMappings.get(1);
        Assert.assertEquals("V", enumValueMapping2._enum()._name());
        ImmutableList<CoreInstance> sourceValuesCoreInstances2 = (ImmutableList<CoreInstance>)enumValueMapping2._sourceValuesCoreInstance();
        Assert.assertEquals(1, sourceValuesCoreInstances2.size());
        Assert.assertTrue(sourceValuesCoreInstances2.get(0) instanceof EnumStub);
        Assert.assertEquals("my::SourceEnum2", PackageableElement.getUserPathForPackageableElement(((EnumStub)sourceValuesCoreInstances2.get(0))._enumeration()));
        ImmutableList<CoreInstance> sourceValues2 = (ImmutableList<CoreInstance>)enumValueMapping2._sourceValues();
        Assert.assertEquals(1, sourceValues2.size());
        Assert.assertTrue(sourceValues2.get(0) instanceof Enum);
        Assert.assertEquals("P", ((Enum)sourceValues2.get(0))._name());

        EnumValueMapping enumValueMapping3 = enumValueMappings.get(2);
        Assert.assertEquals("W", enumValueMapping3._enum()._name());
        ImmutableList<CoreInstance> sourceValuesCoreInstances3 = (ImmutableList<CoreInstance>)enumValueMapping3._sourceValuesCoreInstance();
        Assert.assertEquals(3, sourceValuesCoreInstances3.size());
        Assert.assertTrue(sourceValuesCoreInstances3.get(0) instanceof EnumStub);
        Assert.assertEquals("my::SourceEnum2", PackageableElement.getUserPathForPackageableElement(((EnumStub)sourceValuesCoreInstances3.get(0))._enumeration()));
        Assert.assertTrue(sourceValuesCoreInstances3.get(1) instanceof EnumStub);
        Assert.assertEquals("my::SourceEnum2", PackageableElement.getUserPathForPackageableElement(((EnumStub)sourceValuesCoreInstances3.get(1))._enumeration()));
        ImmutableList<CoreInstance> sourceValues3 = (ImmutableList<CoreInstance>)enumValueMapping3._sourceValues();
        Assert.assertEquals(3, sourceValues3.size());
        Assert.assertTrue(sourceValues3.get(0) instanceof Enum);
        Assert.assertEquals("P", ((Enum)sourceValues3.get(0))._name());
        Assert.assertTrue(sourceValues3.get(1) instanceof Enum);
        Assert.assertEquals("Q", ((Enum)sourceValues3.get(1))._name());
        Assert.assertTrue(sourceValues3.get(2) instanceof Enum);
        Assert.assertEquals("R", ((Enum)sourceValues3.get(2))._name());

        enumValueMappings = (ImmutableList<EnumValueMapping>)mapping._enumerationMappings().detect(detectByEnumerationMappingName("integersTargetEnum2"))._enumValueMappings();
        enumValueMapping1 = enumValueMappings.get(0);
        Assert.assertEquals("X", enumValueMapping1._enum()._name());
        ImmutableList<CoreInstance> sourceValuesCoreInstances4 = (ImmutableList<CoreInstance>)enumValueMapping1._sourceValuesCoreInstance();
        Assert.assertEquals(3, sourceValuesCoreInstances4.size());
        Assert.assertTrue(sourceValuesCoreInstances4.get(0) instanceof IntegerCoreInstance);
        MutableList<? extends Object> sourceValues4 = enumValueMapping1._sourceValues().toList();
        Assert.assertEquals(3, sourceValues4.size());
        Assert.assertEquals(4L, sourceValues4.get(0));
        Assert.assertEquals(5L, sourceValues4.get(1));
        Assert.assertEquals(6L, sourceValues4.get(2));
        enumValueMapping2 = enumValueMappings.get(1);
        Assert.assertEquals("Y", enumValueMapping2._enum()._name());
        ImmutableList<CoreInstance> sourceValuesCoreInstances5 = (ImmutableList<CoreInstance>)enumValueMapping2._sourceValuesCoreInstance();
        Assert.assertEquals(1, sourceValuesCoreInstances5.size());
        Assert.assertTrue(sourceValuesCoreInstances4.get(0) instanceof IntegerCoreInstance);
        MutableList<? extends Object> sourceValues5 = enumValueMapping2._sourceValues().toList();
        Assert.assertEquals(1, sourceValues5.size());
        Assert.assertEquals(3L, sourceValues5.get(0));

        enumValueMappings = (ImmutableList<EnumValueMapping>)mapping._enumerationMappings().detect(detectByEnumerationMappingName("stringsTargetEnum2"))._enumValueMappings();
        enumValueMapping1 = enumValueMappings.get(0);
        Assert.assertEquals("Y", enumValueMapping1._enum()._name());
        ImmutableList<CoreInstance> sourceValuesCoreInstances6 = (ImmutableList<CoreInstance>)enumValueMapping1._sourceValuesCoreInstance();
        Assert.assertEquals(3, sourceValuesCoreInstances6.size());
        Assert.assertTrue(sourceValuesCoreInstances4.get(0) instanceof IntegerCoreInstance);
        MutableList<? extends Object> sourceValues6 = enumValueMapping1._sourceValues().toList();
        Assert.assertEquals(3, sourceValues6.size());
        Assert.assertEquals("One", sourceValues6.get(0));
        Assert.assertEquals("Two", sourceValues6.get(1));
        Assert.assertEquals("Three", sourceValues6.get(2));
        enumValueMapping2 = enumValueMappings.get(1);
        Assert.assertEquals("Z", enumValueMapping2._enum()._name());
        ImmutableList<CoreInstance> sourceValuesCoreInstances7 = (ImmutableList<CoreInstance>)enumValueMapping2._sourceValuesCoreInstance();
        Assert.assertEquals(1, sourceValuesCoreInstances7.size());
        Assert.assertTrue(sourceValuesCoreInstances4.get(0) instanceof IntegerCoreInstance);
        MutableList<? extends Object> sourceValues7 = enumValueMapping2._sourceValues().toList();
        Assert.assertEquals(1, sourceValues7.size());
        Assert.assertEquals("A", sourceValues7.get(0));
    }
}
