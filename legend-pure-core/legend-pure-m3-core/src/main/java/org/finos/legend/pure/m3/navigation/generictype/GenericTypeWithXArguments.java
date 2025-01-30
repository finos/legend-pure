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

import org.eclipse.collections.api.factory.Lists;
import org.eclipse.collections.api.factory.Maps;
import org.eclipse.collections.api.list.ListIterable;
import org.eclipse.collections.api.map.ImmutableMap;
import org.finos.legend.pure.m3.navigation.Instance;
import org.finos.legend.pure.m3.navigation.M3Paths;
import org.finos.legend.pure.m3.navigation.M3ProcessorSupport;
import org.finos.legend.pure.m3.navigation.M3Properties;
import org.finos.legend.pure.m3.navigation.PrimitiveUtilities;
import org.finos.legend.pure.m3.navigation.ProcessorSupport;
import org.finos.legend.pure.m3.navigation.multiplicity.Multiplicity;
import org.finos.legend.pure.m4.coreinstance.CoreInstance;
import org.finos.legend.pure.m4.tools.SafeAppendable;

public class GenericTypeWithXArguments
{
    private final CoreInstance genericType;
    private final ImmutableMap<String, CoreInstance> argumentsByParameterName;

    public GenericTypeWithXArguments(CoreInstance genericType, ImmutableMap<String, CoreInstance> argumentsByParameterName)
    {
        this.genericType = genericType;
        this.argumentsByParameterName = (argumentsByParameterName == null) ? Maps.immutable.empty() : argumentsByParameterName;
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
        return typeParams.isEmpty() ?
               Lists.immutable.empty() :
               typeParams.collect(tp -> this.argumentsByParameterName.get(PrimitiveUtilities.getStringValue(tp.getValueForMetaPropertyToOne(M3Properties.name))));
    }

    public ListIterable<CoreInstance> extractArgumentsAsMultiplicityParameters(ProcessorSupport processorSupport)
    {
        CoreInstance rawType = Instance.getValueForMetaPropertyToOneResolved(this.genericType, M3Properties.rawType, processorSupport);
        ListIterable<? extends CoreInstance> multParams = rawType.getValueForMetaPropertyToMany(M3Properties.multiplicityParameters);
        return multParams.isEmpty() ?
               Lists.immutable.empty() :
               multParams.collect(mp -> this.argumentsByParameterName.get(PrimitiveUtilities.getStringValue(mp.getValueForMetaPropertyToOne(M3Properties.values))));
    }

    public <T extends Appendable> T print(T appendable, ProcessorSupport processorSupport)
    {
        print(SafeAppendable.wrap(appendable), processorSupport);
        return appendable;
    }

    private void print(SafeAppendable appendable, ProcessorSupport processorSupport)
    {
        GenericType.print(appendable, this.genericType, processorSupport).append("  /  {");
        boolean[] first = {true};
        this.argumentsByParameterName.forEachKeyValue((var, value) ->
        {
            if (first[0])
            {
                first[0] = false;
            }
            else
            {
                appendable.append(", ");
            }
            appendable.append(var).append(" = ");
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
        });
        appendable.append('}');
    }

    @Override
    public String toString()
    {
        return print(new StringBuilder(64), new M3ProcessorSupport(this.genericType.getRepository())).toString();
    }
}
