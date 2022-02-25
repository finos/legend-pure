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

import org.eclipse.collections.api.RichIterable;
import org.finos.legend.pure.m3.compiler.ClassPropertyOwnerStrategy;
import org.finos.legend.pure.m3.navigation.M3Paths;
import org.finos.legend.pure.m3.compiler.Context;
import org.finos.legend.pure.m3.compiler.PropertyOwnerStrategy;
import org.finos.legend.pure.m3.coreinstance.meta.pure.metamodel.type.Class;
import org.finos.legend.pure.m3.tools.matcher.MatchRunner;
import org.finos.legend.pure.m3.tools.matcher.Matcher;
import org.finos.legend.pure.m3.tools.matcher.MatcherState;
import org.finos.legend.pure.m4.coreinstance.CoreInstance;
import org.finos.legend.pure.m4.ModelRepository;
import org.finos.legend.pure.m4.exception.PureCompilationException;

public class ClassUnbind implements MatchRunner<Class<?>>
{
    @Override
    public String getClassName()
    {
        return M3Paths.Class;
    }

    @Override
    public void run(Class<?> _class, MatcherState state, Matcher matcher, ModelRepository modelRepository, Context context) throws PureCompilationException
    {
        this.unbindClassProperties(_class, state, matcher);
        PropertyOwnerStrategy strategy = ClassPropertyOwnerStrategy.CLASS_PROPERTY_OWNER_STRATEGY;
        MilestoningUnbind.removeGeneratedMilestoningProperties(_class, state.getProcessorSupport(), _class._properties(), strategy::propertiesRemove, (c, p) -> strategy.setProperties(c, p));
        MilestoningUnbind.removeGeneratedMilestoningProperties(_class, state.getProcessorSupport(), _class._qualifiedProperties(), strategy::qualifiedPropertiesRemove, (c, p) -> strategy.setQualifiedProperties(c, p));
        MilestoningUnbind.removeGeneratedMilestoningProperties(_class, state.getProcessorSupport(), _class._originalMilestonedProperties(), strategy::originalMilestonedPropertiesRemove, (c, p) -> strategy.setOriginalMilestonedProperties(c, p));
        MilestoningUnbind.undoMoveProcessedOriginalMilestonedProperties(_class, context);
    }

    private void unbindClassProperties(Class<?> _class, MatcherState state, Matcher matcher)
    {
        this.unbindValues(matcher, state, _class._properties());
        this.unbindValues(matcher, state, _class._qualifiedProperties());
        this.unbindValues(matcher, state, _class._originalMilestonedProperties());
    }

    private void unbindValues(Matcher matcher, MatcherState state, RichIterable<? extends CoreInstance> values)
    {
        values.forEach(v -> matcher.fullMatch(v, state));
    }
}
