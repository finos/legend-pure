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
import org.finos.legend.pure.m3.compiler.unload.unbind.Shared;
import org.finos.legend.pure.m3.coreinstance.meta.pure.mapping.PropertyMapping;
import org.finos.legend.pure.m3.coreinstance.meta.pure.mapping.modelToModel.PureInstanceSetImplementation;
import org.finos.legend.pure.m3.coreinstance.meta.pure.mapping.modelToModel.PurePropertyMapping;
import org.finos.legend.pure.m3.coreinstance.meta.pure.metamodel._import.ImportStub;
import org.finos.legend.pure.m3.coreinstance.meta.pure.metamodel.function.LambdaFunction;
import org.finos.legend.pure.m3.coreinstance.meta.pure.metamodel.type.FunctionType;
import org.finos.legend.pure.m3.coreinstance.meta.pure.tools.GrammarInfoStub;
import org.finos.legend.pure.m3.tools.matcher.MatchRunner;
import org.finos.legend.pure.m3.tools.matcher.Matcher;
import org.finos.legend.pure.m3.tools.matcher.MatcherState;
import org.finos.legend.pure.m4.ModelRepository;
import org.finos.legend.pure.m4.exception.PureCompilationException;

public class PureInstanceSetImplementationUnbind implements MatchRunner<PureInstanceSetImplementation>
{
    @Override
    public String getClassName()
    {
        return M2MappingPaths.PureInstanceSetImplementation;
    }

    @Override
    public void run(PureInstanceSetImplementation pureInstanceSetImplementation, MatcherState state, Matcher matcher, ModelRepository modelRepository, Context context) throws PureCompilationException
    {
        ImportStub srcClass = (ImportStub)pureInstanceSetImplementation._srcClassCoreInstance();
        if (srcClass != null)
        {
            Shared.cleanUpReferenceUsage(srcClass, pureInstanceSetImplementation, state.getProcessorSupport());
            Shared.cleanImportStub(srcClass, state.getProcessorSupport());
        }

        LambdaFunction filter = pureInstanceSetImplementation._filter();
        if (filter != null)
        {
            matcher.fullMatch(filter, state);
            ((FunctionType)org.finos.legend.pure.m3.navigation.importstub.ImportStub.withImportStubByPass(filter._classifierGenericType()._typeArguments().toList().get(0)._rawTypeCoreInstance(), state.getProcessorSupport()))._parametersRemove();
        }

        for (PropertyMapping propertyMapping : pureInstanceSetImplementation._propertyMappings())
        {
            Shared.cleanPropertyStub(propertyMapping._propertyCoreInstance(), state.getProcessorSupport());
            LambdaFunction transform = ((PurePropertyMapping)propertyMapping)._transform();
            matcher.fullMatch(transform, state);
            FunctionType functionType = (FunctionType)org.finos.legend.pure.m3.navigation.importstub.ImportStub.withImportStubByPass(
                    transform._classifierGenericType()._typeArguments().toList().get(0)._rawTypeCoreInstance(), state.getProcessorSupport());
            functionType._parametersRemove();
            functionType._functionRemove();

            if (((PurePropertyMapping)propertyMapping)._transformerCoreInstance() != null)
            {
                GrammarInfoStub transformerStub = (GrammarInfoStub)((PurePropertyMapping)propertyMapping)._transformerCoreInstance();
                transformerStub._value(transformerStub._original());
                transformerStub._originalRemove();
            }
        }
    }
}