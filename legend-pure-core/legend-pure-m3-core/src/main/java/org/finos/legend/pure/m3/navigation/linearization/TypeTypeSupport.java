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
import org.finos.legend.pure.m3.navigation.ProcessorSupport;
import org.finos.legend.pure.m3.navigation.type.Type;
import org.finos.legend.pure.m4.coreinstance.CoreInstance;

class TypeTypeSupport implements TypeSupport
{
    public static final TypeTypeSupport INSTANCE = new TypeTypeSupport();

    private TypeTypeSupport()
    {
    }

    @Override
    public ListIterable<CoreInstance> getDirectGeneralizations(CoreInstance type, ProcessorSupport processorSupport)
    {
        return Type.getDirectGeneralizations(type, processorSupport);
    }

    @Override
    public ImmutableList<CoreInstance> getGeneralizations(CoreInstance type, Function<? super CoreInstance, ? extends ImmutableList<CoreInstance>> generator, ProcessorSupport processorSupport)
    {
        return processorSupport.type_getTypeGeneralizations(type, generator);
    }

    @Override
    public boolean check_typeEquality(CoreInstance type1, CoreInstance type2, ProcessorSupport processorSupport)
    {
        return type1 == type2;
    }
}
