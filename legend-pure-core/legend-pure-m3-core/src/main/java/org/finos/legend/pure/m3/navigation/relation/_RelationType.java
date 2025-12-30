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
import org.eclipse.collections.api.factory.Bags;
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
import org.finos.legend.pure.m3.coreinstance.meta.pure.metamodel.relation.RelationTypeCoreInstanceWrapper;
import org.finos.legend.pure.m3.coreinstance.meta.pure.metamodel.relationship.Generalization;
import org.finos.legend.pure.m3.coreinstance.meta.pure.metamodel.type.Any;
import org.finos.legend.pure.m3.coreinstance.meta.pure.metamodel.type.Class;
import org.finos.legend.pure.m3.coreinstance.meta.pure.metamodel.type.Type;
import org.finos.legend.pure.m3.coreinstance.meta.pure.metamodel.type.generics.GenericType;
import org.finos.legend.pure.m3.navigation.Instance;
import org.finos.legend.pure.m3.navigation.M3Paths;
import org.finos.legend.pure.m3.navigation.M3Properties;
import org.finos.legend.pure.m3.navigation.ProcessorSupport;
import org.finos.legend.pure.m3.navigation._package._Package;
import org.finos.legend.pure.m3.navigation.importstub.ImportStub;
import org.finos.legend.pure.m3.navigation.multiplicity.Multiplicity;
import org.finos.legend.pure.m4.coreinstance.AbstractCoreInstanceWrapper;
import org.finos.legend.pure.m4.coreinstance.CoreInstance;
import org.finos.legend.pure.m4.coreinstance.SourceInformation;
import org.finos.legend.pure.m4.exception.PureCompilationException;
import org.finos.legend.pure.m4.serialization.grammar.StringEscape;
import org.finos.legend.pure.m4.tools.SafeAppendable;

public class _RelationType
{
    public static boolean isRelationType(CoreInstance type, ProcessorSupport processorSupport)
    {
        if (type == null)
        {
            return false;
        }
        if (type instanceof RelationType)
        {
            return true;
        }
        return (!(type instanceof Any) || (type instanceof AbstractCoreInstanceWrapper)) && processorSupport.instance_instanceOf(type, M3Paths.RelationType);
    }

    public static String print(CoreInstance relationType, ProcessorSupport processorSupport)
    {
        return print(new StringBuilder(), relationType, processorSupport).toString();
    }

    public static <T extends Appendable> T print(T appendable, CoreInstance relationType, ProcessorSupport processorSupport)
    {
        SafeAppendable safeAppendable = SafeAppendable.wrap(appendable);
        safeAppendable.append('(');
        relationType.getValueForMetaPropertyToMany("columns").forEachWithIndex((c, i) -> _Column.print(((i == 0) ? safeAppendable : safeAppendable.append(", ")), c, processorSupport));
        safeAppendable.append(')');
        return appendable;
    }

    public static Function<?> findColumn(RelationType<?> type, String name, SourceInformation sourceInformation, ProcessorSupport processorSupport)
    {
        CoreInstance col = Instance.getValueForMetaPropertyToManyResolved(type, "columns", processorSupport).select(c -> Instance.getValueForMetaPropertyToOneResolved(c, M3Properties.name, processorSupport).getName().equals(StringEscape.unescape(name))).getFirst();
        if (col == null)
        {
            throw new PureCompilationException(sourceInformation, "The system can't find the column " + name + " in the Relation " + org.finos.legend.pure.m3.navigation.generictype.GenericType.print(processorSupport.type_wrapGenericType(type), processorSupport));
        }
        return (Function<?>) col;
    }

    public static RelationType<?> build(ListIterable<? extends CoreInstance> cols, SourceInformation pureSourceInformation, ProcessorSupport processorSupport)
    {
        RelationType<?> newRelationType = (RelationType<?>) processorSupport.newAnonymousCoreInstance(pureSourceInformation, M3Paths.RelationType);
        GenericType source = (GenericType) processorSupport.type_wrapGenericType(newRelationType);

        cols.forEach(c -> _Column.updateSource((Column<?, ?>) c, source));

        Bag<String> duplicates = cols.collect(c -> ((Column<?, ?>) c)._name(), Bags.mutable.empty()).selectDuplicates();
        if (duplicates.notEmpty())
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
        anyG._rawType((Class<?>) processorSupport.type_TopType());
        generalization._general(anyG);
        newRelationType._generalizationsAdd(generalization);

        // Set columns
        newRelationType.setKeyValues(Lists.mutable.with("columns"), cols);

        return newRelationType;
    }

    public static Pair<ListIterable<? extends Column<?, ?>>, ListIterable<? extends Column<?, ?>>> alignColumnSets(RichIterable<? extends Column<?, ?>> candidateColumns, RichIterable<? extends Column<?, ?>> signatureColumns, ProcessorSupport processorSupport)
    {
        // Manage wildcards
        MutableList<? extends Pair<? extends Column<?, ?>, ? extends Column<?, ?>>> wildCard = signatureColumns.zip(candidateColumns).select(c -> c.getOne()._nameWildCard(), Lists.mutable.empty());
        MutableList<Column<?, ?>> _candidateColumns = Lists.mutable.withAll(candidateColumns);
        _candidateColumns.removeAll(wildCard.collect(Pair::getTwo));
        MutableList<Column<?, ?>> _signatureColumns = Lists.mutable.withAll(signatureColumns);
        _signatureColumns.removeAll(wildCard.collect(Pair::getOne));

        // Sort both sets and keep the candidate that aligns to the signature.
        MutableList<Column<?, ?>> sortedSignatures = Lists.mutable.<Column<?, ?>>withAll(_signatureColumns).sortThisBy(FunctionAccessor::_name);
        MutableSet<String> signatureNames = sortedSignatures.collect(FunctionAccessor::_name, Sets.mutable.empty());
        MutableList<Column<?, ?>> sortedCandidateSub = _candidateColumns.select(c -> signatureNames.contains(c._name())).sortThisBy(FunctionAccessor::_name);

        // Add the wildcards back
        wildCard.forEach(pair ->
        {
            sortedSignatures.add(pair.getOne());
            sortedCandidateSub.add(pair.getTwo());
        });
        return Tuples.pair(sortedCandidateSub, sortedSignatures);
    }

    public static boolean isCompatibleWith(CoreInstance one, CoreInstance two, ProcessorSupport processorSupport)
    {
        if (!(isRelationType(one, processorSupport) && isRelationType(two, processorSupport)))
        {
            return false;
        }
        RelationType<?> rOne = RelationTypeCoreInstanceWrapper.toRelationType(one);
        RelationType<?> rTwo = RelationTypeCoreInstanceWrapper.toRelationType(two);
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
        if (!isRelationType(two, processorSupport))
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
            CoreInstance typeOne = b.getOne().getValueForMetaPropertyToOne(M3Properties.classifierGenericType).getValueForMetaPropertyToMany(M3Properties.typeArguments).get(1).getValueForMetaPropertyToOne(M3Properties.rawType);
            CoreInstance typeTwo = b.getTwo().getValueForMetaPropertyToOne(M3Properties.classifierGenericType).getValueForMetaPropertyToMany(M3Properties.typeArguments).get(1).getValueForMetaPropertyToOne(M3Properties.rawType);
            return a && typeOne == typeTwo && b.getOne().getValueForMetaPropertyToOne(M3Properties.name).getName().equals(b.getTwo().getValueForMetaPropertyToOne(M3Properties.name).getName());
        });
    }

    public static boolean canConcatenate(CoreInstance one, CoreInstance two, ProcessorSupport processorSupport)
    {
        MutableList<Column<?, ?>> columns1 = Lists.mutable.withAll(((RelationType<?>) one.getValueForMetaPropertyToOne(M3Properties.rawType))._columns());
        MutableList<Column<?, ?>> columns2 = Lists.mutable.withAll(((RelationType<?>) two.getValueForMetaPropertyToOne(M3Properties.rawType))._columns());

        return columns1.zip(columns2).injectInto(true, (a, b) -> a &&
                (b.getOne()._nameWildCard() || b.getTwo()._nameWildCard() || b.getOne()._name().equals(b.getTwo()._name())) &&
                org.finos.legend.pure.m3.navigation.generictype.GenericType.isGenericCompatibleWith(_Column.getColumnType(b.getOne()), _Column.getColumnType(b.getTwo()), processorSupport));
    }

    public static GenericType merge(GenericType existingGenericType, GenericType genericTypeCopy, boolean isCovariant, ProcessorSupport processorSupport)
    {
        MutableList<Column<?, ?>> columns1 = Lists.mutable.withAll(((RelationType<?>) existingGenericType.getValueForMetaPropertyToOne(M3Properties.rawType))._columns());
        MutableList<Column<?, ?>> columns2 = Lists.mutable.withAll(((RelationType<?>) genericTypeCopy.getValueForMetaPropertyToOne(M3Properties.rawType))._columns());

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
                                    throw new PureCompilationException("Incompatible types " + _RelationType.print(existingGenericType.getValueForMetaPropertyToOne(M3Properties.rawType), processorSupport) + " && " +
                                            _RelationType.print(genericTypeCopy.getValueForMetaPropertyToOne(M3Properties.rawType), processorSupport));
                                }
                            }
                            String cName = c.getOne()._nameWildCard() ? c.getTwo()._name() : c.getOne()._name();
                            GenericType a = _Column.getColumnType(c.getOne());
                            GenericType b = _Column.getColumnType(c.getTwo());
                            Type rawTypeA = (Type)Instance.getValueForMetaPropertyToOneResolved(a, M3Properties.rawType, processorSupport);
                            Type rawTypeB = (Type)Instance.getValueForMetaPropertyToOneResolved(b, M3Properties.rawType, processorSupport);
                            GenericType merged = rawTypeA == null ? b : rawTypeB == null ? a : (GenericType) org.finos.legend.pure.m3.navigation.generictype.GenericType.findBestCommonGenericType(Lists.mutable.with(a, b), isCovariant, false, genericTypeCopy.getSourceInformation(), processorSupport);
                            org.finos.legend.pure.m3.coreinstance.meta.pure.metamodel.multiplicity.Multiplicity mergedMul = rawTypeA == null ? _Column.getColumnMultiplicity(c.getTwo()) : rawTypeB == null ? _Column.getColumnMultiplicity(c.getOne()) :(org.finos.legend.pure.m3.coreinstance.meta.pure.metamodel.multiplicity.Multiplicity) Multiplicity.minSubsumingMultiplicity(_Column.getColumnMultiplicity(c.getOne()), _Column.getColumnMultiplicity(c.getTwo()), processorSupport);
                            return _Column.getColumnInstance(cName, wildcard, merged, mergedMul, null, processorSupport);
                        }),
                        existingGenericType.getValueForMetaPropertyToOne(M3Properties.rawType).getSourceInformation(),
                        processorSupport
                )
        );
        return res;
    }

    public static boolean containsExtendedPrimitiveType(CoreInstance relationType, ProcessorSupport processorSupport)
    {
        RelationType<?> rOne = RelationTypeCoreInstanceWrapper.toRelationType(relationType);
        return rOne._columns().injectInto(false, (a,b) -> a || org.finos.legend.pure.m3.navigation.generictype.GenericType.testContainsExtendedPrimitiveTypes(_Column.getColumnType(b), processorSupport));
    }

    public static boolean isRelationTypeFullyConcrete(CoreInstance relationType, boolean checkFunctionTypes, ProcessorSupport processorSupport)
    {
        return ((RelationType<?>) relationType)._columns()
                .allSatisfy(c -> org.finos.legend.pure.m3.navigation.generictype.GenericType.isGenericTypeFullyConcrete(_Column.getColumnType(c), checkFunctionTypes, processorSupport));
    }

    public static boolean isRelationTypeFullyDefined(CoreInstance relationType, ProcessorSupport processorSupport)
    {
        return ((RelationType<?>) relationType)._columns()
                .allSatisfy(c -> org.finos.legend.pure.m3.navigation.generictype.GenericType.isGenericTypeFullyDefined(_Column.getColumnType(c), processorSupport));
    }

    public static void resolveImportStubs(CoreInstance relationType, ProcessorSupport processorSupport)
    {
        relationType.getValueForMetaPropertyToMany(M3Properties.columns).forEach(c ->
        {
            CoreInstance classifierGenericType = c.getValueForMetaPropertyToOne(M3Properties.classifierGenericType);
            classifierGenericType.getValueForMetaPropertyToMany(M3Properties.multiplicityArguments).forEach(arg -> ImportStub.withImportStubByPass(arg, processorSupport));
            // the first type argument of the column type is the relation type itself: we skip it to avoid infinite recursion
            CoreInstance type = classifierGenericType.getValueForMetaPropertyToMany(M3Properties.typeArguments).get(1);
            org.finos.legend.pure.m3.navigation.generictype.GenericType.resolveImportStubs(type, processorSupport);
        });
    }
}
