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

package org.finos.legend.pure.m2.inlinedsl.store.processor;

import org.eclipse.collections.api.factory.Lists;
import org.eclipse.collections.api.list.MutableList;
import org.eclipse.collections.api.tuple.Pair;
import org.finos.legend.pure.m2.dsl.store.M2StorePaths;
import org.finos.legend.pure.m3.compiler.Context;
import org.finos.legend.pure.m3.compiler.postprocessing.ProcessorState;
import org.finos.legend.pure.m3.compiler.postprocessing.processor.Processor;
import org.finos.legend.pure.m3.coreinstance.meta.pure.metamodel.relation.RelationType;
import org.finos.legend.pure.m3.coreinstance.meta.pure.metamodel.type.Class;
import org.finos.legend.pure.m3.coreinstance.meta.pure.metamodel.type.generics.GenericType;
import org.finos.legend.pure.m3.coreinstance.meta.pure.store.RelationStoreAccessor;
import org.finos.legend.pure.m3.coreinstance.meta.pure.store.Store;
import org.finos.legend.pure.m3.navigation.ProcessorSupport;
import org.finos.legend.pure.m3.navigation._package._Package;
import org.finos.legend.pure.m3.tools.matcher.Matcher;
import org.finos.legend.pure.m4.ModelRepository;
import org.finos.legend.pure.m4.exception.PureCompilationException;

public class RelationStoreAccessorProcessor extends Processor<RelationStoreAccessor<?>>
{
    @Override
    public String getClassName()
    {
        return M2StorePaths.RelationStoreAccessor;
    }

    @Override
    public void process(RelationStoreAccessor<?> instance, ProcessorState state, Matcher matcher, ModelRepository repository, Context context, ProcessorSupport processorSupport)
    {
        MutableList<String> path = Lists.mutable.withAll(instance._path());

        Store store = (Store) _Package.getByUserPath(path.get(0), processorSupport);
        if (store == null)
        {
            throw new PureCompilationException(instance.getSourceInformation(), "The store '" + path.get(0) + "' can't be found");
        }

        instance._store(store);

        Pair<?, RelationType<?>> result = state.getParserLibrary().resolveRelationElementAccessor(store, path, instance.getSourceInformation(), processorSupport);

        instance._sourceElement(result.getOne());

        Class<?> relationDatabaseAccessorType = (Class<?>) processorSupport.package_getByUserPath(M2StorePaths.RelationStoreAccessor);
        GenericType genericType = (GenericType) processorSupport.type_wrapGenericType(relationDatabaseAccessorType);
        GenericType typeParam = (GenericType) processorSupport.newGenericType(instance.getSourceInformation(), relationDatabaseAccessorType, false);

        typeParam._rawTypeCoreInstance(result.getTwo());
        genericType._typeArguments(Lists.mutable.with(typeParam));

        instance._classifierGenericType(genericType);
    }

    @Override
    public void populateReferenceUsages(RelationStoreAccessor<?> relationDatabaseAccessor, ModelRepository repository, ProcessorSupport processorSupport)
    {
        this.addReferenceUsageForToOneProperty(relationDatabaseAccessor, relationDatabaseAccessor._store(), "store", repository, processorSupport);
    }
}
