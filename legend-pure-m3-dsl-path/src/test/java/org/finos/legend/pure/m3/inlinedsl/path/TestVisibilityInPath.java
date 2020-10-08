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

package org.finos.legend.pure.m3.inlinedsl.path;

import org.eclipse.collections.api.RichIterable;
import org.eclipse.collections.impl.factory.Lists;
import org.eclipse.collections.impl.factory.Sets;
import org.finos.legend.pure.m3.AbstractPureTestWithCoreCompiled;
import org.finos.legend.pure.m3.serialization.filesystem.PureCodeStorage;
import org.finos.legend.pure.m3.serialization.filesystem.repository.CodeRepository;
import org.finos.legend.pure.m3.serialization.filesystem.repository.SVNCodeRepository;
import org.finos.legend.pure.m3.serialization.filesystem.usercodestorage.MutableCodeStorage;
import org.finos.legend.pure.m3.serialization.filesystem.usercodestorage.classpath.ClassLoaderCodeStorage;
import org.finos.legend.pure.m4.exception.PureCompilationException;
import org.junit.Assert;
import org.junit.Test;

public class TestVisibilityInPath extends AbstractPureTestWithCoreCompiled
{
    @Override
    protected RichIterable<? extends CodeRepository> getCodeRepositories()
    {
        return Lists.immutable.with(
                SVNCodeRepository.newDatamartCodeRepository("dtm"),
                SVNCodeRepository.newDatamartCodeRepository("datamt"),
                SVNCodeRepository.newModelCodeRepository(""),
                SVNCodeRepository.newModelCodeRepository("candidate", Sets.immutable.with("")),
                SVNCodeRepository.newModelCodeRepository("legacy", Sets.immutable.with("")),
                SVNCodeRepository.newModelValidationCodeRepository(),
                SVNCodeRepository.newSystemCodeRepository(),
                CodeRepository.newCoreCodeRepository(),
                CodeRepository.newPlatformCodeRepository()
        );
    }

    @Override
    protected MutableCodeStorage getCodeStorage()
    {
        return new PureCodeStorage(null, new ClassLoaderCodeStorage(getCodeRepositories()));
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
        Assert.assertNotNull(this.runtime.getCoreInstance("datamarts::datamt::domain::A"));
        try
        {
            compileTestSource(
                    "/datamart_dtm/testFile3.pure",
                    "import meta::pure::metamodel::path::*;\n" +
                            "function datamarts::dtm::domain::testFn1():Path<Nil,String|1>[1]\n" +
                            "{\n" +
                            "  #/datamarts::datamt::domain::A/name#\n" +
                            "}\n");
            Assert.fail("Expected compilation error");
        }
        catch (Exception e)
        {
            assertPureException(PureCompilationException.class, "datamarts::datamt::domain::A is not visible in the file /datamart_dtm/testFile3.pure", "/datamart_dtm/testFile3.pure", 4, 32, 4, 32, 4, 32, e);
        }
    }
}
