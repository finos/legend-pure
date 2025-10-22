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

package org.finos.legend.pure.m2.inlinedsl.store.unloader;

import org.finos.legend.pure.m2.dsl.store.M2StorePaths;
import org.finos.legend.pure.m3.compiler.Context;
import org.finos.legend.pure.m3.compiler.ReferenceUsage;
import org.finos.legend.pure.m3.compiler.unload.unbind.Shared;
import org.finos.legend.pure.m3.compiler.unload.unbind.UnbindState;
import org.finos.legend.pure.m3.coreinstance.meta.pure.store.RelationStoreAccessor;
import org.finos.legend.pure.m3.tools.matcher.MatchRunner;
import org.finos.legend.pure.m3.tools.matcher.Matcher;
import org.finos.legend.pure.m3.tools.matcher.MatcherState;
import org.finos.legend.pure.m4.ModelRepository;
import org.finos.legend.pure.m4.coreinstance.CoreInstance;
import org.finos.legend.pure.m4.exception.PureCompilationException;

public class RelationStoreAccessorUnloader implements MatchRunner<RelationStoreAccessor<CoreInstance>>
{
    @Override
    public String getClassName()
    {
        return M2StorePaths.RelationStoreAccessor;
    }

    @Override
    public void run(RelationStoreAccessor<CoreInstance> modelElement, MatcherState state, Matcher matcher, ModelRepository modelRepository, Context context) throws PureCompilationException
    {
        ReferenceUsage.removeReferenceUsagesForUser(modelElement._store(), modelElement, state.getProcessorSupport());
        modelElement._storeRemove();
        modelElement._sourceElementRemove();
        modelElement._sourceElementContainerRemove();
        Shared.cleanUpGenericType(modelElement._classifierGenericType(), (UnbindState) state, state.getProcessorSupport());
        modelElement._classifierGenericTypeRemove();
    }
}
