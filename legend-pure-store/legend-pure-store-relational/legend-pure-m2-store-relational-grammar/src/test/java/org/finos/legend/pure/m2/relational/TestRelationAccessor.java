// Copyright 2023 Goldman Sachs
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

package org.finos.legend.pure.m2.relational;

import org.finos.legend.pure.m3.coreinstance.meta.pure.metamodel.function.ConcreteFunctionDefinition;
import org.finos.legend.pure.m3.coreinstance.meta.pure.metamodel.valuespecification.InstanceValue;
import org.finos.legend.pure.m3.coreinstance.meta.pure.store.RelationStoreAccessorInstance;
import org.finos.legend.pure.m3.coreinstance.meta.relational.metamodel.DatabaseInstance;
import org.finos.legend.pure.m3.coreinstance.meta.relational.metamodel.relation.Table;
import org.finos.legend.pure.m3.serialization.runtime.PureRuntime;
import org.finos.legend.pure.m4.exception.PureCompilationException;
import org.junit.Test;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.fail;

public class TestRelationAccessor extends AbstractPureRelationalTestWithCoreCompiled
{
    @Test
    public void testRelationAccessor()
    {
        String sourceCode =
                "###Pure\n" +
                        "native function meta::pure::functions::relation::filter<T>(rel:meta::pure::metamodel::relation::Relation<T>[1], f:Function<{T[1]->Boolean[1]}>[1]):meta::pure::metamodel::relation::Relation<T>[1];\n" +
                        "function f():meta::pure::metamodel::relation::Relation<Any>[1]" +
                        "{" +
                        "   #>{my::mainDb.PersonTable}#->filter(f|$f.lastName == 'ee');" +
                        "}\n" +
                        "###Relational\n" +
                        "Database my::mainDb\n" +
                        "( \n" +
                        "   Table PersonTable(firstName VARCHAR(200), lastName VARCHAR(200), firmId INTEGER)\n" +
                        ")\n";
        createAndCompileSourceCode(this.runtime, "myFile.pure", sourceCode);
    }

    @Test
    public void testRelationAccessorMultiplicityError()
    {
        String sourceCode =
                "###Pure\n" +
                        "native function meta::pure::functions::relation::filter<T>(rel:meta::pure::metamodel::relation::Relation<T>[1], f:Function<{T[1]->Boolean[1]}>[1]):meta::pure::metamodel::relation::Relation<T>[1];\n" +
                        "function f():meta::pure::metamodel::relation::Relation<Any>[1]" +
                        "{" +
                        "   #>{my::mainDb.PersonTable}#->filter(f|($f.firmId + 1) == 1);" +
                        "}\n" +
                        "###Relational\n" +
                        "Database my::mainDb\n" +
                        "( \n" +
                        "   Table PersonTable(firstName VARCHAR(200), lastName VARCHAR(200), firmId INTEGER)\n" +
                        ")\n";
        try
        {
            createAndCompileSourceCode(this.runtime, "myFile.pure", sourceCode);
            fail();
        }
        catch (Exception e)
        {
            assertPureException(PureCompilationException.class, "Required multiplicity: 1, found: 0..1", "myFile.pure", 3, 109, e);
        }
    }

    @Test
    public void testRelationAccessorMultiplicity()
    {
        String sourceCode =
                "###Pure\n" +
                        "native function meta::pure::functions::relation::filter<T>(rel:meta::pure::metamodel::relation::Relation<T>[1], f:Function<{T[1]->Boolean[1]}>[1]):meta::pure::metamodel::relation::Relation<T>[1];\n" +
                        "function f():meta::pure::metamodel::relation::Relation<Any>[1]" +
                        "{" +
                        "   #>{my::mainDb.PersonTable}#->filter(f|($f.firmId + 1) == 1);" +
                        "}\n" +
                        "###Relational\n" +
                        "Database my::mainDb\n" +
                        "( \n" +
                        "   Table PersonTable(firstName VARCHAR(200), lastName VARCHAR(200), firmId INTEGER NOT NULL)\n" +
                        ")\n";
        createAndCompileSourceCode(this.runtime, "myFile.pure", sourceCode);
    }

    @Test
    public void testRelationAccessorMultiplicityWithChaining()
    {
        String sourceCode =
                "###Pure\n" +
                        "native function meta::pure::functions::relation::filter<T>(rel:meta::pure::metamodel::relation::Relation<T>[1], f:Function<{T[1]->Boolean[1]}>[1]):meta::pure::metamodel::relation::Relation<T>[1];\n" +
                        "native function meta::pure::functions::relation::select<T,Z>(r:meta::pure::metamodel::relation::Relation<T>[1], cols:meta::pure::metamodel::relation::ColSpecArray<Z⊆T>[1]):meta::pure::metamodel::relation::Relation<Z>[1];\n" +
                        "native function meta::pure::functions::relation::select<T,Z>(r:meta::pure::metamodel::relation::Relation<T>[1], cols:meta::pure::metamodel::relation::ColSpec<Z⊆T>[1]):meta::pure::metamodel::relation::Relation<Z>[1];\n" +
                        "function f():meta::pure::metamodel::relation::Relation<Any>[1]" +
                        "{" +
                        "   #>{my::mainDb.PersonTable}#->select(~[firstName, lastName, firmId])->select(~firmId)->filter(f|($f.firmId + 1) == 1);" +
                        "}\n" +
                        "###Relational\n" +
                        "Database my::mainDb\n" +
                        "( \n" +
                        "   Table PersonTable(firstName VARCHAR(200), lastName VARCHAR(200), firmId INTEGER NOT NULL)\n" +
                        ")\n";
        createAndCompileSourceCode(this.runtime, "myFile.pure", sourceCode);
    }


    @Test
    public void testRelationAccessorWithSpace()
    {
        String sourceCode =
                "###Pure\n" +
                        "native function meta::pure::functions::relation::filter<T>(rel:meta::pure::metamodel::relation::Relation<T>[1], f:Function<{T[1]->Boolean[1]}>[1]):meta::pure::metamodel::relation::Relation<T>[1];\n" +
                        "function f():meta::pure::metamodel::relation::Relation<Any>[1]" +
                        "{" +
                        "   #>{my::mainDb.PersonTable}#->filter(f|$f.'first Name' == 'ee');" +
                        "}\n" +
                        "###Relational\n" +
                        "Database my::mainDb\n" +
                        "( \n" +
                        "   Table PersonTable(\"first Name\" VARCHAR(200), lastName VARCHAR(200), firmId INTEGER)\n" +
                        ")\n";
        createAndCompileSourceCode(this.runtime, "myFile.pure", sourceCode);
    }

    @Test
    public void testRelationAccessorWithDBIncludes()
    {
        String sourceCode =
                "###Pure\n" +
                        "native function meta::pure::functions::relation::filter<T>(rel:meta::pure::metamodel::relation::Relation<T>[1], f:Function<{T[1]->Boolean[1]}>[1]):meta::pure::metamodel::relation::Relation<T>[1];\n" +
                        "function f():meta::pure::metamodel::relation::Relation<Any>[1]" +
                        "{" +
                        "   #>{my::mainDb.PersonTable}#->filter(f|$f.lastName == 'ee');" +
                        "}\n" +
                        "###Relational\n" +
                        "Database my::incDb\n" +
                        "( \n" +
                        "   Table PersonTable(firstName VARCHAR(200), lastName VARCHAR(200), firmId INTEGER)\n" +
                        ")\n" +
                        "###Relational\n" +
                        "Database my::mainDb\n" +
                        "( \n" +
                        "   include my::incDb\n" +
                        "   Table FirmTable(legalName VARCHAR(200), firmId INTEGER)\n" +
                        ")\n";
        createAndCompileSourceCode(this.runtime, "myFile.pure", sourceCode);
    }

    @Test
    public void testRelationAccessorDataTypes()
    {
        String sourceCode =
                "###Pure\n" +
                        "native function meta::pure::functions::relation::filter<T>(rel:meta::pure::metamodel::relation::Relation<T>[1], f:Function<{T[1]->Boolean[1]}>[1]):meta::pure::metamodel::relation::Relation<T>[1];\n" +
                        "function f():meta::pure::metamodel::relation::Relation<Any>[1]" +
                        "{" +
                        "   #>{my::mainDb.PersonTable}#->filter(f|$f.firstName == 'ee');" +
                        "}\n" +
                        "###Relational\n" +
                        "Database my::mainDb\n" +
                        "( \n" +
                        "   Table PersonTable(" +
                        "       firstName VARCHAR(200), " +
                        "       firmId INTEGER," +
                        "       salary DECIMAL(10, 2), " +
                        "       height DOUBLE," +
                        "       modifiedTime TIMESTAMP, " +
                        "       birthDate DATE" +
                        "   )\n" +
                        ")\n";
        createAndCompileSourceCode(this.runtime, "myFile.pure", sourceCode);
    }


    @Test
    public void testMultyChainedFunctions()
    {
        String sourceCode =
                "###Pure\n" +
                        "native function meta::pure::functions::relation::filter<T>(rel:meta::pure::metamodel::relation::Relation<T>[1], f:Function<{T[1]->Boolean[1]}>[1]):meta::pure::metamodel::relation::Relation<T>[1];\n" +
                        "native function meta::pure::functions::relation::select<T,Z>(r:meta::pure::metamodel::relation::Relation<T>[1], cols:meta::pure::metamodel::relation::ColSpecArray<Z⊆T>[1]):meta::pure::metamodel::relation::Relation<Z>[1];" +
                        "native function meta::pure::functions::relation::extend<T,Z>(r:meta::pure::metamodel::relation::Relation<T>[1], f:meta::pure::metamodel::relation::FuncColSpec<{T[1]->Any[0..1]},Z>[1]):meta::pure::metamodel::relation::Relation<T+Z>[1];\n" +
                        "native function meta::pure::functions::string::joinStrings(strings:String[*]):String[1];" +
                        "function f():Any[1]" +
                        "{" +
                        "   {|#>{my::mainDb.PersonTable}#->select(~[FIRSTNAME, LASTNAME])->extend(~fullname:x|[$x.FIRSTNAME->meta::pure::functions::multiplicity::toOne(), $x.LASTNAME->meta::pure::functions::multiplicity::toOne()]->joinStrings())->filter(row2|$row2.FIRSTNAME->meta::pure::functions::collection::isNotEmpty()->meta::pure::functions::boolean::not())}" +
                        "}\n" +
                        "###Relational\n" +
                        "Database my::mainDb\n" +
                        "( \n" +
                        "   Table PersonTable(" +
                        "       FIRSTNAME VARCHAR(200), " +
                        "       LASTNAME VARCHAR(200)" +
                        "   )\n" +
                        ")\n";
        createAndCompileSourceCode(this.runtime, "myFile.pure", sourceCode);
    }

    @Test
    public void testRelationAccessorProperties()
    {
        String sourceCode =
                "###Pure\n" +
                        "function f():meta::pure::metamodel::relation::Relation<Any>[1]" +
                        "{" +
                        "   #>{my::mainDb.PersonTable}#" +
                        "}\n" +
                        "###Relational\n" +
                        "Database my::mainDb\n" +
                        "( \n" +
                        "   Table PersonTable(firstName VARCHAR(200))\n" +
                        ")\n";
        createAndCompileSourceCode(this.runtime, "myFile.pure", sourceCode);

        ConcreteFunctionDefinition<?> function = (ConcreteFunctionDefinition<?>) this.runtime.getFunction("f__Relation_1_");
        InstanceValue iv = (InstanceValue) function._expressionSequence().getOnly();
        RelationStoreAccessorInstance accessor = (RelationStoreAccessorInstance) iv._values().getOnly();

        DatabaseInstance database = (DatabaseInstance) this.runtime.getCoreInstance("my::mainDb");
        Table table = database._schemas().getOnly()._tables().getOnly();

        assertEquals(database, accessor._sourceElementContainer());
        assertEquals(table, accessor._sourceElement());
    }

    @Test
    public void testRelationAccessorWithSchema()
    {
        String sourceCode =
                "###Pure\n" +
                        "native function meta::pure::functions::relation::filter<T>(rel:meta::pure::metamodel::relation::Relation<T>[1], f:Function<{T[1]->Boolean[1]}>[1]):meta::pure::metamodel::relation::Relation<T>[1];\n" +
                        "function f():meta::pure::metamodel::relation::Relation<Any>[1]" +
                        "{" +
                        "   #>{my::mainDb.MySchema.PersonTable}#->filter(f|$f.lastName == 'ee');" +
                        "}\n" +
                        "###Relational\n" +
                        "Database my::mainDb\n" +
                        "(\n" +
                        "   Schema MySchema\n" +
                        "   (\n" +
                        "      Table PersonTable(firstName VARCHAR(200), lastName VARCHAR(200), firmId INTEGER)\n" +
                        "   )\n" +
                        ")\n";
        createAndCompileSourceCode(this.runtime, "myFile.pure", sourceCode);
    }

    @Test
    public void testRelationAccessorWithSchemaProperties()
    {
        // Bare accessor (no chaining) so we can read the value directly off the function's expression sequence.
        String sourceCode =
                "###Pure\n" +
                        "function f():meta::pure::metamodel::relation::Relation<Any>[1]" +
                        "{" +
                        "   #>{my::mainDb.MySchema.PersonTable}#" +
                        "}\n" +
                        "###Relational\n" +
                        "Database my::mainDb\n" +
                        "(\n" +
                        "   Schema MySchema\n" +
                        "   (\n" +
                        "      Table PersonTable(firstName VARCHAR(200))\n" +
                        "   )\n" +
                        ")\n";
        createAndCompileSourceCode(this.runtime, "myFile.pure", sourceCode);

        ConcreteFunctionDefinition<?> function = (ConcreteFunctionDefinition<?>) this.runtime.getFunction("f__Relation_1_");
        InstanceValue iv = (InstanceValue) function._expressionSequence().getOnly();
        RelationStoreAccessorInstance accessor = (RelationStoreAccessorInstance) iv._values().getOnly();

        DatabaseInstance database = (DatabaseInstance) this.runtime.getCoreInstance("my::mainDb");
        Table table = database._schemas().detect(s -> "MySchema".equals(s._name()))._tables().getOnly();

        assertEquals(database, accessor._sourceElementContainer());
        assertEquals(table, accessor._sourceElement());
    }

    @Test
    public void testRelationAccessorWithSchemaInIncludedDb()
    {
        // Schema-qualified accessor must traverse `include`d databases, mirroring testRelationAccessorWithDBIncludes.
        String sourceCode =
                "###Pure\n" +
                        "function f():meta::pure::metamodel::relation::Relation<Any>[1]" +
                        "{" +
                        "   #>{my::mainDb.IncSchema.PersonTable}#" +
                        "}\n" +
                        "###Relational\n" +
                        "Database my::incDb\n" +
                        "(\n" +
                        "   Schema IncSchema\n" +
                        "   (\n" +
                        "      Table PersonTable(firstName VARCHAR(200), lastName VARCHAR(200))\n" +
                        "   )\n" +
                        ")\n" +
                        "###Relational\n" +
                        "Database my::mainDb\n" +
                        "(\n" +
                        "   include my::incDb\n" +
                        "   Table FirmTable(legalName VARCHAR(200))\n" +
                        ")\n";
        createAndCompileSourceCode(this.runtime, "myFile.pure", sourceCode);

        ConcreteFunctionDefinition<?> function = (ConcreteFunctionDefinition<?>) this.runtime.getFunction("f__Relation_1_");
        InstanceValue iv = (InstanceValue) function._expressionSequence().getOnly();
        RelationStoreAccessorInstance accessor = (RelationStoreAccessorInstance) iv._values().getOnly();

        DatabaseInstance mainDb = (DatabaseInstance) this.runtime.getCoreInstance("my::mainDb");
        DatabaseInstance incDb = (DatabaseInstance) this.runtime.getCoreInstance("my::incDb");
        Table table = incDb._schemas().detect(s -> "IncSchema".equals(s._name()))._tables().getOnly();

        // The accessor's container is the *user-facing* database the path starts from, not the included one.
        assertEquals(mainDb, accessor._sourceElementContainer());
        assertEquals(table, accessor._sourceElement());
    }

    @Test
    public void testRelationAccessorUnknownSchema()
    {
        String sourceCode =
                "###Pure\n" +
                        "function f():meta::pure::metamodel::relation::Relation<Any>[1]" +
                        "{" +
                        "   #>{my::mainDb.NoSuchSchema.PersonTable}#" +
                        "}\n" +
                        "###Relational\n" +
                        "Database my::mainDb\n" +
                        "(\n" +
                        "   Schema MySchema\n" +
                        "   (\n" +
                        "      Table PersonTable(firstName VARCHAR(200))\n" +
                        "   )\n" +
                        ")\n";
        try
        {
            createAndCompileSourceCode(this.runtime, "myFile.pure", sourceCode);
            fail();
        }
        catch (Exception e)
        {
            assertPureException(PureCompilationException.class, "The schema 'NoSuchSchema' can't be found in the database 'my::mainDb'", e);
        }
    }

    @Test
    public void testRelationAccessorUnknownTableInSchema()
    {
        String sourceCode =
                "###Pure\n" +
                        "function f():meta::pure::metamodel::relation::Relation<Any>[1]" +
                        "{" +
                        "   #>{my::mainDb.MySchema.NoSuchTable}#" +
                        "}\n" +
                        "###Relational\n" +
                        "Database my::mainDb\n" +
                        "(\n" +
                        "   Schema MySchema\n" +
                        "   (\n" +
                        "      Table PersonTable(firstName VARCHAR(200))\n" +
                        "   )\n" +
                        ")\n";
        try
        {
            createAndCompileSourceCode(this.runtime, "myFile.pure", sourceCode);
            fail();
        }
        catch (Exception e)
        {
            assertPureException(PureCompilationException.class, "The table 'NoSuchTable' can't be found in the schema 'MySchema' in the database 'my::mainDb'", e);
        }
    }

    @Test
    public void testRelationAccessorTooManySegments()
    {
        String sourceCode =
                "###Pure\n" +
                        "function f():meta::pure::metamodel::relation::Relation<Any>[1]" +
                        "{" +
                        "   #>{my::mainDb.A.B.C}#" +
                        "}\n" +
                        "###Relational\n" +
                        "Database my::mainDb()\n";
        try
        {
            createAndCompileSourceCode(this.runtime, "myFile.pure", sourceCode);
            fail();
        }
        catch (Exception e)
        {
            assertPureException(PureCompilationException.class, "RelationStoreAccessor path must be of the form 'db.table' or 'db.schema.table' (got 4 segments)", e);
        }
    }

    private static void createAndCompileSourceCode(PureRuntime runtime, String sourceId, String sourceCode)
    {
        runtime.delete(sourceId);
        runtime.createInMemorySource(sourceId, sourceCode);
        runtime.compile();
    }
}
