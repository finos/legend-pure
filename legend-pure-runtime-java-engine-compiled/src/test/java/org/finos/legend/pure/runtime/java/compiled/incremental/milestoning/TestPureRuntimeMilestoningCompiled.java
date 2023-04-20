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

package org.finos.legend.pure.runtime.java.compiled.incremental.milestoning;

import org.eclipse.collections.api.factory.Lists;
import org.eclipse.collections.api.list.ListIterable;
import org.finos.legend.pure.m3.serialization.filesystem.usercodestorage.classpath.ClassLoaderCodeStorage;
import org.finos.legend.pure.m3.tests.RuntimeVerifier;
import org.finos.legend.pure.m3.execution.FunctionExecution;
import org.finos.legend.pure.m3.serialization.filesystem.usercodestorage.composite.CompositeCodeStorage;
import org.finos.legend.pure.m3.tests.incremental.milestoning.TestMilestoning;
import org.finos.legend.pure.m3.tools.test.ToFix;
import org.finos.legend.pure.runtime.java.compiled.CompiledClassloaderStateVerifier;
import org.finos.legend.pure.runtime.java.compiled.CompiledMetadataStateVerifier;
import org.finos.legend.pure.runtime.java.compiled.execution.FunctionExecutionCompiledBuilder;
import org.finos.legend.pure.runtime.java.compiled.factory.JavaModelFactoryRegistryLoader;
import org.junit.BeforeClass;
import org.junit.Ignore;
import org.junit.Test;

public class TestPureRuntimeMilestoningCompiled extends TestMilestoning
{
    @BeforeClass
    public static void setUp()
    {
        setUpRuntime(getFunctionExecution(), new CompositeCodeStorage(new ClassLoaderCodeStorage(getCodeRepositories())), JavaModelFactoryRegistryLoader.loader(), getOptions(), getExtra());
    }

    protected static FunctionExecution getFunctionExecution()
    {
        return new FunctionExecutionCompiledBuilder().build();
    }

    @Override
    protected ListIterable<RuntimeVerifier.FunctionExecutionStateVerifier> getAdditionalVerifiers()
    {
        return Lists.fixedSize.of(new CompiledMetadataStateVerifier(), new CompiledClassloaderStateVerifier());
    }

    @ToFix
    @Ignore
    @Test
    @Override
    public void testStabilityOnTemporalStereotypeRemovalWithAssociation()
    {
        super.testStabilityOnTemporalStereotypeRemovalWithAssociation();
    }
}
