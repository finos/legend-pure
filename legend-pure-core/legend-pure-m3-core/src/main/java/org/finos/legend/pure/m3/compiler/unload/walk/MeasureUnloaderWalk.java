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

import org.finos.legend.pure.m3.navigation.M3Paths;
import org.finos.legend.pure.m3.compiler.Context;
import org.finos.legend.pure.m3.coreinstance.meta.pure.metamodel.type.Measure;
import org.finos.legend.pure.m3.tools.matcher.MatchRunner;
import org.finos.legend.pure.m3.tools.matcher.Matcher;
import org.finos.legend.pure.m3.tools.matcher.MatcherState;
import org.finos.legend.pure.m4.coreinstance.CoreInstance;
import org.finos.legend.pure.m4.ModelRepository;
import org.finos.legend.pure.m4.exception.PureCompilationException;

public class MeasureUnloaderWalk implements MatchRunner<Measure>
{
    @Override
    public String getClassName()
    {
        return M3Paths.Measure;
    }

    @Override
    public void run(Measure measure, MatcherState state, Matcher matcher, ModelRepository modelRepository, Context context) throws PureCompilationException
    {
        WalkerState walkerState = (WalkerState)state;
        walkerState.addInstance(measure);

        if (measure._canonicalUnit() != null)
        {
            matcher.fullMatch(measure._canonicalUnit(), state);
        }
        if (measure._nonCanonicalUnits() != null)
        {
            for (CoreInstance value : measure._nonCanonicalUnits())
            {
                matcher.fullMatch(value, state);
            }
        }
    }
}
