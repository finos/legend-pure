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

package org.finos.legend.pure.m3.compiler.postprocessing;

import org.eclipse.collections.api.RichIterable;
import org.finos.legend.pure.m3.navigation.importstub.ImportStub;
import org.finos.legend.pure.m3.coreinstance.meta.pure.metamodel.relationship.Generalization;
import org.finos.legend.pure.m3.coreinstance.meta.pure.metamodel.type.Type;
import org.finos.legend.pure.m3.navigation.ProcessorSupport;
import org.finos.legend.pure.m4.coreinstance.CoreInstance;
import org.finos.legend.pure.m4.exception.PureCompilationException;

public class SpecializationProcessor
{
    public static void process(Type instance, ProcessorSupport processorSupport)
    {
        RichIterable<? extends Generalization> generalizations = instance._generalizations();
        if (generalizations.notEmpty())
        {
            CoreInstance topType = processorSupport.type_TopType();
            for (Generalization generalization : generalizations)
            {
                Type general = (Type)ImportStub.withImportStubByPass(generalization._general()._rawTypeCoreInstance(), processorSupport);
                if (general == null)
                {
                    throw new PureCompilationException(instance.getSourceInformation(), "Error accessing generalizations for " + instance);
                }
                if (general != topType)
                {
                    RichIterable<? extends Generalization> specializations = general._specializations();
                    if (!specializations.contains(generalization))
                    {
                        general._specializationsAdd(generalization);
                    }
                }
            }
        }
    }
}
