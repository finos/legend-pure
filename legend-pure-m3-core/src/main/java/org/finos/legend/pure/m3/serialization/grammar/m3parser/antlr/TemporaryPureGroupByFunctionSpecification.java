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

import org.finos.legend.pure.m4.coreinstance.CoreInstance;
import org.finos.legend.pure.m4.coreinstance.SourceInformation;

public class TemporaryPureGroupByFunctionSpecification
{
    public SourceInformation sourceInformation;
    public CoreInstance groupByExpression;

    TemporaryPureGroupByFunctionSpecification(SourceInformation sourceInformation, CoreInstance groupByExpression)
    {
        this.sourceInformation = sourceInformation;
        this.groupByExpression = groupByExpression;
    }

    public static TemporaryPureGroupByFunctionSpecification build(SourceInformation sourceInformation, CoreInstance groupByExpression)
    {
        return new TemporaryPureGroupByFunctionSpecification(sourceInformation, groupByExpression);
    }
}
