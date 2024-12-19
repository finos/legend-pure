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

package org.finos.legend.pure.m3.serialization.compiler.element;

import java.util.function.Consumer;

public abstract class ValueOrReferenceConsumer implements Consumer<ValueOrReference>, ValueOrReferenceVisitor<Void>
{
    @Override
    public Void visit(Reference.ExternalReference reference)
    {
        accept(reference);
        return null;
    }

    @Override
    public Void visit(Reference.InternalReference reference)
    {
        accept(reference);
        return null;
    }

    @Override
    public Void visit(Value.BooleanValue value)
    {
        accept(value);
        return null;
    }

    @Override
    public Void visit(Value.ByteValue value)
    {
        accept(value);
        return null;
    }

    @Override
    public Void visit(Value.DateValue value)
    {
        accept(value);
        return null;
    }

    @Override
    public Void visit(Value.DateTimeValue value)
    {
        accept(value);
        return null;
    }

    @Override
    public Void visit(Value.StrictDateValue value)
    {
        accept(value);
        return null;
    }

    @Override
    public Void visit(Value.LatestDateValue value)
    {
        accept(value);
        return null;
    }

    @Override
    public Void visit(Value.DecimalValue value)
    {
        accept(value);
        return null;
    }

    @Override
    public Void visit(Value.FloatValue value)
    {
        accept(value);
        return null;
    }

    @Override
    public Void visit(Value.IntegerValue value)
    {
        accept(value);
        return null;
    }

    @Override
    public Void visit(Value.StrictTimeValue value)
    {
        accept(value);
        return null;
    }

    @Override
    public Void visit(Value.StringValue value)
    {
        accept(value);
        return null;
    }

    @Override
    public void accept(ValueOrReference valueOrReference)
    {
        valueOrReference.visit(this);
    }

    protected void accept(Reference.ExternalReference reference)
    {
        // do nothing by default
    }

    protected void accept(Reference.InternalReference reference)
    {
        // do nothing by default
    }

    protected void accept(Value.BooleanValue value)
    {
        // do nothing by default
    }

    protected void accept(Value.ByteValue value)
    {
        // do nothing by default
    }

    protected void accept(Value.DateValue value)
    {
        // do nothing by default
    }

    protected void accept(Value.DateTimeValue value)
    {
        // do nothing by default
    }

    protected void accept(Value.StrictDateValue value)
    {
        // do nothing by default
    }

    protected void accept(Value.LatestDateValue value)
    {
        // do nothing by default
    }

    protected void accept(Value.DecimalValue value)
    {
        // do nothing by default
    }

    protected void accept(Value.FloatValue value)
    {
        // do nothing by default
    }

    protected void accept(Value.IntegerValue value)
    {
        // do nothing by default
    }

    protected void accept(Value.StrictTimeValue value)
    {
        // do nothing by default
    }

    protected void accept(Value.StringValue value)
    {
        // do nothing by default
    }
}
