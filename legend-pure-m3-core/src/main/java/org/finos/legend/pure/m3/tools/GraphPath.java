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
import org.eclipse.collections.api.factory.Lists;
import org.eclipse.collections.api.list.ImmutableList;
import org.eclipse.collections.api.list.ListIterable;
import org.eclipse.collections.api.list.MutableList;
import org.eclipse.collections.impl.lazy.AbstractLazyIterable;
import org.eclipse.collections.impl.utility.ArrayIterate;
import org.finos.legend.pure.m3.coreinstance.meta.pure.metamodel.type.Any;
import org.finos.legend.pure.m3.navigation.M3Paths;
import org.finos.legend.pure.m3.navigation.M3Properties;
import org.finos.legend.pure.m3.navigation.PackageableElement.PackageableElement;
import org.finos.legend.pure.m3.navigation.ProcessorSupport;
import org.finos.legend.pure.m4.coreinstance.CoreInstance;
import org.finos.legend.pure.m4.tools.SafeAppendable;

import java.util.Iterator;
import java.util.List;
import java.util.NoSuchElementException;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class GraphPath
{
    private static final Pattern VALID_START_NODE_PATH = Pattern.compile("(::)|((\\w[\\w$]*+::)*+)*+\\w[\\w$~]*+");

    private final String startNodePath;
    private final ImmutableList<Edge> edges;

    private GraphPath(String startNodePath, ImmutableList<Edge> edges)
    {
        this.startNodePath = startNodePath;
        this.edges = (edges == null) ? Lists.immutable.empty() : edges;
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

    public CoreInstance resolveUpTo(int end, ProcessorSupport processorSupport)
    {
        int resolvedEnd = (end < 0) ? (this.edges.size() + end) : end;
        if ((resolvedEnd > this.edges.size()) || (resolvedEnd < 0))
        {
            throw new IndexOutOfBoundsException("Index: " + end + "; size: " + this.edges.size());
        }
        CoreInstance node = getStartNode(processorSupport);
        for (int i = 0; i < resolvedEnd; i++)
        {
            node = applyPathElementAtIndex(node, i);
        }
        return node;
    }

    public CoreInstance resolve(ProcessorSupport processorSupport)
    {
        return resolveUpTo(getEdgeCount(), processorSupport);
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
            return new GraphPath(this.startNodePath, null);
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
            // No path elements: impossible to reduce
            return this;
        }

        CoreInstance node = getStartNode(processorSupport);
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
            return new GraphPath(newStartNodePath, null);
        }
        // Reduced to a new start node with a reduced set of path elements
        return new GraphPath(newStartNodePath, Lists.immutable.with(this.edges.subList(newStartIndex, pathElementCount).toArray(new Edge[pathElementCount - newStartIndex])));
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

        if ((this.edges.size() < other.edges.size()) || !this.startNodePath.equals(other.startNodePath))
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
            StringBuilder message = writeMessageUpTo(new StringBuilder("Error accessing "), index).append(" (final node: ").append(previousNode).append(')');
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
        return newPathBuilder(startNodePath).addToOneProperties(toOneProperties).build();
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
            builder.addEdge(parseEdge(description, start, end));
            start = end + 1;
            end = description.indexOf('.', start);
        }
        builder.addEdge(parseEdge(description, start, description.length()));
        return builder.build();
    }

    private static Edge parseEdge(String description, int start, int end)
    {
        Edge edge = ToOnePropertyEdge.tryParse(description, start, end);
        if (edge == null)
        {
            edge = ToManyPropertyAtIndexEdge.tryParse(description, start, end);
            if (edge == null)
            {
                edge = ToManyPropertyAtIndexEdge.tryParse(description, start, end);
                if (edge == null)
                {
                    edge = ToManyPropertyWithNameEdge.tryParse(description, start, end);
                    if (edge == null)
                    {
                        edge = ToManyPropertyWithStringKeyEdge.tryParse(description, start, end);
                        if (edge == null)
                        {
                            throw new RuntimeException("Invalid GraphPath description (cannot parse region from " + start + " to " + end + ": " + description);
                        }
                    }
                }
            }
        }
        return edge;
    }

    private static String getDescription(String startNodePath, ListIterable<? extends Edge> edges)
    {
        return writeDescription(new StringBuilder(startNodePath.length() + (16 * edges.size())), startNodePath, edges).toString();
    }

    private static <T extends Appendable> T writeDescription(T appendable, String startNodePath, ListIterable<? extends Edge> edges)
    {
        SafeAppendable safeAppendable = SafeAppendable.wrap(appendable).append(startNodePath);
        edges.forEachWith(Edge::writeMessage, safeAppendable);
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

        public String getStartNodePath()
        {
            return this.startNodePath;
        }

        public ListIterable<Edge> getEdges()
        {
            return this.pathElements.asUnmodifiable();
        }

        public String getDescription()
        {
            return GraphPath.getDescription(this.startNodePath, this.pathElements);
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
            if (property == null)
            {
                throw new IllegalArgumentException("Property may not be null");
            }
            return addEdge(new ToOnePropertyEdge(property));
        }

        public Builder addToOneProperties(String... properties)
        {
            ArrayIterate.forEach(properties, this::addToOneProperty);
            return this;
        }

        public Builder addToOneProperties(List<String> properties)
        {
            properties.forEach(this::addToOneProperty);
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

    private static class ToOnePropertyEdge extends Edge
    {
        private static final Pattern PATTERN = Pattern.compile("[a-zA-Z_]\\w*+");

        private ToOnePropertyEdge(String property)
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
            return getClass().hashCode() ^ this.property.hashCode();
        }

        @Override
        CoreInstance apply(CoreInstance node)
        {
            return node.getValueForMetaPropertyToOne(this.property);
        }

        static ToOnePropertyEdge tryParse(String string, int start, int end)
        {
            Matcher matcher = PATTERN.matcher(string).region(start, end);
            if (!matcher.matches())
            {
                return null;
            }

            String property = string.substring(start, end);
            return new ToOnePropertyEdge(property);
        }
    }

    private abstract static class ToManyPropertyEdge extends Edge
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

    private static class ToManyPropertyAtIndexEdge extends ToManyPropertyEdge
    {
        private static final Pattern PATTERN = Pattern.compile("([a-zA-Z_]\\w*+)\\[([0-9]++)]");

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
            ToManyPropertyAtIndexEdge that = (ToManyPropertyAtIndexEdge) other;
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
        SafeAppendable writeToManySelectMessage(SafeAppendable appendable)
        {
            return appendable.append(this.index);
        }

        @Override
        SafeAppendable writePureSelectExpression(SafeAppendable appendable)
        {
            return appendable.append("->at(").append(this.index).append(')');
        }

        static ToManyPropertyAtIndexEdge tryParse(String string, int start, int end)
        {
            Matcher matcher = PATTERN.matcher(string).region(start, end);
            if (!matcher.matches())
            {
                return null;
            }

            String property = matcher.group(1);
            int index = Integer.parseInt(matcher.group(2));
            return new ToManyPropertyAtIndexEdge(property, index);
        }
    }

    private static class ToManyPropertyWithNameEdge extends ToManyPropertyEdge
    {
        private static final Pattern PATTERN = Pattern.compile("([a-zA-Z_]\\w*+)\\['(.*)']");

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
            ToManyPropertyWithNameEdge that = (ToManyPropertyWithNameEdge) other;
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
        SafeAppendable writeToManySelectMessage(SafeAppendable appendable)
        {
            return appendable.append('\'').append(this.valueName).append('\'');
        }

        @Override
        SafeAppendable writePureSelectExpression(SafeAppendable appendable)
        {
            return appendable.append("->get('").append(this.valueName).append("')->toOne()");
        }

        static ToManyPropertyWithNameEdge tryParse(String string, int start, int end)
        {
            Matcher matcher = PATTERN.matcher(string).region(start, end);
            if (!matcher.matches())
            {
                return null;
            }

            String property = matcher.group(1);
            String valueName = matcher.group(2);
            return new ToManyPropertyWithNameEdge(property, valueName);
        }
    }

    private static class ToManyPropertyWithStringKeyEdge extends ToManyPropertyEdge
    {
        private static final Pattern PATTERN = Pattern.compile("([a-zA-Z_]\\w*+)\\[([a-zA-Z_]\\w*+)='(.*)']");

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
            ToManyPropertyWithStringKeyEdge that = (ToManyPropertyWithStringKeyEdge) other;
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
        SafeAppendable writeToManySelectMessage(SafeAppendable appendable)
        {
            return appendable.append(this.keyProperty).append("='").append(this.key).append('\'');
        }

        @Override
        SafeAppendable writePureSelectExpression(SafeAppendable appendable)
        {
            return appendable.append("->filter(x | $x.").append(this.keyProperty).append(" == '").append(this.key).append("')->toOne()");
        }

        static ToManyPropertyWithStringKeyEdge tryParse(String string, int start, int end)
        {
            Matcher matcher = PATTERN.matcher(string).region(start, end);
            if (!matcher.matches())
            {
                return null;
            }

            String property = matcher.group(1);
            String keyProperty = matcher.group(2);
            String key = matcher.group(3);
            return new ToManyPropertyWithStringKeyEdge(property, keyProperty, key);
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
        return !(node instanceof Any) && (node.getValueForMetaPropertyToOne(M3Properties._package) != null) && processorSupport.instance_instanceOf(node, M3Paths.PackageableElement);
    }

    static boolean isTopLevel(CoreInstance node, ProcessorSupport processorSupport)
    {
        CoreInstance topLevel;
        try
        {
            topLevel = processorSupport.repository_getTopLevel(node.getName());
        }
        catch (Exception ignore)
        {
            return false;
        }
        return node == topLevel;
    }
}