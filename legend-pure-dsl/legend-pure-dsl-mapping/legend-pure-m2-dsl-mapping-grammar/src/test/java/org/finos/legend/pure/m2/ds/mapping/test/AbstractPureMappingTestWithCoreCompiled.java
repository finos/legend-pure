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

import org.eclipse.collections.api.RichIterable;
import org.eclipse.collections.api.factory.Lists;
import org.finos.legend.pure.m3.tests.AbstractPureTestWithCoreCompiled;
import org.finos.legend.pure.m3.coreinstance.meta.pure.mapping.SetImplementation;
import org.finos.legend.pure.m3.coreinstance.meta.pure.metamodel.PackageableElement;
import org.finos.legend.pure.m3.coreinstance.meta.pure.metamodel.ReferenceUsage;
import org.finos.legend.pure.m4.coreinstance.SourceInformation;
import org.junit.Assert;

public class AbstractPureMappingTestWithCoreCompiled extends AbstractPureTestWithCoreCompiled
{
    protected void assertSetSourceInformation(String source, String classPath)
    {
        String className = Lists.immutable.of(classPath.split("::")).getLast();
        PackageableElement packageableElement = (PackageableElement) runtime.getCoreInstance(classPath);
        RichIterable<? extends ReferenceUsage> typeViewReferenceUsages = packageableElement._referenceUsages().select(usage -> usage._owner() instanceof SetImplementation);
        String[] lines = source.split("\\R");
        typeViewReferenceUsages.forEach(referenceUsage ->
        {
            SourceInformation sourceInformation = referenceUsage._ownerCoreInstance().getSourceInformation();
            Assert.assertEquals(className, lines[sourceInformation.getLine() - 1].substring(sourceInformation.getColumn() - 1, sourceInformation.getColumn() + className.length() - 1));
        });
    }
}
