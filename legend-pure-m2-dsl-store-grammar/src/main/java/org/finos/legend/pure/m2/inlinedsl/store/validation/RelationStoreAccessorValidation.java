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

package org.finos.legend.pure.m2.inlinedsl.store.validation;

import org.finos.legend.pure.m2.dsl.store.M2StorePaths;
import org.finos.legend.pure.m3.compiler.Context;
import org.finos.legend.pure.m3.compiler.validation.ValidatorState;
import org.finos.legend.pure.m3.navigation.Instance;
import org.finos.legend.pure.m3.navigation.ProcessorSupport;
import org.finos.legend.pure.m3.serialization.grammar.m3parser.inlinedsl.VisibilityValidator;
import org.finos.legend.pure.m4.coreinstance.CoreInstance;
import org.finos.legend.pure.m4.exception.PureCompilationException;

public class RelationStoreAccessorValidation implements VisibilityValidator
{
    @Override
    public void validate(CoreInstance value, CoreInstance pkg, String sourceId, Context context, ValidatorState validatorState, ProcessorSupport processorSupport) throws PureCompilationException
    {
        if (processorSupport.package_getByUserPath(M2StorePaths.RelationStoreAccessor) != null && Instance.instanceOf(value, M2StorePaths.RelationStoreAccessor, processorSupport))
        {
            validatePath(value, pkg, sourceId, context, validatorState, processorSupport);
        }
    }

    private static void validatePath(CoreInstance path, CoreInstance pkg, String sourceId, Context context, ValidatorState validatorState, ProcessorSupport processorSupport) throws PureCompilationException
    {
    }
}