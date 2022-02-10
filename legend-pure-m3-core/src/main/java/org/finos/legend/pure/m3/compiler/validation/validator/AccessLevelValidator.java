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
import org.finos.legend.pure.m3.navigation.M3Paths;
import org.finos.legend.pure.m3.navigation.M3Properties;
import org.finos.legend.pure.m3.compiler.Context;
import org.finos.legend.pure.m3.navigation.Instance;
import org.finos.legend.pure.m3.navigation.function.FunctionDescriptor;
import org.finos.legend.pure.m3.navigation.importstub.ImportStub;
import org.finos.legend.pure.m3.compiler.visibility.AccessLevel;
import org.finos.legend.pure.m3.coreinstance.meta.pure.metamodel.PackageableElement;
import org.finos.legend.pure.m3.coreinstance.meta.pure.metamodel.extension.ElementWithStereotypes;
import org.finos.legend.pure.m3.coreinstance.meta.pure.metamodel.extension.ElementWithStereotypesCoreInstanceWrapper;
import org.finos.legend.pure.m3.coreinstance.meta.pure.metamodel.extension.Stereotype;
import org.finos.legend.pure.m3.coreinstance.meta.pure.metamodel.function.ConcreteFunctionDefinition;
import org.finos.legend.pure.m3.coreinstance.meta.pure.metamodel.function.Function;
import org.finos.legend.pure.m3.coreinstance.meta.pure.metamodel.function.property.AbstractProperty;
import org.finos.legend.pure.m3.coreinstance.meta.pure.metamodel.multiplicity.Multiplicity;
import org.finos.legend.pure.m3.coreinstance.meta.pure.metamodel.type.Class;
import org.finos.legend.pure.m3.coreinstance.meta.pure.metamodel.type.FunctionType;
import org.finos.legend.pure.m3.coreinstance.meta.pure.metamodel.type.PrimitiveType;
import org.finos.legend.pure.m3.coreinstance.meta.pure.metamodel.type.generics.GenericType;
import org.finos.legend.pure.m3.coreinstance.meta.pure.metamodel.valuespecification.VariableExpression;
import org.finos.legend.pure.m3.navigation.ProcessorSupport;
import org.finos.legend.pure.m3.navigation.PrimitiveUtilities;
import org.finos.legend.pure.m3.tools.matcher.MatchRunner;
import org.finos.legend.pure.m3.tools.matcher.Matcher;
import org.finos.legend.pure.m3.tools.matcher.MatcherState;
import org.finos.legend.pure.m4.coreinstance.CoreInstance;
import org.finos.legend.pure.m4.ModelRepository;
import org.finos.legend.pure.m4.exception.PureCompilationException;
import org.finos.legend.pure.m4.coreinstance.indexing.IndexSpecifications;

public class AccessLevelValidator implements MatchRunner
{
    @Override
    public String getClassName()
    {
        return M3Paths.ElementWithStereotypes;
    }

    @Override
    public void run(CoreInstance instance, MatcherState state, Matcher matcher, ModelRepository repository, Context context) throws PureCompilationException
    {
        ElementWithStereotypes elementWithStereotypes = ElementWithStereotypesCoreInstanceWrapper.toElementWithStereotypes(instance);
        ProcessorSupport processorSupport = state.getProcessorSupport();
        ListIterable<Stereotype> stereotypes = AccessLevel.getAccessLevelStereotypes(elementWithStereotypes, processorSupport);
        switch (stereotypes.size())
        {
            case 0:
            {
                // Do nothing
                break;
            }
            case 1:
            {
                this.validateExplicitAccessLevel(elementWithStereotypes, AccessLevel.getAccessLevel(elementWithStereotypes, context, processorSupport), processorSupport);
                break;
            }
            default:
            {
                StringBuilder message = new StringBuilder();
                if (Instance.instanceOf(instance, M3Paths.Function, processorSupport))
                {
                    FunctionDescriptor.writeFunctionDescriptor(message, instance, processorSupport);
                }
                else
                {
                    org.finos.legend.pure.m3.navigation.PackageableElement.PackageableElement.writeUserPathForPackageableElement(message, instance);
                }
                message.append(" has multiple access level stereotypes");
                throw new PureCompilationException(instance.getSourceInformation(), message.toString());
            }
        }
    }

    private void validateExplicitAccessLevel(ElementWithStereotypes instance, AccessLevel accessLevel, ProcessorSupport processorSupport)
    {
        if (accessLevel == AccessLevel.EXTERNALIZABLE)
        {
            // Validate that instance is a non-property concrete function definition
            if (!(instance instanceof ConcreteFunctionDefinition) || instance instanceof AbstractProperty)
            {
                throw new PureCompilationException(instance.getSourceInformation(), "Only functions may have an access level of " + accessLevel.getName());
            }

            // Validate that the function has a name and package
            String functionName = ((ConcreteFunctionDefinition)instance)._functionName();
            if (functionName == null)
            {
                throw new PureCompilationException(instance.getSourceInformation(), "Functions with access level " + accessLevel.getName() + " must have a function name");
            }
            if (((ConcreteFunctionDefinition)instance)._package() == null)
            {
                throw new PureCompilationException(instance.getSourceInformation(), "Functions with access level " + accessLevel.getName() + " must have a package");
            }

            // Validate parameter types and multiplicities
            FunctionType functionType = (FunctionType)processorSupport.function_getFunctionType(instance);
            int i = 1;
            for (VariableExpression parameter : functionType._parameters())
            {
                GenericType genericType = parameter._genericType();
                CoreInstance rawType = ImportStub.withImportStubByPass(genericType._rawTypeCoreInstance(), processorSupport);
                if (!(rawType instanceof PrimitiveType || M3Paths.Map.equals(org.finos.legend.pure.m3.navigation.PackageableElement.PackageableElement.getUserPathForPackageableElement(rawType))))
                {
                    StringBuilder message = new StringBuilder("Functions with access level ");
                    message.append(accessLevel.getName());
                    message.append(" may only have primitive types or 'Maps' as parameter types; found ");
                    org.finos.legend.pure.m3.navigation.generictype.GenericType.print(message, genericType, processorSupport);
                    message.append(" for the type of parameter ");
                    String name = parameter._name();
                    if (name.isEmpty())
                    {
                        message.append(i);
                    }
                    else
                    {
                        message.append("'");
                        message.append(name);
                        message.append("'");
                    }
                    throw new PureCompilationException(instance.getSourceInformation(), message.toString());
                }
                Multiplicity multiplicity = parameter._multiplicity();
                if (!org.finos.legend.pure.m3.navigation.multiplicity.Multiplicity.isToOne(multiplicity, false) && !org.finos.legend.pure.m3.navigation.multiplicity.Multiplicity.isZeroToMany(multiplicity))
                {
                    StringBuilder message = new StringBuilder("Functions with access level ");
                    message.append(accessLevel.getName());
                    message.append(" may only have parameters with multiplicity 0..1, 1, or *; found ");
                    org.finos.legend.pure.m3.navigation.multiplicity.Multiplicity.print(message, multiplicity, false);
                    message.append(" for the multiplicity of parameter ");
                    String name = PrimitiveUtilities.getStringValue(parameter.getValueForMetaPropertyToOne(M3Properties.name));
                    if (name.isEmpty())
                    {
                        message.append(i);
                    }
                    else
                    {
                        message.append("'");
                        message.append(name);
                        message.append("'");
                    }
                    throw new PureCompilationException(instance.getSourceInformation(), message.toString());
                }
                i++;
            }

            // Validate return type and multiplicity
            GenericType returnType = functionType._returnType();
            CoreInstance returnRawType = ImportStub.withImportStubByPass(returnType._rawTypeCoreInstance(), processorSupport);
            if ((returnRawType == null) || !(returnRawType instanceof PrimitiveType))
            {
                StringBuilder message = new StringBuilder("Functions with access level ").append(accessLevel.getName()).append(" may only have primitive types as return types; found ");
                org.finos.legend.pure.m3.navigation.generictype.GenericType.print(message, returnType, processorSupport);
                throw new PureCompilationException(instance.getSourceInformation(), message.toString());
            }

            // Validate no function name conflict
            Stereotype stereotype = (Stereotype)accessLevel.getStereotype(processorSupport);
            ListIterable<? extends CoreInstance> functionsWithSameName = stereotype.getValueInValueForMetaPropertyToManyByIndex(M3Properties.modelElements, IndexSpecifications.getPropertyValueNameIndexSpec(M3Properties.functionName), functionName);
            if (functionsWithSameName.size() > 1)
            {
                StringBuilder message = new StringBuilder("Externalizable function name conflict - multiple functions with the name '");
                message.append(functionName).append("':");
                for (CoreInstance func : functionsWithSameName.toSortedListBy(CoreInstance::getSourceInformation))
                {
                    FunctionDescriptor.writeFunctionDescriptor(message.append("\n\t"), func, processorSupport);
                    func.getSourceInformation().appendMessage(message.append(" (")).append(')');
                }
                throw new PureCompilationException(instance.getSourceInformation(), message.toString());
            }
        }
        else
        {
            // Check that instance is either a class or a non-property function
            if (instance instanceof AbstractProperty ||
                    !(instance instanceof Function || instance instanceof Class))
            {
                throw new PureCompilationException(instance.getSourceInformation(), "Only classes and functions may have an access level");
            }
        }

        // Check that instance has a package
        if (((PackageableElement)instance)._package() == null)
        {
            throw new PureCompilationException(instance.getSourceInformation(), "Element '" + instance + "' has an access level but no package");
        }
    }
}
