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

package org.finos.legend.pure.m2.dsl.mapping.serialization.grammar.v1.antlr;

import org.antlr.v4.runtime.RuleContext;
import org.antlr.v4.runtime.Token;
import org.antlr.v4.runtime.tree.TerminalNode;
import org.eclipse.collections.api.factory.Lists;
import org.eclipse.collections.api.list.MutableList;
import org.eclipse.collections.impl.utility.LazyIterate;
import org.eclipse.collections.impl.utility.ListIterate;
import org.finos.legend.pure.m2.dsl.mapping.serialization.grammar.OperationParser;
import org.finos.legend.pure.m2.dsl.mapping.serialization.grammar.OperationParserBaseVisitor;
import org.finos.legend.pure.m4.serialization.grammar.antlr.AntlrSourceInformation;

public class OperationGraphBuilder extends OperationParserBaseVisitor<String>
{
    private final String id;
    private final String setSourceInfo;
    private final boolean root;
    private final String classPath;
    private final String classSourceInfo;
    private final String mappingPath;
    private final String importId;
    private final AntlrSourceInformation sourceInformation;

    public OperationGraphBuilder(String id, String setSourceInfo, boolean root, String classPath, String classSourceInfo, String mappingPath, String importId, AntlrSourceInformation sourceInformation)
    {
        this.id = id;
        this.setSourceInfo = setSourceInfo;
        this.root = root;
        this.classPath = classPath;
        this.classSourceInfo = classSourceInfo;
        this.mappingPath = mappingPath;
        this.importId = importId;
        this.sourceInformation = sourceInformation;
    }

    @Override
    public String visitMapping(OperationParser.MappingContext ctx)
    {
        visitChildren(ctx);
        Token startToken = ctx.functionPath().qualifiedName().packagePath() == null ? ctx.functionPath().qualifiedName().identifier().getStart() : ctx.functionPath().qualifiedName().packagePath().getStart();
        String idOrPath = ctx.functionPath().qualifiedName().packagePath() == null ? ctx.functionPath().qualifiedName().identifier().getText() : LazyIterate.collect(ctx.functionPath().qualifiedName().packagePath().identifier(), RuleContext::getText).makeString("::") + "::" + ctx.functionPath().qualifiedName().identifier().getText();
        MutableList<String> parameters = Lists.mutable.empty();
        if (ctx.parameters() != null && ctx.parameters().VALID_STRING() != null)
        {
            ListIterate.collect(ctx.parameters().VALID_STRING(), TerminalNode::getText, parameters);
        }
        return "^meta::pure::mapping::OperationSetImplementation" + setSourceInfo +
                "(" +
                ((this.id == null) ? "" : ("    id = '" + this.id + "',")) +
                "    root = " + this.root + "," +
                "    operation = ^meta::pure::metamodel::import::ImportStub " + this.sourceInformation.getPureSourceInformation(startToken, startToken, ctx.functionPath().qualifiedName().identifier().getStop()).toM4String() + " (importGroup=system::imports::" + this.importId + ", idOrPath='" + idOrPath + "')," +
                "    parameters = [" + toSetImplementationContainers(parameters) + "]," +
                "    class = ^meta::pure::metamodel::import::ImportStub" + this.classSourceInfo + " (importGroup=system::imports::" + this.importId + ", idOrPath='" + this.classPath + "')," +
                "    parent = ^meta::pure::metamodel::import::ImportStub (importGroup=system::imports::" + this.importId + ", idOrPath='" + this.mappingPath + "')" +
                ")";
    }

    private String toSetImplementationContainers(MutableList<String> parameters)
    {
        return parameters.isEmpty() ? "" : parameters.makeString("^meta::pure::mapping::SetImplementationContainer(id='", "'),^meta::pure::mapping::SetImplementationContainer(id='", "')");
    }
}
