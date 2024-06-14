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
import org.junit.Test;

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

    private static void createAndCompileSourceCode(PureRuntime runtime, String sourceId, String sourceCode)
    {
        runtime.delete(sourceId);
        runtime.createInMemorySource(sourceId, sourceCode);
        runtime.compile();
    }
}
