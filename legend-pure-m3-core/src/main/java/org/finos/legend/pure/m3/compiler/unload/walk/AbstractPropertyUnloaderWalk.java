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

package org.finos.legend.pure.m3.compiler.unload.walk;

import org.eclipse.collections.api.list.MutableList;
import org.eclipse.collections.impl.list.mutable.FastList;
import org.finos.legend.pure.m3.navigation.M3Paths;
import org.finos.legend.pure.m3.compiler.Context;
import org.finos.legend.pure.m3.compiler.postprocessing.processor.milestoning.MilestoningFunctions;
import org.finos.legend.pure.m3.coreinstance.meta.pure.metamodel.PropertyOwner;
import org.finos.legend.pure.m3.coreinstance.meta.pure.metamodel.function.property.AbstractProperty;
import org.finos.legend.pure.m3.coreinstance.meta.pure.metamodel.relationship.Association;
import org.finos.legend.pure.m3.coreinstance.meta.pure.metamodel.relationship.AssociationProjection;
import org.finos.legend.pure.m3.coreinstance.meta.pure.metamodel.type.Class;
import org.finos.legend.pure.m3.coreinstance.meta.pure.metamodel.type.ClassProjection;
import org.finos.legend.pure.m3.tools.matcher.MatchRunner;
import org.finos.legend.pure.m3.tools.matcher.Matcher;
import org.finos.legend.pure.m3.tools.matcher.MatcherState;
import org.finos.legend.pure.m4.coreinstance.CoreInstance;
import org.finos.legend.pure.m4.ModelRepository;
import org.finos.legend.pure.m4.exception.PureCompilationException;

public class AbstractPropertyUnloaderWalk implements MatchRunner<AbstractProperty>
{
    @Override
    public String getClassName()
    {
        return M3Paths.AbstractProperty;
    }

    @Override
    public void run(AbstractProperty abstractProperty, MatcherState state, Matcher matcher, ModelRepository modelRepository, Context context) throws PureCompilationException
    {
        WalkerState walkerState = (WalkerState)state;
        PropertyOwner owner = abstractProperty._owner();
        if (owner != null)
        {
            walkerState.addInstance(owner);

            if (owner instanceof ClassProjection)
            {
                matcher.fullMatch(owner, state);
            }
            else if (owner instanceof Class)
            {
                Class cls = (Class)owner;
                MutableList<CoreInstance> propertiesProperties = FastList.newList();
                propertiesProperties.addAllIterable(cls._properties());
                propertiesProperties.addAllIterable(cls._propertiesFromAssociations());
                propertiesProperties.addAllIterable(cls._qualifiedProperties());
                propertiesProperties.addAllIterable(cls._qualifiedPropertiesFromAssociations());
                propertiesProperties.addAllIterable(cls._originalMilestonedProperties());

                MutableList<CoreInstance> toUnloadProperties = propertiesProperties.partition(new MilestoningFunctions.IsMilestonePropertyPredicate(state.getProcessorSupport())).getSelected();
                for (CoreInstance property : toUnloadProperties)
                {
                    matcher.fullMatch(property, state);
                }
            }
            else if (owner instanceof AssociationProjection)
            {
                matcher.fullMatch(owner, state);
            }
            else if (owner instanceof Association)
            {
                Association association = (Association)owner;
                MutableList<CoreInstance> propertiesProperties = FastList.newList();
                propertiesProperties.addAllIterable(association._properties());
                propertiesProperties.addAllIterable(association._qualifiedProperties());
                propertiesProperties.addAllIterable(association._originalMilestonedProperties());

                MutableList<CoreInstance> toUnloadProperties = propertiesProperties.partition(new MilestoningFunctions.IsMilestonePropertyPredicate(state.getProcessorSupport())).getSelected();
                for (CoreInstance property : toUnloadProperties)
                {
                    matcher.fullMatch(property, state);
                }
            }
        }
    }
}
