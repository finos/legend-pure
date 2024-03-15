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

import org.finos.legend.pure.m3.navigation.M3Paths;
import org.finos.legend.pure.m3.compiler.Context;
import org.finos.legend.pure.m3.navigation.Instance;
import org.finos.legend.pure.m3.compiler.validation.Validator;
import org.finos.legend.pure.m3.compiler.validation.ValidatorState;
import org.finos.legend.pure.m3.coreinstance.Package;
import org.finos.legend.pure.m3.coreinstance.meta.pure.metamodel.PackageableElement;
import org.finos.legend.pure.m3.navigation.ProcessorSupport;
import org.finos.legend.pure.m3.tools.matcher.MatchRunner;
import org.finos.legend.pure.m3.tools.matcher.Matcher;
import org.finos.legend.pure.m3.tools.matcher.MatcherState;
import org.finos.legend.pure.m4.ModelRepository;
import org.finos.legend.pure.m4.exception.PureCompilationException;

public class PackageValidator implements MatchRunner<Package>
{
    @Override
    public String getClassName()
    {
        return M3Paths.Package;
    }

    @Override
    public void run(Package pkg, MatcherState state, Matcher matcher, ModelRepository modelRepository, Context context) throws PureCompilationException
    {
        ValidatorState validatorState = (ValidatorState)state;
        ProcessorSupport processorSupport = validatorState.getProcessorSupport();

        Package parent = pkg._package();
        if (parent != null)
        {
            Validator.validate(parent, validatorState, matcher, processorSupport);
        }

        for (PackageableElement child : pkg._children())
        {
            if (!Instance.instanceOf(child, M3Paths.PackageableElement, processorSupport))
            {
                StringBuilder message = new StringBuilder("Child of package ");
                org.finos.legend.pure.m3.navigation.PackageableElement.PackageableElement.writeUserPathForPackageableElement(message, pkg);
                message.append(" is an instance of ");
                org.finos.legend.pure.m3.navigation.PackageableElement.PackageableElement.writeUserPathForPackageableElement(message, child.getClassifier());
                message.append(" which is not a subclass of ");
                message.append(M3Paths.PackageableElement);
                message.append(": ");
                message.append(child);
                throw new PureCompilationException(child.getSourceInformation(), message.toString());
            }
            Validator.validate(child, validatorState, matcher, processorSupport);
        }
    }
}
