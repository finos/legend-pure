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
import org.finos.legend.pure.m3.coreinstance.meta.pure.metamodel.type.Any;
import org.finos.legend.pure.m3.coreinstance.meta.pure.metamodel.type.Class;
import org.finos.legend.pure.m3.coreinstance.meta.pure.metamodel.type.Enum;
import org.finos.legend.pure.m3.coreinstance.meta.pure.metamodel.type.Enumeration;
import org.finos.legend.pure.m3.coreinstance.meta.pure.metamodel.type.Measure;
import org.finos.legend.pure.m3.coreinstance.meta.pure.metamodel.type.Nil;
import org.finos.legend.pure.m3.coreinstance.meta.pure.metamodel.type.PrimitiveType;
import org.finos.legend.pure.m3.coreinstance.meta.pure.metamodel.type.Unit;

/**
 * Provides key meta data
 */
public interface MetadataAccessor
{
    Class<?> getClass(String path);

    Measure getMeasure(String path);

    Unit getUnit(String path);

    Enumeration<?> getEnumeration(String path);

    Enum getEnum(String enumerationName, String enumName);

    PrimitiveType getPrimitiveType(String name);

    ConcreteFunctionDefinition<?> getConcreteFunctionDefinition(String path);

    NativeFunction<?> getNativeFunction(String path);

    LambdaFunction<?> getLambdaFunction(String id);

    Package getPackage(String path);

    Association getAssociation(String path);

    Profile getProfile(String path);

    @SuppressWarnings("unchecked")
    default Class<Any> getTopType()
    {
        return (Class<Any>) getClass("Root::meta::pure::metamodel::type::Any");
    }

    @SuppressWarnings("unchecked")
    default Class<Nil> getBottomType()
    {
        return (Class<Nil>) getClass("Root::meta::pure::metamodel::type::Nil");
    }
}
