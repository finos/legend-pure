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

package org.finos.legend.pure.runtime.java.compiled.generation.processors.type;

import org.finos.legend.pure.m3.navigation.M3Paths;
import org.finos.legend.pure.runtime.java.compiled.generation.JavaPackageAndImportBuilder;

/**
 * Java Paths for M3 types
 */
public class FullJavaPaths
{
    public static final String AbstractProperty = JavaPackageAndImportBuilder.buildInterfaceReferenceFromUserPath(M3Paths.AbstractProperty);
    public static final String Any = JavaPackageAndImportBuilder.buildInterfaceReferenceFromUserPath(M3Paths.Any);
    public static final String Association_Impl = JavaPackageAndImportBuilder.buildImplClassReferenceFromUserPath(M3Paths.Association);

    public static final String Class = JavaPackageAndImportBuilder.buildInterfaceReferenceFromUserPath(M3Paths.Class);
    public static final String ConcreteFunctionDefinition = JavaPackageAndImportBuilder.buildInterfaceReferenceFromUserPath(M3Paths.ConcreteFunctionDefinition);
    public static final String ConstraintsGetterOverride_Impl = JavaPackageAndImportBuilder.buildImplClassReferenceFromUserPath(M3Paths.ConstraintsGetterOverride);
    public static final String ConstraintsOverride_Impl = JavaPackageAndImportBuilder.buildImplClassReferenceFromUserPath(M3Paths.ConstraintsOverride);
    public static final String Enum = JavaPackageAndImportBuilder.buildInterfaceReferenceFromUserPath(M3Paths.Enum);
    public static final String Enumeration = JavaPackageAndImportBuilder.buildInterfaceReferenceFromUserPath(M3Paths.Enumeration);
    public static final String Enumeration_Impl = JavaPackageAndImportBuilder.buildImplClassReferenceFromUserPath(M3Paths.Enumeration);

    public static final String Function = JavaPackageAndImportBuilder.buildInterfaceReferenceFromUserPath(M3Paths.Function);
    public static final String FunctionDefinition = JavaPackageAndImportBuilder.buildInterfaceReferenceFromUserPath(M3Paths.FunctionDefinition);
    public static final String FunctionType = JavaPackageAndImportBuilder.buildInterfaceReferenceFromUserPath(M3Paths.FunctionType);
    public static final String Generalization = JavaPackageAndImportBuilder.buildInterfaceReferenceFromUserPath(M3Paths.Generalization);
    public static final String GenericType = JavaPackageAndImportBuilder.buildInterfaceReferenceFromUserPath(M3Paths.GenericType);
    public static final String GenericType_Impl = JavaPackageAndImportBuilder.buildImplClassReferenceFromUserPath(M3Paths.GenericType);
    public static final String GetterOverride = JavaPackageAndImportBuilder.buildInterfaceReferenceFromUserPath(M3Paths.GetterOverride);
    public static final String GetterOverride_Impl = JavaPackageAndImportBuilder.buildImplClassReferenceFromUserPath(M3Paths.GetterOverride);
    public static final String InferredGenericType_Impl = JavaPackageAndImportBuilder.buildImplClassReferenceFromUserPath(M3Paths.InferredGenericType);

    public static final String InstanceValue_Impl = JavaPackageAndImportBuilder.buildImplClassReferenceFromUserPath(M3Paths.InstanceValue);

    public static final String KeyExpression = JavaPackageAndImportBuilder.buildInterfaceReferenceFromUserPath(M3Paths.KeyExpression);
    public static final String LambdaFunction = JavaPackageAndImportBuilder.buildInterfaceReferenceFromUserPath(M3Paths.LambdaFunction);
    public static final String LambdaFunction_Impl = JavaPackageAndImportBuilder.buildImplClassReferenceFromUserPath(M3Paths.LambdaFunction);

    public static final String Measure = JavaPackageAndImportBuilder.buildImplClassReferenceFromUserPath(M3Paths.Measure);
    public static final String Multiplicity = JavaPackageAndImportBuilder.buildInterfaceReferenceFromUserPath(M3Paths.Multiplicity);
    public static final String Multiplicity_Impl = JavaPackageAndImportBuilder.buildImplClassReferenceFromUserPath(M3Paths.Multiplicity);
    public static final String MultiplicityValue_Impl = JavaPackageAndImportBuilder.buildImplClassReferenceFromUserPath(M3Paths.MultiplicityValue);
    public static final String NativeFunction = JavaPackageAndImportBuilder.buildInterfaceReferenceFromUserPath(M3Paths.NativeFunction);
    public static final String Nil = JavaPackageAndImportBuilder.buildInterfaceReferenceFromUserPath(M3Paths.Nil);
    public static final String PackageableMultiplicity = JavaPackageAndImportBuilder.buildInterfaceReferenceFromUserPath(M3Paths.PackageableMultiplicity);
    public static final String Pair = JavaPackageAndImportBuilder.buildInterfaceReferenceFromUserPath(M3Paths.Pair);
    public static final String Pair_Impl = JavaPackageAndImportBuilder.buildImplClassReferenceFromUserPath(M3Paths.Pair);
    public static final String PrimitiveType = JavaPackageAndImportBuilder.buildInterfaceReferenceFromUserPath(M3Paths.PrimitiveType);
    public static final String Property = JavaPackageAndImportBuilder.buildInterfaceReferenceFromUserPath(M3Paths.Property);
    public static final String Property_Impl = JavaPackageAndImportBuilder.buildImplClassReferenceFromUserPath(M3Paths.Property);

    public static final String DefaultValue = JavaPackageAndImportBuilder.buildInterfaceReferenceFromUserPath(M3Paths.DefaultValue);

    public static final String SourceInformation = JavaPackageAndImportBuilder.buildInterfaceReferenceFromUserPath(M3Paths.SourceInformation);
    public static final String SourceInformation_Impl = JavaPackageAndImportBuilder.buildImplClassReferenceFromUserPath(M3Paths.SourceInformation);

    public static final String Type = JavaPackageAndImportBuilder.buildInterfaceReferenceFromUserPath(M3Paths.Type);
    public static final String TypeParameter = JavaPackageAndImportBuilder.buildInterfaceReferenceFromUserPath(M3Paths.TypeParameter);

    public static final String Unit = JavaPackageAndImportBuilder.buildImplClassReferenceFromUserPath(M3Paths.Unit);
    public static final String ValueSpecification_Impl = JavaPackageAndImportBuilder.buildImplClassReferenceFromUserPath(M3Paths.ValueSpecification);

    public static final String List = JavaPackageAndImportBuilder.buildImplClassReferenceFromUserPath(M3Paths.List);

    private FullJavaPaths()
    {
    }
}
