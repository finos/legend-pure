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

package org.finos.legend.pure.m2.relational.inlinedsl.relation.processor;

import org.eclipse.collections.api.factory.Lists;
import org.eclipse.collections.api.list.MutableList;
import org.finos.legend.pure.m2.relational.M2RelationalPaths;
import org.finos.legend.pure.m3.compiler.Context;
import org.finos.legend.pure.m3.compiler.postprocessing.ProcessorState;
import org.finos.legend.pure.m3.compiler.postprocessing.processor.Processor;
import org.finos.legend.pure.m3.coreinstance.meta.pure.metamodel.type.Class;
import org.finos.legend.pure.m3.coreinstance.meta.pure.metamodel.type.generics.GenericType;
import org.finos.legend.pure.m3.coreinstance.meta.relational.metamodel.Database;
import org.finos.legend.pure.m3.coreinstance.meta.relational.metamodel.RelationDatabaseAccessor;
import org.finos.legend.pure.m3.coreinstance.meta.relational.metamodel.Schema;
import org.finos.legend.pure.m3.coreinstance.meta.relational.metamodel.relation.Table;
import org.finos.legend.pure.m3.navigation.ProcessorSupport;
import org.finos.legend.pure.m3.navigation._package._Package;
import org.finos.legend.pure.m3.navigation.relation._Column;
import org.finos.legend.pure.m3.navigation.relation._RelationType;
import org.finos.legend.pure.m3.tools.matcher.Matcher;
import org.finos.legend.pure.m4.ModelRepository;
import org.finos.legend.pure.m4.coreinstance.CoreInstance;
import org.finos.legend.pure.m4.exception.PureCompilationException;

public class RelationDatabaseAccessorProcessor extends Processor<RelationDatabaseAccessor<CoreInstance>>
{
    @Override
    public String getClassName()
    {
        return M2RelationalPaths.RelationDatabaseAccessor;
    }

    @Override
    public void process(RelationDatabaseAccessor<CoreInstance> instance, ProcessorState state, Matcher matcher, ModelRepository repository, Context context, ProcessorSupport processorSupport)
    {
        MutableList<? extends String> path = instance._path().toList();

        Database db = (Database) _Package.getByUserPath(path.get(0), processorSupport);
        if (db == null)
        {
            throw new PureCompilationException(instance.getSourceInformation(), "The database '" + path.getFirst() + "' can't be found");
        }

        instance._database(db);
        instance._store(db);

        Table table = null;
        if (path.size() == 2)
        {
            Schema schema = db._schemas().select(c -> c._name().equals("default")).getFirst();
            table = schema._tables().select(c -> c._name().equals(path.get(1))).getFirst();
            if (table == null)
            {
                throw new PureCompilationException(instance.getSourceInformation(), "The table '" + path.get(1) + "' can't be found in the database '" + path.get(0) + "'");
            }
        }

        instance._relation(table);

        Class<?> relationDatabaseAccessorType = (Class<?>) processorSupport.package_getByUserPath(M2RelationalPaths.RelationDatabaseAccessor);
        GenericType genericType = (GenericType) processorSupport.type_wrapGenericType(relationDatabaseAccessorType);
        GenericType typeParam = (GenericType) processorSupport.newGenericType(instance.getSourceInformation(), relationDatabaseAccessorType, false);

        MutableList<CoreInstance> columns = table._columns().collect(c -> (CoreInstance) _Column.getColumnInstance(c.getValueForMetaPropertyToOne("name").getName(), false, typeParam, (GenericType) processorSupport.type_wrapGenericType(_Package.getByUserPath("String", processorSupport)), instance.getSourceInformation(), processorSupport)).toList();
        typeParam._rawTypeCoreInstance(_RelationType.build(columns, instance.getSourceInformation(), processorSupport));
        genericType._typeArguments(Lists.mutable.with(typeParam));

        instance._classifierGenericType(genericType);
    }

    @Override
    public void populateReferenceUsages(RelationDatabaseAccessor<CoreInstance> relationDatabaseAccessor, ModelRepository repository, ProcessorSupport processorSupport)
    {
        this.addReferenceUsageForToOneProperty(relationDatabaseAccessor, relationDatabaseAccessor._database(), "database", repository, processorSupport);
    }
}
