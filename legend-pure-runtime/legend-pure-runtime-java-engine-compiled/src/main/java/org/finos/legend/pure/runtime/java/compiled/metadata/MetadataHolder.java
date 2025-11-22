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

package org.finos.legend.pure.runtime.java.compiled.metadata;

import org.finos.legend.pure.m3.coreinstance.Package;
import org.finos.legend.pure.m3.coreinstance.meta.pure.metamodel.extension.Profile;
import org.finos.legend.pure.m3.coreinstance.meta.pure.metamodel.function.ConcreteFunctionDefinition;
import org.finos.legend.pure.m3.coreinstance.meta.pure.metamodel.function.LambdaFunction;
import org.finos.legend.pure.m3.coreinstance.meta.pure.metamodel.function.NativeFunction;
import org.finos.legend.pure.m3.coreinstance.meta.pure.metamodel.relationship.Association;
import org.finos.legend.pure.m3.coreinstance.meta.pure.metamodel.type.Class;
import org.finos.legend.pure.m3.coreinstance.meta.pure.metamodel.type.Enum;
import org.finos.legend.pure.m3.coreinstance.meta.pure.metamodel.type.Enumeration;
import org.finos.legend.pure.m3.coreinstance.meta.pure.metamodel.type.Measure;
import org.finos.legend.pure.m3.coreinstance.meta.pure.metamodel.type.PrimitiveType;
import org.finos.legend.pure.m3.coreinstance.meta.pure.metamodel.type.Unit;
import org.finos.legend.pure.m3.navigation.M3Paths;
import org.finos.legend.pure.runtime.java.compiled.generation.processors.type.MetadataJavaPaths;

/**
 * Metadata Provider
 */
public class MetadataHolder implements MetadataAccessor
{
    private final Metadata metadata;

    public MetadataHolder(Metadata metadata)
    {
        this.metadata = metadata;
    }

    @Override
    public Class<?> getClass(String path)
    {
        return (Class<?>) this.metadata.getMetadata(MetadataJavaPaths.Class, path);
    }

    @Override
    public Measure getMeasure(String path)
    {
        return (Measure) this.metadata.getMetadata(MetadataJavaPaths.Measure, path);
    }

    @Override
    public Unit getUnit(String path)
    {
        return (Unit) this.metadata.getMetadata(MetadataJavaPaths.Unit, path);
    }

    @Override
    public Enumeration<?> getEnumeration(String path)
    {
        return (Enumeration<?>) this.metadata.getMetadata(MetadataJavaPaths.Enumeration, path);
    }

    @Override
    public Enum getEnum(String enumerationName, String enumName)
    {
        return (Enum) this.metadata.getEnum(enumerationName, enumName);
    }

    @Override
    public PrimitiveType getPrimitiveType(String name)
    {
        return (PrimitiveType) this.metadata.getMetadata(MetadataJavaPaths.PrimitiveType, name);
    }

    @Override
    public ConcreteFunctionDefinition<?> getConcreteFunctionDefinition(String path)
    {
        return (ConcreteFunctionDefinition<?>) this.metadata.getMetadata(MetadataJavaPaths.ConcreteFunctionDefinition, path);
    }

    @Override
    public NativeFunction<?> getNativeFunction(String path)
    {
        return (NativeFunction<?>) this.metadata.getMetadata(MetadataJavaPaths.NativeFunction, path);
    }

    @Override
    public LambdaFunction<?> getLambdaFunction(String id)
    {
        return (LambdaFunction<?>) this.metadata.getMetadata(MetadataJavaPaths.LambdaFunction, id);
    }

    @Override
    public Package getPackage(String path)
    {
        return (Package) this.metadata.getMetadata(M3Paths.Package, path);
    }

    @Override
    public Association getAssociation(String path)
    {
        return (Association) this.metadata.getMetadata(M3Paths.Association, path);
    }

    @Override
    public Profile getProfile(String path)
    {
        return (Profile) this.metadata.getMetadata(M3Paths.Profile, path);
    }
}
