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

package org.finos.legend.pure.m2.dsl.diagram.serialization.grammar.antlr;

import org.antlr.v4.runtime.ParserRuleContext;
import org.antlr.v4.runtime.RuleContext;
import org.eclipse.collections.api.RichIterable;
import org.eclipse.collections.api.block.predicate.Predicate;
import org.eclipse.collections.api.factory.Lists;
import org.eclipse.collections.api.list.ListIterable;
import org.eclipse.collections.api.list.MutableList;
import org.eclipse.collections.impl.list.mutable.ListAdapter;
import org.eclipse.collections.impl.utility.LazyIterate;
import org.finos.legend.pure.m2.dsl.diagram.M2DiagramPaths;
import org.finos.legend.pure.m2.dsl.diagram.serialization.grammar.DiagramAntlrParser;
import org.finos.legend.pure.m2.dsl.diagram.serialization.grammar.DiagramAntlrParserBaseVisitor;
import org.finos.legend.pure.m3.coreinstance.Package;
import org.finos.legend.pure.m3.coreinstance.meta.pure.metamodel._import.Import;
import org.finos.legend.pure.m3.coreinstance.meta.pure.metamodel._import.ImportGroup;
import org.finos.legend.pure.m3.coreinstance.meta.pure.metamodel._import.ImportGroupInstance;
import org.finos.legend.pure.m3.coreinstance.meta.pure.metamodel._import.ImportInstance;
import org.finos.legend.pure.m3.navigation.M3Paths;
import org.finos.legend.pure.m3.navigation.M3ProcessorSupport;
import org.finos.legend.pure.m3.navigation.ProcessorSupport;
import org.finos.legend.pure.m3.serialization.runtime.Source;
import org.finos.legend.pure.m4.ModelRepository;
import org.finos.legend.pure.m4.coreinstance.SourceInformation;
import org.finos.legend.pure.m4.exception.PureCompilationException;
import org.finos.legend.pure.m4.serialization.grammar.antlr.AntlrSourceInformation;
import org.finos.legend.pure.m4.serialization.grammar.antlr.PureParserException;

import java.util.List;

public class DiagramStoreGraphBuilder extends DiagramAntlrParserBaseVisitor<String>
{
    private final ModelRepository repository;
    private final int count;
    private final ProcessorSupport processorSupport;
    private final AntlrSourceInformation sourceInformation;

    public DiagramStoreGraphBuilder(ModelRepository repository, int count, AntlrSourceInformation sourceInformation)
    {
        this.repository = repository;
        this.count = count;
        this.sourceInformation = sourceInformation;
        this.processorSupport = new M3ProcessorSupport(repository);
    }

    @Override
    public String visitDefinition(DiagramAntlrParser.DefinitionContext ctx)
    {
        final ImportGroup importGroup = imports(sourceInformation.getSourceName(), sourceInformation.getOffsetLine(), ctx.imports());
        String importId = importGroup.getName();

        MutableList<DiagramAntlrParser.DiagramContext> diagramContexts = ListAdapter.adapt(ctx.diagram());
        StringBuilder builder = new StringBuilder();
        for (int i = 0; i < diagramContexts.size(); i++)
        {
            DiagramAntlrParser.DiagramContext diagramContext = diagramContexts.get(i);
            String diagramResult = processDiagram(diagramContext, importId);
            builder.append(diagramResult);
        }

        return builder.toString();
    }

    private String processDiagram(DiagramAntlrParser.DiagramContext ctx, String importId)
    {
        String geometry = "^" + M2DiagramPaths.RectangleGeometry + "(width=0.0,height=0.0)";
        DiagramAntlrParser.QualifiedDiagramNameContext qualifiedDiagramNameContext = ctx.qualifiedDiagramName();
        String diagramName = qualifiedDiagramNameContext.diagramIdentifier().getText();

        final String diagramPath = qualifiedDiagramNameContext.diagramPackagePath() == null ? "" : ListAdapter.adapt(qualifiedDiagramNameContext.diagramPackagePath().diagramIdentifier()).collect(RuleContext::getText).makeString("::");

        MutableList<DiagramAntlrParser.TypeViewContext> typeViewContexts = ListAdapter.adapt(ctx.typeView());
        MutableList<DiagramAntlrParser.AssociationViewContext> associationViewContexts = ListAdapter.adapt(ctx.associationView());
        MutableList<DiagramAntlrParser.PropertyViewContext> propertyViewContexts = ListAdapter.adapt(ctx.propertyView());
        MutableList<DiagramAntlrParser.GeneralizationViewContext> generalizationViewContexts = ListAdapter.adapt(ctx.generalizationView());

        MutableList<String> typeViews = Lists.mutable.empty();
        MutableList<String> associationViews = Lists.mutable.empty();
        MutableList<String> propertyViews = Lists.mutable.empty();
        MutableList<String> generalizationViews = Lists.mutable.empty();

        boolean hasPackage = !"".equals(diagramPath);

        StringBuilder builder = new StringBuilder("^").append(M2DiagramPaths.Diagram).append(" ");
        builder.append(diagramName);
        builder.append(' ');
        builder.append(sourceInformation.getPureSourceInformation(ctx.start, ctx.qualifiedDiagramName().diagramIdentifier().getStart(), ctx.CURLY_BRACKET_CLOSE().getSymbol()).toM4String());
        builder.append(' ');

        if (hasPackage)
        {
            builder.append('@');
            builder.append(diagramPath);
        }
        builder.append("(name='").append(diagramName).append("', package=").append(hasPackage ? diagramPath : "::");

        for (DiagramAntlrParser.TypeViewContext twctx : typeViewContexts)
        {
            typeViews.add(processTypeView(twctx, importId, ctx.qualifiedDiagramName().getText()));
        }

        for (DiagramAntlrParser.AssociationViewContext awctx : associationViewContexts)
        {
            associationViews.add(processAssociationView(awctx, importId, ctx.qualifiedDiagramName().getText()));
        }

        for (DiagramAntlrParser.PropertyViewContext pwctx : propertyViewContexts)
        {
            propertyViews.add(processPropertyView(pwctx, importId, ctx.qualifiedDiagramName().getText()));
        }

        for (DiagramAntlrParser.GeneralizationViewContext gwctx : generalizationViewContexts)
        {
            generalizationViews.add(processGeneralizationView(gwctx, importId, ctx.qualifiedDiagramName().getText()));
        }
        appendPureCollection(builder, typeViews, ", typeViews=");
        appendPureCollection(builder, associationViews, ", associationViews=");
        appendPureCollection(builder, propertyViews, ", propertyViews=");
        appendPureCollection(builder, generalizationViews, ", generalizationViews=");

        builder.append(", rectangleGeometry=");
        if (ctx.geometry() != null)
        {
            builder.append("^" + M2DiagramPaths.RectangleGeometry + " ");
            builder.append(sourceInformation.getPureSourceInformation(ctx.geometry().getStart(), ctx.geometry().getStart(), ctx.geometry().getStop()).toM4String());
            if (ctx.geometry().widthFirst() != null)
            {
                builder.append(" (width=");
                builder.append(ctx.geometry().widthFirst().FLOAT(0).getText());
                builder.append(", height=");
                builder.append(ctx.geometry().widthFirst().FLOAT(1).getText());
            }
            else
            {
                builder.append(" (width=");
                builder.append(ctx.geometry().heightFirst().FLOAT(1).getText());
                builder.append(", height=");
                builder.append(ctx.geometry().heightFirst().FLOAT(0).getText());
            }
            builder.append(')');
        }
        else
        {
            builder.append(geometry);
        }
        builder.append(')');
        return builder.toString();
    }

    private String processGeneralizationView(DiagramAntlrParser.GeneralizationViewContext generalizationViewContext, String importId, String diagramFullQualifiedName)
    {
        String generalizationViewName = generalizationViewContext.diagramIdentifier().getText();

        StringBuilder builder = new StringBuilder("^").append(M3Paths.GeneralizationView).append(" ");
        builder.append(sourceInformation.getPureSourceInformation(generalizationViewContext.getStart(), generalizationViewContext.diagramIdentifier().getStart(), generalizationViewContext.getStop()).toM4String());

        // Id
        builder.append("(id='").append(generalizationViewName).append("'");

        // Diagram
        builder.append(", diagram=").append(diagramFullQualifiedName);

        MutableList<DiagramAntlrParser.GeneralizationViewPropertyContext> generalizationViewPropertyContexts = ListAdapter.adapt(generalizationViewContext.generalizationViewProperty());
        // Geometry
        DiagramAntlrParser.GeneralizationViewPropertyContext lineStylePropertyContext = propertyContext(generalizationViewPropertyContexts, ctx -> ctx.LINE_STYLE() != null, "lineStyle", generalizationViewContext.diagramIdentifier());
        builder.append(", geometry=^").append(M2DiagramPaths.LineGeometry).append("(style=").append(M2DiagramPaths.LineStyle).append(".");
        builder.append(lineStylePropertyContext.diagramIdentifier().getText());
        DiagramAntlrParser.GeneralizationViewPropertyContext pointsPropertyContext = propertyContext(generalizationViewPropertyContexts, ctx -> ctx.POINTS() != null, "points", generalizationViewContext.diagramIdentifier());
        builder.append(", points=[");
        for (int i = 0; i < pointsPropertyContext.FLOAT().size(); i = i + 2)
        {
            if (i == 0)
            {
                builder.append("^").append(M2DiagramPaths.Point).append("(x=");
            }
            else
            {
                builder.append(", ^").append(M2DiagramPaths.Point).append("(x=");
            }
            builder.append(pointsPropertyContext.FLOAT(i).getText());
            builder.append(", y=");
            builder.append(pointsPropertyContext.FLOAT(i + 1).getText());
            builder.append(")");
        }
        builder.append("])");

        // Rendering
        DiagramAntlrParser.GeneralizationViewPropertyContext colorPropertyContext = propertyContext(generalizationViewPropertyContexts, ctx -> ctx.COLOR() != null, "color", generalizationViewContext.diagramIdentifier());
        builder.append(", rendering=^").append(M2DiagramPaths.Rendering).append("(color='");
        builder.append(colorPropertyContext.COLOR_STRING().getText());
        DiagramAntlrParser.GeneralizationViewPropertyContext lineWidthPropertyContext = propertyContext(generalizationViewPropertyContexts, ctx -> ctx.LINE_WIDTH() != null, "lineWidth", generalizationViewContext.diagramIdentifier());
        builder.append("', lineWidth=");
        builder.append(lineWidthPropertyContext.FLOAT(0).getText());
        builder.append(')');

        // Label
        DiagramAntlrParser.GeneralizationViewPropertyContext labelPropertyContext = propertyContext(generalizationViewPropertyContexts, ctx -> ctx.LABEL() != null, "label", generalizationViewContext.diagramIdentifier());
        builder.append(", label=");
        builder.append(labelPropertyContext.STRING().getText());

        // Source
        // NOTE: we put the id string here and convert it to a TypeView in post-processing
        DiagramAntlrParser.GeneralizationViewPropertyContext sourcePropertyContext = propertyContext(generalizationViewPropertyContexts, ctx -> ctx.SOURCE() != null, "source", generalizationViewContext.diagramIdentifier());
        builder.append(", source='");
        builder.append(sourcePropertyContext.diagramIdentifier().getText());
        builder.append("'");

        // Target
        // NOTE: we put the id string here and convert it to a TypeView in post-processing
        DiagramAntlrParser.GeneralizationViewPropertyContext targetPropertyContext = propertyContext(generalizationViewPropertyContexts, ctx -> ctx.TARGET() != null, "target", generalizationViewContext.diagramIdentifier());
        builder.append(", target='");
        builder.append(targetPropertyContext.diagramIdentifier().getText());
        builder.append("')");

        return builder.toString();
    }

    private String processPropertyView(DiagramAntlrParser.PropertyViewContext propertyViewContext, String importId, String diagramFullQualifiedName)
    {
        String propertyViewName = propertyViewContext.diagramIdentifier().getText();

        StringBuilder builder = new StringBuilder("^" + M2DiagramPaths.PropertyView + " ");
        builder.append(sourceInformation.getPureSourceInformation(propertyViewContext.getStart(), propertyViewContext.diagramIdentifier().getStart(), propertyViewContext.getStop()).toM4String());

        // Id
        builder.append("(id='");
        builder.append(propertyViewName);
        builder.append("'");

        // Diagram
        builder.append(", diagram=");
        builder.append(diagramFullQualifiedName);

        MutableList<DiagramAntlrParser.PropertyViewPropertyContext> propertyViewPropertyContexts = ListAdapter.adapt(propertyViewContext.propertyViewProperty());
        // Property
        DiagramAntlrParser.PropertyViewPropertyContext propertyViewPropertyContext = propertyContext(propertyViewPropertyContexts, ctx -> ctx.PROPERTY() != null, "property", propertyViewContext.diagramIdentifier());
        builder.append(", property=");
        builder.append("^" + M3Paths.PropertyStub + " ");
        builder.append(sourceInformation.getPureSourceInformation(propertyViewPropertyContext.qualifiedDiagramName().diagramPackagePath() != null ? propertyViewPropertyContext.qualifiedDiagramName().diagramPackagePath().diagramIdentifier(0).getStart() : propertyViewPropertyContext.qualifiedDiagramName().diagramIdentifier().getStart(), propertyViewPropertyContext.diagramIdentifier().getStart(), propertyViewPropertyContext.diagramIdentifier().getStop()).toM4String());
        builder.append(" (owner=");
        builder.append("^" + M3Paths.ImportStub + " ");
        builder.append(sourceInformation.getPureSourceInformation(propertyViewPropertyContext.qualifiedDiagramName().getStart(), propertyViewPropertyContext.qualifiedDiagramName().diagramIdentifier().getStart(), propertyViewPropertyContext.qualifiedDiagramName().diagramIdentifier().getStop()).toM4String());
        builder.append(" (importGroup=system::imports::");
        builder.append(importId);
        builder.append(", idOrPath='");
        builder.append(propertyViewPropertyContext.qualifiedDiagramName().getText());
        builder.append("'), propertyName='");
        builder.append(propertyViewPropertyContext.diagramIdentifier().getText());
        builder.append("')");

        // Association visibility
        DiagramAntlrParser.PropertyViewPropertyContext stereotypesVisiblePropertyContext = propertyContext(propertyViewPropertyContexts, ctx -> ctx.STEREOTYPES_VISIBLE() != null, "stereotypesVisible", propertyViewContext.diagramIdentifier());
        builder.append(", visibility=^").append(M2DiagramPaths.AssociationVisibility).append("(visibleStereotype=");
        builder.append(stereotypesVisiblePropertyContext.BOOLEAN().getText());
        DiagramAntlrParser.PropertyViewPropertyContext nameVisiblePropertyContext = propertyContext(propertyViewPropertyContexts, ctx -> ctx.NAME_VISIBLE() != null, "nameVisible", propertyViewContext.diagramIdentifier());
        builder.append(", visibleName=");
        builder.append(nameVisiblePropertyContext.BOOLEAN().getText());
        builder.append(')');

        // Geometry
        DiagramAntlrParser.PropertyViewPropertyContext lineStylePropertyContext = propertyContext(propertyViewPropertyContexts, ctx -> ctx.LINE_STYLE() != null, "lineStyle", propertyViewContext.diagramIdentifier());
        builder.append(", geometry=^" + M2DiagramPaths.LineGeometry + "(style=" + M2DiagramPaths.LineStyle + ".");
        builder.append(lineStylePropertyContext.diagramIdentifier().getText());
        DiagramAntlrParser.PropertyViewPropertyContext pointsPropertyContext = propertyContext(propertyViewPropertyContexts, ctx -> ctx.POINTS() != null, "points", propertyViewContext.diagramIdentifier());
        builder.append(", points=[");
        for (int i = 0; i < pointsPropertyContext.FLOAT().size(); i = i + 2)
        {
            if (i == 0)
            {
                builder.append("^" + M2DiagramPaths.Point + "(x=");
            }
            else
            {
                builder.append(", ^" + M2DiagramPaths.Point + "(x=");
            }
            builder.append(pointsPropertyContext.FLOAT(i).getText());
            builder.append(", y=");
            builder.append(pointsPropertyContext.FLOAT(i + 1).getText());
            builder.append(")");
        }
        builder.append("])");

        // Rendering
        DiagramAntlrParser.PropertyViewPropertyContext colorPropertyContext = propertyContext(propertyViewPropertyContexts, ctx -> ctx.COLOR() != null, "color", propertyViewContext.diagramIdentifier());
        builder.append(", rendering=^" + M2DiagramPaths.Rendering + "(color='");
        builder.append(colorPropertyContext.COLOR_STRING().getText());
        DiagramAntlrParser.PropertyViewPropertyContext lineWidthPropertyContext = propertyContext(propertyViewPropertyContexts, ctx -> ctx.LINE_WIDTH() != null, "lineWidth", propertyViewContext.diagramIdentifier());
        builder.append("', lineWidth=");
        builder.append(lineWidthPropertyContext.FLOAT(0).getText());
        builder.append(')');

        // Label
        DiagramAntlrParser.PropertyViewPropertyContext labelPropertyContext = propertyContext(propertyViewPropertyContexts, ctx -> ctx.LABEL() != null, "label", propertyViewContext.diagramIdentifier());
        builder.append(", label=");
        builder.append(labelPropertyContext.STRING().getText());

        // Source
        // NOTE: we put the id string here and convert it to a TypeView in post-processing
        DiagramAntlrParser.PropertyViewPropertyContext sourcePropertyContext = propertyContext(propertyViewPropertyContexts, ctx -> ctx.SOURCE() != null, "source", propertyViewContext.diagramIdentifier());
        builder.append(", source='");
        builder.append(sourcePropertyContext.diagramIdentifier().getText());
        builder.append("'");

        // Target
        // NOTE: we put the id string here and convert it to a TypeView in post-processing
        DiagramAntlrParser.PropertyViewPropertyContext targetPropertyContext = propertyContext(propertyViewPropertyContexts, ctx -> ctx.TARGET() != null, "target", propertyViewContext.diagramIdentifier());
        builder.append(", target='");
        builder.append(targetPropertyContext.diagramIdentifier().getText());
        builder.append("'");

        // Property/multiplicity position
        DiagramAntlrParser.PropertyViewPropertyContext propertyPositionPropertyContext = propertyContext(propertyViewPropertyContexts, ctx -> ctx.PROP_POSITION() != null, "propertyPosition", propertyViewContext.diagramIdentifier());
        builder.append(", view=^" + M2DiagramPaths.AssociationPropertyView + "(propertyLocation=^" + M2DiagramPaths.Point + "(x=");
        builder.append(propertyPositionPropertyContext.FLOAT(0).getText());
        builder.append(", y=");
        builder.append(propertyPositionPropertyContext.FLOAT(1).getText());
        DiagramAntlrParser.PropertyViewPropertyContext multiplicityPositionPropertyContext = propertyContext(propertyViewPropertyContexts, ctx -> ctx.MULT_POSITION() != null, "multiplicityPosition", propertyViewContext.diagramIdentifier());
        builder.append("), multiplicityLocation=^" + M2DiagramPaths.Point + "(x=");
        builder.append(multiplicityPositionPropertyContext.FLOAT(0));
        builder.append(", y=");
        builder.append(multiplicityPositionPropertyContext.FLOAT(1));
        builder.append(")))");

        return builder.toString();

    }

    private String processAssociationView(DiagramAntlrParser.AssociationViewContext associationViewContext, String importId, String diagramFullQualifiedName)
    {
        String associationViewName = associationViewContext.diagramIdentifier().getText();

        StringBuilder builder = new StringBuilder("^" + M2DiagramPaths.AssociationView + " ");
        builder.append(sourceInformation.getPureSourceInformation(associationViewContext.getStart(), associationViewContext.diagramIdentifier().getStart(), associationViewContext.getStop()).toM4String());

        // Id
        builder.append("(id='");
        builder.append(associationViewName);
        builder.append("'");

        // Diagram
        builder.append(", diagram=");
        builder.append(diagramFullQualifiedName);

        MutableList<DiagramAntlrParser.AssociationViewPropertyContext> associationViewPropertyContexts = ListAdapter.adapt(associationViewContext.associationViewProperty());
        // Association
        DiagramAntlrParser.AssociationViewPropertyContext associationPropertyContext = propertyContext(associationViewPropertyContexts, ctx -> ctx.ASSOCIATION() != null, "association", associationViewContext.diagramIdentifier());
        builder.append(", association=");
        builder.append("^").append(M3Paths.ImportStub).append(" ");
        builder.append(sourceInformation.getPureSourceInformation(associationPropertyContext.qualifiedDiagramName().diagramPackagePath() != null ? associationPropertyContext.qualifiedDiagramName().diagramPackagePath().diagramIdentifier(0).getStart() : associationPropertyContext.qualifiedDiagramName().diagramIdentifier().getStart(), associationPropertyContext.qualifiedDiagramName().diagramIdentifier().getStart(), associationPropertyContext.qualifiedDiagramName().diagramIdentifier().getStop()).toM4String());
        builder.append(" (importGroup=system::imports::");
        builder.append(importId);
        builder.append(", idOrPath='");
        builder.append(associationPropertyContext.qualifiedDiagramName().getText());
        builder.append("')");

        // Association visibility
        DiagramAntlrParser.AssociationViewPropertyContext stereotypesVisiblePropertyContext = propertyContext(associationViewPropertyContexts, ctx -> ctx.STEREOTYPES_VISIBLE() != null, "stereotypesVisible", associationViewContext.diagramIdentifier());
        builder.append(", visibility=^").append(M2DiagramPaths.AssociationVisibility).append("(visibleStereotype=");
        builder.append(stereotypesVisiblePropertyContext.BOOLEAN().getText());
        DiagramAntlrParser.AssociationViewPropertyContext nameVisiblePropertyContext = propertyContext(associationViewPropertyContexts, ctx -> ctx.NAME_VISIBLE() != null, "nameVisible", associationViewContext.diagramIdentifier());
        builder.append(", visibleName=");
        builder.append(nameVisiblePropertyContext.BOOLEAN().getText());
        builder.append(')');

        // Geometry
        DiagramAntlrParser.AssociationViewPropertyContext lineStylePropertyContext = propertyContext(associationViewPropertyContexts, ctx -> ctx.LINE_STYLE() != null, "lineStyle", associationViewContext.diagramIdentifier());
        builder.append(", geometry=^").append(M2DiagramPaths.LineGeometry).append("(style=").append(M2DiagramPaths.LineStyle).append(".");
        builder.append(lineStylePropertyContext.diagramIdentifier().getText());
        DiagramAntlrParser.AssociationViewPropertyContext pointsPropertyContext = propertyContext(associationViewPropertyContexts, ctx -> ctx.POINTS() != null, "points", associationViewContext.diagramIdentifier());
        builder.append(", points=[");
        for (int i = 0; i < pointsPropertyContext.FLOAT().size(); i = i + 2)
        {
            if (i == 0)
            {
                builder.append("^").append(M2DiagramPaths.Point).append("(x=");
            }
            else
            {
                builder.append(", ^").append(M2DiagramPaths.Point).append("(x=");
            }
            builder.append(pointsPropertyContext.FLOAT(i).getText());
            builder.append(", y=");
            builder.append(pointsPropertyContext.FLOAT(i + 1).getText());
            builder.append(")");
        }

        builder.append("])");

        // Rendering
        DiagramAntlrParser.AssociationViewPropertyContext colorPropertyContext = propertyContext(associationViewPropertyContexts, ctx -> ctx.COLOR() != null, "color", associationViewContext.diagramIdentifier());
        builder.append(", rendering=^").append(M2DiagramPaths.Rendering).append("(color='");
        builder.append(colorPropertyContext.COLOR_STRING().getText());
        DiagramAntlrParser.AssociationViewPropertyContext lineWidthPropertyContext = propertyContext(associationViewPropertyContexts, ctx -> ctx.LINE_WIDTH() != null, "lineWidth", associationViewContext.diagramIdentifier());
        builder.append("', lineWidth=");
        builder.append(lineWidthPropertyContext.FLOAT().get(0).getText());
        builder.append(')');

        // Label
        DiagramAntlrParser.AssociationViewPropertyContext labelPropertyContext = propertyContext(associationViewPropertyContexts, ctx -> ctx.LABEL() != null, "label", associationViewContext.diagramIdentifier());
        builder.append(", label=");
        builder.append(labelPropertyContext.STRING().getText());

        // Source
        // NOTE: we put the id string here and convert it to a TypeView in post-processing
        DiagramAntlrParser.AssociationViewPropertyContext sourcePropertyContext = propertyContext(associationViewPropertyContexts, ctx -> ctx.SOURCE() != null, "source", associationViewContext.diagramIdentifier());
        builder.append(", source='");
        builder.append(sourcePropertyContext.diagramIdentifier().getText());
        builder.append("'");

        // Target
        // NOTE: we put the id string here and convert it to a TypeView in post-processing
        DiagramAntlrParser.AssociationViewPropertyContext targetPropertyContext = propertyContext(associationViewPropertyContexts, ctx -> ctx.TARGET() != null, "target", associationViewContext.diagramIdentifier());
        builder.append(", target='");
        builder.append(targetPropertyContext.diagramIdentifier().getText());
        builder.append("'");

        // Source position
        DiagramAntlrParser.AssociationViewPropertyContext sourcePropertyPositionPropertyContext = propertyContext(associationViewPropertyContexts, ctx -> ctx.SOURCE_PROP_POSITION() != null, "sourcePropertyPosition", associationViewContext.diagramIdentifier());
        builder.append(", sourcePropertyView=^").append(M2DiagramPaths.AssociationPropertyView).append("(propertyLocation=^").append(M2DiagramPaths.Point).append("(x=");
        builder.append(sourcePropertyPositionPropertyContext.FLOAT(0).getText());
        builder.append(", y=");
        builder.append(sourcePropertyPositionPropertyContext.FLOAT(1).getText());
        DiagramAntlrParser.AssociationViewPropertyContext sourceMultiplicityPositionPropertyContext = propertyContext(associationViewPropertyContexts, ctx -> ctx.SOURCE_MULT_POSITION() != null, "sourceMultiplicityPosition", associationViewContext.diagramIdentifier());
        builder.append("), multiplicityLocation=^").append(M2DiagramPaths.Point).append("(x=");
        builder.append(sourceMultiplicityPositionPropertyContext.FLOAT(0).getText());
        builder.append(", y=");
        builder.append(sourceMultiplicityPositionPropertyContext.FLOAT(1).getText());
        builder.append("))");

        // Target position
        DiagramAntlrParser.AssociationViewPropertyContext targetPropertyPositionPropertyContext = propertyContext(associationViewPropertyContexts, ctx -> ctx.TARGET_PROP_POSITION() != null, "targetPropertyPosition", associationViewContext.diagramIdentifier());
        builder.append(", targetPropertyView=^").append(M2DiagramPaths.AssociationPropertyView).append("(propertyLocation=^").append(M2DiagramPaths.Point).append("(x=");
        builder.append(targetPropertyPositionPropertyContext.FLOAT(0).getText());
        builder.append(", y=");
        builder.append(targetPropertyPositionPropertyContext.FLOAT(1).getText());
        DiagramAntlrParser.AssociationViewPropertyContext targetMultiplicityPositionPropertyContext = propertyContext(associationViewPropertyContexts, ctx -> ctx.TARGET_MULT_POSITION() != null, "targetMultiplicityPosition", associationViewContext.diagramIdentifier());
        builder.append("), multiplicityLocation=^").append(M2DiagramPaths.Point).append("(x=");
        builder.append(targetMultiplicityPositionPropertyContext.FLOAT(0).getText());
        builder.append(", y=");
        builder.append(targetMultiplicityPositionPropertyContext.FLOAT(1).getText());
        builder.append(")))");

        return builder.toString();
    }

    private String processTypeView(DiagramAntlrParser.TypeViewContext typeViewContext, String importId, String diagramFullQualifiedName)
    {

        String typeViewName = typeViewContext.diagramIdentifier().getText();

        StringBuilder builder = new StringBuilder("^" + M2DiagramPaths.TypeView + " ");
        builder.append(sourceInformation.getPureSourceInformation(typeViewContext.getStart(), typeViewContext.diagramIdentifier().getStart(), typeViewContext.getStop()).toM4String());
        // Id
        builder.append("(id='");
        builder.append(typeViewName);
        builder.append("'");
        // Diagram
        builder.append(", diagram=");
        builder.append(diagramFullQualifiedName);

        MutableList<DiagramAntlrParser.TypeViewPropertyContext> typeViewPropertyContexts = ListAdapter.adapt(typeViewContext.typeViewProperty());

        // Type
        DiagramAntlrParser.TypeViewPropertyContext typePropertyContext = propertyContext(typeViewPropertyContexts, ctx -> ctx.TYPE() != null, "type", typeViewContext.diagramIdentifier());
        builder.append(", ").append(typePropertyContext.TYPE().getText());
        builder.append("^").append(M3Paths.ImportStub).append(" ");
        builder.append(sourceInformation.getPureSourceInformation(typePropertyContext.qualifiedDiagramName().diagramPackagePath() != null ? typePropertyContext.qualifiedDiagramName().diagramPackagePath().diagramIdentifier(0).getStart() : typePropertyContext.qualifiedDiagramName().diagramIdentifier().getStart(), typePropertyContext.qualifiedDiagramName().diagramIdentifier().getStart(), typePropertyContext.qualifiedDiagramName().diagramIdentifier().getStop()).toM4String());
        builder.append(" (importGroup=system::imports::");
        builder.append(importId);
        builder.append(", idOrPath='");
        builder.append(typePropertyContext.qualifiedDiagramName().getText());
        builder.append("')");

        // Type visibility
        DiagramAntlrParser.TypeViewPropertyContext stereotypesVisiblePropertyContext = propertyContext(typeViewPropertyContexts, ctx -> ctx.STEREOTYPES_VISIBLE() != null, "stereotypesVisible", typeViewContext.diagramIdentifier());
        builder.append(", typeVisibility=^").append(M2DiagramPaths.TypeVisibility).append("(visibleStereotype=");
        builder.append(stereotypesVisiblePropertyContext.BOOLEAN().getText());
        DiagramAntlrParser.TypeViewPropertyContext attributesVisiblePropertyContext = propertyContext(typeViewPropertyContexts, ctx -> ctx.ATTRIBUTES_VISIBLE() != null, "attributesVisible", typeViewContext.diagramIdentifier());
        builder.append(", visibleAttributeCompartment=");
        builder.append(attributesVisiblePropertyContext.BOOLEAN().getText());
        builder.append(')');

        // Attribute visibility
        DiagramAntlrParser.TypeViewPropertyContext attributeStereotypesVisiblePropertyContext = propertyContext(typeViewPropertyContexts, ctx -> ctx.ATTRIBUTE_STEREOTYPES_VISIBLE() != null, "attributeStereotypesVisible", typeViewContext.diagramIdentifier());
        builder.append(", attributeVisibility=^").append(M2DiagramPaths.AttributeVisibility).append("(visibleStereotype=");
        builder.append(attributeStereotypesVisiblePropertyContext.BOOLEAN().getText());
        DiagramAntlrParser.TypeViewPropertyContext attributeTypesVisiblePropertyContext = propertyContext(typeViewPropertyContexts, ctx -> ctx.ATTRIBUTE_TYPES_VISIBLE() != null, "attributeTypesVisible", typeViewContext.diagramIdentifier());
        builder.append(", visibleTypes=");
        builder.append(attributeTypesVisiblePropertyContext.BOOLEAN().getText());
        builder.append(')');

        // Rendering
        DiagramAntlrParser.TypeViewPropertyContext colorPropertyContext = propertyContext(typeViewPropertyContexts, ctx -> ctx.COLOR() != null, "color", typeViewContext.diagramIdentifier());
        builder.append(", rendering=^").append(M2DiagramPaths.Rendering).append("(color='");
        builder.append(colorPropertyContext.COLOR_STRING().getText());
        DiagramAntlrParser.TypeViewPropertyContext lineWidthPropertyContext = propertyContext(typeViewPropertyContexts, ctx -> ctx.LINE_WIDTH() != null, "lineWidth", typeViewContext.diagramIdentifier());
        builder.append("', lineWidth=");
        builder.append(lineWidthPropertyContext.FLOAT(0).getText());
        builder.append(')');

        // Position
        DiagramAntlrParser.TypeViewPropertyContext positionPropertyContext = propertyContext(typeViewPropertyContexts, ctx -> ctx.POSITION() != null, "position", typeViewContext.diagramIdentifier());
        builder.append(", position=^").append(M2DiagramPaths.Point).append("(x=");
        builder.append(positionPropertyContext.FLOAT(0).getText());
        builder.append(", y=");
        builder.append(positionPropertyContext.FLOAT(1).getText());
        builder.append(')');

        // Rectangle geometry
        DiagramAntlrParser.TypeViewPropertyContext widthPropertyContext = propertyContext(typeViewPropertyContexts, ctx -> ctx.WIDTH() != null, "width", typeViewContext.diagramIdentifier());
        builder.append(", rectangleGeometry=^").append(M2DiagramPaths.RectangleGeometry).append("(width=");
        builder.append(widthPropertyContext.FLOAT(0).getText());
        DiagramAntlrParser.TypeViewPropertyContext heightPropertyContext = propertyContext(typeViewPropertyContexts, ctx -> ctx.HEIGHT() != null, "height", typeViewContext.diagramIdentifier());
        builder.append(", height=");
        builder.append(heightPropertyContext.FLOAT(0).getText());
        builder.append("))");

        return builder.toString();
    }

    public static String createImportGroupId(String fileName, int count)
    {
        return Source.importForSourceName(fileName) + "_" + count;
    }

    private <T> T propertyContext(MutableList<T> propertyContexts, Predicate<? super T> predicate, String propertyName, DiagramAntlrParser.DiagramIdentifierContext diagramIdentifierContext)
    {
        MutableList<T> contexts = propertyContexts.select(predicate);
        if (contexts.isEmpty())
        {
            throw new PureParserException(this.sourceInformation.getPureSourceInformation(diagramIdentifierContext.start, diagramIdentifierContext.start, diagramIdentifierContext.stop), "Missing value for property '" + propertyName + "' on " + ((ParserRuleContext) diagramIdentifierContext.parent).start.getText() + " " + diagramIdentifierContext.getText());
        }
        if (contexts.size() > 1)
        {
            T secondOccurence = contexts.get(1);
            SourceInformation sourceInfo = this.sourceInformation.getPureSourceInformation(((ParserRuleContext) secondOccurence).start, ((ParserRuleContext) secondOccurence).start, ((ParserRuleContext) secondOccurence).stop);
            throw new PureCompilationException(sourceInfo, propertyName + " should only be specified once");
        }
        return contexts.get(0);
    }

    private static void appendPureCollection(StringBuilder builder, RichIterable<String> collection, String prefix)
    {
        appendPureCollection(builder, collection, prefix, false);
    }

    private static void appendPureCollection(StringBuilder builder, RichIterable<String> collection, String prefix, boolean emptyListIfEmpty)
    {
        switch (collection.size())
        {
            case 0:
            {
                if (emptyListIfEmpty)
                {
                    builder.append(prefix);
                    builder.append("[]");
                }
                break;
            }
            case 1:
            {
                builder.append(prefix);
                builder.append(collection.getFirst());
                break;
            }
            default:
            {
                builder.append(prefix);
                collection.appendString(builder, "[", ", ", "]");
            }
        }
    }

    public ImportGroup imports(String src, int offset, DiagramAntlrParser.ImportsContext ctx)
    {
        MutableList<Import> imports = Lists.mutable.empty();
        int importGroupStartLine = -1;
        int importGroupStartColumn = -1;
        int importGroupEndLine = -1;
        int importGroupEndColumn = -1;
        for (DiagramAntlrParser.Import_statementContext isCtx : ctx.import_statement())
        {
            Import _import = ImportInstance.createPersistent(this.repository, this.sourceInformation.getPureSourceInformation(isCtx.getStart(), isCtx.packagePath().getStart(), isCtx.STAR().getSymbol()), this.packageToString(isCtx.packagePath().diagramIdentifier()));

            imports.add(_import);
            SourceInformation sourceInfo = _import.getSourceInformation();
            if (importGroupStartLine == -1)
            {
                importGroupStartLine = sourceInfo.getStartLine();
                importGroupStartColumn = sourceInfo.getStartColumn();
                importGroupEndLine = sourceInfo.getEndLine();
                importGroupEndColumn = sourceInfo.getEndColumn();
            }
            if (importGroupStartLine > sourceInfo.getStartLine())
            {
                importGroupStartLine = sourceInfo.getStartLine();
                importGroupStartColumn = sourceInfo.getStartColumn();
            }
            if (importGroupEndLine < sourceInfo.getEndLine())
            {
                importGroupEndLine = sourceInfo.getEndLine();
                importGroupEndColumn = sourceInfo.getEndColumn();
            }
        }
        if (importGroupStartLine == -1)
        {
            importGroupStartLine = 1 + offset;
            importGroupStartColumn = 0;
            importGroupEndLine = 1 + offset;
            importGroupEndColumn = 0;
        }
        return buildImportGroupFromImport(sourceInformation.getSourceName(), this.count, imports, new SourceInformation(this.sourceInformation.getSourceName(), importGroupStartLine, importGroupStartColumn, importGroupEndLine, importGroupEndColumn));
    }

    public ImportGroup buildImportGroupFromImport(String fileName, int count, ListIterable<Import> imports, SourceInformation sourceInfo)
    {
        String id = createImportGroupId(fileName, count);
        ImportGroup ig = ImportGroupInstance.createPersistent(this.repository, id, sourceInfo)
                ._name(id)
                ._imports(imports);
        Package parent = ((Package) this.processorSupport.package_getByUserPath("system::imports"))._childrenAdd(ig);
        ig._package(parent);
        return ig;
    }

    public String packageToString(List<DiagramAntlrParser.DiagramIdentifierContext> identifier)
    {
        return LazyIterate.collect(identifier, RuleContext::getText).makeString("::");
    }
}
