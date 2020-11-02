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

import org.finos.legend.pure.m3.navigation.M3Paths;
import org.finos.legend.pure.m3.navigation.M3Properties;
import org.eclipse.collections.api.block.predicate.Predicate;
import org.eclipse.collections.impl.utility.Iterate;
import org.finos.legend.pure.m3.navigation.PackageableElement.PackageableElement;
import org.finos.legend.pure.m4.tools.GraphNodeIterable;
import org.finos.legend.pure.m4.coreinstance.CoreInstance;
import org.finos.legend.pure.m4.coreinstance.SourceInformation;
import org.junit.Assert;
import org.junit.BeforeClass;
import org.junit.Test;

import java.util.Collection;

public class TestUnresolvedImportStubs extends AbstractPureTestWithCoreCompiledPlatform
{
    @BeforeClass
    public static void setUp() {
        setUpRuntime(getExtra());
    }

    @Test
    public void testNoUnresolvedImportStubs()
    {
        final CoreInstance importStubClass = this.runtime.getCoreInstance(M3Paths.ImportStub);
        final CoreInstance propertyStubClass = this.runtime.getCoreInstance(M3Paths.PropertyStub);
        final CoreInstance enumStubClass = this.runtime.getCoreInstance(M3Paths.EnumStub);
        Collection<CoreInstance> unresolvedStubs = Iterate.select(GraphNodeIterable.fromModelRepository(this.repository), new Predicate<CoreInstance>()
        {
            @Override
            public boolean accept(CoreInstance node)
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
            }
        });
        if (!unresolvedStubs.isEmpty())
        {
            StringBuilder message = new StringBuilder("The following ").append(unresolvedStubs.size()).append(" stubs are unresolved:");
            for (CoreInstance instance : unresolvedStubs)
            {
                CoreInstance classifier = instance.getClassifier();
                message.append("\n\t");
                if (classifier == importStubClass)
                {
                    message.append("ImportStub: importGroup=");
                    PackageableElement.writeUserPathForPackageableElement(message, instance.getValueForMetaPropertyToOne(M3Properties.importGroup));
                    message.append(", idOrPath=");
                    message.append(instance.getValueForMetaPropertyToOne(M3Properties.idOrPath).getName());
                }
                else if (classifier == propertyStubClass)
                {
                    message.append("PropertyStub: owner=");
                    message.append(instance.getValueForMetaPropertyToOne(M3Properties.owner).getValueForMetaPropertyToOne(M3Properties.idOrPath).getName());
                    message.append(", propertyName=");
                    message.append(instance.getValueForMetaPropertyToOne(M3Properties.propertyName).getName());
                }
                else
                {
                    message.append("EnumStub: enumeration=");
                    message.append(instance.getValueForMetaPropertyToOne(M3Properties.enumeration).getValueForMetaPropertyToOne(M3Properties.idOrPath).getName());
                    message.append(", enumName=");
                    message.append(instance.getValueForMetaPropertyToOne(M3Properties.enumName).getName());
                }
                SourceInformation sourceInfo = instance.getSourceInformation();
                if (sourceInfo != null)
                {
                    sourceInfo.writeMessage(message);
                }
            }
            Assert.fail(message.toString());
        }
    }
}
