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
import org.eclipse.collections.api.list.MutableList;
import org.finos.legend.pure.m4.coreinstance.SourceInformation;

import java.util.Objects;

public class Obj
{
    private final SourceInformation sourceInformation;
    private final String identifier;
    private final String classifier;
    private final String name;
    private final MutableList<PropertyValue> properties;

    public Obj(SourceInformation sourceInformation, String identifier, String classifier, String name, MutableList<PropertyValue> propertiesList)
    {
        this.sourceInformation = sourceInformation;
        this.identifier = identifier;
        this.classifier = classifier;
        this.name = name;
        this.properties = (propertiesList == null) ? Lists.mutable.empty() : propertiesList;
    }

    public Obj(SourceInformation sourceInformation, String identifier, String classifier, String name)
    {
        this(sourceInformation, identifier, classifier, name, null);
    }

    public SourceInformation getSourceInformation()
    {
        return this.sourceInformation;
    }

    public String getIdentifier()
    {
        return this.identifier;
    }

    public String getClassifier()
    {
        return this.classifier;
    }

    public String getName()
    {
        return this.name;
    }

    public ListIterable<PropertyValue> getPropertyValues()
    {
        return this.properties;
    }

    public void addPropertyValue(PropertyValue value)
    {
        this.properties.add(value);
    }

    @Override
    public boolean equals(Object other)
    {
        if (this == other)
        {
            return true;
        }

        if (other == null || this.getClass() != other.getClass())
        {
            return false;
        }

        Obj that = (Obj)other;
        return this.identifier.equals(that.identifier) &&
                this.classifier.equals(that.classifier) &&
                this.name.equals(that.name) &&
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
        builder.append('{');
        if (this.sourceInformation != null)
        {
            builder.append("sourceInformation=");
            this.sourceInformation.writeMessage(builder);
            builder.append(", ");
        }
        builder.append("classifier='");
        builder.append(this.classifier);
        builder.append("', identifier='");
        builder.append(this.identifier);
        builder.append("', name='");
        builder.append(this.name);
        builder.append("', properties=");
        this.properties.appendString(builder);
        builder.append('}');
        return builder.toString();
    }
}
