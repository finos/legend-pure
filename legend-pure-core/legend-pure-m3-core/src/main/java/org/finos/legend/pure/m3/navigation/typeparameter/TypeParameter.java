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

package org.finos.legend.pure.m3.navigation.typeparameter;

import org.finos.legend.pure.m3.navigation.M3Properties;
import org.finos.legend.pure.m3.navigation.Instance;
import org.finos.legend.pure.m3.navigation.PrimitiveUtilities;
import org.finos.legend.pure.m3.navigation.ProcessorSupport;
import org.finos.legend.pure.m4.coreinstance.CoreInstance;

public class TypeParameter
{
    public static boolean isCovariant(CoreInstance typeParameter)
    {
        return !PrimitiveUtilities.getBooleanValue(typeParameter.getValueForMetaPropertyToOne(M3Properties.contravariant), false);
    }

    public static CoreInstance wrapGenericType(CoreInstance typeParameter, ProcessorSupport processorSupport)
    {
        CoreInstance genericType = processorSupport.newGenericType(null, typeParameter, false);
        Instance.addValueToProperty(genericType, M3Properties.typeParameter, typeParameter, processorSupport);
        return genericType;
    }
}
