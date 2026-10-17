// Copyright 2026 Goldman Sachs
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
import org.finos.legend.pure.m3.coreinstance.meta.pure.metamodel.type.generics.GenericType;
import org.finos.legend.pure.m3.coreinstance.meta.pure.metamodel.valuespecification.FunctionExpression;
import org.finos.legend.pure.m3.coreinstance.meta.pure.metamodel.valuespecification.ValueSpecification;
import org.finos.legend.pure.m3.navigation.ProcessorSupport;
import org.finos.legend.pure.m3.navigation.importstub.ImportStub;
import org.finos.legend.pure.m3.navigation.type.Type;
import org.finos.legend.pure.m4.coreinstance.CoreInstance;
import org.finos.legend.pure.m4.exception.PureCompilationException;

/**
 * Rejects casts that can never succeed at runtime.
 * <p>
 * A cast from source type S to target type T is rejected when neither raw type is a subtype of the
 * other and at least one of them is a DataType (a PrimitiveType, Enumeration, Measure or Unit). This
 * relies on the generalization rules enforced by
 * {@link org.finos.legend.pure.m3.compiler.validation.validator.TypeValidator}: classes may only
 * extend classes, a primitive type extends exactly one primitive type or Any, an enumeration extends
 * only Enum and cannot itself be extended, and a unit extends only its measure. Every subtype of a
 * DataType therefore lies on a single-inheritance chain, so S and T can only share a subtype other
 * than Nil if one of them is a subtype of the other. If those rules are relaxed, or a new kind of
 * DataType is introduced, this validator must be revisited.
 * <p>
 * Casts between two non-DataTypes (e.g., two unrelated classes) are never rejected, since multiple
 * inheritance means a common subtype can always be defined. Only raw types are compared: type
 * arguments are not considered.
 */
public class CastValidator
{
    public static void validateCast(FunctionExpression instance, ProcessorSupport processorSupport) throws PureCompilationException
    {
        ListIterable<? extends ValueSpecification> valueSpecifications = instance._parametersValues().toList();
        GenericType sourceGenericType = valueSpecifications.get(0)._genericType();
        GenericType targetGenericType = valueSpecifications.get(1)._genericType();
        CoreInstance sourceRawType = ImportStub.withImportStubByPass(sourceGenericType._rawTypeCoreInstance(), processorSupport);
        CoreInstance targetRawType = ImportStub.withImportStubByPass(targetGenericType._rawTypeCoreInstance(), processorSupport);
        if ((sourceRawType == null) || (targetRawType == null) ||
                processorSupport.type_subTypeOf(sourceRawType, targetRawType) ||
                processorSupport.type_subTypeOf(targetRawType, sourceRawType))
        {
            return;
        }
        if (Type.isDataType(sourceRawType, processorSupport) || Type.isDataType(targetRawType, processorSupport))
        {
            throw new PureCompilationException(instance.getSourceInformation(), "Cast exception: " + org.finos.legend.pure.m3.navigation.generictype.GenericType.print(sourceGenericType, processorSupport) + " cannot be cast to " + org.finos.legend.pure.m3.navigation.generictype.GenericType.print(targetGenericType, processorSupport));
        }
    }
}
