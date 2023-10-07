// Copyright 2023 Goldman Sachs
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

package org.finos.legend.pure.m3.navigation.relation;

import org.eclipse.collections.api.factory.Lists;
import org.finos.legend.pure.m3.coreinstance.meta.pure.metamodel.relation.Column;
import org.finos.legend.pure.m3.coreinstance.meta.pure.metamodel.type.Type;
import org.finos.legend.pure.m3.coreinstance.meta.pure.metamodel.type.generics.GenericType;
import org.finos.legend.pure.m3.navigation.M3Paths;
import org.finos.legend.pure.m3.navigation.ProcessorSupport;
import org.finos.legend.pure.m3.navigation._package._Package;
import org.finos.legend.pure.m4.coreinstance.CoreInstance;
import org.finos.legend.pure.m4.coreinstance.SourceInformation;

public class _Column
{
    public static Column<?, ?> getColumnInstance(String name, boolean nameWildCard, GenericType sourceType, String type, SourceInformation src, ProcessorSupport processorSupport)
    {
        GenericType target = (GenericType) processorSupport.newEphemeralAnonymousCoreInstance(M3Paths.GenericType);
        target._rawType(type == null ? null : (Type) _Package.getByUserPath(type, processorSupport));
        return _Column.getColumnInstance(name, nameWildCard, sourceType, target, src, processorSupport);
    }

    public static Column<?, ?> getColumnInstance(String name, boolean nameWildCard, GenericType sourceType, GenericType targetType, SourceInformation src, ProcessorSupport processorSupport)
    {
        Column<?, ?> columnInstance = (Column<?, ?>) processorSupport.newAnonymousCoreInstance(src, M3Paths.Column);
        columnInstance._name(name);
        columnInstance._nameWildCard(nameWildCard);
        GenericType columnGenericType = (GenericType) processorSupport.newAnonymousCoreInstance(src, M3Paths.GenericType);
        columnGenericType._rawType((Type) _Package.getByUserPath(M3Paths.Column, processorSupport));
        columnGenericType._typeArguments(Lists.mutable.with(sourceType, targetType));
        columnGenericType._multiplicityArgumentsAdd((org.finos.legend.pure.m3.coreinstance.meta.pure.metamodel.multiplicity.Multiplicity) org.finos.legend.pure.m3.navigation.multiplicity.Multiplicity.newMultiplicity(0, 1, processorSupport));
        columnInstance.setKeyValues(Lists.mutable.with("classifierGenericType"), Lists.mutable.with(columnGenericType));
        return columnInstance;
    }

    public static GenericType getColumnType(Column<?, ?> column)
    {
        return column._classifierGenericType()._typeArguments().toList().get(1);
    }

    public static String print(CoreInstance c, ProcessorSupport processorSupport)
    {
        Column<?, ?> col = (Column<?, ?>) c;
        return (col._nameWildCard() ? "?" : col._name()) + ":" + org.finos.legend.pure.m3.navigation.generictype.GenericType.print(getColumnType(col), processorSupport);
    }
}
