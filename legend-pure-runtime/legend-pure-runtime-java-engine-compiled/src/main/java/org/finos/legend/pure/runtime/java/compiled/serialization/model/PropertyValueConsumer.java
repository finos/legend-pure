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

package org.finos.legend.pure.runtime.java.compiled.serialization.model;

import java.util.function.Consumer;

public abstract class PropertyValueConsumer implements PropertyValueVisitor<Void>, Consumer<PropertyValue>
{
    @Override
    public final Void visit(PropertyValueMany many)
    {
        accept(many);
        return null;
    }

    @Override
    public final Void visit(PropertyValueOne one)
    {
        accept(one);
        return null;
    }

    @Override
    public final void accept(PropertyValue propertyValue)
    {
        propertyValue.visit(this);
    }

    protected abstract void accept(PropertyValueMany many);

    protected abstract void accept(PropertyValueOne one);
}
