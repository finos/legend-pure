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

package org.finos.legend.pure.m3.navigation;

import org.eclipse.collections.api.list.ImmutableList;
import org.eclipse.collections.api.list.ListIterable;
import org.eclipse.collections.api.list.MutableList;
import org.eclipse.collections.api.set.ImmutableSet;
import org.eclipse.collections.impl.factory.Sets;
import org.finos.legend.pure.m3.navigation._package._Package;

/**
 * M4 property paths for standard properties in the M3 model.
 */
public class M3PropertyPaths
{
    private M3PropertyPaths()
    {
    }

    public static final ImmutableList<String> applications = makePropertyPath(M3Paths.Function, M3Properties.applications);
    public static final ImmutableList<String> children = makePropertyPath(M3Paths.Package, M3Properties.children);
    public static final ImmutableList<String> func = makePropertyPath(M3Paths.FunctionExpression, M3Properties.func);
    public static final ImmutableList<String> function = makePropertyPath(M3Paths.FunctionType, M3Properties.function);
    public static final ImmutableList<String> functionName_Function = makePropertyPath(M3Paths.Function, M3Properties.functionName);
    public static final ImmutableList<String> general = makePropertyPath(M3Paths.Generalization, M3Properties.general);
    public static final ImmutableList<String> generalizations = makePropertyPath(M3Paths.Type, M3Properties.generalizations);
    public static final ImmutableList<String> modelElements = makePropertyPath(M3Paths.Annotation, M3Properties.modelElements);
    public static final ImmutableList<String> multiplicityArguments = makePropertyPath(M3Paths.GenericType, M3Properties.multiplicityArguments);
    public static final ImmutableList<String> multiplicityParameters = makePropertyPath(M3Paths.Class, M3Properties.multiplicityParameters);
    public static final ImmutableList<String> name = makePropertyPath(M3Paths.ModelElement, M3Properties.name);
    public static final ImmutableList<String> offset_ReferenceUsage = makePropertyPath(M3Paths.ReferenceUsage, M3Properties.offset);
    public static final ImmutableList<String> owner_ReferenceUsage = makePropertyPath(M3Paths.ReferenceUsage, M3Properties.owner);
    public static final ImmutableList<String> _package = makePropertyPath(M3Paths.PackageableElement, M3Properties._package);
    public static final ImmutableList<String> properties = makePropertyPath(M3Paths.Class, M3Properties.properties);
    public static final ImmutableList<String> originalMilestonedProperties = makePropertyPath(M3Paths.Class, M3Properties.originalMilestonedProperties);
    public static final ImmutableList<String> propertiesFromAssociations = makePropertyPath(M3Paths.Class, M3Properties.propertiesFromAssociations);
    public static final ImmutableList<String> propertyName_ReferenceUsage = makePropertyPath(M3Paths.ReferenceUsage, M3Properties.propertyName);
    public static final ImmutableList<String> qualifiedProperties = makePropertyPath(M3Paths.Class, M3Properties.qualifiedProperties);
    public static final ImmutableList<String> qualifiedPropertiesFromAssociations = makePropertyPath(M3Paths.Class, M3Properties.qualifiedPropertiesFromAssociations);
    public static final ImmutableList<String> rawType = makePropertyPath(M3Paths.GenericType, M3Properties.rawType);
    public static final ImmutableList<String> referenceUsages_GenericType = makePropertyPath(M3Paths.GenericType, M3Properties.referenceUsages);
    public static final ImmutableList<String> referenceUsages_PackageableElement = makePropertyPath(M3Paths.PackageableElement, M3Properties.referenceUsages);
    public static final ImmutableList<String> referenceUsages_Function = makePropertyPath(M3Paths.Function, M3Properties.referenceUsages);
    public static final ImmutableList<String> specializations = makePropertyPath(M3Paths.Type, M3Properties.specializations);
    public static final ImmutableList<String> specific = makePropertyPath(M3Paths.Generalization, M3Properties.specific);
    public static final ImmutableList<String> typeArguments = makePropertyPath(M3Paths.GenericType, M3Properties.typeArguments);
    public static final ImmutableList<String> typeParameter = makePropertyPath(M3Paths.GenericType, M3Properties.typeParameter);
    public static final ImmutableList<String> typeParameters = makePropertyPath(M3Paths.Class, M3Properties.typeParameters);
    public static final ImmutableList<String> stereotypes = makePropertyPath(M3Paths.Class, M3Properties.stereotypes);
    public static final ImmutableList<String> taggedValues = makePropertyPath(M3Paths.Class, M3Properties.taggedValues);

    public static final ImmutableSet<ImmutableList<String>> BACK_REFERENCE_PROPERTY_PATHS = Sets.immutable.with(
            applications,
            modelElements,
            propertiesFromAssociations,
            qualifiedPropertiesFromAssociations,
            referenceUsages_GenericType,
            referenceUsages_PackageableElement,
            referenceUsages_Function,
            specializations
    );

    private static ImmutableList<String> makePropertyPath(String typePath, String propertyName)
    {
        return getM4PropertyPath(typePath, propertyName).toImmutable();
    }

    private static ListIterable<String> getM4PropertyPath(String typePath, String propertyName)
    {
        MutableList<String> path = _Package.convertM3PathToM4(typePath);
        path.add(M3Properties.properties);
        path.add(propertyName);
        return path;
    }
}
