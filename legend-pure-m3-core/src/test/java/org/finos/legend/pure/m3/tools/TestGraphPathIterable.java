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

package org.finos.legend.pure.m3.tools;

import org.eclipse.collections.api.block.predicate.Predicate;
import org.eclipse.collections.api.set.MutableSet;
import org.eclipse.collections.impl.block.factory.Functions;
import org.eclipse.collections.impl.block.factory.Predicates;
import org.eclipse.collections.impl.factory.Lists;
import org.eclipse.collections.impl.factory.Sets;
import org.eclipse.collections.impl.test.Verify;
import org.finos.legend.pure.m3.AbstractPureTestWithCoreCompiledPlatform;
import org.finos.legend.pure.m4.coreinstance.CoreInstance;
import org.junit.Before;
import org.junit.Test;

public class TestGraphPathIterable extends AbstractPureTestWithCoreCompiledPlatform
{
    @Before
    public void testModel()
    {
        compileTestSource("/test/testModel.pure",
                "import test::domain::*;\n" +
                        "Class test::domain::ClassA\n" +
                        "{\n" +
                        "  prop1 : String[1];\n" +
                        "  prop2 : ClassB[*];\n" +
                        "}\n" +
                        "\n" +
                        "Class test::domain::ClassB\n" +
                        "{\n" +
                        "  prop3 : String[0..1];\n" +
                        "}\n");
    }

    @Test
    public void testFromPackage()
    {
        MutableSet<String> paths = GraphPathIterable.newGraphPathIterable(Lists.immutable.with("test::domain"), 1, this.processorSupport).collect(GraphPath.GET_DESCRIPTION).toSet();
        Verify.assertContainsAll(paths, "test::domain.package", "test::domain.name", "test::domain", "test::domain.children[0]", "test::domain.children[1]");
    }

    @Test
    public void testClassAToClassB()
    {
        CoreInstance classB = this.runtime.getCoreInstance("test::domain::ClassB");
        Predicate<GraphPath> endsWithClassB = Predicates.attributePredicate(Functions.bind(GraphPath.RESOLVE, this.processorSupport), Predicates.equal(classB));

        MutableSet<String> paths1 = GraphPathIterable.newGraphPathIterable(Lists.immutable.with("test::domain::ClassA"), Predicates.equal(classB), 5, this.processorSupport).collectIf(endsWithClassB, GraphPath.GET_DESCRIPTION).toSet();
        Verify.assertSetsEqual(Sets.mutable.with("test::domain::ClassA.package.children[1]", "test::domain::ClassA.properties[1].genericType.rawType.resolvedNode", "test::domain::ClassA.properties[1].classifierGenericType.typeArguments[1].rawType.resolvedNode"), paths1);
    }

    @Test
    public void testFromClassA()
    {
        MutableSet<String> paths = GraphPathIterable.newGraphPathIterable(Lists.immutable.with("test::domain::ClassA"), this.processorSupport).collect(GraphPath.GET_DESCRIPTION).toSet();
        Verify.assertContainsAll(paths, "test::domain::ClassA.properties[1].genericType.rawType.resolvedNode", "test::domain::ClassA.properties[1].classifierGenericType.typeArguments[1].rawType.resolvedNode");
    }

    @Test
    public void testFromClassAWithLimitedProperties()
    {
        MutableSet<String> paths = GraphPathIterable.newGraphPathIterable(Lists.immutable.with("test::domain::ClassA"), Lists.immutable.with("properties", "genericType", "rawType"), -1, this.processorSupport).collect(GraphPath.GET_DESCRIPTION).toSet();
        Verify.assertSetsEqual(Sets.mutable.with("test::domain::ClassA", "test::domain::ClassA.properties[0]", "test::domain::ClassA.properties[0].genericType", "test::domain::ClassA.properties[0].genericType.rawType", "test::domain::ClassA.properties[1]", "test::domain::ClassA.properties[1].genericType", "test::domain::ClassA.properties[1].genericType.rawType"), paths);
    }
}
