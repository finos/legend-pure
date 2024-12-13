// Copyright 2024 Goldman Sachs
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

package org.finos.legend.pure.m2.dsl.mapping.test;

import org.eclipse.collections.api.RichIterable;
import org.eclipse.collections.api.factory.Lists;
import org.eclipse.collections.impl.tuple.Tuples;
import org.finos.legend.pure.m3.coreinstance.meta.pure.mapping.Mapping;
import org.finos.legend.pure.m3.coreinstance.meta.pure.mapping.PropertyMapping;
import org.finos.legend.pure.m3.coreinstance.meta.pure.mapping.relation.RelationFunctionInstanceSetImplementation;
import org.finos.legend.pure.m3.coreinstance.meta.pure.mapping.SetImplementation;
import org.finos.legend.pure.m3.coreinstance.meta.pure.mapping.relation.RelationFunctionPropertyMapping;
import org.finos.legend.pure.m3.coreinstance.meta.pure.metamodel.function.FunctionDefinition;
import org.finos.legend.pure.m3.coreinstance.meta.pure.metamodel.relation.RelationType;
import org.finos.legend.pure.m3.coreinstance.meta.pure.metamodel.type.Type;
import org.finos.legend.pure.m3.navigation.relation._Column;
import org.finos.legend.pure.m3.tests.AbstractPureTestWithCoreCompiled;
import org.finos.legend.pure.m3.tests.RuntimeVerifier;
import org.junit.After;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;

import static org.finos.legend.pure.m2.dsl.mapping.test.RelationMappingShared.RELATION_MAPPING_CLASS_SOURCE;
import static org.finos.legend.pure.m2.dsl.mapping.test.RelationMappingShared.RELATION_MAPPING_FUNCTION_SOURCE;

public class TestRelationMapping extends AbstractPureTestWithCoreCompiled
{
    @Before
    public void _setUp()
    {
        setUpRuntime();
    }

    @After
    public void _tearDown()
    {
        tearDownRuntime();
    }
    
    @Test
    public void testRelationMapping()
    {
        String mappingSource = "###Mapping\n" +
                "Mapping my::testMapping\n" +
                "(\n" +
                "  *my::Person[person]: Relation\n" +
                "  {\n" +
                "    ~func my::personFunction__Relation_1_\n" +
                "    firstName: FIRSTNAME,\n" +
                "    +age: Integer[0..1]: AGE\n" +
                "  }\n" +
                ")\n";

        compileTestSource("class.pure", RELATION_MAPPING_CLASS_SOURCE);
        compileTestSource("func.pure", RELATION_MAPPING_FUNCTION_SOURCE);
        compileTestSource("mapping.pure", mappingSource);
        RuntimeVerifier.deleteCompileAndReloadMultipleTimesIsStable(
            runtime, functionExecution, Lists.fixedSize.of(
                Tuples.pair("class.pure", RELATION_MAPPING_CLASS_SOURCE),
                Tuples.pair("func.pure", RELATION_MAPPING_FUNCTION_SOURCE)
            ), this.getAdditionalVerifiers()
        );
        
        RichIterable<? extends SetImplementation> setImpls = ((Mapping) runtime.getCoreInstance("my::testMapping"))._classMappings();
        Assert.assertEquals(1, setImpls.size());
        
        Assert.assertTrue(setImpls.getOnly() instanceof RelationFunctionInstanceSetImplementation);
        RelationFunctionInstanceSetImplementation relationSetImpl = (RelationFunctionInstanceSetImplementation) setImpls.getOnly();
        Assert.assertEquals("person", relationSetImpl._id());
        Assert.assertTrue(relationSetImpl._root());
        
        FunctionDefinition<?> relationFunction = relationSetImpl._relationFunction();
        Assert.assertEquals("personFunction", relationFunction._functionName());
        Type lastExpressionType = relationFunction._expressionSequence().getOnly()._genericType()._typeArguments().getOnly()._rawType();
        Assert.assertTrue(lastExpressionType instanceof RelationType);
        
        RichIterable<? extends PropertyMapping> propertyMappings = relationSetImpl._propertyMappings();
        Assert.assertEquals(2, propertyMappings.size());
        propertyMappings.each(r -> Assert.assertTrue(r instanceof RelationFunctionPropertyMapping));
        
        RelationFunctionPropertyMapping propertyMapping1 = (RelationFunctionPropertyMapping) propertyMappings.toList().get(0);
        Assert.assertFalse(propertyMapping1._localMappingProperty());
        Assert.assertEquals("person", propertyMapping1._sourceSetImplementationId());
        Assert.assertEquals("FIRSTNAME", propertyMapping1._column()._name());
        Assert.assertEquals("String", _Column.getColumnType(propertyMapping1._column())._rawType()._name());

        RelationFunctionPropertyMapping propertyMapping2 = (RelationFunctionPropertyMapping) propertyMappings.toList().get(1);
        Assert.assertTrue(propertyMapping2._localMappingProperty());
        Assert.assertEquals("person", propertyMapping2._sourceSetImplementationId());
        Assert.assertEquals("AGE", propertyMapping2._column()._name());
        Assert.assertEquals("Integer", _Column.getColumnType(propertyMapping2._column())._rawType()._name());
    }
}
