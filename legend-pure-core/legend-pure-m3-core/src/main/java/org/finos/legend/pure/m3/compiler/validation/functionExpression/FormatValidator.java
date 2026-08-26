// Copyright 2026 Goldman Sachs
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

import org.eclipse.collections.api.RichIterable;
import org.eclipse.collections.api.list.ListIterable;
import org.finos.legend.pure.m3.coreinstance.meta.pure.metamodel.valuespecification.FunctionExpression;
import org.finos.legend.pure.m3.coreinstance.meta.pure.metamodel.valuespecification.InstanceValue;
import org.finos.legend.pure.m3.coreinstance.meta.pure.metamodel.valuespecification.ValueSpecification;
import org.finos.legend.pure.m3.navigation.M3Paths;
import org.finos.legend.pure.m3.navigation.PrimitiveUtilities;
import org.finos.legend.pure.m3.navigation.ProcessorSupport;
import org.finos.legend.pure.m3.tools.FormatTools;
import org.finos.legend.pure.m4.coreinstance.CoreInstance;
import org.finos.legend.pure.m4.exception.PureCompilationException;

/**
 * Checks a literal format string where {@code format} is called with one, so a format string that
 * could never be used is a compilation error rather than something waiting for the line to be
 * executed.
 *
 * <p>Only a literal can be checked here. A format string that is computed reaches this as something
 * other than a literal and is left alone, which is why the run-time check stays: the format strings
 * the relational stores build for their SQL literals are assembled from a time zone and a
 * granularity, and are never literal at any call site.
 *
 * <p>What is checked is what {@link FormatTools#validate} checks, which is the format string alone.
 * Whether the specifiers and the arguments agree, in number or in kind, is a different question with
 * a different answer at every call site, and is left to the run time on purpose.
 */
public class FormatValidator
{
    public static void validateFormat(FunctionExpression instance, ProcessorSupport processorSupport) throws PureCompilationException
    {
        ListIterable<? extends ValueSpecification> parametersValues = instance._parametersValues().toList();
        if (parametersValues.isEmpty() || !(parametersValues.get(0) instanceof InstanceValue))
        {
            return;
        }

        RichIterable<? extends CoreInstance> values = ((InstanceValue) parametersValues.get(0))._valuesCoreInstance();
        if (values.size() != 1)
        {
            return;
        }

        CoreInstance value = values.getFirst();
        if ((value != null) && processorSupport.instance_instanceOf(value, M3Paths.String))
        {
            try
            {
                FormatTools.validate(PrimitiveUtilities.getStringValue(value));
            }
            catch (IllegalArgumentException e)
            {
                throw new PureCompilationException(instance.getSourceInformation(), e.getMessage(), e);
            }
        }
    }
}
