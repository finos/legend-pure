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
import org.finos.legend.pure.m4.coreinstance.CoreInstance;
import org.finos.legend.pure.m4.exception.PureCompilationException;

public class MilestoningDates
{
    private final CoreInstance businessDate;
    private final CoreInstance processingDate;

    public MilestoningDates(MilestoningStereotype stereotype, ListIterable<? extends CoreInstance> temporalParameterValues)
    {
        if (stereotype == MilestoningStereotypeEnum.businesstemporal)
        {
            this.businessDate = temporalParameterValues.get(0);
            this.processingDate = null;
        }
        else if (stereotype == MilestoningStereotypeEnum.processingtemporal)
        {
            this.businessDate = null;
            this.processingDate = temporalParameterValues.get(0);
        }
        else if (stereotype == MilestoningStereotypeEnum.bitemporal)
        {
            this.businessDate = temporalParameterValues.get(1);
            this.processingDate = temporalParameterValues.get(0);
        }
        else
        {
            this.businessDate = null;
            this.processingDate = null;
        }
    }

    public MilestoningDates(CoreInstance businessDate, CoreInstance processingDate)
    {
        this.businessDate = businessDate;
        this.processingDate = processingDate;
    }

    public CoreInstance getBusinessDate()
    {
        return this.businessDate;
    }

    public CoreInstance getProcessingDate()
    {
        return this.processingDate;
    }

    public CoreInstance getMilestoningDate(MilestoningStereotype milestoningDateType)
    {
        if (milestoningDateType == MilestoningStereotypeEnum.businesstemporal)
        {
            return this.businessDate;
        }
        if (milestoningDateType == MilestoningStereotypeEnum.processingtemporal)
        {
            return this.processingDate;
        }
        throw new PureCompilationException("Unexpected milestoning type encountered during propagation: " + milestoningDateType);
    }
}
