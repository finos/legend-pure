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

package org.finos.legend.pure.m3;

import org.eclipse.collections.api.factory.Lists;
import org.eclipse.collections.api.list.MutableList;
import org.finos.legend.pure.m3.navigation.M3Paths;
import org.finos.legend.pure.m3.navigation.M3Properties;
import org.finos.legend.pure.m3.navigation.PackageableElement.PackageableElement;
import org.finos.legend.pure.m4.coreinstance.CoreInstance;
import org.finos.legend.pure.m4.coreinstance.SourceInformation;
import org.finos.legend.pure.m4.tools.GraphNodeIterable;
import org.junit.Assert;
import org.junit.BeforeClass;
import org.junit.Test;

public class TestUnresolvedImportStubs extends AbstractPureTestWithCoreCompiledPlatform
{
    @BeforeClass
    public static void setUp()
    {
        setUpRuntime(getExtra());
    }

    @Test
    public void testNoUnresolvedImportStubs()
    {
        CoreInstance importStubClass = runtime.getCoreInstance(M3Paths.ImportStub);
        CoreInstance propertyStubClass = runtime.getCoreInstance(M3Paths.PropertyStub);
        CoreInstance enumStubClass = runtime.getCoreInstance(M3Paths.EnumStub);
        MutableList<CoreInstance> unresolvedStubs = GraphNodeIterable.fromModelRepository(repository).select(node ->
        {
            CoreInstance classifier = node.getClassifier();
            if (classifier == importStubClass)
            {
                return node.getValueForMetaPropertyToOne(M3Properties.resolvedNode) == null;
            }
            if (classifier == propertyStubClass)
            {
                return node.getValueForMetaPropertyToOne(M3Properties.resolvedProperty) == null;
            }
            if (classifier == enumStubClass)
            {
                return node.getValueForMetaPropertyToOne(M3Properties.resolvedEnum) == null;
            }
            return false;
        }, Lists.mutable.empty());
        if (unresolvedStubs.notEmpty())
        {
            StringBuilder message = new StringBuilder("The following ").append(unresolvedStubs.size()).append(" stubs are unresolved:");
            unresolvedStubs.forEach(instance ->
            {
                CoreInstance classifier = instance.getClassifier();
                message.append("\n\t");
                if (classifier == importStubClass)
                {
                    PackageableElement.writeUserPathForPackageableElement(message.append("ImportStub: importGroup="), instance.getValueForMetaPropertyToOne(M3Properties.importGroup));
                    message.append(", idOrPath=").append(instance.getValueForMetaPropertyToOne(M3Properties.idOrPath).getName());
                }
                else if (classifier == propertyStubClass)
                {
                    message.append("PropertyStub: owner=").append(instance.getValueForMetaPropertyToOne(M3Properties.owner).getValueForMetaPropertyToOne(M3Properties.idOrPath).getName());
                    message.append(", propertyName=").append(instance.getValueForMetaPropertyToOne(M3Properties.propertyName).getName());
                }
                else
                {
                    message.append("EnumStub: enumeration=").append(instance.getValueForMetaPropertyToOne(M3Properties.enumeration).getValueForMetaPropertyToOne(M3Properties.idOrPath).getName());
                    message.append(", enumName=").append(instance.getValueForMetaPropertyToOne(M3Properties.enumName).getName());
                }
                SourceInformation sourceInfo = instance.getSourceInformation();
                if (sourceInfo != null)
                {
                    sourceInfo.appendMessage(message);
                }
            });
            Assert.fail(message.toString());
        }
    }
}
