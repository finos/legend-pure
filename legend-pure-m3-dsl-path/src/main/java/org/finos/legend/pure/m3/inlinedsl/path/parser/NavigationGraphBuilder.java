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

package org.finos.legend.pure.m3.inlinedsl.path.parser;

import org.eclipse.collections.api.list.ListIterable;
import org.eclipse.collections.api.list.MutableList;
import org.eclipse.collections.impl.list.mutable.FastList;
import org.finos.legend.pure.m3.inlinedsl.path.serialization.grammar.NavigationParser.AtomicContext;
import org.finos.legend.pure.m3.inlinedsl.path.serialization.grammar.NavigationParser.CollectionContext;
import org.finos.legend.pure.m3.inlinedsl.path.serialization.grammar.NavigationParser.DefinitionContext;
import org.finos.legend.pure.m3.inlinedsl.path.serialization.grammar.NavigationParser.EnumStubContext;
import org.finos.legend.pure.m3.inlinedsl.path.serialization.grammar.NavigationParser.GenericTypeContext;
import org.finos.legend.pure.m3.inlinedsl.path.serialization.grammar.NavigationParser.ParameterContext;
import org.finos.legend.pure.m3.inlinedsl.path.serialization.grammar.NavigationParser.PropertyWithParametersContext;
import org.finos.legend.pure.m3.inlinedsl.path.serialization.grammar.NavigationParser.ScalarContext;
import org.finos.legend.pure.m3.inlinedsl.path.serialization.grammar.NavigationParserBaseVisitor;
import org.finos.legend.pure.m3.navigation.M3Paths;
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
import org.finos.legend.pure.m3.coreinstance.meta.pure.metamodel.type.ClassInstance;
import org.finos.legend.pure.m3.coreinstance.meta.pure.metamodel.type.generics.GenericType;
import org.finos.legend.pure.m3.coreinstance.meta.pure.metamodel.type.generics.GenericTypeInstance;
import org.finos.legend.pure.m3.coreinstance.meta.pure.metamodel.valuespecification.InstanceValueInstance;
import org.finos.legend.pure.m3.coreinstance.meta.pure.metamodel.valuespecification.ValueSpecification;
import org.finos.legend.pure.m3.navigation.M3ProcessorSupport;
import org.finos.legend.pure.m3.navigation.ProcessorSupport;
import org.finos.legend.pure.m3.serialization.grammar.m3parser.antlr.M3AntlrParser;
import org.finos.legend.pure.m4.coreinstance.CoreInstance;
import org.finos.legend.pure.m4.ModelRepository;
import org.finos.legend.pure.m4.serialization.grammar.antlr.AntlrSourceInformation;
import org.finos.legend.pure.m4.serialization.grammar.antlr.PureParserException;
import org.antlr.v4.runtime.Token;
import org.apache.commons.lang3.StringEscapeUtils;


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
        GenericType owner;
        MutableList<PathElement> props = FastList.newList();

        Token firstChar = ctx.SEPARATOR().getSymbol();
        owner = visitGenericTypeBlock(ctx.genericType());

        String name = ctx.name() != null ? ctx.name().VALID_STRING().getText() : "";

        if (ctx.propertyWithParameters() != null)
        {
            for (PropertyWithParametersContext propertyWithParametersContext : ctx.propertyWithParameters())
            {
                visitPropertyWithParametersBlock(propertyWithParametersContext, props, firstChar);
            }
        }

        Token end = ctx.EOF().getSymbol();
        if (props.isEmpty())
        {
            throw new PureParserException(this.sourceInformation.getPureSourceInformation(firstChar, firstChar, end), "A path must contain at least one navigation");
        }
        ClassInstance ppeType = (ClassInstance)this.processorSupport.package_getByUserPath(M3Paths.Path);
        Path propertyPath = (Path)this.repository.newAnonymousCoreInstance(this.sourceInformation.getPureSourceInformation(firstChar, firstChar, end), ppeType, true);
        GenericType classifierGT = GenericTypeInstance.createPersistent(this.repository);
        classifierGT._rawTypeCoreInstance(ppeType);
        propertyPath._classifierGenericType(classifierGT);
        propertyPath._start(owner);
        propertyPath._name(name);
        propertyPath._path(props);
        return propertyPath;
    }

    private void visitPropertyWithParametersBlock(PropertyWithParametersContext ctx, MutableList<PathElement> props, Token firstChar)
    {
        MutableList<ValueSpecification> parameters = FastList.newList();
        Token property = ctx.VALID_STRING().getSymbol();

        ClassInstance ppeType = (ClassInstance)this.processorSupport.package_getByUserPath(M3Paths.PropertyPathElement);
        PropertyPathElement propertyPathElement = (PropertyPathElement)this.repository.newAnonymousCoreInstance(this.sourceInformation.getPureSourceInformation(property), ppeType, true);
        GenericType classifierGT = GenericTypeInstance.createPersistent(this.repository);
        classifierGT._rawTypeCoreInstance(ppeType);
        propertyPathElement._classifierGenericType(classifierGT);

        PropertyStub propStub = PropertyStubInstance.createPersistent(this.repository, this.sourceInformation.getPureSourceInformation(property), null, property.getText());
        propertyPathElement._propertyCoreInstance(propStub);

        if (ctx.parameter() != null)
        {
            for (ParameterContext parameterContext : ctx.parameter())
            {
                parameters.add(visitParameterBlock(parameterContext));
            }
        }
        if (!parameters.isEmpty())
        {
            propertyPathElement._parameters(parameters);
        }

        props.add(propertyPathElement);
    }

    private ValueSpecification visitParameterBlock(ParameterContext ctx)
    {
        ListIterable<CoreInstance> rawValue;
        if (ctx.scalar() != null)
        {
            MutableList<CoreInstance> elements = FastList.newList();
            CoreInstance result = visitScalar(ctx.scalar());
            elements.add(result);
            rawValue = elements;
        }
        else
        {
            rawValue = visitCollectionBlock(ctx.collection());
        }
        InstanceValueInstance instanceVal = InstanceValueInstance.createPersistent(this.repository, null, null);
        instanceVal._values(rawValue);
        return instanceVal;
    }

    private ListIterable<CoreInstance> visitCollectionBlock(CollectionContext ctx)
    {
        MutableList<CoreInstance> elements = FastList.newList();
        if (ctx.scalar() != null)
        {
            for (ScalarContext scalarContext : ctx.scalar())
            {
                elements.add(visitScalar(scalarContext));
            }
        }
        return elements;
    }

    @Override
    public CoreInstance visitScalar(ScalarContext ctx)
    {
        return ctx.atomic() != null ? visitAtomic(ctx.atomic()) : visitEnumStub(ctx.enumStub());
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
        return (GenericType)new M3AntlrParser(null).parseType(ctx.getText(), this.sourceInformation.getSourceName(), ctx.getStart().getLine() + this.sourceInformation.getOffsetLine(), ctx.getStart().getCharPositionInLine() + 1 + this.sourceInformation.getOffsetColumn(), importId, repository, context);
    }

}
