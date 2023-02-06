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

package org.finos.legend.pure.runtime.java.interpreted.natives;

import org.eclipse.collections.api.block.procedure.Procedure;
import org.eclipse.collections.api.list.MutableList;
import org.eclipse.collections.api.map.MutableMap;
import org.eclipse.collections.api.multimap.MutableMultimap;
import org.eclipse.collections.api.stack.MutableStack;
import org.eclipse.collections.impl.factory.Lists;
import org.eclipse.collections.impl.factory.Maps;
import org.eclipse.collections.impl.factory.Multimaps;
import org.eclipse.collections.impl.factory.Stacks;
import org.finos.legend.pure.m4.coreinstance.CoreInstance;

public class InstantiationContext
{
    private final MutableStack<Object> stack = Stacks.mutable.empty();
    private final MutableMap<Object, CoreInstance> newInstancesByNewFunction = Maps.mutable.empty();
    private final MutableMultimap<CoreInstance, Object> stackObjectByClassifier = Multimaps.mutable.list.empty();
    private final MutableList<Runnable> validations = Lists.mutable.empty();

    public void push(CoreInstance classifier)
    {
        Object object = new Object();
        this.stack.push(object);
        this.stackObjectByClassifier.put(classifier, object);
    }

    public void popAndExecuteProcedures(CoreInstance instance)
    {
        Object stackObject = this.stack.pop();
        this.newInstancesByNewFunction.put(stackObject, instance);
    }

    public boolean isEmpty()
    {
        return this.stack.isEmpty();
    }

    public void runValidations()
    {
        if (this.isEmpty())
        {
            this.validations.each(new Procedure<Runnable>()
            {
                @Override
                public void value(Runnable runnable)
                {
                    runnable.run();
                }
            });
        }
    }

    public void reset()
    {
        this.stack.clear();
        this.newInstancesByNewFunction.clear();
        this.stackObjectByClassifier.clear();
        this.validations.clear();
    }

    public void registerValidation(Runnable runnable)
    {
        this.validations.add(runnable);
    }
}
