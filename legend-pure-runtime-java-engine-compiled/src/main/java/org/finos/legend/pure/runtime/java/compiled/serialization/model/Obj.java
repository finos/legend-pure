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
import org.eclipse.collections.api.factory.Sets;
import org.eclipse.collections.api.list.ListIterable;
import org.eclipse.collections.api.list.MutableList;
import org.eclipse.collections.api.set.MutableSet;
import org.finos.legend.pure.m4.coreinstance.SourceInformation;

import java.util.Arrays;
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Objects;
import java.util.function.BiConsumer;

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

    public static Obj newObj(String classifier, String identifier, String name, ListIterable<PropertyValue> propertyValues, SourceInformation sourceInformation, boolean isEnum)
    {
        Objects.requireNonNull(classifier, "classifier may not be null");
        Objects.requireNonNull(identifier, "identifier may not be null");
        return isEnum ?
                new Enum(classifier, identifier, name, propertyValues, sourceInformation) :
                new Obj(classifier, identifier, name, propertyValues, sourceInformation);
    }

    public static Obj merge(Obj... objs)
    {
        return merge(Arrays.asList(objs));
    }

    public static Obj merge(Iterable<? extends Obj> objs)
    {
        Iterator<? extends Obj> iterator = objs.iterator();
        if (!iterator.hasNext())
        {
            // No Objs: error
            throw new IllegalArgumentException("No Objs to merge");
        }

        Obj first = iterator.next();
        if (!iterator.hasNext())
        {
            // Only one Obj: no need to merge
            return first;
        }

        // Multiple Objs: merge required
        boolean isEnum = first.isEnum();
        String classifier = first.getClassifier();
        String identifier = first.getIdentifier();
        String name = first.getName();
        SourceInformation sourceInfo = first.getSourceInformation();
        Map<String, MutableList<RValue>> propertyValuesByName = new LinkedHashMap<>();
        PropertyValueConsumer propertyValueCollector = new PropertyValueConsumer()
        {
            @Override
            protected void accept(PropertyValueMany many)
            {
                ListIterable<RValue> values = many.getValues();
                if (values.notEmpty())
                {
                    propertyValuesByName.computeIfAbsent(many.getProperty(), k -> Lists.mutable.empty()).addAllIterable(values);
                }
            }

            @Override
            protected void accept(PropertyValueOne one)
            {
                RValue value = one.getValue();
                if (value != null)
                {
                    propertyValuesByName.computeIfAbsent(one.getProperty(), k -> Lists.mutable.empty()).add(value);
                }
            }
        };
        first.getPropertyValues().forEach(propertyValueCollector);

        // Collect values from remaining Objs
        while (iterator.hasNext())
        {
            Obj obj = iterator.next();
            if (isEnum != obj.isEnum())
            {
                throw new IllegalArgumentException("Cannot merge, isEnum mismatch: " + isEnum + " vs " + obj.isEnum());
            }

            classifier = mergeObjValue(classifier, obj.getClassifier(), "classifier");
            identifier = mergeObjValue(identifier, obj.getIdentifier(), "identifier");
            name = mergeObjValue(name, obj.getName(), "name");
            sourceInfo = mergeObjValue(sourceInfo, obj.getSourceInformation(), "source information", SourceInformation::appendMessage);
            obj.getPropertyValues().forEach(propertyValueCollector);
        }

        // Build list of property values
        MutableList<PropertyValue> propertyValuesList = Lists.mutable.withInitialCapacity(propertyValuesByName.size());
        propertyValuesByName.forEach((property, rValues) ->
        {
            if (rValues.size() > 1)
            {
                MutableSet<RValue> rValueSet = Sets.mutable.withInitialCapacity(rValues.size());
                rValues.removeIf(r -> !rValueSet.add(r));
            }
            propertyValuesList.add((rValues.size() == 1) ? new PropertyValueOne(property, rValues.get(0)) : new PropertyValueMany(property, rValues.asUnmodifiable()));
        });

        return newObj(classifier, identifier, name, propertyValuesList.asUnmodifiable(), sourceInfo, isEnum);
    }

    private static <T> T mergeObjValue(T currentValue, T newValue, String description)
    {
        return mergeObjValue(currentValue, newValue, description, null);
    }

    private static  <T> T mergeObjValue(T currentValue, T newValue, String description, BiConsumer<? super T, ? super StringBuilder> messageAppender)
    {
        if (currentValue == null)
        {
            return newValue;
        }
        if ((newValue != null) && !currentValue.equals(newValue))
        {
            StringBuilder builder = new StringBuilder("Cannot merge, ").append(description).append(" mismatch: ");
            if (messageAppender == null)
            {
                builder.append(currentValue).append(" vs ").append(newValue);
            }
            else
            {
                messageAppender.accept(currentValue, builder);
                builder.append(" vs ");
                messageAppender.accept(newValue, builder);
            }
            throw new IllegalArgumentException(builder.toString());
        }
        return currentValue;
    }
}
