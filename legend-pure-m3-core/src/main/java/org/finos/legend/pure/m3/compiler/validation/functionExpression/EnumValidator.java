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

package org.finos.legend.pure.m3.compiler.validation.functionExpression;

import org.eclipse.collections.api.list.ListIterable;
import org.finos.legend.pure.m3.navigation.PackageableElement.PackageableElement;
import org.finos.legend.pure.m3.navigation.enumeration.Enumeration;

import org.finos.legend.pure.m3.navigation.importstub.ImportStub;
import org.finos.legend.pure.m3.coreinstance.meta.pure.metamodel.valuespecification.FunctionExpression;
import org.finos.legend.pure.m3.coreinstance.meta.pure.metamodel.valuespecification.InstanceValue;
import org.finos.legend.pure.m3.coreinstance.meta.pure.metamodel.valuespecification.ValueSpecification;
import org.finos.legend.pure.m3.navigation.ProcessorSupport;
import org.finos.legend.pure.m4.coreinstance.CoreInstance;
import org.finos.legend.pure.m4.exception.PureCompilationException;

public class EnumValidator
{
    public static void validateEnum(FunctionExpression instance, ProcessorSupport processorSupport) throws PureCompilationException
    {
        ListIterable<? extends ValueSpecification> parametersValues = instance._parametersValues().toList();
        if (parametersValues.get(0) instanceof InstanceValue && parametersValues.get(1) instanceof InstanceValue)
        {
            CoreInstance enumeration = ImportStub.withImportStubByPass(((InstanceValue)parametersValues.get(0))._valuesCoreInstance().getFirst(), processorSupport);
            String enumName = ImportStub.withImportStubByPass(((InstanceValue)parametersValues.get(1))._valuesCoreInstance().getFirst(), processorSupport).getName();
            CoreInstance enumValue = Enumeration.findEnum(enumeration, enumName);
            if (enumValue == null)
            {
                throw new PureCompilationException(instance.getSourceInformation(), "The enum value '" + enumName + "' can't be found in the enumeration " + PackageableElement.getUserPathForPackageableElement(enumeration));
            }
        }
    }
}
