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

package org.finos.legend.pure.m3.compiler.postprocessing.processor.milestoning;

import org.eclipse.collections.api.list.ListIterable;

public class MilestonedPropertyMetaData
{
    private final ListIterable<String> classTemporalStereotypes;
    private final ListIterable<String> temporalDatePropertyNamesForStereotypes;

    MilestonedPropertyMetaData(ListIterable<String> classTemporalStereotypes, ListIterable<String> temporalDatePropertyNamesForStereotypes)
    {
        this.classTemporalStereotypes = classTemporalStereotypes;
        this.temporalDatePropertyNamesForStereotypes = temporalDatePropertyNamesForStereotypes;
    }

    public ListIterable<String> getClassTemporalStereotypes()
    {
        return this.classTemporalStereotypes;
    }

    public ListIterable<String> getTemporalDatePropertyNamesForStereotypes()
    {
        return this.temporalDatePropertyNamesForStereotypes;
    }
}
