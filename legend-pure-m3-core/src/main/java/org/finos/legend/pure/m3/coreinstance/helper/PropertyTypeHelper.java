// Copyright 2022 Goldman Sachs
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

package org.finos.legend.pure.m3.coreinstance.helper;

import org.eclipse.collections.impl.factory.Maps;
import org.finos.legend.pure.m3.navigation.Instance;
import org.finos.legend.pure.m3.navigation.M3Properties;
import org.finos.legend.pure.m3.navigation.ProcessorSupport;
import org.finos.legend.pure.m3.navigation.generictype.GenericType;
import org.finos.legend.pure.m3.navigation.generictype.GenericTypeWithXArguments;
import org.finos.legend.pure.m4.coreinstance.CoreInstance;

public final class PropertyTypeHelper
{
    private PropertyTypeHelper()
    {

    }

    public static CoreInstance getPropertyResolvedReturnType(CoreInstance classGenericType, CoreInstance property, ProcessorSupport processorSupport)
    {
        CoreInstance functionType = processorSupport.function_getFunctionType(property);
        CoreInstance returnType = Instance.getValueForMetaPropertyToOneResolved(functionType, M3Properties.returnType, processorSupport);
        CoreInstance propertyOwner = functionType.getValueForMetaPropertyToMany(M3Properties.parameters).getFirst().getValueForMetaPropertyToOne(M3Properties.genericType);
        if (!GenericType.isGenericTypeFullyConcrete(returnType, true, processorSupport)
                && Instance.getValueForMetaPropertyToOneResolved(classGenericType, M3Properties.rawType, processorSupport) != Instance.getValueForMetaPropertyToOneResolved(propertyOwner, M3Properties.rawType, processorSupport))
        {
            GenericTypeWithXArguments res = GenericType.resolveClassTypeParameterUsingInheritance(classGenericType, propertyOwner, processorSupport);
            returnType = GenericType.makeTypeArgumentAsConcreteAsPossible(returnType, res.getArgumentsByParameterName(), Maps.immutable.empty(), processorSupport);
        }
        return returnType;
    }
}
