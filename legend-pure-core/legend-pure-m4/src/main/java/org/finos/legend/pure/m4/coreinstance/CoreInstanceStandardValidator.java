// Copyright 2025 Goldman Sachs
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

package org.finos.legend.pure.m4.coreinstance;

import org.eclipse.collections.api.factory.Stacks;
import org.eclipse.collections.api.list.ListIterable;
import org.eclipse.collections.api.set.MutableSet;
import org.eclipse.collections.api.stack.MutableStack;
import org.finos.legend.pure.m4.exception.PureCompilationException;
import org.finos.legend.pure.m4.exception.PureException;

public class CoreInstanceStandardValidator
{
    public static void validate(CoreInstance instance, MutableSet<CoreInstance> doneList)
    {
        validate(doneList, Stacks.mutable.with(instance));
    }

    private static void validate(MutableSet<CoreInstance> doneList, MutableStack<CoreInstance> stack) throws PureCompilationException
    {
        while (stack.notEmpty())
        {
            CoreInstance element = stack.pop();
            doneList.add(element);

            if (element.getClassifier() == null)
            {
                SourceInformation foundSourceInformation = element.getSourceInformation();
                if (stack.size() > 1)
                {
                    SourceInformation cursorSourceInformation = element.getSourceInformation();
                    int cursor = stack.size() - 1;
                    while (cursorSourceInformation == null && cursor >= 0)
                    {
                        cursorSourceInformation = stack.peekAt(cursor--).getSourceInformation();
                    }
                    foundSourceInformation = stack.peekAt(cursor + 1).getSourceInformation();
                }
                throw new PureCompilationException(foundSourceInformation, element.getName() + " has not been defined!");
            }

            try
            {
                element.getKeys().forEach(keyName ->
                {
                    ListIterable<String> realKey = element.getRealKeyByName(keyName);
                    if (realKey == null)
                    {
                        throw new RuntimeException("No real key can be found for '" + keyName + "' in\n" + element.getName() + " (" + element + ")");
                    }

                    CoreInstance key = element.getKeyByName(keyName);
                    if (key.getClassifier() == null)
                    {
                        StringBuilder builder = new StringBuilder("'").append(key.getName()).append("' used in '").append(element.getName()).append("' has not been defined!\n");
                        element.print(builder, "   ");
                        throw new RuntimeException(builder.toString());
                    }

                    ListIterable<? extends CoreInstance> values = element.getValueForMetaPropertyToMany(keyName);
                    if (values != null)
                    {
                        values.forEach(childElement ->
                        {
                            if (!doneList.contains(childElement) || (childElement.getClassifier() == null))
                            {
                                stack.push(childElement);
                            }
                        });
                    }
                });
            }
            catch (Exception e)
            {
                PureException pe = PureException.findPureException(e);
                if (pe != null)
                {
                    throw pe;
                }
                throw e;
            }
        }
    }
}
