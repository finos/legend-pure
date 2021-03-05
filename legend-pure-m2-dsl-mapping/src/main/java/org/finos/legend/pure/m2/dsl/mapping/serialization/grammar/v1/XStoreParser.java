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

package org.finos.legend.pure.m2.dsl.mapping.serialization.grammar.v1;

import org.eclipse.collections.api.RichIterable;
import org.eclipse.collections.api.block.function.Function;
import org.eclipse.collections.api.list.ListIterable;
import org.eclipse.collections.api.list.MutableList;
import org.eclipse.collections.api.set.SetIterable;
import org.eclipse.collections.impl.factory.Lists;
import org.eclipse.collections.impl.factory.Sets;
import org.finos.legend.pure.m2.dsl.mapping.serialization.grammar.v1.processor.XStoreProcessor;
import org.finos.legend.pure.m2.dsl.mapping.serialization.grammar.v1.unloader.XStoreUnbind;
import org.finos.legend.pure.m2.dsl.mapping.serialization.grammar.v1.validator.XStoreValidator;
import org.finos.legend.pure.m3.compiler.Context;
import org.finos.legend.pure.m3.coreinstance.CoreInstanceFactoryRegistry;
import org.finos.legend.pure.m3.navigation.M3ProcessorSupport;
import org.finos.legend.pure.m3.serialization.grammar.Parser;
import org.finos.legend.pure.m3.serialization.grammar.ParserLibrary;
import org.finos.legend.pure.m4.serialization.grammar.antlr.PureParserException;
import org.finos.legend.pure.m3.serialization.grammar.m3parser.antlr.AntlrContextToM3CoreInstance;
import org.finos.legend.pure.m3.serialization.grammar.m3parser.antlr.M3AntlrParser;
import org.finos.legend.pure.m3.serialization.grammar.m3parser.antlr.TemporaryPurePropertyMapping;
import org.finos.legend.pure.m3.serialization.grammar.m3parser.antlr.TemporaryPureSetImplementation;
import org.finos.legend.pure.m3.serialization.runtime.SourceState;
import org.finos.legend.pure.m3.serialization.runtime.binary.reference.ExternalReferenceSerializer;
import org.finos.legend.pure.m3.serialization.runtime.navigation.NavigationHandler;
import org.finos.legend.pure.m3.statelistener.M3M4StateListener;
import org.finos.legend.pure.m3.tools.matcher.MatchRunner;
import org.finos.legend.pure.m4.coreinstance.CoreInstance;
import org.finos.legend.pure.m4.ModelRepository;

public class XStoreParser implements Parser
{
    @Override
    public String getName()
    {
        return "XStore";
    }

    @Override
    public RichIterable<MatchRunner> getProcessors()
    {
        return Lists.immutable.<MatchRunner>with(new XStoreProcessor());
    }

    @Override
    public RichIterable<MatchRunner> getUnLoadWalkers()
    {
        return Lists.immutable.<MatchRunner>with();
    }

    @Override
    public RichIterable<MatchRunner> getUnLoadUnbinders()
    {
        return Lists.immutable.<MatchRunner>with(new XStoreUnbind());
    }

    @Override
    public RichIterable<MatchRunner> getValidators()
    {
        return Lists.immutable.<MatchRunner>with(new XStoreValidator());
    }

    @Override
    public RichIterable<NavigationHandler> getNavigationHandlers()
    {
        return Lists.immutable.empty();
    }

    @Override
    public RichIterable<ExternalReferenceSerializer> getExternalReferenceSerializers()
    {
        return Lists.immutable.<ExternalReferenceSerializer>with();
    }

    @Override
    public RichIterable<CoreInstanceFactoryRegistry> getCoreInstanceFactoriesRegistry()
    {
        return Lists.immutable.empty();
    }

    @Override
    public Parser newInstance(ParserLibrary library)
    {
        return new XStoreParser();
    }

    @Override
    public SetIterable<String> getRequiredParsers()
    {
        return Sets.immutable.with("Pure");
    }

    @Override
    public ListIterable<String> getRequiredFiles()
    {
        return Lists.immutable.with("/platform/pure/mapping.pure");
    }

    public void parse(String string, String sourceName, boolean addLines, int offsetLine, ModelRepository repository, MutableList<CoreInstance> coreInstancesResult, M3M4StateListener listener, Context context, int count, SourceState oldState) throws PureParserException
    {
        throw new RuntimeException("Not Supported!");
    }

    public String parseMapping(String content, String id ,String extendsId, String setSourceInfo, boolean root, String classPath, String classSourceInfo, String mappingPath, String sourceName, int offsetLine, String importId, ModelRepository repository, final Context context) throws PureParserException
    {
        final M3AntlrParser parser = new M3AntlrParser();
        final M3ProcessorSupport processorSupport = new M3ProcessorSupport(context, repository);
        String mappingName = mappingPath.replace("::","_");
        String classMappingName = classPath.replace("::","_");
        final AntlrContextToM3CoreInstance.LambdaContext lambdaContext = new AntlrContextToM3CoreInstance.LambdaContext(mappingName + '_' + classMappingName + (id == null ? "" : '_' + id));
        TemporaryPureSetImplementation arg = parser.parseMappingInfo(content, classPath, lambdaContext, sourceName, offsetLine, importId, repository, processorSupport, context);
        return "^meta::pure::mapping::xStore::XStoreAssociationImplementation" + setSourceInfo + "(" +
                ((id == null) ? "" : ("id = '" + id + "',")) +
                "association = ^meta::pure::metamodel::import::ImportStub " + classSourceInfo + " (importGroup=system::imports::" + importId + ", idOrPath='" + classPath + "')," +
                "parent = ^meta::pure::metamodel::import::ImportStub (importGroup=system::imports::" + importId + ", idOrPath='" + mappingPath + "')," +
                "propertyMappings = [" + arg.propertyMappings.collect(new Function<TemporaryPurePropertyMapping, Object>()
        {
            @Override
            public Object valueOf(TemporaryPurePropertyMapping propertyStubCoreInstancePair)
            {
                return "^meta::pure::mapping::xStore::XStorePropertyMapping " + propertyStubCoreInstancePair.sourceInformation.toM4String() + " (property='" + propertyStubCoreInstancePair.property + "'," +
                        (propertyStubCoreInstancePair.sourceMappingId == null ? "" : "                                         sourceSetImplementationId='" + propertyStubCoreInstancePair.sourceMappingId + "',") +
                        (propertyStubCoreInstancePair.targetMappingId == null ? "" : "                                         targetSetImplementationId='" + propertyStubCoreInstancePair.targetMappingId + "',") +
                        "                                         crossExpression=^meta::pure::metamodel::function::LambdaFunction "+lambdaContext.getLambdaFunctionUniqueName() +' '+ propertyStubCoreInstancePair.expression.getSourceInformation().toM4String() + " (" +
                        "                                                           classifierGenericType=^meta::pure::metamodel::type::generics::GenericType(rawType=meta::pure::metamodel::function::LambdaFunction, typeArguments=^meta::pure::metamodel::type::generics::GenericType(rawType = ^meta::pure::metamodel::type::FunctionType()))," +
                        "                                                           expressionSequence=" + parser.process(propertyStubCoreInstancePair.expression, context, processorSupport) + ")" +
                        "                                                                   )\n";
            }
        }).makeString(",") +
                "])";
    }
}
