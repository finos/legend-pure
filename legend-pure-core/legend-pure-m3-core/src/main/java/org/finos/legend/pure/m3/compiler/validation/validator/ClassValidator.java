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

package org.finos.legend.pure.m3.compiler.validation.validator;

import org.eclipse.collections.api.RichIterable;
import org.eclipse.collections.api.factory.Lists;
import org.eclipse.collections.api.factory.Maps;
import org.eclipse.collections.api.list.ListIterable;
import org.eclipse.collections.api.list.MutableList;
import org.eclipse.collections.api.map.MapIterable;
import org.eclipse.collections.api.map.MutableMap;
import org.finos.legend.pure.m3.compiler.Context;
import org.finos.legend.pure.m3.compiler.validation.Validator;
import org.finos.legend.pure.m3.compiler.validation.ValidatorState;
import org.finos.legend.pure.m3.compiler.validation.VisibilityValidation;
import org.finos.legend.pure.m3.coreinstance.meta.pure.metamodel.function.property.QualifiedProperty;
import org.finos.legend.pure.m3.coreinstance.meta.pure.metamodel.multiplicity.Multiplicity;
import org.finos.legend.pure.m3.coreinstance.meta.pure.metamodel.type.Class;
import org.finos.legend.pure.m3.coreinstance.meta.pure.metamodel.type.FunctionType;
import org.finos.legend.pure.m3.coreinstance.meta.pure.metamodel.type.generics.GenericType;
import org.finos.legend.pure.m3.coreinstance.meta.pure.metamodel.valuespecification.VariableExpression;
import org.finos.legend.pure.m3.navigation.Instance;
import org.finos.legend.pure.m3.navigation.M3Paths;
import org.finos.legend.pure.m3.navigation.M3Properties;
import org.finos.legend.pure.m3.navigation.PackageableElement.PackageableElement;
import org.finos.legend.pure.m3.navigation.ProcessorSupport;
import org.finos.legend.pure.m3.navigation.function.FunctionDescriptor;
import org.finos.legend.pure.m3.navigation.type.Type;
import org.finos.legend.pure.m3.tools.matcher.MatchRunner;
import org.finos.legend.pure.m3.tools.matcher.Matcher;
import org.finos.legend.pure.m3.tools.matcher.MatcherState;
import org.finos.legend.pure.m4.ModelRepository;
import org.finos.legend.pure.m4.coreinstance.CoreInstance;
import org.finos.legend.pure.m4.coreinstance.SourceInformation;
import org.finos.legend.pure.m4.exception.PureCompilationException;

public class ClassValidator implements MatchRunner<Class<?>>
{
    @Override
    public String getClassName()
    {
        return M3Paths.Class;
    }

    @Override
    public void run(Class<?> cls, MatcherState state, Matcher matcher, ModelRepository modelRepository, Context context) throws PureCompilationException
    {
        ValidatorState validatorState = (ValidatorState) state;
        ProcessorSupport processorSupport = validatorState.getProcessorSupport();
        validateTypeVariables(cls, processorSupport);
        cls._properties().forEach(p -> Validator.validate(p, validatorState, matcher, processorSupport));
        cls._qualifiedProperties().forEach(qp -> Validator.validate(qp, validatorState, matcher, processorSupport));
        Validator.testProperties(cls, validatorState, matcher, processorSupport);
        validatePropertyOverrides(cls, processorSupport);
        VisibilityValidation.validateClass(cls, context, validatorState, processorSupport);
        MilestoningClassValidator.validateTemporalStereotypesAppliedForAllSubTypesInTemporalHierarchy(cls, processorSupport);
    }

    private void validateTypeVariables(Class<?> cls, ProcessorSupport processorSupport)
    {
        // Validate there are no name conflicts with properties
        MapIterable<String, CoreInstance> propertiesByName = processorSupport.class_getSimplePropertiesByName(cls);
        cls._typeVariables().forEach(v ->
        {
            String name = v._name();
            CoreInstance property = propertiesByName.get(name);
            if (property != null)
            {
                StringBuilder builder = new StringBuilder("Type variable '").append(name).append("' conflicts with the property ").append(name).append(':');
                org.finos.legend.pure.m3.navigation.generictype.GenericType.print(builder, property.getValueForMetaPropertyToOne(M3Properties.genericType), true, processorSupport);
                org.finos.legend.pure.m3.navigation.multiplicity.Multiplicity.print(builder, Instance.getValueForMetaPropertyToOneResolved(property, M3Properties.multiplicity, processorSupport), true);
                SourceInformation propSourceInfo = property.getSourceInformation();
                if (propSourceInfo != null)
                {
                    propSourceInfo.appendMessage(builder.append(" at "));
                }
                throw new PureCompilationException(v.getSourceInformation(), builder.toString());
            }
        });

        // Validate there are no name conflicts with other type variables
        MutableMap<String, VariableExpression> typeVariablesByName = Maps.mutable.empty();
        cls._typeVariables().forEach(v ->
        {
            VariableExpression old = typeVariablesByName.put(v._name(), v);
            if (old != null)
            {
                StringBuilder builder = new StringBuilder("Type variable '").append(v._name()).append("' is already defined");
                SourceInformation oldSourceInfo = old.getSourceInformation();
                if (oldSourceInfo != null)
                {
                    oldSourceInfo.appendMessage(builder.append(" (at ")).append(')');
                }
                throw new PureCompilationException(v.getSourceInformation(), builder.toString());
            }
        });

        Type.getGeneralizationResolutionOrder(cls, processorSupport).drop(1).forEach(genl ->
        {
            if (genl instanceof Class)
            {
                ((Class<?>) genl)._typeVariables().forEach(v ->
                {
                    VariableExpression conflict = typeVariablesByName.get(v._name());
                    if (conflict != null)
                    {
                        StringBuilder builder = new StringBuilder("Type variable '").append(conflict._name()).append("' is already defined in supertype ");
                        PackageableElement.writeUserPathForPackageableElement(builder, genl);
                        SourceInformation oldSourceInfo = v.getSourceInformation();
                        if (oldSourceInfo != null)
                        {
                            oldSourceInfo.appendMessage(builder.append(" at "));
                        }
                        throw new PureCompilationException(conflict.getSourceInformation(), builder.toString());
                    }
                });
            }
        });
    }

    /**
     * Validate any property overrides for cls.  Simple properties may be
     * overridden if the type and multiplicity are the same.  Qualified
     * properties may be overridden on three conditions.  First, the return
     * type and multiplicity must be at least as specific.  Second, the
     * number of parameters must be the same.  Third, for each parameter
     * the type and multiplicity must be at least as general.
     *
     * @param cls Pure class
     * @throws PureCompilationException if there is an invalid property override
     */
    private void validatePropertyOverrides(Class<?> cls, ProcessorSupport processorSupport) throws PureCompilationException
    {
        ListIterable<CoreInstance> generalizations = Type.getGeneralizationResolutionOrder(cls, processorSupport);
        int size = generalizations.size();
        if (size > 1)
        {
            for (int i = 0; i < size; i++)
            {
                Class<?> spec = (Class<?>) generalizations.get(i);
                MutableList<CoreInstance> specProperties = Lists.mutable.<CoreInstance>withAll(spec._properties()).withAll(spec._propertiesFromAssociations());
                for (int j = 0, specPropCount = specProperties.size(); j < specPropCount; j++)
                {
                    CoreInstance specProperty = specProperties.get(j);
                    String name = org.finos.legend.pure.m3.navigation.property.Property.getPropertyName(specProperty);

                    // Check for other properties with the same name on the same class
                    for (int k = j + 1; k < specPropCount; k++)
                    {
                        CoreInstance otherSpecProperty = specProperties.get(k);
                        String otherName = org.finos.legend.pure.m3.navigation.property.Property.getPropertyName(otherSpecProperty);
                        if (name.equals(otherName))
                        {
                            throw new PureCompilationException(cls.getSourceInformation(), "Property conflict on class " + spec.getName() + ": property '" + name + "' defined more than once");
                        }
                    }

                    // Check for other properties with the same name on generalizations
                    for (int k = i + 1; k < size; k++)
                    {
                        Class<?> genl = (Class<?>) generalizations.get(k);
                        MutableList<CoreInstance> genlProperties = Lists.mutable.<CoreInstance>withAll(genl._properties()).withAll(genl._propertiesFromAssociations());
                        for (CoreInstance genlProperty : genlProperties)
                        {
                            if (name.equals(org.finos.legend.pure.m3.navigation.property.Property.getPropertyName(genlProperty)) && !isPropertyOverrideValid(specProperty, genlProperty, processorSupport))
                            {
                                throwPropertyConflictException(name, cls, spec, genl);
                            }
                        }
                    }
                }

                MutableList<QualifiedProperty<?>> specQualifiedProperties = Lists.mutable.<QualifiedProperty<?>>withAll(spec._qualifiedProperties()).withAll(spec._qualifiedPropertiesFromAssociations());
                for (int j = 0, specQualifiedPropCount = specQualifiedProperties.size(); j < specQualifiedPropCount; j++)
                {
                    QualifiedProperty<?> specQualifiedProperty = specQualifiedProperties.get(j);
                    String name = org.finos.legend.pure.m3.navigation.property.Property.getPropertyName(specQualifiedProperty);

                    // Check for other qualified properties with the same name on the same class
                    String srcSignature = FunctionDescriptor.getFunctionDescriptor(specQualifiedProperty, processorSupport);
                    for (int k = j + 1; k < specQualifiedPropCount; k++)
                    {
                        QualifiedProperty<?> otherSpecQualifiedProperty = specQualifiedProperties.get(k);
                        String otherSignature = FunctionDescriptor.getFunctionDescriptor(otherSpecQualifiedProperty, processorSupport);
                        if (srcSignature.equals(otherSignature))
                        {
                            throw new PureCompilationException(cls.getSourceInformation(), "Property conflict on class " + spec.getName() + ": qualified property '" + name + "' defined more than once");
                        }
                    }

                    // Check for other qualified properties with the same name on generalizations
                    for (int k = i + 1; k < size; k++)
                    {
                        Class<?> genl = (Class<?>) generalizations.get(k);
                        QualifiedProperty<?> genlQualifiedProperty = getSourcePropertyOverrideByNameAndArgCount(specQualifiedProperty, genl, name, processorSupport);
                        if ((genlQualifiedProperty != null) && !isQualifiedPropertyOverrideValid(specQualifiedProperty, genlQualifiedProperty, processorSupport))
                        {
                            throwPropertyConflictException(name, cls, spec, genl);
                        }
                    }
                }
            }
        }
    }

    private QualifiedProperty<?> getSourcePropertyOverrideByNameAndArgCount(QualifiedProperty<?> specProperty, Class<?> genClass, String name, ProcessorSupport processorSupport)
    {
        FunctionType specFunctionType = (FunctionType) processorSupport.function_getFunctionType(specProperty);
        RichIterable<? extends VariableExpression> specParameters = specFunctionType._parameters();
        return Lists.mutable.<QualifiedProperty<?>>withAll(genClass._qualifiedProperties()).withAll(genClass._qualifiedPropertiesFromAssociations())
                .detect(qp -> name.equals(qp._name()) && (specParameters.size() == ((FunctionType) processorSupport.function_getFunctionType(qp))._parameters().size()));
    }

    private boolean isPropertyOverrideValid(CoreInstance specProperty, CoreInstance genlProperty, ProcessorSupport processorSupport)
    {
        FunctionType specFunctionType = (FunctionType) processorSupport.function_getFunctionType(specProperty);
        GenericType specReturnType = specFunctionType._returnType();
        Multiplicity specReturnMultiplicity = specFunctionType._returnMultiplicity();

        FunctionType genlFunctionType = (FunctionType) processorSupport.function_getFunctionType(genlProperty);
        GenericType genlReturnType = genlFunctionType._returnType();
        Multiplicity genlReturnMultiplicity = genlFunctionType._returnMultiplicity();

        // Return type and multiplicity must be the same between specProperty and genlProperty
        return org.finos.legend.pure.m3.navigation.generictype.GenericType.genericTypesEqual(specReturnType, genlReturnType, processorSupport) && org.finos.legend.pure.m3.navigation.multiplicity.Multiplicity.multiplicitiesEqual(specReturnMultiplicity, genlReturnMultiplicity);
    }

    private boolean isQualifiedPropertyOverrideValid(QualifiedProperty<?> specProperty, QualifiedProperty<?> genlProperty, ProcessorSupport processorSupport)
    {
        FunctionType specFunctionType = (FunctionType) processorSupport.function_getFunctionType(specProperty);
        FunctionType genlFunctionType = (FunctionType) processorSupport.function_getFunctionType(genlProperty);

        // Return type of specProperty must be the same as genlProperty or a specialization
        GenericType specReturnType = specFunctionType._returnType();
        GenericType genlReturnType = genlFunctionType._returnType();
        if (!org.finos.legend.pure.m3.navigation.generictype.GenericType.isGenericCompatibleWith(specReturnType, genlReturnType, processorSupport))
        {
            return false;
        }

        // Return multiplicity of specProperty must be subsumed by multiplicity of genlProperty
        Multiplicity specReturnMultiplicity = specFunctionType._returnMultiplicity();
        Multiplicity genlReturnMultiplicity = genlFunctionType._returnMultiplicity();
        if (!org.finos.legend.pure.m3.navigation.multiplicity.Multiplicity.subsumes(genlReturnMultiplicity, specReturnMultiplicity))
        {
            return false;
        }

        // specProperty must have the same number of parameters as genlProperty
        ListIterable<? extends VariableExpression> specParameters = specFunctionType._parameters().toList();
        ListIterable<? extends VariableExpression> genlParameters = genlFunctionType._parameters().toList();
        int parameterCount = specParameters.size();
        if (parameterCount != genlParameters.size())
        {
            return false;
        }

        for (int i = 1; i < parameterCount; i++)
        {
            VariableExpression specParameter = specParameters.get(i);
            VariableExpression genlParameter = genlParameters.get(i);

            // Parameter type for specProperty must be the same as genlProperty or a generalization
            GenericType specParamGenericType = specParameter._genericType();
            GenericType genlParamGenericType = genlParameter._genericType();
            if (!org.finos.legend.pure.m3.navigation.generictype.GenericType.isGenericCompatibleWith(genlParamGenericType, specParamGenericType, processorSupport))
            {
                return false;
            }

            // Parameter multiplicity for specProperty must subsume parameter multiplicity for genlProperty
            Multiplicity specParamMultiplicity = specParameter._multiplicity();
            Multiplicity genlParamMultiplicity = genlParameter._multiplicity();
            if (!org.finos.legend.pure.m3.navigation.multiplicity.Multiplicity.subsumes(specParamMultiplicity, genlParamMultiplicity))
            {
                return false;
            }
        }

        return true;
    }

    private void throwPropertyConflictException(String propName, Class<?> cls, Class<?> specCls, Class<?> genlCls) throws PureCompilationException
    {
        throw new PureCompilationException(cls.getSourceInformation(), "Property conflict on class " + cls.getName() + ": property '" + propName + "' defined on " + specCls.getName() + " conflicts with property '" + propName + "' defined on " + genlCls.getName());
    }
}
