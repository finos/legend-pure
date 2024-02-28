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

package org.finos.legend.pure.m3.navigation.linearization;

import org.eclipse.collections.api.block.function.Function;
import org.eclipse.collections.api.list.ImmutableList;
import org.eclipse.collections.api.list.ListIterable;
import org.eclipse.collections.api.list.MutableList;
import org.eclipse.collections.api.map.MapIterable;
import org.eclipse.collections.api.map.MutableMap;
import org.eclipse.collections.impl.factory.Maps;
import org.eclipse.collections.impl.list.mutable.FastList;
import org.eclipse.collections.impl.map.mutable.UnifiedMap;
import org.finos.legend.pure.m3.navigation.M3Properties;
import org.finos.legend.pure.m3.navigation.Instance;
import org.finos.legend.pure.m3.navigation._class._Class;
import org.finos.legend.pure.m3.navigation.generictype.GenericType;
import org.finos.legend.pure.m3.navigation.ProcessorSupport;
import org.finos.legend.pure.m3.navigation.PrimitiveUtilities;
import org.finos.legend.pure.m4.coreinstance.CoreInstance;

class GenericTypeTypeSupport implements TypeSupport
{
    public static final GenericTypeTypeSupport INSTANCE = new GenericTypeTypeSupport();

    private GenericTypeTypeSupport()
    {
    }

    @Override
    public ListIterable<CoreInstance> getDirectGeneralizations(CoreInstance genericType, ProcessorSupport processorSupport)
    {
        CoreInstance rawType = Instance.getValueForMetaPropertyToOneResolved(genericType, M3Properties.rawType, processorSupport);
        MapIterable<String, CoreInstance> typeArgumentsByParameter = getTypeArgumentsByParameter(genericType, rawType, processorSupport);
        MapIterable<String, CoreInstance> multiplicityArgumentsByParameter = getMultiplicityArgumentsByParameter(genericType, rawType, processorSupport);

        ListIterable<? extends CoreInstance> rawGeneralizations = Instance.getValueForMetaPropertyToManyResolved(rawType, M3Properties.generalizations, processorSupport);
        MutableList<CoreInstance> directGeneralizations = FastList.newList(rawGeneralizations.size());
        for (CoreInstance rawGeneralization : rawGeneralizations)
        {
            CoreInstance general = Instance.getValueForMetaPropertyToOneResolved(rawGeneralization, M3Properties.general, processorSupport);
            if (typeArgumentsByParameter.notEmpty() || multiplicityArgumentsByParameter.notEmpty())
            {
                general = GenericType.makeTypeArgumentAsConcreteAsPossible(general, typeArgumentsByParameter, multiplicityArgumentsByParameter, processorSupport);
            }
            directGeneralizations.add(general);
        }
        return directGeneralizations;
    }

    private MapIterable<String, CoreInstance> getTypeArgumentsByParameter(CoreInstance genericType, CoreInstance rawType, ProcessorSupport processorSupport)
    {
        ListIterable<? extends CoreInstance> typeArguments = Instance.getValueForMetaPropertyToManyResolved(genericType, M3Properties.typeArguments, processorSupport);
        int size = typeArguments.size();
        if (size == 0)
        {
            return Maps.immutable.empty();
        }

        ListIterable<? extends CoreInstance> typeParameters = Instance.getValueForMetaPropertyToManyResolved(rawType, M3Properties.typeParameters, processorSupport);
        if (size > typeParameters.size())
        {
            StringBuilder message = new StringBuilder("Type argument mismatch for ");
            _Class.print(message, rawType);
            message.append("; got: ");
            GenericType.print(message, genericType, processorSupport);
            throw new RuntimeException(message.toString());
        }

        MutableMap<String, CoreInstance> argumentsByParameter = UnifiedMap.newMap(size);
        for (int i = 0; i < size; i++)
        {
            CoreInstance typeParameter = typeParameters.get(i);
            CoreInstance typeArgument = typeArguments.get(i);
            argumentsByParameter.put(PrimitiveUtilities.getStringValue(typeParameter.getValueForMetaPropertyToOne(M3Properties.name)), typeArgument);
        }
        return argumentsByParameter;
    }

    private MapIterable<String, CoreInstance> getMultiplicityArgumentsByParameter(CoreInstance genericType, CoreInstance rawType, ProcessorSupport processorSupport)
    {
        ListIterable<? extends CoreInstance> multiplicityArguments = Instance.getValueForMetaPropertyToManyResolved(genericType, M3Properties.multiplicityArguments, processorSupport);
        int size = multiplicityArguments.size();
        if (size == 0)
        {
            return Maps.immutable.empty();
        }

        ListIterable<? extends CoreInstance> multiplicityParameters = Instance.getValueForMetaPropertyToManyResolved(rawType, M3Properties.multiplicityParameters, processorSupport);
        if (size > multiplicityParameters.size())
        {
            StringBuilder message = new StringBuilder("Multiplicity argument mismatch for ");
            _Class.print(message, rawType);
            message.append("; got: ");
            GenericType.print(message, genericType, processorSupport);
            throw new RuntimeException(message.toString());
        }

        MutableMap<String, CoreInstance> argumentsByParameter = UnifiedMap.newMap(size);
        for (int i = 0; i < size; i++)
        {
            CoreInstance multiplicityParameter = multiplicityParameters.get(i);
            CoreInstance typeArgument = multiplicityArguments.get(i);
            argumentsByParameter.put(PrimitiveUtilities.getStringValue(multiplicityParameter.getValueForMetaPropertyToOne(M3Properties.values)), typeArgument);
        }
        return argumentsByParameter;
    }

    @Override
    public ImmutableList<CoreInstance> getGeneralizations(CoreInstance type, Function<? super CoreInstance, ? extends ImmutableList<CoreInstance>> generator, ProcessorSupport processorSupport)
    {
        return generator.valueOf(type);
    }

    @Override
    public boolean check_typeEquality(CoreInstance type1, CoreInstance type2, ProcessorSupport processorSupport)
    {
        return GenericType.genericTypesEqual(type1, type2, processorSupport);
    }
}
