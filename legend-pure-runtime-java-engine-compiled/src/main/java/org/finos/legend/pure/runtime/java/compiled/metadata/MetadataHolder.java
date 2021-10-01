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

import org.finos.legend.pure.m3.coreinstance.meta.pure.metamodel.function.ConcreteFunctionDefinition;
import org.finos.legend.pure.m3.coreinstance.meta.pure.metamodel.function.LambdaFunction;
import org.finos.legend.pure.m3.coreinstance.meta.pure.metamodel.type.Any;
import org.finos.legend.pure.m3.coreinstance.meta.pure.metamodel.type.Class;
import org.finos.legend.pure.m3.coreinstance.meta.pure.metamodel.type.Enumeration;
import org.finos.legend.pure.m3.coreinstance.meta.pure.metamodel.type.Measure;
import org.finos.legend.pure.m3.coreinstance.meta.pure.metamodel.type.Nil;
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
    public Class<?> getClass(String fullPath)
    {
        return (Class)this.metadata.getMetadata(MetadataJavaPaths.Class, fullPath);
    }

    @Override
    public Measure getMeasure(String fullPath)
    {
        return (Measure)this.metadata.getMetadata(MetadataJavaPaths.Measure, fullPath);
    }

    @Override
    public Unit getUnit(String fullPath)
    {
        return (Unit)this.metadata.getMetadata(MetadataJavaPaths.Unit, fullPath);
    }

    @Override
    public Enumeration<?> getEnumeration(String fullPath)
    {
        return (Enumeration)this.metadata.getMetadata(MetadataJavaPaths.Enumeration, fullPath);
    }

    @Override
    public PrimitiveType getPrimitiveType(String name)
    {
        return (PrimitiveType)this.metadata.getMetadata(MetadataJavaPaths.PrimitiveType, name);
    }

    @Override
    public ConcreteFunctionDefinition<?> getConcreteFunctionDefinition(String name)
    {
        return (ConcreteFunctionDefinition)this.metadata.getMetadata(MetadataJavaPaths.ConcreteFunctionDefinition, name);
    }

    @Override
    public LambdaFunction<?> getLambdaFunction(String id)
    {
        return (LambdaFunction)this.metadata.getMetadata(MetadataJavaPaths.LambdaFunction, id);
    }

    @Override
    public org.finos.legend.pure.m3.coreinstance.Package getPackage(String path)
    {
        return (org.finos.legend.pure.m3.coreinstance.Package)this.metadata.getMetadata(M3Paths.Package, path);
    }

    @Override
    public Class<Any> getTopType()
    {
        return (Class<Any>)this.getClass("Root::" + M3Paths.Any);
    }

    @Override
    public Class<Nil> getBottomType()
    {
        return (Class<Nil>)this.getClass("Root::" + M3Paths.Nil);
    }

    @Override
    public org.finos.legend.pure.m3.coreinstance.meta.pure.metamodel.type.Enum getEnum(String enumerationName, String enumName)
    {
        return (org.finos.legend.pure.m3.coreinstance.meta.pure.metamodel.type.Enum)this.metadata.getEnum(enumerationName, enumName);
    }
}
