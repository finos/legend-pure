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

package org.finos.legend.pure.m2.dsl.mapping.serialization.grammar.v1.walker;

import org.finos.legend.pure.m2.dsl.mapping.M2MappingPaths;
import org.finos.legend.pure.m3.compiler.Context;
import org.finos.legend.pure.m3.navigation.importstub.ImportStub;
import org.finos.legend.pure.m3.coreinstance.meta.pure.mapping.Mapping;
import org.finos.legend.pure.m3.coreinstance.meta.pure.mapping.PropertyOwnerImplementation;
import org.finos.legend.pure.m3.tools.matcher.MatchRunner;
import org.finos.legend.pure.m3.tools.matcher.Matcher;
import org.finos.legend.pure.m3.tools.matcher.MatcherState;
import org.finos.legend.pure.m4.coreinstance.CoreInstance;
import org.finos.legend.pure.m4.ModelRepository;
import org.finos.legend.pure.m4.exception.PureCompilationException;

public class PropertyOwnerImplementationWalker implements MatchRunner<PropertyOwnerImplementation>
{
    @Override
    public String getClassName()
    {
        return M2MappingPaths.PropertyOwnerImplementation;
    }

    @Override
    public void run(PropertyOwnerImplementation propertyOwnerImplementation, MatcherState state, Matcher matcher, ModelRepository modelRepository, Context context) throws PureCompilationException
    {
        CoreInstance parentCoreInstance = propertyOwnerImplementation._parentCoreInstance();
        if (parentCoreInstance != null)
        {
            Mapping parent = (Mapping)ImportStub.withImportStubByPassDoNotResolve(parentCoreInstance, state.getProcessorSupport());
            if (parent != null)
            {
                matcher.fullMatch(parent, state);
            }
        }
    }
}
