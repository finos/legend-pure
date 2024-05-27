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

package org.finos.legend.pure.m3.compiler.unload.walk;

import org.finos.legend.pure.m3.compiler.Context;
import org.finos.legend.pure.m3.coreinstance.meta.pure.metamodel.function.property.AbstractProperty;
import org.finos.legend.pure.m3.coreinstance.meta.pure.metamodel.function.property.Property;
import org.finos.legend.pure.m3.coreinstance.meta.pure.metamodel.function.property.QualifiedProperty;
import org.finos.legend.pure.m3.coreinstance.meta.pure.metamodel.relationship.Association;
import org.finos.legend.pure.m3.coreinstance.meta.pure.metamodel.type.Type;
import org.finos.legend.pure.m3.navigation.M3Paths;
import org.finos.legend.pure.m3.navigation.ProcessorSupport;
import org.finos.legend.pure.m3.navigation.importstub.ImportStub;
import org.finos.legend.pure.m3.tools.matcher.MatchRunner;
import org.finos.legend.pure.m3.tools.matcher.Matcher;
import org.finos.legend.pure.m3.tools.matcher.MatcherState;
import org.finos.legend.pure.m4.ModelRepository;
import org.finos.legend.pure.m4.exception.PureCompilationException;

public class AssociationUnloaderWalk implements MatchRunner<Association>
{
    @Override
    public String getClassName()
    {
        return M3Paths.Association;
    }

    @Override
    public void run(Association association, MatcherState state, Matcher matcher, ModelRepository modelRepository, Context context) throws PureCompilationException
    {
        WalkerState walkerState = (WalkerState)state;
        ProcessorSupport processorSupport = walkerState.getProcessorSupport();
        walkerState.addInstance(association);

        for (Property<?, ?> property : association._properties())
        {
            Type targetClass = getTargetClass(property, processorSupport);
            if (targetClass != null)
            {
                matcher.match(targetClass, state);
            }
            matcher.fullMatch(property, state);
        }

        for (QualifiedProperty<?> qualifiedProperty : association._qualifiedProperties())
        {
            matcher.fullMatch(qualifiedProperty, state);
        }
    }

    private Type getTargetClass(AbstractProperty property, ProcessorSupport processorSupport)
    {
        try
        {
            return (Type)ImportStub.withImportStubByPass(property._genericType()._rawTypeCoreInstance(), processorSupport);
        }
        catch (PureCompilationException e)
        {
            return null;
        }
    }
}
