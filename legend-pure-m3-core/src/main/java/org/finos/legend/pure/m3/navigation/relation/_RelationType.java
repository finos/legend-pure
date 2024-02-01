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
import org.eclipse.collections.api.bag.Bag;
import org.eclipse.collections.api.factory.Lists;
import org.eclipse.collections.api.factory.Sets;
import org.eclipse.collections.api.list.ListIterable;
import org.eclipse.collections.api.list.MutableList;
import org.eclipse.collections.api.set.MutableSet;
import org.eclipse.collections.api.tuple.Pair;
import org.eclipse.collections.impl.tuple.Tuples;
import org.finos.legend.pure.m3.coreinstance.meta.pure.metamodel.function.Function;
import org.finos.legend.pure.m3.coreinstance.meta.pure.metamodel.function.FunctionAccessor;
import org.finos.legend.pure.m3.coreinstance.meta.pure.metamodel.relation.Column;
import org.finos.legend.pure.m3.coreinstance.meta.pure.metamodel.relation.RelationType;
import org.finos.legend.pure.m3.coreinstance.meta.pure.metamodel.relationship.Generalization;
import org.finos.legend.pure.m3.coreinstance.meta.pure.metamodel.type.Class;
import org.finos.legend.pure.m3.coreinstance.meta.pure.metamodel.type.Type;
import org.finos.legend.pure.m3.coreinstance.meta.pure.metamodel.type.generics.GenericType;
import org.finos.legend.pure.m3.navigation.Instance;
import org.finos.legend.pure.m3.navigation.M3Paths;
import org.finos.legend.pure.m3.navigation.ProcessorSupport;
import org.finos.legend.pure.m3.navigation._package._Package;
import org.finos.legend.pure.m4.coreinstance.CoreInstance;
import org.finos.legend.pure.m4.coreinstance.SourceInformation;
import org.finos.legend.pure.m4.exception.PureCompilationException;

import java.util.Comparator;

public class _RelationType
{
    public static String print(CoreInstance rawType, ProcessorSupport processorSupport)
    {
        return "(" + rawType.getValueForMetaPropertyToMany("columns").collect(c -> _Column.print(c, processorSupport)).makeString(", ") + ")";
    }

    public static Function<?> findColumn(RelationType<?> type, String name, SourceInformation sourceInformation, ProcessorSupport processorSupport)
    {
        CoreInstance col = Instance.getValueForMetaPropertyToManyResolved(type, "columns", processorSupport).select(c -> Instance.getValueForMetaPropertyToOneResolved(c, "name", processorSupport).getName().equals(name)).getFirst();
        if (col == null)
        {
            throw new PureCompilationException("The system can't find the column " + name + " in the Relation " + org.finos.legend.pure.m3.navigation.generictype.GenericType.print(processorSupport.type_wrapGenericType(type), processorSupport));
        }
        return (Function<?>) col;
    }

    public static RelationType<?> build(MutableList<CoreInstance> cols, SourceInformation pureSourceInformation, ProcessorSupport processorSupport)
    {
        RelationType<?> newRelationType = (RelationType<?>) processorSupport.newAnonymousCoreInstance(pureSourceInformation, M3Paths.RelationType);
        GenericType source = (GenericType) processorSupport.type_wrapGenericType(newRelationType);

        cols.forEach(c -> _Column.updateSource((Column<?, ?>) c, source));

        Bag<String> duplicates = cols.collect(c -> ((Column<?, ?>) c)._name()).toBag().selectDuplicates();
        if (!duplicates.isEmpty())
        {
            throw new PureCompilationException("The relation contains duplicates: " + Sets.mutable.withAll(duplicates));
        }


        // Ensure T is set to parent
        GenericType classifierGenericType = (GenericType) processorSupport.newAnonymousCoreInstance(pureSourceInformation, M3Paths.GenericType);
        classifierGenericType._rawType((Type) _Package.getByUserPath(M3Paths.RelationType, processorSupport));
        classifierGenericType._typeArgumentsAdd(source);
        newRelationType._classifierGenericType(classifierGenericType);

        // SubType of Any
        Generalization generalization = (Generalization) processorSupport.newAnonymousCoreInstance(pureSourceInformation, M3Paths.Generalization);
        generalization._specific(newRelationType);
        org.finos.legend.pure.m3.coreinstance.meta.pure.metamodel.type.generics.GenericType anyG = (org.finos.legend.pure.m3.coreinstance.meta.pure.metamodel.type.generics.GenericType) processorSupport.newAnonymousCoreInstance(pureSourceInformation, M3Paths.GenericType);
        anyG._rawType((Class<?>) processorSupport.package_getByUserPath(M3Paths.Any));
        generalization._general(anyG);
        newRelationType._generalizationsAdd(generalization);

        // Set columns
        newRelationType.setKeyValues(Lists.mutable.with("columns"), cols);

        return newRelationType;
    }

    public static Pair<ListIterable<? extends Column<?, ?>>, ListIterable<? extends Column<?, ?>>> alignColumnSets(RichIterable<? extends Column<?, ?>> candidateColumns, RichIterable<? extends Column<?, ?>> signatureColumns, ProcessorSupport processorSupport)
    {
        // Manage wildcards
        RichIterable<? extends Pair<? extends Column<?, ?>, ? extends Column<?, ?>>> wildCard = signatureColumns.zip(candidateColumns).select(c -> c.getOne()._nameWildCard());
        MutableList<Column<?, ?>> _candidateColumns = (MutableList<Column<?, ?>>) candidateColumns.toList();
        _candidateColumns.removeAll(wildCard.collect(c -> c.getTwo()).toList());

        // Sort both sets and keep the candidate that aligns to the signature.
        RichIterable<Column<?, ?>> _signatureColumns = (MutableList<Column<?, ?>>) signatureColumns.toList();
        MutableSet<String> signatureNames = _signatureColumns.collect(FunctionAccessor::_name).toSet();
        MutableList<Column<?, ?>> sortedCandidateSub = _candidateColumns.select(c -> signatureNames.contains(c._name())).toSortedList(Comparator.comparing(FunctionAccessor::_name));
        MutableList<Column<?, ?>> sortedSignatures = _signatureColumns.toSortedList(Comparator.comparing(FunctionAccessor::_name));

        // Add the wildcards back
        sortedSignatures.addAllIterable(wildCard.collect(Pair::getOne));
        sortedCandidateSub.addAllIterable(wildCard.collect(Pair::getTwo));
        return Tuples.pair(sortedCandidateSub, sortedSignatures);
    }

    public static boolean isCompatibleWith(CoreInstance one, CoreInstance two, ProcessorSupport processorSupport)
    {
        if (!(processorSupport.instance_instanceOf(one, M3Paths.RelationType) && processorSupport.instance_instanceOf(two, M3Paths.RelationType)))
        {
            return false;
        }
        RelationType<?> rOne = (RelationType<?>) one;
        RelationType<?> rTwo = (RelationType<?>) two;
        boolean twoContainsWildCard = rTwo._columns().injectInto(true, (a, b) -> a && b._nameWildCard());
        if (!twoContainsWildCard)
        {
            MutableSet<String> twoNames = rTwo._columns().collect(FunctionAccessor::_name).toSet();
            if (!rOne._columns().injectInto(true, (a, b) -> a && twoNames.contains(b._name())))
            {
                return false;
            }
        }
        Pair<ListIterable<? extends Column<?, ?>>, ListIterable<? extends Column<?, ?>>> cols = alignColumnSets(rOne._columns(), rTwo._columns(), processorSupport);
        return cols.getOne().zip(cols.getTwo()).injectInto(true, (a, b) -> a && org.finos.legend.pure.m3.navigation.generictype.GenericType.isGenericCompatibleWith(_Column.getColumnType(b.getOne()), _Column.getColumnType(b.getTwo()), processorSupport));
    }

    public static boolean equalRelationType(CoreInstance one, CoreInstance two, ProcessorSupport processorSupport)
    {
        if (!processorSupport.instance_instanceOf(two, M3Paths.RelationType))
        {
            return false;
        }
        ListIterable<? extends CoreInstance> col_one = one.getValueForMetaPropertyToMany("columns");
        ListIterable<? extends CoreInstance> col_two = two.getValueForMetaPropertyToMany("columns");
        if (col_one.size() != col_two.size())
        {
            return false;
        }
        return col_one.zip(col_two).injectInto(true, (a, b) ->
        {
            CoreInstance typeOne = b.getOne().getValueForMetaPropertyToOne("classifierGenericType").getValueForMetaPropertyToMany("typeArguments").get(1).getValueForMetaPropertyToOne("rawType");
            CoreInstance typeTwo = b.getTwo().getValueForMetaPropertyToOne("classifierGenericType").getValueForMetaPropertyToMany("typeArguments").get(1).getValueForMetaPropertyToOne("rawType");
            return a && typeOne == typeTwo && b.getOne().getValueForMetaPropertyToOne("name").getName().equals(b.getTwo().getValueForMetaPropertyToOne("name").getName());
        });
    }

    public static boolean canConcatenate(CoreInstance one, CoreInstance two, ProcessorSupport processorSupport)
    {
        MutableList<Column<?, ?>> columns1 = (MutableList<Column<?, ?>>) ((RelationType<?>) one.getValueForMetaPropertyToOne("rawType"))._columns().toList();
        MutableList<Column<?, ?>> columns2 = (MutableList<Column<?, ?>>) ((RelationType<?>) two.getValueForMetaPropertyToOne("rawType"))._columns().toList();

        return columns1.zip(columns2).injectInto(true, (a, b) -> a &&
                (b.getOne()._nameWildCard() || b.getTwo()._nameWildCard() || b.getOne()._name().equals(b.getTwo()._name())) &&
                org.finos.legend.pure.m3.navigation.generictype.GenericType.isGenericCompatibleWith(_Column.getColumnType(b.getOne()), _Column.getColumnType(b.getTwo()), processorSupport));
    }

    public static GenericType merge(GenericType existingGenericType, GenericType genericTypeCopy, boolean isCovariant, ProcessorSupport processorSupport)
    {
        MutableList<Column<?, ?>> columns1 = (MutableList<Column<?, ?>>) ((RelationType<?>) existingGenericType.getValueForMetaPropertyToOne("rawType"))._columns().toList();
        MutableList<Column<?, ?>> columns2 = (MutableList<Column<?, ?>>) ((RelationType<?>) genericTypeCopy.getValueForMetaPropertyToOne("rawType"))._columns().toList();

        GenericType res = (GenericType) processorSupport.newGenericType(null, existingGenericType, true);
        res._rawType(
                _RelationType.build(
                        columns1.zip(columns2).collect(c ->
                        {
                            boolean wildcard = c.getOne()._nameWildCard() && c.getTwo()._nameWildCard();
                            if (!c.getOne()._nameWildCard() && !c.getTwo()._nameWildCard())
                            {
                                if (!c.getOne()._name().equals(c.getTwo()._name()))
                                {
                                    throw new PureCompilationException("Incompatible types " + _RelationType.print((RelationType<?>) existingGenericType.getValueForMetaPropertyToOne("rawType"), processorSupport) + " && " +
                                            _RelationType.print((RelationType<?>) genericTypeCopy.getValueForMetaPropertyToOne("rawType"), processorSupport));
                                }
                            }
                            String cName = c.getOne()._nameWildCard() ? c.getTwo()._name() : c.getOne()._name();
                            GenericType a = _Column.getColumnType(c.getOne());
                            GenericType b = _Column.getColumnType(c.getTwo());
                            GenericType merged = a._rawType() == null && b._rawType() == null ? a : (GenericType) org.finos.legend.pure.m3.navigation.generictype.GenericType.findBestCommonGenericType(Lists.mutable.with(a, b), isCovariant, false, genericTypeCopy.getSourceInformation(), processorSupport);
                            return _Column.getColumnInstance(cName, wildcard, res, merged, null, processorSupport);
                        }),
                        existingGenericType.getValueForMetaPropertyToOne("rawType").getSourceInformation(),
                        processorSupport
                )
        );
        return res;
    }
}
