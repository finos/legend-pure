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


public class TemporaryPureAggregationFunctionSpecification
{
    public SourceInformation sourceInformation;
    public CoreInstance mapExpression;
    public CoreInstance aggregateExpression;

    TemporaryPureAggregationFunctionSpecification(SourceInformation sourceInformation, CoreInstance mapExpression, CoreInstance aggregateExpression)
    {
        this.sourceInformation = sourceInformation;
        this.mapExpression = mapExpression;
        this.aggregateExpression = aggregateExpression;
    }

    public static TemporaryPureAggregationFunctionSpecification build(SourceInformation sourceInformation, CoreInstance mapExpression, CoreInstance aggregateExpression)
    {
        return new TemporaryPureAggregationFunctionSpecification(sourceInformation, mapExpression, aggregateExpression);
    }
}