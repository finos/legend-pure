// Copyright 2025 Goldman Sachs
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

package org.finos.legend.pure.runtime.java.compiled.generation;

import org.finos.legend.pure.m3.coreinstance.meta.pure.metamodel.relation.Column;
import org.finos.legend.pure.m3.coreinstance.meta.pure.metamodel.relation.RelationType;
import org.finos.legend.pure.m3.coreinstance.meta.pure.metamodel.type.Class;
import org.finos.legend.pure.m3.coreinstance.meta.pure.metamodel.type.Enumeration;
import org.finos.legend.pure.m3.coreinstance.meta.pure.metamodel.type.FunctionType;
import org.finos.legend.pure.m3.coreinstance.meta.pure.metamodel.type.PrimitiveType;
import org.finos.legend.pure.m3.coreinstance.meta.pure.metamodel.type.Type;
import org.finos.legend.pure.m3.coreinstance.meta.pure.metamodel.type.Unit;
import org.finos.legend.pure.m3.coreinstance.meta.pure.metamodel.valuespecification.ValueSpecification;
import org.finos.legend.pure.m3.coreinstance.meta.pure.metamodel.valuespecification.VariableExpression;
import org.finos.legend.pure.m3.navigation.M3Paths;
import org.finos.legend.pure.m3.navigation.relation._Column;
import org.finos.legend.pure.runtime.java.compiled.generation.processors.support.Pure;
import org.finos.legend.pure.runtime.java.compiled.generation.processors.valuespecification.ValueSpecificationProcessor;

public class GenericTypeSerializationInCode
{
    // GenericType 'serialization' in code

    public static String generateGenericTypeBuilder(org.finos.legend.pure.m3.coreinstance.meta.pure.metamodel.type.generics.GenericType genericType, ProcessorContext processorContext)
    {
        return "new Root_meta_pure_metamodel_type_generics_GenericType_Impl(\"\", null, ((CompiledExecutionSupport)es).getMetadataAccessor().getClass(\"" + M3Paths.GenericType + "\"))" +
                (genericType._rawType() == null ? "" : "._rawType(" + generateTypeBuilder(genericType._rawType(), processorContext) + ")") +
                (genericType._typeParameter() == null ? "" : "._typeParameter(new Root_meta_pure_metamodel_type_generics_TypeParameter_Impl(\"\", null, ((CompiledExecutionSupport)es).getMetadataAccessor().getClass(\"" + M3Paths.TypeParameter + "\"))._name(\"" + genericType._typeParameter()._name() + "\"))") +
                (genericType._typeArguments().isEmpty() ? "" : "._typeArguments(Lists.mutable.with(" + genericType._typeArguments().collect(x -> generateGenericTypeBuilder(x, processorContext)).makeString(", ") + "))") +
                (genericType._multiplicityArguments().isEmpty() ? "" : "._multiplicityArguments(Lists.mutable.with(" + genericType._multiplicityArguments().collect(GenericTypeSerializationInCode::generateMultiplicityBuilder).makeString(", ") + "))") +
                (genericType._typeVariableValues().isEmpty() ? "" : "._typeVariableValues(Lists.mutable.with(" + genericType._typeVariableValues().collect(vs -> generateValueSpecification(vs, processorContext)).makeString(", ") + "))");
    }

    private static String generateValueSpecification(ValueSpecification vs, ProcessorContext processorContext)
    {
        return "new Root_meta_pure_metamodel_valuespecification_InstanceValue_Impl(\"\", null, ((CompiledExecutionSupport)es).getMetadataAccessor().getClass(\"" + M3Paths.InstanceValue + "\"))" +
                "\"))" +
                "._genericType(" + generateGenericTypeBuilder(vs._genericType(), processorContext) + ")" +
                "._multiplicity(" + generateMultiplicityBuilder(vs._multiplicity()) + ")" +
                "._values(Lists.mutable.with(" + ValueSpecificationProcessor.processValueSpecification(vs, processorContext) + "))";
    }

    private static String generateTypeBuilder(Type type, ProcessorContext processorContext)
    {
        if (type instanceof Class)
        {
            return "((CompiledExecutionSupport)es).getMetadataAccessor().getClass(\"" + Pure.elementToPath((org.finos.legend.pure.m3.coreinstance.meta.pure.metamodel.PackageableElement) type, "::") + "\")";
        }
        else if (type instanceof PrimitiveType)
        {
            return "((CompiledExecutionSupport)es).getMetadataAccessor().getPrimitiveType(\"" + Pure.elementToPath((org.finos.legend.pure.m3.coreinstance.meta.pure.metamodel.PackageableElement) type, "::") + "\")";
        }
        else if (type instanceof RelationType)
        {
            return "org.finos.legend.pure.m3.navigation.relation._RelationType.build(Lists.mutable.<org.finos.legend.pure.m3.coreinstance.meta.pure.metamodel.relation.Column<?,?>>with(" + ((RelationType<?>) type)._columns().collect(c -> generateColumnBuilder(c, processorContext)).makeString(", ") + "), null, ((CompiledExecutionSupport)es).getProcessorSupport())";
        }
        else if (type instanceof FunctionType)
        {
            return "new Root_meta_pure_metamodel_type_FunctionType_Impl(\"\", null, ((CompiledExecutionSupport)es).getMetadataAccessor().getClass(\"" + M3Paths.FunctionType + "\"))" +
                    "._parameters(Lists.mutable.with(" + ((FunctionType) type)._parameters().collect(v -> generateVariableBuilder(v, processorContext)).makeString(", ") + "))" +
                    "._returnType(" + generateGenericTypeBuilder(((FunctionType) type)._returnType(), processorContext) + ")" +
                    "._returnMultiplicity(" + generateMultiplicityBuilder(((FunctionType) type)._returnMultiplicity()) + ")";
        }
        else if (type instanceof Unit)
        {
            return "((CompiledExecutionSupport)es).getMetadataAccessor().getUnit(\"" + Pure.elementToPath((org.finos.legend.pure.m3.coreinstance.meta.pure.metamodel.PackageableElement) type, "::") + "\")";
        }
        else if (type instanceof org.finos.legend.pure.m3.coreinstance.meta.pure.metamodel.type.Measure)
        {
            return "((CompiledExecutionSupport)es).getMetadataAccessor().getMeasure(\"" + Pure.elementToPath((org.finos.legend.pure.m3.coreinstance.meta.pure.metamodel.PackageableElement) type, "::") + "\")";
        }
        else if (type instanceof Enumeration)
        {
            return "((CompiledExecutionSupport)es).getMetadataAccessor().getEnumeration(\"" + Pure.elementToPath((org.finos.legend.pure.m3.coreinstance.meta.pure.metamodel.PackageableElement) type, "::") + "\")";
        }
        throw new RuntimeException("Unsupported type: " + type.getClass());
    }

    private static Object generateVariableBuilder(VariableExpression v, ProcessorContext processorContext)
    {
        return "new Root_meta_pure_metamodel_valuespecification_VariableExpression_Impl(\"\", null, ((CompiledExecutionSupport)es).getMetadataAccessor().getClass(\"" + M3Paths.VariableExpression + "\"))" +
                "._name(\"" + v._name() + "\")" +
                "._genericType(" + generateGenericTypeBuilder(v._genericType(), processorContext) + ")" +
                "._multiplicity(" + generateMultiplicityBuilder(v._multiplicity()) + ")";
    }

    public static String generateColumnBuilder(Column<?, ?> column, ProcessorContext processorContext)
    {
        return "org.finos.legend.pure.m3.navigation.relation._Column.getColumnInstance(\"" + column._name() + "\", " + column._nameWildCard() + ", " + generateGenericTypeBuilder(_Column.getColumnType(column), processorContext) + ", " + generateMultiplicityBuilder(_Column.getColumnMultiplicity(column)) + ", null, ((CompiledExecutionSupport)es).getProcessorSupport())";
    }

    public static String generateMultiplicityBuilder(org.finos.legend.pure.m3.coreinstance.meta.pure.metamodel.multiplicity.Multiplicity multiplicity)
    {
        return "new Root_meta_pure_metamodel_multiplicity_Multiplicity_Impl(\"\", null, ((CompiledExecutionSupport)es).getMetadataAccessor().getClass(\"" + M3Paths.Multiplicity + "\"))" +
                (multiplicity._multiplicityParameter() != null ?
                        "._multiplicityParameter(\"" + multiplicity._multiplicityParameter() + "\")" :
                        "._lowerBound(new Root_meta_pure_metamodel_multiplicity_MultiplicityValue_Impl(\"\", null, ((CompiledExecutionSupport)es).getMetadataAccessor().getClass(\"" + M3Paths.MultiplicityValue + "\"))._value(" + multiplicity._lowerBound()._value() + "L))" +
                                "._upperBound(new Root_meta_pure_metamodel_multiplicity_MultiplicityValue_Impl(\"\", null, ((CompiledExecutionSupport)es).getMetadataAccessor().getClass(\"" + M3Paths.MultiplicityValue + "\"))" +
                                (multiplicity._upperBound()._value() == null ? "" : "._value(" + multiplicity._upperBound()._value() + "L)") + ")");
    }
}
