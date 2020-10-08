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

package org.finos.legend.pure.m3.compiler.unload.unbind;

import org.finos.legend.pure.m3.navigation.M3Paths;
import org.finos.legend.pure.m3.compiler.Context;
import org.finos.legend.pure.m3.compiler.postprocessing.processor.projection.ProjectionUtil;
import org.finos.legend.pure.m3.navigation.importstub.ImportStub;
import org.finos.legend.pure.m3.coreinstance.meta.pure.metamodel.treepath.RootRouteNode;
import org.finos.legend.pure.m3.coreinstance.meta.pure.metamodel.treepath.RootRouteNodeCoreInstanceWrapper;
import org.finos.legend.pure.m3.coreinstance.meta.pure.metamodel.type.ClassProjection;
import org.finos.legend.pure.m3.coreinstance.meta.pure.metamodel.type.Type;
import org.finos.legend.pure.m3.coreinstance.meta.pure.metamodel.type.generics.GenericTypeCoreInstanceWrapper;
import org.finos.legend.pure.m3.tools.matcher.MatchRunner;
import org.finos.legend.pure.m3.tools.matcher.Matcher;
import org.finos.legend.pure.m3.tools.matcher.MatcherState;
import org.finos.legend.pure.m4.ModelRepository;
import org.finos.legend.pure.m4.exception.PureCompilationException;


public class ClassProjectionUnbind implements MatchRunner<ClassProjection>
{
    @Override
    public String getClassName()
    {
        return M3Paths.ClassProjection;
    }

    @Override
    public void run(ClassProjection classProjection, MatcherState state, Matcher matcher, ModelRepository modelRepository, Context context) throws PureCompilationException
    {

        if (classProjection._properties().notEmpty())
        {
            classProjection._propertiesRemove();
        }
        if (classProjection._qualifiedProperties().notEmpty())
        {
            classProjection._qualifiedPropertiesRemove();
        }

        RootRouteNode projectionSpecification = RootRouteNodeCoreInstanceWrapper.toRootRouteNode(classProjection._projectionSpecification());
        if (projectionSpecification._type() != null && GenericTypeCoreInstanceWrapper.toGenericType(projectionSpecification._type())._rawTypeCoreInstance() != null)
        {
            try
            {
                Type projectedClass = (Type)ImportStub.withImportStubByPass(projectionSpecification._type()._rawTypeCoreInstance(), state.getProcessorSupport());
                ProjectionUtil.removedCopiedAnnotations(projectedClass, classProjection, state.getProcessorSupport());
                ProjectionUtil.removedCopiedAnnotations(projectionSpecification, classProjection, state.getProcessorSupport());
            }
            catch (PureCompilationException pe)
            {
                //This might happen, the rawType might be unbound and cannot be resolved.
            }
        }

        Shared.cleanUpReferenceUsage(classProjection._projectionSpecification(), classProjection, state.getProcessorSupport());

        matcher.fullMatch(projectionSpecification, state);
    }
}
