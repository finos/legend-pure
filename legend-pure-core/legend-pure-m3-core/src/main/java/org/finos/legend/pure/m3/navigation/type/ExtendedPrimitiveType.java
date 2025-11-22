// Copyright 2024 Goldman Sachs
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

package org.finos.legend.pure.m3.navigation.type;

import org.eclipse.collections.api.list.ListIterable;
import org.finos.legend.pure.m3.coreinstance.meta.pure.metamodel.valuespecification.VariableExpression;
import org.finos.legend.pure.m3.navigation.Instance;
import org.finos.legend.pure.m3.navigation.M3Properties;
import org.finos.legend.pure.m3.navigation.PackageableElement.PackageableElement;
import org.finos.legend.pure.m3.navigation.PrimitiveUtilities;
import org.finos.legend.pure.m3.navigation.ProcessorSupport;
import org.finos.legend.pure.m3.navigation.generictype.GenericType;
import org.finos.legend.pure.m3.navigation.multiplicity.Multiplicity;
import org.finos.legend.pure.m4.coreinstance.CoreInstance;
import org.finos.legend.pure.m4.tools.SafeAppendable;

public class ExtendedPrimitiveType
{
    public static boolean testTypeVariableValuesEquals(CoreInstance genericType1, CoreInstance genericType2, ProcessorSupport processorSupport)
    {
        ListIterable<? extends CoreInstance> typeVariableValues1 = genericType1.getValueForMetaPropertyToMany(M3Properties.typeVariableValues);
        ListIterable<? extends CoreInstance> typeVariableValues2 = genericType2.getValueForMetaPropertyToMany(M3Properties.typeVariableValues);
        return typeVariableValues1.zip(typeVariableValues2).allSatisfy(x -> x.getOne().getValueForMetaPropertyToMany(M3Properties.values).equals(x.getTwo().getValueForMetaPropertyToMany(M3Properties.values)));
    }

    public static boolean testTypeVariableValuesCompatible(CoreInstance genericType1, CoreInstance genericType2, ProcessorSupport processorSupport)
    {
        ListIterable<? extends CoreInstance> typeVariableValues1 = genericType1.getValueForMetaPropertyToMany(M3Properties.typeVariableValues);
        ListIterable<? extends CoreInstance> typeVariableValues2 = genericType2.getValueForMetaPropertyToMany(M3Properties.typeVariableValues);
        return typeVariableValues1.zip(typeVariableValues2).allSatisfy(x -> x.getOne() instanceof VariableExpression || x.getOne().getValueForMetaPropertyToMany(M3Properties.values).equals(x.getTwo().getValueForMetaPropertyToMany(M3Properties.values)));
    }

    public static String print(CoreInstance extendedPrimitiveType, boolean fullPaths, ProcessorSupport processorSupport)
    {
        return print(new StringBuilder(), extendedPrimitiveType, fullPaths, processorSupport).toString();
    }

    public static <T extends Appendable> T print(T appendable, CoreInstance primitiveType, boolean fullPaths, ProcessorSupport processorSupport)
    {
        SafeAppendable safeAppendable = SafeAppendable.wrap(appendable);
        if (fullPaths)
        {
            PackageableElement.writeUserPathForPackageableElement(safeAppendable, primitiveType);
        }
        else
        {
            safeAppendable.append(primitiveType.getName());
        }
        ListIterable<? extends CoreInstance> typeVariables = primitiveType.getValueForMetaPropertyToMany(M3Properties.typeVariables);
        if (typeVariables.notEmpty())
        {
            safeAppendable.append('(');
            typeVariables.forEachWithIndex((v, i) ->
            {
                if (i > 0)
                {
                    safeAppendable.append(", ");
                }
                safeAppendable.append(PrimitiveUtilities.getStringValue(v.getValueForMetaPropertyToOne(M3Properties.name))).append(':');
                GenericType.print(safeAppendable, v.getValueForMetaPropertyToOne(M3Properties.genericType), fullPaths, processorSupport);
                Multiplicity.print(safeAppendable, Instance.getValueForMetaPropertyToOneResolved(v, M3Properties.multiplicity, processorSupport), true);
            });
            safeAppendable.append(')');
        }
        return appendable;
    }
}
