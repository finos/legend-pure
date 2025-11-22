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

package org.finos.legend.pure.m3.serialization.compiler.metadata;

import java.util.function.Consumer;

public abstract class BackReferenceConsumer implements BackReferenceVisitor<Void>, Consumer<BackReference>
{
    @Override
    public final void accept(BackReference backReference)
    {
        backReference.visit(this);
    }

    @Override
    public final Void visit(BackReference.Application application)
    {
        accept(application);
        return null;
    }

    @Override
    public final Void visit(BackReference.ModelElement modelElement)
    {
        accept(modelElement);
        return null;
    }

    @Override
    public final Void visit(BackReference.PropertyFromAssociation propertyFromAssociation)
    {
        accept(propertyFromAssociation);
        return null;
    }

    @Override
    public final Void visit(BackReference.QualifiedPropertyFromAssociation qualifiedPropertyFromAssociation)
    {
        accept(qualifiedPropertyFromAssociation);
        return null;
    }

    @Override
    public final Void visit(BackReference.ReferenceUsage referenceUsage)
    {
        accept(referenceUsage);
        return null;
    }

    @Override
    public final Void visit(BackReference.Specialization specialization)
    {
        accept(specialization);
        return null;
    }

    protected void accept(BackReference.Application application)
    {
        // do nothing by default
    }

    protected void accept(BackReference.ModelElement modelElement)
    {
        // do nothing by default
    }

    protected void accept(BackReference.PropertyFromAssociation propertyFromAssociation)
    {
        // do nothing by default
    }

    protected void accept(BackReference.QualifiedPropertyFromAssociation qualifiedPropertyFromAssociation)
    {
        // do nothing by default
    }

    protected void accept(BackReference.ReferenceUsage referenceUsage)
    {
        // do nothing by default
    }

    protected void accept(BackReference.Specialization specialization)
    {
        // do nothing by default
    }
}
