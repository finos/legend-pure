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

package org.finos.legend.pure.m4.coreinstance.primitive;

import org.finos.legend.pure.m4.ModelRepository;
import org.finos.legend.pure.m4.coreinstance.AbstractCoreInstance;
import org.finos.legend.pure.m4.coreinstance.CoreInstance;
import org.finos.legend.pure.m4.tools.SafeAppendable;

public abstract class AbstractPrimitiveCoreInstance<T> extends AbstractCoreInstance implements PrimitiveCoreInstance<T>
{
    private final T value;
    private final int internalSyntheticId;
    private CoreInstance classifier;

    protected AbstractPrimitiveCoreInstance(T value, CoreInstance classifier, int internalSyntheticId)
    {
        if (value == null)
        {
            throw new IllegalArgumentException("Primitive value may not be null");
        }
        this.value = value;
        this.classifier = classifier;
        this.internalSyntheticId = internalSyntheticId;
    }

    public T getValue()
    {
        return this.value;
    }

    @Override
    public String toString()
    {
        return getName() + "(" + this.internalSyntheticId + ") instanceOf " + this.classifier.getName();
    }

    @Override
    public CoreInstance getClassifier()
    {
        return this.classifier;
    }

    @Override
    public ModelRepository getRepository()
    {
        return this.classifier.getRepository();
    }

    @Override
    public int getSyntheticId()
    {
        return this.internalSyntheticId;
    }

    @Override
    public void setClassifier(CoreInstance classifier)
    {
        this.classifier = classifier;
    }

    @Override
    public void printFull(Appendable appendable, String tab)
    {
        print(SafeAppendable.wrap(appendable), tab, true);
    }

    @Override
    public void print(Appendable appendable, String tab, int max)
    {
        print(SafeAppendable.wrap(appendable), tab, false);
    }

    private void print(SafeAppendable appendable, String tab, boolean full)
    {
        appendable.append(tab).append(getName());
        if (full)
        {
            appendable.append('_').append(this.internalSyntheticId);
        }

        appendable.append(" instance ").append(this.classifier.getName());
        if (full)
        {
            appendable.append('_').append(this.classifier.getSyntheticId());
        }
    }
}
