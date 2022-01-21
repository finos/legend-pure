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
import org.eclipse.collections.api.factory.Maps;
import org.eclipse.collections.api.map.MutableMap;
import org.eclipse.collections.impl.set.mutable.UnmodifiableMutableSet;
import org.finos.legend.pure.m4.coreinstance.CoreInstance;

class TypeInferenceContextState
{
    private final MutableMap<String, ParameterValueWithFlag> typeParameterToGenericType = Maps.mutable.empty();
    private final MutableMap<String, ParameterValueWithFlag> multiplicityParameterToMultiplicity = Maps.mutable.empty();
    private boolean ahead = false;
    private boolean aheadConsumed = false;

    RichIterable<String> getTypeParameters()
    {
        return UnmodifiableMutableSet.of(this.typeParameterToGenericType.keySet());
    }

    boolean hasTypeParameter(String parameter)
    {
        return this.typeParameterToGenericType.containsKey(parameter);
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

    RichIterable<String> getMultiplicityParameters()
    {
        return UnmodifiableMutableSet.of(this.multiplicityParameterToMultiplicity.keySet());
    }

    boolean hasMultiplicityParameter(String parameter)
    {
        return this.multiplicityParameterToMultiplicity.containsKey(parameter);
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
