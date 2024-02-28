// Copyright 2021 Goldman Sachs
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

package org.finos.legend.pure.m2.relational.incremental;

import org.eclipse.collections.impl.factory.Lists;
import org.eclipse.collections.impl.factory.Maps;
import org.finos.legend.pure.m2.relational.AbstractPureRelationalTestWithCoreCompiled;
import org.finos.legend.pure.m3.tests.RuntimeTestScriptBuilder;
import org.finos.legend.pure.m3.tests.RuntimeVerifier;
import org.finos.legend.pure.m3.tests.RuntimeVerifier.FunctionExecutionStateVerifier;
import org.finos.legend.pure.m4.exception.PureCompilationException;
import org.junit.Assert;
import org.junit.Test;

public class TestMapping extends AbstractPureRelationalTestWithCoreCompiled
{

    private static final String CLASS_PERSON = "Class Person{name:String[1];}\n";
    private static final String RELATIONAL = "###Relational\n";
    private static final String DATABASE = "Database db(Table myTable(name VARCHAR(200)))\n";
    private static final String RELATIONAL_DATABASE = RELATIONAL + DATABASE;
    private static final String MAPPING = "###Mapping\n";
    private static final String CLASS_MAPPING_PERSON_RELATIONAL = "Mapping myMap(Person:Relational{name : [db]myTable.name})";
    private static final String CLASS_MAPPING = MAPPING +
                                                CLASS_MAPPING_PERSON_RELATIONAL;
    private static final String SOURCE_ID = "sourceId.pure";
    private static final String USER_ID = "userId.pure";
    private static final String MAPPING_SOURCE_ID = "mappingSourceId.pure";
    private static final String FUNCTION_TEST_CLASS_MAPPINGS_SIZE = "function test():Boolean[1]{assert(1 == myMap.classMappings->size(), |'');}";
    private static final String MAPPING_CLASS_MAPPING_PERSON_RELATIONAL = MAPPING + CLASS_MAPPING_PERSON_RELATIONAL;
    private static final String CLASS_PERSON_RELATIONAL_DATABASE = CLASS_PERSON + RELATIONAL_DATABASE;
    private static final String MAPPING_MAPPING_WITH_INCLUDE = MAPPING +
                                                               "Mapping myMapWithInclude(\n" +
                                                               " include myMap" +
                                                               ")\n";

    @Test
    public void testMappingReferenceError() throws Exception
    {
        this.runtime.createInMemorySource(SOURCE_ID, CLASS_PERSON +
                                                     RELATIONAL_DATABASE +
                                                     CLASS_MAPPING);
        this.runtime.createInMemorySource(USER_ID, FUNCTION_TEST_CLASS_MAPPINGS_SIZE);

        this.runtime.compile();
        int size = this.runtime.getModelRepository().serialize().length;

        for (int i = 0; i < 10; i++)
        {
            this.runtime.delete(SOURCE_ID);
            try
            {
                this.runtime.compile();
                Assert.fail();
            }
            catch (Exception e)
            {
                assertPureException(PureCompilationException.class, "myMap has not been defined!", "userId.pure", 1, 40, e);
            }

            try
            {
                this.runtime.createInMemorySource(SOURCE_ID, CLASS_PERSON +
                                                             RELATIONAL_DATABASE +
                                                             MAPPING +
                                                             "Mapping myMap2(Person:Relational{name : [db]myTable.name})"
                );
                this.runtime.compile();
                Assert.fail();
            }
            catch (Exception e)
            {
                assertPureException(PureCompilationException.class, "myMap has not been defined!", "userId.pure", 1, 40, e);
            }

            this.runtime.delete(SOURCE_ID);
            this.runtime.createInMemorySource(SOURCE_ID, CLASS_PERSON + RELATIONAL_DATABASE + CLASS_MAPPING);
            this.runtime.compile();
            Assert.assertEquals(size, this.runtime.getModelRepository().serialize().length);
        }
    }

    @Test
    public void testMappingClassReference() throws Exception
    {
        this.runtime.createInMemorySource("sourceId.pure", "Class Person{name:String[1];}\n");
        this.runtime.createInMemorySource("userId.pure", "###Relational\n" +
                                                    "Database db(Table myTable(name VARCHAR(200)))\n" +
                                                    "###Mapping\n" +
                                                    "Mapping myMap(Person:Relational{name : [db]myTable.name})");

        this.runtime.compile();
        int size = this.runtime.getModelRepository().serialize().length;

        for (int i = 0; i < 10; i++)
        {
            this.runtime.delete(SOURCE_ID);
            try
            {
                this.runtime.compile();
                Assert.fail();
            }
            catch (Exception e)
            {
                assertPureException(PureCompilationException.class, "Person has not been defined!", "userId.pure", 4, 15, e);
            }

            this.runtime.createInMemorySource(SOURCE_ID, CLASS_PERSON);
            this.runtime.compile();
            Assert.assertEquals(size, this.runtime.getModelRepository().serialize().length);
        }
    }

    @Test
    public void testMappingMainTable() throws Exception
    {
        this.runtime.createInMemorySource("sourceId.pure", "###Relational\n" +
                                                      "Database db(Table myTable(name VARCHAR(200)))\n");
        this.runtime.createInMemorySource("userId.pure", "###Pure\n" +
                                                    "Class Person{name : String[1];}\n" +
                                                    "###Mapping\n" +
                                                    "Mapping myMap(\n" +
                                                    "   Person:Relational{\n" +
                                                    "       ~mainTable [db]myTable\n" +
                                                    "       name : [db]myTable.name\n" +
                                                    "   }\n" +
                                                    ")\n");

        this.runtime.compile();
        int size = this.runtime.getModelRepository().serialize().length;

        for (int i = 0; i < 10; i++)
        {
            this.runtime.delete(SOURCE_ID);
            try
            {
                this.runtime.compile();
                Assert.fail();
            }
            catch (Exception e)
            {
                assertPureException(PureCompilationException.class, "db has not been defined!", "userId.pure", 7, 16, e);
            }

            this.runtime.createInMemorySource(SOURCE_ID, "###Relational\n" +
                                                         "Database db(Table myTable(name VARCHAR(200)))\n");
            this.runtime.compile();
            Assert.assertEquals(size, this.runtime.getModelRepository().serialize().length);
        }
    }


    @Test
    public void testMappingClassReferenceError() throws Exception
    {
        this.runtime.createInMemorySource(SOURCE_ID, CLASS_PERSON);
        this.runtime.createInMemorySource(USER_ID, RELATIONAL_DATABASE +
                                                   MAPPING +
                                                   "Mapping myMap(Person:Relational{name : [db]myTable.name})\n" +
                                                   "###Pure\n" +
                                                   "function test():Boolean[1]{assert('Person' == myMap.classMappings->first().class.name, |'');}");

        this.runtime.compile();
        int size = this.runtime.getModelRepository().serialize().length;

        for (int i = 0; i < 10; i++)
        {
            this.runtime.delete(SOURCE_ID);
            try
            {
                this.runtime.compile();
                Assert.fail();
            }
            catch (Exception e)
            {
                assertPureException(PureCompilationException.class, "Person has not been defined!", "userId.pure", 4, 15, e);
            }

            try
            {
                this.runtime.createInMemorySource(SOURCE_ID, "Class PersonError{name:String[1];}\n");
                this.runtime.compile();
                Assert.fail();
            }
            catch (Exception e)
            {
                assertPureException(PureCompilationException.class, "Person has not been defined!", "userId.pure", 4, 15, e);
            }

            this.runtime.delete(SOURCE_ID);
            this.runtime.createInMemorySource(SOURCE_ID, CLASS_PERSON);
            this.runtime.compile();
            Assert.assertEquals(size, this.runtime.getModelRepository().serialize().length);
        }
    }

    @Test
    public void testMappingIncludeReferenceErrorAndReAttach() throws Exception
    {
        RuntimeVerifier.verifyOperationIsStable(new RuntimeTestScriptBuilder().createInMemorySources(Maps.immutable.with(SOURCE_ID, CLASS_PERSON_RELATIONAL_DATABASE, MAPPING_SOURCE_ID, MAPPING_CLASS_MAPPING_PERSON_RELATIONAL, USER_ID, MAPPING_MAPPING_WITH_INCLUDE))
                        .compile(),
                new RuntimeTestScriptBuilder()
                        .deleteSources(Lists.fixedSize.of(MAPPING_SOURCE_ID))
                        .compileWithExpectedCompileFailure("myMap has not been defined!", "userId.pure", 3, 10)
                        .createInMemorySource(MAPPING_SOURCE_ID, MAPPING_CLASS_MAPPING_PERSON_RELATIONAL)
                        .compile(),
                this.runtime, this.functionExecution, Lists.fixedSize.<FunctionExecutionStateVerifier>of());
    }

    @Test
    public void testAddRemoveMappingWithInclude() throws Exception
    {
        RuntimeVerifier.verifyOperationIsStable(new RuntimeTestScriptBuilder().createInMemorySources(Maps.immutable.with(SOURCE_ID, CLASS_PERSON_RELATIONAL_DATABASE, MAPPING_SOURCE_ID, MAPPING_CLASS_MAPPING_PERSON_RELATIONAL, USER_ID, MAPPING_MAPPING_WITH_INCLUDE))
                        .compile(),
                new RuntimeTestScriptBuilder()
                        .deleteSources(Lists.fixedSize.of(USER_ID))
                        .createInMemorySource(USER_ID, MAPPING_MAPPING_WITH_INCLUDE)
                        .compile(),
                this.runtime, this.functionExecution, Lists.fixedSize.<FunctionExecutionStateVerifier>of());
        this.setUpRuntime();
    }

    @Test
    public void testMappingClassReferenceUsingQualifierError() throws Exception
    {
        this.runtime.createInMemorySource(USER_ID, CLASS_PERSON +
                                                   RELATIONAL_DATABASE +
                                                   MAPPING +
                                                   "Mapping myMap(Person:Relational{name : [db]myTable.name})\n" +
                                                   "###Pure\n" +
                                                   "function test():Boolean[1]{assert('Person' == myMap->meta::pure::mapping::_classMappingByClass(Person).class.name, |'');}");
        this.runtime.compile();
        int size = this.runtime.getModelRepository().serialize().length;

        this.runtime.delete(USER_ID);
        this.runtime.createInMemorySource(USER_ID, CLASS_PERSON +
                                                   RELATIONAL_DATABASE +
                                                   MAPPING +
                                                   "Mapping myMap(PersonXX:Relational{name : [db]myTable.name})\n" +
                                                   "###Pure\n" +
                                                   "function test():Boolean[1]{assert('Person' == myMap->meta::pure::mapping::_classMappingByClass(Person).class.name, |'');}");
        try
        {
            this.runtime.compile();
        }
        catch (Exception e)
        {
            assertPureException(PureCompilationException.class, "PersonXX has not been defined!", "userId.pure", 5, 15, e);
        }
        this.runtime.delete(USER_ID);
        this.runtime.createInMemorySource(USER_ID, CLASS_PERSON +
                                                   RELATIONAL_DATABASE +
                                                   MAPPING +
                                                   "Mapping myMap(Person:Relational{name : [db]myTable.name})\n" +
                                                   "###Pure\n" +
                                                   "function test():Boolean[1]{assert('Person' == myMap->meta::pure::mapping::_classMappingByClass(Person).class.name, |'');}");
        this.runtime.compile();
        Assert.assertEquals(size, this.runtime.getModelRepository().serialize().length);
    }


    @Test
    public void testMappingDBReferenceColumn() throws Exception
    {
        this.runtime.createInMemorySource(SOURCE_ID, RELATIONAL_DATABASE);
        this.runtime.createInMemorySource(USER_ID, CLASS_PERSON +
                MAPPING +
                "Mapping myMap(Person:Relational{name:[db]myTable.name})\n" +
                "###Pure\n" +
                "function test():Boolean[1]" +
                "{" +
                "   assert('db' == myMap.classMappings->first()->cast(@meta::relational::mapping::RelationalInstanceSetImplementation).propertyMappings->first()->cast(@meta::relational::mapping::RelationalPropertyMapping).relationalOperationElement->cast(@meta::relational::metamodel::TableAliasColumn).alias.database->toOne()->id(), |'');" +
                "   assert('name' == myMap.classMappings->first()->cast(@meta::relational::mapping::RelationalInstanceSetImplementation).propertyMappings->at(0)->cast(@meta::relational::mapping::RelationalPropertyMapping).relationalOperationElement->cast(@meta::relational::metamodel::TableAliasColumn).column.name, |'');" +
                "}");

        this.runtime.compile();
        int size = this.runtime.getModelRepository().serialize().length;

        for (int i = 0; i < 10; i++)
        {
            this.runtime.delete(SOURCE_ID);
            try
            {
                this.runtime.compile();
                Assert.fail();
            }
            catch (Exception e)
            {
                assertPureException(PureCompilationException.class, "db has not been defined!", "userId.pure", 3, 7, e);
            }

            this.runtime.createInMemorySource(SOURCE_ID, RELATIONAL_DATABASE);
            this.runtime.compile();
            Assert.assertEquals(size, this.runtime.getModelRepository().serialize().length);
        }
    }


    @Test
    public void testMappingDBReferenceColumnFunc() throws Exception
    {
        this.runtime.createInMemorySource(SOURCE_ID, RELATIONAL_DATABASE);
        this.runtime.createInMemorySource(USER_ID, CLASS_PERSON +
                                                   MAPPING +
                                                   "Mapping myMap(Person:Relational{name: f([db]myTable.name)})\n" +
                                                   "###Pure\n" +
                                                   "function test():Boolean[1]" +
                                                   "{" +
                                                   "   true;" +
                                                   "}");

        this.runtime.compile();
        int size = this.runtime.getModelRepository().serialize().length;

        for (int i = 0; i < 10; i++)
        {
            this.runtime.delete(SOURCE_ID);
            try
            {
                this.runtime.compile();
                Assert.fail();
            }
            catch (Exception e)
            {
                assertPureException(PureCompilationException.class, "db has not been defined!", "userId.pure", 3, 10, e);
            }

            this.runtime.createInMemorySource(SOURCE_ID, RELATIONAL_DATABASE);
            this.runtime.compile();
            Assert.assertEquals(size, this.runtime.getModelRepository().serialize().length);
        }
    }

    @Test
    public void testMappingDBReferenceColumnError() throws Exception
    {
        this.runtime.createInMemorySource(SOURCE_ID, RELATIONAL_DATABASE);
        this.runtime.createInMemorySource(USER_ID, CLASS_PERSON +
                                                   MAPPING +
                                                   "Mapping myMap(Person:Relational{name:[db]myTable.name})\n" +
                                                   "###Pure\n" +
                                                   "function test():Boolean[1]" +
                                                   "{" +
                                                   "   assert('name' == myMap.classMappings->first()->cast(@meta::relational::mapping::RelationalInstanceSetImplementation).propertyMappings->first()->cast(@meta::relational::mapping::RelationalPropertyMapping).relationalOperationElement->cast(@meta::relational::metamodel::TableAliasColumn).column.name, |'');" +
                                                   "}");

        this.runtime.compile();
        int size = this.runtime.getModelRepository().serialize().length;

        for (int i = 0; i < 10; i++)
        {
            this.runtime.delete(SOURCE_ID);
            try
            {
                this.runtime.compile();
                Assert.fail();
            }
            catch (Exception e)
            {
                assertPureException(PureCompilationException.class, "db has not been defined!", "userId.pure", 3, 7, e);
            }

            try
            {
                this.runtime.createInMemorySource(SOURCE_ID, RELATIONAL +
                                                             "Database db(Table myTable2(name VARCHAR(200)))\n");
                this.runtime.compile();
                Assert.fail();
            }
            catch (Exception e)
            {
                assertPureException(PureCompilationException.class, "The table 'myTable' can't be found in the schema 'default' in the database 'db'", "userId.pure", 3, 10, e);
            }

            this.runtime.delete(SOURCE_ID);
            this.runtime.createInMemorySource(SOURCE_ID, RELATIONAL_DATABASE);
            this.runtime.compile();
            Assert.assertEquals(size, this.runtime.getModelRepository().serialize().length);
        }
    }


    @Test
    public void testMappingDBReferenceJoin() throws Exception
    {
        this.runtime.createInMemorySource(SOURCE_ID, RELATIONAL +
                                                     "Database db(Table personTb(name VARCHAR(200), firm VARCHAR(200)) Table firmTb(name VARCHAR(200)) Join myJoin(personTb.firm = firmTb.name))\n");
        this.runtime.createInMemorySource(USER_ID, "Class Person{name:String[1];firm:Firm[1];}" +
                                                   "Class Firm{name:String[1];}\n" +
                                                   MAPPING +
                                                   "Mapping myMap(Firm:Relational{name:[db]firmTb.name} Person:Relational{firm:[db]@myJoin, name:[db]personTb.name})\n" +
                                                   "###Pure\n" +
                                                   "function test():Boolean[1]" +
                                                   "{" +
                                                   "  print('ok',1);" +
                                                   "   assert('myJoin' == myMap.classMappings->first()->cast(@meta::relational::mapping::RelationalInstanceSetImplementation).propertyMappings->at(0)->cast(@meta::relational::mapping::RelationalPropertyMapping).relationalOperationElement->cast(@meta::relational::metamodel::RelationalOperationElementWithJoin).joinTreeNode.join.name, |'');" +
                                                   "}");

        this.runtime.compile();
        int size = this.runtime.getModelRepository().serialize().length;

        for (int i = 0; i < 10; i++)
        {
            this.runtime.delete(SOURCE_ID);
            try
            {
                this.runtime.compile();
                Assert.fail();
            }
            catch (Exception e)
            {
                Assert.assertTrue(e.getMessage().equals("Compilation error at (resource:userId.pure line:3 column:7), \"db has not been defined!\"") |
                        e.getMessage().equals("Compilation error at (resource:userId.pure line:3 column:31), \"db has not been defined!\""));
            }

            this.runtime.createInMemorySource(SOURCE_ID, RELATIONAL +
                                                         "Database db(Table personTb(name VARCHAR(200), firm VARCHAR(200)) Table firmTb(name VARCHAR(200)) Join myJoin(personTb.firm = firmTb.name))\n");
            this.runtime.compile();
            Assert.assertEquals(size, this.runtime.getModelRepository().serialize().length);
        }
    }

    @Test
    public void testMappingDBReferenceJoinError() throws Exception
    {
        this.runtime.createInMemorySource(SOURCE_ID, RELATIONAL +
                                                     "Database db(Table personTb(name VARCHAR(200), firm VARCHAR(200)) Table firmTb(name VARCHAR(200)) Join myJoin(personTb.firm = firmTb.name))\n");
        this.runtime.createInMemorySource(USER_ID, "Class Person{name:String[1];firm:Firm[1];}Class Firm{name:String[1];}\n" +
                                                   MAPPING +
                                                   "Mapping myMap(Firm:Relational{name:[db]firmTb.name} Person:Relational{name:[db]personTb.name, firm:[db]@myJoin})\n" +
                                                   "###Pure\n" +
                                                   "function test():Boolean[1]" +
                                                   "{" +
                                                   "   assert('myJoin' == myMap.classMappings->first()->cast(@meta::relational::mapping::RelationalInstanceSetImplementation).propertyMappings->at(1)->cast(@meta::relational::mapping::RelationalPropertyMapping).relationalOperationElement->cast(@meta::relational::metamodel::RelationalOperationElementWithJoin).joinTreeNode.join.name, |'');" +
                                                   "}");

        this.runtime.compile();
        int size = this.runtime.getModelRepository().serialize().length;

        for (int i = 0; i < 10; i++)
        {
            this.runtime.delete(SOURCE_ID);
            try
            {
                this.runtime.compile();
                Assert.fail();
            }
            catch (Exception e)
            {
                Assert.assertTrue(e.getMessage().equals("Compilation error at (resource:userId.pure line:3 column:7), \"db has not been defined!\"") |
                                  e.getMessage().equals("Compilation error at (resource:userId.pure line:3 column:31), \"db has not been defined!\""));
            }

            try
            {
                this.runtime.createInMemorySource(SOURCE_ID, RELATIONAL +
                                                             "Database db(Table personTb(name VARCHAR(200), firm VARCHAR(200)) Table firmTb(name VARCHAR(200)) Join myJoin32(personTb.firm = firmTb.name))\n");
                this.runtime.compile();
                Assert.fail();
            }
            catch (Exception e)
            {
                assertPureException(PureCompilationException.class, "The join 'myJoin' has not been found in the database 'db'", "userId.pure", 3, 35, e);
            }

            this.runtime.delete(SOURCE_ID);
            this.runtime.createInMemorySource(SOURCE_ID, RELATIONAL +
                    "Database db(Table personTb(name VARCHAR(200), firm VARCHAR(200)) Table firmTb(name VARCHAR(200)) Join myJoin(personTb.firm = firmTb.name))\n");
            this.runtime.compile();
            Assert.assertEquals(size, this.runtime.getModelRepository().serialize().length);
        }
    }

    @Test
    public void testMappingDBReferenceJoinWithTargetId() throws Exception
    {
        this.runtime.createInMemorySource(SOURCE_ID, "Class Person{name:String[1];firm:Firm[1];}" +
                                                     "Class Firm{name:String[1];}\n" +
                                                     RELATIONAL +
                                                     "Database db(Table personTb(name VARCHAR(200), firm VARCHAR(200)) Table firmTb(name VARCHAR(200)) Join myJoin(personTb.firm = firmTb.name))\n");
        this.runtime.compile();
        int size = this.runtime.getModelRepository().serialize().length;

        for (int i = 0; i < 10; i++)
        {
            this.runtime.createInMemorySource(USER_ID, MAPPING +
                                                       "Mapping myMap(" +
                                                       "    Firm[targetId]: Relational" +
                                                       "          {" +
                                                       "             name : [db]firmTb.name" +
                                                       "          }" +
                                                       "    Person: Relational" +
                                                       "            {" +
                                                       "                firm[targetId]:[db]@myJoin," +
                                                       "                name:[db]personTb.name" +
                                                       "            }" +
                                                       ")\n" +
                                                       "###Pure\n" +
                                                       "function test():Boolean[1]" +
                                                       "{" +
                                                       "   assert('targetId' == myMap->meta::pure::mapping::classMappingById('targetId')->cast(@meta::relational::mapping::RelationalInstanceSetImplementation).id, |'');" +
                                                       "}");
            this.runtime.compile();
            this.runtime.delete(USER_ID);
            this.runtime.compile();
            Assert.assertEquals("Failed on iteration #" + i, size, this.runtime.getModelRepository().serialize().length);
        }
    }


    @Test
    public void testMappingDBReferenceDifferentIDs() throws Exception
    {
        this.runtime.createInMemorySource(SOURCE_ID, "Class Person{name:String[1];firm:Firm[1];}" +
                                                     "Class Firm{name:String[1];}\n" +
                                                     RELATIONAL +
                                                     "Database db(Table personTb(name VARCHAR(200), firm VARCHAR(200)) Table firmTb(name VARCHAR(200)) Join myJoin(personTb.firm = firmTb.name))\n");
        this.runtime.compile();
        int size = this.runtime.getModelRepository().serialize().length;

        for (int i = 0; i < 10; i++)
        {
            this.runtime.createInMemorySource(USER_ID, MAPPING +
                                                       "Mapping myMap(" +
                                                       "    Firm[targetId]: Relational" +
                                                       "          {" +
                                                       "             name : [db]firmTb.name" +
                                                       "          }" +
                                                       "    *Firm[targetId2]: Relational" +
                                                       "          {" +
                                                       "             name : [db]firmTb.name" +
                                                       "          }" +
                                                       "    Person: Relational" +
                                                       "            {" +
                                                       "                firm[targetId]:[db]@myJoin," +
                                                       "                name:[db]personTb.name" +
                                                       "            }" +
                                                       ")\n" +
                                                       "###Pure\n" +
                                                       "function test():Boolean[1]" +
                                                       "{" +
                                                       "   assert('targetId' == myMap->meta::pure::mapping::classMappingById('targetId')->cast(@meta::relational::mapping::RelationalInstanceSetImplementation).id, |'');" +
                                                       "}");
            this.runtime.compile();
            this.runtime.delete(USER_ID);
            this.runtime.compile();
            Assert.assertEquals("Failed on iteration #" + i, size, this.runtime.getModelRepository().serialize().length);
        }
    }

    @Test
    public void testMappingDBReferenceErrorSameIDs() throws Exception
    {
        this.runtime.createInMemorySource(SOURCE_ID, "Class Person{name:String[1];firm:Firm[1];}" +
                                                     "Class Firm{name:String[1];}\n" +
                                                     RELATIONAL +
                                                     "Database db(Table personTb(name VARCHAR(200), firm VARCHAR(200)) Table firmTb(name VARCHAR(200)) Join myJoin(personTb.firm = firmTb.name))\n");
        this.runtime.compile();
        int size = this.runtime.getModelRepository().serialize().length;

        for (int i = 0; i < 10; i++)
        {
            this.runtime.createInMemorySource(USER_ID, MAPPING +
                    "Mapping myMap(" +
                    "    Firm[targetId]: Relational" +
                    "          {" +
                    "             name : [db]firmTb.name" +
                    "          }" +
                    "    Firm[targetId]: Relational" +
                    "          {" +
                    "             name : [db]firmTb.name" +
                    "          }" +
                    "    Person: Relational" +
                    "            {" +
                    "                firm[targetId]:[db]@myJoin," +
                    "                name:[db]personTb.name" +
                    "            }" +
                    ")\n" +
                    "###Pure\n" +
                    "function test():Boolean[1]" +
                    "{" +
                    "   assert('targetId' == myMap->meta::pure::mapping::classMappingById('targetId')->cast(@meta::relational::mapping::RelationalInstanceSetImplementation).id, |'');" +
                    "}");
            try
            {
                this.runtime.compile();
                Assert.fail();
            }
            catch (Exception e)
            {
                assertPureException(PureCompilationException.class, "Duplicate mapping found with id: 'targetId' in mapping myMap", "userId.pure", 2, 106, e);
            }
            this.runtime.delete(USER_ID);
        }

        Assert.assertEquals(size, this.runtime.getModelRepository().serialize().length);
    }

    @Test
    public void testMappingDBReferenceErrorSameIDImplicit() throws Exception
    {
        this.runtime.createInMemorySource(SOURCE_ID, "Class Person{name:String[1];firm:Firm[1];}" +
                                                     "Class Firm{name:String[1];}\n" +
                                                     RELATIONAL +
                                                     "Database db(Table personTb(name VARCHAR(200), firm VARCHAR(200)) Table firmTb(name VARCHAR(200)) Join myJoin(personTb.firm = firmTb.name))\n");
        this.runtime.compile();
        int size = this.runtime.getModelRepository().serialize().length;

        for (int i = 0; i < 10; i++)
        {
            this.runtime.createInMemorySource(USER_ID, MAPPING +
                                                       "Mapping myMap(" +
                                                       "    Firm: Relational" +
                                                       "          {" +
                                                       "             name : [db]firmTb.name" +
                                                       "          }" +
                                                       "    Firm: Relational" +
                                                       "          {" +
                                                       "             name : [db]firmTb.name" +
                                                       "          }" +
                                                       "    Person: Relational" +
                                                       "            {" +
                                                       "                firm[firm]:[db]@myJoin," +
                                                       "                name:[db]personTb.name" +
                                                       "            }" +
                                                       ")\n" +
                                                       "###Pure\n" +
                                                       "function test():Boolean[1]" +
                                                       "{" +
                                                       "   assert('targetId' == myMap->meta::pure::mapping::classMappingById('targetId')->cast(@meta::relational::mapping::RelationalInstanceSetImplementation).id, |'');" +
                                                       "}");
            try
            {

                this.runtime.compile();
                Assert.fail();
            }
            catch (Exception e)
            {
                assertPureException(PureCompilationException.class, "Duplicate mapping found with id: 'Firm' in mapping myMap", "userId.pure", 2, 96, e);
            }
            this.runtime.delete(USER_ID);
        }

        Assert.assertEquals(size, this.runtime.getModelRepository().serialize().length);
    }


    @Test
    public void testMappingWithIncludeEmptyMain() throws Exception
    {
        this.runtime.createInMemorySource(SOURCE_ID, "Class Person{name:String[1];firm:Firm[1];}" +
                                                     "Class Firm{name:String[1];}\n" +
                                                     RELATIONAL +
                                                     "Database subDb" +
                                                     "(" +
                                                     "  Table personTb(name VARCHAR(200), firm VARCHAR(200))" +
                                                     "  Table firmTb(name VARCHAR(200))" +
                                                     ")\n" +
                                                     RELATIONAL +
                                                     "Database db" +
                                                     "(" +
                                                     "  include subDb" +
                                                     ")\n");
        this.runtime.compile();
        int size = this.runtime.getModelRepository().serialize().length;

        for (int i = 0; i < 10; i++)
        {
            this.runtime.createInMemorySource(USER_ID, MAPPING +
                                                       "Mapping myMap(" +
                                                       "    Firm: Relational" +
                                                       "          {" +
                                                       "             name : [db]firmTb.name" +
                                                       "          }" +
                                                       "    Person: Relational" +
                                                       "            {" +
                                                       "                name:[db]personTb.name" +
                                                       "            }" +
                                                       ")\n");
            this.runtime.compile();
            this.runtime.delete(USER_ID);
            this.runtime.compile();
            Assert.assertEquals("Failed on iteration #" + i, size, this.runtime.getModelRepository().serialize().length);
        }
    }


    @Test
    public void testMappingWithIncludeJoin() throws Exception
    {
        this.runtime.createInMemorySource(SOURCE_ID, "Class Person{name:String[1];firm:Firm[1];}" +
                                                     "Class Firm{name:String[1];}\n" +
                                                     RELATIONAL +
                                                     "Database subDb" +
                                                     "(" +
                                                     "  Table personTb(name VARCHAR(200), firm VARCHAR(200))" +
                                                     "  Table firmTb(name VARCHAR(200))" +
                                                     "  Join myJoin(personTb.firm = firmTb.name)" +
                                                     ")\n" +
                                                     RELATIONAL +
                                                     "Database db" +
                                                     "(" +
                                                     "  include subDb" +
                                                     ")\n");
        this.runtime.compile();
        int size = this.runtime.getModelRepository().serialize().length;

        for (int i = 0; i < 10; i++)
        {
            this.runtime.createInMemorySource(USER_ID, MAPPING +
                                                       "Mapping myMap(" +
                                                       "    Firm: Relational" +
                                                       "          {" +
                                                       "             name : [db]firmTb.name" +
                                                       "          }" +
                                                       "    Person: Relational" +
                                                       "            {" +
                                                       "                firm[firm]:[db]@myJoin," +
                                                       "                name:[db]personTb.name" +
                                                       "            }" +
                                                       ")");
            this.runtime.compile();
            this.runtime.delete(USER_ID);
            this.runtime.compile();
            Assert.assertEquals("Failed on iteration #" + i, size, this.runtime.getModelRepository().serialize().length);
        }
    }


    @Test
    public void testMappingWithIncludeAndSchema() throws Exception
    {
        this.runtime.createInMemorySource(SOURCE_ID, "Class Person{name:String[1];firm:Firm[1];}" +
                                                     "Class Firm{name:String[1];}\n" +
                                                     RELATIONAL +
                                                     "Database subDb" +
                                                     "(" +
                                                     "  Schema s" +
                                                     "  (" +
                                                     "      Table personTb(name VARCHAR(200), firm VARCHAR(200))" +
                                                     "  )" +
                                                     "  Table firmTb(name VARCHAR(200))" +
                                                     "  Join myJoin(s.personTb.firm = firmTb.name)" +
                                                     ")\n" +
                                                     RELATIONAL +
                                                     "Database db" +
                                                     "(" +
                                                     "  include subDb" +
                                                     ")\n");
        this.runtime.compile();
        int size = this.runtime.getModelRepository().serialize().length;

        for (int i = 0; i < 10; i++)
        {
            this.runtime.createInMemorySource(USER_ID, MAPPING +
                                                       "Mapping myMap(" +
                                                       "    Firm: Relational" +
                                                       "          {" +
                                                       "             name : [db]firmTb.name" +
                                                       "          }" +
                                                       "    Person: Relational" +
                                                       "            {" +
                                                       "                firm[firm]:[db]@myJoin," +
                                                       "                name:[db]s.personTb.name" +
                                                       "            }" +
                                                       ")");
            this.runtime.compile();
            this.runtime.delete(USER_ID);
            this.runtime.compile();
            Assert.assertEquals("Failed on iteration #" + i, size, this.runtime.getModelRepository().serialize().length);
        }
    }


    @Test
    public void testMappingWithStoreInclude() throws Exception
    {
        this.runtime.createInMemorySource(SOURCE_ID, "Class Person{name:String[1];firm:Firm[1];}" +
                "Class Firm{name:String[1];}\n" +
                RELATIONAL +
                "Database subDb" +
                "(" +
                "  Table personTb(name VARCHAR(200), firm VARCHAR(200))" +
                "  Table firmTb(name VARCHAR(200))" +
                ")\n" +
                RELATIONAL +
                "Database db" +
                "(" +
                "  include subDb" +
                ")\n" +
                MAPPING +
                "Mapping myMap(" +
                "    Firm[m1]: Relational" +
                "          {" +
                "             name : [db]firmTb.name" +
                "          }" +
                "    *Firm[m2]: Relational" +
                "          {" +
                "             name : [db]firmTb.name" +
                "          }" +
                ")\n");
        this.runtime.compile();
    }

    @Test
    public void testMappingWithMappingAndStoreInclude() throws Exception
    {
        this.runtime.createInMemorySource(SOURCE_ID, "Class Person{name:String[1];firm:Firm[1];}" +
                "Class Firm{name:String[1];}\n" +
                RELATIONAL +
                "Database subDb" +
                "(" +
                "  Table personTb(name VARCHAR(200), firm VARCHAR(200))" +
                "  Table firmTb(name VARCHAR(200))" +
                ")\n" +
                RELATIONAL +
                "Database db" +
                "(" +
                "  include subDb" +
                ")\n" +
                MAPPING +
                "Mapping myMap(" +
                "    Firm[m1]: Relational" +
                "          {" +
                "             name : [db]firmTb.name" +
                "          }" +
                "    *Firm[m2]: Relational" +
                "          {" +
                "             name : [db]firmTb.name" +
                "          }" +
                ")\n");
        this.runtime.compile();

        this.runtime.createInMemorySource(MAPPING_SOURCE_ID, MAPPING +
                "Mapping myMap2\n" +
                "(\n" +
                "  include myMap\n" +
                ")");
        this.runtime.compile();

        this.runtime.delete(SOURCE_ID);
        try
        {
            this.runtime.compile();
            Assert.fail("Expected compilation exception");
        }
        catch (Exception e)
        {
            assertPureException(PureCompilationException.class, "myMap has not been defined!", MAPPING_SOURCE_ID, 4, 11, e);
        }
    }

    @Test
    public void testMappingWithJoinSequence()
    {
        this.runtime.createInMemorySource("testModel.pure", "###Pure\n" +
                "\n" +
                "Class Table1\n" +
                "{\n" +
                "   name: String[1];\n" +
                "   table3: Table3[0..1];\n" +
                "}\n" +
                "\n" +
                "Class Table2\n" +
                "{\n" +
                "   name:String[1];\n" +
                "}\n" +
                "Class Table3\n" +
                "{\n" +
                "   name:String[1];\n" +
                "}\n");

        this.runtime.createInMemorySource("testRelational.pure", "###Relational\n" +
                "Database db\n" +
                "(\n" +
                "   Table table1\n" +
                "   (\n" +
                "       name VARCHAR(200) PRIMARY KEY,\n" +
                "       t2name VARCHAR(200)\n" +
                "   )\n" +
                "   Table table2\n" +
                "   (\n" +
                "       name VARCHAR(200) PRIMARY KEY,\n" +
                "       t3name VARCHAR(200)\n" +
                "   )\n" +
                "   Table table3\n" +
                "   (\n" +
                "       name VARCHAR(200) PRIMARY KEY\n" +
                "   )\n" +
                "   Join T1_T2(table1.t2name = table2.name)" +
                "   Join T2_T3(table2.t3name = table3.name)\n" +
                ")");

        this.runtime.createInMemorySource("testMapping.pure", "###Mapping\n" +
                "Mapping mapping\n" +
                "(\n" +
                "    Table1: Relational\n" +
                "    {\n" +
                "        name: [db]table1.name,\n" +
                "        table3: [db]@T1_T2 > (INNER) [db]@T2_T3\n" +
                "    }\n" +
                "\n" +
                "    Table2: Relational\n" +
                "    {\n" +
                "        name: [db]table2.name\n" +
                "    }\n" +
                "\n" +
                "    Table3: Relational\n" +
                "    {\n" +
                "        name: [db]table3.name\n" +
                "    }\n" +
                ")\n");

        this.runtime.compile();
    }

    @Test
    public void testMappingWithJoinSequenceWithRecompile()
    {
        this.runtime.createInMemorySource("testModel.pure", "###Pure\n" +
                "\n" +
                "Class Table1\n" +
                "{\n" +
                "   name: String[1];\n" +
                "   table3: Table3[0..1];\n" +
                "}\n" +
                "\n" +
                "Class Table2\n" +
                "{\n" +
                "   name:String[1];\n" +
                "}\n" +
                "Class Table3\n" +
                "{\n" +
                "   name:String[1];\n" +
                "}\n");

        this.runtime.createInMemorySource("testRelational.pure", "###Relational\n" +
                "Database db\n" +
                "(\n" +
                "   Table table1\n" +
                "   (\n" +
                "       name VARCHAR(200) PRIMARY KEY,\n" +
                "       t2name VARCHAR(200)\n" +
                "   )\n" +
                "   Table table2\n" +
                "   (\n" +
                "       name VARCHAR(200) PRIMARY KEY,\n" +
                "       t3name VARCHAR(200)\n" +
                "   )\n" +
                "   Table table3\n" +
                "   (\n" +
                "       name VARCHAR(200) PRIMARY KEY\n" +
                "   )\n" +
                "   Join T1_T2(table1.t2name = table2.name)" +
                "   Join T2_T3(table2.t3name = table3.name)\n" +
                ")");

        this.runtime.createInMemorySource("testMapping.pure", "###Mapping\n" +
                "Mapping mapping\n" +
                "(\n" +
                "    Table1: Relational\n" +
                "    {\n" +
                "        name: [db]table1.name,\n" +
                "        table3: [db]@T1_T2 > (INNER) [db]@T2_T3\n" +
                "    }\n" +
                "\n" +
                "    Table2: Relational\n" +
                "    {\n" +
                "        name: [db]table2.name\n" +
                "    }\n" +
                "\n" +
                "    Table3: Relational\n" +
                "    {\n" +
                "        name: [db]table3.name\n" +
                "    }\n" +
                ")\n");

        this.runtime.compile();

        this.runtime.delete("testMapping.pure");

        this.runtime.createInMemorySource("testMapping.pure", "###Mapping\n" +
                "Mapping mapping\n" +
                "(\n" +
                "    Table1: Relational\n" +
                "    {\n" +
                "        name: [db]table1.name,\n" +
                "        table3: [db]@T1_T2 > (INNER) [db]@T2_T3\n" +
                "    }\n" +
                "\n" +
                "    Table2: Relational\n" +
                "    {\n" +
                "        name: [db]table2.name\n" +
                "    }\n" +
                "\n" +
                "    Table3: Relational\n" +
                "    {\n" +
                "        name: [db]table3.name\n" +
                "    }\n" +
                ")\n");

        this.runtime.compile();
    }
}
