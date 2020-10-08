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

package org.finos.legend.pure.m3.compiler.postprocessing.inference;

import org.eclipse.collections.api.list.ListIterable;
import org.eclipse.collections.api.list.MutableList;
import org.eclipse.collections.api.map.MapIterable;
import org.eclipse.collections.api.map.MutableMap;
import org.eclipse.collections.api.set.MutableSet;
import org.eclipse.collections.impl.factory.Lists;
import org.eclipse.collections.impl.factory.Maps;
import org.eclipse.collections.impl.list.mutable.FastList;
import org.eclipse.collections.impl.set.mutable.UnifiedSet;
import org.finos.legend.pure.m3.navigation.function.FunctionType;
import org.finos.legend.pure.m3.navigation.generictype.GenericTypeWithXArguments;
import org.finos.legend.pure.m3.navigation.importstub.ImportStub;
import org.finos.legend.pure.m3.navigation.type.Type;
import org.finos.legend.pure.m3.navigation.typeparameter.TypeParameter;
import org.finos.legend.pure.m3.coreinstance.meta.pure.metamodel.function.Function;
import org.finos.legend.pure.m3.coreinstance.meta.pure.metamodel.multiplicity.Multiplicity;
import org.finos.legend.pure.m3.coreinstance.meta.pure.metamodel.type.Class;
import org.finos.legend.pure.m3.coreinstance.meta.pure.metamodel.type.generics.GenericType;
import org.finos.legend.pure.m3.coreinstance.meta.pure.metamodel.valuespecification.FunctionExpression;
import org.finos.legend.pure.m3.coreinstance.meta.pure.metamodel.valuespecification.VariableExpression;
import org.finos.legend.pure.m3.navigation.ProcessorSupport;
import org.finos.legend.pure.m4.coreinstance.CoreInstance;

import java.io.IOException;
import java.util.Iterator;
import java.util.List;
import java.util.Objects;
import java.util.concurrent.atomic.AtomicInteger;

public class TypeInferenceContext
{
    private static final AtomicInteger counter = new AtomicInteger(0);

    private final int id;
    private final TypeInferenceContext parent;
    private final ProcessorSupport processorSupport;
    private CoreInstance scope;
    private MutableList<TypeInferenceContextState> states = FastList.newList();

    private MutableSet<String> tops = UnifiedSet.newSet();

    public TypeInferenceContext(CoreInstance owner, ProcessorSupport processorSupport)
    {
        this.id = counter.incrementAndGet();
        this.states.add(new TypeInferenceContextState());
        this.parent = null;
        this.processorSupport = processorSupport;
        if (owner != null)
        {
            ListIterable<? extends org.finos.legend.pure.m3.coreinstance.meta.pure.metamodel.type.generics.TypeParameter> typeParameters = Lists.immutable.empty();
            if (owner instanceof Class)
            {
                typeParameters = ((Class)owner)._typeParameters().toList();
            }
            else if (owner instanceof org.finos.legend.pure.m3.coreinstance.meta.pure.metamodel.type.FunctionType)
            {
                typeParameters = ((org.finos.legend.pure.m3.coreinstance.meta.pure.metamodel.type.FunctionType)owner)._typeParameters().toList();
            }
            for (org.finos.legend.pure.m3.coreinstance.meta.pure.metamodel.type.generics.TypeParameter typeParameter : typeParameters)
            {
                String paramName = typeParameter._name();
                this.states.getLast().putTypeParameterValue(paramName, TypeParameter.wrapGenericType(typeParameter, processorSupport), this, true);
                this.tops.add(paramName);
            }
        }
    }

    public TypeInferenceContext(TypeInferenceContext parent, ProcessorSupport processorSupport)
    {
        this.id = counter.incrementAndGet();
        this.states.add(new TypeInferenceContextState());
        this.parent = parent;
        this.processorSupport = processorSupport;
    }

    public String getId()
    {
        return Integer.toHexString(id);
    }

    public void addStateForCollectionElement()
    {
        this.states.add(this.states.getFirst().copy());
    }

    public MutableList<TypeInferenceContextState> drop(int size)
    {
        MutableList<TypeInferenceContextState> res = this.states.subList(this.states.size() - size - 1, this.states.size());
        this.states = this.states.subList(0, this.states.size() - size);
        return res;
    }


    public boolean isTop(String typeParam)
    {
        TypeInferenceContext ctx = this;
        while (ctx.parent != null)
        {
            ctx = ctx.parent;
        }
        return ctx.tops.contains(typeParam);
    }

    public TypeInferenceContext getParent()
    {
        return this.parent;
    }

    public boolean isTypeParameterResolved(CoreInstance genericType)
    {
        if (org.finos.legend.pure.m3.navigation.generictype.GenericType.isGenericTypeConcrete(genericType, this.processorSupport))
        {
            return true;
        }
        ParameterValueWithFlag gf = this.states.getLast().getTypeParameterValueWithFlag(org.finos.legend.pure.m3.navigation.generictype.GenericType.getTypeParameterName(genericType, this.processorSupport));

        if (gf != null && (gf.isTerminal(this.processorSupport) || org.finos.legend.pure.m3.navigation.generictype.GenericType.isGenericTypeConcrete(gf.getParameterValue(), this.processorSupport)))
        {
            return true;
        }

        TypeInferenceContext top = getTopContext();
        if (top != null)
        {
            ParameterValueWithFlag parentGf = top.states.getLast().getTypeParameterValueWithFlag(org.finos.legend.pure.m3.navigation.generictype.GenericType.getTypeParameterName(genericType, this.processorSupport));
            return (parentGf != null && (parentGf.isTerminal(this.processorSupport) || org.finos.legend.pure.m3.navigation.generictype.GenericType.isGenericTypeConcrete(parentGf.getParameterValue(), this.processorSupport)));
        }
        return false;
    }

    public TypeInferenceContext getTopContext()
    {
        return this.parent == null? this : this.parent.getTopContext();
    }

    @Override
    public String toString()
    {
        StringBuilder builder = new StringBuilder(128);
        print(builder);
        return builder.toString();
    }

    public void print(Appendable appendable)
    {
        try
        {
            appendable.append("##>");
            printRecursive(appendable);
            appendable.append("<##");
        }
        catch (IOException e)
        {
            throw new RuntimeException(e);
        }
    }

    private void printRecursive(Appendable appendable) throws IOException
    {
        appendable.append('[');
        appendable.append(getId());
        appendable.append(":");
        printScope(appendable);
        appendable.append(" ~ ");
        printStates(appendable);
        appendable.append(']');
        if (this.parent != null)
        {
            appendable.append(" -> ");
            this.parent.printRecursive(appendable);
        }
    }

    private void printScope(Appendable appendable) throws IOException
    {
        if (this.scope == null)
        {
            appendable.append("NULL");
        }
        else if (this.scope instanceof GenericType)
        {
            org.finos.legend.pure.m3.navigation.generictype.GenericType.print(appendable, this.scope, this.processorSupport);
        }
        else
        {
            String functionName = null;
            if (this.scope instanceof FunctionExpression)
            {
                functionName = ((FunctionExpression)this.scope)._functionName();
            }
            else if (this.scope instanceof Function)
            {
                functionName = ((Function)this.scope)._functionName();
            }
            appendable.append(functionName == null ? "null" : functionName);
            FunctionType.print(appendable, this.processorSupport.function_getFunctionType(this.scope), this.processorSupport);
        }
    }

    private void printStates(Appendable appendable) throws IOException
    {
        boolean firstState = true;
        for (TypeInferenceContextState state : this.states)
        {
            if (firstState)
            {
                firstState = false;
            }
            else
            {
                appendable.append(" || ");
            }
            printState(appendable, state);
        }
    }

    private void printState(Appendable appendable, TypeInferenceContextState state) throws IOException
    {
        boolean firstTypeParam = true;
        appendable.append('[');
        for (String parameter : state.getTypeParameters().toSortedList())
        {
            if (firstTypeParam)
            {
                firstTypeParam = false;
            }
            else
            {
                appendable.append(", ");
            }
            appendable.append(parameter);
            appendable.append(" = ");
            ParameterValueWithFlag valueWithFlag = state.getTypeParameterValueWithFlag(parameter);
            if (valueWithFlag.isTerminal(this.processorSupport))
            {
                appendable.append('*');
            }
            org.finos.legend.pure.m3.navigation.generictype.GenericType.print(appendable, valueWithFlag.getParameterValue(), this.processorSupport);
            if (!org.finos.legend.pure.m3.navigation.generictype.GenericType.isGenericTypeConcrete(valueWithFlag.getParameterValue(), processorSupport))
            {
                appendable.append(':').append(valueWithFlag.getTargetGenericsContext().getId());
            }
        }
        appendable.append("] / [");

        boolean firstMultParam = true;
        for (String parameter : state.getMultiplicityParameters().toSortedList())
        {
            if (firstMultParam)
            {
                firstMultParam = false;
            }
            else
            {
                appendable.append(", ");
            }
            appendable.append(parameter);
            appendable.append(" = ");
            ParameterValueWithFlag valueWithFlag = state.getMultiplicityParameterValueWithFlag(parameter);
            if (valueWithFlag.isTerminal(this.processorSupport))
            {
                appendable.append('*');
            }
            org.finos.legend.pure.m3.navigation.multiplicity.Multiplicity.print(appendable, valueWithFlag.getParameterValue(), true);
        }
        appendable.append(']');
    }

    public void registerMul(Multiplicity templateMul, Multiplicity valueMul, TypeInferenceContext targetGenericsContext, TypeInferenceObserver observer)
    {
        Objects.requireNonNull(targetGenericsContext);

        String name = org.finos.legend.pure.m3.navigation.multiplicity.Multiplicity.getMultiplicityParameter(templateMul);
        if (name != null)
        {
            RegistrationRequest forward = null;

            ParameterValueWithFlag existing = this.states.getLast().getMultiplicityParameterValueWithFlag(name);
            if (existing == null)
            {
                // New registration
                this.states.getLast().putMultiplicityParameterValue(name, valueMul, targetGenericsContext, false);
            }
            else  if (org.finos.legend.pure.m3.navigation.multiplicity.Multiplicity.isMultiplicityConcrete(existing.getParameterValue()) && org.finos.legend.pure.m3.navigation.multiplicity.Multiplicity.isMultiplicityConcrete(valueMul))
            {
                // Merge two concrete multiplicities
                CoreInstance mul = org.finos.legend.pure.m3.navigation.multiplicity.Multiplicity.minSubsumingMultiplicity(FastList.newListWith(valueMul, existing.getParameterValue()), this.processorSupport);
                this.states.getLast().putMultiplicityParameterValue(name, mul, targetGenericsContext, false);
            }
            else if (this.states.size() > 1)
            {
                // We  are processing elements of a collection, record what we learn for the element which will later
                // be processed by TypeInference.potentiallyUpdateParentTypeParamForInstanceValueWithManyElements later
                this.states.getLast().putMultiplicityParameterValue(name, valueMul, targetGenericsContext, false);
            }
            else  if (org.finos.legend.pure.m3.navigation.multiplicity.Multiplicity.isMultiplicityConcrete(existing.getParameterValue()))
            {
                // Replace the existing concrete registration with a generic one and move the concrete one to the referenced type
                this.states.getLast().putMultiplicityParameterValue(name, valueMul, targetGenericsContext, false);
                forward = new RegistrationRequest(targetGenericsContext, valueMul, (Multiplicity) existing.getParameterValue());
            }
            else  if (org.finos.legend.pure.m3.navigation.multiplicity.Multiplicity.isMultiplicityConcrete(valueMul))
            {
                // forward the registration of this concrete type to the already referenced type
                forward = new RegistrationRequest(existing.getTargetGenericsContext(), existing.getParameterValue(), valueMul);
            }
            else
            {
                // forward the registration of this generic type to the already referenced type
                forward = new RegistrationRequest(existing.getTargetGenericsContext(), existing.getParameterValue(), valueMul);
            }
            observer.registerMul(templateMul, valueMul, this, targetGenericsContext);

            if (forward != null && !forward.context.equals(this))
            {
                observer.shiftTab();
                forward.context.registerMul((Multiplicity) forward.template, (Multiplicity) forward.value, targetGenericsContext, observer);
                observer.unShiftTab();
            }
        }
    }

    public ListIterable<CoreInstance> extractTypes(GenericTypeWithXArguments homogenizedTypeArgs)
    {
        return homogenizedTypeArgs.extractArgumentsAsTypeParameters(this.processorSupport);
    }

    public ListIterable<CoreInstance> extractMuls(GenericTypeWithXArguments homogenizedTypeArgs)
    {
        return homogenizedTypeArgs.extractArgumentsAsMultiplicityParameters(this.processorSupport);
    }

    public void register(GenericType templateGenType, GenericType valueForMetaPropertyToOne, TypeInferenceContext targetGenericsContext, TypeInferenceObserver observer)
    {
        Objects.requireNonNull(targetGenericsContext);

        if (valueForMetaPropertyToOne != null)
        {
            valueForMetaPropertyToOne = (GenericType)org.finos.legend.pure.m3.navigation.generictype.GenericType.copyGenericType(valueForMetaPropertyToOne, this.processorSupport);
            String name = org.finos.legend.pure.m3.navigation.generictype.GenericType.getTypeParameterName(templateGenType, this.processorSupport);
            if (name != null)
            {
                ParameterValueWithFlag existing = this.states.getLast().getTypeParameterValueWithFlag(name);

                List<RegistrationRequest> forwards = Lists.mutable.empty();

                if (existing == null)
                {
                    // New registration
                    this.states.getLast().putTypeParameterValue(name, valueForMetaPropertyToOne, targetGenericsContext, false);
                }
                else  if (org.finos.legend.pure.m3.navigation.generictype.GenericType.isGenericTypeConcrete(existing.getParameterValue(), this.processorSupport) && org.finos.legend.pure.m3.navigation.generictype.GenericType.isGenericTypeConcrete(valueForMetaPropertyToOne))
                {
                    // Merge two concrete types
                    GenericType res = (GenericType)org.finos.legend.pure.m3.navigation.generictype.GenericType.findBestCommonGenericType(FastList.newListWith(existing.getParameterValue(), valueForMetaPropertyToOne), TypeParameter.isCovariant(templateGenType), false, this.processorSupport);
                    this.states.getLast().putTypeParameterValue(name, res, targetGenericsContext, false);

                    // See if the replacement is the more concrete version of a previously semi-concrete type (List<T> replaced by List<String>)
                    CoreInstance existingRawType = ((GenericType) existing.getParameterValue())._rawType();
                    CoreInstance replacementRawType = res._rawType();
                    if (existingRawType.equals(replacementRawType))
                    {
                        Iterator<? extends GenericType> existingTypeArguments = ((GenericType)existing.getParameterValue())._typeArguments().iterator();
                        Iterator<? extends GenericType> replacementTypeArguments = res._typeArguments().iterator();
                        while (existingTypeArguments.hasNext())
                        {
                            GenericType existingArgument = existingTypeArguments.next();
                            GenericType replacementArgument = replacementTypeArguments.next();
                            if (!org.finos.legend.pure.m3.navigation.generictype.GenericType.isGenericTypeConcrete(existingArgument) && org.finos.legend.pure.m3.navigation.generictype.GenericType.isGenericTypeConcrete(replacementArgument))
                            {
                                forwards.add(new RegistrationRequest(existing.getTargetGenericsContext(), existingArgument, replacementArgument));
                            }
                        }
                    }
                }
                else if (this.states.size() > 1)
                {
                    // We  are processing elements of a collection, record what we learn for the element which will later
                    // be processed by TypeInference.potentiallyUpdateParentTypeParamForInstanceValueWithManyElements later
                    this.states.getLast().putTypeParameterValue(name, valueForMetaPropertyToOne, targetGenericsContext, false);
                }
                else  if (org.finos.legend.pure.m3.navigation.generictype.GenericType.isGenericTypeConcrete(existing.getParameterValue(), this.processorSupport))
                {
                    // Replace the existing concrete registration with a generic one and move the concrete one to the referenced type
                    this.states.getLast().putTypeParameterValue(name, valueForMetaPropertyToOne, targetGenericsContext, false);
                    forwards.add(new RegistrationRequest(targetGenericsContext, valueForMetaPropertyToOne, existing.getParameterValue()));
                }
                else  if (org.finos.legend.pure.m3.navigation.generictype.GenericType.isGenericTypeConcrete(valueForMetaPropertyToOne, this.processorSupport))
                {
                    if (!existing.getTargetGenericsContext().equals(this))
                    {
                        // forward the registration of this concrete type to the already referenced type
                        forwards.add(new RegistrationRequest(existing.getTargetGenericsContext(), existing.getParameterValue(), valueForMetaPropertyToOne));
                    }
                }
                else
                {
                    if (!existing.getTargetGenericsContext().equals(this))
                    {
                        // forward the registration of this generic type to the already referenced type
                        forwards.add(new RegistrationRequest(existing.getTargetGenericsContext(), existing.getParameterValue(), valueForMetaPropertyToOne));
                    }
                }
                observer.register(templateGenType, valueForMetaPropertyToOne, this, targetGenericsContext);

                observer.shiftTab();
                for (RegistrationRequest request: forwards)
                {
                    request.context.register((GenericType) request.template, (GenericType) request.value, targetGenericsContext, observer);
                }
                observer.unShiftTab();
            }

            if (org.finos.legend.pure.m3.navigation.generictype.GenericType.isGenericTypeConcrete(templateGenType, this.processorSupport) && org.finos.legend.pure.m3.navigation.generictype.GenericType.isGenericTypeConcrete(valueForMetaPropertyToOne, this.processorSupport))
            {
                if (!Type.isBottomType(ImportStub.withImportStubByPass(templateGenType._rawTypeCoreInstance(), this.processorSupport), this.processorSupport) &&
                    !Type.isBottomType(ImportStub.withImportStubByPass(valueForMetaPropertyToOne._rawTypeCoreInstance(), this.processorSupport), this.processorSupport) &&
                    !Type.isTopType(ImportStub.withImportStubByPass(templateGenType._rawTypeCoreInstance(), this.processorSupport), this.processorSupport) &&
                    !Type.isTopType(ImportStub.withImportStubByPass(valueForMetaPropertyToOne._rawTypeCoreInstance(), this.processorSupport), this.processorSupport))
                {
                    ListIterable<? extends CoreInstance> typeValues;
                    ListIterable<? extends CoreInstance> mulValues;
                    ListIterable<? extends CoreInstance> typeTemplates;
                    ListIterable<? extends CoreInstance> mulTemplates;
                    if (Type.subTypeOf(ImportStub.withImportStubByPass(templateGenType._rawTypeCoreInstance(), this.processorSupport), ImportStub.withImportStubByPass(valueForMetaPropertyToOne._rawTypeCoreInstance(), this.processorSupport), this.processorSupport))
                    {
                        typeTemplates = extractTypes(org.finos.legend.pure.m3.navigation.generictype.GenericType.resolveClassTypeParameterUsingInheritance(templateGenType, valueForMetaPropertyToOne, this.processorSupport));
                        mulTemplates = extractMuls(org.finos.legend.pure.m3.navigation.generictype.GenericType.resolveClassMultiplicityParameterUsingInheritance(templateGenType, ImportStub.withImportStubByPass(valueForMetaPropertyToOne._rawTypeCoreInstance(), this.processorSupport), this.processorSupport));
                        typeValues = valueForMetaPropertyToOne._typeArguments().toList();
                        mulValues =  valueForMetaPropertyToOne._multiplicityArguments().toList();

                    }
                    else
                    {
                        typeTemplates = templateGenType._typeArguments().toList();
                        mulTemplates = templateGenType._multiplicityArguments().toList();
                        typeValues = extractTypes(org.finos.legend.pure.m3.navigation.generictype.GenericType.resolveClassTypeParameterUsingInheritance(valueForMetaPropertyToOne, templateGenType, this.processorSupport));
                        mulValues = extractMuls(org.finos.legend.pure.m3.navigation.generictype.GenericType.resolveClassMultiplicityParameterUsingInheritance(valueForMetaPropertyToOne, ImportStub.withImportStubByPass(templateGenType._rawTypeCoreInstance(), this.processorSupport), this.processorSupport));
                    }

                    for (int z = 0; z < mulValues.size(); z++)
                    {
                        registerMul((Multiplicity)mulTemplates.get(z), (Multiplicity)mulValues.get(z), targetGenericsContext, observer);
                    }

                    for (int z = 0; z < typeValues.size(); z++)
                    {
                        GenericType first = (GenericType)typeTemplates.get(z);
                        GenericType second = (GenericType)typeValues.get(z);

                        if (org.finos.legend.pure.m3.navigation.generictype.GenericType.isGenericTypeConcrete(first, this.processorSupport) && first._rawTypeCoreInstance() instanceof org.finos.legend.pure.m3.coreinstance.meta.pure.metamodel.type.FunctionType &&
                                org.finos.legend.pure.m3.navigation.generictype.GenericType.isGenericTypeConcrete(second, this.processorSupport) && second._rawTypeCoreInstance() instanceof org.finos.legend.pure.m3.coreinstance.meta.pure.metamodel.type.FunctionType)
                        {
                            org.finos.legend.pure.m3.coreinstance.meta.pure.metamodel.type.FunctionType firstFuncType = (org.finos.legend.pure.m3.coreinstance.meta.pure.metamodel.type.FunctionType)first._rawTypeCoreInstance();
                            org.finos.legend.pure.m3.coreinstance.meta.pure.metamodel.type.FunctionType secondFuncType = (org.finos.legend.pure.m3.coreinstance.meta.pure.metamodel.type.FunctionType)second._rawTypeCoreInstance();
                            observer.register(first, second, this, targetGenericsContext);
                            observer.shiftTab();

                            ListIterable<? extends VariableExpression> firstParams = firstFuncType._parameters().toList();
                            ListIterable<? extends VariableExpression> secondParams = secondFuncType._parameters().toList();

                            for (int i = 0; i < firstParams.size(); i++)
                            {
                                register(firstParams.get(i)._genericType(), secondParams.get(i)._genericType(), targetGenericsContext, observer);
                                registerMul(firstParams.get(i)._multiplicity(), secondParams.get(i)._multiplicity(), targetGenericsContext, observer);
                            }
                            register(firstFuncType._returnType(), secondFuncType._returnType(), targetGenericsContext, observer);
                            registerMul(firstFuncType._returnMultiplicity(), secondFuncType._returnMultiplicity(), targetGenericsContext, observer);
                            observer.unShiftTab();
                        }
                        else
                        {
                            register(first, second, targetGenericsContext, observer);
                        }
                    }
                }
            }
        }
    }

    public CoreInstance resolve(CoreInstance genericType)
    {
        if (org.finos.legend.pure.m3.navigation.generictype.GenericType.isGenericTypeConcrete(genericType, this.processorSupport))
        {
            return genericType;
        }
        ParameterValueWithFlag gf = this.states.getLast().getTypeParameterValueWithFlag(org.finos.legend.pure.m3.navigation.generictype.GenericType.getTypeParameterName(genericType, this.processorSupport));
        if (gf != null)
        {
            return gf.getParameterValue();
        }
        TypeInferenceContext top = getTopContext();
        if (top != null)
        {
            ParameterValueWithFlag parentGf = top.states.getLast().getTypeParameterValueWithFlag(org.finos.legend.pure.m3.navigation.generictype.GenericType.getTypeParameterName(genericType, this.processorSupport));
            if (parentGf != null)
            {
                return parentGf.getParameterValue();
            }
        }
        return null;
    }

    public void setAhead()
    {
        this.states.getLast().setAhead();
    }

    public boolean isAhead()
    {
        return this.states.getLast().isAhead();
    }

    public boolean isAheadConsumed()
    {
        return this.states.getLast().isAheadConsumed();
    }

    public void aheadConsumed()
    {
        this.states.getLast().setAheadConsumed();
    }

    public MapIterable<String, CoreInstance> getTypeParameterToGenericType()
    {
        TypeInferenceContextState state = this.states.getLast();
        MutableMap<String, CoreInstance> result = Maps.mutable.empty();

        for (String parameter: state.getTypeParameters())
        {
            result.put(parameter, this.resolveTypeParameter(parameter));
        }
        return result;
    }

    public MapIterable<String, CoreInstance> getMultiplicityParameterToMultiplicity()
    {
        TypeInferenceContextState state = this.states.getLast();
        MutableMap<String, CoreInstance> result = Maps.mutable.empty();

        for (String parameter: state.getMultiplicityParameters())
        {
            result.put(parameter, this.resolveMultiplicityParameter(parameter));
        }
        return result;
    }

    CoreInstance resolveTypeParameter(String parameter)
    {
        ParameterValueWithFlag value = this.states.getLast().getTypeParameterValueWithFlag(parameter);
        if (!org.finos.legend.pure.m3.navigation.generictype.GenericType.isGenericTypeConcrete(value.getParameterValue(), this.processorSupport))
        {
            String referencedName = org.finos.legend.pure.m3.navigation.generictype.GenericType.getTypeParameterName(value.getParameterValue(), this.processorSupport);
            TypeInferenceContext referencedContext = value.getTargetGenericsContext();
            if (!this.equals(referencedContext) && referencedContext.containsTypeParameter(referencedName))
            {
                return referencedContext.resolveTypeParameter(referencedName);
            }
        }
        return value.getParameterValue();
    }

    CoreInstance resolveMultiplicityParameter(String parameter)
    {
        ParameterValueWithFlag value = this.states.getLast().getMultiplicityParameterValueWithFlag(parameter);
        if (!org.finos.legend.pure.m3.navigation.multiplicity.Multiplicity.isMultiplicityConcrete(value.getParameterValue()))
        {
            String referencedName = org.finos.legend.pure.m3.navigation.multiplicity.Multiplicity.getMultiplicityParameter(value.getParameterValue());
            TypeInferenceContext referencedContext = value.getTargetGenericsContext();
            if (!this.equals(referencedContext) && referencedContext.containsMultiplicityParameter(referencedName))
            {
                return referencedContext.resolveMultiplicityParameter(referencedName);
            }
        }
        return value.getParameterValue();
    }

    boolean containsTypeParameter(String parameter)
    {
        return this.states.getLast().getTypeParameters().contains(parameter);
    }

    boolean containsMultiplicityParameter(String parameter)
    {
        return this.states.getLast().getMultiplicityParameters().contains(parameter);
    }

    public void setScope(CoreInstance scope)
    {
        this.scope = scope;
    }

    private static class RegistrationRequest
    {
        TypeInferenceContext context = null;
        CoreInstance template = null;
        CoreInstance value = null;

        public RegistrationRequest(TypeInferenceContext context, CoreInstance template,  CoreInstance value)
        {
            this.value = value;
            this.template = template;
            this.context = context;
        }
    }
}
