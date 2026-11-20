// Copyright 2026 Goldman Sachs
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

import org.eclipse.collections.api.factory.Lists;
import org.finos.legend.pure.m3.coreinstance.meta.relational.metamodel.Database;
import org.finos.legend.pure.m3.coreinstance.meta.relational.metamodel.relation.Table;
import org.junit.Assert;
import org.junit.Test;

import java.util.Locale;

/**
 * In Turkish, "i".toUpperCase() is a dotted capital I (U+0130), so the names that the relational grammar compares in
 * upper case must not depend on the default locale.
 */
public class TestRelationalWithTurkishDefaultLocale extends AbstractPureRelationalTestWithCoreCompiled
{
    @Test
    public void testLowerCaseColumnTypes()
    {
        compileWithTurkishDefaultLocale("###Relational\n" +
                "Database test::localeDb\n" +
                "(\n" +
                "   Table myTable(id integer PRIMARY KEY, bigId bigint, flag bit, amount decimal(10, 2), created timestamp)\n" +
                ")\n");
        Table table = ((Database) this.runtime.getCoreInstance("test::localeDb"))._schemas().getOnly()._tables().getOnly();
        Assert.assertEquals(
                Lists.mutable.with("Integer", "BigInt", "Bit", "Decimal", "Timestamp"),
                table._columns().collect(c -> c.getValueForMetaPropertyToOne("type").getClassifier().getName(), Lists.mutable.empty()));
    }

    @Test
    public void testRelationAccessorColumnTypes()
    {
        compileWithTurkishDefaultLocale("###Relational\n" +
                "Database test::localeDb\n" +
                "(\n" +
                "   Table myTable(id INTEGER PRIMARY KEY, bigId BIGINT, flag BIT, amount DECIMAL(10, 2), created TIMESTAMP)\n" +
                ")\n" +
                "###Pure\n" +
                "function test::localeRelation():meta::pure::metamodel::relation::Relation<Any>[1]\n" +
                "{\n" +
                "   #>{test::localeDb.myTable}#\n" +
                "}\n");
        Assert.assertNotNull(this.runtime.getFunction("test::localeRelation__Relation_1_"));
    }

    @Test
    public void testLowerCaseJoinType()
    {
        compileWithTurkishDefaultLocale("###Relational\n" +
                "Database test::localeDb\n" +
                "(\n" +
                "   Table table1(id INTEGER PRIMARY KEY, t2Id INTEGER)\n" +
                "   Table table2(id INTEGER PRIMARY KEY, t3Id INTEGER)\n" +
                "   Table table3(id INTEGER PRIMARY KEY, name VARCHAR(200))\n" +
                "   Join T1_T2(table1.t2Id = table2.id)\n" +
                "   Join T2_T3(table2.t3Id = table3.id)\n" +
                "   View myView\n" +
                "   (\n" +
                "      id : table1.id,\n" +
                "      name : @T1_T2 > (inner) @T2_T3 | table3.name\n" +
                "   )\n" +
                ")\n");
        Assert.assertNotNull(this.runtime.getCoreInstance("test::localeDb"));
    }

    private void compileWithTurkishDefaultLocale(String code)
    {
        Locale defaultLocale = Locale.getDefault();
        Locale displayLocale = Locale.getDefault(Locale.Category.DISPLAY);
        Locale formatLocale = Locale.getDefault(Locale.Category.FORMAT);
        Locale.setDefault(Locale.forLanguageTag("tr-TR"));
        try
        {
            this.runtime.createInMemorySource("locale.pure", code);
            this.runtime.compile();
        }
        finally
        {
            Locale.setDefault(defaultLocale);
            Locale.setDefault(Locale.Category.DISPLAY, displayLocale);
            Locale.setDefault(Locale.Category.FORMAT, formatLocale);
        }
    }
}
