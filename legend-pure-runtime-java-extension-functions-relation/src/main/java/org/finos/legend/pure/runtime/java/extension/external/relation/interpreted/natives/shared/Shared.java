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

package org.finos.legend.pure.runtime.java.extension.external.relation.interpreted.natives.shared;

import io.deephaven.csv.CsvSpecs;
import io.deephaven.csv.reading.CsvReader;
import io.deephaven.csv.sinks.SinkFactory;
import org.eclipse.collections.api.list.ListIterable;
import org.finos.legend.pure.m3.navigation.ProcessorSupport;
import org.finos.legend.pure.m4.ModelRepository;
import org.finos.legend.pure.m4.coreinstance.CoreInstance;
import org.finos.legend.pure.runtime.java.extension.external.relation.shared.TDS;
import org.finos.legend.pure.runtime.java.interpreted.FunctionExecutionInterpreted;
import org.finos.legend.pure.runtime.java.interpreted.natives.NativeFunction;

import java.io.ByteArrayInputStream;

public abstract class Shared extends NativeFunction
{
    protected final ModelRepository repository;
    protected final FunctionExecutionInterpreted functionExecution;

    public Shared(FunctionExecutionInterpreted functionExecution, ModelRepository repository)
    {
        this.repository = repository;
        this.functionExecution = functionExecution;
    }

    public TDS getTDS(ListIterable<? extends CoreInstance> params, ProcessorSupport processorSupport)
    {
        TDS tds;
        CoreInstance obj = params.get(0).getValueForMetaPropertyToOne("values");
        return obj instanceof TDSCoreInstance ?
                ((TDSCoreInstance) obj).getTDS() :
                new TDS(readCsv((obj.getValueForMetaPropertyToOne("csv")).getName()), repository, processorSupport);

    }

    public CsvReader.Result readCsv(String csv)
    {
        try
        {
            return CsvReader.read(CsvSpecs.csv(), new ByteArrayInputStream(csv.getBytes()), SinkFactory.arrays());
        }
        catch (Exception e)
        {
            throw new RuntimeException(e);
        }
    }
}