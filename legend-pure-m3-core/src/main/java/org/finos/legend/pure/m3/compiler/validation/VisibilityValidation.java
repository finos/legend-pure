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

package org.finos.legend.pure.m3.compiler.validation;

import org.finos.legend.pure.m3.compiler.Context;
import org.finos.legend.pure.m3.compiler.visibility.Visibility;
import org.finos.legend.pure.m3.coreinstance.Package;
import org.finos.legend.pure.m3.coreinstance.meta.pure.functions.lang.KeyExpression;
import org.finos.legend.pure.m3.coreinstance.meta.pure.metamodel.extension.AnnotatedElement;
import org.finos.legend.pure.m3.coreinstance.meta.pure.metamodel.extension.Annotation;
import org.finos.legend.pure.m3.coreinstance.meta.pure.metamodel.extension.ElementWithStereotypes;
import org.finos.legend.pure.m3.coreinstance.meta.pure.metamodel.extension.Profile;
import org.finos.legend.pure.m3.coreinstance.meta.pure.metamodel.extension.Tag;
import org.finos.legend.pure.m3.coreinstance.meta.pure.metamodel.extension.TaggedValue;
import org.finos.legend.pure.m3.coreinstance.meta.pure.metamodel.function.Function;
import org.finos.legend.pure.m3.coreinstance.meta.pure.metamodel.function.FunctionDefinition;
import org.finos.legend.pure.m3.coreinstance.meta.pure.metamodel.function.LambdaFunction;
import org.finos.legend.pure.m3.coreinstance.meta.pure.metamodel.function.property.AbstractProperty;
import org.finos.legend.pure.m3.coreinstance.meta.pure.metamodel.function.property.Property;
import org.finos.legend.pure.m3.coreinstance.meta.pure.metamodel.function.property.QualifiedProperty;
import org.finos.legend.pure.m3.coreinstance.meta.pure.metamodel.relationship.Association;
import org.finos.legend.pure.m3.coreinstance.meta.pure.metamodel.relationship.Generalization;
import org.finos.legend.pure.m3.coreinstance.meta.pure.metamodel.type.Class;
import org.finos.legend.pure.m3.coreinstance.meta.pure.metamodel.type.FunctionType;
import org.finos.legend.pure.m3.coreinstance.meta.pure.metamodel.type.Type;
import org.finos.legend.pure.m3.coreinstance.meta.pure.metamodel.type.generics.GenericType;
import org.finos.legend.pure.m3.coreinstance.meta.pure.metamodel.valuespecification.FunctionExpression;
import org.finos.legend.pure.m3.coreinstance.meta.pure.metamodel.valuespecification.InstanceValue;
import org.finos.legend.pure.m3.coreinstance.meta.pure.metamodel.valuespecification.ValueSpecification;
import org.finos.legend.pure.m3.coreinstance.meta.pure.metamodel.valuespecification.VariableExpression;
import org.finos.legend.pure.m3.navigation.Instance;
import org.finos.legend.pure.m3.navigation.M3Paths;
import org.finos.legend.pure.m3.navigation.M3Properties;
import org.finos.legend.pure.m3.navigation.PackageableElement.PackageableElement;
import org.finos.legend.pure.m3.navigation.ProcessorSupport;
import org.finos.legend.pure.m3.navigation.function.FunctionDescriptor;
import org.finos.legend.pure.m3.navigation.importstub.ImportStub;
import org.finos.legend.pure.m3.serialization.filesystem.PureCodeStorage;
import org.finos.legend.pure.m4.coreinstance.CoreInstance;
import org.finos.legend.pure.m4.coreinstance.SourceInformation;
import org.finos.legend.pure.m4.exception.PureCompilationException;

public class VisibilityValidation
{
    public static void validateFunctionDefinition(FunctionDefinition<? extends CoreInstance> function, Context context, ValidatorState validatorState, ProcessorSupport processorSupport) throws PureCompilationException
    {
        Package pkg = function._package();
        if (pkg != null)
        {
            SourceInformation sourceInfo = function.getSourceInformation();
            validateFunctionDefinition(function, pkg, (sourceInfo == null) ? null : sourceInfo.getSourceId(), context, validatorState, processorSupport);
        }
    }

    public static void validateClass(Class<? extends CoreInstance> cls, Context context, ValidatorState validatorState, ProcessorSupport processorSupport) throws PureCompilationException
    {
        Package pkg = cls._package();
        if (pkg != null)
        {
            SourceInformation sourceInfo = cls.getSourceInformation();
            validateClass(cls, pkg, (sourceInfo == null) ? null : sourceInfo.getSourceId(), context, validatorState, processorSupport);
        }
    }

    public static void validateAssociation(Association association, Context context, ValidatorState validatorState, ProcessorSupport processorSupport) throws PureCompilationException
    {
        Package pkg = association._package();
        if (pkg != null)
        {
            SourceInformation sourceInfo = association.getSourceInformation();
            validateAssociation(association, pkg, (sourceInfo == null) ? null : sourceInfo.getSourceId(), context, validatorState, processorSupport);
        }
    }

    private static void validateAnnotatedElement(AnnotatedElement element, String sourceId, ValidatorState validatorState, ProcessorSupport processorSupport) throws PureCompilationException
    {
        // Check stereotypes
        for (CoreInstance stereotype : ImportStub.withImportStubByPasses(element._stereotypesCoreInstance().toList(), processorSupport))
        {
            Profile profile = ((Annotation)stereotype)._profile();
            if (!Visibility.isVisibleInSource(profile, sourceId, validatorState.getCodeStorage().getAllRepositories(), processorSupport))
            {
                throwRepoVisibilityException(element.getSourceInformation(), profile, sourceId, validatorState, processorSupport);
            }
        }

        // Check tagged values
        for (TaggedValue taggedValue : element._taggedValues())
        {
            Tag tag = (Tag)ImportStub.withImportStubByPass(taggedValue._tagCoreInstance(), processorSupport);
            Profile profile = tag == null ? null : tag._profile();
            if (!Visibility.isVisibleInSource(profile, sourceId, validatorState.getCodeStorage().getAllRepositories(), processorSupport))
            {
                throwRepoVisibilityException(element.getSourceInformation(), profile, sourceId, validatorState, processorSupport);
            }
        }
    }

    private static void validateFunctionDefinition(FunctionDefinition<? extends CoreInstance> function, CoreInstance pkg, String sourceId, Context context, ValidatorState validatorState, ProcessorSupport processorSupport) throws PureCompilationException
    {
        FunctionType functionType = (FunctionType)processorSupport.function_getFunctionType(function);

        // Check return type
        validateGenericType(functionType._returnType(), pkg, sourceId, context, validatorState, processorSupport, true);

        // Check parameter types
        for (VariableExpression parameter : functionType._parameters())
        {
            validateGenericType(parameter._genericType(), pkg, sourceId, context, validatorState, processorSupport, true);
        }

        // Check expression sequence
        for (ValueSpecification expression : function._expressionSequence())
        {
            validateValueSpecification(expression, pkg, sourceId, context, validatorState, processorSupport);
        }

        // Check annotations
        validateAnnotatedElement(function, sourceId, validatorState, processorSupport);
    }

    private static void validateClass(Class<? extends CoreInstance> cls, CoreInstance pkg, String sourceId, Context context, ValidatorState validatorState, ProcessorSupport processorSupport) throws PureCompilationException
    {
        // Check properties
        for (Property<?, ?> property : cls._properties())
        {
            validateGenericType(property._genericType(), pkg, sourceId, context, validatorState, processorSupport, true);
            validateAnnotatedElement(property, sourceId, validatorState, processorSupport);
        }
        for (Property<?, ?> propertyFromAssoc : cls._propertiesFromAssociations())
        {
            validateGenericType(propertyFromAssoc._genericType(), pkg, sourceId, context, validatorState, processorSupport, false);
            validateAnnotatedElement(propertyFromAssoc, sourceId, validatorState, processorSupport);

            GenericType genericType = propertyFromAssoc._genericType();
            Type rawType = (Type)ImportStub.withImportStubByPass(genericType._rawTypeCoreInstance(), processorSupport);
            if (org.finos.legend.pure.m3.navigation.generictype.GenericType.isGenericTypeConcrete(genericType, processorSupport) && !Visibility.isVisibleInSource(rawType, sourceId, validatorState.getCodeStorage().getAllRepositories(), processorSupport))
            {
                String classRepoName = PureCodeStorage.getSourceRepoName(cls.getSourceInformation().getSourceId());
                String otherRepoName = PureCodeStorage.getSourceRepoName(rawType.getSourceInformation().getSourceId());
                //todo: temporarily allow these while we clean-up
                boolean classIsInModelRepo = (classRepoName != null) && classRepoName.startsWith("model");
                boolean otherIsInModelRepo = (otherRepoName != null) && otherRepoName.startsWith("model");
                if (!(classIsInModelRepo && otherIsInModelRepo))
                {
                    throw new PureCompilationException(propertyFromAssoc.getSourceInformation(), "Associations are not permitted between classes in different repositories, " +
                            getElementNameForExceptionMessage(rawType, validatorState, processorSupport) + " is in the \"" + PureCodeStorage.getSourceRepoName(rawType.getSourceInformation().getSourceId()) + "\" repository and " +
                            getElementNameForExceptionMessage(cls, validatorState, processorSupport) + " is in the \"" + PureCodeStorage.getSourceRepoName(cls.getSourceInformation().getSourceId()) + "\" repository" +
                            ". This can be solved by first creating a subclass located in the same repository and creating an Association to the subclass.");
                }
            }

        }

        // Check qualified properties
        // TODO validate qualified property definitions
        for (QualifiedProperty property : cls._qualifiedProperties())
        {
            validateGenericType(property._genericType(), pkg, sourceId, context, validatorState, processorSupport, true);
            validateAnnotatedElement(property, sourceId, validatorState, processorSupport);
        }
        for (QualifiedProperty propertyFromAssoc : cls._qualifiedPropertiesFromAssociations())
        {
            validateGenericType(propertyFromAssoc._genericType(), pkg, sourceId, context, validatorState, processorSupport, false);
            validateAnnotatedElement(propertyFromAssoc, sourceId, validatorState, processorSupport);
        }

        // Check generalizations
        for (Generalization generalization : cls._generalizations())
        {
            validateGenericType(generalization._general(), pkg, sourceId, context, validatorState, processorSupport, true);
        }

        // Check annotations
        validateAnnotatedElement(cls, sourceId, validatorState, processorSupport);
    }

    private static void validateAssociation(Association association, CoreInstance pkg, String sourceId, Context context, ValidatorState validatorState, ProcessorSupport processorSupport) throws PureCompilationException
    {
        // Check properties
        for (Property<?,?> property : association._properties())
        {
            validateGenericType(property._genericType(), pkg, sourceId, context, validatorState, processorSupport, true);
        }

        // Check annotations
        validateAnnotatedElement(association, sourceId, validatorState, processorSupport);
    }

    private static void validateGenericType(GenericType genericType, CoreInstance pkg, String sourceId, Context context, ValidatorState validatorState, ProcessorSupport processorSupport, boolean checkSourceVisibility) throws PureCompilationException
    {
        if (org.finos.legend.pure.m3.navigation.generictype.GenericType.isGenericTypeConcrete(genericType, processorSupport))
        {
            Type rawType = (Type)ImportStub.withImportStubByPass(genericType._rawTypeCoreInstance(), processorSupport);
            if (rawType instanceof FunctionType)
            {
                for (VariableExpression parameter : ((FunctionType)rawType)._parameters())
                {
                    GenericType parameterType = parameter._genericType();
                    validateGenericType(parameterType, pkg, sourceId, context, validatorState, processorSupport, checkSourceVisibility);
                }

                GenericType returnType = ((FunctionType)rawType)._returnType();
                validateGenericType(returnType, pkg, sourceId, context, validatorState, processorSupport, checkSourceVisibility);
            }
            else
            {
                if (!Visibility.isVisibleInPackage(rawType, pkg, context, processorSupport))
                {
                    throwAccessException(genericType.getSourceInformation(), rawType, pkg, validatorState, processorSupport);
                }
                if (checkSourceVisibility && !Visibility.isVisibleInSource(rawType, sourceId, validatorState.getCodeStorage().getAllRepositories(), processorSupport))
                {
                    throwRepoVisibilityException(genericType.getSourceInformation(), rawType, sourceId, validatorState, processorSupport);
                }
            }
            for (GenericType typeArgument : genericType._typeArguments())
            {
                validateGenericType(typeArgument, pkg, sourceId, context, validatorState, processorSupport, checkSourceVisibility);
            }
        }
    }

    private static void validateValueSpecification(ValueSpecification valueSpec, CoreInstance pkg, String sourceId, Context context, ValidatorState validatorState, ProcessorSupport processorSupport) throws PureCompilationException
    {
        if (valueSpec instanceof FunctionExpression)
        {
            validateFunctionExpression((FunctionExpression)valueSpec, pkg, sourceId, context, validatorState, processorSupport);
        }
        else if (valueSpec instanceof InstanceValue)
        {
            for (CoreInstance value : ImportStub.withImportStubByPasses(((InstanceValue)valueSpec)._valuesCoreInstance().toList(), processorSupport))
            {
                if (value instanceof LambdaFunction)
                {
                    validateFunctionDefinition((LambdaFunction<? extends CoreInstance>)value, pkg, sourceId, context, validatorState, processorSupport);
                }
                else if (Instance.instanceOf(value, M3Paths.Path, processorSupport))
                {
                    validatePath(value, pkg, sourceId, context, validatorState, processorSupport);
                }
                else if (Instance.instanceOf(value, M3Paths.RootGraphFetchTree, processorSupport))
                {
                    validateRootGraphFetchTree(value, pkg, sourceId, context, validatorState, processorSupport);
                }
                else if (value instanceof Function)
                {
                    validatePackageAndSourceVisibility(valueSpec, pkg, sourceId, context, validatorState, processorSupport, (Function)value);
                }
                else if (value instanceof ValueSpecification)
                {
                    validateValueSpecification((ValueSpecification)value, pkg, sourceId, context, validatorState, processorSupport);
                }
                else if (value instanceof Class)
                {
                    validatePackageAndSourceVisibility(valueSpec, pkg, sourceId, context, validatorState, processorSupport, (Class)value);
                }
                else if (value instanceof KeyExpression)
                {
                    validateValueSpecification(((KeyExpression)value)._key(), pkg, sourceId, context, validatorState, processorSupport);
                    validateValueSpecification(((KeyExpression)value)._expression(), pkg, sourceId, context, validatorState, processorSupport);
                }
                else if (Instance.instanceOf(value, M3Paths.PackageableElement, processorSupport))
                {
                    if (!Visibility.isVisibleInSource(value, sourceId, validatorState.getCodeStorage().getAllRepositories(), processorSupport))
                    {
                        throwRepoVisibilityException(valueSpec.getSourceInformation(), value, sourceId, validatorState, processorSupport);
                    }
                }
            }
        }
    }

    public static void validatePackageAndSourceVisibility(CoreInstance valueSpec, CoreInstance pkg, String sourceId, Context context, ValidatorState validatorState, ProcessorSupport processorSupport, ElementWithStereotypes value)
    {
        if (!Visibility.isVisibleInPackage(value, pkg, context, processorSupport))
        {
            throwAccessException(valueSpec.getSourceInformation(), value, pkg, validatorState, processorSupport);
        }
        if (!Visibility.isVisibleInSource(value, sourceId, validatorState.getCodeStorage().getAllRepositories(), processorSupport))
        {
            throwRepoVisibilityException(valueSpec.getSourceInformation(), value, sourceId, validatorState, processorSupport);
        }
    }

    private static void validatePackageAndSourceVisibility(SourceInformation sourceInformationForError, CoreInstance pkg, String sourceId, Context context, ValidatorState validatorState, ProcessorSupport processorSupport, ElementWithStereotypes value)
    {
        if (!Visibility.isVisibleInPackage(value, pkg, context, processorSupport))
        {
            throwAccessException(sourceInformationForError, value, pkg, validatorState, processorSupport);
        }
        if (!Visibility.isVisibleInSource(value, sourceId, validatorState.getCodeStorage().getAllRepositories(), processorSupport))
        {
            throwRepoVisibilityException(sourceInformationForError, value, sourceId, validatorState, processorSupport);
        }
    }

    private static void validateFunctionExpression(FunctionExpression expression, CoreInstance pkg, String sourceId, Context context, ValidatorState validatorState, ProcessorSupport processorSupport) throws PureCompilationException
    {
        Function function = (Function)ImportStub.withImportStubByPass(expression._funcCoreInstance(), processorSupport);
        validatePackageAndSourceVisibility(expression, pkg, sourceId, context, validatorState, processorSupport, function);

        for (ValueSpecification parameterValue : expression._parametersValues())
        {
            validateValueSpecification(parameterValue, pkg, sourceId, context, validatorState, processorSupport);
        }
    }

    //TODO: move to m2-path
    private static void validatePath(CoreInstance path, CoreInstance pkg, String sourceId, Context context, ValidatorState validatorState, ProcessorSupport processorSupport) throws PureCompilationException
    {
        validateGenericType((GenericType)Instance.getValueForMetaPropertyToOneResolved(path, M3Properties.start, processorSupport), pkg, sourceId, context, validatorState, processorSupport, true);
        // TODO consider validating the parameters of the Path
    }

    //TODO: move to m2-graph
    private static void validateRootGraphFetchTree(CoreInstance rootTree, CoreInstance pkg, String sourceId, Context context, ValidatorState validatorState, ProcessorSupport processorSupport) throws PureCompilationException
    {
        validatePackageAndSourceVisibility(rootTree.getValueForMetaPropertyToOne("class").getSourceInformation(), pkg, sourceId, context, validatorState, processorSupport, (Class) Instance.getValueForMetaPropertyToOneResolved(rootTree, "class", processorSupport));
        for (CoreInstance subTree : Instance.getValueForMetaPropertyToManyResolved(rootTree, "subTrees", processorSupport))
        {
            validatePropertyGraphFetchTree(subTree, pkg, sourceId, context, validatorState, processorSupport);
        }
    }

    private static void validatePropertyGraphFetchTree(CoreInstance propertyTree, CoreInstance pkg, String sourceId, Context context, ValidatorState validatorState, ProcessorSupport processorSupport) throws PureCompilationException
    {
        validatePackageAndSourceVisibility(propertyTree.getValueForMetaPropertyToOne("property").getSourceInformation(), pkg, sourceId, context, validatorState, processorSupport, (AbstractProperty) Instance.getValueForMetaPropertyToOneResolved(propertyTree, "property", processorSupport));
        CoreInstance subTypeClass = Instance.getValueForMetaPropertyToOneResolved(propertyTree, "subType", processorSupport);
        if (subTypeClass != null)
        {
            validatePackageAndSourceVisibility(propertyTree.getValueForMetaPropertyToOne("subType").getSourceInformation(), pkg, sourceId, context, validatorState, processorSupport, (Class) subTypeClass);
        }
        for (CoreInstance subTree : Instance.getValueForMetaPropertyToManyResolved(propertyTree, "subTrees", processorSupport))
        {
            validatePropertyGraphFetchTree(subTree, pkg, sourceId, context, validatorState, processorSupport);
        }
    }

    private static void throwAccessException(SourceInformation sourceInfo, CoreInstance element, CoreInstance pkg, ValidatorState validatorState, ProcessorSupport processorSupport) throws PureCompilationException
    {
        throw new PureCompilationException(sourceInfo, getElementNameForExceptionMessage(element, validatorState, processorSupport) + " is not accessible in " + PackageableElement.getUserPathForPackageableElement(pkg, "::"));
    }

    private static void throwRepoVisibilityException(SourceInformation sourceInfo, CoreInstance element, String sourceId, ValidatorState validatorState, ProcessorSupport processorSupport) throws PureCompilationException
    {
        throw new PureCompilationException(sourceInfo, getElementNameForExceptionMessage(element, validatorState, processorSupport) + " is not visible in the file " + sourceId);
    }

    private static String getElementNameForExceptionMessage(CoreInstance element, ValidatorState validatorState, ProcessorSupport processorSupport)
    {
        if (Instance.instanceOf(element, M3Paths.Function, processorSupport))
        {
            return FunctionDescriptor.getFunctionDescriptor(element, processorSupport);
        }

        if (Instance.instanceOf(element, M3Paths.PackageableElement, processorSupport))
        {
            return PackageableElement.getUserPathForPackageableElement(element, "::");
        }

        return element.getName();
    }
}
