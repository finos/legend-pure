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

package org.finos.legend.pure.runtime.java.extension.dsl.mapping.compiled;

import org.eclipse.collections.api.factory.Lists;
import org.eclipse.collections.api.factory.Sets;
import org.eclipse.collections.api.list.MutableList;
import org.eclipse.collections.api.set.MutableSet;
import org.eclipse.collections.impl.tuple.Tuples;
import org.finos.legend.pure.m3.coreinstance.meta.external.store.model.PureInstanceSetImplementation;
import org.finos.legend.pure.m3.coreinstance.meta.external.store.model.PurePropertyMapping;
import org.finos.legend.pure.m3.coreinstance.meta.pure.mapping.Mapping;
import org.finos.legend.pure.m3.tests.AbstractPureTestWithCoreCompiled;
import org.finos.legend.pure.runtime.java.compiled.execution.CompiledExecutionSupport;
import org.finos.legend.pure.runtime.java.compiled.execution.FunctionExecutionCompiled;
import org.finos.legend.pure.runtime.java.compiled.execution.FunctionExecutionCompiledBuilder;
import org.finos.legend.pure.runtime.java.compiled.extension.CompiledExtensionLoader;
import org.finos.legend.pure.runtime.java.compiled.generation.processors.support.Pure;
import org.junit.Assert;
import org.junit.BeforeClass;
import org.junit.Test;

public class TestMappingExtensionCompiled extends AbstractPureTestWithCoreCompiled
{
    @BeforeClass
    public static void setUp()
    {
        setUpRuntime(
                new FunctionExecutionCompiledBuilder().build(),
                Tuples.pair(
                        "test.pure",
                        "Class Firm\n" +
                                "{\n" +
                                "  legalName : String[1];\n" +
                                "}\n" +
                                "Class FirmSource\n" +
                                "{\n" +
                                "   val : String[1];\n" +
                                "}\n" +
                                "\n" +
                                "###Mapping\n" +
                                "Mapping test::TestMapping\n" +
                                "(\n" +
                                "  Firm[firm] : Pure\n" +
                                "  {\n" +
                                "    ~src FirmSource\n" +
                                "    ~filter $src.val == 'ok'\n" +
                                "    legalName : $src.val\n" +
                                "  }\n" +
                                ")\n"));
    }

    @Test
    public void testMappingExtensionAsService()
    {
        MutableSet<Class<?>> extensionClasses = CompiledExtensionLoader.extensions().collect(Object::getClass, Sets.mutable.empty());
        if (!extensionClasses.contains(MappingExtensionCompiled.class))
        {
            Assert.fail("Could not find " + MappingExtensionCompiled.class.getName() + " in extensions: " + extensionClasses.collect(Class::getName, Lists.mutable.empty()).sortThis());
        }
    }

    @Test
    public void testSharedPureFunctions()
    {
        CompiledExecutionSupport executionSupport = ((FunctionExecutionCompiled) functionExecution).getExecutionSupport();
        Mapping mapping = (Mapping) runtime.getCoreInstance("test::TestMapping");
        MutableList<String> missingImpls = Lists.mutable.empty();
        mapping._classMappings().forEach(cm ->
        {
            if (cm instanceof PureInstanceSetImplementation)
            {
                PureInstanceSetImplementation classMapping = (PureInstanceSetImplementation) cm;
                String id = classMapping._id();
                if (!Pure.canFindNativeOrLambdaFunction(executionSupport, classMapping._filter()))
                {
                    missingImpls.add(id + "~filter");
                }
                classMapping._propertyMappings().forEach(pm ->
                {
                    if (!Pure.canFindNativeOrLambdaFunction(executionSupport, ((PurePropertyMapping) pm)._transform()))
                    {
                        missingImpls.add(id + "." + pm._property()._name());
                    }
                });
            }
        });
        if (missingImpls.notEmpty())
        {
            Assert.fail(missingImpls.makeString("Missing implementations for: ", ", ", ""));
        }
    }
}
