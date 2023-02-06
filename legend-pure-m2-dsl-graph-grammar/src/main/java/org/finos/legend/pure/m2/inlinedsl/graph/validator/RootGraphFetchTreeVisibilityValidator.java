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

package org.finos.legend.pure.m2.inlinedsl.graph.validator;

import org.finos.legend.pure.m2.inlinedsl.graph.M2GraphPaths;
import org.finos.legend.pure.m3.compiler.Context;
import org.finos.legend.pure.m3.compiler.validation.ValidatorState;
import org.finos.legend.pure.m3.compiler.validation.VisibilityValidation;
import org.finos.legend.pure.m3.coreinstance.meta.pure.metamodel.function.property.AbstractProperty;
import org.finos.legend.pure.m3.coreinstance.meta.pure.metamodel.type.Class;
import org.finos.legend.pure.m3.navigation.Instance;
import org.finos.legend.pure.m3.navigation.M3Properties;
import org.finos.legend.pure.m3.navigation.ProcessorSupport;
import org.finos.legend.pure.m3.serialization.grammar.m3parser.inlinedsl.VisibilityValidator;
import org.finos.legend.pure.m4.coreinstance.CoreInstance;
import org.finos.legend.pure.m4.exception.PureCompilationException;

public class RootGraphFetchTreeVisibilityValidator implements VisibilityValidator
{
    @Override
    public void validate(CoreInstance value, CoreInstance pkg, String sourceId, Context context, ValidatorState validatorState, ProcessorSupport processorSupport) throws PureCompilationException
    {
        if (processorSupport.package_getByUserPath(M2GraphPaths.RootGraphFetchTree) != null && Instance.instanceOf(value, M2GraphPaths.RootGraphFetchTree, processorSupport))
        {
            validateRootGraphFetchTree(value, pkg, sourceId, context, validatorState, processorSupport);
        }
    }

    //TODO: move to m2-graph
    private static void validateRootGraphFetchTree(CoreInstance rootTree, CoreInstance pkg, String sourceId, Context context, ValidatorState validatorState, ProcessorSupport processorSupport) throws PureCompilationException
    {
        VisibilityValidation.validatePackageAndSourceVisibility(rootTree.getValueForMetaPropertyToOne(M3Properties._class).getSourceInformation(), pkg, sourceId, context, validatorState, processorSupport, (Class<?>) Instance.getValueForMetaPropertyToOneResolved(rootTree, M3Properties._class, processorSupport));
        Instance.getValueForMetaPropertyToManyResolved(rootTree, "subTrees", processorSupport).forEach(subTree -> validatePropertyGraphFetchTree(subTree, pkg, sourceId, context, validatorState, processorSupport));
    }

    private static void validatePropertyGraphFetchTree(CoreInstance propertyTree, CoreInstance pkg, String sourceId, Context context, ValidatorState validatorState, ProcessorSupport processorSupport) throws PureCompilationException
    {
        VisibilityValidation.validatePackageAndSourceVisibility(propertyTree.getValueForMetaPropertyToOne(M3Properties.property).getSourceInformation(), pkg, sourceId, context, validatorState, processorSupport, (AbstractProperty<?>) Instance.getValueForMetaPropertyToOneResolved(propertyTree, M3Properties.property, processorSupport));
        CoreInstance subTypeClass = Instance.getValueForMetaPropertyToOneResolved(propertyTree, "subType", processorSupport);
        if (subTypeClass != null)
        {
            VisibilityValidation.validatePackageAndSourceVisibility(propertyTree.getValueForMetaPropertyToOne("subType").getSourceInformation(), pkg, sourceId, context, validatorState, processorSupport, (Class<?>) subTypeClass);
        }
        Instance.getValueForMetaPropertyToManyResolved(propertyTree, "subTrees", processorSupport).forEach(subTree -> validatePropertyGraphFetchTree(subTree, pkg, sourceId, context, validatorState, processorSupport));
    }
}
