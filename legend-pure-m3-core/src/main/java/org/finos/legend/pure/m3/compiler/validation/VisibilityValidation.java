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
import org.finos.legend.pure.m3.coreinstance.meta.pure.metamodel.function.PackageableFunction;
import org.finos.legend.pure.m3.coreinstance.meta.pure.metamodel.extension.AnnotatedElement;
import org.finos.legend.pure.m3.coreinstance.meta.pure.metamodel.extension.Annotation;
import org.finos.legend.pure.m3.coreinstance.meta.pure.metamodel.extension.ElementWithStereotypes;
import org.finos.legend.pure.m3.coreinstance.meta.pure.metamodel.extension.Profile;
import org.finos.legend.pure.m3.coreinstance.meta.pure.metamodel.extension.Tag;
import org.finos.legend.pure.m3.coreinstance.meta.pure.metamodel.function.Function;
import org.finos.legend.pure.m3.coreinstance.meta.pure.metamodel.function.FunctionDefinition;
import org.finos.legend.pure.m3.coreinstance.meta.pure.metamodel.function.LambdaFunction;
import org.finos.legend.pure.m3.coreinstance.meta.pure.metamodel.function.property.AbstractProperty;
import org.finos.legend.pure.m3.coreinstance.meta.pure.metamodel.relationship.Association;
import org.finos.legend.pure.m3.coreinstance.meta.pure.metamodel.type.Class;
import org.finos.legend.pure.m3.coreinstance.meta.pure.metamodel.type.FunctionType;
import org.finos.legend.pure.m3.coreinstance.meta.pure.metamodel.type.Type;
import org.finos.legend.pure.m3.coreinstance.meta.pure.metamodel.type.generics.GenericType;
import org.finos.legend.pure.m3.coreinstance.meta.pure.metamodel.valuespecification.FunctionExpression;
import org.finos.legend.pure.m3.coreinstance.meta.pure.metamodel.valuespecification.InstanceValue;
import org.finos.legend.pure.m3.coreinstance.meta.pure.metamodel.valuespecification.ValueSpecification;
import org.finos.legend.pure.m3.navigation.Instance;
import org.finos.legend.pure.m3.navigation.M3Paths;
import org.finos.legend.pure.m3.navigation.M3Properties;
import org.finos.legend.pure.m3.navigation.PackageableElement.PackageableElement;
import org.finos.legend.pure.m3.navigation.ProcessorSupport;
import org.finos.legend.pure.m3.navigation.function.FunctionDescriptor;
import org.finos.legend.pure.m3.navigation.importstub.ImportStub;
import org.finos.legend.pure.m3.serialization.filesystem.PureCodeStorage;
import org.finos.legend.pure.m3.tools.ListHelper;
import org.finos.legend.pure.m4.coreinstance.CoreInstance;
import org.finos.legend.pure.m4.coreinstance.SourceInformation;
import org.finos.legend.pure.m4.exception.PureCompilationException;

public class VisibilityValidation
{
    public static void validateFunctionDefinition(FunctionDefinition<?> function, Context context, ValidatorState validatorState, ProcessorSupport processorSupport) throws PureCompilationException
    {
        if (function instanceof PackageableFunction)
        {
            Package pkg = ((PackageableFunction)function)._package();
            SourceInformation sourceInfo = function.getSourceInformation();
            validateFunctionDefinition(function, pkg, (sourceInfo == null) ? null : sourceInfo.getSourceId(), context, validatorState, processorSupport);
        }
    }

    public static void validateClass(Class<?> cls, Context context, ValidatorState validatorState, ProcessorSupport processorSupport) throws PureCompilationException
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
        ImportStub.withImportStubByPasses(ListHelper.wrapListIterable(element._stereotypesCoreInstance()), processorSupport).forEach(stereotype ->
        {
            Profile profile = ((Annotation) stereotype)._profile();
            if (!Visibility.isVisibleInSource(profile, sourceId, validatorState.getCodeStorage().getAllRepositories(), processorSupport))
            {
                throwRepoVisibilityException(element.getSourceInformation(), profile, sourceId, processorSupport);
            }
        });

        // Check tagged values
        element._taggedValues().forEach(taggedValue ->
        {
            Tag tag = (Tag) ImportStub.withImportStubByPass(taggedValue._tagCoreInstance(), processorSupport);
            Profile profile = (tag == null) ? null : tag._profile();
            if (!Visibility.isVisibleInSource(profile, sourceId, validatorState.getCodeStorage().getAllRepositories(), processorSupport))
            {
                throwRepoVisibilityException(element.getSourceInformation(), profile, sourceId, processorSupport);
            }
        });
    }

    private static void validateFunctionDefinition(FunctionDefinition<?> function, CoreInstance pkg, String sourceId, Context context, ValidatorState validatorState, ProcessorSupport processorSupport) throws PureCompilationException
    {
        FunctionType functionType = (FunctionType) processorSupport.function_getFunctionType(function);

        // Check return type
        validateGenericType(functionType._returnType(), pkg, sourceId, context, validatorState, processorSupport, true);

        // Check parameter types
        functionType._parameters().forEach(p -> validateGenericType(p._genericType(), pkg, sourceId, context, validatorState, processorSupport, true));

        // Check expression sequence
        function._expressionSequence().forEach(e -> validateValueSpecification(e, pkg, sourceId, context, validatorState, processorSupport));

        // Check annotations
        if (function instanceof AnnotatedElement)
        {
            validateAnnotatedElement((AnnotatedElement)function, sourceId, validatorState, processorSupport);
        }
    }

    private static void validateClass(Class<?> cls, CoreInstance pkg, String sourceId, Context context, ValidatorState validatorState, ProcessorSupport processorSupport) throws PureCompilationException
    {
        // Check properties
        cls._properties().forEach(p ->
        {
            validateGenericType(p._genericType(), pkg, sourceId, context, validatorState, processorSupport, true);
            validateAnnotatedElement(p, sourceId, validatorState, processorSupport);
        });
        cls._propertiesFromAssociations().forEach(p ->
        {
            validateGenericType(p._genericType(), pkg, sourceId, context, validatorState, processorSupport, false);
            validateAnnotatedElement(p, sourceId, validatorState, processorSupport);

            GenericType genericType = p._genericType();
            Type rawType = (Type) ImportStub.withImportStubByPass(genericType._rawTypeCoreInstance(), processorSupport);
            if (org.finos.legend.pure.m3.navigation.generictype.GenericType.isGenericTypeConcrete(genericType) && !Visibility.isVisibleInSource(rawType, sourceId, validatorState.getCodeStorage().getAllRepositories(), processorSupport))
            {
                String classRepoName = PureCodeStorage.getSourceRepoName(cls.getSourceInformation().getSourceId());
                String otherRepoName = PureCodeStorage.getSourceRepoName(rawType.getSourceInformation().getSourceId());
                //todo: temporarily allow these while we clean-up
                boolean classIsInModelRepo = (classRepoName != null) && classRepoName.startsWith("model");
                boolean otherIsInModelRepo = (otherRepoName != null) && otherRepoName.startsWith("model");
                if (!(classIsInModelRepo && otherIsInModelRepo))
                {
                    throw new PureCompilationException(p.getSourceInformation(), "Associations are not permitted between classes in different repositories, " +
                            getElementNameForExceptionMessage(rawType, processorSupport) + " is in the \"" + PureCodeStorage.getSourceRepoName(rawType.getSourceInformation().getSourceId()) + "\" repository and " +
                            getElementNameForExceptionMessage(cls, processorSupport) + " is in the \"" + PureCodeStorage.getSourceRepoName(cls.getSourceInformation().getSourceId()) + "\" repository" +
                            ". This can be solved by first creating a subclass located in the same repository and creating an Association to the subclass.");
                }
            }
        });

        // Check qualified properties
        // TODO validate qualified property definitions
        cls._qualifiedProperties().forEach(qp ->
        {
            validateGenericType(qp._genericType(), pkg, sourceId, context, validatorState, processorSupport, true);
            validateAnnotatedElement(qp, sourceId, validatorState, processorSupport);
        });
        cls._qualifiedPropertiesFromAssociations().forEach(qp ->
        {
            validateGenericType(qp._genericType(), pkg, sourceId, context, validatorState, processorSupport, false);
            validateAnnotatedElement(qp, sourceId, validatorState, processorSupport);
        });

        // Check generalizations
        cls._generalizations().forEach(genl -> validateGenericType(genl._general(), pkg, sourceId, context, validatorState, processorSupport, true));

        // Check annotations
        validateAnnotatedElement(cls, sourceId, validatorState, processorSupport);
    }

    private static void validateAssociation(Association association, CoreInstance pkg, String sourceId, Context context, ValidatorState validatorState, ProcessorSupport processorSupport) throws PureCompilationException
    {
        // Check properties
        association._properties().forEach(p -> validateGenericType(p._genericType(), pkg, sourceId, context, validatorState, processorSupport, true));

        // Check annotations
        validateAnnotatedElement(association, sourceId, validatorState, processorSupport);
    }

    private static void validateGenericType(GenericType genericType, CoreInstance pkg, String sourceId, Context context, ValidatorState validatorState, ProcessorSupport processorSupport, boolean checkSourceVisibility) throws PureCompilationException
    {
        if (org.finos.legend.pure.m3.navigation.generictype.GenericType.isGenericTypeConcrete(genericType))
        {
            Type rawType = (Type) ImportStub.withImportStubByPass(genericType._rawTypeCoreInstance(), processorSupport);
            if (rawType instanceof FunctionType)
            {
                ((FunctionType) rawType)._parameters().forEach(p -> validateGenericType(p._genericType(), pkg, sourceId, context, validatorState, processorSupport, checkSourceVisibility));
                validateGenericType(((FunctionType) rawType)._returnType(), pkg, sourceId, context, validatorState, processorSupport, checkSourceVisibility);
            }
            else
            {
                if (!Visibility.isVisibleInPackage((ElementWithStereotypes)rawType, pkg, context, processorSupport))
                {
                    throwAccessException(genericType.getSourceInformation(), rawType, pkg, processorSupport);
                }
                if (checkSourceVisibility && !Visibility.isVisibleInSource(rawType, sourceId, validatorState.getCodeStorage().getAllRepositories(), processorSupport))
                {
                    throwRepoVisibilityException(genericType.getSourceInformation(), rawType, sourceId, processorSupport);
                }
            }
            genericType._typeArguments().forEach(arg -> validateGenericType(arg, pkg, sourceId, context, validatorState, processorSupport, checkSourceVisibility));
        }
    }

    private static void validateValueSpecification(ValueSpecification valueSpec, CoreInstance pkg, String sourceId, Context context, ValidatorState validatorState, ProcessorSupport processorSupport) throws PureCompilationException
    {
        if (valueSpec instanceof FunctionExpression)
        {
            validateFunctionExpression((FunctionExpression) valueSpec, pkg, sourceId, context, validatorState, processorSupport);
        }
        else if (valueSpec instanceof InstanceValue)
        {
            ImportStub.withImportStubByPasses(ListHelper.wrapListIterable(((InstanceValue) valueSpec)._valuesCoreInstance()), processorSupport).forEach(value ->
            {
                if (value instanceof LambdaFunction)
                {
                    validateFunctionDefinition((LambdaFunction<?>) value, pkg, sourceId, context, validatorState, processorSupport);
                }
                else if (Instance.instanceOf(value, M3Paths.Path, processorSupport))
                {
                    validatePath(value, pkg, sourceId, context, validatorState, processorSupport);
                }
                else if (Instance.instanceOf(value, M3Paths.RootGraphFetchTree, processorSupport))
                {
                    validateRootGraphFetchTree(value, pkg, sourceId, context, validatorState, processorSupport);
                }
                else if (value instanceof PackageableFunction)
                {
                    validatePackageAndSourceVisibility(valueSpec, pkg, sourceId, context, validatorState, processorSupport, (PackageableFunction<?>) value);
                }
                else if (value instanceof ValueSpecification)
                {
                    validateValueSpecification((ValueSpecification) value, pkg, sourceId, context, validatorState, processorSupport);
                }
                else if (value instanceof Class)
                {
                    validatePackageAndSourceVisibility(valueSpec, pkg, sourceId, context, validatorState, processorSupport, (Class<?>) value);
                }
                else if (value instanceof KeyExpression)
                {
                    validateValueSpecification(((KeyExpression) value)._key(), pkg, sourceId, context, validatorState, processorSupport);
                    validateValueSpecification(((KeyExpression) value)._expression(), pkg, sourceId, context, validatorState, processorSupport);
                }
                else if (Instance.instanceOf(value, M3Paths.PackageableElement, processorSupport))
                {
                    if (!Visibility.isVisibleInSource(value, sourceId, validatorState.getCodeStorage().getAllRepositories(), processorSupport))
                    {
                        throwRepoVisibilityException(valueSpec.getSourceInformation(), value, sourceId, processorSupport);
                    }
                }
            });
        }
    }

    public static void validatePackageAndSourceVisibility(CoreInstance valueSpec, CoreInstance pkg, String sourceId, Context context, ValidatorState validatorState, ProcessorSupport processorSupport, ElementWithStereotypes value)
    {
        if (!Visibility.isVisibleInPackage(value, pkg, context, processorSupport))
        {
            throwAccessException(valueSpec.getSourceInformation(), value, pkg, processorSupport);
        }
        if (!Visibility.isVisibleInSource(value, sourceId, validatorState.getCodeStorage().getAllRepositories(), processorSupport))
        {
            throwRepoVisibilityException(valueSpec.getSourceInformation(), value, sourceId, processorSupport);
        }
    }

    private static void validatePackageAndSourceVisibility(SourceInformation sourceInformationForError, CoreInstance pkg, String sourceId, Context context, ValidatorState validatorState, ProcessorSupport processorSupport, ElementWithStereotypes value)
    {
        if (!Visibility.isVisibleInPackage(value, pkg, context, processorSupport))
        {
            throwAccessException(sourceInformationForError, value, pkg, processorSupport);
        }
        if (!Visibility.isVisibleInSource(value, sourceId, validatorState.getCodeStorage().getAllRepositories(), processorSupport))
        {
            throwRepoVisibilityException(sourceInformationForError, value, sourceId, processorSupport);
        }
    }

    private static void validateFunctionExpression(FunctionExpression expression, CoreInstance pkg, String sourceId, Context context, ValidatorState validatorState, ProcessorSupport processorSupport) throws PureCompilationException
    {
        Function<?> function = (Function<?>) ImportStub.withImportStubByPass(expression._funcCoreInstance(), processorSupport);
        if (function instanceof PackageableFunction)
        {
            validatePackageAndSourceVisibility(expression, pkg, sourceId, context, validatorState, processorSupport, (PackageableFunction<?>)function);
        }
        expression._parametersValues().forEach(pv -> validateValueSpecification(pv, pkg, sourceId, context, validatorState, processorSupport));
    }

    //TODO: move to m2-path
    private static void validatePath(CoreInstance path, CoreInstance pkg, String sourceId, Context context, ValidatorState validatorState, ProcessorSupport processorSupport) throws PureCompilationException
    {
        validateGenericType((GenericType) Instance.getValueForMetaPropertyToOneResolved(path, M3Properties.start, processorSupport), pkg, sourceId, context, validatorState, processorSupport, true);
        // TODO consider validating the parameters of the Path
    }

    //TODO: move to m2-graph
    private static void validateRootGraphFetchTree(CoreInstance rootTree, CoreInstance pkg, String sourceId, Context context, ValidatorState validatorState, ProcessorSupport processorSupport) throws PureCompilationException
    {
        validatePackageAndSourceVisibility(rootTree.getValueForMetaPropertyToOne(M3Properties._class).getSourceInformation(), pkg, sourceId, context, validatorState, processorSupport, (Class<?>) Instance.getValueForMetaPropertyToOneResolved(rootTree, M3Properties._class, processorSupport));
        Instance.getValueForMetaPropertyToManyResolved(rootTree, "subTrees", processorSupport).forEach(subTree -> validatePropertyGraphFetchTree(subTree, pkg, sourceId, context, validatorState, processorSupport));
    }

    private static void validatePropertyGraphFetchTree(CoreInstance propertyTree, CoreInstance pkg, String sourceId, Context context, ValidatorState validatorState, ProcessorSupport processorSupport) throws PureCompilationException
    {
        validatePackageAndSourceVisibility(propertyTree.getValueForMetaPropertyToOne(M3Properties.property).getSourceInformation(), pkg, sourceId, context, validatorState, processorSupport, (AbstractProperty<?>) Instance.getValueForMetaPropertyToOneResolved(propertyTree, M3Properties.property, processorSupport));
        CoreInstance subTypeClass = Instance.getValueForMetaPropertyToOneResolved(propertyTree, "subType", processorSupport);
        if (subTypeClass != null)
        {
            validatePackageAndSourceVisibility(propertyTree.getValueForMetaPropertyToOne("subType").getSourceInformation(), pkg, sourceId, context, validatorState, processorSupport, (Class<?>) subTypeClass);
        }
        Instance.getValueForMetaPropertyToManyResolved(propertyTree, "subTrees", processorSupport).forEach(subTree -> validatePropertyGraphFetchTree(subTree, pkg, sourceId, context, validatorState, processorSupport));
    }

    private static void throwAccessException(SourceInformation sourceInfo, CoreInstance element, CoreInstance pkg, ProcessorSupport processorSupport) throws PureCompilationException
    {
        throw new PureCompilationException(sourceInfo, getElementNameForExceptionMessage(element, processorSupport) + " is not accessible in " + PackageableElement.getUserPathForPackageableElement(pkg, "::"));
    }

    private static void throwRepoVisibilityException(SourceInformation sourceInfo, CoreInstance element, String sourceId, ProcessorSupport processorSupport) throws PureCompilationException
    {
        throw new PureCompilationException(sourceInfo, getElementNameForExceptionMessage(element, processorSupport) + " is not visible in the file " + sourceId);
    }

    private static String getElementNameForExceptionMessage(CoreInstance element, ProcessorSupport processorSupport)
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
