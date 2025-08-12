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

package org.finos.legend.pure.m3.navigation.graph;

import org.eclipse.collections.api.factory.Lists;
import org.eclipse.collections.api.list.ImmutableList;
import org.eclipse.collections.api.list.ListIterable;
import org.eclipse.collections.api.list.MutableList;
import org.finos.legend.pure.m3.coreinstance.meta.pure.metamodel.type.Any;
import org.finos.legend.pure.m3.navigation.M3Paths;
import org.finos.legend.pure.m3.navigation.M3Properties;
import org.finos.legend.pure.m3.navigation.PackageableElement.PackageableElement;
import org.finos.legend.pure.m3.navigation.ProcessorSupport;
import org.finos.legend.pure.m4.coreinstance.AbstractCoreInstanceWrapper;
import org.finos.legend.pure.m4.coreinstance.CoreInstance;
import org.finos.legend.pure.m4.coreinstance.SourceInformation;
import org.finos.legend.pure.m4.serialization.grammar.StringEscape;
import org.finos.legend.pure.m4.tools.SafeAppendable;

import java.util.Arrays;
import java.util.List;
import java.util.Objects;
import java.util.function.Consumer;
import java.util.function.Function;

public class GraphPath
{
    private final String startNodePath;
    private final ImmutableList<Edge> edges;

    private GraphPath(String startNodePath, ImmutableList<Edge> edges)
    {
        this.startNodePath = startNodePath;
        this.edges = edges;
    }

    private GraphPath(String startNodePath, Edge... edges)
    {
        this(startNodePath, Lists.immutable.with(edges));
    }

    private GraphPath(String startNodePath)
    {
        this(startNodePath, Lists.immutable.empty());
    }

    public int getNodeCount()
    {
        return getEdgeCount() + 1;
    }

    public String getStartNodePath()
    {
        return this.startNodePath;
    }

    public void forEachNode(Consumer<? super CoreInstance> consumer, ProcessorSupport processorSupport)
    {
        forEachNode(consumer, processorSupport::package_getByUserPath);
    }

    public void forEachNode(Consumer<? super CoreInstance> consumer, Function<? super String, ? extends CoreInstance> packagePathResolver)
    {
        CoreInstance node = getStartNode(packagePathResolver);
        consumer.accept(node);
        for (int i = 0, edgeCount = getEdgeCount(); i < edgeCount; i++)
        {
            node = applyPathElementAtIndex(node, i);
            consumer.accept(node);
        }
    }

    public int getEdgeCount()
    {
        return this.edges.size();
    }

    public Edge getEdge(int index)
    {
        return this.edges.get(index);
    }

    public void forEachEdge(Consumer<? super Edge> consumer)
    {
        this.edges.forEach(consumer);
    }

    public ListIterable<Edge> getEdges()
    {
        return this.edges;
    }

    public CoreInstance resolveUpTo(int end, ProcessorSupport processorSupport)
    {
        return resolveUpTo(end, processorSupport::package_getByUserPath);
    }

    public CoreInstance resolveUpTo(int end, Function<? super String, ? extends CoreInstance> packagePathResolver)
    {
        int resolvedEnd = (end < 0) ? (this.edges.size() + end) : end;
        if ((resolvedEnd > this.edges.size()) || (resolvedEnd < 0))
        {
            throw new IndexOutOfBoundsException("Index: " + end + "; size: " + this.edges.size());
        }
        CoreInstance node = getStartNode(packagePathResolver);
        for (int i = 0; i < resolvedEnd; i++)
        {
            node = applyPathElementAtIndex(node, i);
        }
        return node;
    }

    public CoreInstance resolve(ProcessorSupport processorSupport)
    {
        return resolve(processorSupport::package_getByUserPath);
    }

    public CoreInstance resolve(Function<? super String, ? extends CoreInstance> packagePathResolver)
    {
        return resolveUpTo(getEdgeCount(), packagePathResolver);
    }

    public ResolvedGraphPath resolveFully(ProcessorSupport processorSupport)
    {
        return resolveFully(processorSupport::package_getByUserPath);
    }

    public ResolvedGraphPath resolveFully(Function<? super String, ? extends CoreInstance> packagePathResolver)
    {
        MutableList<CoreInstance> nodes = Lists.mutable.ofInitialCapacity(getNodeCount());
        forEachNode(nodes::add, packagePathResolver);
        return new ResolvedGraphPath(this, nodes.toImmutable());
    }

    public GraphPath subpath(int end)
    {
        if (end == this.edges.size())
        {
            return this;
        }

        int resolvedEnd = (end < 0) ? (this.edges.size() + end) : end;
        if (resolvedEnd == 0)
        {
            return new GraphPath(this.startNodePath);
        }

        if ((resolvedEnd > this.edges.size()) || (resolvedEnd < 0))
        {
            throw new IndexOutOfBoundsException("Index: " + end + "; size: " + this.edges.size());
        }
        return new GraphPath(this.startNodePath, this.edges.subList(0, resolvedEnd));
    }

    public GraphPath reduce(ProcessorSupport processorSupport)
    {
        if (this.edges.isEmpty())
        {
            // No edges: impossible to reduce
            return this;
        }

        CoreInstance node = getStartNode(processorSupport::package_getByUserPath);
        CoreInstance lastPackagedOrTopLevel = node;
        int newStartIndex = 0;
        int pathElementCount = this.edges.size();
        for (int i = 0; i < pathElementCount; i++)
        {
            node = applyPathElementAtIndex(node, i);
            if (isPackagedOrTopLevel(node, processorSupport))
            {
                lastPackagedOrTopLevel = node;
                newStartIndex = i + 1;
            }
        }
        if (newStartIndex == 0)
        {
            // Not reducible
            return this;
        }
        String newStartNodePath = PackageableElement.getUserPathForPackageableElement(lastPackagedOrTopLevel);
        if (newStartIndex == pathElementCount)
        {
            // Reduced to a new start node with no path elements
            return new GraphPath(newStartNodePath);
        }
        // Reduced to a new start node with a reduced set of path elements
        return new GraphPath(newStartNodePath, this.edges.subList(newStartIndex, pathElementCount).toArray(new Edge[pathElementCount - newStartIndex]));
    }

    public String getDescription()
    {
        return getDescription(this.startNodePath, this.edges);
    }

    public <T extends Appendable> T writeDescription(T appendable)
    {
        return writeDescription(appendable, this.startNodePath, this.edges);
    }

    public String getPureExpression()
    {
        return getPureExpression(this.startNodePath, this.edges);
    }

    public <T extends Appendable> T writePureExpression(T appendable)
    {
        return writePureExpression(appendable, this.startNodePath, this.edges);
    }

    public boolean startsWith(GraphPath other)
    {
        if (this == other)
        {
            return true;
        }

        int thisSize = this.edges.size();
        int otherSize = other.edges.size();
        return (otherSize <= thisSize) &&
                other.startNodePath.equals(this.startNodePath) &&
                other.edges.equals((otherSize < thisSize) ? this.edges.subList(0, otherSize) : this.edges);
    }

    public Builder extend()
    {
        return extend(true);
    }

    public Builder extend(boolean validate)
    {
        return new Builder(this, validate);
    }

    public GraphPath withToOneProperty(String property)
    {
        return withToOneProperty(property, true);
    }

    public GraphPath withToOneProperty(String property, boolean validate)
    {
        return withEdge(newToOnePropertyEdge(property, validate));
    }

    public GraphPath withToManyPropertyValueAtIndex(String property, int index)
    {
        return withToManyPropertyValueAtIndex(property, index, true);
    }

    public GraphPath withToManyPropertyValueAtIndex(String property, int index, boolean validate)
    {
        return withEdge(newToManyPropertyAtIndexEdge(property, index, validate));
    }

    public GraphPath withToManyPropertyValueWithName(String property, String valueName)
    {
        return withToManyPropertyValueWithName(property, valueName, true);
    }

    public GraphPath withToManyPropertyValueWithName(String property, String valueName, boolean validate)
    {
        return withEdge(newToManyPropertyWithNameEdge(property, valueName, validate));
    }

    public GraphPath withToManyPropertyValueWithKey(String property, String keyProperty, String key)
    {
        return withToManyPropertyValueWithKey(property, keyProperty, key, true);
    }

    public GraphPath withToManyPropertyValueWithKey(String property, String keyProperty, String key, boolean validate)
    {
        return withEdge(newToManyPropertyWithStringKeyEdge(property, keyProperty, key, validate));
    }

    private GraphPath withEdge(Edge edge)
    {
        return new GraphPath(this.startNodePath, this.edges.newWith(edge));
    }

    @Override
    public boolean equals(Object other)
    {
        if (this == other)
        {
            return true;
        }

        if (!(other instanceof GraphPath))
        {
            return false;
        }

        GraphPath that = (GraphPath) other;
        return this.startNodePath.equals(that.startNodePath) && this.edges.equals(that.edges);
    }

    @Override
    public int hashCode()
    {
        return this.startNodePath.hashCode() + (43 * this.edges.hashCode());
    }

    @Override
    public String toString()
    {
        return getDescription();
    }

    private CoreInstance getStartNode(Function<? super String, ? extends CoreInstance> packagePathResolver)
    {
        CoreInstance node = packagePathResolver.apply(this.startNodePath);
        if (node == null)
        {
            throw new RuntimeException("Could not find " + this.startNodePath);
        }
        return node;
    }

    private CoreInstance applyPathElementAtIndex(CoreInstance previousNode, int index)
    {
        CoreInstance result;
        try
        {
            result = this.edges.get(index).apply(previousNode);
        }
        catch (Exception e)
        {
            StringBuilder message = writeMessageUpTo(new StringBuilder("Error accessing "), index).append(" (final node: ").append(previousNode);
            SourceInformation finalNodeSourceInfo = previousNode.getSourceInformation();
            if (finalNodeSourceInfo != null)
            {
                finalNodeSourceInfo.appendMessage(message.append(" at "));
            }
            message.append(')');
            String errorMessage = e.getMessage();
            if (errorMessage != null)
            {
                message.append(": ").append(errorMessage);
            }
            throw new RuntimeException(message.toString(), e);
        }
        if (result == null)
        {
            throw new RuntimeException(writeMessageUpTo(new StringBuilder("Could not find "), index).toString());
        }
        return result;
    }

    private StringBuilder writeMessageUpTo(StringBuilder message, int index)
    {
        message.append(this.startNodePath);
        for (int i = 0; i <= index; i++)
        {
            this.edges.get(i).writeMessage(message);
        }
        return message;
    }

    public static Builder builder()
    {
        return builder(true);
    }

    public static Builder builder(boolean validate)
    {
        return new Builder(validate);
    }

    public static Builder builder(int initEdgeCapacity)
    {
        return builder(initEdgeCapacity, true);
    }

    public static Builder builder(int initEdgeCapacity, boolean validate)
    {
        return new Builder(initEdgeCapacity, validate);
    }

    public static Builder builder(String startNodePath)
    {
        return builder(startNodePath, true);
    }

    public static Builder builder(String startNodePath, boolean validate)
    {
        return builder(validate).withStartNodePath(startNodePath);
    }

    public static Builder builder(GraphPath path)
    {
        return builder(path, true);
    }

    public static Builder builder(GraphPath path, boolean validate)
    {
        return new Builder(Objects.requireNonNull(path, "path may not be null"), validate);
    }

    @Deprecated
    public static Builder newPathBuilder()
    {
        return builder();
    }

    @Deprecated
    public static Builder newPathBuilder(String startNodePath)
    {
        return builder(startNodePath);
    }

    @Deprecated
    public static Builder newPathBuilder(GraphPath path)
    {
        return builder(path);
    }

    public static GraphPath buildPath(String startNodePath)
    {
        return buildPath(startNodePath, true);
    }

    public static GraphPath buildPath(String startNodePath, boolean validate)
    {
        if (validate)
        {
            validateStartNodePath(startNodePath);
        }
        return new GraphPath(startNodePath, Lists.immutable.empty());
    }

    public static GraphPath buildPath(String startNodePath, String... toOneProperties)
    {
        return builder().withStartNodePath(startNodePath).addToOneProperties(toOneProperties).build();
    }

    public static GraphPath parse(String description)
    {
        return builder().fromDescription(description).build();
    }

    private static String getDescription(String startNodePath, ListIterable<? extends Edge> edges)
    {
        return writeDescription(new StringBuilder(startNodePath.length() + (16 * edges.size())), startNodePath, edges).toString();
    }

    private static <T extends Appendable> T writeDescription(T appendable, String startNodePath, ListIterable<? extends Edge> edges)
    {
        SafeAppendable safeAppendable = SafeAppendable.wrap(appendable).append(startNodePath);
        edges.forEach(e -> e.writeMessage(safeAppendable));
        return appendable;
    }

    private static String getPureExpression(String startNodePath, ListIterable<? extends Edge> edges)
    {
        return writePureExpression(new StringBuilder(startNodePath.length() + (16 * edges.size())), startNodePath, edges).toString();
    }

    private static <T extends Appendable> T writePureExpression(T appendable, String startNodePath, ListIterable<? extends Edge> edges)
    {
        SafeAppendable safeAppendable = SafeAppendable.wrap(appendable).append(startNodePath);
        edges.forEachWith(Edge::writePureExpression, safeAppendable);
        return appendable;
    }

    private static ToOnePropertyEdge newToOnePropertyEdge(String property, boolean validate)
    {
        if (validate)
        {
            validateProperty(property);
        }
        return new ToOnePropertyEdge(property);
    }

    private static ToManyPropertyAtIndexEdge newToManyPropertyAtIndexEdge(String property, int index, boolean validate)
    {
        if (validate)
        {
            validateProperty(property);
            validateIndex(index);
        }
        return new ToManyPropertyAtIndexEdge(property, index);
    }

    private static ToManyPropertyWithStringKeyEdge newToManyPropertyWithNameEdge(String property, String valueName, boolean validate)
    {
        if (validate)
        {
            validateProperty(property);
            validateKey(valueName);
        }
        return new ToManyPropertyWithStringKeyEdge(property, M3Properties.name, valueName);
    }

    private static ToManyPropertyWithStringKeyEdge newToManyPropertyWithStringKeyEdge(String property, String keyProperty, String key, boolean validate)
    {
        if (validate)
        {
            validateProperty(property);
            validateKeyProperty(keyProperty);
            validateKey(key);
        }
        return new ToManyPropertyWithStringKeyEdge(property, keyProperty, key);
    }

    public static class Builder
    {
        private String startNodePath;
        private final MutableList<Edge> pathElements;
        private final boolean validate;

        private Builder(boolean validate)
        {
            this.pathElements = Lists.mutable.empty();
            this.validate = validate;
        }

        private Builder(int initEdgeCapacity, boolean validate)
        {
            this.pathElements = Lists.mutable.withInitialCapacity(initEdgeCapacity);
            this.validate = validate;
        }

        private Builder(GraphPath path, boolean validate)
        {
            this.startNodePath = path.startNodePath;
            this.pathElements = Lists.mutable.withAll(path.edges);
            this.validate = validate;
        }

        public String getStartNodePath()
        {
            return this.startNodePath;
        }

        public void setStartNodePath(String path)
        {
            if (this.validate)
            {
                validateStartNodePath(path);
            }
            this.startNodePath = path;
        }

        public Builder withStartNodePath(String path)
        {
            setStartNodePath(path);
            return this;
        }

        public ListIterable<Edge> getEdges()
        {
            return this.pathElements.asUnmodifiable();
        }

        public String getDescription()
        {
            return GraphPath.getDescription(this.startNodePath, this.pathElements);
        }

        public <T extends Appendable> T writeDescription(T appendable)
        {
            return GraphPath.writeDescription(appendable, this.startNodePath, this.pathElements);
        }

        public Builder fromDescription(String description)
        {
            return parseDescription(description, this, this.validate);
        }

        public String getPureExpression()
        {
            return GraphPath.getPureExpression(this.startNodePath, this.pathElements);
        }

        public GraphPath build()
        {
            return new GraphPath(this.startNodePath, Lists.immutable.withAll(this.pathElements));
        }

        public Builder addToOneProperty(String property)
        {
            return addEdge(newToOnePropertyEdge(property, this.validate));
        }

        public Builder addToOneProperties(String... properties)
        {
            return addToOneProperties(Arrays.asList(properties));
        }

        public Builder addToOneProperties(List<String> properties)
        {
            properties.forEach(this::addToOneProperty);
            return this;
        }

        public Builder addToManyPropertyValueAtIndex(String property, int index)
        {
            return addEdge(newToManyPropertyAtIndexEdge(property, index, this.validate));
        }

        public Builder addToManyPropertyValueWithName(String property, String valueName)
        {
            return addEdge(newToManyPropertyWithNameEdge(property, valueName, this.validate));
        }

        public Builder addToManyPropertyValueWithKey(String property, String keyProperty, String key)
        {
            return addEdge(newToManyPropertyWithStringKeyEdge(property, keyProperty, key, this.validate));
        }

        private Builder addEdge(Edge pathElement)
        {
            this.pathElements.add(pathElement);
            return this;
        }
    }

    public abstract static class Edge
    {
        protected final String property;

        Edge(String property)
        {
            this.property = property;
        }

        public String getProperty()
        {
            return this.property;
        }

        public abstract <T> T visit(EdgeVisitor<T> visitor);

        @Override
        public String toString()
        {
            return writeMessage(new StringBuilder("<").append(getClass().getSimpleName()).append(" ")).append('>').toString();
        }

        abstract CoreInstance apply(CoreInstance node);

        <T extends Appendable> T writeMessage(T appendable)
        {
            writeMessage(SafeAppendable.wrap(appendable));
            return appendable;
        }

        SafeAppendable writeMessage(SafeAppendable appendable)
        {
            return appendable.append('.').append(this.property);
        }

        SafeAppendable writePureExpression(SafeAppendable appendable)
        {
            return appendable.append('.').append(this.property);
        }
    }

    public static class ToOnePropertyEdge extends Edge
    {
        ToOnePropertyEdge(String property)
        {
            super(property);
        }

        @Override
        public boolean equals(Object other)
        {
            return (this == other) || ((other instanceof ToOnePropertyEdge) && this.property.equals(((ToOnePropertyEdge) other).property));
        }

        @Override
        public int hashCode()
        {
            return this.property.hashCode();
        }

        @Override
        public <T> T visit(EdgeVisitor<T> visitor)
        {
            return visitor.visit(this);
        }

        @Override
        CoreInstance apply(CoreInstance node)
        {
            return node.getValueForMetaPropertyToOne(this.property);
        }
    }

    public abstract static class ToManyPropertyEdge extends Edge
    {
        ToManyPropertyEdge(String property)
        {
            super(property);
        }

        @Override
        final SafeAppendable writeMessage(SafeAppendable appendable)
        {
            return writeToManySelectMessage(super.writeMessage(appendable).append('[')).append(']');
        }

        @Override
        final SafeAppendable writePureExpression(SafeAppendable appendable)
        {
            return writePureSelectExpression(super.writePureExpression(appendable));
        }

        abstract SafeAppendable writeToManySelectMessage(SafeAppendable appendable);

        abstract SafeAppendable writePureSelectExpression(SafeAppendable appendable);
    }

    public static class ToManyPropertyAtIndexEdge extends ToManyPropertyEdge
    {
        private final int index;

        ToManyPropertyAtIndexEdge(String property, int index)
        {
            super(property);
            this.index = index;
        }

        public int getIndex()
        {
            return this.index;
        }

        @Override
        public boolean equals(Object other)
        {
            if (this == other)
            {
                return true;
            }
            if (!(other instanceof ToManyPropertyAtIndexEdge))
            {
                return false;
            }
            ToManyPropertyAtIndexEdge that = (ToManyPropertyAtIndexEdge) other;
            return this.property.equals(that.property) && (this.index == that.index);
        }

        @Override
        public int hashCode()
        {
            return this.property.hashCode() + 31 * this.index;
        }

        @Override
        public <T> T visit(EdgeVisitor<T> visitor)
        {
            return visitor.visit(this);
        }

        @Override
        CoreInstance apply(CoreInstance node)
        {
            ListIterable<? extends CoreInstance> values = node.getValueForMetaPropertyToMany(this.property);
            return values.get(this.index);
        }

        @Override
        SafeAppendable writeToManySelectMessage(SafeAppendable appendable)
        {
            return appendable.append(this.index);
        }

        @Override
        SafeAppendable writePureSelectExpression(SafeAppendable appendable)
        {
            return appendable.append("->at(").append(this.index).append(')');
        }
    }

    public static class ToManyPropertyWithStringKeyEdge extends ToManyPropertyEdge
    {
        private final String keyProperty;
        private final String key;

        ToManyPropertyWithStringKeyEdge(String property, String keyProperty, String key)
        {
            super(property);
            this.keyProperty = keyProperty;
            this.key = key;
        }

        public String getKeyProperty()
        {
            return this.keyProperty;
        }

        public String getKey()
        {
            return this.key;
        }

        @Override
        public boolean equals(Object other)
        {
            if (this == other)
            {
                return true;
            }
            if (!(other instanceof ToManyPropertyWithStringKeyEdge))
            {
                return false;
            }
            ToManyPropertyWithStringKeyEdge that = (ToManyPropertyWithStringKeyEdge) other;
            return this.property.equals(that.property) && this.keyProperty.equals(that.keyProperty) && this.key.equals(that.key);
        }

        @Override
        public int hashCode()
        {
            return this.property.hashCode() + 31 * (this.keyProperty.hashCode() + 31 * this.key.hashCode());
        }

        @Override
        public <T> T visit(EdgeVisitor<T> visitor)
        {
            return visitor.visit(this);
        }

        @Override
        CoreInstance apply(CoreInstance node)
        {
            return node.getValueInValueForMetaPropertyToManyWithKey(this.property, this.keyProperty, this.key);
        }

        @Override
        SafeAppendable writeToManySelectMessage(SafeAppendable appendable)
        {
            if (!M3Properties.name.equals(this.keyProperty))
            {
                appendable.append(this.keyProperty).append('=');
            }
            return StringEscape.escape(appendable.append('\''), this.key).append('\'');
        }

        @Override
        SafeAppendable writePureSelectExpression(SafeAppendable appendable)
        {
            return StringEscape.escape(appendable.append("->find(x | $x.").append(this.keyProperty).append(" == '"), this.key).append("')->toOne()");
        }
    }

    public interface EdgeVisitor<T>
    {
        default T visit(ToOnePropertyEdge edge)
        {
            throw new UnsupportedOperationException();
        }

        default T visit(ToManyPropertyAtIndexEdge edge)
        {
            throw new UnsupportedOperationException();
        }

        default T visit(ToManyPropertyWithStringKeyEdge edge)
        {
            throw new UnsupportedOperationException();
        }
    }

    public abstract static class EdgeConsumer implements EdgeVisitor<Void>, Consumer<Edge>
    {
        @Override
        public final void accept(Edge edge)
        {
            edge.visit(this);
        }

        @Override
        public final Void visit(ToOnePropertyEdge edge)
        {
            accept(edge);
            return null;
        }

        @Override
        public final Void visit(ToManyPropertyAtIndexEdge edge)
        {
            accept(edge);
            return null;
        }

        @Override
        public final Void visit(ToManyPropertyWithStringKeyEdge edge)
        {
            accept(edge);
            return null;
        }

        protected void accept(ToOnePropertyEdge edge)
        {
            // do nothing by default
        }

        protected void accept(ToManyPropertyAtIndexEdge edge)
        {
            // do nothing by default
        }

        protected void accept(ToManyPropertyWithStringKeyEdge edge)
        {
            // do nothing by default
        }
    }

    static boolean isPackagedOrTopLevel(CoreInstance node, ProcessorSupport processorSupport)
    {
        return isPackaged(node, processorSupport) || isTopLevel(node, processorSupport);
    }

    static boolean isPackaged(CoreInstance node, ProcessorSupport processorSupport)
    {
        if (node instanceof org.finos.legend.pure.m3.coreinstance.meta.pure.metamodel.PackageableElement)
        {
            return ((org.finos.legend.pure.m3.coreinstance.meta.pure.metamodel.PackageableElement) node)._package() != null;
        }
        return (!(node instanceof Any) || (node instanceof AbstractCoreInstanceWrapper)) &&
                (node.getValueForMetaPropertyToOne(M3Properties._package) != null) &&
                processorSupport.instance_instanceOf(node, M3Paths.PackageableElement);
    }

    static boolean isTopLevel(CoreInstance node, ProcessorSupport processorSupport)
    {
        try
        {
            return node == processorSupport.repository_getTopLevel(node.getName());
        }
        catch (Exception ignore)
        {
            return false;
        }
    }

    // Validation

    private static void validateStartNodePath(String path)
    {
        Objects.requireNonNull(path, "start node path may not be null");
        if (!isValidStartNodePath(path))
        {
            throw new IllegalArgumentException(StringEscape.escape(new StringBuilder("Invalid start node path '"), path).append("'").toString());
        }
    }

    private static String validateStartNodePath(String startNodePath, String description, boolean validate)
    {
        if (!validate || isValidStartNodePath(startNodePath))
        {
            return startNodePath;
        }

        throw new IllegalArgumentException(StringEscape.escape(newValidationMessageBuilder(description).append("invalid start node path '"), startNodePath).append("'").toString());
    }

    private static void validateProperty(String property)
    {
        Objects.requireNonNull(property, "property may not be null");
        if (!isValidPropertyName(property))
        {
            throw new IllegalArgumentException(StringEscape.escape(new StringBuilder("Invalid property name '"), property).append("'").toString());
        }
    }

    private static String validateProperty(String property, String description, boolean validate)
    {
        if (!validate || isValidPropertyName(property))
        {
            return property;
        }

        throw new IllegalArgumentException(StringEscape.escape(newValidationMessageBuilder(description).append("invalid property name '"), property).append("'").toString());
    }

    private static void validateKeyProperty(String keyProperty)
    {
        Objects.requireNonNull(keyProperty, "key property may not be null");
        if (!isValidPropertyName(keyProperty))
        {
            throw new IllegalArgumentException(StringEscape.escape(new StringBuilder("Invalid key property name '"), keyProperty).append("'").toString());
        }
    }

    private static String validateKeyProperty(String keyProperty, String description, boolean validate)
    {
        if (!validate || isValidPropertyName(keyProperty))
        {
            return keyProperty;
        }

        throw new IllegalArgumentException(StringEscape.escape(newValidationMessageBuilder(description).append("invalid key property name '"), keyProperty).append("'").toString());
    }

    private static void validateKey(String key)
    {
        Objects.requireNonNull(key, "key name may not be null");
    }

    private static void validateIndex(int index)
    {
        if (index < 0)
        {
            throw new IllegalArgumentException("Index must be non-negative: " + index);
        }
    }

    private static int validateIndex(int index, String description, boolean validate)
    {
        if (!validate || (index >= 0))
        {
            return index;
        }
        throw new IllegalArgumentException(newValidationMessageBuilder(description).append("index must be non-negative: ").append(index).toString());
    }

    private static StringBuilder newValidationMessageBuilder(String description)
    {
        return StringEscape.escape(new StringBuilder("Invalid GraphPath description '"), description).append("': ");
    }

    // Validation helpers

    private static boolean isValidStartNodePath(String path)
    {
        if (path == null)
        {
            return false;
        }
        if (PackageableElement.DEFAULT_PATH_SEPARATOR.equals(path))
        {
            return true;
        }

        int sepLen = PackageableElement.DEFAULT_PATH_SEPARATOR.length();
        int start = 0;
        int end;
        while ((end = path.indexOf(PackageableElement.DEFAULT_PATH_SEPARATOR, start)) != -1)
        {
            if (!isValidName(path, start, end))
            {
                return false;
            }
            start = end + sepLen;
        }
        return isValidName(path, start, path.length());
    }

    private static boolean isValidPropertyName(String name)
    {
        if (name == null)
        {
            return false;
        }
        int length = name.length();
        return ((length >= 2) && (name.charAt(0) == '\'')) ?
               (nextIndexOf(name, '\'', 1) == (length - 1)) :
               isValidName(name, 0, length);
    }

    private static boolean isValidName(String path, int start, int end)
    {
        if (start >= end)
        {
            return false;
        }
        int index = start;
        while (index < end)
        {
            int cp = path.codePointAt(index);
            if (!isValidNameCodePoint(cp))
            {
                return false;
            }
            index += Character.charCount(cp);
        }
        return (index == end);
    }

    private static boolean isValidNameCodePoint(int cp)
    {
        return ('_' == cp) || ('$' == cp) || Character.isLetterOrDigit(cp);
    }

    // Parsing

    private static Builder parseDescription(String description, Builder builder, boolean validate)
    {
        int index = nextIndexOf(description, '.', 0);
        if (index == -1)
        {
            builder.startNodePath = validateStartNodePath(trim(description), description, validate);
            builder.pathElements.clear();
            return builder;
        }

        String startNodePath = validateStartNodePath(trim(description, 0, index), description, validate);
        MutableList<Edge> edges = Lists.mutable.empty();
        int start = index + 1;
        int end;
        while ((end = nextIndexOf(description, '.', start)) != -1)
        {
            edges.add(parseEdge(description, start, end, validate));
            start = end + 1;
        }
        edges.add(parseEdge(description, start, description.length(), validate));

        builder.startNodePath = startNodePath;
        builder.pathElements.clear();
        builder.pathElements.addAll(edges);
        return builder;
    }

    private static Edge parseEdge(String description, int start, int end, boolean validate)
    {
        int openBracket = nextIndexOf(description, '[', start, end);
        if (openBracket == -1)
        {
            return new ToOnePropertyEdge(validateProperty(trim(description, start, end), description, validate));
        }
        String property = validateProperty(trim(description, start, openBracket), description, validate);
        int closeBracket = lastNonWhiteSpace(description, start, end);
        if (description.codePointAt(closeBracket) != ']')
        {
            throw new IllegalArgumentException("Invalid GraphPath description '" + StringEscape.escape(description) + "': missing ']' at index " + closeBracket);
        }
        int openQuote = nextIndexOf(description, '\'', openBracket + 1, closeBracket);
        if (openQuote == -1)
        {
            int index;
            try
            {
                String indexString = trim(description, openBracket + 1, closeBracket);
                index = Integer.parseInt((indexString == null) ? description.substring(openBracket + 1, closeBracket) : indexString);
            }
            catch (NumberFormatException e)
            {
                throw new IllegalArgumentException(newValidationMessageBuilder(description).append("index at ").append(openBracket + 1).append(" invalid").toString(), e);
            }
            return new ToManyPropertyAtIndexEdge(property, validateIndex(index, description, validate));
        }

        int closeQuote = nextIndexOf(description, '\'', openQuote + 1, closeBracket);
        if (closeQuote == -1)
        {
            throw new IllegalArgumentException("Invalid GraphPath description '" + StringEscape.escape(description) + "': missing closing quote at for quote at index " + openQuote);
        }
        String key = StringEscape.unescape(description.substring(openQuote + 1, closeQuote));

        int equals = nextIndexOf(description, '=', openBracket + 1, openQuote);
        String keyProperty = (equals == -1) ? M3Properties.name : validateKeyProperty(trim(description, openBracket + 1, equals), description, validate);
        return new ToManyPropertyWithStringKeyEdge(property, keyProperty, key);
    }

    // Parsing helpers

    private static String trim(String string)
    {
        return trim(string, 0, string.length());
    }

    private static String trim(String string, int start, int end)
    {
        int trimmedStart = nextNonWhiteSpace(string, start, end);
        if (trimmedStart != -1)
        {
            int cp;
            for (int index = end; index > start; index -= Character.charCount(cp))
            {
                cp = string.codePointBefore(index);
                if (!Character.isWhitespace(cp))
                {
                    return string.substring(trimmedStart, index);
                }
            }
        }
        return null;
    }

    private static int nextNonWhiteSpace(String string, int start, int end)
    {
        int index = start;
        while (index < end)
        {
            int cp = string.codePointAt(index);
            if (!Character.isWhitespace(cp))
            {
                return (index + Character.charCount(cp) > end) ? -1 : index;
            }
            index += Character.charCount(cp);
        }
        return -1;
    }

    private static int lastNonWhiteSpace(String string, int start, int end)
    {
        int index = end;
        while (index > start)
        {
            int cp = string.codePointBefore(index);
            index -= Character.charCount(cp);
            if (!Character.isWhitespace(cp))
            {
                return (index < start) ? -1 : index;
            }
        }
        return -1;
    }

    private static int nextIndexOf(String string, int target, int start)
    {
        return nextIndexOf(string, target, start, string.length());
    }

    private static int nextIndexOf(String string, int target, int start, int end)
    {
        int cp;
        boolean inQuotes = false;
        boolean escaped = false;
        for (int i = start; i < end; i += Character.charCount(cp))
        {
            cp = string.codePointAt(i);
            if (escaped)
            {
                escaped = false;
            }
            else if (!inQuotes && (cp == target))
            {
                return i;
            }
            else if (cp == '\\')
            {
                escaped = true;
            }
            else if (cp == '\'')
            {
                inQuotes = !inQuotes;
            }
        }
        return -1;
    }
}
