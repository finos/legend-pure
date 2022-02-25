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
import org.eclipse.collections.api.factory.Lists;
import org.eclipse.collections.api.list.ListIterable;
import org.eclipse.collections.api.list.MutableList;
import org.eclipse.collections.api.multimap.MutableMultimap;
import org.eclipse.collections.impl.factory.Multimaps;
import org.eclipse.collections.impl.utility.LazyIterate;
import org.finos.legend.pure.m2.dsl.mapping.serialization.grammar.MappingParser;
import org.finos.legend.pure.m2.dsl.mapping.serialization.grammar.MappingParserBaseVisitor;
import org.finos.legend.pure.m3.compiler.Context;
import org.finos.legend.pure.m3.coreinstance.Package;
import org.finos.legend.pure.m3.coreinstance.meta.pure.metamodel._import.Import;
import org.finos.legend.pure.m3.coreinstance.meta.pure.metamodel._import.ImportGroup;
import org.finos.legend.pure.m3.coreinstance.meta.pure.metamodel._import.ImportGroupInstance;
import org.finos.legend.pure.m3.coreinstance.meta.pure.metamodel._import.ImportInstance;
import org.finos.legend.pure.m3.navigation.M3ProcessorSupport;
import org.finos.legend.pure.m3.navigation.ProcessorSupport;
import org.finos.legend.pure.m3.serialization.grammar.Parser;
import org.finos.legend.pure.m3.serialization.grammar.ParserLibrary;
import org.finos.legend.pure.m3.serialization.runtime.Source;
import org.finos.legend.pure.m4.ModelRepository;
import org.finos.legend.pure.m4.coreinstance.SourceInformation;
import org.finos.legend.pure.m4.exception.PureException;
import org.finos.legend.pure.m4.serialization.grammar.antlr.AntlrSourceInformation;
import org.finos.legend.pure.m4.serialization.grammar.antlr.PureParserException;

import java.util.Iterator;
import java.util.List;

public class MappingGraphBuilder extends MappingParserBaseVisitor<String>
{

    private final int count;
    private final ModelRepository repository;
    private final Context context;
    private final AntlrSourceInformation sourceInformation;
    private final ProcessorSupport processorSupport;
    private final ParserLibrary parserLibrary;
    private final StringBuilder sb = new StringBuilder();

    private final MutableMultimap<String, String> results = Multimaps.mutable.list.empty();
    private final MutableList<String> includes = Lists.mutable.empty();

    private String importId;

    public MappingGraphBuilder(ModelRepository repository, Context context, int count, ParserLibrary parserLibrary, AntlrSourceInformation sourceInformation)
    {
        this.count = count;
        this.repository = repository;
        this.context = context;
        this.sourceInformation = sourceInformation;
        this.parserLibrary = parserLibrary;
        this.processorSupport = new M3ProcessorSupport(repository);
    }

    @Override
    public String visitDefinition(MappingParser.DefinitionContext ctx)
    {
        final ImportGroup importGroup = imports(this.sourceInformation.getSourceName(), this.sourceInformation.getOffsetLine(), ctx.imports());
        this.importId = importGroup.getName();
        visitChildren(ctx);
        return sb.toString();
    }

    @Override
    public String visitMapping(MappingParser.MappingContext ctx)
    {
        visitChildren(ctx);
        String mappingName = ctx.qualifiedName().identifier().getText();
        boolean hasPackage = ctx.qualifiedName().packagePath() != null;
        String packageName = hasPackage ? LazyIterate.collect(ctx.qualifiedName().packagePath().identifier(), RuleContext::getText).makeString("::") : "";
        if (ctx.tests() != null)
        {
            throw new PureParserException(this.sourceInformation.getPureSourceInformation(ctx.tests().getStart()),
                    "Grammar Tests in Mapping currently not supported in Pure");
        }
        this.sb.append("^meta::pure::mapping::Mapping ")
                .append(mappingName).append(this.sourceInformation.getPureSourceInformation(ctx.MAPPING().getSymbol(), ctx.qualifiedName().identifier().getStart(), ctx.GROUP_CLOSE().getSymbol()).toM4String()).append(hasPackage ? "@" : "").append(packageName)
                .append("(name='").append(mappingName).append("', package=").append(hasPackage ? packageName : "::")
                .append(", includes = [").append(this.includes.makeString(",")).append("]")
                .append(", enumerationMappings = [").append(this.results.get("enumerationMappings").makeString(",")).append("]")
                .append(", classMappings = [").append(this.results.get("classMappings").makeString(",")).append("]")
                .append(", associationMappings = [").append(this.results.get("associationMappings").makeString(",")).append("])");
        this.results.clear();
        this.includes.clear();
        return null;
    }

    @Override
    public String visitIncludeMapping(MappingParser.IncludeMappingContext ctx)
    {
        StringBuilder builder = new StringBuilder();
        String sourceInfo = this.sourceInformation.getPureSourceInformation(ctx.qualifiedName().identifier().getStart()).toM4String();
        builder.append("^meta::pure::mapping::MappingInclude");
        builder.append(sourceInfo);
        builder.append("(");

        // owner mapping (we include a direct reference rather than an import stub since the Mapping owns the MappingInclude)
        builder.append("owner=").append(((MappingParser.MappingContext) ctx.getParent()).qualifiedName().getText());

        // included mapping
        builder.append(", included=^meta::pure::metamodel::import::ImportStub");
        builder.append(sourceInfo);
        builder.append("(importGroup=system::imports::");
        builder.append(this.importId);
        builder.append(", idOrPath='").append(ctx.qualifiedName().getText()).append("')");

        // source substitutions
        if (ctx.storeSubPath() != null)
        {
            builder.append(", storeSubstitutions=[");
            Iterator<MappingParser.StoreSubPathContext> iterator = ctx.storeSubPath().iterator();
            while (iterator.hasNext())
            {
                MappingParser.StoreSubPathContext storeSubPathContext = iterator.next();
                builder.append("^meta::pure::mapping::SubstituteStore(original=^meta::pure::metamodel::import::ImportStub");
                builder.append(this.sourceInformation.getPureSourceInformation(storeSubPathContext.sourceStore().qualifiedName().identifier().getStart()).toM4String());
                builder.append("(importGroup=system::imports::").append(this.importId).append(", idOrPath='").append(storeSubPathContext.sourceStore().qualifiedName().getText()).append("')");
                builder.append(", substitute=^meta::pure::metamodel::import::ImportStub");
                builder.append(this.sourceInformation.getPureSourceInformation(storeSubPathContext.targetStore().qualifiedName().identifier().getStart()).toM4String());
                builder.append("(importGroup=system::imports::").append(this.importId).append(", idOrPath='").append(storeSubPathContext.targetStore().qualifiedName().getText()).append("'))");
                if (iterator.hasNext())
                {
                    builder.append(", ");
                }
            }
            builder.append(']');
        }

        builder.append(')');
        this.includes.add(builder.toString());

        return null;
    }

    @Override
    public String visitClassMapping(MappingParser.ClassMappingContext ctx)
    {
        StringBuilder builder = new StringBuilder();
        String parserName = ctx.parserName().getText();

        if (ctx.superClassMappingId() != null && !("Relational").equals(parserName))
        {
            throw new PureParserException(this.sourceInformation.getPureSourceInformation(ctx.parserName().getStart()),
                    "Mapping Inheritance feature is applicable only for Class Mappings, not applicable for " + ("Pure".equals(parserName) ? "Model to Model Pure" : parserName) + " Mappings.");
        }

        Parser parser;
        try
        {
            parser = this.parserLibrary.getParser(parserName);
        }
        catch (RuntimeException e)
        {
            throw new PureParserException(this.sourceInformation.getPureSourceInformation(ctx.mappingInstance().CURLY_BRACKET_OPEN().getSymbol()), e.getMessage(), e);
        }

        String setSourceInfo = this.sourceInformation.getPureSourceInformation(ctx.qualifiedName().packagePath() != null ? ctx.qualifiedName().packagePath().getStart() : ctx.qualifiedName().identifier().getStart(), ctx.qualifiedName().identifier().getStart(),
                ctx.mappingInstance().mappingInstanceElement().get(ctx.mappingInstance().mappingInstanceElement().size() - 1).getStart()).toM4String();
        String classMappingName = ctx.classMappingName() == null ? "default" : ctx.classMappingName().getText();
        String classSourceInfo = this.sourceInformation.getPureSourceInformation(ctx.qualifiedName().identifier().getStart()).toM4String();

        StringBuilder mappingInstanceText = new StringBuilder();
        for (MappingParser.MappingInstanceElementContext element : ctx.mappingInstance().mappingInstanceElement())
        {
            mappingInstanceText.append(element.getText());
        }
        String parseContent = mappingInstanceText.length() > 0 ? mappingInstanceText.substring(0, mappingInstanceText.length() - 1) : mappingInstanceText.toString();
        if (parserName.equals("EnumerationMapping"))
        {
            String linesResult = parser.parseMapping(parseContent, ctx.classMappingId() == null ? null : ctx.classMappingId().getText(), null, setSourceInfo,
                    ctx.STAR() != null, ctx.qualifiedName().getText(), this.sourceInformation.getPureSourceInformation(ctx.qualifiedName().identifier().getStart()).toM4String(), ((MappingParser.MappingContext) ctx.getParent()).qualifiedName().getText(), this.sourceInformation.getSourceName(),
                    this.sourceInformation.getOffsetLine() + ctx.mappingInstance().CURLY_BRACKET_OPEN().getSymbol().getLine() - 1, this.importId, this.repository, this.context);

            builder.append("^meta::pure::mapping::EnumerationMapping<Any>").append(classSourceInfo)
                    .append("(")
                    .append("name = '").append(classMappingName).append("',")
                    .append("parent = ").append(((MappingParser.MappingContext) ctx.getParent()).qualifiedName().getText()).append(",")
                    .append("enumeration = ").append("^meta::pure::metamodel::import::ImportStub ")
                    .append(this.sourceInformation.getPureSourceInformation(ctx.qualifiedName().identifier().getStart()).toM4String())
                    .append(" (importGroup=system::imports::").append(this.importId).append(", idOrPath='").append(ctx.qualifiedName().getText()).append("')").append(",")
                    .append("enumValueMappings = [").append(linesResult).append("]")
                    .append(")");

            this.results.put("enumerationMappings", builder.toString());
        }
        else
        {
            try
            {
                builder.append(parser.parseMapping(parseContent, ctx.classMappingId() == null ? null : ctx.classMappingId().getText(), ctx.superClassMappingId() == null ? null : ctx.superClassMappingId().getText(), setSourceInfo, ctx.STAR() != null, ctx.qualifiedName().getText(), classSourceInfo,
                        ((MappingParser.MappingContext) ctx.getParent()).qualifiedName().getText(), this.sourceInformation.getSourceName(), this.sourceInformation.getOffsetLine() + ctx.mappingInstance().CURLY_BRACKET_OPEN().getSymbol().getLine() - 1, this.importId, this.repository, this.context));
            }
            catch (PureException e)
            {
                if (e.getSourceInformation() != null)
                {
                    throw e;
                }
                throw new PureParserException(this.sourceInformation.getPureSourceInformation(ctx.mappingInstance().CURLY_BRACKET_OPEN().getSymbol()), e.getInfo(), e);
            }
            catch (RuntimeException e)
            {
                PureException pe = PureException.findPureException(e);
                if ((pe != null) && (pe.getSourceInformation() != null))
                {
                    throw e;
                }
                throw new PureParserException(this.sourceInformation.getPureSourceInformation(ctx.mappingInstance().CURLY_BRACKET_OPEN().getSymbol()), e.getMessage(), e);
            }

            String mapping = builder.toString();
            if (mapping.contains("AssociationImplementation"))
            {
                this.results.put("associationMappings", mapping);
            }
            else
            {
                this.results.put("classMappings", mapping);
            }
        }
        return null;
    }

    public ImportGroup imports(String src, int offset, MappingParser.ImportsContext ctx)
    {
        MutableList<Import> imports = Lists.mutable.empty();
        int importGroupStartLine = -1;
        int importGroupStartColumn = -1;
        int importGroupEndLine = -1;
        int importGroupEndColumn = -1;
        for (MappingParser.Import_statementContext isCtx : ctx.import_statement())
        {
            Import _import = ImportInstance.createPersistent(this.repository, this.sourceInformation.getPureSourceInformation(isCtx.getStart(), isCtx.packagePath().getStart(), isCtx.STAR().getSymbol()), this.packageToString(isCtx.packagePath().identifier()));

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
        return buildImportGroupFromImport(src, this.count, imports, new SourceInformation(src, importGroupStartLine, importGroupStartColumn, importGroupEndLine, importGroupEndColumn));
    }

    public ImportGroupInstance buildImportGroupFromImport(String fileName, int count, ListIterable<Import> imports, SourceInformation sourceInfo)
    {
        String id = createImportGroupId(fileName, count);
        ImportGroupInstance ig = ImportGroupInstance.createPersistent(this.repository, id, sourceInfo);
        ig._imports(imports);

        Package parent = (Package) this.processorSupport.package_getByUserPath("system::imports");
        parent._childrenAdd(ig);
        ig._package(parent);
        ig._name(id);
        return ig;
    }

    public String packageToString(List<MappingParser.IdentifierContext> identifier)
    {
        return LazyIterate.collect(identifier, RuleContext::getText).makeString("::");
    }

    public static String createImportGroupId(String fileName, int count)
    {
        return Source.importForSourceName(fileName) + "_" + count;
    }
}
