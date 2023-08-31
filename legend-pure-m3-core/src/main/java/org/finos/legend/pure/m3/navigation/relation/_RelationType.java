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

import org.eclipse.collections.api.factory.Bags;
import org.eclipse.collections.api.factory.Lists;
import org.eclipse.collections.api.factory.Sets;
import org.eclipse.collections.api.list.ListIterable;
import org.eclipse.collections.api.list.MutableList;
import org.eclipse.collections.api.set.MutableSet;
import org.finos.legend.pure.m3.coreinstance.meta.pure.metamodel.function.Function;
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

public class _RelationType
{

    public static String print(CoreInstance rawType)
    {
        return "(" + rawType.getValueForMetaPropertyToMany("columns").collect(c ->
                {
                    CoreInstance type = c.getValueForMetaPropertyToOne("classifierGenericType").getValueForMetaPropertyToMany("typeArguments").get(1).getValueForMetaPropertyToOne("rawType");
                    return c.getValueForMetaPropertyToOne("name").getName() + ":" + (type == null ? null : type.getName());
                }
        ).makeString(", ") + ")";
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

    public static RelationType<?> build(GenericType parent, MutableList<CoreInstance> cols, SourceInformation pureSourceInformation, ProcessorSupport processorSupport)
    {
        RelationType<?> newRelationType = (RelationType<?>) processorSupport.newAnonymousCoreInstance(pureSourceInformation, M3Paths.RelationType);

        // Ensure T is set to parent
        GenericType classifierGenericType = (GenericType) processorSupport.newAnonymousCoreInstance(pureSourceInformation, M3Paths.GenericType);
        classifierGenericType._rawType((Type) _Package.getByUserPath(M3Paths.RelationType, processorSupport));
        classifierGenericType._typeArgumentsAdd(parent);
        newRelationType._classifierGenericType(classifierGenericType);

        // SubType of Any
        Generalization generalization = (Generalization) processorSupport.newAnonymousCoreInstance(pureSourceInformation, M3Paths.Generalization);
        generalization._specific(newRelationType);
        org.finos.legend.pure.m3.coreinstance.meta.pure.metamodel.type.generics.GenericType anyG = (org.finos.legend.pure.m3.coreinstance.meta.pure.metamodel.type.generics.GenericType) processorSupport.newAnonymousCoreInstance(pureSourceInformation, M3Paths.GenericType);
        anyG._rawType((Class<?>) processorSupport.package_getByUserPath(M3Paths.Any));
        generalization._general(anyG);
        newRelationType._generalizationsAdd(generalization);

        // Check duplicate names
        MutableSet<String> potentialDups = Sets.mutable.withAll(Bags.mutable.withAll(cols.collect(c -> c.getValueForMetaPropertyToOne("name").getName())).selectDuplicates());
        if (!potentialDups.isEmpty())
        {
            throw new PureCompilationException("The relation contains duplicates: " + potentialDups);
        }

        // Set columns
        newRelationType.setKeyValues(Lists.mutable.with("columns"), cols);

        return newRelationType;
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

}
