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

package org.finos.legend.pure.runtime.java.compiled.serialization.model;

import org.eclipse.collections.api.factory.Lists;
import org.eclipse.collections.api.list.ListIterable;
import org.finos.legend.pure.m4.coreinstance.SourceInformation;

import java.util.Objects;

public class Obj
{
    private final String classifier;
    private final String identifier;
    private final String name;
    private final ListIterable<PropertyValue> properties;
    private final SourceInformation sourceInformation;

    protected Obj(String classifier, String identifier, String name, ListIterable<PropertyValue> propertiesList, SourceInformation sourceInformation)
    {
        this.classifier = classifier;
        this.identifier = identifier;
        this.name = name;
        this.properties = (propertiesList == null) ? Lists.immutable.empty() : propertiesList;
        this.sourceInformation = sourceInformation;
    }

    public String getClassifier()
    {
        return this.classifier;
    }

    public String getIdentifier()
    {
        return this.identifier;
    }

    public String getName()
    {
        return this.name;
    }

    public ListIterable<PropertyValue> getPropertyValues()
    {
        return this.properties;
    }

    public SourceInformation getSourceInformation()
    {
        return this.sourceInformation;
    }

    public boolean isEnum()
    {
        return false;
    }

    @Override
    public boolean equals(Object other)
    {
        if (this == other)
        {
            return true;
        }

        if (!(other instanceof Obj))
        {
            return false;
        }

        Obj that = (Obj) other;
        return (isEnum() == that.isEnum()) &&
                this.classifier.equals(that.classifier) &&
                this.identifier.equals(that.identifier) &&
                Objects.equals(this.name, that.name) &&
                Objects.equals(this.sourceInformation, that.sourceInformation) &&
                this.properties.equals(that.properties);
    }

    @Override
    public int hashCode()
    {
        return this.classifier.hashCode() + (31 * this.identifier.hashCode());
    }

    @Override
    public String toString()
    {
        StringBuilder builder = new StringBuilder(getClass().getSimpleName());
        builder.append("{classifier='").append(this.classifier).append("'");
        builder.append(", identifier='").append(this.identifier).append("'");
        if (this.name != null)
        {
            builder.append(", name='").append(this.name).append("'");
        }
        this.properties.appendString(builder, ", properties=[", ", ", "]");
        if (this.sourceInformation != null)
        {
            this.sourceInformation.appendMessage(builder.append(", sourceInformation="));
        }
        builder.append('}');
        return builder.toString();
    }

    public static Obj newObj(String classifier, String identifier, String name, ListIterable<PropertyValue> propertiesList, SourceInformation sourceInformation, boolean isEnum)
    {
        return isEnum ?
                new Enum(classifier, identifier, name, propertiesList, sourceInformation) :
                new Obj(classifier, identifier, name, propertiesList, sourceInformation);
    }
}
