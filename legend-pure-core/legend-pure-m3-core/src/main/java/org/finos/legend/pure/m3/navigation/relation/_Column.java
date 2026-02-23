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

import org.eclipse.collections.api.RichIterable;
import org.eclipse.collections.api.factory.Lists;
import org.finos.legend.pure.m3.coreinstance.meta.pure.metamodel.extension.Stereotype;
import org.finos.legend.pure.m3.coreinstance.meta.pure.metamodel.extension.TaggedValue;
import org.finos.legend.pure.m3.coreinstance.meta.pure.metamodel.multiplicity.Multiplicity;
import org.finos.legend.pure.m3.coreinstance.meta.pure.metamodel.relation.Column;
import org.finos.legend.pure.m3.coreinstance.meta.pure.metamodel.type.Type;
import org.finos.legend.pure.m3.coreinstance.meta.pure.metamodel.type.generics.GenericType;
import org.finos.legend.pure.m3.navigation.M3Paths;
import org.finos.legend.pure.m3.navigation.M3PropertyPaths;
import org.finos.legend.pure.m3.navigation.ProcessorSupport;
import org.finos.legend.pure.m3.navigation._package._Package;
import org.finos.legend.pure.m3.tools.ListHelper;
import org.finos.legend.pure.m4.coreinstance.CoreInstance;
import org.finos.legend.pure.m4.coreinstance.SourceInformation;
import org.finos.legend.pure.m4.exception.PureCompilationException;
import org.finos.legend.pure.m4.serialization.grammar.StringEscape;
import org.finos.legend.pure.m4.tools.SafeAppendable;

public class _Column
{
    public static Column<?, ?> getColumnInstance(String name, boolean nameWildCard, String type, Multiplicity multiplicity, SourceInformation src, ProcessorSupport processorSupport)
    {
        return getColumnInstance(name, nameWildCard, type, multiplicity, null, false, null, src, processorSupport);
    }

    public static Column<?, ?> getColumnInstance(String name, boolean nameWildCard, GenericType targetType, Multiplicity multiplicity, SourceInformation sourceInformation, ProcessorSupport processorSupport)
    {
        return getColumnInstance(name, nameWildCard, targetType, multiplicity, null, false, null, sourceInformation, processorSupport);
    }

    public static Column<?, ?> getColumnInstance(String name, boolean nameWildCard, String type, Multiplicity multiplicity, RichIterable<? extends CoreInstance> stereotypes, boolean assumeNoImportStubs, RichIterable<? extends TaggedValue> taggedValues, SourceInformation src, ProcessorSupport processorSupport)
    {
        GenericType target = (GenericType) processorSupport.newAnonymousCoreInstance(src, M3Paths.GenericType);
        if (type == null)
        {
            target._rawType(null);
        }
        else
        {
            CoreInstance _type = _Package.getByUserPath(type, processorSupport);
            if (_type == null)
            {
                throw new PureCompilationException(src, type + " not found!  (imports are not scan for TDS column type resolution)");
            }
            target._rawType((Type)_type);
        }
        return _Column.getColumnInstance(name, nameWildCard, target, multiplicity, stereotypes, assumeNoImportStubs, taggedValues, src, processorSupport);
    }

    // Parameterize getColumnInstance
    // One version does exactly what is here
    // The other version will call ._stereotypes(stereotypes) and it's going to assume that it's not getting any import stubs
    //      i.e., make the stereotypes param a RichIterable<Stereotype> instead of RichIterable<CoreInstance>
    //      compiled version should assume it's not getting any import stubs
    // OR
    // Create 1 method and get a warning by casting to RichIterable<Stereotype>
    public static Column<?, ?> getColumnInstance(String name, boolean nameWildCard, GenericType targetType, Multiplicity multiplicity, RichIterable<? extends CoreInstance> stereotypes, boolean assumeNoImportStubs, RichIterable<? extends TaggedValue> taggedValues, SourceInformation sourceInformation, ProcessorSupport processorSupport)
    {
        Column<?, ?> columnInstance = (Column<?, ?>) processorSupport.newAnonymousCoreInstance(sourceInformation, M3Paths.Column);
        columnInstance._name(StringEscape.unescape(removeQuotes(name)));
        columnInstance._functionName("column:" + columnInstance._name());
        columnInstance._nameWildCard(nameWildCard);
        GenericType columnGenericType = (GenericType) processorSupport.newAnonymousCoreInstance(sourceInformation, M3Paths.GenericType);
        columnGenericType._rawType((Type) _Package.getByUserPath(M3Paths.Column, processorSupport));
        columnGenericType._typeArguments(Lists.mutable.with(null, targetType));
        columnGenericType._multiplicityArgumentsAdd(multiplicity);
        columnInstance.setKeyValues(M3PropertyPaths.classifierGenericType, Lists.mutable.with(columnGenericType));
        if (stereotypes != null && stereotypes.notEmpty())
        {
            if (assumeNoImportStubs)
            {
                columnInstance._stereotypes(ListHelper.wrapListIterable(stereotypes).selectInstancesOf(Stereotype.class));
            }
            else
            {
                columnInstance._stereotypesCoreInstance(stereotypes);
            }
        }
        if (taggedValues != null && taggedValues.notEmpty())
        {
            columnInstance._taggedValues(taggedValues);
        }
        return columnInstance;
    }

    private static String removeQuotes(String name)
    {
        name = name.trim();
        return name.startsWith("\"") ? name.substring(1, name.length() - 1) : name;
    }

    public static Column<?, ?> updateSource(Column<?, ?> col, GenericType sourceType)
    {
        col._classifierGenericType()._typeArguments(Lists.mutable.with(sourceType, getColumnType(col)));
        return col;
    }

    public static GenericType getColumnType(Column<?, ?> column)
    {
        return ListHelper.wrapListIterable(column._classifierGenericType()._typeArguments()).get(1);
    }

    public static Multiplicity getColumnMultiplicity(Column<?, ?> column)
    {
        return ListHelper.wrapListIterable(column._classifierGenericType()._multiplicityArguments()).get(0);
    }

    public static GenericType getColumnSourceType(Column<?, ?> column)
    {
        return ListHelper.wrapListIterable(column._classifierGenericType()._typeArguments()).get(0);
    }

    public static String print(CoreInstance column, ProcessorSupport processorSupport)
    {
        return print(new StringBuilder(), column, processorSupport).toString();
    }

    public static <T extends Appendable> T print(T appendable, CoreInstance col, ProcessorSupport processorSupport)
    {
        Column<?, ?> column = (Column<?, ?>) col;
        org.finos.legend.pure.m3.navigation.generictype.GenericType.print(SafeAppendable.wrap(appendable).append(column._nameWildCard() ? "?" : column._name()).append(':'), getColumnType(column), processorSupport);
        Multiplicity multiplicity = getColumnMultiplicity(column);
        if (multiplicity == null || !(multiplicity._lowerBound()._value() == 0 && multiplicity._upperBound()._value() == 1))
        {
            org.finos.legend.pure.m3.navigation.multiplicity.Multiplicity.print(appendable, getColumnMultiplicity(column), true);
        }
        return appendable;
    }
}
