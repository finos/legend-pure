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
import org.finos.legend.pure.m3.compiler.Context;
import org.finos.legend.pure.m3.navigation.M3ProcessorSupport;
import org.finos.legend.pure.m3.serialization.grammar.m3parser.antlr.AntlrContextToM3CoreInstance;
import org.finos.legend.pure.m3.serialization.grammar.m3parser.antlr.M3AntlrParser;
import org.finos.legend.pure.m3.serialization.grammar.m3parser.antlr.TemporaryPureMergeOperationFunctionSpecification;
import org.finos.legend.pure.m4.ModelRepository;
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
    private final ModelRepository repository;
    private final Context context;

    public OperationGraphBuilder(String id, String setSourceInfo, boolean root, String classPath, String classSourceInfo, String mappingPath, String importId, AntlrSourceInformation sourceInformation, ModelRepository repository, Context context)
    {
        this.id = id;
        this.setSourceInfo = setSourceInfo;
        this.root = root;
        this.classPath = classPath;
        this.classSourceInfo = classSourceInfo;
        this.mappingPath = mappingPath;
        this.importId = importId;
        this.sourceInformation = sourceInformation;
        this.repository = repository;
        this.context = context;
    }

    @Override
    public String visitMapping(OperationParser.MappingContext ctx) {
        visitChildren(ctx);
        Token startToken = ctx.functionPath().qualifiedName().packagePath() == null ? ctx.functionPath().qualifiedName().identifier().getStart() : ctx.functionPath().qualifiedName().packagePath().getStart();
        String idOrPath = ctx.functionPath().qualifiedName().packagePath() == null ? ctx.functionPath().qualifiedName().identifier().getText() : LazyIterate.collect(ctx.functionPath().qualifiedName().packagePath().identifier(), RuleContext::getText).makeString("::") + "::" + ctx.functionPath().qualifiedName().identifier().getText();
        MutableList<String> parameters = Lists.mutable.empty();
        if (ctx.mergeParameters() != null) {
            OperationParser.MergeParametersContext mergeContext = ctx.mergeParameters();
            String lambdaText = "";
            if (mergeContext.setParameter() != null && mergeContext.setParameter().VALID_STRING() != null) ;
            {
                for (int i = 0; i < mergeContext.setParameter().VALID_STRING().size(); i++) {
                    parameters.add(mergeContext.setParameter().VALID_STRING().get(i).getText());
                }
            }

            if (mergeContext.validationLambdaInstance() != null) {
                lambdaText = "{" + mergeContext.validationLambdaInstance().lambdaElement().get(0).getText() + "}";

            }
            final M3ProcessorSupport processorSupport = new M3ProcessorSupport(context, repository);

            final M3AntlrParser parser = new M3AntlrParser();
            final AntlrContextToM3CoreInstance.LambdaContext lambdaContext = new AntlrContextToM3CoreInstance.LambdaContext(setSourceInfo + '_' + (id == null ? "" : '_' + id) + "_MergeOP");
            TemporaryPureMergeOperationFunctionSpecification temporarySpecification = parser.parseMergeSpecification(lambdaText, lambdaContext, sourceInformation.getSourceName(), sourceInformation.getOffsetLine(), importId, repository, processorSupport, context);
            String processedMergeFunction = parser.process(temporarySpecification.validationExpression, context, processorSupport);
            ;
            return "^meta::pure::mapping::MergeOperationSetImplementation" + setSourceInfo +
                    "(" +
                    ((id == null) ? "" : ("    id = '" + id + "',")) +
                    "    root = " + root + "," +
                    "    operation = ^meta::pure::metamodel::import::ImportStub " + sourceInformation.getPureSourceInformation(startToken, startToken, ctx.functionPath().qualifiedName().identifier().getStop()).toM4String() + " (importGroup=system::imports::" + importId + ", idOrPath='" + idOrPath + "')," +
                    "    parameters = [" + toSetImplementationContainers(parameters) + "]," +
                    "    class = ^meta::pure::metamodel::import::ImportStub" + classSourceInfo + " (importGroup=system::imports::" + importId + ", idOrPath='" + classPath + "')," +
                    "    parent = ^meta::pure::metamodel::import::ImportStub (importGroup=system::imports::" + importId + ", idOrPath='" + mappingPath + "'),\n" +
                    "    validationFunction=^meta::pure::metamodel::function::LambdaFunction " + temporarySpecification.validationExpression.getSourceInformation().toM4String() + " (" +
                    "                                                           classifierGenericType=^meta::pure::metamodel::type::generics::GenericType(rawType=meta::pure::metamodel::function::LambdaFunction, typeArguments=^meta::pure::metamodel::type::generics::GenericType(rawType = ^meta::pure::metamodel::type::FunctionType()))," +
                    "                                                           expressionSequence=" + processedMergeFunction + ")" +
                    ")";
        } else {
            if (ctx.parameters() != null && ctx.parameters().VALID_STRING() != null) {
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
    }
    private String toSetImplementationContainers(MutableList<String> parameters)
    {
        return parameters.isEmpty() ? "" : parameters.makeString("^meta::pure::mapping::SetImplementationContainer(id='", "'),^meta::pure::mapping::SetImplementationContainer(id='", "')");
    }
}
