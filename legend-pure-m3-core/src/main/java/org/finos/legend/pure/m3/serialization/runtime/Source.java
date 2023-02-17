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

package org.finos.legend.pure.m3.serialization.runtime;

import org.apache.commons.lang3.StringUtils;
import org.eclipse.collections.api.RichIterable;
import org.eclipse.collections.api.block.function.Function;
import org.eclipse.collections.api.block.predicate.Predicate;
import org.eclipse.collections.api.factory.Lists;
import org.eclipse.collections.api.factory.Sets;
import org.eclipse.collections.api.factory.Stacks;
import org.eclipse.collections.api.list.ImmutableList;
import org.eclipse.collections.api.list.ListIterable;
import org.eclipse.collections.api.list.MutableList;
import org.eclipse.collections.api.multimap.Multimap;
import org.eclipse.collections.api.multimap.list.ImmutableListMultimap;
import org.eclipse.collections.api.multimap.list.ListMultimap;
import org.eclipse.collections.api.set.ImmutableSet;
import org.eclipse.collections.api.set.MutableSet;
import org.eclipse.collections.api.stack.MutableStack;
import org.eclipse.collections.impl.factory.Multimaps;
import org.eclipse.collections.impl.utility.StringIterate;
import org.finos.legend.pure.m3.coreinstance.Package;
import org.finos.legend.pure.m3.coreinstance.meta.pure.metamodel.function.ConcreteFunctionDefinition;
import org.finos.legend.pure.m3.coreinstance.meta.pure.metamodel.function.LambdaFunctionInstance;
import org.finos.legend.pure.m3.coreinstance.meta.pure.metamodel.function.property.AbstractProperty;
import org.finos.legend.pure.m3.coreinstance.meta.pure.metamodel.type.generics.GenericType;
import org.finos.legend.pure.m3.coreinstance.meta.pure.metamodel.valuespecification.FunctionExpression;
import org.finos.legend.pure.m3.coreinstance.meta.pure.metamodel.valuespecification.InstanceValue;
import org.finos.legend.pure.m3.coreinstance.meta.pure.metamodel.valuespecification.InstanceValueInstance;
import org.finos.legend.pure.m3.coreinstance.meta.pure.metamodel.valuespecification.SimpleFunctionExpression;
import org.finos.legend.pure.m3.coreinstance.meta.pure.metamodel.valuespecification.ValueSpecification;
import org.finos.legend.pure.m3.coreinstance.meta.pure.metamodel.valuespecification.VariableExpression;
import org.finos.legend.pure.m3.coreinstance.meta.pure.metamodel.valuespecification.VariableExpressionInstance;
import org.finos.legend.pure.m3.coreinstance.meta.pure.tools.GrammarInfoStub;
import org.finos.legend.pure.m3.navigation.M3Properties;
import org.finos.legend.pure.m3.navigation.PackageableElement.PackageableElement;
import org.finos.legend.pure.m3.navigation.ProcessorSupport;
import org.finos.legend.pure.m3.navigation.importstub.ImportStub;
import org.finos.legend.pure.m3.navigation.type.Type;
import org.finos.legend.pure.m3.serialization.filesystem.usercodestorage.MutableCodeStorage;
import org.finos.legend.pure.m3.serialization.grammar.Parser;
import org.finos.legend.pure.m3.serialization.runtime.navigation.NavigationHandler;
import org.finos.legend.pure.m4.coreinstance.CoreInstance;
import org.finos.legend.pure.m4.coreinstance.SourceInformation;

import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class Source
{
    private static final int SEARCH_TEXT_PREVIEW_CHARACTER_LIMIT = 25;
    public static final Function<Source, String> SOURCE_ID = Source::getId;
    public static final Function<Source, ListIterable<CoreInstance>> SOURCE_NEW_INSTANCES = Source::getNewInstances;
    public static final Predicate<Source> IS_COMPILED = Source::isCompiled;

    private static final Pattern LINE_PATTERN = Pattern.compile("^.*$", Pattern.MULTILINE);

    private final Object lock = new Object();

    private SourceRegistry sourceRegistry;

    private final String id;
    private String content;

    private final boolean immutable;
    private final boolean inMemory;
    private boolean compiled;

    private ImmutableList<CoreInstance> newInstances;
    private ImmutableSet<CoreInstance> allInstances;
    private ImmutableListMultimap<Parser, CoreInstance> elementsByParser;

    public Source(String id, boolean immutable, boolean inMemory, String content)
    {
        this.id = id;
        this.inMemory = inMemory;
        this.immutable = immutable;
        this.compiled = this.immutable;
        this.content = (content == null) ? "" : content;
        if (this.content.isEmpty())
        {
            this.compiled = true;
            this.newInstances = Lists.immutable.with();
            this.elementsByParser = Multimaps.immutable.list.with();
        }
    }

    public boolean refreshContent()
    {
        if (isImmutable() || isInMemory())
        {
            return false;
        }
        synchronized (this.lock)
        {
            MutableCodeStorage codeStorage = this.sourceRegistry.getCodeStorage();
            if (codeStorage.exists(this.id))
            {
                String currentContent = this.content;
                String contentFromStorage = codeStorage.getContentAsText(this.id);
                if (currentContent.equals(contentFromStorage))
                {
                    return false;
                }

                String oldContent = this.content;
                this.content = (contentFromStorage == null) ? "" : contentFromStorage;
                this.sourceRegistry.getSourceEventHandlers().forEach(eh -> eh.updateSource(this, oldContent));
                unCompile();
            }
            else
            {
                this.sourceRegistry.unregisterSource(this.id);
                this.sourceRegistry.getSourceEventHandlers().forEach(eh -> eh.deleteSource(this));
            }
            return true;
        }
    }

    void updateContent(String content)
    {
        if (!isImmutable())
        {
            synchronized (this.lock)
            {
                if (isModified(content))
                {
                    if (!isInMemory())
                    {
                        this.sourceRegistry.getCodeStorage().writeContent(this.id, content);
                    }
                    String oldContent = this.content;
                    this.content = content;
                    this.sourceRegistry.getSourceEventHandlers().forEach(eh -> eh.updateSource(this, oldContent));
                    unCompile();
                }
            }
        }
    }

    public void delete(boolean logical)
    {
        if (!isImmutable())
        {
            synchronized (this.lock)
            {
                if (!isInMemory() && !logical)
                {
                    this.sourceRegistry.getCodeStorage().deleteFile(this.id);
                }

                this.sourceRegistry.unregisterSource(this.id);
                this.sourceRegistry.getSourceEventHandlers().forEach(eh -> eh.deleteSource(this));
            }
        }
    }

    public void moveSource(String destinationId)
    {
        if (!isImmutable())
        {
            synchronized (this.lock)
            {
                this.sourceRegistry.registerSource(new Source(destinationId, this.isImmutable(), this.isInMemory(), this.getContent()));
                this.sourceRegistry.getSourceEventHandlers().forEach(eh -> eh.moveSource(this, this.sourceRegistry.getSource(destinationId)));
                this.sourceRegistry.unregisterSource(this.id);
            }
        }
    }

    private void unCompile()
    {
        synchronized (this.lock)
        {
            this.compiled = this.immutable;
            this.elementsByParser = null;
            this.newInstances = null;
            this.allInstances = null;
        }
    }

    private NavigationHandler<?> getNavigationHandler(CoreInstance found, ProcessorSupport processorSupport)
    {
        for (CoreInstance type : Type.getGeneralizationResolutionOrder(found.getClassifier(), processorSupport))
        {
            String path = PackageableElement.getUserPathForPackageableElement(type);
            NavigationHandler<?> handler = this.sourceRegistry.getNavigationHandler(path);
            if (handler != null)
            {
                return handler;
            }
        }
        return null;
    }

    public void setCompiled(boolean compiled)
    {
        if (!isImmutable())
        {
            synchronized (this.lock)
            {
                if (!compiled)
                {
                    unCompile();
                }
                this.compiled = compiled;
            }
        }
    }

    public boolean isCompiled()
    {
        return this.compiled;
    }

    public ListIterable<CoreInstance> getNewInstances()
    {
        return this.newInstances;
    }

    void linkInstances(ListMultimap<Parser, CoreInstance> elementsByParser)
    {
        synchronized (this.lock)
        {
            this.newInstances = Lists.immutable.withAll(elementsByParser.valuesView());
            this.allInstances = null;
            this.elementsByParser = elementsByParser.toImmutable();
        }
    }

    public ListMultimap<Parser, CoreInstance> getElementsByParser()
    {
        return this.elementsByParser;
    }

    @Override
    public boolean equals(Object o)
    {
        return (this == o) || ((o instanceof Source) && this.id.equals(((Source) o).id));
    }

    @Override
    public int hashCode()
    {
        return this.id.hashCode();
    }

    @Override
    public String toString()
    {
        return "<Source id=" + this.id + " immutable=" + this.immutable + " inMemory=" + this.inMemory + " compiled=" + this.compiled + ">";
    }

    public String getId()
    {
        return this.id;
    }

    public String getContent()
    {
        return this.content;
    }

    public boolean isInMemory()
    {
        return this.inMemory;
    }

    public boolean isImmutable()
    {
        return this.immutable;
    }

    public CoreInstance navigate(int line, int column, ProcessorSupport processorSupport)
    {
        CoreInstance found = findElementAt(line, column);
        if (found != null)
        {
            // TODO remove type specific code - find a better way to do this
            if (found instanceof GenericType)
            {
                found = found.getValueForMetaPropertyToOne(M3Properties.rawType);
            }

            else if (found instanceof VariableExpression)
            {
                found = resolveVariableOrParameter(line, column, (VariableExpression) found);
            }

            else if (found instanceof InstanceValue)
            {
                if (!Type.isPrimitiveType(found.getValueForMetaPropertyToOne(M3Properties.genericType).getValueForMetaPropertyToOne(M3Properties.rawType), processorSupport))
                {
                    if (found.getValueForMetaPropertyToOne(M3Properties.values) != null)
                    {
                        found = found.getValueForMetaPropertyToOne(M3Properties.values);
                    }
                    else
                    {
                        // New & Copy use case
                        found = found.getValueForMetaPropertyToOne(M3Properties.genericType).getValueForMetaPropertyToOne(M3Properties.typeArguments).getValueForMetaPropertyToOne(M3Properties.rawType);
                    }
                }
            }

            else if (found instanceof FunctionExpression)
            {
                CoreInstance func = found.getValueForMetaPropertyToOne(M3Properties.func);
                // enum instance
                if ("extractEnumValue_Enumeration_1__String_1__T_1_".equals(func.getName()))
                {
                    CoreInstance enumType = found.getValueForMetaPropertyToOne(M3Properties.genericType).getValueForMetaPropertyToOne(M3Properties.rawType);
                    try
                    {
                        String enumValue = found.getValueForMetaPropertyToMany(M3Properties.parametersValues).get(1).getValueForMetaPropertyToOne(M3Properties.values).getName();
                        found = enumType.getValueInValueForMetaPropertyToMany(M3Properties.values, enumValue);
                    }
                    catch (Exception ignore)
                    {
                        // with best effort, show the enumeration instead
                        found = enumType;
                    }
                }
                else
                {
                    found = func;
                }
            }

            else if (found instanceof GrammarInfoStub)
            {
                found = found.getValueForMetaPropertyToOne(M3Properties.value);
            }

            else
            {
                NavigationHandler handler = this.getNavigationHandler(found, processorSupport);
                if (handler != null)
                {
                    found = handler.findNavigationElement(found, this.getContent(), line, column, processorSupport);
                }
            }
        }

        return ImportStub.withImportStubByPass(found, processorSupport);
    }

    public ListIterable<CoreInstance> findFunctionsOrLambasAt(int line, int column)
    {
        return findRawElementsAt(line, column)
                .select(entry -> entry instanceof LambdaFunctionInstance || entry instanceof ConcreteFunctionDefinition, Lists.mutable.empty())
                // NOTE: since the position made up of the line and column is guaranteed to be within the specified source informations
                // we now just need to find the narrowest source information hence the comparator used
                .sortThis((entry1, entry2) -> Source.compareSourceInformation(entry1.getSourceInformation(), entry2.getSourceInformation()));
    }

    private CoreInstance resolveVariableOrParameter(int line, int column, VariableExpression variable)
    {
        String varName = variable._name();
        // NOTE: here we are only interested in lamba functions (i.e. function type) and a single concrete function definition
        // we then sort them by how close their scope is to the position of selection, the logic here is that the closer
        // the lambda function is to the position, the closer its scope and thus if a match in parameter/variable name is found
        // in that scope would finish our lookup
        ListIterable<CoreInstance> functionsOrLambdas = findFunctionsOrLambasAt(line, column);
        for (CoreInstance fn : functionsOrLambdas)
        {
            // scan for the let expressions then follows by the parameters
            RichIterable<InstanceValueInstance> letVars = fn.getValueForMetaPropertyToMany(M3Properties.expressionSequence)
                    .select(expression -> expression instanceof SimpleFunctionExpression && "letFunction".equals(((SimpleFunctionExpression) expression)._functionName()))
                    .collect(expression -> ((SimpleFunctionExpression) expression)._parametersValues().toList().getFirst())
                    // NOTE: make sure to only consider let statements prior to the call
                    .select(letVar -> letVar.getSourceInformation().getEndLine() < line || (letVar.getSourceInformation().getEndLine() == line && letVar.getSourceInformation().getEndColumn() < column))
                    .selectInstancesOf(InstanceValueInstance.class);
            for (InstanceValueInstance var : letVars)
            {
                if (varName.equals(var.getValueForMetaPropertyToOne(M3Properties.values).getName()))
                {
                    return var;
                }
            }
            RichIterable<VariableExpressionInstance> params = fn.getValueForMetaPropertyToOne(M3Properties.classifierGenericType)
                    .getValueForMetaPropertyToOne(M3Properties.typeArguments)
                    .getValueForMetaPropertyToOne(M3Properties.rawType)
                    .getValueForMetaPropertyToMany(M3Properties.parameters)
                    .selectInstancesOf(VariableExpressionInstance.class);
            for (VariableExpressionInstance var : params)
            {
                if (varName.equals(var._name()))
                {
                    return var;
                }
            }
        }
        return variable;
    }

    @Deprecated
    public CoreInstance findElementAt(int line, int column, ProcessorSupport processorSupport)
    {
        return findElementAt(line, column);
    }

    public CoreInstance findElementAt(int line, int column)
    {
        ListIterable<CoreInstance> elements = findRawElementsAt(line, column);
        switch (elements.size())
        {
            case 0:
            {
                return null;
            }
            case 1:
            {
                return elements.get(0);
            }
            default:
            {
                Multimap<SourceInformation, CoreInstance> elementsBySourceInfo = elements.groupBy(CoreInstance::getSourceInformation);
                // NOTE: since the position made up of the line and column is guaranteed to be within the specified source informations
                // we now just need to find the narrowest source information hence the comparator used
                SourceInformation minSourceInfo = elementsBySourceInfo.keysView().min(Source::compareSourceInformation);
                RichIterable<CoreInstance> results = elementsBySourceInfo.get(minSourceInfo);
                if (results.size() == 1)
                {
                    return results.getAny();
                }
                else
                {
                    // NOTE: here, we check for function expression first
                    // which is a bias in favor of function and property usage
                    // TODO find a better way to do this
                    return results.maxBy(result ->
                    {
                        if (result instanceof FunctionExpression)
                        {
                            return 5;
                        }
                        if (result instanceof InstanceValue)
                        {
                            return 4;
                        }
                        if (result instanceof ValueSpecification)
                        {
                            return 3;
                        }
                        if (result instanceof AbstractProperty)
                        {
                            return 2;
                        }
                        if (result instanceof org.finos.legend.pure.m3.coreinstance.meta.pure.metamodel._import.ImportStub)
                        {
                            return 1;
                        }
                        return 0;
                    });
                }
            }
        }
    }

    /**
     * Attempt to find the narrower source information range
     * without going through the source and do character counting
     * as that's expensive
     */
    private static int compareSourceInformation(SourceInformation s1, SourceInformation s2)
    {
        if (s1 == s2)
        {
            return 0;
        }

        int startLine1 = s1.getStartLine();
        int startLine2 = s2.getStartLine();
        if (startLine1 != startLine2)
        {
            return startLine2 - startLine1;
        }

        int startColumn1 = s1.getStartColumn();
        int startColumn2 = s2.getStartColumn();
        if (startColumn1 != startColumn2)
        {
            return startColumn2 - startColumn1;
        }

        int endLine1 = s1.getEndLine();
        int endLine2 = s2.getEndLine();
        if (endLine1 != endLine2)
        {
            return endLine1 - endLine2;
        }

        int endColumn1 = s1.getEndColumn();
        int endColumn2 = s2.getEndColumn();
        if (endColumn1 != endColumn2)
        {
            return endColumn1 - endColumn2;
        }

        int line1 = s1.getLine();
        int line2 = s2.getLine();
        if (line1 != line2)
        {
            return line1 - line2;
        }

        int column1 = s1.getColumn();
        int column2 = s2.getColumn();
        return column1 - column2;
    }

    public RichIterable<SourceCoordinates> find(Pattern pattern)
    {
        if (pattern.matcher("").matches() || StringIterate.isEmpty(this.content))
        {
            return Lists.immutable.empty();
        }

        MutableList<SourceCoordinates> results = Lists.mutable.with();
        Matcher lines = LINE_PATTERN.matcher(this.content);
        for (int i = 0; lines.find(); i++)
        {
            String line = lines.group();
            Matcher matcher = pattern.matcher(lines.group());
            while (matcher.find())
            {
                results.add(new SourceCoordinates(this.id, i + 1, matcher.start() + 1, i + 1, matcher.end(),
                        new SourceCoordinates.Preview(
                                StringUtils.stripStart(line.substring(Math.max(0, matcher.start() - SEARCH_TEXT_PREVIEW_CHARACTER_LIMIT), matcher.start()), null),
                                line.substring(matcher.start(), matcher.end()),
                                StringUtils.stripEnd(line.substring(matcher.end(), Math.min(line.length(), matcher.end() + SEARCH_TEXT_PREVIEW_CHARACTER_LIMIT)), null)
                        )));
            }
        }
        return results;
    }

    public RichIterable<SourceCoordinates> find(String string)
    {
        return find(string, true);
    }

    public RichIterable<SourceCoordinates> find(String string, boolean caseSensitive)
    {
        if (string.isEmpty() || StringIterate.isEmpty(this.content))
        {
            return Lists.immutable.empty();
        }

        return caseSensitive ? findCaseSensitive(string) : findCaseInsensitive(string);
    }

    private RichIterable<SourceCoordinates> findCaseSensitive(String string)
    {
        MutableList<SourceCoordinates> results = Lists.mutable.with();
        Matcher lines = LINE_PATTERN.matcher(this.content);
        int length = string.length();
        for (int i = 0; lines.find(); i++)
        {
            String line = lines.group();
            for (int index = line.indexOf(string); index != -1; index = line.indexOf(string, index + 1))
            {
                results.add(new SourceCoordinates(this.id, i + 1, index + 1, i + 1, index + length,
                        new SourceCoordinates.Preview(
                                StringUtils.stripStart(line.substring(Math.max(0, index - SEARCH_TEXT_PREVIEW_CHARACTER_LIMIT), index), null),
                                line.substring(index, index + length),
                                StringUtils.stripEnd(line.substring(index + length, Math.min(line.length(), index + length + SEARCH_TEXT_PREVIEW_CHARACTER_LIMIT)), null)
                        )));
            }
        }
        return results;
    }

    private RichIterable<SourceCoordinates> findCaseInsensitive(String string)
    {
        String lowerCase = string.toLowerCase();
        MutableList<SourceCoordinates> results = Lists.mutable.with();
        Matcher lines = LINE_PATTERN.matcher(this.content);
        int length = lowerCase.length();
        for (int i = 0; lines.find(); i++)
        {
            String originalLine = lines.group();
            String line = originalLine.toLowerCase();
            for (int index = line.indexOf(lowerCase); index != -1; index = line.indexOf(lowerCase, index + 1))
            {
                results.add(new SourceCoordinates(this.id, i + 1, index + 1, i + 1, index + length,
                        new SourceCoordinates.Preview(
                                StringUtils.stripStart(originalLine.substring(Math.max(0, index - SEARCH_TEXT_PREVIEW_CHARACTER_LIMIT), index), null),
                                originalLine.substring(index, index + length),
                                StringUtils.stripEnd(originalLine.substring(index + length, Math.min(line.length(), index + length + SEARCH_TEXT_PREVIEW_CHARACTER_LIMIT)), null)
                        )));
            }
        }
        return results;
    }

    private ListIterable<CoreInstance> findRawElementsAt(int line, int column)
    {
        registerAllInstances();
        synchronized (this.lock)
        {
            return this.allInstances.select(e -> isElementAtPoint(e, line, column), Lists.mutable.empty());
        }
    }

    @Deprecated
    public ConcreteFunctionDefinition<?> findConcreteFunctionDefinitionAt(int line, int column, ProcessorSupport processorSupport)
    {
        return findConcreteFunctionDefinitionAt(line, column);
    }

    public ConcreteFunctionDefinition<?> findConcreteFunctionDefinitionAt(int line, int column)
    {
        synchronized (this.lock)
        {
            return (ConcreteFunctionDefinition<?>) this.newInstances.detect(e -> isElementAtPoint(e, line, column) && (e instanceof ConcreteFunctionDefinition));
        }
    }

    public SourceCoordinates.Preview getPreviewTextWithCoordinates(int startLine, int startColumn, int endLine, int endColumn)
    {
        if (StringIterate.isEmpty(this.content))
        {
            return null;
        }

        String[] lines = this.content.split("\\R");

        if (startLine < 1 ||
                endLine < 1 ||
                startLine > lines.length ||
                endLine > lines.length ||
                startLine > endLine ||
                (startLine == endLine && startColumn > endColumn) ||
                startColumn < 1 ||
                endColumn < 1 ||
                startColumn > lines[startLine - 1].length() ||
                endColumn > lines[endLine - 1].length()
        )
        {
            throw new IllegalArgumentException("Invalid source coordinates");
        }

        String beforeText = "";
        String foundText = "";
        String afterText = "";

        for (int i = 0; i < lines.length; i++)
        {
            String line = lines[i];
            if (i == startLine - 1)
            {
                beforeText = StringUtils.stripStart(line.substring(Math.max(0, startColumn - 1 - SEARCH_TEXT_PREVIEW_CHARACTER_LIMIT), startColumn - 1), null);
                foundText += line.substring(startColumn - 1);
                if (startLine == endLine)
                {
                    foundText = line.substring(startColumn - 1, endColumn);
                    afterText = StringUtils.stripEnd(line.substring(endColumn, Math.min(line.length(), endColumn + SEARCH_TEXT_PREVIEW_CHARACTER_LIMIT)), null);
                    break;
                }
            }
            else if (i > startLine - 1 && i < endLine - 1)
            {
                foundText += "\n" + line;
            }
            else if (i == endLine - 1)
            {
                foundText += "\n" + line.substring(0, endColumn);
                afterText = StringUtils.stripEnd(line.substring(endColumn, Math.min(line.length(), endColumn + SEARCH_TEXT_PREVIEW_CHARACTER_LIMIT)), null);
                break;
            }
        }

        return new SourceCoordinates.Preview(beforeText, foundText, afterText);
    }

    private boolean isElementAtPoint(CoreInstance element, int line, int column)
    {
        SourceInformation sourceInfo = element.getSourceInformation();
        if (sourceInfo == null)
        {
            return false;
        }

        int startLine = sourceInfo.getStartLine();
        if ((startLine > line) || ((startLine == line) && (sourceInfo.getStartColumn() > column)))
        {
            return false;
        }

        int endLine = sourceInfo.getEndLine();
        return (endLine >= line) && ((endLine != line) || (sourceInfo.getEndColumn() >= column));
    }

    private void registerAllInstances()
    {
        synchronized (this.lock)
        {
            if (this.allInstances == null)
            {
                MutableSet<CoreInstance> result = Sets.mutable.ofInitialCapacity(this.newInstances.size());
                MutableSet<CoreInstance> visited = Sets.mutable.ofInitialCapacity(this.newInstances.size());
                MutableStack<CoreInstance> searchStack = Stacks.mutable.withAll(this.newInstances);
                while (!searchStack.isEmpty())
                {
                    CoreInstance next = searchStack.pop();
                    if (visited.add(next))
                    {
                        boolean searchPropertyValues = false;
                        SourceInformation sourceInfo = next.getSourceInformation();
                        if (sourceInfo == null)
                        {
                            searchPropertyValues = !(next instanceof Package);
                        }
                        else if (this.id.equals(sourceInfo.getSourceId()))
                        {
                            result.add(next);
                            searchPropertyValues = true;
                        }
                        if (searchPropertyValues)
                        {
                            next.getKeys().forEach(key -> next.getValueForMetaPropertyToMany(key).forEach(searchStack::push));
                        }
                    }
                }
                this.allInstances = result.toImmutable();
            }
        }
    }

    public static String formatForImportGroupId(String sourceId)
    {
        return sourceId.replace('.', '_').replace('/', '_');
    }

    public static String importForSourceName(String base)
    {
        return importForSourceName(base, -1);
    }

    public static String importForSourceName(String base, int count)
    {
        return importForSourceName(base, null, count);
    }

    public static String importForSourceName(String base, String parserName, int count)
    {
        StringBuilder builder = new StringBuilder("import");
        if (parserName != null)
        {
            builder.append(parserName);
        }
        builder.append('_').append(formatForImportGroupId(base));
        if (count >= 0)
        {
            builder.append('_').append(count);
        }
        return builder.toString();
    }

    public boolean isModified(String code)
    {
        return !this.content.equals(code);
    }

    public void setSourceRegistry(SourceRegistry sourceRegistry)
    {
        this.sourceRegistry = sourceRegistry;
    }

    public static boolean isInMemory(String path)
    {
        return !path.startsWith("/");
    }

    public static Source createMutableInMemorySource(String id, String content)
    {
        return new Source(id, false, true, content);
    }
}
