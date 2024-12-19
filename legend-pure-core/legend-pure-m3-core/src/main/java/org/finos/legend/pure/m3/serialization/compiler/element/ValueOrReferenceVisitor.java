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

public interface ValueOrReferenceVisitor<T>
{
    default T visit(Reference.ExternalReference reference)
    {
        throw new UnsupportedOperationException();
    }

    default T visit(Reference.InternalReference reference)
    {
        throw new UnsupportedOperationException();
    }

    default T visit(Value.BooleanValue value)
    {
        throw new UnsupportedOperationException();
    }

    default T visit(Value.ByteValue value)
    {
        throw new UnsupportedOperationException();
    }

    default T visit(Value.DateValue value)
    {
        throw new UnsupportedOperationException();
    }

    default T visit(Value.DateTimeValue value)
    {
        throw new UnsupportedOperationException();
    }

    default T visit(Value.StrictDateValue value)
    {
        throw new UnsupportedOperationException();
    }

    default T visit(Value.LatestDateValue value)
    {
        throw new UnsupportedOperationException();
    }

    default T visit(Value.DecimalValue value)
    {
        throw new UnsupportedOperationException();
    }

    default T visit(Value.FloatValue value)
    {
        throw new UnsupportedOperationException();
    }

    default T visit(Value.IntegerValue value)
    {
        throw new UnsupportedOperationException();
    }

    default T visit(Value.StrictTimeValue value)
    {
        throw new UnsupportedOperationException();
    }

    default T visit(Value.StringValue value)
    {
        throw new UnsupportedOperationException();
    }
}
