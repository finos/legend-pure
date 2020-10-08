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

package org.finos.legend.pure.m3.serialization.grammar.m3parser.antlr;

import org.eclipse.collections.api.list.MutableList;
import org.finos.legend.pure.m3.coreinstance.meta.pure.metamodel._import.ImportStub;
import org.finos.legend.pure.m4.coreinstance.CoreInstance;

public class TemporaryPureSetImplementation
{
    public ImportStub _class;
    public CoreInstance filter;
    public MutableList<TemporaryPurePropertyMapping> propertyMappings;

    TemporaryPureSetImplementation(ImportStub _class, CoreInstance filter, MutableList<TemporaryPurePropertyMapping> propertyMappings)
    {
        this._class = _class;
        this.filter = filter;
        this.propertyMappings = propertyMappings;
    }

    public static TemporaryPureSetImplementation build(ImportStub _class, CoreInstance filter, MutableList<TemporaryPurePropertyMapping> propertyMappings)
    {
        return new TemporaryPureSetImplementation(_class, filter, propertyMappings);
    }
}
