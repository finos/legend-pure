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

package org.finos.legend.pure.m3.compiler.validation.validator;

import org.eclipse.collections.api.RichIterable;
import org.eclipse.collections.api.factory.Sets;
import org.eclipse.collections.api.list.ListIterable;
import org.finos.legend.pure.m3.compiler.postprocessing.processor.milestoning.MilestonedPropertyMetaData;
import org.finos.legend.pure.m3.compiler.postprocessing.processor.milestoning.MilestoningFunctions;
import org.finos.legend.pure.m3.coreinstance.meta.pure.metamodel.function.Function;
import org.finos.legend.pure.m3.coreinstance.meta.pure.metamodel.function.property.QualifiedProperty;
import org.finos.legend.pure.m3.coreinstance.meta.pure.metamodel.valuespecification.FunctionExpression;
import org.finos.legend.pure.m3.navigation.PackageableElement.PackageableElement;
import org.finos.legend.pure.m3.navigation.ProcessorSupport;
import org.finos.legend.pure.m4.ModelRepository;
import org.finos.legend.pure.m4.coreinstance.CoreInstance;
import org.finos.legend.pure.m4.exception.PureCompilationException;

public class MilestoningFunctionExpressionValidator
{
    @Deprecated
    public static void validateFunctionExpression(FunctionExpression functionExpression, Function<?> function, ModelRepository repository, ProcessorSupport processorSupport) throws PureCompilationException
    {
        validateFunctionExpression(functionExpression, function, processorSupport);
    }

    public static void validateFunctionExpression(FunctionExpression functionExpression, Function<?> function, ProcessorSupport processorSupport) throws PureCompilationException
    {
        validateLatestDateUsage(functionExpression, function, processorSupport);
        validateMissingMilestoningDateArguments(functionExpression, function, processorSupport);
    }

    private static void validateLatestDateUsage(FunctionExpression functionExpression, Function<?> function, ProcessorSupport processorSupport)
    {
        RichIterable<CoreInstance> funcParams = functionExpression._parametersValues().flatCollect(MilestoningFunctions::toInstanceValues);
        boolean hasLatestDateParam = funcParams.anySatisfy(p -> MilestoningFunctions.isLatestDate(p, processorSupport));
        boolean isGetAllMilestoningFunction = Sets.immutable.with(MilestoningFunctions.MILESTONING_GET_ALL_FUNCTION_PATH, MilestoningFunctions.BITEMPORAL_MILESTONING_GET_ALL_FUNCTION_PATH).contains(PackageableElement.getUserPathForPackageableElement(function));
        boolean isGeneratedProperty = MilestoningFunctions.isGeneratedMilestoningProperty(function, processorSupport);
        if (hasLatestDateParam && !(isGetAllMilestoningFunction || isGeneratedProperty))
        {
            throw new PureCompilationException(functionExpression.getSourceInformation(), "%latest may only be used as an argument to milestoned properties");
        }
        if (hasLatestDateParam && MilestoningFunctions.isAllVersionsInRangeProperty(function, processorSupport))
        {
            throw new PureCompilationException(functionExpression.getSourceInformation(), "%latest not a valid parameter for AllVersionsInRange()");
        }
    }

    private static void validateMissingMilestoningDateArguments(FunctionExpression functionExpression, Function<?> function, ProcessorSupport processorSupport)
    {
        if (MilestoningFunctions.isGeneratedMilestonedQualifiedPropertyWithMissingDates(function, processorSupport))
        {
            MilestonedPropertyMetaData milestoningPropertyMetaData = MilestoningFunctions.getMilestonedMetaDataForProperty((QualifiedProperty<?>) function, processorSupport);
            ListIterable<String> temporalPropertyNames = milestoningPropertyMetaData.getTemporalDatePropertyNamesForStereotypes();
            throw new PureCompilationException(functionExpression.getSourceInformation(), "No-Arg milestoned property: '" + function._functionName() + "' must be either called in a milestoning context or supplied with " + temporalPropertyNames.makeString("[", ", ", "]") + " parameters");
        }
    }
}
