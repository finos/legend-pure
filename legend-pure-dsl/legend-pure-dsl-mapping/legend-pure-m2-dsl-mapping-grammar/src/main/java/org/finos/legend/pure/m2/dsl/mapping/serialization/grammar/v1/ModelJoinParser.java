// Copyright 2024 Goldman Sachs
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

package org.finos.legend.pure.m2.dsl.mapping.serialization.grammar.v1;

import org.eclipse.collections.api.RichIterable;
import org.eclipse.collections.api.factory.Lists;
import org.eclipse.collections.api.factory.Sets;
import org.eclipse.collections.api.list.ListIterable;
import org.eclipse.collections.api.list.MutableList;
import org.eclipse.collections.api.set.SetIterable;
import org.finos.legend.pure.m2.dsl.mapping.serialization.grammar.v1.processor.ModelJoinProcessor;
import org.finos.legend.pure.m2.dsl.mapping.serialization.grammar.v1.unloader.ModelJoinUnbind;
import org.finos.legend.pure.m2.dsl.mapping.serialization.grammar.v1.validator.ModelJoinValidator;
import org.finos.legend.pure.m3.compiler.Context;
import org.finos.legend.pure.m3.coreinstance.CoreInstanceFactoryRegistry;
import org.finos.legend.pure.m3.navigation.M3ProcessorSupport;
import org.finos.legend.pure.m3.navigation.M3Properties;
import org.finos.legend.pure.m3.serialization.grammar.Parser;
import org.finos.legend.pure.m3.serialization.grammar.ParserLibrary;
import org.finos.legend.pure.m3.serialization.grammar.m3parser.antlr.AntlrContextToM3CoreInstance;
import org.finos.legend.pure.m3.serialization.grammar.m3parser.antlr.M3AntlrParser;
import org.finos.legend.pure.m3.serialization.runtime.SourceState;
import org.finos.legend.pure.m3.serialization.runtime.binary.reference.ExternalReferenceSerializer;
import org.finos.legend.pure.m3.serialization.runtime.navigation.NavigationHandler;
import org.finos.legend.pure.m3.statelistener.M3M4StateListener;
import org.finos.legend.pure.m3.tools.matcher.MatchRunner;
import org.finos.legend.pure.m4.ModelRepository;
import org.finos.legend.pure.m4.coreinstance.CoreInstance;
import org.finos.legend.pure.m4.serialization.grammar.antlr.PureParserException;

public class ModelJoinParser implements IMappingParser
{
    @Override
    public String getName()
    {
        return "ModelJoin";
    }

    @Override
    public RichIterable<MatchRunner> getProcessors()
    {
        return Lists.immutable.with(new ModelJoinProcessor());
    }

    @Override
    public RichIterable<MatchRunner> getUnLoadWalkers()
    {
        return Lists.immutable.empty();
    }

    @Override
    public RichIterable<MatchRunner> getUnLoadUnbinders()
    {
        return Lists.immutable.with(new ModelJoinUnbind());
    }

    @Override
    public RichIterable<MatchRunner> getValidators()
    {
        return Lists.immutable.with(new ModelJoinValidator());
    }

    @Override
    public RichIterable<NavigationHandler> getNavigationHandlers()
    {
        return Lists.immutable.empty();
    }

    @Override
    public RichIterable<ExternalReferenceSerializer> getExternalReferenceSerializers()
    {
        return Lists.immutable.empty();
    }

    @Override
    public RichIterable<CoreInstanceFactoryRegistry> getCoreInstanceFactoriesRegistry()
    {
        return Lists.immutable.empty();
    }

    @Override
    public Parser newInstance(ParserLibrary library)
    {
        return new ModelJoinParser();
    }

    @Override
    public SetIterable<String> getRequiredParsers()
    {
        return Sets.immutable.with("Pure");
    }

    @Override
    public ListIterable<String> getRequiredFiles()
    {
        return Lists.immutable.with("/platform_dsl_mapping/grammar/mapping.pure");
    }

    public void parse(String string, String sourceName, boolean addLines, int offsetLine, ModelRepository repository, MutableList<CoreInstance> coreInstancesResult, M3M4StateListener listener, Context context, int count, SourceState oldState) throws PureParserException
    {
        throw new UnsupportedOperationException("Not Supported!");
    }

    @Override
    public String parseMapping(String content, String id, String extendsId, String setSourceInfo, boolean root, String classPath, String classSourceInfo, String mappingPath, String sourceName, int offsetLine, String importId, ModelRepository repository, Context context) throws PureParserException
    {
        M3AntlrParser parser = new M3AntlrParser();
        M3ProcessorSupport processorSupport = new M3ProcessorSupport(context, repository);
        String mappingName = mappingPath.replace("::", "_");
        String classMappingName = classPath.replace("::", "_");
        AntlrContextToM3CoreInstance.LambdaContext lambdaContext = new AntlrContextToM3CoreInstance.LambdaContext(mappingName + '_' + classMappingName + (id == null ? "" : '_' + id));

        CoreInstance parsed = parser.parseCombinedExpression(
                content.trim(), lambdaContext, sourceName, offsetLine, importId, repository, processorSupport, context);
        // The M3 parser wraps the typed lambda in an InstanceValue via doWrap; extract the lambda.
        ListIterable<? extends CoreInstance> wrapped = parsed == null ? null : parsed.getValueForMetaPropertyToMany(M3Properties.values);
        CoreInstance typedLambda = (wrapped != null && wrapped.size() == 1) ? wrapped.get(0) : null;
        if (typedLambda == null)
        {
            throw new PureParserException(null, "ModelJoin mapping body must be a typed lambda expression, e.g. {a:T1[1], b:T2[1]|<boolean expression>}");
        }

        String joinConditionM4 = M3AntlrParser.process(typedLambda, processorSupport);

        // Use the lambda's parameter names as the property names. The user MUST name lambda
        // params after the association's property names — this is validated by the processor
        // when the association is resolved.
        String[] paramNames = extractParamNames(typedLambda);
        String prop0Name = paramNames[0];
        String prop1Name = paramNames[1];

        return "^meta::pure::mapping::modelJoin::ModelJoinAssociationImplementation" + setSourceInfo + "(" +
                ((id == null) ? "" : ("id = '" + id + "',")) +
                "association = ^meta::pure::metamodel::import::ImportStub " + classSourceInfo + " (importGroup=system::imports::" + importId + ", idOrPath='" + classPath + "')," +
                "parent = ^meta::pure::metamodel::import::ImportStub (importGroup=system::imports::" + importId + ", idOrPath='" + mappingPath + "')," +
                "propertyMappings = [" +
                "^meta::pure::mapping::modelJoin::ModelJoinPropertyMapping " + setSourceInfo + " (" +
                "property='" + prop0Name + "'," +
                "sourceSetImplementationId='" + prop0Name + "'," +
                "targetSetImplementationId='" + prop1Name + "'," +
                "joinCondition=" + joinConditionM4 +
                ")," +
                "^meta::pure::mapping::modelJoin::ModelJoinPropertyMapping " + setSourceInfo + " (" +
                "property='" + prop1Name + "'," +
                "sourceSetImplementationId='" + prop1Name + "'," +
                "targetSetImplementationId='" + prop0Name + "'," +
                "joinCondition=" + joinConditionM4 +
                ")" +
                "])";
    }

    /**
     * Extract the two parameter names from the typed lambda. Same-name parameters are permitted:
     * when the association declares two properties with the same name (distinguished by type).
     */
    private static String[] extractParamNames(CoreInstance typedLambda)
    {
        CoreInstance functionType = typedLambda
                .getValueForMetaPropertyToOne(M3Properties.classifierGenericType)
                .getValueForMetaPropertyToMany(M3Properties.typeArguments).get(0)
                .getValueForMetaPropertyToOne(M3Properties.rawType);
        ListIterable<? extends CoreInstance> params = functionType.getValueForMetaPropertyToMany(M3Properties.parameters);
        if (params.size() != 2)
        {
            throw new PureParserException(null, "ModelJoin: typed lambda must have exactly 2 parameters, found " + params.size());
        }
        return new String[]{
                params.get(0).getValueForMetaPropertyToOne(M3Properties.name).getName(),
                params.get(1).getValueForMetaPropertyToOne(M3Properties.name).getName()
        };
    }
}