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

import org.antlr.v4.runtime.BailErrorStrategy;
import org.antlr.v4.runtime.CharStreams;
import org.antlr.v4.runtime.CommonTokenStream;
import org.antlr.v4.runtime.atn.PredictionMode;
import org.antlr.v4.runtime.misc.ParseCancellationException;
import org.eclipse.collections.api.factory.Lists;
import org.eclipse.collections.api.list.ImmutableList;
import org.eclipse.collections.api.list.ListIterable;
import org.eclipse.collections.api.list.MutableList;
import org.eclipse.collections.impl.utility.ArrayIterate;
import org.finos.legend.pure.m3.coreinstance.meta.pure.metamodel.type.Any;
import org.finos.legend.pure.m3.navigation.M3Paths;
import org.finos.legend.pure.m3.navigation.M3Properties;
import org.finos.legend.pure.m3.navigation.PackageableElement.PackageableElement;
import org.finos.legend.pure.m3.navigation.ProcessorSupport;
import org.finos.legend.pure.m3.serialization.grammar.m3parser.antlr.M3Lexer;
import org.finos.legend.pure.m3.serialization.grammar.m3parser.antlr.M3Parser;
import org.finos.legend.pure.m4.coreinstance.CoreInstance;
import org.finos.legend.pure.m4.coreinstance.SourceInformation;
import org.finos.legend.pure.m4.serialization.grammar.StringEscape;
import org.finos.legend.pure.m4.tools.SafeAppendable;

import java.util.List;
import java.util.Objects;
import java.util.function.Consumer;

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
        CoreInstance node = getStartNode(processorSupport);
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

    public ResolvedGraphPath resolveFully(ProcessorSupport processorSupport)
    {
        MutableList<CoreInstance> nodes = Lists.mutable.ofInitialCapacity(getNodeCount());
        forEachNode(nodes::add, processorSupport);
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
        return new Builder();
    }

    public static Builder builder(int initEdgeCapacity)
    {
        return new Builder(initEdgeCapacity);
    }

    public static Builder builder(String startNodePath)
    {
        return builder().withStartNodePath(startNodePath);
    }

    public static Builder builder(GraphPath path)
    {
        return new Builder(Objects.requireNonNull(path, "path may not be null"));
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
        return builder().withStartNodePath(startNodePath).build();
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

    public static class Builder
    {
        private M3Lexer lexer;
        private M3Parser parser;
        private String startNodePath;
        private final MutableList<Edge> pathElements;

        private Builder()
        {
            this.pathElements = Lists.mutable.empty();
        }

        private Builder(int initEdgeCapacity)
        {
            this.pathElements = Lists.mutable.withInitialCapacity(initEdgeCapacity);
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

        public void setStartNodePath(String path)
        {
            initParser(path);
            M3Parser.GraphPathStartNodeContext context;
            try
            {
                context = this.parser.graphPathStartNode();
            }
            catch (Exception e)
            {
                throw new IllegalArgumentException("Invalid GraphPath start node path '" + StringEscape.escape(path) + "'", (e instanceof ParseCancellationException) ? e.getCause() : e);
            }
            this.startNodePath = context.getText();
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
            // parse
            initParser(description);
            M3Parser.GraphPathContext context;
            try
            {
                context = this.parser.graphPath();
            }
            catch (Exception e)
            {
                throw new IllegalArgumentException("Invalid GraphPath description '" + StringEscape.escape(description) + "'", (e instanceof ParseCancellationException) ? e.getCause() : e);
            }

            // check that there's nothing more in the string (except possibly whitespace)
            for (int i = context.getStop().getStopIndex() + 1, len = description.length(); i < len;)
            {
                int codePoint = description.codePointAt(i);
                if (!Character.isWhitespace(codePoint))
                {
                    throw new IllegalArgumentException("Invalid GraphPath description '" + StringEscape.escape(description) + "': error at index " + i);
                }
                i += Character.charCount(codePoint);
            }

            // build graph path
            MutableList<Edge> edges = Lists.mutable.empty();
            List<M3Parser.GraphPathEdgeContext> edgeContexts = context.graphPathEdge();
            if (edgeContexts != null)
            {
                edgeContexts.forEach(edgeContext ->
                {
                    List<M3Parser.PropertyNameContext> propertyNameContexts = edgeContext.propertyName();
                    String property = propertyNameContexts.get(0).getText();
                    if (edgeContext.INTEGER() != null)
                    {
                        int index;
                        try
                        {
                            index = Integer.parseInt(edgeContext.INTEGER().getText());
                        }
                        catch (NumberFormatException e)
                        {
                            throw new IllegalArgumentException("Invalid GraphPath description '" + StringEscape.escape(description) + "': index at " + edgeContext.INTEGER().getSymbol().getStartIndex() + " invalid", e);
                        }
                        edges.add(new ToManyPropertyAtIndexEdge(property, validateIndex(index)));
                    }
                    else if (edgeContext.STRING() != null)
                    {
                        String withQuote = StringEscape.unescape(edgeContext.STRING().getText());
                        String key = withQuote.substring(1, withQuote.length() - 1);
                        String keyProperty = (propertyNameContexts.size() == 1) ? M3Properties.name : propertyNameContexts.get(1).getText();
                        edges.add(new ToManyPropertyWithStringKeyEdge(property, keyProperty, key));
                    }
                    else
                    {
                        edges.add(new ToOnePropertyEdge(property));
                    }
                });
            }

            this.startNodePath = context.graphPathStartNode().getText();
            this.pathElements.clear();
            this.pathElements.addAll(edges);
            return this;
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
            return addEdge(new ToOnePropertyEdge(validateProperty(property)));
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
            return addEdge(new ToManyPropertyAtIndexEdge(validateProperty(property), validateIndex(index)));
        }

        public Builder addToManyPropertyValueWithName(String property, String valueName)
        {
            return addToManyPropertyValueWithKey(property, M3Properties.name, valueName);
        }

        public Builder addToManyPropertyValueWithKey(String property, String keyProperty, String key)
        {
            return addEdge(new ToManyPropertyWithStringKeyEdge(validateProperty(property), validateKeyProperty(keyProperty), validateKey(key)));
        }

        private Builder addEdge(Edge pathElement)
        {
            this.pathElements.add(pathElement);
            return this;
        }

        private String validateProperty(String property)
        {
            initParser(Objects.requireNonNull(property, "property may not be null"));
            try
            {
                return this.parser.propertyName().getText();
            }
            catch (Exception e)
            {
                throw new IllegalArgumentException("Invalid property name '" + StringEscape.escape(property) + "'", (e instanceof ParseCancellationException) ? e.getCause() : e);
            }
        }

        private int validateIndex(int index)
        {
            if (index < 0)
            {
                throw new IllegalArgumentException("Index must be non-negative: " + index);
            }
            return index;
        }

        private String validateKeyProperty(String keyProperty)
        {
            initParser(Objects.requireNonNull(keyProperty, "key property may not be null"));
            try
            {
                return this.parser.propertyName().getText();
            }
            catch (Exception e)
            {
                throw new IllegalArgumentException("Invalid key property name '" + StringEscape.escape(keyProperty) + "\"", (e instanceof ParseCancellationException) ? e.getCause() : e);
            }
        }

        private String validateKey(String key)
        {
            return Objects.requireNonNull(key, "key name may not be null");
        }

        private void initParser(String text)
        {
            if (this.lexer == null)
            {
                this.lexer = new M3Lexer(CharStreams.fromString(text));
                this.lexer.removeErrorListeners();
            }
            else
            {
                this.lexer.setInputStream(CharStreams.fromString(text));
            }

            if (this.parser == null)
            {
                this.parser = new M3Parser(new CommonTokenStream(this.lexer));
                this.parser.removeErrorListeners();
                this.parser.setErrorHandler(new BailErrorStrategy());
                this.parser.getInterpreter().setPredictionMode(PredictionMode.SLL);
            }
            else
            {
                this.parser.setTokenStream(new CommonTokenStream(this.lexer));
            }
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
