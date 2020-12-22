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

package org.finos.legend.pure.m3.tools;

import org.eclipse.collections.api.RichIterable;
import org.eclipse.collections.api.block.procedure.Procedure;
import org.eclipse.collections.api.list.ImmutableList;
import org.eclipse.collections.api.list.ListIterable;
import org.eclipse.collections.api.list.MutableList;
import org.eclipse.collections.impl.block.factory.Predicates;
import org.eclipse.collections.impl.factory.Lists;
import org.eclipse.collections.impl.lazy.AbstractLazyIterable;
import org.eclipse.collections.impl.utility.ArrayIterate;
import org.finos.legend.pure.m3.navigation.M3Paths;
import org.finos.legend.pure.m3.navigation.M3Properties;
import org.finos.legend.pure.m3.navigation.PackageableElement.PackageableElement;
import org.finos.legend.pure.m3.navigation.ProcessorSupport;
import org.finos.legend.pure.m4.coreinstance.CoreInstance;

import java.util.Iterator;
import java.util.NoSuchElementException;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class GraphPath
{
    private static final Pattern VALID_START_NODE_PATH = Pattern.compile("\\w++(::\\w[\\w$]*+)*+");

    private final String startNodePath;
    private final ImmutableList<Edge> edges;

    private GraphPath(String startNodePath, ListIterable<Edge> edges)
    {
        this.startNodePath = startNodePath;
        this.edges = (edges == null) ? Lists.immutable.empty() : edges.toImmutable();
    }

    public int getNodeCount()
    {
        return getEdgeCount() + 1;
    }

    public String getStartNodePath()
    {
        return this.startNodePath;
    }

    public RichIterable<CoreInstance> getNodes(ProcessorSupport processorSupport)
    {
        return new GraphPathNodeIterable(processorSupport);
    }

    public int getEdgeCount()
    {
        return this.edges.size();
    }

    public ListIterable<Edge> getEdges()
    {
        return this.edges;
    }

    public CoreInstance resolve(ProcessorSupport processorSupport)
    {
        CoreInstance node = getStartNode(processorSupport);
        for (int i = 0; i < this.edges.size(); i++)
        {
            node = applyPathElementAtIndex(node, i);
        }
        return node;
    }

    public GraphPath reduce(ProcessorSupport processorSupport)
    {
        if (this.edges.isEmpty())
        {
            // No path elements: impossible to reduce
            return this;
        }

        CoreInstance node = getStartNode(processorSupport);
        CoreInstance lastPackaged = node;
        int newStartIndex = 0;
        int pathElementCount = this.edges.size();
        for (int i = 0; i < pathElementCount; i++)
        {
            node = applyPathElementAtIndex(node, i);
            if ((node.getValueForMetaPropertyToOne(M3Properties._package) != null) && processorSupport.instance_instanceOf(node, M3Paths.PackageableElement))
            {
                lastPackaged = node;
                newStartIndex = i + 1;
            }
        }
        if (newStartIndex == 0)
        {
            // Not reducible
            return this;
        }
        if (newStartIndex == pathElementCount)
        {
            // Reduced to a new start node with no path elements
            return new GraphPath(PackageableElement.getUserPathForPackageableElement(lastPackaged), null);
        }
        // Reduced to a new start node with a reduced set of path elements
        return new GraphPath(PackageableElement.getUserPathForPackageableElement(lastPackaged), Lists.immutable.with(this.edges.subList(newStartIndex, pathElementCount).toArray(new Edge[pathElementCount - newStartIndex])));
    }

    public String getDescription()
    {
        StringBuilder builder = new StringBuilder(this.startNodePath.length() + (16 * this.edges.size()));
        writeDescription(builder);
        return builder.toString();
    }

    public void writeDescription(StringBuilder builder)
    {
        builder.append(this.startNodePath);
        this.edges.forEachWith(Edge::writeMessage, builder);
    }

    public String getPureExpression()
    {
        StringBuilder builder = new StringBuilder(this.startNodePath.length() + (16 * this.edges.size()));
        writePureExpression(builder);
        return builder.toString();
    }

    public void writePureExpression(StringBuilder builder)
    {
        builder.append(this.startNodePath);
        this.edges.forEachWith(Edge::writePureExpression, builder);
    }

    public boolean startsWith(GraphPath other)
    {
        if (this == other)
        {
            return true;
        }

        if (!this.startNodePath.equals(other.startNodePath) || (this.edges.size() < other.edges.size()))
        {
            return false;
        }
        for (int i = 0; i < other.edges.size(); i++)
        {
            if (!this.edges.get(i).equals(other.edges.get(i)))
            {
                return false;
            }
        }
        return true;
    }

    public Builder extend()
    {
        return new Builder(this);
    }

    public GraphPath withToOneProperty(String property)
    {
        return extend().addToOneProperty(property).build();
    }

    public GraphPath withToManyPropertyValueAtIndex(String property, int index)
    {
        return extend().addToManyPropertyValueAtIndex(property, index).build();
    }

    public GraphPath withToManyPropertyValueWithName(String property, String valueName)
    {
        return extend().addToManyPropertyValueWithName(property, valueName).build();
    }

    public GraphPath withToManyPropertyValueWithKey(String property, String keyProperty, String key)
    {
        return extend().addToManyPropertyValueWithKey(property, keyProperty, key).build();
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

        GraphPath that = (GraphPath)other;
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

    private CoreInstance getStartNode(ProcessorSupport processorSupport)
    {
        CoreInstance node = processorSupport.package_getByUserPath(this.startNodePath);
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
            StringBuilder message = new StringBuilder("Error accessing ");
            writeMessageUpTo(message, index);
            message.append(" (final node: ");
            message.append(previousNode);
            message.append(')');
            String errorMessage = e.getMessage();
            if (errorMessage != null)
            {
                message.append(": ");
                message.append(errorMessage);
            }
            throw new RuntimeException(message.toString(), e);
        }
        if (result == null)
        {
            StringBuilder message = new StringBuilder("Could not find ");
            writeMessageUpTo(message, index);
            throw new RuntimeException(message.toString());
        }
        return result;
    }

    private void writeMessageUpTo(StringBuilder message, int index)
    {
        message.append(this.startNodePath);
        for (int i = 0; i <= index; i++)
        {
            this.edges.get(i).writeMessage(message);
        }
    }

    private class GraphPathNodeIterable extends AbstractLazyIterable<CoreInstance>
    {
        private final ProcessorSupport processorSupport;

        private GraphPathNodeIterable(ProcessorSupport processorSupport)
        {
            this.processorSupport = processorSupport;
        }

        @Override
        public int size()
        {
            return getNodeCount();
        }

        @Override
        public boolean isEmpty()
        {
            return false;
        }

        @Override
        public void each(Procedure<? super CoreInstance> procedure)
        {
            CoreInstance node = getStartNode(this.processorSupport);
            procedure.value(node);
            for (int i = 0, edgeCount = getEdgeCount(); i < edgeCount; i++)
            {
                node = applyPathElementAtIndex(node, i);
                procedure.value(node);
            }
        }

        @Override
        public Iterator<CoreInstance> iterator()
        {
            return new GraphPathNodeIterator(this.processorSupport);
        }
    }

    private class GraphPathNodeIterator implements Iterator<CoreInstance>
    {
        private final ProcessorSupport processorSupport;
        private CoreInstance node = null;
        private int currentEdge = -1;

        private GraphPathNodeIterator(ProcessorSupport processorSupport)
        {
            this.processorSupport = processorSupport;
        }

        @Override
        public boolean hasNext()
        {
            return this.currentEdge < getEdgeCount();
        }

        @Override
        public CoreInstance next()
        {
            if (!hasNext())
            {
                throw new NoSuchElementException();
            }
            this.node = (this.node == null) ? getStartNode(this.processorSupport) : applyPathElementAtIndex(this.node, this.currentEdge);
            this.currentEdge++;
            return this.node;
        }

        @Override
        public void remove()
        {
            throw new UnsupportedOperationException();
        }
    }

    public static Builder newPathBuilder(String startNodePath)
    {
        if (startNodePath == null)
        {
            throw new IllegalArgumentException("Start node path may not be null");
        }
        if (!VALID_START_NODE_PATH.matcher(startNodePath).matches())
        {
            throw new IllegalArgumentException("Invalid start node path: " + startNodePath);
        }
        return new Builder(startNodePath);
    }

    public static Builder newPathBuilder(GraphPath path)
    {
        if (path == null)
        {
            throw new IllegalArgumentException("Path may not be null");
        }
        return new Builder(path);
    }

    public static GraphPath buildPath(String startNodePath)
    {
        return newPathBuilder(startNodePath).build();
    }

    public static GraphPath buildPath(String startNodePath, String... toOneProperties)
    {
        Builder builder = newPathBuilder(startNodePath);
        for (String toOneProperty : toOneProperties)
        {
            builder.addToOneProperty(toOneProperty);
        }
        return builder.build();
    }

    public static GraphPath parseDescription(String description)
    {
        if (description == null)
        {
            throw new IllegalArgumentException("GraphPath description may not be null");
        }
        int end = description.indexOf('.');
        if (end == -1)
        {
            return buildPath(description);
        }
        Builder builder = newPathBuilder(description.substring(0, end));
        int start = end + 1;
        end = description.indexOf('.', start);
        while (end != -1)
        {
            parseEdge(builder, description, start, end);
            start = end + 1;
            end = description.indexOf('.', start);
        }
        parseEdge(builder, description, start, description.length());
        return builder.build();
    }

    private static void parseEdge(Builder builder, String description, int start, int end)
    {
        Matcher matcher = ToOnePropertyEdge.PATTERN.matcher(description).region(start, end);
        if (matcher.matches())
        {
            builder.addToOneProperty(description.substring(start, end));
            return;
        }

        matcher = ToManyPropertyAtIndexEdge.PATTERN.matcher(description).region(start, end);
        if (matcher.matches())
        {
            String property = matcher.group(1);
            int index = Integer.parseInt(matcher.group(2));
            builder.addToManyPropertyValueAtIndex(property, index);
            return;
        }

        matcher = ToManyPropertyWithNameEdge.PATTERN.matcher(description).region(start, end);
        if (matcher.matches())
        {
            String property = matcher.group(1);
            String valueName = matcher.group(2);
            builder.addToManyPropertyValueWithName(property, valueName);
            return;
        }

        matcher = ToManyPropertyWithStringKeyEdge.PATTERN.matcher(description).region(start, end);
        if (matcher.matches())
        {
            String property = matcher.group(1);
            String keyProperty = matcher.group(2);
            String key = matcher.group(3);
            builder.addToManyPropertyValueWithKey(property, keyProperty, key);
            return;
        }

        throw new RuntimeException("Invalid GraphPath description (cannot parse region from " + start + " to " + end + ": " + description);
    }

    public static class Builder
    {
        private final String startNodePath;
        private final MutableList<Edge> pathElements;

        private Builder(String startNodePath)
        {
            this.startNodePath = startNodePath;
            this.pathElements = Lists.mutable.empty();
        }

        private Builder(GraphPath path)
        {
            this.startNodePath = path.startNodePath;
            this.pathElements = Lists.mutable.withAll(path.edges);
        }

        public GraphPath build()
        {
            return new GraphPath(this.startNodePath, this.pathElements);
        }

        public Builder addToOneProperty(String property)
        {
            if (property == null)
            {
                throw new IllegalArgumentException("Property may not be null");
            }
            return addEdge(new ToOnePropertyEdge(property));
        }

        public Builder addToOneProperties(String... properties)
        {
            if (!ArrayIterate.allSatisfy(properties, Predicates.notNull()))
            {
                throw new IllegalArgumentException("Properties may not be null");
            }
            for (int i = 0; i < properties.length; i++)
            {
                addEdge(new ToOnePropertyEdge(properties[i]));
            }
            return this;
        }

        public Builder addToManyPropertyValueAtIndex(String property, int index)
        {
            if (property == null)
            {
                throw new IllegalArgumentException("Property may not be null");
            }
            if (index < 0)
            {
                throw new IllegalArgumentException("Index must be non-negative: " + index);
            }
            return addEdge(new ToManyPropertyAtIndexEdge(property, index));
        }

        public Builder addToManyPropertyValueWithName(String property, String valueName)
        {
            if (property == null)
            {
                throw new IllegalArgumentException("Property may not be null");
            }
            if (valueName == null)
            {
                throw new IllegalArgumentException("Value name may not be null");
            }
            return addEdge(new ToManyPropertyWithNameEdge(property, valueName));
        }

        public Builder addToManyPropertyValueWithKey(String property, String keyProperty, String key)
        {
            if (property == null)
            {
                throw new IllegalArgumentException("Property may not be null");
            }
            if (keyProperty == null)
            {
                throw new IllegalArgumentException("Key property may not be null");
            }
            if (key == null)
            {
                throw new IllegalArgumentException("Key name may not be null");
            }
            return addEdge(new ToManyPropertyWithStringKeyEdge(property, keyProperty, key));
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

        @Override
        public String toString()
        {
            StringBuilder builder = new StringBuilder("<");
            builder.append(getClass().getSimpleName());
            builder.append("node");
            writeMessage(builder);
            builder.append('>');
            return builder.toString();
        }

        abstract CoreInstance apply(CoreInstance node);

        void writeMessage(StringBuilder message)
        {
            message.append('.');
            message.append(this.property);
        }

        void writePureExpression(StringBuilder expression)
        {
            expression.append('.');
            expression.append(this.property);
        }
    }

    private static class ToOnePropertyEdge extends Edge
    {
        private static final Pattern PATTERN = Pattern.compile("[a-zA-Z_][a-zA-Z0-9_]*+");

        private ToOnePropertyEdge(String property)
        {
            super(property);
        }

        @Override
        public boolean equals(Object other)
        {
            return (this == other) || ((other instanceof ToOnePropertyEdge) && this.property.equals(((ToOnePropertyEdge)other).property));
        }

        @Override
        public int hashCode()
        {
            return getClass().hashCode() ^ this.property.hashCode();
        }

        @Override
        CoreInstance apply(CoreInstance node)
        {
            return node.getValueForMetaPropertyToOne(this.property);
        }
    }

    private abstract static class ToManyPropertyEdge extends Edge
    {
        ToManyPropertyEdge(String property)
        {
            super(property);
        }

        @Override
        final void writeMessage(StringBuilder message)
        {
            super.writeMessage(message);
            message.append('[');
            writeToManySelectMessage(message);
            message.append(']');
        }

        @Override
        final void writePureExpression(StringBuilder expression)
        {
            super.writePureExpression(expression);
            writePureSelectExpression(expression);
        }

        abstract void writeToManySelectMessage(StringBuilder message);

        abstract void writePureSelectExpression(StringBuilder expression);
    }

    private static class ToManyPropertyAtIndexEdge extends ToManyPropertyEdge
    {
        private static final Pattern PATTERN = Pattern.compile("([a-zA-Z_][a-zA-Z0-9_]*+)\\[([0-9]++)\\]");

        private final int index;

        private ToManyPropertyAtIndexEdge(String property, int index)
        {
            super(property);
            this.index = index;
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
            ToManyPropertyAtIndexEdge that = (ToManyPropertyAtIndexEdge)other;
            return this.property.equals(that.property) && (this.index == that.index);
        }

        @Override
        public int hashCode()
        {
            return getClass().hashCode() ^ this.property.hashCode() ^ this.index;
        }

        @Override
        CoreInstance apply(CoreInstance node)
        {
            ListIterable<? extends CoreInstance> values = node.getValueForMetaPropertyToMany(this.property);
            return values.get(this.index);
        }

        @Override
        void writeToManySelectMessage(StringBuilder message)
        {
            message.append(this.index);
        }

        @Override
        void writePureSelectExpression(StringBuilder expression)
        {
            expression.append("->at(");
            expression.append(this.index);
            expression.append(')');
        }
    }

    private static class ToManyPropertyWithNameEdge extends ToManyPropertyEdge
    {
        private static final Pattern PATTERN = Pattern.compile("([a-zA-Z_][a-zA-Z0-9_]*+)\\['(.*)'\\]");

        private final String valueName;

        private ToManyPropertyWithNameEdge(String property, String valueName)
        {
            super(property);
            this.valueName = valueName;
        }

        @Override
        public boolean equals(Object other)
        {
            if (this == other)
            {
                return true;
            }
            if (!(other instanceof ToManyPropertyWithNameEdge))
            {
                return false;
            }
            ToManyPropertyWithNameEdge that = (ToManyPropertyWithNameEdge)other;
            return this.property.equals(that.property) && this.valueName.equals(that.valueName);
        }

        @Override
        public int hashCode()
        {
            return getClass().hashCode() ^ this.property.hashCode() ^ this.valueName.hashCode();
        }

        @Override
        CoreInstance apply(CoreInstance node)
        {
            return node.getValueInValueForMetaPropertyToMany(this.property, this.valueName);
        }

        @Override
        void writeToManySelectMessage(StringBuilder message)
        {
            message.append('\'');
            message.append(this.valueName);
            message.append('\'');
        }

        @Override
        void writePureSelectExpression(StringBuilder expression)
        {
            expression.append("->get('");
            expression.append(this.valueName);
            expression.append("')->toOne()");
        }
    }

    private static class ToManyPropertyWithStringKeyEdge extends ToManyPropertyEdge
    {
        private static final Pattern PATTERN = Pattern.compile("([a-zA-Z_][a-zA-Z0-9_]*+)\\[([a-zA-Z_][a-zA-Z0-9_]*+)='(.*)'\\]");

        private final String keyProperty;
        private final String key;

        private ToManyPropertyWithStringKeyEdge(String property, String keyProperty, String key)
        {
            super(property);
            this.keyProperty = keyProperty;
            this.key = key;
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
            ToManyPropertyWithStringKeyEdge that = (ToManyPropertyWithStringKeyEdge)other;
            return this.property.equals(that.property) && this.keyProperty.equals(that.keyProperty) && this.key.equals(that.key);
        }

        @Override
        public int hashCode()
        {
            return getClass().hashCode() ^ this.property.hashCode() ^ this.keyProperty.hashCode() ^ this.key.hashCode();
        }

        @Override
        CoreInstance apply(CoreInstance node)
        {
            return node.getValueInValueForMetaPropertyToManyWithKey(this.property, this.keyProperty, this.key);
        }

        @Override
        void writeToManySelectMessage(StringBuilder message)
        {
            message.append(this.keyProperty);
            message.append("='");
            message.append(this.key);
            message.append('\'');
        }

        @Override
        void writePureSelectExpression(StringBuilder expression)
        {
            expression.append("->filter(x | $x.");
            expression.append(this.keyProperty);
            expression.append(" == '");
            expression.append(this.key);
            expression.append("')->toOne()");
        }
    }
}
