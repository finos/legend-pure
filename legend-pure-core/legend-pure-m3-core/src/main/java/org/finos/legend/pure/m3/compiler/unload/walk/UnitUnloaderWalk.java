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
import org.finos.legend.pure.m3.coreinstance.meta.pure.metamodel.type.Unit;
import org.finos.legend.pure.m3.tools.matcher.MatchRunner;
import org.finos.legend.pure.m3.tools.matcher.Matcher;
import org.finos.legend.pure.m3.tools.matcher.MatcherState;
import org.finos.legend.pure.m4.ModelRepository;
import org.finos.legend.pure.m4.exception.PureCompilationException;

public class UnitUnloaderWalk implements MatchRunner<Unit>
{
    @Override
    public void run(Unit unit, MatcherState state, Matcher matcher, ModelRepository modelRepository, Context context) throws PureCompilationException
    {
        WalkerState walkerState = (WalkerState)state;
        walkerState.addInstance(unit);
        if (unit._conversionFunction() != null)
        {
            matcher.fullMatch(unit._conversionFunction(), state);
        }
        if (unit._measure() != null)
        {
            matcher.fullMatch(unit._measure(), state);
        }
    }

    @Override
    public String getClassName()
    {
        return M3Paths.Unit;
    }
}
