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

package org.finos.legend.pure.m2.inlinedsl.graph.antlr;

import org.eclipse.collections.api.factory.Lists;
import org.eclipse.collections.api.list.ListIterable;
import org.eclipse.collections.api.list.MutableList;
import org.eclipse.collections.impl.utility.ListIterate;
import org.finos.legend.pure.m2.inlinedsl.graph.serialization.grammar.GraphParser.DefinitionContext;
import org.finos.legend.pure.m2.inlinedsl.graph.serialization.grammar.GraphParser.GraphDefinitionContext;
import org.finos.legend.pure.m2.inlinedsl.graph.serialization.grammar.GraphParser.GraphPathContext;
import org.finos.legend.pure.m2.inlinedsl.graph.serialization.grammar.GraphParser.ParameterContext;
import org.finos.legend.pure.m2.inlinedsl.graph.serialization.grammar.GraphParser.ScalarParameterContext;
import org.finos.legend.pure.m2.inlinedsl.graph.serialization.grammar.GraphParserBaseVisitor;
import org.finos.legend.pure.m3.coreinstance.meta.pure.metamodel._import.ImportGroup;
import org.finos.legend.pure.m4.coreinstance.SourceInformation;
import org.finos.legend.pure.m4.serialization.grammar.antlr.AntlrSourceInformation;


public class GraphAntlrTreeWalker extends GraphParserBaseVisitor<String>
{
    private final AntlrSourceInformation sourceInformation;
    private final ImportGroup importId;

    GraphAntlrTreeWalker(AntlrSourceInformation sourceInformation, ImportGroup importId)
    {
        this.sourceInformation = sourceInformation;
        this.importId = importId;
    }

    @Override
    public String visitDefinition(DefinitionContext definitionContext)
    {
        return this.visitRootGraphDefinition(definitionContext.graphDefinition(), definitionContext);
    }

    private String visitRootGraphDefinition(GraphDefinitionContext graphDefinitionContext, DefinitionContext definitionContext)
    {
        SourceInformation definitionSourceInfo = this.sourceInformation.getPureSourceInformation(definitionContext.start, graphDefinitionContext.start, graphDefinitionContext.stop);
        SourceInformation classSourceInfo = this.sourceInformation.getPureSourceInformation(definitionContext.qualifiedName().start, definitionContext.qualifiedName().identifier().start, definitionContext.qualifiedName().stop);

        MutableList<String> subTrees = ListIterate.collect(graphDefinitionContext.graphPaths().graphPath(), this::visitGraphPathContext);

        return "^meta::pure::graphFetch::RootGraphFetchTree<" + definitionContext.qualifiedName().getText() + "> " + definitionSourceInfo.toM4String() + " (" +
                "class = " + "^meta::pure::metamodel::import::ImportStub " + classSourceInfo.toM4String() + " (importGroup = system::imports::" + this.importId._name() + ", idOrPath = '" + definitionContext.qualifiedName().getText() + "'), " +
                "subTrees = " + subTrees.makeString("[", ", ", "]") +
                ")";
    }

    private String visitGraphPathContext(GraphPathContext graphPathContext)
    {
        SourceInformation definitionSourceInfo = this.sourceInformation.getPureSourceInformation(graphPathContext.start, graphPathContext.start, graphPathContext.stop);
        SourceInformation propertySourceInfo = this.sourceInformation.getPureSourceInformation(graphPathContext.identifier().start, graphPathContext.identifier().start, graphPathContext.identifier().stop);

        ListIterable<String> subTrees = (graphPathContext.graphDefinition() == null) ? Lists.immutable.empty() : ListIterate.collect(graphPathContext.graphDefinition().graphPaths().graphPath(), this::visitGraphPathContext);

        ListIterable<String> parameters = (graphPathContext.propertyParameters() == null) ? Lists.immutable.empty() : ListIterate.collect(graphPathContext.propertyParameters().parameter(), this::visitParameterContext);

        String subType;
        if (graphPathContext.subtype() == null)
        {
            subType = "";
        }
        else
        {
            SourceInformation subTypeSourceInfo = this.sourceInformation.getPureSourceInformation(graphPathContext.subtype().qualifiedName().start, graphPathContext.subtype().qualifiedName().start, graphPathContext.subtype().qualifiedName().stop);
            subType = "^meta::pure::metamodel::import::ImportStub " + subTypeSourceInfo.toM4String() + " (importGroup = system::imports::" + this.importId._name() + ", idOrPath = '" + graphPathContext.subtype().qualifiedName().getText() + "')";
        }

        String alias = (graphPathContext.alias() == null) ? "" : graphPathContext.alias().STRING().getText();

        return "^meta::pure::graphFetch::PropertyGraphFetchTree " + definitionSourceInfo.toM4String() + " (" +
                "property = " + "^meta::pure::metamodel::import::PropertyStub " + propertySourceInfo.toM4String() + " (propertyName = '" + graphPathContext.identifier().getText() + "'), " +
                "parameters = " + parameters.makeString("[", ", ", "]") + ", " +
                "subTrees = " + subTrees.makeString("[", ", ", "]") + ", " +
                "subType = [" + subType + "]" + ", " +
                "alias = [" + alias + "]" +
                ")";
    }

    private String visitParameterContext(ParameterContext parameterContext)
    {
        SourceInformation sourceInfo = this.sourceInformation.getPureSourceInformation(parameterContext.getStart(), parameterContext.getStart(), parameterContext.getStop(), true);
        ListIterable<String> values = (parameterContext.scalarParameter() != null) ?
                                      Lists.immutable.with(visitScalarParameterContext(parameterContext.scalarParameter())) :
                                      ListIterate.collect(parameterContext.collectionParameter().scalarParameter(), this::visitScalarParameterContext);
        return "^meta::pure::metamodel::valuespecification::InstanceValue" + sourceInfo.toM4String() + "(values = " + values.makeString("[", ", ", "]") + ")";
    }

    private String visitScalarParameterContext(ScalarParameterContext scalarParameterContext)
    {
        if (scalarParameterContext.enumReference() != null)
        {
            SourceInformation enumSourceInfo = this.sourceInformation.getPureSourceInformation(scalarParameterContext.enumReference().qualifiedName().start, scalarParameterContext.enumReference().identifier().start, scalarParameterContext.enumReference().identifier().stop);
            SourceInformation enumerationSourceInfo = this.sourceInformation.getPureSourceInformation(scalarParameterContext.enumReference().qualifiedName().start, scalarParameterContext.enumReference().qualifiedName().start, scalarParameterContext.enumReference().qualifiedName().stop);

            return "^meta::pure::metamodel::import::EnumStub " + enumSourceInfo.toM4String() + " (" +
                    "enumName = '" + scalarParameterContext.enumReference().identifier().getText() + "', " +
                    "enumeration = ^meta::pure::metamodel::import::ImportStub " + enumerationSourceInfo.toM4String() + " (importGroup = system::imports::" + this.importId._name() + ", idOrPath = '" + scalarParameterContext.enumReference().qualifiedName().getText() + "')" +
                    ")";
        }
        if (scalarParameterContext.variable() != null)
        {
            SourceInformation variableSourceInfo = this.sourceInformation.getPureSourceInformation(scalarParameterContext.variable().identifier().getStart());
            return "^meta::pure::metamodel::valuespecification::VariableExpression " + variableSourceInfo.toM4String() + " (" +
                    "name = '" + scalarParameterContext.variable().identifier().getText() + "'" +
                    ")";
        }
        return scalarParameterContext.getText();
    }
}
