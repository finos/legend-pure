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

package org.finos.legend.pure.m3.inlinedsl.path.unloader;

import org.eclipse.collections.api.RichIterable;
import org.finos.legend.pure.m3.navigation.M3Paths;
import org.finos.legend.pure.m3.compiler.Context;
import org.finos.legend.pure.m3.compiler.ReferenceUsage;
import org.finos.legend.pure.m3.compiler.unload.unbind.Shared;
import org.finos.legend.pure.m3.compiler.unload.unbind.UnbindState;
import org.finos.legend.pure.m3.coreinstance.meta.pure.metamodel._import.PropertyStub;
import org.finos.legend.pure.m3.coreinstance.meta.pure.metamodel.path.Path;
import org.finos.legend.pure.m3.coreinstance.meta.pure.metamodel.path.PathElement;
import org.finos.legend.pure.m3.coreinstance.meta.pure.metamodel.path.PropertyPathElement;
import org.finos.legend.pure.m3.coreinstance.meta.pure.metamodel.type.generics.GenericType;
import org.finos.legend.pure.m3.coreinstance.meta.pure.metamodel.valuespecification.InstanceValue;
import org.finos.legend.pure.m3.coreinstance.meta.pure.metamodel.valuespecification.ValueSpecification;
import org.finos.legend.pure.m3.navigation.ProcessorSupport;
import org.finos.legend.pure.m3.tools.matcher.MatchRunner;
import org.finos.legend.pure.m3.tools.matcher.Matcher;
import org.finos.legend.pure.m3.tools.matcher.MatcherState;
import org.finos.legend.pure.m4.coreinstance.CoreInstance;
import org.finos.legend.pure.m4.ModelRepository;
import org.finos.legend.pure.m4.exception.PureCompilationException;

public class PathUnbind implements MatchRunner<Path>
{
    @Override
    public String getClassName()
    {
        return M3Paths.Path;
    }

    @Override
    public void run(Path modelElement, MatcherState state, Matcher matcher, ModelRepository modelRepository, Context context) throws PureCompilationException
    {
        ProcessorSupport processorSupport = state.getProcessorSupport();
        GenericType start = modelElement._start();
        Shared.cleanUpGenericType(start, (UnbindState)state, processorSupport);
        for (PathElement pathElement : (RichIterable<PathElement>) modelElement._path())
        {
            if (pathElement instanceof PropertyPathElement)
            {
                PropertyStub property = (PropertyStub)((PropertyPathElement)pathElement)._propertyCoreInstance();
                CoreInstance resolved = property._resolvedPropertyCoreInstance();
                if (resolved != null)
                {
                    ReferenceUsage.removeReferenceUsagesForUser(resolved, modelElement, state.getProcessorSupport());
                }
                Shared.cleanPropertyStub(property, processorSupport);
                property._ownerRemove();

                RichIterable<? extends ValueSpecification> parameters = ((PropertyPathElement)pathElement)._parameters();
                for (ValueSpecification parameter : parameters)
                {

                    if (parameter instanceof InstanceValue)
                    {
                        RichIterable<? extends CoreInstance> values = ((InstanceValue)parameter)._valuesCoreInstance();
                        for (CoreInstance value : values)
                        {
                            Shared.cleanEnumStub(value, processorSupport);
                        }

                        parameter._multiplicityRemove();
                        parameter._genericTypeRemove();
                    }

                }
            }

        }
        modelElement._classifierGenericTypeRemove();
    }
}
