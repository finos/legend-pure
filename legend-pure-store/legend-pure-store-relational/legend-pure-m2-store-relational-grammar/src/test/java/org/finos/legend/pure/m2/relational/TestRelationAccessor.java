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

import org.finos.legend.pure.m3.serialization.runtime.PureRuntime;
import org.finos.legend.pure.m4.exception.PureCompilationException;
import org.junit.Test;

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

    private static void createAndCompileSourceCode(PureRuntime runtime, String sourceId, String sourceCode)
    {
        runtime.delete(sourceId);
        runtime.createInMemorySource(sourceId, sourceCode);
        runtime.compile();
    }
}
