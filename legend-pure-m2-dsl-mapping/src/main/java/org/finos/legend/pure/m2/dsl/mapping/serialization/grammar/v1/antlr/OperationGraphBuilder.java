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

import org.eclipse.collections.api.block.function.Function;
import org.eclipse.collections.api.list.MutableList;
import org.eclipse.collections.impl.list.mutable.FastList;
import org.eclipse.collections.impl.list.mutable.ListAdapter;
import org.finos.legend.pure.m2.dsl.mapping.serialization.grammar.OperationParser;
import org.finos.legend.pure.m2.dsl.mapping.serialization.grammar.OperationParserBaseVisitor;
import org.finos.legend.pure.m4.serialization.grammar.antlr.AntlrSourceInformation;
import org.antlr.v4.runtime.Token;

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
        String idOrPath = ctx.functionPath().qualifiedName().packagePath() == null ? ctx.functionPath().qualifiedName().identifier().getText() : ListAdapter.adapt(ctx.functionPath().qualifiedName().packagePath().identifier()).collect(IDENTIFIER_CONTEXT_STRING_FUNCTION).makeString("::") + "::" + ctx.functionPath().qualifiedName().identifier().getText();
        MutableList<String> parameters = FastList.newList();
        if (ctx.parameters() != null && ctx.parameters().VALID_STRING() != null)
        {
            for (int i = 0; i < ctx.parameters().VALID_STRING().size(); i++)
            {
                parameters.add(ctx.parameters().VALID_STRING().get(i).getText());
            }
        }
        return "^meta::pure::mapping::OperationSetImplementation" + setSourceInfo +
                "(" +
                ((id == null) ? "" : ("    id = '" + id + "',")) +
                "    root = " + root + "," +
                "    operation = ^meta::pure::metamodel::import::ImportStub " + sourceInformation.getPureSourceInformation(startToken, startToken, ctx.functionPath().qualifiedName().identifier().getStop()).toM4String() + " (importGroup=system::imports::" + importId + ", idOrPath='" + idOrPath + "')," +
                "    parameters = [" + toSetImplementationContainers(parameters) + "]," +
                "    class = ^meta::pure::metamodel::import::ImportStub" + classSourceInfo + " (importGroup=system::imports::" + importId + ", idOrPath='" + classPath + "')," +
                "    parent = ^meta::pure::metamodel::import::ImportStub (importGroup=system::imports::" + importId + ", idOrPath='" + mappingPath + "')" +
                ")";
    }

    private static final Function<OperationParser.IdentifierContext, String> IDENTIFIER_CONTEXT_STRING_FUNCTION = new Function<OperationParser.IdentifierContext, String>()
    {
        @Override
        public String valueOf(OperationParser.IdentifierContext identifierContext)
        {
            return identifierContext.getText();
        }
    };

    private String toSetImplementationContainers(MutableList<String> parameters)
    {
        return parameters.collect(new Function<String, String>()
        {
            @Override
            public String valueOf(String parameter)
            {
                return "^meta::pure::mapping::SetImplementationContainer(id='" + parameter + "')";
            }
        }).makeString(",");
    }
}
