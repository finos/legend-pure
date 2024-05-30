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

import org.eclipse.collections.api.block.predicate.Predicate;
import org.eclipse.collections.api.list.ListIterable;
import org.finos.legend.pure.m3.compiler.postprocessing.processor.milestoning.MilestoningFunctions;
import org.finos.legend.pure.m3.compiler.postprocessing.processor.milestoning.MilestoningStereotypeEnum;
import org.finos.legend.pure.m3.coreinstance.meta.pure.metamodel.type.Type;
import org.finos.legend.pure.m3.coreinstance.meta.pure.metamodel.valuespecification.FunctionExpression;
import org.finos.legend.pure.m3.coreinstance.meta.pure.metamodel.valuespecification.InstanceValue;
import org.finos.legend.pure.m3.coreinstance.meta.pure.metamodel.valuespecification.ValueSpecification;
import org.finos.legend.pure.m3.coreinstance.meta.pure.metamodel.valuespecification.VariableExpression;
import org.finos.legend.pure.m3.navigation.M3Paths;
import org.finos.legend.pure.m3.navigation.ProcessorSupport;
import org.finos.legend.pure.m3.navigation.importstub.ImportStub;
import org.finos.legend.pure.m4.coreinstance.CoreInstance;
import org.finos.legend.pure.m4.exception.PureCompilationException;

public class GetAllValidator
{

    public static void validate(FunctionExpression instance, ProcessorSupport processorSupport) throws PureCompilationException
    {
        if (canDetermineGenericType(instance, processorSupport))
        {
            Type sourceRawType = (Type)ImportStub.withImportStubByPass(instance._genericType()._rawTypeCoreInstance(), processorSupport);
            ListIterable<String> temporalPropertyNames = MilestoningFunctions.getTemporalStereoTypePropertyNamesFromTopMostNonTopTypeGeneralizations(sourceRawType, processorSupport);
            ListIterable<MilestoningStereotypeEnum> temporalStereotypes = MilestoningFunctions.getTemporalStereoTypesFromTopMostNonTopTypeGeneralizations(sourceRawType, processorSupport);
            ListIterable<? extends ValueSpecification> params = instance._parametersValues().toList().drop(1);

            validateParameterCount(instance, params, sourceRawType, temporalPropertyNames, temporalStereotypes, processorSupport);
        }
    }

    static boolean canDetermineGenericType(FunctionExpression instance, ProcessorSupport processorSupport)
    {
        return ImportStub.withImportStubByPass(instance._genericType()._rawTypeCoreInstance(), processorSupport) != null;
    }

    private static void validateParameterCount(CoreInstance instance, ListIterable<? extends ValueSpecification> params, CoreInstance sourceRawType, ListIterable<String> temporalPropertyNames, ListIterable<MilestoningStereotypeEnum> temporalStereotypes, ProcessorSupport processorSupport)
    {
        int numberOfDateParameters = getDateParameterCount(params, processorSupport);
        if (!temporalStereotypes.isEmpty() && temporalPropertyNames.size() != numberOfDateParameters)
        {
            throw new PureCompilationException(instance.getSourceInformation(), "The type " + sourceRawType.getName() + " is  " + temporalStereotypes.collect(MilestoningFunctions.GET_PLATFORM_NAME) + ", " + temporalPropertyNames.makeString("[", ",", "]" + " should be supplied as a parameter to all()"));
        }
        else if (temporalStereotypes.isEmpty() && numberOfDateParameters != 0)
        {
            throw new PureCompilationException(instance.getSourceInformation(), "The type " + sourceRawType.getName() + " is not Temporal, Dates should not be supplied to all()");
        }
    }

    private static int getDateParameterCount(ListIterable<? extends ValueSpecification> params, final ProcessorSupport processorSupport)
    {
        return params.select(new Predicate<ValueSpecification>()
        {
            @Override
            public boolean accept(ValueSpecification coreInstance)
            {
                return isDateConstant(coreInstance) || isVariableExpressionReturningADate(coreInstance);
            }

            private boolean isDateConstant(ValueSpecification param)
            {
                return param instanceof InstanceValue && isDateType(param);
            }

            private boolean isVariableExpressionReturningADate(ValueSpecification param)
            {
                return param instanceof VariableExpression && isDateType(param);
            }

            private boolean isDateType(ValueSpecification param)
            {
                String rawTypeName = ImportStub.withImportStubByPass(param._genericType()._rawTypeCoreInstance(), processorSupport).getName();
                return M3Paths.Date.equals(rawTypeName) || M3Paths.DateTime.equals(rawTypeName) || M3Paths.StrictDate.equals(rawTypeName) || M3Paths.LatestDate.equals(rawTypeName);
            }

        }).size();
    }
}
