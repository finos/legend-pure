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

package org.finos.legend.pure.m2.dsl.mapping.serialization.grammar.v1.unloader;

import org.finos.legend.pure.m2.dsl.mapping.M2MappingPaths;
import org.finos.legend.pure.m3.compiler.Context;
import org.finos.legend.pure.m3.navigation.importstub.ImportStub;
import org.finos.legend.pure.m3.compiler.unload.unbind.Shared;
import org.finos.legend.pure.m3.coreinstance.meta.pure.mapping.PropertyMapping;
import org.finos.legend.pure.m3.coreinstance.meta.pure.mapping.xStore.XStoreAssociationImplementation;
import org.finos.legend.pure.m3.coreinstance.meta.pure.mapping.xStore.XStorePropertyMapping;
import org.finos.legend.pure.m3.coreinstance.meta.pure.metamodel.type.FunctionType;
import org.finos.legend.pure.m3.navigation.ProcessorSupport;
import org.finos.legend.pure.m3.tools.matcher.MatchRunner;
import org.finos.legend.pure.m3.tools.matcher.Matcher;
import org.finos.legend.pure.m3.tools.matcher.MatcherState;
import org.finos.legend.pure.m4.ModelRepository;
import org.finos.legend.pure.m4.exception.PureCompilationException;

public class XStoreUnbind implements MatchRunner<XStoreAssociationImplementation>
{
    @Override
    public String getClassName()
    {
        return M2MappingPaths.XStoreAssociationImplementation;
    }

    @Override
    public void run(XStoreAssociationImplementation instance, MatcherState state, Matcher matcher, ModelRepository modelRepository, Context context) throws PureCompilationException
    {
        ProcessorSupport processorSupport = state.getProcessorSupport();
        Shared.cleanImportStub(instance._associationCoreInstance(), processorSupport);
        Shared.cleanImportStub(instance._parentCoreInstance(), processorSupport);
        for (PropertyMapping propertyMapping : instance._propertyMappings())
        {
            Shared.cleanPropertyStub(propertyMapping._propertyCoreInstance(), processorSupport);
            matcher.fullMatch(((XStorePropertyMapping)propertyMapping)._crossExpression(), state);
            FunctionType functionType = (FunctionType)ImportStub.withImportStubByPass(((XStorePropertyMapping)propertyMapping)._crossExpression()._classifierGenericType()._typeArguments().toList().get(0)._rawTypeCoreInstance(), processorSupport);
            functionType._parametersRemove();
            functionType._functionRemove();
        }
    }
}
