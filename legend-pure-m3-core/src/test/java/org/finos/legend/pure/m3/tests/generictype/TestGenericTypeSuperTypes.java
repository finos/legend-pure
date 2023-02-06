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

package org.finos.legend.pure.m3.tests.generictype;

import org.finos.legend.pure.m3.navigation.generictype.GenericType;
import org.eclipse.collections.api.list.ListIterable;
import org.eclipse.collections.api.list.MutableList;
import org.eclipse.collections.impl.factory.Lists;
import org.eclipse.collections.impl.list.mutable.FastList;
import org.eclipse.collections.impl.test.Verify;
import org.finos.legend.pure.m3.tests.AbstractPureTestWithCoreCompiledPlatform;
import org.finos.legend.pure.m3.navigation.M3Paths;
import org.finos.legend.pure.m3.navigation.M3Properties;
import org.finos.legend.pure.m3.navigation.Instance;
import org.finos.legend.pure.m3.navigation.type.Type;
import org.finos.legend.pure.m4.coreinstance.CoreInstance;
import org.junit.After;
import org.junit.BeforeClass;
import org.junit.Test;

public class TestGenericTypeSuperTypes extends AbstractPureTestWithCoreCompiledPlatform
{
    @BeforeClass
    public static void setUp() {
        setUpRuntime(getExtra());
    }

    @After
    public void clearRuntime() {
        runtime.delete("fromString.pure");
    }

    @Test
    public void testDSBCase()
    {
        compileTestSource("fromString.pure","import test::*;\n" +
                "\n" +
                "Class test::Post\n" +
                "{\n" +
                "}\n" +
                "\n" +
                "Class test::Pre \n" +
                "{\n" +
                "}\n" +
                "\n" +
                "Class test::DataSetFilterOperation<F>\n" +
                "{\n" +
                "}\n" +
                "\n" +
                "Class test::DataSetComparisonPostFilterOperation<T|m> extends DataSetFilterOperation<Post>\n" +
                "{\n" +
                "     value : T[m];\n" +
                "}\n" +
                "\n" +
                "Class test::DataSetCompositeFilterOperation<F> extends DataSetFilterOperation<F>\n" +
                "{\n" +
                "     rules : DataSetFilterOperation<F>[*];\n" +
                "}\n");
        CoreInstance genericType1 = Type.wrapGenericType(this.runtime.getCoreInstance("test::DataSetCompositeFilterOperation"), this.processorSupport);
        Instance.addValueToProperty(genericType1, M3Properties.typeArguments, Type.wrapGenericType(this.runtime.getCoreInstance("test::Post"), this.processorSupport), this.processorSupport);
        Verify.assertListsEqual(Lists.mutable.with("test::DataSetCompositeFilterOperation<test::Post>", "test::DataSetFilterOperation<test::Post>", M3Paths.Any), getSuperTypesAsStrings(genericType1));

        CoreInstance genericType2 = Type.wrapGenericType(this.runtime.getCoreInstance("test::DataSetComparisonPostFilterOperation"), this.processorSupport);
        Instance.addValueToProperty(genericType2, M3Properties.typeArguments, Type.wrapGenericType(this.runtime.getCoreInstance(M3Paths.Any), this.processorSupport), this.processorSupport);
        Instance.addValueToProperty(genericType2, M3Properties.multiplicityArguments, this.runtime.getCoreInstance(M3Paths.PureOne), this.processorSupport);
        Verify.assertListsEqual(Lists.mutable.with("test::DataSetComparisonPostFilterOperation<" + M3Paths.Any + "|1>", "test::DataSetFilterOperation<test::Post>", M3Paths.Any), getSuperTypesAsStrings(genericType2));
    }

    @Test
    public void testWithMultiLevelTypeAndMultiplicityArguments()
    {
        compileTestSource("fromString.pure","import test::*;\n" +
                "\n" +
                "Class test::A<T,U,V|a,b,c> {}\n" +
                "Class test::B<W,X|d,e> extends A<W,X,String|d,e,1> {}\n" +
                "Class test::C<Y|f> extends B<Y,Integer|f,*> {}\n");

        CoreInstance genericType1 = Type.wrapGenericType(this.runtime.getCoreInstance("test::C"), this.processorSupport);
        Instance.addValueToProperty(genericType1, M3Properties.typeArguments, Type.wrapGenericType(this.runtime.getCoreInstance(M3Paths.Date), this.processorSupport), this.processorSupport);
        Instance.addValueToProperty(genericType1, M3Properties.multiplicityArguments, this.runtime.getCoreInstance(M3Paths.ZeroOne), this.processorSupport);
        Verify.assertListsEqual(Lists.mutable.with("test::C<Date|0..1>", "test::B<Date, Integer|0..1, *>", "test::A<Date, Integer, String|0..1, *, 1>", M3Paths.Any), getSuperTypesAsStrings(genericType1));
    }

    @Test
    public void testWithNestedTypeArguments()
    {
        compileTestSource("fromString.pure","import test::*;\n" +
                "\n" +
                "Class test::A<W> {}\n" +
                "Class test::B<X> {}\n" +
                "Class test::C<Y> extends A<B<Y>> {}\n" +
                "Class test::D<Z> extends test::C<Z> {}\n");
        CoreInstance genericType1 = Type.wrapGenericType(this.runtime.getCoreInstance("test::D"), this.processorSupport);
        Instance.addValueToProperty(genericType1, M3Properties.typeArguments, Type.wrapGenericType(this.runtime.getCoreInstance(M3Paths.Boolean), this.processorSupport), this.processorSupport);
        Verify.assertListsEqual(Lists.mutable.with("test::D<Boolean>", "test::C<Boolean>", "test::A<test::B<Boolean>>", M3Paths.Any), getSuperTypesAsStrings(genericType1));
    }

    private MutableList<String> getSuperTypesAsStrings(CoreInstance genericType)
    {
        ListIterable<CoreInstance> superTypes = GenericType.getAllSuperTypesIncludingSelf(genericType, this.processorSupport);
        MutableList<String> result = FastList.newList(superTypes.size());
        for (CoreInstance superType : superTypes)
        {
            result.add(GenericType.print(superType, true, this.processorSupport));
        }
        return result;
    }
}
