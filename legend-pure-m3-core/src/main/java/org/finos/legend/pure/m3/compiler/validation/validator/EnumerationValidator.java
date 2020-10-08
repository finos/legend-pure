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
import org.eclipse.collections.api.set.MutableSet;
import org.eclipse.collections.impl.factory.Sets;
import org.finos.legend.pure.m3.navigation.M3Paths;
import org.finos.legend.pure.m3.compiler.Context;
import org.finos.legend.pure.m3.navigation.PackageableElement.PackageableElement;
import org.finos.legend.pure.m3.coreinstance.meta.pure.metamodel.type.Enumeration;
import org.finos.legend.pure.m3.tools.matcher.MatchRunner;
import org.finos.legend.pure.m3.tools.matcher.Matcher;
import org.finos.legend.pure.m3.tools.matcher.MatcherState;
import org.finos.legend.pure.m4.coreinstance.CoreInstance;
import org.finos.legend.pure.m4.ModelRepository;
import org.finos.legend.pure.m4.exception.PureCompilationException;

public class EnumerationValidator implements MatchRunner<Enumeration>
{
    @Override
    public String getClassName()
    {
        return M3Paths.Enumeration;
    }

    @Override
    public void run(Enumeration enumeration, MatcherState state, Matcher matcher, ModelRepository modelRepository, Context context) throws PureCompilationException
    {
        RichIterable<? extends CoreInstance> values = enumeration._values();
        if (values.notEmpty())
        {
            MutableSet<String> enumNames = Sets.mutable.with();
            MutableSet<String> duplicatedEnumNames = Sets.mutable.with();
            for (CoreInstance value : values)
            {
                String name = value.getName();
                if (!enumNames.add(name))
                {
                    duplicatedEnumNames.add(name);
                }
            }
            if (duplicatedEnumNames.notEmpty())
            {
                StringBuilder message = new StringBuilder("Enumeration ");
                PackageableElement.writeUserPathForPackageableElement(message, enumeration, "::");
                message.append(" has duplicate values: ");
                duplicatedEnumNames.toSortedList().appendString(message, ", ");
                throw new PureCompilationException(enumeration.getSourceInformation(), message.toString());
            }
        }
    }
}