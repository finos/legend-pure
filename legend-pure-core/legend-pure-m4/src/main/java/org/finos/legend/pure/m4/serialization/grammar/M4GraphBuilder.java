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

package org.finos.legend.pure.m4.serialization.grammar;

import org.antlr.v4.runtime.Token;
import org.antlr.v4.runtime.tree.TerminalNode;
import org.apache.commons.lang3.StringEscapeUtils;
import org.eclipse.collections.api.factory.Lists;
import org.eclipse.collections.api.list.MutableList;
import org.eclipse.collections.impl.utility.Iterate;
import org.finos.legend.pure.m4.ModelRepository;
import org.finos.legend.pure.m4.coreinstance.CoreInstance;
import org.finos.legend.pure.m4.coreinstance.SourceInformation;
import org.finos.legend.pure.m4.coreinstance.simple.SimpleCoreInstance;
import org.finos.legend.pure.m4.serialization.grammar.antlr.AntlrSourceInformation;
import org.finos.legend.pure.m4.serialization.grammar.antlr.PureParserException;

import java.util.List;
import java.util.function.Function;

public class M4GraphBuilder extends M4AntlrParserBaseVisitor<MutableList<CoreInstance>>
{
    private final String sourceName;
    private final ModelRepository repository;
    private final boolean addLines;

    M4GraphBuilder(String sourceName, ModelRepository repository, boolean addLines)
    {
        this.sourceName = sourceName;
        this.repository = repository;
        this.addLines = addLines;
    }

    @Override
    public MutableList<CoreInstance> visitDefinition(M4AntlrParser.DefinitionContext ctx)
    {
        MutableList<CoreInstance> results = Lists.mutable.empty();
        if (ctx.metaClass() != null)
        {
            Iterate.collect(ctx.metaClass(), mcc -> visitMetaClassBlock(mcc, null, null, false), results);
        }
        return results;
    }

    private CoreInstance visitMetaClassBlock(M4AntlrParser.MetaClassContext ctx, CoreInstance ownerClassifier, NameSpace namespace, boolean deep)
    {
        CoreInstance instanceClassifierOwner = this.visitInstanceBlock(ctx.instance());
        SourceInformation sourceInfo = visitSourceInfoBlock(ctx.sourceInfo());
        NameSpace possibleNamespace = (ctx.nameSpace() == null) ? namespace : visitNameSpaceBlock(ctx.nameSpace());
        CoreInstance classifier = this.repository.instantiate(instanceClassifierOwner, ctx.newTypeStr() == null ? null : ctx.newTypeStr().VALID_STRING().getText(), sourceInfo, ownerClassifier, possibleNamespace, deep);

        visitPropertiesBlock(ctx.properties(), classifier);
        if (this.addLines)
        {
            if (sourceInfo != null)
            {
                throw new RuntimeException("Adding debugging info should only be done when addLines is set to false!");
            }
            Token main = ctx.newTypeStr() == null ? ctx.CARET().getSymbol() : ctx.newTypeStr().VALID_STRING().getSymbol();
            Token end = ctx.CURLY_BRACKET_CLOSE() == null ? main : ctx.CURLY_BRACKET_CLOSE().getSymbol();
            sourceInfo = new AntlrSourceInformation(0, 0, this.sourceName).getPureSourceInformation(ctx.CARET().getSymbol(), main, end);
            classifier.setSourceInformation(sourceInfo);
        }

        return classifier;
    }

    private void visitPropertiesBlock(List<M4AntlrParser.PropertiesContext> properties, CoreInstance classifier)
    {
        if (properties != null)
        {
            for (M4AntlrParser.PropertiesContext pc : properties)
            {
                MutableList<String> path = visitPathBlock(pc.path());
                MutableList<CoreInstance> right = visitRightSideBlock(pc.rightSide(), classifier, new NameSpace(classifier, path.getLast()));
                classifier.setKeyValues(path, right);
            }
        }
    }

    private CoreInstance visitInstanceBlock(M4AntlrParser.InstanceContext ctx)
    {
        String name = ctx.name().VALID_STRING().getText();
        CoreInstance classifierOwner = this.repository.getOrCreateTopLevel(name, null);
        if (ctx.classifierOwner() != null)
        {
            for (M4AntlrParser.ClassifierOwnerContext coc : ctx.classifierOwner())
            {
                classifierOwner = ((SimpleCoreInstance) classifierOwner).getOrCreateUnknownTypeNode(coc.key().VALID_STRING().getText(), coc.keyInArray() != null ? coc.keyInArray().VALID_STRING().getText() : null, this.repository);
            }
        }
        return classifierOwner;
    }

    private MutableList<CoreInstance> visitRightSideBlock(M4AntlrParser.RightSideContext ctx, CoreInstance classifier, NameSpace nameSpace)
    {
        MutableList<CoreInstance> elements = Lists.mutable.empty();
        if (ctx.element() != null)
        {
            Iterate.collect(ctx.element(), ec -> visitElementBlock(ec, classifier, nameSpace), elements);
        }
        return elements;
    }

    private CoreInstance visitElementBlock(M4AntlrParser.ElementContext ctx, CoreInstance classifier, NameSpace nameSpace)
    {
        if (ctx.metaClass() != null)
        {
            return visitMetaClassBlock(ctx.metaClass(), classifier, nameSpace, true);
        }
        if (ctx.literalElement() != null)
        {
            return visitLiteralElementBlock(ctx.literalElement());
        }
        if (ctx.instance() != null)
        {
            return visitInstanceBlock(ctx.instance());
        }
        if (ctx.SELF() != null)
        {
            throw new RuntimeException("EE");
        }

        return null;
    }

    private CoreInstance visitLiteralElementBlock(M4AntlrParser.LiteralElementContext ctx)
    {
        if (ctx.STRING() != null)
        {
            return visitLiteralWithErrorHandling(ctx.STRING(), text ->
            {
                String withQuote = StringEscapeUtils.unescapeJava(text);
                return this.repository.newStringCoreInstance_cached(withQuote.substring(1, withQuote.length() - 1));
            });
        }
        if (ctx.INTEGER() != null)
        {
            return visitLiteralWithErrorHandling(ctx.INTEGER(), this.repository::newIntegerCoreInstance);
        }
        if (ctx.FLOAT() != null)
        {
            return visitLiteralWithErrorHandling(ctx.FLOAT(), this.repository::newFloatCoreInstance);
        }
        if (ctx.BOOLEAN() != null)
        {
            return visitLiteralWithErrorHandling(ctx.BOOLEAN(), this.repository::newBooleanCoreInstance);
        }
        if (ctx.DATE() != null)
        {
            return visitLiteralWithErrorHandling(ctx.DATE(), text -> this.repository.newDateCoreInstance(text.substring(1)));
        }
        if (ctx.STRICTTIME() != null)
        {
            return visitLiteralWithErrorHandling(ctx.STRICTTIME(), text -> this.repository.newStrictTimeCoreInstance(text.substring(1)));
        }
        return null;
    }

    private CoreInstance visitLiteralWithErrorHandling(TerminalNode node, Function<? super String, ? extends CoreInstance> function)
    {
        String text = node.getText();
        try
        {
            return function.apply(text);
        }
        catch (PureParserException e)
        {
            throw e;
        }
        catch (Exception e)
        {
            Token token = node.getSymbol();
            int line = token.getLine();
            int start = token.getCharPositionInLine() + 1;
            SourceInformation sourceInfo = new SourceInformation(this.sourceName, line, start, line, start + text.length());
            String message = e.getMessage();
            throw new PureParserException(sourceInfo, (message == null) ? ("Exception parsing: " + text) : message, e);
        }
    }

    private SourceInformation visitSourceInfoBlock(M4AntlrParser.SourceInfoContext ctx)
    {
        return isSourceInfoPresent(ctx) ? new SourceInformation(ctx.VALID_STRING_EXT().getText().substring(2), Integer.parseInt(ctx.INTEGER().get(0).getText()), Integer.parseInt(ctx.INTEGER().get(1).getText()), Integer.parseInt(ctx.INTEGER().get(2).getText()), Integer.parseInt(ctx.INTEGER().get(3).getText()), Integer.parseInt(ctx.INTEGER().get(4).getText()), Integer.parseInt(ctx.INTEGER().get(5).getText())) : null;
    }

    private boolean isSourceInfoPresent(M4AntlrParser.SourceInfoContext ctx)
    {
        return ctx != null && ctx.VALID_STRING_EXT() != null && ctx.INTEGER() != null && ctx.INTEGER().get(0) != null && ctx.INTEGER().get(1) != null && ctx.INTEGER().get(2) != null && ctx.INTEGER().get(3) != null && ctx.INTEGER().get(4) != null && ctx.INTEGER().get(5) != null;
    }

    private NameSpace visitNameSpaceBlock(M4AntlrParser.NameSpaceContext ctx)
    {
        String key = null;
        CoreInstance classifierOwner = this.repository.getOrCreateTopLevel(ctx.name().VALID_STRING().getText(), null);
        if (ctx.classifierOwner() != null)
        {
            for (M4AntlrParser.ClassifierOwnerContext coc : ctx.classifierOwner())
            {
                key = coc.key().VALID_STRING().getText();
                if (coc.keyInArray() != null)
                {
                    classifierOwner = ((SimpleCoreInstance) classifierOwner).getOrCreateUnknownTypeNode(key, coc.keyInArray().VALID_STRING().getText(), this.repository);
                }
            }
        }
        return new NameSpace(classifierOwner, key);
    }

    private MutableList<String> visitPathBlock(M4AntlrParser.PathContext ctx)
    {
        MutableList<String> pathResult = Lists.mutable.empty();
        pathResult.add(ctx.name().VALID_STRING().getText());
        if (ctx.classifierOwner() != null)
        {
            for (M4AntlrParser.ClassifierOwnerContext coc : ctx.classifierOwner())
            {
                pathResult.add(coc.key().VALID_STRING().getText());
                if (coc.keyInArray() != null)
                {
                    pathResult.add(coc.keyInArray().VALID_STRING().getText());
                }
            }
        }
        return pathResult;
    }
}
