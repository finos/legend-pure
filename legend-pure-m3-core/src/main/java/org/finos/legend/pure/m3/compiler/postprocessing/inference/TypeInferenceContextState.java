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

package org.finos.legend.pure.m3.compiler.postprocessing.inference;

import org.eclipse.collections.api.RichIterable;
import org.eclipse.collections.api.block.function.Function2;
import org.eclipse.collections.api.map.MapIterable;
import org.eclipse.collections.api.map.MutableMap;
import org.eclipse.collections.api.tuple.Pair;
import org.eclipse.collections.impl.map.mutable.UnifiedMap;
import org.eclipse.collections.impl.tuple.Tuples;
import org.finos.legend.pure.m4.coreinstance.CoreInstance;

class TypeInferenceContextState
{
    private static final Function2<String, ParameterValueWithFlag, Pair<String, CoreInstance>> UNBOX = new Function2<String, ParameterValueWithFlag, Pair<String, CoreInstance>>()
    {
        @Override
        public Pair<String, CoreInstance> value(String parameter, ParameterValueWithFlag valueWithFlag)
        {
            return Tuples.pair(parameter, valueWithFlag.getParameterValue());
        }
    };

    private final MutableMap<String, ParameterValueWithFlag> typeParameterToGenericType = UnifiedMap.newMap();
    private final MutableMap<String, ParameterValueWithFlag> multiplicityParameterToMultiplicity = UnifiedMap.newMap();
    private boolean ahead = false;
    private boolean aheadConsumed = false;

    public RichIterable<String> getTypeParameters()
    {
        return this.typeParameterToGenericType.keysView();
    }

    ParameterValueWithFlag getTypeParameterValueWithFlag(String parameter)
    {
        return this.typeParameterToGenericType.get(parameter);
    }

    CoreInstance getTypeParameterValue(String parameter)
    {
        ParameterValueWithFlag genericTypeWithFlag = this.typeParameterToGenericType.get(parameter);
        return (genericTypeWithFlag == null) ? null : genericTypeWithFlag.getParameterValue();
    }

    void putTypeParameterValue(String parameter, CoreInstance genericType, TypeInferenceContext targetGenericsContext, boolean isFinal)
    {
        this.typeParameterToGenericType.put(parameter, new ParameterValueWithFlag(genericType, targetGenericsContext, isFinal));
    }

    MapIterable<String, CoreInstance> getGenericTypeByParameter()
    {
        return this.typeParameterToGenericType.collect(UNBOX);
    }

    public RichIterable<String> getMultiplicityParameters()
    {
        return this.multiplicityParameterToMultiplicity.keysView();
    }

    ParameterValueWithFlag getMultiplicityParameterValueWithFlag(String parameter)
    {
        return this.multiplicityParameterToMultiplicity.get(parameter);
    }

    public CoreInstance getMultiplicityParameterValue(String parameter)
    {
        ParameterValueWithFlag multiplicityWithFlag = this.multiplicityParameterToMultiplicity.get(parameter);
        return (multiplicityWithFlag == null) ? null : multiplicityWithFlag.getParameterValue();
    }

    void putMultiplicityParameterValue(String parameter, CoreInstance multiplicity, TypeInferenceContext targetGenericsContext, boolean isFinal)
    {
        this.multiplicityParameterToMultiplicity.put(parameter, new ParameterValueWithFlag(multiplicity, targetGenericsContext, isFinal));
    }

    MapIterable<String, CoreInstance> getMultiplicityByParameter()
    {
        return this.multiplicityParameterToMultiplicity.collect(UNBOX);
    }

    boolean isAhead()
    {
        return this.ahead;
    }

    void setAhead()
    {
        this.ahead = true;
    }

    boolean isAheadConsumed()
    {
        return this.aheadConsumed;
    }

    void setAheadConsumed()
    {
        this.aheadConsumed = true;
    }

    public TypeInferenceContextState copy()
    {
        TypeInferenceContextState copy = new TypeInferenceContextState();
        copy.typeParameterToGenericType.putAll(this.typeParameterToGenericType);
        copy.multiplicityParameterToMultiplicity.putAll(this.multiplicityParameterToMultiplicity);
        copy.ahead = this.ahead;
        copy.aheadConsumed = this.aheadConsumed;
        return copy;
    }
}
