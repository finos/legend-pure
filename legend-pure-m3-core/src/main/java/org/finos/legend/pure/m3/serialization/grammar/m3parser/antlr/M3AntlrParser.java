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

package org.finos.legend.pure.m3.serialization.grammar.m3parser.antlr;

import org.antlr.v4.runtime.ANTLRInputStream;
import org.antlr.v4.runtime.CommonTokenStream;
import org.antlr.v4.runtime.RecognitionException;
import org.antlr.v4.runtime.atn.PredictionMode;
import org.eclipse.collections.api.RichIterable;
import org.eclipse.collections.api.factory.Lists;
import org.eclipse.collections.api.factory.Sets;
import org.eclipse.collections.api.list.ListIterable;
import org.eclipse.collections.api.list.MutableList;
import org.eclipse.collections.api.set.SetIterable;
import org.finos.legend.pure.m3.compiler.Context;
import org.finos.legend.pure.m3.compiler.postprocessing.processor.AnnotatedElementProcessor;
import org.finos.legend.pure.m3.compiler.postprocessing.processor.AssociationProcessor;
import org.finos.legend.pure.m3.compiler.postprocessing.processor.ClassProcessor;
import org.finos.legend.pure.m3.compiler.postprocessing.processor.ElementWithConstraintsProcessor;
import org.finos.legend.pure.m3.compiler.postprocessing.processor.EnumerationProcessor;
import org.finos.legend.pure.m3.compiler.postprocessing.processor.FunctionDefinitionProcessor;
import org.finos.legend.pure.m3.compiler.postprocessing.processor.LambdaFunctionProcessor;
import org.finos.legend.pure.m3.compiler.postprocessing.processor.PropertyOwnerProcessor;
import org.finos.legend.pure.m3.compiler.postprocessing.processor.PropertyProcessor;
import org.finos.legend.pure.m3.compiler.postprocessing.processor.UnitProcessor;
import org.finos.legend.pure.m3.compiler.postprocessing.processor.projection.ClassProjectionProcessor;
import org.finos.legend.pure.m3.compiler.postprocessing.processor.projection.ClassProjectionUnloaderWalk;
import org.finos.legend.pure.m3.compiler.postprocessing.processor.projection.ClassProjectionValidator;
import org.finos.legend.pure.m3.compiler.postprocessing.processor.valuespecification.FunctionExpressionProcessor;
import org.finos.legend.pure.m3.compiler.postprocessing.processor.valuespecification.InstanceValueProcessor;
import org.finos.legend.pure.m3.compiler.postprocessing.processor.valuespecification.KeyExpressionProcessor;
import org.finos.legend.pure.m3.compiler.postprocessing.processor.valuespecification.VariableExpressionProcessor;
import org.finos.legend.pure.m3.compiler.unload.unbind.AbstractPropertyUnbind;
import org.finos.legend.pure.m3.compiler.unload.unbind.AnyUnbind;
import org.finos.legend.pure.m3.compiler.unload.unbind.AssociationUnbind;
import org.finos.legend.pure.m3.compiler.unload.unbind.ClassProjectionUnbind;
import org.finos.legend.pure.m3.compiler.unload.unbind.ClassUnbind;
import org.finos.legend.pure.m3.compiler.unload.unbind.ConcreteFunctionDefinitionUnbind;
import org.finos.legend.pure.m3.compiler.unload.unbind.ElementWithConstraintsUnbind;
import org.finos.legend.pure.m3.compiler.unload.unbind.ElementWithStereotypesUnbind;
import org.finos.legend.pure.m3.compiler.unload.unbind.ElementWithTaggedValuesUnbind;
import org.finos.legend.pure.m3.compiler.unload.unbind.EnumerationUnbind;
import org.finos.legend.pure.m3.compiler.unload.unbind.FunctionDefinitionUnbind;
import org.finos.legend.pure.m3.compiler.unload.unbind.InstanceValueUnbind;
import org.finos.legend.pure.m3.compiler.unload.unbind.LambdaFunctionUnbind;
import org.finos.legend.pure.m3.compiler.unload.unbind.SimpleFunctionExpressionUnbind;
import org.finos.legend.pure.m3.compiler.unload.unbind.TypeUnbind;
import org.finos.legend.pure.m3.compiler.unload.unbind.UnitUnbind;
import org.finos.legend.pure.m3.compiler.unload.unbind.ValueSpecificationUnbind;
import org.finos.legend.pure.m3.compiler.unload.walk.AbstractPropertyUnloaderWalk;
import org.finos.legend.pure.m3.compiler.unload.walk.AssociationUnloaderWalk;
import org.finos.legend.pure.m3.compiler.unload.walk.ClassConstraintValueSpecificationContextUnloaderWalk;
import org.finos.legend.pure.m3.compiler.unload.walk.ClassUnloaderWalk;
import org.finos.legend.pure.m3.compiler.unload.walk.ConcreteFunctionDefinitionUnloaderWalk;
import org.finos.legend.pure.m3.compiler.unload.walk.EnumUnloaderWalk;
import org.finos.legend.pure.m3.compiler.unload.walk.EnumerationUnloaderWalk;
import org.finos.legend.pure.m3.compiler.unload.walk.ExpressionSequenceValueSpecificationContextUnloaderWalk;
import org.finos.legend.pure.m3.compiler.unload.walk.FunctionTypeUnloaderWalk;
import org.finos.legend.pure.m3.compiler.unload.walk.FunctionUnloaderWalk;
import org.finos.legend.pure.m3.compiler.unload.walk.GeneralizationUnloaderWalk;
import org.finos.legend.pure.m3.compiler.unload.walk.GenericTypeUnloaderWalk;
import org.finos.legend.pure.m3.compiler.unload.walk.InstanceValueSpecificationContextUnloaderWalk;
import org.finos.legend.pure.m3.compiler.unload.walk.KeyValueValueSpecificationContextUnloaderWalk;
import org.finos.legend.pure.m3.compiler.unload.walk.MeasureUnloaderWalk;
import org.finos.legend.pure.m3.compiler.unload.walk.NativeFunctionUnloaderWalk;
import org.finos.legend.pure.m3.compiler.unload.walk.PackageableElementUnloaderWalk;
import org.finos.legend.pure.m3.compiler.unload.walk.ParameterValueSpecificationContextUnloaderWalk;
import org.finos.legend.pure.m3.compiler.unload.walk.ProfileUnloaderWalk;
import org.finos.legend.pure.m3.compiler.unload.walk.PropertyUnloaderWalk;
import org.finos.legend.pure.m3.compiler.unload.walk.TypeUnloaderWalk;
import org.finos.legend.pure.m3.compiler.unload.walk.UnitUnloaderWalk;
import org.finos.legend.pure.m3.compiler.unload.walk.ValueSpecificationUnloaderWalk;
import org.finos.legend.pure.m3.compiler.unload.walk.VariableExpressionUnloaderWalk;
import org.finos.legend.pure.m3.compiler.validation.validator.AbstractPropertyValidator;
import org.finos.legend.pure.m3.compiler.validation.validator.AccessLevelValidator;
import org.finos.legend.pure.m3.compiler.validation.validator.AssociationValidator;
import org.finos.legend.pure.m3.compiler.validation.validator.ClassValidator;
import org.finos.legend.pure.m3.compiler.validation.validator.ElementWithConstraintsValidator;
import org.finos.legend.pure.m3.compiler.validation.validator.ElementWithStereotypesValidator;
import org.finos.legend.pure.m3.compiler.validation.validator.ElementWithTaggedValueValidator;
import org.finos.legend.pure.m3.compiler.validation.validator.EnumerationValidator;
import org.finos.legend.pure.m3.compiler.validation.validator.FunctionDefinitionValidator;
import org.finos.legend.pure.m3.compiler.validation.validator.FunctionExpressionValidator;
import org.finos.legend.pure.m3.compiler.validation.validator.GenericTypeValidator;
import org.finos.legend.pure.m3.compiler.validation.validator.InstanceValueValidator;
import org.finos.legend.pure.m3.compiler.validation.validator.PackageValidator;
import org.finos.legend.pure.m3.compiler.validation.validator.PropertyValidator;
import org.finos.legend.pure.m3.compiler.validation.validator.RepositoryPackageValidator;
import org.finos.legend.pure.m3.compiler.validation.validator.TaggedValueValidator;
import org.finos.legend.pure.m3.compiler.validation.validator.TypeValidator;
import org.finos.legend.pure.m3.compiler.validation.validator.ValueSpecificationValidator;
import org.finos.legend.pure.m3.coreinstance.CoreInstanceFactoryRegistry;
import org.finos.legend.pure.m3.coreinstance.M3CoreInstanceFactoryRegistry;
import org.finos.legend.pure.m3.coreinstance.meta.pure.metamodel._import.ImportGroup;
import org.finos.legend.pure.m3.coreinstance.meta.pure.metamodel._import.ImportStub;
import org.finos.legend.pure.m3.coreinstance.meta.pure.metamodel.function.property.Property;
import org.finos.legend.pure.m3.coreinstance.meta.pure.metamodel.function.property.QualifiedProperty;
import org.finos.legend.pure.m3.navigation.Instance;
import org.finos.legend.pure.m3.navigation.M3Paths;
import org.finos.legend.pure.m3.navigation.M3ProcessorSupport;
import org.finos.legend.pure.m3.navigation.PackageableElement.PackageableElement;
import org.finos.legend.pure.m3.navigation.ProcessorSupport;
import org.finos.legend.pure.m3.serialization.grammar.Parser;
import org.finos.legend.pure.m3.serialization.grammar.ParserLibrary;
import org.finos.legend.pure.m3.serialization.grammar.m3parser.inlinedsl.InlineDSLLibrary;
import org.finos.legend.pure.m3.serialization.grammar.m3parser.inlinedsl.treepath.NewPropertyRouteNodeFunctionDefinitionUnloaderWalk;
import org.finos.legend.pure.m3.serialization.grammar.m3parser.inlinedsl.treepath.PropertyRouteNodeUnloaderWalk;
import org.finos.legend.pure.m3.serialization.grammar.m3parser.inlinedsl.treepath.RootRouteNodePostProcessor;
import org.finos.legend.pure.m3.serialization.grammar.m3parser.inlinedsl.treepath.RootRouteNodeUnbind;
import org.finos.legend.pure.m3.serialization.grammar.m3parser.inlinedsl.treepath.RootRouteNodeUnloaderWalk;
import org.finos.legend.pure.m3.serialization.grammar.m3parser.inlinedsl.treepath.RootRouteNodeValidator;
import org.finos.legend.pure.m3.serialization.grammar.m3parser.inlinedsl.treepath.RouteNodePropertyStubUnloaderWalk;
import org.finos.legend.pure.m3.serialization.runtime.SourceState;
import org.finos.legend.pure.m3.serialization.runtime.binary.reference.EnumReferenceSerializer;
import org.finos.legend.pure.m3.serialization.runtime.binary.reference.ExternalReferenceSerializer;
import org.finos.legend.pure.m3.serialization.runtime.binary.reference.PropertyReferenceSerializer;
import org.finos.legend.pure.m3.serialization.runtime.binary.reference.QualifiedPropertyReferenceSerializer;
import org.finos.legend.pure.m3.serialization.runtime.binary.reference.StereotypeReferenceSerializer;
import org.finos.legend.pure.m3.serialization.runtime.binary.reference.TagReferenceSerializer;
import org.finos.legend.pure.m3.serialization.runtime.navigation.NavigationHandler;
import org.finos.legend.pure.m3.statelistener.M3M4StateListener;
import org.finos.legend.pure.m3.tools.matcher.MatchRunner;
import org.finos.legend.pure.m4.ModelRepository;
import org.finos.legend.pure.m4.coreinstance.CoreInstance;
import org.finos.legend.pure.m4.serialization.grammar.antlr.AntlrDescriptiveErrorListener;
import org.finos.legend.pure.m4.serialization.grammar.antlr.AntlrSourceInformation;
import org.finos.legend.pure.m4.serialization.grammar.antlr.PureAntlrErrorStrategy;
import org.finos.legend.pure.m4.serialization.grammar.antlr.PureParserException;

public class M3AntlrParser implements Parser
{

    private int offsetLine;
    private final InlineDSLLibrary inlineDSLLibrary;
    private final boolean useImportStubsInInstanceParser;


    public M3AntlrParser()
    {
        this(true, null);
    }

    public M3AntlrParser(boolean useImportStubsInInstanceParser)
    {
        this(useImportStubsInInstanceParser, null);
    }

    public M3AntlrParser(InlineDSLLibrary inlineDSLLibrary)
    {
        this(true, inlineDSLLibrary);
    }

    public M3AntlrParser(boolean useImportStubsInInstanceParser, InlineDSLLibrary inlineDSLLibrary)
    {
        this.useImportStubsInInstanceParser = useImportStubsInInstanceParser;
        this.inlineDSLLibrary = inlineDSLLibrary;
    }

    @Override
    public String getName()
    {
        return "Pure";
    }

    @Override
    public void parse(String code, String sourceName, boolean addLines, int offset, ModelRepository repository, MutableList<CoreInstance> coreInstancesResult, M3M4StateListener listener, Context context, int count, SourceState oldState) throws PureParserException
    {
        this.offsetLine = offset;
        listener.startParsingM3(code);
        this.parseDefinition(true, code, sourceName, repository, coreInstancesResult, listener, context, count, addLines, oldState);
        listener.finishedParsingM3(code);
    }

    private void parseDefinition(boolean useFastParser, String code, String sourceName, ModelRepository repository, MutableList<CoreInstance> coreInstancesResult, M3M4StateListener listener, Context context, int count, boolean addLines, SourceState oldState)
    {
        try
        {
            AntlrSourceInformation sourceInformation = new AntlrSourceInformation(this.offsetLine, 0, sourceName, addLines);
            org.finos.legend.pure.m3.serialization.grammar.m3parser.antlr.M3Parser parser = initAntlrParser(useFastParser, code, sourceInformation);
            final M3AntlrTreeWalker visitor = new M3AntlrTreeWalker(sourceInformation, this.inlineDSLLibrary, repository, coreInstancesResult, listener, context, null, count, this.useImportStubsInInstanceParser, addLines, oldState);
            visitor.visit(parser.definition());
        }
        catch (Exception e)
        {
            if (isAntlrRecognitionExceptionUsingFastParser(useFastParser, e))
            {
                //System.err.println("Error using fast Antlr Parser: " + ExceptionUtils.getStackTrace(e));
                this.parseDefinition(false, code, sourceName, repository, coreInstancesResult, listener, context, count, addLines, oldState);
            }
            else
            {
                throw e;
            }
        }
    }

    public CoreInstance parseType(String code, String fileName, int offsetLine, int offsetColumn, ImportGroup importId, ModelRepository repository, Context context) throws PureParserException
    {
        return this.parseType(true, code, fileName, offsetLine, offsetColumn, importId, repository, context);
    }

    public CoreInstance parseType(boolean useFastParser, String code, String fileName, int offsetLine, int offsetColumn, ImportGroup importId, ModelRepository repository, Context context) throws PureParserException
    {
        try
        {
            AntlrSourceInformation sourceInformation = new AntlrSourceInformation(offsetLine - 1, offsetColumn - 1, fileName, true);
            org.finos.legend.pure.m3.serialization.grammar.m3parser.antlr.M3Parser parser = initAntlrParser(useFastParser, code, sourceInformation);
            M3AntlrTreeWalker visitor = new M3AntlrTreeWalker(sourceInformation, this.inlineDSLLibrary, repository, null, null, context, importId, 0, null);
            return visitor.visit(parser.type());
        }
        catch (Exception e)
        {
            if (isAntlrRecognitionExceptionUsingFastParser(useFastParser, e))
            {
                //System.err.println("Error using fast Antlr Parser: " + ExceptionUtils.getStackTrace(e));
                return this.parseType(false, code, fileName, offsetLine, offsetColumn, importId, repository, context);
            }
            else
            {
                throw e;
            }
        }
    }

    public CoreInstance parseInstance(boolean useFastParser, String code, String fileName, int offsetLine, int offsetColumn, ImportGroup importId, ModelRepository repository, Context context) throws PureParserException
    {
        try
        {
            AntlrSourceInformation sourceInformation = new AntlrSourceInformation(offsetLine - 1, offsetColumn - 1, fileName, true);
            org.finos.legend.pure.m3.serialization.grammar.m3parser.antlr.M3Parser parser = initAntlrParser(useFastParser, code, sourceInformation);
            M3AntlrTreeWalker visitor = new M3AntlrTreeWalker(sourceInformation, this.inlineDSLLibrary, repository, null, null, context, importId, 0, null);
            return visitor.visit(parser.instance());
        }
        catch (Exception e)
        {
            if (isAntlrRecognitionExceptionUsingFastParser(useFastParser, e))
            {
                //System.err.println("Error using fast Antlr Parser: " + ExceptionUtils.getStackTrace(e));
                return this.parseInstance(false, code, fileName, offsetLine, offsetColumn, importId, repository, context);
            }
            else
            {
                throw e;
            }
        }
    }

    public void parseProperties(String code, String fileName, MutableList<Property<? extends CoreInstance, ?>> properties, MutableList<QualifiedProperty<? extends CoreInstance>> qualifiedProperties, ImportStub typeOwner, ImportGroup importId, boolean addLines, ModelRepository repository, Context context, int startingQualifiedPropertyIndex) throws PureParserException
    {
        this.parseProperties(true, code, fileName, properties, qualifiedProperties, typeOwner, importId, addLines, repository, context, startingQualifiedPropertyIndex);
    }

    private void parseProperties(boolean useFastParser, String code, String fileName, MutableList<Property<? extends CoreInstance, ?>> properties, MutableList<QualifiedProperty<? extends CoreInstance>> qualifiedProperties, ImportStub typeOwner, ImportGroup importId, boolean addLines, ModelRepository repository, Context context, int startingQualifiedPropertyIndex) throws PureParserException
    {
        try
        {
            AntlrSourceInformation sourceInformation = new AntlrSourceInformation(this.offsetLine, 0, fileName, addLines);
            org.finos.legend.pure.m3.serialization.grammar.m3parser.antlr.M3Parser parser = initAntlrParser(useFastParser, code, sourceInformation);
            M3AntlrPropertiesWalker visitor = new M3AntlrPropertiesWalker(sourceInformation, this.inlineDSLLibrary, repository, context, importId, properties, qualifiedProperties, typeOwner, startingQualifiedPropertyIndex);
            visitor.visit(parser.properties());
        }
        catch (Exception e)
        {
            if (isAntlrRecognitionExceptionUsingFastParser(useFastParser, e))
            {
                //System.err.println("Error using fast Antlr Parser: " + ExceptionUtils.getStackTrace(e));
                this.parseProperties(false, code, fileName, properties, qualifiedProperties, typeOwner, importId, addLines, repository, context, startingQualifiedPropertyIndex);
            }
            else
            {
                throw e;
            }
        }
    }

    private CoreInstance parseTreePath(boolean useFastParser, String code, String fileName, int offsetLine, int offsetColumn, ImportGroup importId, ModelRepository repository, Context context) throws PureParserException
    {
        try
        {
            AntlrSourceInformation sourceInformation = new AntlrSourceInformation(offsetLine - 1, offsetColumn - 1, fileName, true);
            org.finos.legend.pure.m3.serialization.grammar.m3parser.antlr.M3Parser parser = initAntlrParser(useFastParser, code, sourceInformation);
            M3AntlrTreeWalker visitor = new M3AntlrTreeWalker(sourceInformation, this.inlineDSLLibrary, repository, null, null, context, importId, 0, null);
            return visitor.visit(parser.treePath());
        }
        catch (Exception e)
        {
            if (isAntlrRecognitionExceptionUsingFastParser(useFastParser, e))
            {
                //System.err.println("Error using fast Antlr Parser: " + ExceptionUtils.getStackTrace(e));
                return this.parseTreePath(false, code, fileName, offsetLine, offsetColumn, importId, repository, context);
            }
            else
            {
                throw e;
            }
        }
    }

    public CoreInstance parseTreePath(String code, String fileName, int offsetLine, int offsetColumn, ImportGroup importId, ModelRepository repository, Context context) throws PureParserException
    {
        return this.parseTreePath(true, code, fileName, offsetLine, offsetColumn, importId, repository, context);
    }

    public TemporaryPureSetImplementation parseMappingInfo(String content, String classPath, AntlrContextToM3CoreInstance.LambdaContext lambdaContext, String sourceName, int offset, String importId, ModelRepository repository, ProcessorSupport processorSupport, final Context context)
    {
        AntlrSourceInformation sourceInformation = new AntlrSourceInformation(offset, 0, sourceName, true);
        org.finos.legend.pure.m3.serialization.grammar.m3parser.antlr.M3Parser parser = initAntlrParser(true, content, sourceInformation);
        ImportGroup grp = (ImportGroup)processorSupport.package_getByUserPath("system::imports::" + importId);
        M3AntlrTreeWalker visitor = new M3AntlrTreeWalker(classPath, sourceInformation, this.inlineDSLLibrary, repository, null, null, context, grp, 0, null);
        return visitor.walkMapping(parser.mapping(), lambdaContext);
    }

    @Override
    public String parseMapping(String content, String id, String extendsId, String setSourceInfo, boolean root, String classPath, String classSourceInfo, String mappingPath, String sourceName, int offset, String importId, ModelRepository repository, Context context) throws PureParserException
    {
        M3ProcessorSupport processorSupport = new M3ProcessorSupport(context, repository);
        String mappingName = mappingPath.replace("::","_");
        String classMappingName = classPath.replace("::","_");
        AntlrContextToM3CoreInstance.LambdaContext lambdaContext = new AntlrContextToM3CoreInstance.LambdaContext(mappingName + '_' + classMappingName + (id == null ? "" : '_' + id));
        TemporaryPureSetImplementation arg = parseMappingInfo(content, classPath, lambdaContext, sourceName, offset, importId, repository, processorSupport, context);

        return "^meta::pure::mapping::modelToModel::PureInstanceSetImplementation"+ setSourceInfo+ "(\n" +
                        (id == null ? "" : "id = '" + id + "',\n") +
                        "   root = " + root + ",\n" +
                        (arg._class==null ? "" : "   srcClass = " + process(arg._class, processorSupport) + ",\n") +
                        (arg.filter==null ? "" : "   filter = ^meta::pure::metamodel::function::LambdaFunction " + lambdaContext.getLambdaFunctionUniqueName() + ' ' + arg.filter.getSourceInformation().toM4String() + " (classifierGenericType=^meta::pure::metamodel::type::generics::GenericType " + arg.filter.getSourceInformation().toM4String() + " (rawType=meta::pure::metamodel::function::LambdaFunction, typeArguments=^meta::pure::metamodel::type::generics::GenericType " + arg.filter.getSourceInformation().toM4String() + " (rawType = ^meta::pure::metamodel::type::FunctionType " + arg.filter.getSourceInformation().toM4String() + " ()))," +
                                "                                                                          expressionSequence=" + process(arg.filter, processorSupport) + "),") +
                        "   class = ^meta::pure::metamodel::import::ImportStub " + classSourceInfo + " (idOrPath='" + classPath + "', importGroup=system::imports::" + importId + "),\n" +
                        "   parent= ^meta::pure::metamodel::import::ImportStub(idOrPath='" + mappingPath + "', importGroup=system::imports::" + importId + "),\n" +
                        "   propertyMappings=[" + arg.propertyMappings.collect(temporaryPurePropertyMapping ->
                                "^meta::pure::mapping::modelToModel::PurePropertyMapping " + temporaryPurePropertyMapping.sourceInformation.toM4String() + " (property='" + temporaryPurePropertyMapping.property + "'," +
                                "        explodeProperty = " + temporaryPurePropertyMapping.explodeProperty + "," +
                                "        localMappingProperty = " + temporaryPurePropertyMapping.localMappingProperty + "," +

                                (temporaryPurePropertyMapping.localMappingPropertyType == null ? "" : "        localMappingPropertyType = " + process(temporaryPurePropertyMapping.localMappingPropertyType._rawTypeCoreInstance(), processorSupport) + ",\n") +
                                (temporaryPurePropertyMapping.localMappingPropertyMultiplicity == null ? "" : "        localMappingPropertyMultiplicity = " + process(temporaryPurePropertyMapping.localMappingPropertyMultiplicity, processorSupport) + ",") +
                                (temporaryPurePropertyMapping.targetMappingId == null ? "" : "                                         targetSetImplementationId='" + temporaryPurePropertyMapping.targetMappingId + "',") +
                                (temporaryPurePropertyMapping.enumerationMappingInformation == null ? "" : "transformer=^meta::pure::tools::GrammarInfoStub" + temporaryPurePropertyMapping.enumerationMappingInformation.getTwo().toM4String() + "(value='" + mappingPath + "," + temporaryPurePropertyMapping.enumerationMappingInformation.getOne() + "'),") +
                                "                                         transform=^meta::pure::metamodel::function::LambdaFunction " + lambdaContext.getLambdaFunctionUniqueName() + ' ' + temporaryPurePropertyMapping.expression.getSourceInformation().toM4String() + " (" +
                                "                                                           classifierGenericType=^meta::pure::metamodel::type::generics::GenericType " + temporaryPurePropertyMapping.expression.getSourceInformation().toM4String() + " (rawType=meta::pure::metamodel::function::LambdaFunction, typeArguments=^meta::pure::metamodel::type::generics::GenericType " + temporaryPurePropertyMapping.expression.getSourceInformation().toM4String() + " (rawType = ^meta::pure::metamodel::type::FunctionType " + temporaryPurePropertyMapping.expression.getSourceInformation().toM4String() + " ()))," +
                                "                                                           expressionSequence=" + process(temporaryPurePropertyMapping.expression, processorSupport) + ")" +
                                "                                                      )\n").makeString(",")+"]\n"+
                        ")";
    }

    public TemporaryPureAggregateSpecification parseAggregateSpecification(String content, AntlrContextToM3CoreInstance.LambdaContext lambdaContext, String sourceName, int offset, String importId, int index, ModelRepository repository, ProcessorSupport processorSupport, Context context)
    {
        AntlrSourceInformation sourceInformation = new AntlrSourceInformation(offset, 0, sourceName, true);
        org.finos.legend.pure.m3.serialization.grammar.m3parser.antlr.M3Parser parser = initAntlrParser(true, content, sourceInformation);
        ImportGroup grp = (ImportGroup)processorSupport.package_getByUserPath("system::imports::" + importId);
        M3AntlrTreeWalker visitor = new M3AntlrTreeWalker(null, sourceInformation, this.inlineDSLLibrary, repository, null, null, context, grp, 0, null);
        return visitor.walkAggregateSpecification(parser.aggregateSpecification(), lambdaContext, index);
    }

    @Deprecated
    public static String process(CoreInstance ci, Context context, ProcessorSupport processorSupport)
    {
        return process(ci, processorSupport);
    }

    public static String process(CoreInstance ci, ProcessorSupport processorSupport)
    {
        if (Instance.instanceOf(ci, M3Paths.PackageableMultiplicity, processorSupport) || Instance.instanceOf(ci, M3Paths.PrimitiveType, processorSupport))
        {
            return PackageableElement.getUserPathForPackageableElement(ci);
        }
        return "^" + PackageableElement.getUserPathForPackageableElement(ci.getClassifier()) + (ci.getSourceInformation() == null ? "" : " " + ci.getSourceInformation().toM4String() + " ") + "(" + ci.getKeys().collect(s -> s + "=" + ci.getValueForMetaPropertyToMany(s).collect(coreInstance ->
        {
            if (Instance.instanceOf(coreInstance, M3Paths.PackageableElement, processorSupport) &&
                    !Instance.instanceOf(coreInstance, M3Paths.FunctionType, processorSupport) &&
                    !Instance.instanceOf(coreInstance, M3Paths.Function, processorSupport))
            {
                return PackageableElement.getUserPathForPackageableElement(coreInstance);
            }
            if (Instance.instanceOf(coreInstance.getClassifier(), M3Paths.PrimitiveType, processorSupport))
            {
                if (Instance.instanceOf(coreInstance, M3Paths.String, processorSupport))
                {
                    return "'" + coreInstance.getName() + "'";
                }
                if (Instance.instanceOf(coreInstance, M3Paths.LatestDate, processorSupport))
                {
                    return "%latest";
                }
                if (Instance.instanceOf(coreInstance, M3Paths.Date, processorSupport))
                {
                    return "%" + coreInstance.getName();
                }
                if (Instance.instanceOf(coreInstance, M3Paths.StrictTime, processorSupport))
                {
                    return "%" + coreInstance.getName();
                }
                return coreInstance.getName();
            }
            return process(coreInstance, processorSupport);
        })).makeString(",") + ")";
    }

    @Override
    public RichIterable<MatchRunner> getProcessors()
    {
        return Lists.immutable.with(
                new ElementWithConstraintsProcessor(),
                new ClassProcessor(),
                new ClassProjectionProcessor(),
                new RootRouteNodePostProcessor(),
                new EnumerationProcessor(),
                new AssociationProcessor(),
                new PropertyOwnerProcessor(),
                new FunctionDefinitionProcessor(),
                new LambdaFunctionProcessor(),
                new PropertyProcessor(),
                new AnnotatedElementProcessor(),
                new KeyExpressionProcessor(),
                new VariableExpressionProcessor(),
                new InstanceValueProcessor(),
                new FunctionExpressionProcessor(),
                new UnitProcessor()
        );
    }

    @Override
    public RichIterable<MatchRunner> getUnLoadWalkers()
    {
        return Lists.immutable.with(
                new PackageableElementUnloaderWalk(),
                new TypeUnloaderWalk(),
                new FunctionTypeUnloaderWalk(),
                new ClassUnloaderWalk(),
                new ClassProjectionUnloaderWalk(),
                new EnumerationUnloaderWalk(),
                new EnumUnloaderWalk(),
                new ProfileUnloaderWalk(),
                new GeneralizationUnloaderWalk(),
                new AssociationUnloaderWalk(),
                new GenericTypeUnloaderWalk(),
                new ValueSpecificationUnloaderWalk(),
                new VariableExpressionUnloaderWalk(),
                new MeasureUnloaderWalk(),
                new UnitUnloaderWalk(),

                new ParameterValueSpecificationContextUnloaderWalk(),
                new ExpressionSequenceValueSpecificationContextUnloaderWalk(),
                new KeyValueValueSpecificationContextUnloaderWalk(),
                new InstanceValueSpecificationContextUnloaderWalk(),
                new ClassConstraintValueSpecificationContextUnloaderWalk(),

                new ConcreteFunctionDefinitionUnloaderWalk(),
                new AbstractPropertyUnloaderWalk(),
                new PropertyUnloaderWalk(),
                new FunctionUnloaderWalk(),
                new NativeFunctionUnloaderWalk(),

                new RootRouteNodeUnloaderWalk(),
                new PropertyRouteNodeUnloaderWalk(),
                new RouteNodePropertyStubUnloaderWalk(),
                new NewPropertyRouteNodeFunctionDefinitionUnloaderWalk()
        );
    }

    @Override
    public RichIterable<MatchRunner> getUnLoadUnbinders()
    {
        return Lists.immutable.with(
                new AnyUnbind(),
                new ElementWithStereotypesUnbind(),
                new ElementWithTaggedValuesUnbind(),
                new ElementWithConstraintsUnbind(),
                new TypeUnbind(),
                new ClassUnbind(),
                new ClassProjectionUnbind(),
                new EnumerationUnbind(),
                new RootRouteNodeUnbind(),
                new AssociationUnbind(),
                new ConcreteFunctionDefinitionUnbind(),
                new FunctionDefinitionUnbind(),
                new LambdaFunctionUnbind(),
                new AbstractPropertyUnbind(),
                new ValueSpecificationUnbind(),
                new SimpleFunctionExpressionUnbind(),
                new InstanceValueUnbind(),
                new UnitUnbind()
        );
    }

    @Override
    public RichIterable<MatchRunner> getValidators()
    {
        return Lists.immutable.with(
                new AbstractPropertyValidator(),
                new ElementWithConstraintsValidator(),
                new ClassValidator(),
                new ClassProjectionValidator(),
                new RootRouteNodeValidator(),
                new EnumerationValidator(),
                new TypeValidator(),
                new AssociationValidator(),
                new PropertyValidator(),
                new FunctionDefinitionValidator(),
                new ValueSpecificationValidator(),
                new FunctionExpressionValidator(),
                new InstanceValueValidator(),
                new GenericTypeValidator(),
                new TaggedValueValidator(),
                new ElementWithStereotypesValidator(),
                new ElementWithTaggedValueValidator(),
                new AccessLevelValidator(),
                new RepositoryPackageValidator(),
                new PackageValidator()
        );
    }

    @Override
    public RichIterable<NavigationHandler> getNavigationHandlers()
    {
        return Lists.immutable.empty();
    }

    @Override
    public RichIterable<ExternalReferenceSerializer> getExternalReferenceSerializers()
    {
        return Lists.immutable.with(
                new EnumReferenceSerializer(),
                new PropertyReferenceSerializer(),
                new QualifiedPropertyReferenceSerializer(),
                new TagReferenceSerializer(),
                new StereotypeReferenceSerializer()
        );
    }

    @Override
    public RichIterable<CoreInstanceFactoryRegistry> getCoreInstanceFactoriesRegistry()
    {
        return Lists.immutable.with(M3CoreInstanceFactoryRegistry.REGISTRY);
    }

    @Override
    public Parser newInstance(ParserLibrary library)
    {
        return new M3AntlrParser(this.inlineDSLLibrary);
    }

    @Override
    public SetIterable<String> getRequiredParsers()
    {
        return Sets.immutable.empty();
    }

    @Override
    public ListIterable<String> getRequiredFiles()
    {
        return Lists.immutable.empty();
    }

    public static org.finos.legend.pure.m3.serialization.grammar.m3parser.antlr.M3Parser initAntlrParser(boolean fastParser, String code, AntlrSourceInformation sourceInformation)
    {
        AntlrDescriptiveErrorListener pureErrorListener = new AntlrDescriptiveErrorListener(sourceInformation);

        org.finos.legend.pure.m3.serialization.grammar.m3parser.antlr.M3Lexer lexer = new org.finos.legend.pure.m3.serialization.grammar.m3parser.antlr.M3Lexer(new ANTLRInputStream(code));
        lexer.removeErrorListeners();
        lexer.addErrorListener(pureErrorListener);

        org.finos.legend.pure.m3.serialization.grammar.m3parser.antlr.M3Parser parser = new org.finos.legend.pure.m3.serialization.grammar.m3parser.antlr.M3Parser(new CommonTokenStream(lexer));
        parser.removeErrorListeners();
        parser.addErrorListener(pureErrorListener);
        parser.setErrorHandler(new PureAntlrErrorStrategy(sourceInformation));
        parser.getInterpreter().setPredictionMode(fastParser ? PredictionMode.SLL : PredictionMode.LL);
        return parser;
    }

    private static boolean isAntlrRecognitionExceptionUsingFastParser(boolean parseFast, Exception e)
    {
        return parseFast && e instanceof PureParserException && e.getCause() instanceof RecognitionException;
    }

}
