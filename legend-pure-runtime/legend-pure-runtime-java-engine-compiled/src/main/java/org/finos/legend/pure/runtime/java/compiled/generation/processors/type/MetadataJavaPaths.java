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
import org.finos.legend.pure.m3.navigation.PackageableElement.PackageableElement;
import org.finos.legend.pure.m4.coreinstance.CoreInstance;

public class MetadataJavaPaths
{
    public static final String Any = buildMetadataKeyFromUserPath(M3Paths.Any);
    public static final String Class = buildMetadataKeyFromUserPath(M3Paths.Class);
    public static final String ConcreteFunctionDefinition = buildMetadataKeyFromUserPath(M3Paths.ConcreteFunctionDefinition);
    public static final String Enum = buildMetadataKeyFromUserPath(M3Paths.Enum);
    public static final String Enumeration = buildMetadataKeyFromUserPath(M3Paths.Enumeration);

    public static final String GenericType = buildMetadataKeyFromUserPath(M3Paths.GenericType);
    public static final String KeyExpression = buildMetadataKeyFromUserPath(M3Paths.KeyExpression);
    public static final String LambdaFunction = buildMetadataKeyFromUserPath(M3Paths.LambdaFunction);

    public static final String Measure = buildMetadataKeyFromUserPath(M3Paths.Measure);
    public static final String NativeFunction = buildMetadataKeyFromUserPath(M3Paths.NativeFunction);
    public static final String Nil = buildMetadataKeyFromUserPath(M3Paths.Nil);

    public static final String PackageableMultiplicity = buildMetadataKeyFromUserPath(M3Paths.PackageableMultiplicity);
    public static final String PrimitiveType = buildMetadataKeyFromUserPath(M3Paths.PrimitiveType);

    public static final String Type = buildMetadataKeyFromUserPath(M3Paths.Type);
    public static final String Unit = buildMetadataKeyFromUserPath(M3Paths.Unit);


    private MetadataJavaPaths()
    {
    }

    public static String buildMetadataKeyFromUserPath(String userPath)
    {
       return userPath;
    }

    public static String buildMetadataKeyFromType(CoreInstance element)
    {
       return PackageableElement.getUserPathForPackageableElement(element, "::");
    }
}
