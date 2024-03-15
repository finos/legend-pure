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

package org.finos.legend.pure.m3.navigation.generictype;

import org.eclipse.collections.api.list.ListIterable;
import org.eclipse.collections.api.list.MutableList;
import org.eclipse.collections.api.map.ImmutableMap;
import org.eclipse.collections.api.map.MapIterable;
import org.eclipse.collections.api.tuple.Pair;
import org.eclipse.collections.impl.factory.Lists;
import org.eclipse.collections.impl.factory.Maps;
import org.eclipse.collections.impl.list.mutable.FastList;
import org.finos.legend.pure.m3.navigation.M3Paths;
import org.finos.legend.pure.m3.navigation.M3Properties;
import org.finos.legend.pure.m3.navigation.Instance;
import org.finos.legend.pure.m3.navigation.multiplicity.Multiplicity;
import org.finos.legend.pure.m3.navigation.M3ProcessorSupport;
import org.finos.legend.pure.m3.navigation.ProcessorSupport;
import org.finos.legend.pure.m4.coreinstance.CoreInstance;

import java.io.IOException;

public class GenericTypeWithXArguments
{
    private final CoreInstance genericType;
    private final ImmutableMap<String, CoreInstance> argumentsByParameterName;

    public GenericTypeWithXArguments(CoreInstance genericType, ImmutableMap<String, CoreInstance> argumentsByParameterName)
    {
        this.genericType = genericType;
        this.argumentsByParameterName = (argumentsByParameterName == null) ? Maps.immutable.<String, CoreInstance>empty() : argumentsByParameterName;
    }

    public CoreInstance getGenericType()
    {
        return this.genericType;
    }

    public ImmutableMap<String, CoreInstance> getArgumentsByParameterName()
    {
        return this.argumentsByParameterName;
    }

    public CoreInstance getArgumentByParameterName(String parameterName)
    {
        return this.argumentsByParameterName.get(parameterName);
    }

    public ListIterable<CoreInstance> extractArgumentsAsTypeParameters(ProcessorSupport processorSupport)
    {
        CoreInstance rawType = Instance.getValueForMetaPropertyToOneResolved(this.genericType, M3Properties.rawType, processorSupport);
        ListIterable<? extends CoreInstance> typeParams = rawType.getValueForMetaPropertyToMany(M3Properties.typeParameters);
        if (typeParams.isEmpty())
        {
            return Lists.immutable.empty();
        }

        MutableList<CoreInstance> result = FastList.newList(typeParams.size());
        for (CoreInstance typeParam : typeParams)
        {
            result.add(this.argumentsByParameterName.get(typeParam.getValueForMetaPropertyToOne(M3Properties.name).getName()));
        }
        return result;
    }

    public ListIterable<CoreInstance> extractArgumentsAsMultiplicityParameters(ProcessorSupport processorSupport)
    {
        CoreInstance rawType = Instance.getValueForMetaPropertyToOneResolved(this.genericType, M3Properties.rawType, processorSupport);
        ListIterable<? extends CoreInstance> multParams = rawType.getValueForMetaPropertyToMany(M3Properties.multiplicityParameters);
        if (multParams.isEmpty())
        {
            return Lists.immutable.empty();
        }

        MapIterable<String, CoreInstance> multArgumentsByName = this.argumentsByParameterName;
        MutableList<CoreInstance> result = FastList.newList(multParams.size());
        for (CoreInstance multParam : multParams)
        {
            result.add(multArgumentsByName.get(multParam.getValueForMetaPropertyToOne(M3Properties.values).getName()));
        }
        return result;
    }

    public void print(Appendable appendable, ProcessorSupport processorSupport)
    {
        try
        {
            GenericType.print(appendable, this.genericType, processorSupport);
            appendable.append("  /  {");
            boolean first = true;
            for (Pair<String, CoreInstance> varValue : this.argumentsByParameterName.keyValuesView())
            {
                if (first)
                {
                    first = false;
                }
                else
                {
                    appendable.append(", ");
                }
                String var = varValue.getOne();
                CoreInstance value = varValue.getTwo();
                appendable.append(var);
                appendable.append(" = ");
                if (value == null)
                {
                    appendable.append("null");
                }
                else if (Instance.instanceOf(value, M3Paths.GenericType, processorSupport))
                {
                    GenericType.print(appendable, value, processorSupport);
                }
                else if (Instance.instanceOf(value, M3Paths.Multiplicity, processorSupport))
                {
                    Multiplicity.print(appendable, value, true);
                }
                else
                {
                    appendable.append(value.toString());
                }
            }
            appendable.append('}');
        }
        catch (IOException e)
        {
            throw new RuntimeException(e);
        }
    }

    @Override
    public String toString()
    {
        StringBuilder builder = new StringBuilder(64);
        print(builder, new M3ProcessorSupport(this.genericType.getRepository()));
        return builder.toString();
    }
}
