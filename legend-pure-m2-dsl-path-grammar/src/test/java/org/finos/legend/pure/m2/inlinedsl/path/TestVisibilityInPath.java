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

package org.finos.legend.pure.m2.inlinedsl.path;

import org.eclipse.collections.api.RichIterable;
import org.eclipse.collections.api.factory.Lists;
import org.eclipse.collections.api.factory.Sets;
import org.finos.legend.pure.m3.serialization.filesystem.repository.GenericCodeRepository;
import org.finos.legend.pure.m3.serialization.filesystem.usercodestorage.composite.CompositeCodeStorage;
import org.finos.legend.pure.m3.serialization.filesystem.repository.CodeRepository;
import org.finos.legend.pure.m3.serialization.filesystem.repository.CodeRepositoryProviderHelper;
import org.finos.legend.pure.m3.serialization.filesystem.usercodestorage.MutableRepositoryCodeStorage;
import org.finos.legend.pure.m3.serialization.filesystem.usercodestorage.classpath.ClassLoaderCodeStorage;
import org.finos.legend.pure.m3.tests.AbstractPureTestWithCoreCompiled;
import org.finos.legend.pure.m4.exception.PureCompilationException;
import org.junit.After;
import org.junit.Assert;
import org.junit.BeforeClass;
import org.junit.Test;

public class TestVisibilityInPath extends AbstractPureTestWithCoreCompiled
{
    @BeforeClass
    public static void setUp()
    {
        setUpRuntime(getCodeStorage());
    }

    @After
    public void cleanRuntime()
    {
        runtime.delete("/datamart_datamt/testFile1.pure");
        runtime.delete("/datamart_dtm/testFile3.pure");
        runtime.compile();
    }

    protected static RichIterable<? extends CodeRepository> getCodeRepositories()
    {
        RichIterable<CodeRepository> repositories = CodeRepositoryProviderHelper.findCodeRepositories();
        return Lists.immutable.<CodeRepository>with(
                GenericCodeRepository.build("datamart_dtm", "((datamarts::dtm::(domain|mapping|store))|(apps::dtm))(::.*)?", "platform", "platform_dsl_path", "system", "model", "model_legacy"),
                GenericCodeRepository.build("datamart_datamt", "((datamarts::datamt::(domain|mapping|store))|(apps::datamt))(::.*)?", "platform", "platform_dsl_path", "system", "model", "model_legacy"),
                GenericCodeRepository.build("model", "(model::(domain|mapping|store|producers|consumers|external)||(apps::model))(::.*)?", "platform", "system"),
                GenericCodeRepository.build("model_candidate", "(model_candidate::(domain|mapping|store|producers|consumers|external)||(apps::model_candidate))(::.*)?", "platform", "system", "model"),
                GenericCodeRepository.build("model_legacy", "(model_legacy::(domain|mapping|store|producers|consumers|external)||(apps::model_legacy))(::.*)?", "platform", "system", "model"),
                GenericCodeRepository.build("model_validation", "(model::producers)(::.*)?", "platform", "system", "model"),
                GenericCodeRepository.build("system", "((meta)|(system)|(apps::pure))(::.*)?", "platform")
        ).newWithAll(repositories);
    }

    protected static MutableRepositoryCodeStorage getCodeStorage()
    {
        return new CompositeCodeStorage(new ClassLoaderCodeStorage(getCodeRepositories()));
    }

    @Test
    public void testReferenceInPath()
    {
        assertRepoExists("datamart_datamt");
        assertRepoExists("datamart_dtm");

        compileTestSource(
                "/datamart_datamt/testFile1.pure",
                "Class datamarts::datamt::domain::A\n" +
                        "{\n" +
                        "  name : String[1];\n" +
                        "}\n");
        Assert.assertNotNull(runtime.getCoreInstance("datamarts::datamt::domain::A"));
        PureCompilationException e = Assert.assertThrows(PureCompilationException.class, () -> compileTestSource(
                "/datamart_dtm/testFile3.pure",
                "import meta::pure::metamodel::path::*;\n" +
                        "function datamarts::dtm::domain::testFn1():Path<Nil,String|1>[1]\n" +
                        "{\n" +
                        "  #/datamarts::datamt::domain::A/name#\n" +
                        "}\n"));
        assertPureException(PureCompilationException.class, "datamarts::datamt::domain::A is not visible in the file /datamart_dtm/testFile3.pure", "/datamart_dtm/testFile3.pure", 4, 32, 4, 32, 4, 32, e);
    }
}
