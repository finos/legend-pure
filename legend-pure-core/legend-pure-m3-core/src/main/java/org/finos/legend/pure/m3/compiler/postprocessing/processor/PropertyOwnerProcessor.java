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

package org.finos.legend.pure.m3.compiler.postprocessing.processor;

import org.eclipse.collections.api.list.MutableList;
import org.eclipse.collections.impl.list.mutable.FastList;
import org.finos.legend.pure.m3.navigation.M3Paths;
import org.finos.legend.pure.m3.compiler.Context;
import org.finos.legend.pure.m3.coreinstance.meta.pure.metamodel.PropertyOwner;
import org.finos.legend.pure.m3.coreinstance.meta.pure.metamodel.function.property.AbstractProperty;
import org.finos.legend.pure.m3.coreinstance.meta.pure.metamodel.relationship.Association;
import org.finos.legend.pure.m3.coreinstance.meta.pure.metamodel.type.Class;
import org.finos.legend.pure.m3.tools.matcher.MatchRunner;
import org.finos.legend.pure.m3.tools.matcher.Matcher;
import org.finos.legend.pure.m3.tools.matcher.MatcherState;
import org.finos.legend.pure.m4.ModelRepository;
import org.finos.legend.pure.m4.exception.PureCompilationException;

public class PropertyOwnerProcessor implements MatchRunner<PropertyOwner>
{
    @Override
    public String getClassName()
    {
        return M3Paths.PropertyOwner;
    }

    @Override
    public void run(PropertyOwner propertyOwner, MatcherState state, Matcher matcher, ModelRepository modelRepository, Context context) throws PureCompilationException
    {
        MutableList<AbstractProperty> propertiesProperties = FastList.newList();

        if (propertyOwner instanceof Class)
        {
            propertiesProperties.addAllIterable(((Class)propertyOwner)._properties());
            propertiesProperties.addAllIterable(((Class)propertyOwner)._qualifiedProperties());
            propertiesProperties.addAllIterable(((Class)propertyOwner)._originalMilestonedProperties());
        }
        else if (propertyOwner instanceof Association)
        {
            propertiesProperties.addAllIterable(((Association)propertyOwner)._properties());
            propertiesProperties.addAllIterable(((Association)propertyOwner)._qualifiedProperties());
            propertiesProperties.addAllIterable(((Association)propertyOwner)._originalMilestonedProperties());
        }

        for (AbstractProperty property : propertiesProperties)
        {
            property._owner(propertyOwner);
        }
    }
}
