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
import org.eclipse.collections.api.factory.Sets;
import org.eclipse.collections.api.set.MutableSet;
import org.finos.legend.pure.m3.compiler.Context;
import org.finos.legend.pure.m3.compiler.validation.Validator;
import org.finos.legend.pure.m3.compiler.validation.ValidatorState;
import org.finos.legend.pure.m3.coreinstance.meta.pure.metamodel.relationship.Generalization;
import org.finos.legend.pure.m3.coreinstance.meta.pure.metamodel.type.Class;
import org.finos.legend.pure.m3.coreinstance.meta.pure.metamodel.type.ClassProjection;
import org.finos.legend.pure.m3.coreinstance.meta.pure.metamodel.type.Enumeration;
import org.finos.legend.pure.m3.coreinstance.meta.pure.metamodel.type.PrimitiveType;
import org.finos.legend.pure.m3.coreinstance.meta.pure.metamodel.type.Type;
import org.finos.legend.pure.m3.coreinstance.meta.pure.metamodel.type.generics.GenericType;
import org.finos.legend.pure.m3.navigation.M3Paths;
import org.finos.legend.pure.m3.navigation.M3Properties;
import org.finos.legend.pure.m3.navigation.PackageableElement.PackageableElement;
import org.finos.legend.pure.m3.navigation.ProcessorSupport;
import org.finos.legend.pure.m3.navigation._class._Class;
import org.finos.legend.pure.m3.navigation.importstub.ImportStub;
import org.finos.legend.pure.m3.tools.matcher.MatchRunner;
import org.finos.legend.pure.m3.tools.matcher.Matcher;
import org.finos.legend.pure.m3.tools.matcher.MatcherState;
import org.finos.legend.pure.m4.ModelRepository;
import org.finos.legend.pure.m4.coreinstance.CoreInstance;
import org.finos.legend.pure.m4.coreinstance.SourceInformation;
import org.finos.legend.pure.m4.exception.PureCompilationException;
import org.finos.legend.pure.m4.exception.PureException;

import java.util.ArrayDeque;
import java.util.Deque;

public class TypeValidator implements MatchRunner<Type>
{
    @Override
    public String getClassName()
    {
        return M3Paths.Type;
    }

    @Override
    public void run(Type type, MatcherState state, Matcher matcher, ModelRepository modelRepository, Context context) throws PureCompilationException
    {
        ProcessorSupport processorSupport = state.getProcessorSupport();
        RichIterable<? extends Generalization> generalizations = type._generalizations();
        if (generalizations.notEmpty())
        {
            validateGeneralizations(type, generalizations, (ValidatorState) state, matcher, processorSupport);
            if (type instanceof Class)
            {
                validateClassGeneralizations((Class<?>) type, generalizations, processorSupport);
            }
            else if (type instanceof PrimitiveType)
            {
                validatePrimitiveTypeGeneralizations((PrimitiveType) type, generalizations, processorSupport);
            }
            else if (type instanceof Enumeration)
            {
                validateEnumerationGeneralizations((Enumeration<?>) type, generalizations, processorSupport);
            }
        }
        try
        {
            org.finos.legend.pure.m3.navigation.type.Type.getGeneralizationResolutionOrder(type, processorSupport);
        }
        catch (Exception e)
        {
            PureException pe = PureException.findPureException(e);
            if (pe instanceof PureCompilationException)
            {
                throw pe;
            }
            throw e;
        }
    }

    private void validateGeneralizations(Type type, RichIterable<? extends Generalization> generalizations, ValidatorState state, Matcher matcher, ProcessorSupport processorSupport)
    {
        generalizations.forEach(generalization ->
        {
            GenericType general = generalization._general();
            Validator.validate(general, state, matcher, processorSupport);
            CoreInstance generalRawType = ImportStub.withImportStubByPass(general._rawTypeCoreInstance(), processorSupport);
            if (generalRawType instanceof ClassProjection)
            {
                StringBuilder builder = PackageableElement.writeUserPathForPackageableElement(new StringBuilder("Class "), generalRawType)
                        .append(" is a projection and cannot be extended");
                throw new PureCompilationException(chooseSourceInfo(type, generalization), builder.toString());
            }

            RichIterable<? extends GenericType> typeArguments = general._typeArguments();
            if (typeArguments.notEmpty())
            {
                CoreInstance bottomType = processorSupport.type_BottomType();
                typeArguments.forEach(typeArgument -> collectAllConcreteTypes(typeArgument, processorSupport).forEach(t ->
                {
                    if ((t != bottomType) && ((t == type) || processorSupport.type_subTypeOf(t, type)))
                    {
                        StringBuilder message = new StringBuilder();
                        String typeName = PackageableElement.isPackageableElement(type, processorSupport) ? PackageableElement.getUserPathForPackageableElement(type) : type.getName();
                        message.append(typeName).append(" extends ");
                        org.finos.legend.pure.m3.navigation.generictype.GenericType.print(message, general, true, processorSupport);
                        message.append(" which contains a reference to ");
                        if (t == type)
                        {
                            message.append("itself");
                        }
                        else
                        {
                            if (PackageableElement.isPackageableElement(t, processorSupport))
                            {
                                PackageableElement.writeUserPathForPackageableElement(message, t);
                            }
                            else
                            {
                                message.append(t.getName());
                            }
                            message.append(" which is a subtype of ").append(typeName);
                        }
                        throw new PureCompilationException(chooseSourceInfo(type, generalization), message.toString());
                    }
                }));
            }
        });
    }

    private static MutableSet<CoreInstance> collectAllConcreteTypes(GenericType initialGenericType, ProcessorSupport processorSupport)
    {
        MutableSet<CoreInstance> set = Sets.mutable.empty();
        Deque<GenericType> deque = new ArrayDeque<>();
        deque.add(initialGenericType);
        while (!deque.isEmpty())
        {
            GenericType genericType = deque.pollFirst();
            CoreInstance rawType = ImportStub.withImportStubByPass(genericType._rawTypeCoreInstance(), processorSupport);
            if (rawType != null)
            {
                set.add(rawType);
            }
            genericType._typeArguments().forEach(deque::add);
        }
        return set;
    }

    private void validateClassGeneralizations(Class<?> cls, RichIterable<? extends Generalization> generalizations, ProcessorSupport processorSupport)
    {
        generalizations.forEach(generalization ->
        {
            GenericType general = generalization._general();
            CoreInstance generalRawType = ImportStub.withImportStubByPass(general._rawTypeCoreInstance(), processorSupport);
            if (!(generalRawType instanceof Class))
            {
                StringBuilder builder = new StringBuilder("Invalid generalization: ");
                _Class.print(builder, cls, true);
                builder.append(" cannot extend ");
                org.finos.legend.pure.m3.navigation.generictype.GenericType.print(builder, general, true, processorSupport);
                builder.append(" as it is not a Class");
                throw new PureCompilationException(chooseSourceInfo(cls, generalization), builder.toString());
            }
            if (generalRawType.getValueForMetaPropertyToMany(M3Properties.typeVariables).notEmpty())
            {
                StringBuilder builder = new StringBuilder("Invalid generalization: ");
                _Class.print(builder, cls, true);
                builder.append(" cannot extend ");
                org.finos.legend.pure.m3.navigation.generictype.GenericType.print(builder, general, true, processorSupport);
                builder.append(" as extending a class with type variables is not currently supported");
                throw new PureCompilationException(chooseSourceInfo(cls, generalization), builder.toString());
            }
        });
    }

    private void validatePrimitiveTypeGeneralizations(PrimitiveType type, RichIterable<? extends Generalization> generalizations, ProcessorSupport processorSupport)
    {
        CoreInstance anyClass = processorSupport.package_getByUserPath(M3Paths.Any);
        generalizations.forEach(generalization ->
        {
            GenericType general = generalization._general();
            CoreInstance generalRawType = ImportStub.withImportStubByPass(general._rawTypeCoreInstance(), processorSupport);
            if ((generalRawType != anyClass) && !(generalRawType instanceof PrimitiveType))
            {
                StringBuilder builder = new StringBuilder("Invalid generalization: ");
                PackageableElement.writeUserPathForPackageableElement(builder, type);
                builder.append(" cannot extend ");
                org.finos.legend.pure.m3.navigation.generictype.GenericType.print(builder, general, true, processorSupport);
                builder.append(" as it is not a PrimitiveType or Any");
                throw new PureCompilationException(chooseSourceInfo(type, generalization), builder.toString());
            }
        });
    }

    private void validateEnumerationGeneralizations(Enumeration<?> enumeration, RichIterable<? extends Generalization> generalizations, ProcessorSupport processorSupport)
    {
        CoreInstance enumClass = processorSupport.package_getByUserPath(M3Paths.Enum);
        generalizations.forEach(generalization ->
        {
            GenericType general = generalization._general();
            CoreInstance generalRawType = ImportStub.withImportStubByPass(general._rawTypeCoreInstance(), processorSupport);
            if (generalRawType != enumClass)
            {
                StringBuilder builder = new StringBuilder("Invalid generalization: ");
                PackageableElement.writeUserPathForPackageableElement(builder, enumeration);
                builder.append(" cannot extend ");
                org.finos.legend.pure.m3.navigation.generictype.GenericType.print(builder, general, true, processorSupport);
                builder.append(" as it is not Enum");
                throw new PureCompilationException(chooseSourceInfo(enumeration, generalization), builder.toString());
            }
        });
    }

    private SourceInformation chooseSourceInfo(Type type, Generalization generalization)
    {
        SourceInformation genlSourceInfo = generalization.getSourceInformation();
        return (genlSourceInfo == null) ? type.getSourceInformation() : genlSourceInfo;
    }
}
