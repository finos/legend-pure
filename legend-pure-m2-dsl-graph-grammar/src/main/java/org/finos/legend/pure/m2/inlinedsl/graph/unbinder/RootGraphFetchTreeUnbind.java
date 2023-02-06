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

package org.finos.legend.pure.m2.inlinedsl.graph.unbinder;

import org.finos.legend.pure.m2.inlinedsl.graph.M2GraphPaths;
import org.finos.legend.pure.m3.compiler.Context;
import org.finos.legend.pure.m3.compiler.ReferenceUsage;
import org.finos.legend.pure.m3.compiler.unload.unbind.Shared;
import org.finos.legend.pure.m3.coreinstance.meta.pure.graphFetch.GraphFetchTree;
import org.finos.legend.pure.m3.coreinstance.meta.pure.graphFetch.PropertyGraphFetchTree;
import org.finos.legend.pure.m3.coreinstance.meta.pure.graphFetch.RootGraphFetchTree;
import org.finos.legend.pure.m3.coreinstance.meta.pure.metamodel._import.ImportStub;
import org.finos.legend.pure.m3.coreinstance.meta.pure.metamodel._import.PropertyStub;
import org.finos.legend.pure.m3.coreinstance.meta.pure.metamodel.valuespecification.InstanceValue;
import org.finos.legend.pure.m3.coreinstance.meta.pure.metamodel.valuespecification.ValueSpecification;
import org.finos.legend.pure.m3.navigation.ProcessorSupport;
import org.finos.legend.pure.m3.tools.matcher.MatchRunner;
import org.finos.legend.pure.m3.tools.matcher.Matcher;
import org.finos.legend.pure.m3.tools.matcher.MatcherState;
import org.finos.legend.pure.m4.coreinstance.CoreInstance;
import org.finos.legend.pure.m4.ModelRepository;
import org.finos.legend.pure.m4.exception.PureCompilationException;

public class RootGraphFetchTreeUnbind implements MatchRunner<RootGraphFetchTree>
{
    @Override
    public String getClassName()
    {
        return M2GraphPaths.RootGraphFetchTree;
    }

    @Override
    public void run(RootGraphFetchTree instance, MatcherState state, Matcher matcher, ModelRepository modelRepository, Context context) throws PureCompilationException
    {
        ProcessorSupport processorSupport = state.getProcessorSupport();
        for(GraphFetchTree subTree : instance._subTrees())
        {
            this.unbindPropertyGraphFetchTree((PropertyGraphFetchTree) subTree, instance, state, matcher);
        }

        ImportStub _class = (ImportStub) instance._classCoreInstance();
        CoreInstance resolved = _class._resolvedNodeCoreInstance();
        if (resolved != null)
        {
            ReferenceUsage.removeReferenceUsagesForUser(resolved, instance, state.getProcessorSupport());
        }
        Shared.cleanImportStub(_class, processorSupport);
        instance._classifierGenericTypeRemove();
    }

    private void unbindPropertyGraphFetchTree(PropertyGraphFetchTree propertyGraphFetchTree, RootGraphFetchTree mainTree, MatcherState state, Matcher matcher)
    {
        ProcessorSupport processorSupport = state.getProcessorSupport();

        PropertyStub property = (PropertyStub) propertyGraphFetchTree._propertyCoreInstance();
        CoreInstance resolved = property._resolvedPropertyCoreInstance();
        if (resolved != null)
        {
            ReferenceUsage.removeReferenceUsagesForUser(resolved, mainTree, state.getProcessorSupport());
        }

        Shared.cleanPropertyStub(property, processorSupport);
        property._ownerRemove();


        for (ValueSpecification vs : propertyGraphFetchTree._parameters())
        {
            if (vs instanceof InstanceValue)
            {
                for (CoreInstance value : ((InstanceValue) vs)._valuesCoreInstance())
                {
                    Shared.cleanEnumStub(value, processorSupport);
                }
                vs._genericTypeRemove();
                vs._multiplicityRemove();
            }
            matcher.fullMatch(vs, state);
        }

        for(GraphFetchTree subTree : propertyGraphFetchTree._subTrees())
        {
            this.unbindPropertyGraphFetchTree((PropertyGraphFetchTree) subTree, mainTree, state, matcher);
        }

        ImportStub subTypeClass = (ImportStub) propertyGraphFetchTree._subTypeCoreInstance();
        if (subTypeClass != null)
        {
            CoreInstance resolvedSubTypeClass = subTypeClass._resolvedNodeCoreInstance();
            if (resolvedSubTypeClass != null)
            {
                ReferenceUsage.removeReferenceUsagesForUser(resolvedSubTypeClass, mainTree, state.getProcessorSupport());
            }
            Shared.cleanImportStub(subTypeClass, processorSupport);
        }

        propertyGraphFetchTree._classifierGenericTypeRemove();
    }
}
