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

package org.finos.legend.pure.m2.inlinedsl.path.processor;

import org.eclipse.collections.api.factory.Lists;
import org.eclipse.collections.api.list.ListIterable;
import org.finos.legend.pure.m3.compiler.Context;
import org.finos.legend.pure.m3.compiler.postprocessing.GenericTypeTraceability;
import org.finos.legend.pure.m3.compiler.postprocessing.PostProcessor;
import org.finos.legend.pure.m3.compiler.postprocessing.ProcessorState;
import org.finos.legend.pure.m3.compiler.postprocessing.processor.Processor;
import org.finos.legend.pure.m3.compiler.postprocessing.processor.milestoning.MilestoningDates;
import org.finos.legend.pure.m3.compiler.postprocessing.processor.milestoning.MilestoningDatesPropagationFunctions;
import org.finos.legend.pure.m3.compiler.postprocessing.processor.milestoning.MilestoningFunctions;
import org.finos.legend.pure.m3.compiler.postprocessing.processor.milestoning.MilestoningStereotype;
import org.finos.legend.pure.m3.compiler.postprocessing.processor.milestoning.MilestoningStereotypeEnum;
import org.finos.legend.pure.m3.compiler.postprocessing.processor.valuespecification.InstanceValueProcessor;
import org.finos.legend.pure.m3.coreinstance.meta.pure.metamodel._import.PropertyStub;
import org.finos.legend.pure.m3.coreinstance.meta.pure.metamodel.function.property.AbstractProperty;
import org.finos.legend.pure.m3.coreinstance.meta.pure.metamodel.multiplicity.Multiplicity;
import org.finos.legend.pure.m3.coreinstance.meta.pure.metamodel.path.Path;
import org.finos.legend.pure.m3.coreinstance.meta.pure.metamodel.path.PathElement;
import org.finos.legend.pure.m3.coreinstance.meta.pure.metamodel.path.PropertyPathElement;
import org.finos.legend.pure.m3.coreinstance.meta.pure.metamodel.type.FunctionType;
import org.finos.legend.pure.m3.coreinstance.meta.pure.metamodel.type.generics.GenericType;
import org.finos.legend.pure.m3.coreinstance.meta.pure.metamodel.valuespecification.InstanceValue;
import org.finos.legend.pure.m3.coreinstance.meta.pure.metamodel.valuespecification.ValueSpecification;
import org.finos.legend.pure.m2.inlinedsl.path.M2PathPaths;
import org.finos.legend.pure.m2.inlinedsl.path.PathMilestoningDatesVarNamesExtractor;
import org.finos.legend.pure.m3.navigation.M3Paths;
import org.finos.legend.pure.m3.navigation.ProcessorSupport;
import org.finos.legend.pure.m3.navigation.importstub.ImportStub;
import org.finos.legend.pure.m3.navigation.type.Type;
import org.finos.legend.pure.m3.tools.matcher.Matcher;
import org.finos.legend.pure.m4.ModelRepository;
import org.finos.legend.pure.m4.coreinstance.CoreInstance;
import org.finos.legend.pure.m4.coreinstance.SourceInformation;
import org.finos.legend.pure.m4.exception.PureCompilationException;

public class PathProcessor extends Processor<Path<?, ?>>
{
    @Override
    public String getClassName()
    {
        return M2PathPaths.Path;
    }

    @Override
    public void process(Path<?, ?> instance, ProcessorState state, Matcher matcher, ModelRepository repository, Context context, ProcessorSupport processorSupport)
    {
        GenericType source = (GenericType) org.finos.legend.pure.m3.navigation.generictype.GenericType.copyGenericType(instance._start(), processorSupport);
        CoreInstance _class = ImportStub.withImportStubByPass(source._rawTypeCoreInstance(), processorSupport);

        boolean possiblyZero = false;
        boolean toMany = false;
        MilestoningDates propagatedDates = state.getMilestoningDates(PathMilestoningDatesVarNamesExtractor.PATH_MILESTONING_DATES_VARIABLE_NAME);

        GenericType finalReturnType = null;
        for (PathElement pathElement : instance._path())
        {
            PostProcessor.processElement(matcher, _class, state, processorSupport);
            if (pathElement instanceof PropertyPathElement)
            {
                PropertyPathElement propertyPathElement = (PropertyPathElement) pathElement;
                PropertyStub propertyStubNonResolved = (PropertyStub) propertyPathElement._propertyCoreInstance();
                propertyStubNonResolved._ownerCoreInstance(_class);
                AbstractProperty<?> property = (AbstractProperty<?>) ImportStub.withImportStubByPass(propertyPathElement._propertyCoreInstance(), processorSupport);
                FunctionType functionType = (FunctionType) processorSupport.function_getFunctionType(property);
                finalReturnType = (GenericType)ImportStub.withImportStubByPass(functionType._returnType(), processorSupport);
                _class = ImportStub.withImportStubByPass(finalReturnType._rawTypeCoreInstance(), processorSupport);
                toMany = toMany || !org.finos.legend.pure.m3.navigation.multiplicity.Multiplicity.isToOne(functionType._returnMultiplicity(), false);
                possiblyZero = possiblyZero || org.finos.legend.pure.m3.navigation.multiplicity.Multiplicity.isLowerZero(functionType._returnMultiplicity());

                propertyPathElement._parameters().forEach(vs ->
                {
                    if (vs instanceof InstanceValue)
                    {
                        InstanceValueProcessor.updateInstanceValue(vs, processorSupport);
                    }
                });

                propagatedDates = processMilestoningQualifiedProperty(_class, state, propagatedDates, propertyPathElement, propertyStubNonResolved, property, processorSupport);
            }
        }

        GenericType classifierGenericType = (GenericType) processorSupport.newEphemeralAnonymousCoreInstance(M3Paths.GenericType);
        classifierGenericType._rawTypeCoreInstance(instance.getClassifier());
        classifierGenericType._typeArgumentsAdd(source);
        classifierGenericType._typeArgumentsAdd((GenericType) org.finos.legend.pure.m3.navigation.generictype.GenericType.copyGenericType(finalReturnType, processorSupport));

        CoreInstance multiplicity = processorSupport.package_getByUserPath(possiblyZero ? (toMany ? M3Paths.ZeroMany : M3Paths.ZeroOne) : (toMany ? M3Paths.OneMany : M3Paths.PureOne));

        classifierGenericType._multiplicityArgumentsAdd((Multiplicity) multiplicity);

        instance._classifierGenericType(classifierGenericType);
    }

    @Override
    public void populateReferenceUsages(Path<?, ?> path, ModelRepository repository, ProcessorSupport processorSupport)
    {
        GenericTypeTraceability.addTraceForPath(path, repository, processorSupport);
        int i = 0;
        for (PathElement pathElement : path._path())
        {
            if (pathElement instanceof PropertyPathElement)
            {
                AbstractProperty<?> property = (AbstractProperty<?>) ImportStub.withImportStubByPass(((PropertyPathElement) pathElement)._propertyCoreInstance(), processorSupport);
                this.addReferenceUsage(path, property, "path", i, repository, processorSupport, pathElement.getSourceInformation());
            }
            i++;
        }
    }

    private MilestoningDates processMilestoningQualifiedProperty(CoreInstance _class, ProcessorState state, MilestoningDates propagatedDates, PropertyPathElement pathElement, PropertyStub propertyStubNonResolved, AbstractProperty<?> property, ProcessorSupport processorSupport)
    {
        if (!MilestoningFunctions.isGeneratedQualifiedProperty(property, processorSupport))
        {
            return null;
        }

        MilestoningStereotype milestoningStereotype = MilestoningFunctions.getTemporalStereoTypesFromTopMostNonTopTypeGeneralizations(_class, processorSupport).getFirst();
        CoreInstance milestonedPropertyWithArg = MilestoningDatesPropagationFunctions.getMatchingMilestoningQualifiedPropertyWithDateArg(property, property._functionName(), processorSupport);
        propertyStubNonResolved._resolvedPropertyCoreInstance(milestonedPropertyWithArg);
        if (milestoningStereotype == MilestoningStereotypeEnum.businesstemporal)
        {
            if (pathElement._parameters().isEmpty())
            {
                if (propagatedDates != null)
                {
                    pathElement._parameters(Lists.fixedSize.of((ValueSpecification) propagatedDates.getBusinessDate()));
                }
                else
                {
                    throwMilestoningPropertyPathValidationException(property, pathElement.getSourceInformation(), processorSupport);
                }
            }
            else
            {
                ValueSpecification businessDate = pathElement._parameters().getFirst();
                return new MilestoningDates(businessDate, null);
            }
        }
        return propagatedDates;
    }

    private void throwMilestoningPropertyPathValidationException(AbstractProperty<?> property, SourceInformation pathSouceInformation, ProcessorSupport processorSupport)
    {
        String noArgPropertyName = property._functionName();
        CoreInstance noArgPropertyReturnType = ImportStub.withImportStubByPass(property._genericType()._rawTypeCoreInstance(), processorSupport);
        ListIterable<String> temporalPropertyNames = MilestoningFunctions.getTemporalStereoTypePropertyNamesFromTopMostNonTopTypeGeneralizations(noArgPropertyReturnType, processorSupport);
        throw new PureCompilationException(pathSouceInformation, "No-Arg milestoned property: '" + noArgPropertyName + "' must be either called in a milestoning context or supplied with " + temporalPropertyNames.makeString("[", ",", "]") + " parameters");
    }
}
