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

import org.eclipse.collections.api.list.ListIterable;
import org.eclipse.collections.impl.factory.Lists;
import org.eclipse.collections.impl.factory.Maps;
import org.finos.legend.pure.m3.navigation.M3Paths;
import org.finos.legend.pure.m3.compiler.Context;
import org.finos.legend.pure.m3.navigation.generictype.GenericTypeWithXArguments;
import org.finos.legend.pure.m3.navigation.importstub.ImportStub;
import org.finos.legend.pure.m3.compiler.validation.Validator;
import org.finos.legend.pure.m3.compiler.validation.ValidatorState;
import org.finos.legend.pure.m3.compiler.validation.functionExpression.CopyValidator;
import org.finos.legend.pure.m3.compiler.validation.functionExpression.EnumValidator;
import org.finos.legend.pure.m3.compiler.validation.functionExpression.GetAllValidator;
import org.finos.legend.pure.m3.compiler.validation.functionExpression.GetAllVersionsInRangeValidator;
import org.finos.legend.pure.m3.compiler.validation.functionExpression.GetAllVersionsValidator;
import org.finos.legend.pure.m3.compiler.validation.functionExpression.NewValidator;
import org.finos.legend.pure.m3.compiler.validation.functionExpression.SubTypeValidator;
import org.finos.legend.pure.m3.coreinstance.meta.pure.metamodel.function.Function;
import org.finos.legend.pure.m3.coreinstance.meta.pure.metamodel.type.Class;
import org.finos.legend.pure.m3.coreinstance.meta.pure.metamodel.type.FunctionType;
import org.finos.legend.pure.m3.coreinstance.meta.pure.metamodel.type.Type;
import org.finos.legend.pure.m3.coreinstance.meta.pure.metamodel.type.generics.GenericType;
import org.finos.legend.pure.m3.coreinstance.meta.pure.metamodel.type.generics.TypeParameter;
import org.finos.legend.pure.m3.coreinstance.meta.pure.metamodel.valuespecification.FunctionExpression;
import org.finos.legend.pure.m3.coreinstance.meta.pure.metamodel.valuespecification.ValueSpecification;
import org.finos.legend.pure.m3.coreinstance.meta.pure.metamodel.valuespecification.VariableExpression;
import org.finos.legend.pure.m3.navigation.ProcessorSupport;
import org.finos.legend.pure.m3.tools.matcher.MatchRunner;
import org.finos.legend.pure.m3.tools.matcher.Matcher;
import org.finos.legend.pure.m3.tools.matcher.MatcherState;
import org.finos.legend.pure.m4.coreinstance.CoreInstance;
import org.finos.legend.pure.m4.ModelRepository;
import org.finos.legend.pure.m4.exception.PureCompilationException;

public class FunctionExpressionValidator implements MatchRunner<FunctionExpression>
{
    @Override
    public String getClassName()
    {
        return M3Paths.FunctionExpression;
    }

    @Override
    public void run(FunctionExpression instance, MatcherState state, Matcher matcher, ModelRepository modelRepository, Context context) throws PureCompilationException
    {
        validateFunctionExpression(matcher, (ValidatorState)state, instance, modelRepository, state.getProcessorSupport());
    }

    public static void validateFunctionExpression(Matcher matcher, ValidatorState validatorState, FunctionExpression instance, ModelRepository repository, ProcessorSupport processorSupport) throws PureCompilationException
    {
        Function function = (Function)ImportStub.withImportStubByPass(instance._funcCoreInstance(), processorSupport);
        if (function == null)
        {
            throw new RuntimeException("No function found to the expression:\n" + instance.print(""));
        }
        if ("new_Class_1__String_1__KeyExpression_MANY__T_1_".equals(function.getName()) || "new_Class_1__String_1__T_1_".equals(function.getName()))
        {
            NewValidator.validateNew(matcher, validatorState, instance, processorSupport);
        }
        if ("copy_T_1__String_1__KeyExpression_MANY__T_1_".equals(function.getName()) || "copy_T_1__String_1__T_1_".equals(function.getName()))
        {
            CopyValidator.validateCopy(matcher, validatorState, instance, processorSupport);
        }
        if ("extractEnumValue_Enumeration_1__String_1__T_1_".equals(function.getName()))
        {
            EnumValidator.validateEnum(instance, processorSupport);
        }
        if ("subType_Any_m__T_1__T_m_".equals(function.getName()) || "whenSubType_Any_1__T_1__T_$0_1$_".equals(function.getName()) || "whenSubType_Any_$0_1$__T_1__T_$0_1$_".equals(function.getName()) || "whenSubType_Any_MANY__T_1__T_MANY_".equals(function.getName()))
        {
            SubTypeValidator.validateSubType(instance, processorSupport);
        }
        if (function.getName().startsWith("getAll_Class_1__"))
        {
            GetAllValidator.validate(instance, processorSupport);
        }
        if (function.getName().startsWith("getAllVersions_Class_1__"))
        {
            GetAllVersionsValidator.validate(instance, processorSupport);
        }
        if (function.getName().startsWith("getAllVersionsInRange_Class_1__"))
        {
            GetAllVersionsInRangeValidator.validate(instance, processorSupport);
        }

        MilestoningFunctionExpressionValidator.validateFunctionExpression(instance, function, repository, processorSupport);

        FunctionType functionType = (FunctionType)processorSupport.function_getFunctionType(ImportStub.withImportStubByPass(instance._funcCoreInstance(), processorSupport));
        ListIterable<? extends VariableExpression> parameters = (ListIterable<? extends VariableExpression>)functionType._parameters();
        ListIterable<? extends ValueSpecification> parametersValues = (ListIterable<? extends ValueSpecification>)instance._parametersValues();
        int i = 0;
        for (ValueSpecification inst : parametersValues)
        {
            Validator.validate(inst, validatorState,  matcher, processorSupport);
            GenericType genericType = inst._genericType();
            Validator.validate(genericType, validatorState, matcher, processorSupport);

            if (genericType._rawTypeCoreInstance() != processorSupport.package_getByUserPath(M3Paths.Nil))
            {
                ListIterable<? extends GenericType> funcP = (ListIterable<? extends GenericType>)parameters.get(i)._genericType()._typeArguments();
                GenericType givenInstanceGenericType;
                if (!funcP.isEmpty() && org.finos.legend.pure.m3.navigation.generictype.GenericType.isGenericTypeConcrete(parameters.get(i)._genericType(), processorSupport) &&
                    org.finos.legend.pure.m3.navigation.generictype.GenericType.isGenericTypeConcrete(inst._genericType(), processorSupport))
                {
                    // This line should actually return a Pair (including the resolved multiplicities)
                    GenericTypeWithXArguments homogenizedTypeArgs = org.finos.legend.pure.m3.navigation.generictype.GenericType.resolveClassTypeParameterUsingInheritance(inst._genericType(), parameters.get(i)._genericType(), processorSupport);
                    // The empty multiplicities bellow should come from the Pair
                    givenInstanceGenericType = (GenericType)org.finos.legend.pure.m3.navigation.generictype.GenericType.makeTypeArgumentAsConcreteAsPossible(homogenizedTypeArgs.getGenericType(), homogenizedTypeArgs.getArgumentsByParameterName(), Maps.immutable.<String, CoreInstance>of(), processorSupport);
                }
                else
                {
                    givenInstanceGenericType = inst._genericType();
                }
                ListIterable<? extends GenericType> bound = (ListIterable<? extends GenericType>)givenInstanceGenericType._typeArguments();

                if (funcP.size() > bound.size())
                {
                    throw new RuntimeException("Type Arguments mismatch of a function parameter. Function:'" + ImportStub.withImportStubByPass(instance._funcCoreInstance(), processorSupport).getName() + "'" +
                                               " / Param:'"+parameters.get(i)._name()+":"+ org.finos.legend.pure.m3.navigation.generictype.GenericType.print(parameters.get(i)._genericType(), processorSupport)
                            +"' / Given Instance Type:'"+ org.finos.legend.pure.m3.navigation.generictype.GenericType.print(inst._genericType(), processorSupport)+"'");
                }
                for (int k = 0; k < funcP.size(); k++)
                {
                    if (ImportStub.withImportStubByPass(funcP.get(k)._rawTypeCoreInstance(), processorSupport) != null)
                    {
                        Type typeArgument1 = (Type)ImportStub.withImportStubByPass(funcP.get(k)._rawTypeCoreInstance(), processorSupport);
                        if ("FunctionType".equals(typeArgument1.getClassifier().getName()))
                        {
                            if (!org.finos.legend.pure.m3.navigation.generictype.GenericType.isGenericCompatibleWith(inst._genericType(), parameters.get(i)._genericType(), processorSupport))
                            {
                                throw new PureCompilationException(instance.getSourceInformation(), "typeArgument mismatch! Expected:" + org.finos.legend.pure.m3.navigation.generictype.GenericType.print(parameters.get(i)._genericType(), processorSupport)
                                        + " Found:" + org.finos.legend.pure.m3.navigation.generictype.GenericType.print(inst._genericType(), processorSupport));
                            }
                        }
                        else
                        {
                            Type type = (Type)ImportStub.withImportStubByPass(givenInstanceGenericType._rawTypeCoreInstance(), processorSupport);
                            ListIterable<? extends TypeParameter> typeParameters = type instanceof Class ? (ListIterable<? extends TypeParameter>)((Class)type)._typeParameters() : Lists.fixedSize.<TypeParameter>empty();
                            boolean covariant = org.finos.legend.pure.m3.navigation.typeparameter.TypeParameter.isCovariant(typeParameters.get(k));
                            Type typeArgument2 = (Type)ImportStub.withImportStubByPass(bound.get(k)._rawTypeCoreInstance(), processorSupport);

                            if (typeArgument2 != null && (covariant?!org.finos.legend.pure.m3.navigation.type.Type.subTypeOf(typeArgument2, typeArgument1, processorSupport):!org.finos.legend.pure.m3.navigation.type.Type.subTypeOf(typeArgument1, typeArgument2, processorSupport)))
                            {
                                if (!("Any".equals(typeArgument1.getName()) &&  "FunctionType".equals(typeArgument2.getClassifier().getName())))
                                {
                                    throw new PureCompilationException(instance.getSourceInformation(), function.getName()+" "+" / typeArgument mismatch! Expected:" + org.finos.legend.pure.m3.navigation.generictype.GenericType.print(parameters.get(i)._genericType(), processorSupport)
                                            + " Found:" + org.finos.legend.pure.m3.navigation.generictype.GenericType.print(inst._genericType(), processorSupport));
                                }
                            }
                        }
                    }
                }
            }

            i++;
        }
    }

}
