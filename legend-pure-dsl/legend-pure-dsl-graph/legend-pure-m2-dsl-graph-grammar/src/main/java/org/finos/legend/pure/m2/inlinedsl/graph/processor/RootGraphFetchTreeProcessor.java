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

package org.finos.legend.pure.m2.inlinedsl.graph.processor;

import org.eclipse.collections.api.list.ListIterable;
import org.eclipse.collections.api.list.MutableList;
import org.eclipse.collections.impl.list.mutable.FastList;
import org.finos.legend.pure.m2.inlinedsl.graph.M2GraphPaths;
import org.finos.legend.pure.m2.inlinedsl.graph.M2GraphProperties;
import org.finos.legend.pure.m3.compiler.Context;
import org.finos.legend.pure.m3.compiler.postprocessing.PostProcessor;
import org.finos.legend.pure.m3.compiler.postprocessing.ProcessorState;
import org.finos.legend.pure.m3.compiler.postprocessing.functionmatch.FunctionExpressionMatcher;
import org.finos.legend.pure.m3.compiler.postprocessing.processor.Processor;
import org.finos.legend.pure.m3.compiler.postprocessing.processor.milestoning.MilestoningFunctions;
import org.finos.legend.pure.m3.compiler.postprocessing.processor.valuespecification.InstanceValueProcessor;
import org.finos.legend.pure.m3.coreinstance.meta.pure.graphFetch.GraphFetchTree;
import org.finos.legend.pure.m3.coreinstance.meta.pure.graphFetch.PropertyGraphFetchTree;
import org.finos.legend.pure.m3.coreinstance.meta.pure.graphFetch.RootGraphFetchTree;
import org.finos.legend.pure.m3.coreinstance.meta.pure.metamodel._import.EnumStub;
import org.finos.legend.pure.m3.coreinstance.meta.pure.metamodel._import.PropertyStub;
import org.finos.legend.pure.m3.coreinstance.meta.pure.metamodel.function.property.AbstractProperty;
import org.finos.legend.pure.m3.coreinstance.meta.pure.metamodel.function.property.QualifiedProperty;
import org.finos.legend.pure.m3.coreinstance.meta.pure.metamodel.multiplicity.Multiplicity;
import org.finos.legend.pure.m3.coreinstance.meta.pure.metamodel.type.ClassInstance;
import org.finos.legend.pure.m3.coreinstance.meta.pure.metamodel.type.Enumeration;
import org.finos.legend.pure.m3.coreinstance.meta.pure.metamodel.type.FunctionType;
import org.finos.legend.pure.m3.coreinstance.meta.pure.metamodel.type.Type;
import org.finos.legend.pure.m3.coreinstance.meta.pure.metamodel.type.generics.GenericType;
import org.finos.legend.pure.m3.coreinstance.meta.pure.metamodel.type.generics.GenericTypeInstance;
import org.finos.legend.pure.m3.coreinstance.meta.pure.metamodel.valuespecification.InstanceValue;
import org.finos.legend.pure.m3.coreinstance.meta.pure.metamodel.valuespecification.ValueSpecification;
import org.finos.legend.pure.m3.coreinstance.meta.pure.metamodel.valuespecification.VariableExpression;
import org.finos.legend.pure.m3.navigation.Instance;
import org.finos.legend.pure.m3.navigation.M3Paths;
import org.finos.legend.pure.m3.navigation.M3Properties;
import org.finos.legend.pure.m3.navigation.PackageableElement.PackageableElement;
import org.finos.legend.pure.m3.navigation.ProcessorSupport;
import org.finos.legend.pure.m3.navigation._class._Class;
import org.finos.legend.pure.m3.navigation.functionexpression.FunctionExpression;
import org.finos.legend.pure.m3.navigation.importstub.ImportStub;
import org.finos.legend.pure.m3.tools.matcher.Matcher;
import org.finos.legend.pure.m4.ModelRepository;
import org.finos.legend.pure.m4.coreinstance.CoreInstance;
import org.finos.legend.pure.m4.coreinstance.SourceInformation;
import org.finos.legend.pure.m4.exception.PureCompilationException;

public class RootGraphFetchTreeProcessor extends Processor<RootGraphFetchTree<?>>
{
    @Override
    public String getClassName()
    {
        return M2GraphPaths.RootGraphFetchTree;
    }

    @Override
    public void process(RootGraphFetchTree<?> instance, ProcessorState state, Matcher matcher, ModelRepository repository, Context context, ProcessorSupport processorSupport)
    {
        CoreInstance _class = ImportStub.withImportStubByPass(instance._classCoreInstance(), processorSupport);
        PostProcessor.processElement(matcher, _class, state, processorSupport);

        ClassInstance type = (ClassInstance) processorSupport.package_getByUserPath(M2GraphPaths.RootGraphFetchTree);
        GenericType classifierGT = GenericTypeInstance.createPersistent(repository);
        GenericType typeArg = GenericTypeInstance.createPersistent(repository);
        typeArg._rawType((Type) _class);
        classifierGT._rawType(type);
        classifierGT._typeArgumentsAdd(typeArg);
        instance._classifierGenericType(classifierGT);

        for (GraphFetchTree subTree : instance._subTrees())
        {
            this.processPropertyGraphFetchTree((PropertyGraphFetchTree) subTree, _class, state, matcher, repository, processorSupport);
        }
    }

    private void processPropertyGraphFetchTree(PropertyGraphFetchTree propertyGraphFetchTree, CoreInstance _class, ProcessorState state, Matcher matcher, ModelRepository repository, ProcessorSupport processorSupport)
    {
        ClassInstance type = (ClassInstance) processorSupport.package_getByUserPath(M2GraphPaths.PropertyGraphFetchTree);
        GenericType classifierGT = GenericTypeInstance.createPersistent(repository);
        classifierGT._rawTypeCoreInstance(type);
        propertyGraphFetchTree._classifierGenericType(classifierGT);

        PropertyStub propertyStubNonResolved = (PropertyStub) propertyGraphFetchTree._propertyCoreInstance();
        String propertyName = propertyStubNonResolved._propertyName();
        propertyStubNonResolved._ownerCoreInstance(_class);

        for (ValueSpecification vs : propertyGraphFetchTree._parameters())
        {
            PostProcessor.processElement(matcher, vs, state, processorSupport);
            if (vs instanceof InstanceValue)
            {
                for (CoreInstance value : ((InstanceValue) vs)._valuesCoreInstance())
                {
                    if (value instanceof EnumStub)
                    {
                        EnumStub enumStub = (EnumStub) value;
                        Enumeration<?> enumerationCoreInstance = (Enumeration<?>) ImportStub.withImportStubByPass(enumStub._enumerationCoreInstance(), processorSupport);
                        org.finos.legend.pure.m3.coreinstance.meta.pure.metamodel.type.Enum enumValue = (org.finos.legend.pure.m3.coreinstance.meta.pure.metamodel.type.Enum) org.finos.legend.pure.m3.navigation.enumeration.Enumeration.findEnum(enumerationCoreInstance, enumStub._enumName());

                        if (enumValue == null)
                        {
                            throw new PureCompilationException(enumStub.getSourceInformation(), "The enum value '" + enumStub._enumName() + "' can't be found in the enumeration " + PackageableElement.getUserPathForPackageableElement(enumerationCoreInstance, "::"));
                        }
                    }
                }
            }
            InstanceValueProcessor.updateInstanceValue(vs, processorSupport);
        }

        AbstractProperty<?> property = null;

        if (propertyGraphFetchTree._parameters().isEmpty())
        {
            CoreInstance resolvedProperty = processorSupport.class_findPropertyUsingGeneralization(_class, propertyName);
            if (resolvedProperty != null)
            {
                property = (AbstractProperty<?>) resolvedProperty;
                Instance.addValueToProperty(propertyStubNonResolved, M3Properties.resolvedProperty, property, processorSupport);
            }
        }

        if (property == null)
        {
            // Qualified
            VariableExpression firstParam = (VariableExpression) processorSupport.newAnonymousCoreInstance(null, M3Paths.VariableExpression);
            firstParam._genericType((GenericType) org.finos.legend.pure.m3.navigation.type.Type.wrapGenericType(_class, processorSupport));
            firstParam._multiplicity((Multiplicity) processorSupport.package_getByUserPath(M3Paths.PureOne));
            MutableList<ValueSpecification> params = FastList.newList();
            params.add(firstParam);
            for (ValueSpecification vs : propertyGraphFetchTree._parameters())
            {
                params.add(vs);
            }

            ListIterable<QualifiedProperty<?>> qualifiedProperties = _Class.findQualifiedPropertiesUsingGeneralization(_class, propertyName, processorSupport);
            ListIterable<QualifiedProperty<?>> foundQualifiedProperties = FunctionExpressionMatcher.getFunctionMatches(qualifiedProperties, params, propertyName, propertyStubNonResolved.getSourceInformation(), true, processorSupport);

            if (foundQualifiedProperties.isEmpty())
            {
                StringBuilder message = new StringBuilder("The system can't find a match for the property / qualified property: ");
                FunctionExpression.printFunctionSignatureFromExpression(message, propertyName, params.without(firstParam), processorSupport);
                if (qualifiedProperties.notEmpty())
                {
                    if (MilestoningFunctions.isGeneratedQualifiedProperty(qualifiedProperties.getFirst(), processorSupport))
                    {
                        CoreInstance noArgPropertyReturnType = ImportStub.withImportStubByPass(qualifiedProperties.getFirst()._genericType()._rawTypeCoreInstance(), processorSupport);
                        ListIterable<String> temporalPropertyNames = MilestoningFunctions.getTemporalStereoTypePropertyNamesFromTopMostNonTopTypeGeneralizations(noArgPropertyReturnType, processorSupport);
                        message.append(". No-Arg milestoned property: '").append(propertyName).append("' is not supported yet in graph fetch flow! It needs to be supplied with ").append(temporalPropertyNames.makeString("[", ",", "]")).append(" parameters");
                    }
                }
                throw new PureCompilationException(propertyStubNonResolved.getSourceInformation(), message.toString());
            }

            property = foundQualifiedProperties.getFirst();
            Instance.addValueToProperty(propertyStubNonResolved, M3Properties.resolvedProperty, property, processorSupport);
        }

        FunctionType functionType = (FunctionType) processorSupport.function_getFunctionType(property);
        CoreInstance returnType = ImportStub.withImportStubByPass(functionType._returnType()._rawTypeCoreInstance(), processorSupport);
        PostProcessor.processElement(matcher, returnType, state, processorSupport);

        CoreInstance subTypeClass;
        if (propertyGraphFetchTree._subTypeCoreInstance() != null)
        {
            subTypeClass = ImportStub.withImportStubByPass(propertyGraphFetchTree._subTypeCoreInstance(), processorSupport);
            PostProcessor.processElement(matcher, subTypeClass, state, processorSupport);
            returnType = subTypeClass;
        }

        for (GraphFetchTree subTree : propertyGraphFetchTree._subTrees())
        {
            this.processPropertyGraphFetchTree((PropertyGraphFetchTree) subTree, returnType, state, matcher, repository, processorSupport);
        }

        if (MilestoningFunctions.isGeneratedQualifiedProperty(property, processorSupport))
        {
            CoreInstance propReturnType = ImportStub.withImportStubByPass(property._genericType()._rawTypeCoreInstance(), processorSupport);
            ListIterable<String> temporalPropertyNames = MilestoningFunctions.getTemporalStereoTypePropertyNamesFromTopMostNonTopTypeGeneralizations(propReturnType, processorSupport);

            if (propertyGraphFetchTree._parameters().size() != temporalPropertyNames.size())
            {
                this.throwMilestoningPropertyPathValidationException(property, propertyStubNonResolved.getSourceInformation(), processorSupport);
            }
        }
    }

    private void throwMilestoningPropertyPathValidationException(AbstractProperty<?> property, SourceInformation souceInformation, ProcessorSupport processorSupport)
    {
        String noArgPropertyName = property._functionName();
        CoreInstance noArgPropertyReturnType = ImportStub.withImportStubByPass(property._genericType()._rawTypeCoreInstance(), processorSupport);
        ListIterable<String> temporalPropertyNames = MilestoningFunctions.getTemporalStereoTypePropertyNamesFromTopMostNonTopTypeGeneralizations(noArgPropertyReturnType, processorSupport);
        throw new PureCompilationException(souceInformation, "No-Arg milestoned property: '" + noArgPropertyName + "' is not supported yet in graph fetch flow! It needs to be supplied with " + temporalPropertyNames.makeString("[", ",", "]") + " parameters");
    }

    @Override
    public void populateReferenceUsages(RootGraphFetchTree<?> instance, ModelRepository repository, ProcessorSupport processorSupport)
    {
        this.addReferenceUsageForToOneProperty(instance, instance._classCoreInstance(), M2GraphProperties._class, repository, processorSupport, instance._classCoreInstance().getSourceInformation());
        for (GraphFetchTree subTree : instance._subTrees())
        {
            this.populateReferenceUsagesForPropertyGraphFetchTrees((PropertyGraphFetchTree) subTree, instance, repository, processorSupport);
        }
    }

    private void populateReferenceUsagesForPropertyGraphFetchTrees(PropertyGraphFetchTree propertyGraphFetchTree, RootGraphFetchTree<?> mainTree, ModelRepository repository, ProcessorSupport processorSupport)
    {
        // TODO fix this reference usage: the reference should be to propertyGraphFetchTree, not mainTree
        this.addReferenceUsageForToOneProperty(mainTree, propertyGraphFetchTree._propertyCoreInstance(), M2GraphProperties.property, repository, processorSupport, propertyGraphFetchTree._propertyCoreInstance().getSourceInformation());
        if (propertyGraphFetchTree._subTypeCoreInstance() != null)
        {
            // TODO fix this reference usage: the reference should be to propertyGraphFetchTree, not mainTree
            this.addReferenceUsageForToOneProperty(mainTree, propertyGraphFetchTree._subTypeCoreInstance(), M2GraphProperties.subType, repository, processorSupport, propertyGraphFetchTree._subTypeCoreInstance().getSourceInformation());
        }
        for (GraphFetchTree subTree : propertyGraphFetchTree._subTrees())
        {
            this.populateReferenceUsagesForPropertyGraphFetchTrees((PropertyGraphFetchTree) subTree, mainTree, repository, processorSupport);
        }
    }
}
