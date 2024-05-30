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
import org.finos.legend.pure.m3.compiler.Context;
import org.finos.legend.pure.m3.coreinstance.meta.pure.metamodel.relationship.Generalization;
import org.finos.legend.pure.m3.coreinstance.meta.pure.metamodel.type.Any;
import org.finos.legend.pure.m3.coreinstance.meta.pure.metamodel.type.Class;
import org.finos.legend.pure.m3.coreinstance.meta.pure.metamodel.type.Enumeration;
import org.finos.legend.pure.m3.coreinstance.meta.pure.metamodel.type.PrimitiveType;
import org.finos.legend.pure.m3.coreinstance.meta.pure.metamodel.type.Type;
import org.finos.legend.pure.m3.coreinstance.meta.pure.metamodel.type.generics.GenericType;
import org.finos.legend.pure.m3.navigation.M3Paths;
import org.finos.legend.pure.m3.navigation.PackageableElement.PackageableElement;
import org.finos.legend.pure.m3.navigation.ProcessorSupport;
import org.finos.legend.pure.m3.navigation._class._Class;
import org.finos.legend.pure.m3.navigation.importstub.ImportStub;
import org.finos.legend.pure.m3.tools.matcher.MatchRunner;
import org.finos.legend.pure.m3.tools.matcher.Matcher;
import org.finos.legend.pure.m3.tools.matcher.MatcherState;
import org.finos.legend.pure.m4.ModelRepository;
import org.finos.legend.pure.m4.coreinstance.CoreInstance;
import org.finos.legend.pure.m4.exception.PureCompilationException;
import org.finos.legend.pure.m4.exception.PureException;

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
            if (type instanceof Class)
            {
                validateClassGeneralizations((Class)type, generalizations, processorSupport);
            }
            else if (type instanceof PrimitiveType)
            {
                validatePrimitiveTypeGeneralizations((PrimitiveType)type, generalizations, processorSupport);
            }
            else if (type instanceof Enumeration)
            {
                validateEnumerationGeneralizations((Enumeration)type, generalizations, processorSupport);
            }
        }
        try
        {
            org.finos.legend.pure.m3.navigation.type.Type.getGeneralizationResolutionOrder(type, processorSupport);
        }
        catch (RuntimeException e)
        {
            PureException pe = PureException.findPureException(e);
            if (pe instanceof PureCompilationException)
            {
                throw pe;
            }
            else
            {
                throw e;
            }
        }
    }

    private void validateClassGeneralizations(Class cls, RichIterable<? extends Generalization> generalizations, ProcessorSupport processorSupport) throws PureCompilationException
    {
        for (Generalization generalization : generalizations)
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
                throw new PureCompilationException(cls.getSourceInformation(), builder.toString());
            }
        }
    }

    private void validatePrimitiveTypeGeneralizations(PrimitiveType type, RichIterable<? extends Generalization> generalizations, ProcessorSupport processorSupport) throws PureCompilationException
    {
        Any anyClass = (Any)processorSupport.package_getByUserPath(M3Paths.Any);
        for (Generalization generalization : generalizations)
        {
            GenericType general = generalization._general();
            CoreInstance generalRawType = ImportStub.withImportStubByPass(general._rawTypeCoreInstance(), processorSupport);
            if ((generalRawType != anyClass) && ! (generalRawType instanceof PrimitiveType))
            {
                StringBuilder builder = new StringBuilder("Invalid generalization: ");
                PackageableElement.writeUserPathForPackageableElement(builder, type);
                builder.append(" cannot extend ");
                org.finos.legend.pure.m3.navigation.generictype.GenericType.print(builder, general, true, processorSupport);
                builder.append(" as it is not a PrimitiveType or Any");
                throw new PureCompilationException(type.getSourceInformation(), builder.toString());
            }
        }
    }

    private void validateEnumerationGeneralizations(Enumeration enumeration, RichIterable<? extends Generalization> generalizations, ProcessorSupport processorSupport) throws PureCompilationException
    {
        CoreInstance enumClass = processorSupport.package_getByUserPath(M3Paths.Enum);
        for (Generalization generalization : generalizations)
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
                throw new PureCompilationException(enumeration.getSourceInformation(), builder.toString());
            }
        }
    }
}
