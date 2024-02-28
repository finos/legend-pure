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
import org.finos.legend.pure.m3.coreinstance.meta.pure.metamodel.type.Type;
import org.finos.legend.pure.m3.coreinstance.meta.pure.metamodel.valuespecification.FunctionExpression;
import org.finos.legend.pure.m3.coreinstance.meta.pure.metamodel.valuespecification.ValueSpecification;
import org.finos.legend.pure.m3.navigation.ProcessorSupport;
import org.finos.legend.pure.m3.navigation.importstub.ImportStub;
import org.finos.legend.pure.m4.coreinstance.CoreInstance;
import org.finos.legend.pure.m4.exception.PureCompilationException;

public class GetAllVersionsInRangeValidator
{
    private GetAllVersionsInRangeValidator()
    {
    }

    public static void validate(FunctionExpression instance, ProcessorSupport processorSupport) throws PureCompilationException
    {
        if (canDetermineGenericType(instance, processorSupport))
        {
            Type sourceRawType = (Type) ImportStub.withImportStubByPass(instance._genericType()._rawTypeCoreInstance(), processorSupport);
            ListIterable<MilestoningStereotypeEnum> temporalStereotypes = MilestoningFunctions.getTemporalStereoTypesFromTopMostNonTopTypeGeneralizations(sourceRawType, processorSupport);
            ListIterable<? extends ValueSpecification> params = instance._parametersValues().toList().drop(1);
            validateAllVersionsInRangeUsage(instance, temporalStereotypes);
            validateLatestDateUsage(instance, params, processorSupport);
        }
    }

    private static boolean canDetermineGenericType(FunctionExpression instance, ProcessorSupport processorSupport)
    {
        return ImportStub.withImportStubByPass(instance._genericType()._rawTypeCoreInstance(), processorSupport) != null;
    }

    private static void validateAllVersionsInRangeUsage(CoreInstance instance, ListIterable<MilestoningStereotypeEnum> temporalStereotypes)
    {
        if (temporalStereotypes.isEmpty() || temporalStereotypes.contains(MilestoningStereotypeEnum.bitemporal))
        {
            throw new PureCompilationException(instance.getSourceInformation(), ".allVersionsInRange() is applicable only for businessTemporal and processingTemporal types");
        }
    }

    private static void validateLatestDateUsage(FunctionExpression instance, ListIterable<? extends ValueSpecification> params, ProcessorSupport processorSupport)
    {
        if (params.anySatisfy(p -> MilestoningFunctions.toInstanceValues(p).anySatisfy(v -> MilestoningFunctions.isLatestDate(v, processorSupport))))
        {
            throw new PureCompilationException(instance.getSourceInformation(), "%latest not a valid parameter for .allVersionsInRange()");
        }
    }
}
