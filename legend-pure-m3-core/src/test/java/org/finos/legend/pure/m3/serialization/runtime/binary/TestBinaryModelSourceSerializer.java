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

package org.finos.legend.pure.m3.serialization.runtime.binary;

import org.eclipse.collections.api.RichIterable;
import org.eclipse.collections.api.list.MutableList;
import org.eclipse.collections.api.multimap.list.MutableListMultimap;
import org.eclipse.collections.api.set.MutableSet;
import org.eclipse.collections.api.set.SetIterable;
import org.eclipse.collections.api.tuple.Pair;
import org.eclipse.collections.impl.block.factory.Functions;
import org.eclipse.collections.impl.block.factory.Predicates;
import org.eclipse.collections.impl.factory.Lists;
import org.eclipse.collections.impl.factory.Multimaps;
import org.eclipse.collections.impl.factory.Sets;
import org.eclipse.collections.impl.set.mutable.UnifiedSet;
import org.eclipse.collections.impl.tuple.Tuples;
import org.eclipse.collections.impl.utility.Iterate;
import org.eclipse.collections.impl.utility.LazyIterate;
import org.finos.legend.pure.m3.AbstractPureTestWithCoreCompiledPlatform;
import org.finos.legend.pure.m3.navigation.M3Properties;
import org.finos.legend.pure.m3.navigation.PackageableElement.PackageableElement;
import org.finos.legend.pure.m3.navigation.imports.Imports;
import org.finos.legend.pure.m3.serialization.runtime.Message;
import org.finos.legend.pure.m3.serialization.runtime.PureRuntime;
import org.finos.legend.pure.m3.serialization.runtime.PureRuntimeBuilder;
import org.finos.legend.pure.m3.serialization.runtime.Source;
import org.finos.legend.pure.m3.serialization.runtime.binary.reference.ExternalReferenceSerializerLibrary;
import org.finos.legend.pure.m4.coreinstance.CoreInstance;
import org.finos.legend.pure.m4.serialization.binary.BinaryReaders;
import org.finos.legend.pure.m4.serialization.binary.BinaryWriters;
import org.junit.After;
import org.junit.Assert;
import org.junit.BeforeClass;
import org.junit.Test;

import java.io.ByteArrayOutputStream;
import java.util.Collection;

public class TestBinaryModelSourceSerializer extends AbstractPureTestWithCoreCompiledPlatform
{
    @BeforeClass
    public static void setUp() {
        setUpRuntime(getExtra());
    }

    @After
    public void clearRuntime() {
        runtime.delete("/test/model/testSource.pure");
        runtime.delete("/test/model/testSource2.pure");
    }

    @Test
    public void testSimpleClassSerialization()
    {
        this.compileTestSource("/test/model/testSource.pure",
                "import test::*;\n" +
                        "Class test::ClassA\n" +
                        "{\n" +
                        "  name : String[1];\n" +
                        "  id : Integer[1];\n" +
                        "}\n" +
                        "Class test::ClassB extends ClassA\n" +
                        "{\n" +
                        "  value : Float[1..*];\n" +
                        "}\n" +
                        "Class test::ClassC\n" +
                        "{\n" +
                        "  rel : test::ClassB[*];\n" +
                        "}");
        SetIterable<String> expectedInstances = Sets.mutable.with("test::ClassA", "test::ClassB", "test::ClassC", "system::imports::import__test_model_testSource_pure_1");
        SetIterable<String> expectedReferences = Sets.mutable.with("Float", "Integer", "String", "meta::pure::metamodel::function::property::AggregationKind", "meta::pure::metamodel::function::property::Property", "meta::pure::metamodel::multiplicity::OneMany", "meta::pure::metamodel::multiplicity::PureOne", "meta::pure::metamodel::multiplicity::ZeroMany", "meta::pure::metamodel::relationship::Generalization", "meta::pure::metamodel::type::Any", "meta::pure::metamodel::type::Class", "meta::pure::metamodel::type::generics::GenericType", "meta::pure::metamodel::import::Import", "meta::pure::metamodel::import::ImportGroup", "meta::pure::metamodel::import::ImportStub");

        Pair<SourceSerializationResult, byte[]> serializationResult = this.serializeSource("/test/model/testSource.pure");
        SourceSerializationResult result = serializationResult.getOne();
        byte[] serialized = serializationResult.getTwo();

        this.assertSetsEqual(expectedInstances, result.getSerializedInstances());
        this.assertSetsEqual(expectedReferences, result.getExternalReferences());
        this.assertExternalReferencePathsResolvable(result.getExternalReferences());

        SourceDeserializationResult deserializationResult = BinaryModelSourceDeserializer.readIndexes(BinaryReaders.newBinaryReader(serialized));
        this.assertSetsEqual(expectedInstances, deserializationResult.getInstances().toSet());
        this.assertSetsEqual(expectedReferences, deserializationResult.getExternalReferences());
    }

    @Test
    public void testSimpleProfileSerialization()
    {
        this.compileTestSource("/test/model/testSource.pure",
                "Profile test::profiles::MyProfile\n" +
                        "{\n" +
                        "    stereotypes: [st1, st2];\n" +
                        "    tags: [tag1];\n" +
                        "}\n");
        SetIterable<String> expectedInstances = Sets.mutable.with("test::profiles::MyProfile", "system::imports::import__test_model_testSource_pure_1");
        SetIterable<String> expectedReferences = Sets.mutable.with("meta::pure::metamodel::extension::Profile", "meta::pure::metamodel::extension::Stereotype", "meta::pure::metamodel::extension::Tag", "meta::pure::metamodel::import::ImportGroup");

        Pair<SourceSerializationResult, byte[]> serializationResult = this.serializeSource("/test/model/testSource.pure");
        SourceSerializationResult result = serializationResult.getOne();
        byte[] serialized = serializationResult.getTwo();

        this.assertSetsEqual(expectedInstances, result.getSerializedInstances());
        this.assertSetsEqual(expectedReferences, result.getExternalReferences());
        this.assertExternalReferencePathsResolvable(result.getExternalReferences());

        SourceDeserializationResult deserializationResult = BinaryModelSourceDeserializer.readIndexes(BinaryReaders.newBinaryReader(serialized));
        this.assertSetsEqual(expectedInstances, deserializationResult.getInstances().toSet());
        this.assertSetsEqual(expectedReferences, deserializationResult.getExternalReferences());
    }

    @Test
    public void testUnicodeInString()
    {
        String sourceId = "/test/model/testSource.pure";
        String sourceCode = "function test::testFn() : String[1]\n" +
                "{\n" +
                "  'hello\u2022world'\n" +
                "}\n";
        compileTestSource(sourceId, sourceCode);

        SetIterable<String> expectedInstances = Sets.mutable.with("test::testFn__String_1_", "system::imports::import__test_model_testSource_pure_1");

        Pair<SourceSerializationResult, byte[]> serializationResult = this.serializeSource("/test/model/testSource.pure");
        SourceSerializationResult result = serializationResult.getOne();
        byte[] serialized = serializationResult.getTwo();

        assertSetsEqual(expectedInstances, result.getSerializedInstances());
        assertExternalReferencePathsResolvable(result.getExternalReferences());

        SourceDeserializationResult deserializationResult = BinaryModelSourceDeserializer.deserialize(BinaryReaders.newBinaryReader(serialized), ExternalReferenceSerializerLibrary.newLibrary(this.runtime));
        assertSetsEqual(expectedInstances, deserializationResult.getInstances().toSet());
        Assert.assertEquals(sourceCode, deserializationResult.getSource().getContent());
    }

    @Test
    public void testNonConcreteGenericTypeReference()
    {
        this.compileTestSourceM3("/test/model/testSource.pure",
                "import test::*;\n" +
                        "\n" +
                        "Class test::TopClass<X,Y>\n" +
                        "{\n" +
                        "  prop1:X[*];\n" +
                        "  prop2:Y[1];\n" +
                        "}\n" +
                        "\n" +
                        "Class test::LeftClass<T> extends TopClass<T,T>\n" +
                        "{\n" +
                        "}\n" +
                        "\n" +
                        "Class test::RightClass<U,V> extends TopClass<V,U>\n" +
                        "{\n" +
                        "}");

        this.compileTestSourceM3("/test/model/testSource2.pure",
                "import test::*;\n" +
                        "\n" +
                        "function test::testFn<X,Y>(lefts:LeftClass<Any>[*], rights:RightClass<X,Y>[*]):TopClass<Any,Any>[*]\n" +
                        "{\n" +
                        "  concatenate($lefts, $rights)\n" +
                        "}");
        SetIterable<String> expectedInstances = Sets.mutable.with("test::testFn_LeftClass_MANY__RightClass_MANY__TopClass_MANY_", "system::imports::import__test_model_testSource2_pure_1");
        SetIterable<String> someExpectedReferences = Sets.mutable.with("meta::pure::functions::collection::concatenate_T_MANY__T_MANY__T_MANY_", "meta::pure::metamodel::function::ConcreteFunctionDefinition", "meta::pure::metamodel::multiplicity::ZeroMany", "meta::pure::metamodel::type::Any", "meta::pure::metamodel::type::FunctionType", "meta::pure::metamodel::type::generics::GenericType", "meta::pure::metamodel::type::generics::TypeParameter", "meta::pure::metamodel::valuespecification::VariableExpression", "test::LeftClass", "test::RightClass", "test::TopClass");

        Pair<SourceSerializationResult, byte[]> serializationResult = this.serializeSource("/test/model/testSource2.pure");
        SourceSerializationResult result = serializationResult.getOne();
        byte[] serialized = serializationResult.getTwo();

        this.assertSetsEqual(expectedInstances, result.getSerializedInstances());
        this.assertContainsAll(someExpectedReferences, result.getExternalReferences());
        this.assertExternalReferencePathsResolvable(result.getExternalReferences());

        SourceDeserializationResult deserializationResult = BinaryModelSourceDeserializer.readIndexes(BinaryReaders.newBinaryReader(serialized));
        this.assertSetsEqual(expectedInstances, deserializationResult.getInstances().toSet());
        this.assertContainsAll(someExpectedReferences, deserializationResult.getExternalReferences());
    }

    @Test
    public void testNonConcreteMultiplicityReference()
    {
        this.compileTestSourceM3("/test/model/testSource.pure",
                "function test::testFn(class:Class<Any>[1]) : AbstractProperty<Any>[*]\n" +
                        "{\n" +
                        "  []\n" +
                        "     ->concatenate($class.properties)\n" +
                        "     ->concatenate($class.propertiesFromAssociations)\n" +
                        "     ->concatenate($class.qualifiedProperties)\n" +
                        "     ->concatenate($class.qualifiedPropertiesFromAssociations)\n" +
                        "}\n");
        SetIterable<String> expectedInstances = Sets.mutable.with("test::testFn_Class_1__AbstractProperty_MANY_", "system::imports::import__test_model_testSource_pure_1");
        SetIterable<String> someExpectedReferences = Sets.mutable.with("meta::pure::functions::collection::concatenate_T_MANY__T_MANY__T_MANY_", "meta::pure::metamodel::function::ConcreteFunctionDefinition", "meta::pure::metamodel::function::property::AbstractProperty", "meta::pure::metamodel::function::property::Property", "meta::pure::metamodel::function::property::QualifiedProperty", "meta::pure::metamodel::multiplicity::PureOne", "meta::pure::metamodel::multiplicity::PureZero", "meta::pure::metamodel::multiplicity::ZeroMany", "meta::pure::metamodel::type::Any", "meta::pure::metamodel::type::Class", "meta::pure::metamodel::type::Nil");

        Pair<SourceSerializationResult, byte[]> serializationResult = this.serializeSource("/test/model/testSource.pure");
        SourceSerializationResult result = serializationResult.getOne();
        byte[] serialized = serializationResult.getTwo();

        this.assertSetsEqual(expectedInstances, result.getSerializedInstances());
        this.assertContainsAll(someExpectedReferences, result.getExternalReferences());
        this.assertExternalReferencePathsResolvable(result.getExternalReferences());

        SourceDeserializationResult deserializationResult = BinaryModelSourceDeserializer.readIndexes(BinaryReaders.newBinaryReader(serialized));
        this.assertSetsEqual(expectedInstances, deserializationResult.getInstances().toSet());
        this.assertContainsAll(someExpectedReferences, deserializationResult.getExternalReferences());
    }

    @Test
    public void testFunctionTypeReference()
    {
        this.runtime.createInMemorySource("/test/model/testSource.pure",
                "import test::matrix::*;\n" +
                        "\n" +
                        "Class test::matrix::Matrix\n" +
                        "{\n" +
                        "  rows : Row[*];\n" +
                        "}\n" +
                        "\n" +
                        "Class test::matrix::Row\n" +
                        "{\n" +
                        "  values : Number[*];\n" +
                        "}\n" +
                        "\n" +
                        "function test::matrix::filter(matrix:Matrix[1], func:Function<{Row[1]->Boolean[1]}>[1]):Matrix[1]\n" +
                        "{\n" +
                        "  ^$matrix(rows=$matrix.rows->filter($func))\n" +
                        "}");
        this.runtime.compile();
        this.runtime.createInMemorySource("/test/model/testSource2.pure",
                "import test::matrix::*;\n" +
                        "function test::matrix::testFn(m:Matrix[1]):Matrix[1]\n" +
                        "{\n" +
                        "  $m->filter(r | $r.values->forAll(x | $x < 5))\n" +
                        "}");
        this.runtime.compile();
        SetIterable<String> expectedInstances = Sets.mutable.with("test::matrix::testFn_Matrix_1__Matrix_1_", "system::imports::import__test_model_testSource2_pure_1");
        SetIterable<String> someExpectedReferences = Sets.mutable.with("test::matrix::Matrix", "test::matrix::Row", "test::matrix::filter_Matrix_1__Function_1__Matrix_1_");

        Pair<SourceSerializationResult, byte[]> serializationResult = this.serializeSource("/test/model/testSource2.pure");
        SourceSerializationResult result = serializationResult.getOne();
        byte[] serialized = serializationResult.getTwo();

        this.assertSetsEqual(expectedInstances, result.getSerializedInstances());
        this.assertContainsAll(someExpectedReferences, result.getExternalReferences());
        this.assertExternalReferencePathsResolvable(result.getExternalReferences());

        SourceDeserializationResult deserializationResult = BinaryModelSourceDeserializer.readIndexes(BinaryReaders.newBinaryReader(serialized));
        this.assertSetsEqual(expectedInstances, deserializationResult.getInstances().toSet());
        this.assertContainsAll(someExpectedReferences, deserializationResult.getExternalReferences());
    }

    @Test
    public void testTreePath()
    {
        this.compileTestSource("/test/model/testSource.pure",
                "import test::treepath::*;\n" +
                        "Class test::treepath::TestClass1\n" +
                        "{\n" +
                        "  name : String[1];\n" +
                        "  others : TestClass2[*];\n" +
                        "  otherStuff : Any[*];\n" +
                        "}\n" +
                        "\n" +
                        "Class test::treepath::TestClass2\n" +
                        "{\n" +
                        "  mainName : String[1];\n" +
                        "  otherNames : String[*];\n" +
                        "}");
        this.compileTestSource("/test/model/testSource2.pure",
                "import test::treepath::*;\n" +
                        "function test::treepath::simpleTreePath():Any[*]\n" +
                        "{\n" +
                        "  #\n" +
                        "   TestClass1 as TestProjection\n" +
                        "   {\n" +
                        "     others as TestClass2\n" +
                        "     {\n" +
                        "       *\n" +
                        "     }\n" +
                        "   }\n" +
                        "  #\n" +
                        "}");
        SetIterable<String> expectedInstances = Sets.mutable.with("test::treepath::simpleTreePath__Any_MANY_", "system::imports::import__test_model_testSource2_pure_1");
        SetIterable<String> someExpectedReferences = Sets.mutable.with("test::treepath::TestClass1", "test::treepath::TestClass2");

        Pair<SourceSerializationResult, byte[]> serializationResult = this.serializeSource("/test/model/testSource2.pure");
        SourceSerializationResult result = serializationResult.getOne();
        byte[] serialized = serializationResult.getTwo();

        this.assertSetsEqual(expectedInstances, result.getSerializedInstances());
        this.assertContainsAll(someExpectedReferences, result.getExternalReferences());
        this.assertExternalReferencePathsResolvable(result.getExternalReferences());

        SourceDeserializationResult deserializationResult = BinaryModelSourceDeserializer.readIndexes(BinaryReaders.newBinaryReader(serialized));
        this.assertSetsEqual(expectedInstances, deserializationResult.getInstances().toSet());
        this.assertContainsAll(someExpectedReferences, deserializationResult.getExternalReferences());
    }

    @Test
    public void testM3Serialization()
    {
        Source source = this.runtime.getSourceById("/platform/pure/m3.pure");
        SetIterable<String> expectedInstances = this.collectInstancePaths(source.getNewInstances(), Sets.mutable.with("meta", "meta::pure", "meta::pure::functions", "meta::pure::functions::lang", "meta::pure::metamodel", "meta::pure::metamodel::extension", "meta::pure::metamodel::function", "meta::pure::metamodel::function::property", "meta::pure::metamodel::import", "meta::pure::metamodel::multiplicity", "meta::pure::metamodel::relationship", "meta::pure::metamodel::treepath", "meta::pure::metamodel::type", "meta::pure::metamodel::type::generics", "meta::pure::metamodel::constraint", "meta::pure::metamodel::valuespecification", "meta::pure::router", "meta::pure::tools", "system", "system::imports"));
        SetIterable<String> expectedReferences = Sets.mutable.with();

        Pair<SourceSerializationResult, byte[]> serializationResult = this.serializeSource(source);
        SourceSerializationResult result = serializationResult.getOne();
        byte[] serialized = serializationResult.getTwo();

        this.assertSetsEqual(expectedInstances, result.getSerializedInstances());
        this.assertSetsEqual(expectedReferences, result.getExternalReferences());
        this.assertExternalReferencePathsResolvable(result.getExternalReferences());

        SourceDeserializationResult deserializationResult = BinaryModelSourceDeserializer.readIndexes(BinaryReaders.newBinaryReader(serialized));
        this.assertSetsEqual(expectedInstances, deserializationResult.getInstances().toSet());
        this.assertSetsEqual(expectedReferences, deserializationResult.getExternalReferences());
    }

    @Test
    public void testPlatformSerialization()
    {
        MutableListMultimap<String, String> importGroupsByBaseName = Multimaps.mutable.list.empty();
        CoreInstance systemImports = this.runtime.getCoreInstance("system::imports");
        if (systemImports != null)
        {
            for (CoreInstance importGroup : systemImports.getValueForMetaPropertyToMany(M3Properties.children))
            {
                importGroupsByBaseName.put(Imports.getImportGroupBaseName(importGroup), "system::imports::" + importGroup.getName());
            }
        }


        for (Source source : this.runtime.getSourceRegistry().getSources())
        {
            MutableSet<String> expectedInstances = this.getInstancePaths(source.getNewInstances());
            expectedInstances.addAllIterable(importGroupsByBaseName.get(Source.formatForImportGroupId(source.getId())));
            if ("/platform/pure/m3.pure".equals(source.getId()))
            {
                expectedInstances.addAllIterable(Lists.mutable.with("meta", "meta::pure", "meta::pure::functions", "meta::pure::functions::lang", "meta::pure::metamodel", "meta::pure::metamodel::constraint", "meta::pure::metamodel::extension", "meta::pure::metamodel::function", "meta::pure::metamodel::function::property", "meta::pure::metamodel::import", "meta::pure::metamodel::multiplicity", "meta::pure::metamodel::relationship", "meta::pure::metamodel::treepath", "meta::pure::metamodel::type", "meta::pure::metamodel::type::generics", "meta::pure::metamodel::valuespecification", "meta::pure::router", "meta::pure::tools", "system", "system::imports"));
            }

            Pair<SourceSerializationResult, byte[]> serializationResult = this.serializeSource(source);
            SourceSerializationResult result = serializationResult.getOne();
            byte[] serialized = serializationResult.getTwo();

            this.assertSetsEqual(expectedInstances, result.getSerializedInstances());
            this.assertExternalReferencePathsResolvable(result.getExternalReferences(), source.getId());

            SourceDeserializationResult deserializationResult = BinaryModelSourceDeserializer.readIndexes(BinaryReaders.newBinaryReader(serialized));
            this.assertSetsEqual(expectedInstances, deserializationResult.getInstances().toSet());
            this.assertSetsEqual(result.getExternalReferences(), deserializationResult.getExternalReferences());
        }
    }

    @Test
    public void testBinaryStability_SameRuntime()
    {
        for (Source source : this.runtime.getSourceRegistry().getSources())
        {
            for (int i = 0; i < 10; i++)
            {
                byte[] bytes1 = this.serializeSource(source).getTwo();
                byte[] bytes2 = this.serializeSource(source).getTwo();
                Assert.assertArrayEquals("Failure for source '" + source.getId() + "' on iteration #" + (i + 1), bytes1, bytes2);
            }
        }
    }

    @Test
    public void testBinaryStability_DifferentRuntimes()
    {
        for (int i = 0; i < 10; i++)
        {
            PureRuntime runtime2 = new PureRuntimeBuilder(this.runtime.getCodeStorage())
                    .withRuntimeStatus(this.getPureRuntimeStatus()).build();
            this.getFunctionExecution().init(runtime2, new Message(""));
            runtime2.loadAndCompileCore();

            Assert.assertEquals(this.runtime.getSourceRegistry().getSources().select(s->!s.isInMemory()).collect(s->s.getId()).toSet(), runtime2.getSourceRegistry().getSourceIds().toSet());

            for (Source source1 : this.runtime.getSourceRegistry().getSources().select(s->!s.isInMemory()))
            {
                Source source2 = runtime2.getSourceById(source1.getId());
                byte[] bytes1 = this.serializeSource(source1).getTwo();
                byte[] bytes2 = this.serializeSource(source2, runtime2).getTwo();
                Assert.assertArrayEquals("Failure for source '" + source1.getId() + "' on iteration #" + (i + 1), bytes1, bytes2);
            }
        }
    }

    protected Pair<SourceSerializationResult, byte[]> serializeSource(String sourceId)
    {
        Source source = this.runtime.getSourceById(sourceId);
        Assert.assertNotNull("Unknown source: " + sourceId, source);
        return this.serializeSource(source);
    }

    protected Pair<SourceSerializationResult, byte[]> serializeSource(Source source)
    {
        return this.serializeSource(source, this.runtime);
    }

    protected Pair<SourceSerializationResult, byte[]> serializeSource(Source source, PureRuntime runtime)
    {
        ByteArrayOutputStream stream = new ByteArrayOutputStream();
        SourceSerializationResult result = BinaryModelSourceSerializer.serialize(BinaryWriters.newBinaryWriter(stream), source, runtime);
        return Tuples.pair(result, stream.toByteArray());
    }

    protected MutableSet<String> getInstancePaths(RichIterable<? extends CoreInstance> instances)
    {
        return this.collectInstancePaths(instances, UnifiedSet.<String>newSet(instances.size()));
    }

    protected <T extends Collection<? super String>> T collectInstancePaths(Iterable<? extends CoreInstance> instances, T target)
    {
        for (CoreInstance instance : instances)
        {
            target.add(PackageableElement.getUserPathForPackageableElement(instance));
        }
        return target;
    }

    protected void assertExternalReferencePathsResolvable(Iterable<String> paths)
    {
        this.assertExternalReferencePathsResolvable(paths, null);
    }

    protected void assertExternalReferencePathsResolvable(Iterable<String> paths, String sourceId)
    {
        MutableList<String> badPaths = Lists.mutable.empty();
        for (String path : paths)
        {
            CoreInstance instance = this.processorSupport.package_getByUserPath(path);
            if (instance == null)
            {
                badPaths.add(path);
            }
        }
        if (badPaths.notEmpty())
        {
            StringBuilder message = new StringBuilder("Could not resolve the following external reference paths");
            if (sourceId != null)
            {
                message.append(" for source ");
                message.append(sourceId);
            }
            badPaths.sortThis().appendString(message, ": ", ", ", "");
            Assert.fail(message.toString());
        }
    }

    protected <T> void assertSetsEqual(SetIterable<T> expected, SetIterable<T> actual)
    {
        this.assertSetsEqual(null, expected, actual);
    }

    protected <T> void assertSetsEqual(String description, SetIterable<T> expected, SetIterable<T> actual)
    {
        if (description == null)
        {
            description = "elements";
        }
        if (!expected.equals(actual))
        {
            StringBuilder message = new StringBuilder();
            SetIterable<T> missing = expected.difference(actual);
            SetIterable<T> extra = actual.difference(expected);
            if (missing.notEmpty())
            {
                message.append("Missing ");
                message.append(description);
                missing.toSortedList().appendString(message, ": ", ", ", "");
            }
            if (extra.notEmpty())
            {
                if (missing.notEmpty())
                {
                    message.append("; ");
                }
                message.append("Unexpected ");
                message.append(description);
                extra.toSortedList().appendString(message, ": ", ", ", "");
            }
            Assert.fail(message.toString());
        }
    }

    protected <T> void assertContainsAll(Iterable<T> expected, Iterable<T> actual)
    {
        this.assertContainsAll(null, expected, actual);
    }

    protected <T> void assertContainsAll(String description, Iterable<T> expected, Iterable<T> actual)
    {
        Collection<T> missing = Iterate.select(expected, Predicates.notIn(actual));
        if (!missing.isEmpty())
        {
            StringBuilder message = new StringBuilder("Missing ");
            message.append(description == null ? "elements" : description);
            message.append(": ");
            LazyIterate.collect(missing, Functions.getToString()).toSortedList().appendString(message, ", ");
            Assert.fail(message.toString());
        }
    }
}
