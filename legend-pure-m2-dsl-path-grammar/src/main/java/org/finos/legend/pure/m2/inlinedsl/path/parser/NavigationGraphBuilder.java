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

package org.finos.legend.pure.m2.inlinedsl.path.parser;

import org.antlr.v4.runtime.Token;
import org.apache.commons.lang3.StringEscapeUtils;
import org.eclipse.collections.api.factory.Lists;
import org.eclipse.collections.api.list.ListIterable;
import org.eclipse.collections.api.list.MutableList;
import org.eclipse.collections.impl.utility.ListIterate;
import org.finos.legend.pure.m2.inlinedsl.path.M2PathPaths;
import org.finos.legend.pure.m3.compiler.Context;
import org.finos.legend.pure.m3.coreinstance.meta.pure.metamodel._import.EnumStubInstance;
import org.finos.legend.pure.m3.coreinstance.meta.pure.metamodel._import.ImportGroup;
import org.finos.legend.pure.m3.coreinstance.meta.pure.metamodel._import.ImportStub;
import org.finos.legend.pure.m3.coreinstance.meta.pure.metamodel._import.ImportStubInstance;
import org.finos.legend.pure.m3.coreinstance.meta.pure.metamodel._import.PropertyStub;
import org.finos.legend.pure.m3.coreinstance.meta.pure.metamodel._import.PropertyStubInstance;
import org.finos.legend.pure.m3.coreinstance.meta.pure.metamodel.path.Path;
import org.finos.legend.pure.m3.coreinstance.meta.pure.metamodel.path.PathElement;
import org.finos.legend.pure.m3.coreinstance.meta.pure.metamodel.path.PropertyPathElement;
import org.finos.legend.pure.m3.coreinstance.meta.pure.metamodel.type.Class;
import org.finos.legend.pure.m3.coreinstance.meta.pure.metamodel.type.generics.GenericType;
import org.finos.legend.pure.m3.coreinstance.meta.pure.metamodel.type.generics.GenericTypeInstance;
import org.finos.legend.pure.m3.coreinstance.meta.pure.metamodel.valuespecification.InstanceValueInstance;
import org.finos.legend.pure.m3.coreinstance.meta.pure.metamodel.valuespecification.ValueSpecification;
import org.finos.legend.pure.m2.inlinedsl.path.serialization.grammar.NavigationParser.AtomicContext;
import org.finos.legend.pure.m2.inlinedsl.path.serialization.grammar.NavigationParser.CollectionContext;
import org.finos.legend.pure.m2.inlinedsl.path.serialization.grammar.NavigationParser.DefinitionContext;
import org.finos.legend.pure.m2.inlinedsl.path.serialization.grammar.NavigationParser.EnumStubContext;
import org.finos.legend.pure.m2.inlinedsl.path.serialization.grammar.NavigationParser.GenericTypeContext;
import org.finos.legend.pure.m2.inlinedsl.path.serialization.grammar.NavigationParser.ParameterContext;
import org.finos.legend.pure.m2.inlinedsl.path.serialization.grammar.NavigationParser.PropertyWithParametersContext;
import org.finos.legend.pure.m2.inlinedsl.path.serialization.grammar.NavigationParser.ScalarContext;
import org.finos.legend.pure.m2.inlinedsl.path.serialization.grammar.NavigationParserBaseVisitor;
import org.finos.legend.pure.m3.navigation.M3ProcessorSupport;
import org.finos.legend.pure.m3.navigation.ProcessorSupport;
import org.finos.legend.pure.m3.serialization.grammar.m3parser.antlr.M3AntlrParser;
import org.finos.legend.pure.m4.ModelRepository;
import org.finos.legend.pure.m4.coreinstance.CoreInstance;
import org.finos.legend.pure.m4.serialization.grammar.antlr.AntlrSourceInformation;
import org.finos.legend.pure.m4.serialization.grammar.antlr.PureParserException;

public class NavigationGraphBuilder extends NavigationParserBaseVisitor<CoreInstance>
{
    private final ImportGroup importId;
    private final AntlrSourceInformation sourceInformation;
    private final ModelRepository repository;
    private final Context context;
    private final ProcessorSupport processorSupport;

    public NavigationGraphBuilder(ImportGroup importId, AntlrSourceInformation sourceInformation, ModelRepository repository, Context context)
    {
        this.importId = importId;
        this.sourceInformation = sourceInformation;
        this.repository = repository;
        this.context = context;
        this.processorSupport = new M3ProcessorSupport(context, repository);
    }

    @Override
    public CoreInstance visitDefinition(DefinitionContext ctx)
    {
        Token firstChar = ctx.SEPARATOR().getSymbol();
        Token end = ctx.EOF().getSymbol();

        GenericType owner = visitGenericTypeBlock(ctx.genericType());

        String name = ctx.name() != null ? ctx.name().VALID_STRING().getText() : "";

        MutableList<PathElement> props = (ctx.propertyWithParameters() == null) ? Lists.fixedSize.empty() : ListIterate.collect(ctx.propertyWithParameters(), this::visitPropertyWithParametersBlock);
        if (props.isEmpty())
        {
            throw new PureParserException(this.sourceInformation.getPureSourceInformation(firstChar, firstChar, end), "A path must contain at least one navigation");
        }
        Class<?> ppeType = (Class<?>) this.processorSupport.package_getByUserPath(M2PathPaths.Path);
        return ((Path<?, ?>) this.repository.newAnonymousCoreInstance(this.sourceInformation.getPureSourceInformation(firstChar, firstChar, end), ppeType, true))
                ._classifierGenericType(GenericTypeInstance.createPersistent(this.repository)._rawTypeCoreInstance(ppeType))
                ._start(owner)
                ._name(name)
                ._path(props);
    }

    private PathElement visitPropertyWithParametersBlock(PropertyWithParametersContext ctx)
    {
        Token property = ctx.VALID_STRING().getSymbol();

        Class<?> ppeType = (Class<?>) this.processorSupport.package_getByUserPath(M2PathPaths.PropertyPathElement);
        PropertyStub propStub = PropertyStubInstance.createPersistent(this.repository, this.sourceInformation.getPureSourceInformation(property), null, property.getText());
        PropertyPathElement propertyPathElement = ((PropertyPathElement) this.repository.newAnonymousCoreInstance(this.sourceInformation.getPureSourceInformation(property), ppeType, true))
                ._classifierGenericType(GenericTypeInstance.createPersistent(this.repository)._rawTypeCoreInstance(ppeType))
                ._propertyCoreInstance(propStub);

        if (ctx.parameter() != null)
        {
            MutableList<ValueSpecification> parameters = ListIterate.collect(ctx.parameter(), this::visitParameterBlock);
            if (parameters.notEmpty())
            {
                propertyPathElement._parameters(parameters);
            }
        }

        return propertyPathElement;
    }

    private ValueSpecification visitParameterBlock(ParameterContext ctx)
    {
        ListIterable<CoreInstance> rawValue = (ctx.scalar() != null) ? Lists.mutable.with(visitScalar(ctx.scalar())) : visitCollectionBlock(ctx.collection());
        return InstanceValueInstance.createPersistent(this.repository, null, null)._values(rawValue);
    }

    private ListIterable<CoreInstance> visitCollectionBlock(CollectionContext ctx)
    {
        return (ctx.scalar() == null) ? Lists.immutable.empty() : ListIterate.collect(ctx.scalar(), this::visitScalar);
    }

    @Override
    public CoreInstance visitScalar(ScalarContext ctx)
    {
        return (ctx.atomic() == null) ? visitEnumStub(ctx.enumStub()) : visitAtomic(ctx.atomic());
    }

    @Override
    public CoreInstance visitEnumStub(EnumStubContext ctx)
    {
        Token type = ctx.VALID_STRING(0).getSymbol();
        Token enumValue = ctx.VALID_STRING(1).getSymbol();
        ImportStub importStub = ImportStubInstance.createPersistent(this.repository, this.sourceInformation.getPureSourceInformation(type), type.getText(), this.importId);
        return EnumStubInstance.createPersistent(this.repository, this.sourceInformation.getPureSourceInformation(type), enumValue.getText(), importStub);

    }

    @Override
    public CoreInstance visitAtomic(AtomicContext ctx)
    {
        if (ctx.BOOLEAN() != null)
        {
            return this.repository.newBooleanCoreInstance(ctx.BOOLEAN().getText());
        }
        if (ctx.INTEGER() != null)
        {
            return this.repository.newIntegerCoreInstance(ctx.INTEGER().getText());
        }

        if (ctx.FLOAT() != null)
        {
            return this.repository.newFloatCoreInstance(ctx.FLOAT().getText());
        }
        if (ctx.STRING() != null)
        {
            String withQuote = StringEscapeUtils.unescapeJava(ctx.STRING().getText());
            return this.repository.newStringCoreInstance_cached(withQuote.substring(1, withQuote.length() - 1));
        }
        if (ctx.LATEST_DATE() != null)
        {
            return this.repository.newLatestDateCoreInstance();
        }
        if (ctx.DATE() != null)
        {
            return this.repository.newDateCoreInstance(ctx.DATE().getText());
        }
        throw new PureParserException(this.sourceInformation.getPureSourceInformation(ctx.getStart()), "Unknown atomic operation found");
    }

    private GenericType visitGenericTypeBlock(GenericTypeContext ctx)
    {
        return (GenericType) new M3AntlrParser(null).parseType(ctx.getText(), this.sourceInformation.getSourceName(), ctx.getStart().getLine() + this.sourceInformation.getOffsetLine(), ctx.getStart().getCharPositionInLine() + 1 + this.sourceInformation.getOffsetColumn(), this.importId, this.repository, this.context);
    }
}
