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

package org.finos.legend.pure.m4.statelistener;

import org.eclipse.collections.api.set.SetIterable;
import org.finos.legend.pure.m4.coreinstance.CoreInstance;

public class VoidM4StateListener implements M4StateListener
{
    @Override
    public void startParsingM4(String fileLocation)
    {
    }

    @Override
    public void finishedParsingM4(String fileLocation)
    {
    }

    @Override
    public void startParsingClassM4(String fileLocation)
    {
    }

    @Override
    public void finishedParsingClassM4(String fileLocation)
    {
    }

    @Override
    public void startRepositorySimpleValidation()
    {
    }

    @Override
    public void finishedRepositorySimpleValidation(SetIterable<? extends CoreInstance> visitedNodes)
    {
    }
}
