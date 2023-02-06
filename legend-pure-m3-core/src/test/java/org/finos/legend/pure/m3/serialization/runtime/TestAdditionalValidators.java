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

package org.finos.legend.pure.m3.serialization.runtime;

import org.eclipse.collections.api.RichIterable;
import org.eclipse.collections.api.factory.Lists;
import org.finos.legend.pure.m3.tests.AbstractPureTestWithCoreCompiled;
import org.finos.legend.pure.m3.compiler.Context;
import org.finos.legend.pure.m3.coreinstance.meta.pure.metamodel.type.Class;
import org.finos.legend.pure.m3.navigation.M3Paths;
import org.finos.legend.pure.m3.serialization.filesystem.PureCodeStorage;
import org.finos.legend.pure.m3.serialization.filesystem.repository.CodeRepository;
import org.finos.legend.pure.m3.serialization.filesystem.repository.GenericCodeRepository;
import org.finos.legend.pure.m3.serialization.filesystem.repository.PlatformCodeRepository;
import org.finos.legend.pure.m3.tools.matcher.MatchRunner;
import org.finos.legend.pure.m3.tools.matcher.Matcher;
import org.finos.legend.pure.m3.tools.matcher.MatcherState;
import org.finos.legend.pure.m4.ModelRepository;
import org.finos.legend.pure.m4.exception.PureCompilationException;
import org.junit.After;
import org.junit.Assert;
import org.junit.BeforeClass;
import org.junit.Test;

public class TestAdditionalValidators extends AbstractPureTestWithCoreCompiled
{
    @BeforeClass
    public static void setUp()
    {
        setUpRuntime(getFunctionExecution(), PureCodeStorage.createCodeStorage(getCodeStorageRoot(), getCodeRepositories()), getFactoryRegistryOverride(), getOptions(), getExtra());
    }

    protected static RichIterable<? extends CodeRepository> getCodeRepositories()
    {
        return Lists.immutable.with(CodeRepository.newPlatformCodeRepository(),
                GenericCodeRepository.build("test", "test(::.*)?", PlatformCodeRepository.NAME, "system"));
    }

    @After
    public void cleanRuntime()
    {
        runtime.delete("/test/testClass.pure");
        runtime.compile();
    }

    @Test
    public void testAdditionalValidators()
    {
        String sourceId = "/test/testClass.pure";
        String sourceCode = "Class test::" + TestValidator.FORBIDDEN_NAME + "\n" +
                "{\n" +
                "}\n";

        // Compile without additional validator (this should succeed)
        compileTestSource(sourceId, sourceCode);

        // Delete file
        runtime.delete(sourceId);
        runtime.compile();

        // Add validator and compile again (this should fail)
        runtime.getIncrementalCompiler().addValidator(new TestValidator());
        PureCompilationException e = Assert.assertThrows(PureCompilationException.class, () -> compileTestSource(sourceId, sourceCode));
        assertPureException(PureCompilationException.class, TestValidator.ERROR_MESSAGE, sourceId, 1, 1, 1, 13, 3, 1, e);
    }

    private static class TestValidator implements MatchRunner<Class<?>>
    {
        private static final String FORBIDDEN_NAME = "ForbiddenName";
        private static final String ERROR_MESSAGE = "The name " + FORBIDDEN_NAME + " is forbidden for classes";

        @Override
        public String getClassName()
        {
            return M3Paths.Class;
        }

        @Override
        public void run(Class<?> instance, MatcherState state, Matcher matcher, ModelRepository modelRepository, Context context)
        {
            if (FORBIDDEN_NAME.equals(instance.getName()))
            {
                throw new PureCompilationException(instance.getSourceInformation(), ERROR_MESSAGE);
            }
        }
    }
}
