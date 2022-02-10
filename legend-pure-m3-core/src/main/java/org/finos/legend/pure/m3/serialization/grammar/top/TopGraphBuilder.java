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

package org.finos.legend.pure.m3.serialization.grammar.top;

import org.antlr.v4.runtime.tree.TerminalNode;
import org.eclipse.collections.api.factory.Lists;
import org.eclipse.collections.api.list.MutableList;
import org.eclipse.collections.api.multimap.list.MutableListMultimap;
import org.eclipse.collections.impl.factory.Multimaps;
import org.eclipse.collections.impl.utility.LazyIterate;
import org.finos.legend.pure.m3.compiler.Context;
import org.finos.legend.pure.m3.serialization.grammar.Parser;
import org.finos.legend.pure.m3.serialization.grammar.ParserLibrary;
import org.finos.legend.pure.m3.serialization.grammar.top.antlr.TopAntlrParser;
import org.finos.legend.pure.m3.serialization.grammar.top.antlr.TopAntlrParserBaseVisitor;
import org.finos.legend.pure.m3.serialization.runtime.SourceState;
import org.finos.legend.pure.m3.statelistener.M3M4StateListener;
import org.finos.legend.pure.m4.ModelRepository;
import org.finos.legend.pure.m4.coreinstance.CoreInstance;
import org.finos.legend.pure.m4.exception.PureException;
import org.finos.legend.pure.m4.serialization.grammar.antlr.PureParserException;

public class TopGraphBuilder extends TopAntlrParserBaseVisitor<MutableListMultimap<Parser, CoreInstance>>
{

    private MutableListMultimap<Parser, CoreInstance> newInstancesByParser;
    private final ParserLibrary parserLibrary;
    private final ModelRepository repository;
    private final M3M4StateListener listener;
    private final Context context;
    private final SourceState oldState;
    private final String sourceName;
    private int count;

    public TopGraphBuilder(String sourceName, ModelRepository repository, M3M4StateListener listener, Context context, ParserLibrary parserLibrary, SourceState oldState)
    {
        this.sourceName = sourceName;
        this.parserLibrary = parserLibrary;
        this.repository = repository;
        this.listener = listener;
        this.context = context;
        this.oldState = oldState;
    }

    @Override
    public MutableListMultimap<Parser, CoreInstance> visitDefinition(TopAntlrParser.DefinitionContext ctx)
    {
        this.count = 0;
        this.newInstancesByParser = Multimaps.mutable.list.empty();
        visitChildren(ctx);
        return this.newInstancesByParser;
    }

    @Override
    public MutableListMultimap<Parser, CoreInstance> visitTop(TopAntlrParser.TopContext ctx)
    {
        String parserName = ctx.CODE_BLOCK_START().getText().substring(4);
        if (ctx.CODE() != null)
        {
            String code = LazyIterate.collect(ctx.CODE(), TerminalNode::getText).makeString("");
            try
            {
                Parser parser = this.parserLibrary.getParser(parserName);
                MutableList<CoreInstance> subResults = Lists.mutable.empty();
                this.count++;
                parser.parse(code, this.sourceName, true, ctx.CODE_BLOCK_START().getSymbol().getLine() - 2, this.repository, subResults, this.listener, this.context, this.count, this.oldState);
                this.newInstancesByParser.putAll(parser, subResults);
            }
            catch (Error e)
            {
                throw new PureParserException(this.sourceName, ctx.CODE_BLOCK_START().getSymbol().getLine() - 2, 0, e.getMessage(), code, e);
            }
            catch (RuntimeException e)
            {
                PureException pe = PureException.findPureException(e);
                if (pe != null && pe.getSourceInformation() != null)
                {
                    throw e;
                }
                throw new PureParserException(this.sourceName, ctx.CODE_BLOCK_START().getSymbol().getLine() - 2, 0, e.getMessage(), code, e);
            }
        }
        return null;
    }
}
