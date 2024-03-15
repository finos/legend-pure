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

package org.finos.legend.pure.m3.compiler.validation.functionExpression;

import org.eclipse.collections.api.list.ListIterable;
import org.finos.legend.pure.m3.compiler.postprocessing.processor.milestoning.MilestoningFunctions;
import org.finos.legend.pure.m3.compiler.postprocessing.processor.milestoning.MilestoningStereotypeEnum;
import org.finos.legend.pure.m3.navigation.importstub.ImportStub;
import org.finos.legend.pure.m3.coreinstance.meta.pure.metamodel.type.Type;
import org.finos.legend.pure.m3.coreinstance.meta.pure.metamodel.valuespecification.FunctionExpression;
import org.finos.legend.pure.m3.navigation.ProcessorSupport;
import org.finos.legend.pure.m4.exception.PureCompilationException;

public class GetAllVersionsValidator
{
    public static void validate(FunctionExpression instance, ProcessorSupport processorSupport) throws PureCompilationException
    {
        if (canDetermineGenericType(instance, processorSupport))
        {
            Type sourceRawType = (Type)ImportStub.withImportStubByPass(instance._genericType()._rawTypeCoreInstance(), processorSupport);
            ListIterable<MilestoningStereotypeEnum> temporalStereotypes = MilestoningFunctions.getTemporalStereoTypesFromTopMostNonTopTypeGeneralizations(sourceRawType, processorSupport);
            validateGenericTypeIsTemporal(instance, sourceRawType, temporalStereotypes);
        }
    }

    static boolean canDetermineGenericType(FunctionExpression instance, ProcessorSupport processorSupport)
    {
        return ImportStub.withImportStubByPass(instance._genericType()._rawTypeCoreInstance(), processorSupport) != null;
    }

    private static void validateGenericTypeIsTemporal(FunctionExpression instance, Type sourceRawType, ListIterable<MilestoningStereotypeEnum> temporalStereotypes)
    {
        if (temporalStereotypes.isEmpty())
        {
            throw new PureCompilationException(instance.getSourceInformation(), "The function 'getAllVersions' may only be used with temporal types: processingtemporal & businesstemporal, the type " + sourceRawType.getName() + " is  not temporal");
        }
    }

}
