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

package org.finos.legend.pure.m3.compiler.validation.validator;

import org.finos.legend.pure.m3.navigation.M3Paths;
import org.finos.legend.pure.m3.compiler.Context;
import org.finos.legend.pure.m3.compiler.PropertyOwnerStrategy;
import org.finos.legend.pure.m3.navigation.PackageableElement.PackageableElement;
import org.finos.legend.pure.m3.coreinstance.meta.pure.metamodel.PropertyOwner;
import org.finos.legend.pure.m3.coreinstance.meta.pure.metamodel.function.property.AbstractProperty;
import org.finos.legend.pure.m3.tools.matcher.MatchRunner;
import org.finos.legend.pure.m3.tools.matcher.Matcher;
import org.finos.legend.pure.m3.tools.matcher.MatcherState;
import org.finos.legend.pure.m4.ModelRepository;
import org.finos.legend.pure.m4.exception.PureCompilationException;

public class AbstractPropertyValidator implements MatchRunner<AbstractProperty<?>>
{
    @Override
    public String getClassName()
    {
        return M3Paths.AbstractProperty;
    }

    @Override
    public void run(AbstractProperty<?> abstractProperty, MatcherState state, Matcher matcher, ModelRepository modelRepository, final Context context) throws PureCompilationException
    {
        PropertyOwner owner = abstractProperty._owner();
        if (owner == null)
        {
            throw new PureCompilationException(abstractProperty.getSourceInformation(), "Property '" + abstractProperty._name() + "' has no owner");
        }

        PropertyOwnerStrategy propertyOwnerStrategy = PropertyOwnerStrategy.getPropertyOwnerStrategy(owner);
        if (!(propertyOwnerStrategy.properties(owner).contains(abstractProperty) || propertyOwnerStrategy.qualifiedProperties(owner).contains(abstractProperty)))
        {
            throw new PureCompilationException(abstractProperty.getSourceInformation(), "Property '" + abstractProperty._name() + "' not present as a property or qualified property of its owner " + PackageableElement.getUserPathForPackageableElement(owner, "::"));
        }
    }
}
