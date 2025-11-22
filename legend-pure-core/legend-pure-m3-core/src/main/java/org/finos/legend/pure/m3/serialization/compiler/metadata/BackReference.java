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

import org.finos.legend.pure.m4.coreinstance.SourceInformation;

import java.util.Objects;

public abstract class BackReference implements Comparable<BackReference>
{
    private BackReference()
    {
    }

    @Override
    public String toString()
    {
        return appendString(new StringBuilder(32)).toString();
    }

    StringBuilder appendString(StringBuilder builder)
    {
        builder.append(getClass().getSimpleName()).append('{');
        appendStringInfo(builder);
        return builder.append('}');
    }

    public abstract <T> T visit(BackReferenceVisitor<T> visitor);

    protected abstract void appendStringInfo(StringBuilder builder);

    protected int compareByClass(BackReference other)
    {
        Class<? extends BackReference> thisClass = this.getClass();
        Class<? extends BackReference> otherClass = other.getClass();
        return (thisClass == otherClass) ? 0 : thisClass.getSimpleName().compareTo(otherClass.getSimpleName());
    }

    public static Application newApplication(String functionExpression)
    {
        return new Application(functionExpression);
    }

    public static ModelElement newModelElement(String element)
    {
        return new ModelElement(element);
    }

    public static PropertyFromAssociation newPropertyFromAssociation(String property)
    {
        return new PropertyFromAssociation(property);
    }

    public static QualifiedPropertyFromAssociation newQualifiedPropertyFromAssociation(String qualifiedProperty)
    {
        return new QualifiedPropertyFromAssociation(qualifiedProperty);
    }

    public static ReferenceUsage newReferenceUsage(String owner, String property, int offset)
    {
        return newReferenceUsage(owner, property, offset, null);
    }

    public static ReferenceUsage newReferenceUsage(String owner, String property, int offset, SourceInformation sourceInfo)
    {
        return new ReferenceUsage(owner, property, offset, sourceInfo);
    }

    public static Specialization newSpecialization(String generalization)
    {
        return new Specialization(generalization);
    }

    public static class Application extends BackReference
    {
        private final String functionExpression;

        private Application(String functionExpression)
        {
            this.functionExpression = Objects.requireNonNull(functionExpression);
        }

        @Override
        public boolean equals(Object other)
        {
            if (this == other)
            {
                return true;
            }

            if (!(other instanceof Application))
            {
                return false;
            }

            return this.functionExpression.equals(((Application) other).functionExpression);
        }

        @Override
        public int hashCode()
        {
            return this.functionExpression.hashCode();
        }

        @Override
        public int compareTo(BackReference other)
        {
            if (this == other)
            {
                return 0;
            }

            int cmp = compareByClass(other);
            return (cmp != 0) ? cmp : compareApplications(this, (Application) other);
        }

        @Override
        public <T> T visit(BackReferenceVisitor<T> visitor)
        {
            return visitor.visit(this);
        }

        public String getFunctionExpression()
        {
            return this.functionExpression;
        }

        @Override
        protected void appendStringInfo(StringBuilder builder)
        {
            builder.append("functionExpression=").append(this.functionExpression);
        }
    }

    public static class ModelElement extends BackReference
    {
        private final String element;

        private ModelElement(String element)
        {
            this.element = Objects.requireNonNull(element);
        }

        @Override
        public boolean equals(Object other)
        {
            if (this == other)
            {
                return true;
            }

            if (!(other instanceof ModelElement))
            {
                return false;
            }

            return this.element.equals(((ModelElement) other).element);
        }

        @Override
        public int hashCode()
        {
            return this.element.hashCode();
        }

        @Override
        public int compareTo(BackReference other)
        {
            if (this == other)
            {
                return 0;
            }

            int cmp = compareByClass(other);
            return (cmp != 0) ? cmp : compareModelElements(this, (ModelElement) other);
        }

        @Override
        public <T> T visit(BackReferenceVisitor<T> visitor)
        {
            return visitor.visit(this);
        }

        public String getElement()
        {
            return this.element;
        }

        @Override
        protected void appendStringInfo(StringBuilder builder)
        {
            builder.append("element=").append(this.element);
        }
    }

    public static class PropertyFromAssociation extends BackReference
    {
        private final String property;

        private PropertyFromAssociation(String property)
        {
            this.property = Objects.requireNonNull(property);
        }

        @Override
        public boolean equals(Object other)
        {
            if (this == other)
            {
                return true;
            }

            if (!(other instanceof PropertyFromAssociation))
            {
                return false;
            }

            return this.property.equals(((PropertyFromAssociation) other).property);
        }

        @Override
        public int hashCode()
        {
            return this.property.hashCode();
        }

        @Override
        public int compareTo(BackReference other)
        {
            if (this == other)
            {
                return 0;
            }

            int cmp = compareByClass(other);
            return (cmp != 0) ? cmp : comparePropertiesFromAssociations(this, (PropertyFromAssociation) other);
        }

        @Override
        public <T> T visit(BackReferenceVisitor<T> visitor)
        {
            return visitor.visit(this);
        }

        public String getProperty()
        {
            return this.property;
        }

        @Override
        protected void appendStringInfo(StringBuilder builder)
        {
            builder.append("property=").append(this.property);
        }
    }

    public static class QualifiedPropertyFromAssociation extends BackReference
    {
        private final String qualifiedProperty;

        private QualifiedPropertyFromAssociation(String qualifiedProperty)
        {
            this.qualifiedProperty = Objects.requireNonNull(qualifiedProperty);
        }

        @Override
        public boolean equals(Object other)
        {
            if (this == other)
            {
                return true;
            }

            if (!(other instanceof QualifiedPropertyFromAssociation))
            {
                return false;
            }

            return this.qualifiedProperty.equals(((QualifiedPropertyFromAssociation) other).qualifiedProperty);
        }

        @Override
        public int hashCode()
        {
            return this.qualifiedProperty.hashCode();
        }

        @Override
        public int compareTo(BackReference other)
        {
            if (this == other)
            {
                return 0;
            }

            int cmp = compareByClass(other);
            return (cmp != 0) ? cmp : compareQualifiedPropertiesFromAssociations(this, (QualifiedPropertyFromAssociation) other);
        }

        @Override
        public <T> T visit(BackReferenceVisitor<T> visitor)
        {
            return visitor.visit(this);
        }

        public String getQualifiedProperty()
        {
            return this.qualifiedProperty;
        }

        @Override
        protected void appendStringInfo(StringBuilder builder)
        {
            builder.append("qualifiedProperty=").append(this.qualifiedProperty);
        }
    }

    public static class ReferenceUsage extends BackReference
    {
        private final String owner;
        private final String property;
        private final int offset;
        private final SourceInformation sourceInfo;

        private ReferenceUsage(String owner, String property, int offset, SourceInformation sourceInfo)
        {
            this.owner = Objects.requireNonNull(owner);
            this.property = Objects.requireNonNull(property);
            this.offset = offset;
            this.sourceInfo = sourceInfo;
        }

        @Override
        public boolean equals(Object other)
        {
            if (this == other)
            {
                return true;
            }

            if (!(other instanceof ReferenceUsage))
            {
                return false;
            }

            ReferenceUsage that = (ReferenceUsage) other;
            return (this.offset == that.offset) &&
                    this.owner.equals(that.owner) &&
                    this.property.equals(that.property) &&
                    Objects.equals(this.sourceInfo, that.sourceInfo);
        }

        @Override
        public int hashCode()
        {
            return Objects.hash(this.owner, this.property, this.offset, this.sourceInfo);
        }

        @Override
        public int compareTo(BackReference other)
        {
            if (this == other)
            {
                return 0;
            }

            int cmp = compareByClass(other);
            return (cmp != 0) ? cmp : compareReferenceUsages(this, (ReferenceUsage) other);
        }

        @Override
        public <T> T visit(BackReferenceVisitor<T> visitor)
        {
            return visitor.visit(this);
        }

        public String getOwner()
        {
            return this.owner;
        }

        public String getProperty()
        {
            return this.property;
        }

        public int getOffset()
        {
            return this.offset;
        }

        public SourceInformation getSourceInformation()
        {
            return this.sourceInfo;
        }

        @Override
        protected void appendStringInfo(StringBuilder builder)
        {
            builder.append("owner=").append(this.owner)
                    .append(" property=").append(this.property)
                    .append(" offset=").append(this.offset);
            if (this.sourceInfo != null)
            {
                this.sourceInfo.appendMessage(builder.append(" sourceInfo="));
            }
        }
    }

    public static class Specialization extends BackReference
    {
        private final String generalization;

        private Specialization(String generalization)
        {
            this.generalization = Objects.requireNonNull(generalization);
        }

        @Override
        public boolean equals(Object other)
        {
            if (this == other)
            {
                return true;
            }

            if (!(other instanceof Specialization))
            {
                return false;
            }

            return this.generalization.equals(((Specialization) other).generalization);
        }

        @Override
        public int hashCode()
        {
            return this.generalization.hashCode();
        }

        @Override
        public int compareTo(BackReference other)
        {
            if (this == other)
            {
                return 0;
            }

            int cmp = compareByClass(other);
            return (cmp != 0) ? cmp : compareSpecializations(this, (Specialization) other);
        }

        @Override
        public <T> T visit(BackReferenceVisitor<T> visitor)
        {
            return visitor.visit(this);
        }

        public String getGeneralization()
        {
            return this.generalization;
        }

        @Override
        protected void appendStringInfo(StringBuilder builder)
        {
            builder.append("generalization=").append(this.generalization);
        }
    }

    public static int compareApplications(Application first, Application second)
    {
        return first.getFunctionExpression().compareTo(second.getFunctionExpression());
    }

    public static int compareModelElements(ModelElement first, ModelElement second)
    {
        return first.getElement().compareTo(second.getElement());
    }

    public static int comparePropertiesFromAssociations(PropertyFromAssociation first, PropertyFromAssociation second)
    {
        return first.getProperty().compareTo(second.getProperty());
    }

    public static int compareQualifiedPropertiesFromAssociations(QualifiedPropertyFromAssociation first, QualifiedPropertyFromAssociation second)
    {
        return first.getQualifiedProperty().compareTo(second.getQualifiedProperty());
    }

    public static int compareReferenceUsages(ReferenceUsage first, ReferenceUsage second)
    {
        int cmp = first.getOwner().compareTo(second.getOwner());
        if (cmp == 0)
        {
            cmp = first.getProperty().compareTo(second.getProperty());
            if (cmp == 0)
            {
                cmp = Integer.compare(first.getOffset(), second.getOffset());
            }
        }
        return cmp;
    }

    public static int compareSpecializations(Specialization first, Specialization second)
    {
        return first.getGeneralization().compareTo(second.getGeneralization());
    }
}
