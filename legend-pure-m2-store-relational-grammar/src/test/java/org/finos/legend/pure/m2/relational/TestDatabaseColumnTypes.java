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

package org.finos.legend.pure.m2.relational;

import org.finos.legend.pure.m3.navigation.PrimitiveUtilities;
import org.finos.legend.pure.m4.coreinstance.CoreInstance;
import org.junit.Assert;
import org.junit.Test;

public class TestDatabaseColumnTypes extends AbstractPureRelationalTestWithCoreCompiled
{
    @Test
    public void testDatabaseWithBigIntColumnDef()
    {
        String dataType = "BIGINT";

        String dbDef = String.format("###Relational\n " +
                "Database db\n " +
                "(\n " +
                "   Table myTable\n " +
                "   (\n " +
                "       name %s PRIMARY KEY \n " +
                "   )\n " +
                ")\n ", dataType);
        this.runtime.createInMemorySource("sourceId.pure", dbDef);
        this.runtime.createInMemorySource("userId.pure", "function test():Boolean[1]{assert(1 == db->meta::relational::metamodel::schema('default').tables->size(), |'');}");

        this.runtime.compile();

        CoreInstance db = processorSupport.package_getByUserPath("db");
        Assert.assertEquals(1, db.getValueForMetaPropertyToMany(M2RelationalProperties.schemas).size());
        Assert.assertEquals(1, db.getValueForMetaPropertyToMany(M2RelationalProperties.schemas).getFirst().getValueForMetaPropertyToMany(M2RelationalProperties.tables).size());
        Assert.assertEquals(1, db.getValueForMetaPropertyToMany(M2RelationalProperties.schemas).getFirst().getValueForMetaPropertyToMany(M2RelationalProperties.tables).getFirst().getValueForMetaPropertyToMany(M2RelationalProperties.columns).size());
        Assert.assertEquals(dataType.toUpperCase(), db.getValueForMetaPropertyToMany(M2RelationalProperties.schemas).getFirst().getValueForMetaPropertyToMany(M2RelationalProperties.tables).getFirst().getValueForMetaPropertyToMany(M2RelationalProperties.columns).getFirst().getValueForMetaPropertyToOne("type").getClassifier().getName().toUpperCase());
    }

    @Test
    public void testDatabaseWithTinyIntColumnDef()
    {
        String dataType = "TINYINT";

        String dbDef = String.format("###Relational\n " +
                "Database db\n " +
                "(\n " +
                "   Table myTable\n " +
                "   (\n " +
                "       name %s PRIMARY KEY \n " +
                "   )\n " +
                ")\n ", dataType);
        this.runtime.createInMemorySource("sourceId.pure", dbDef);
        this.runtime.createInMemorySource("userId.pure", "function test():Boolean[1]{assert(1 == db->meta::relational::metamodel::schema('default').tables->size(), |'');}");

        this.runtime.compile();

        CoreInstance db = processorSupport.package_getByUserPath("db");
        Assert.assertEquals(1, db.getValueForMetaPropertyToMany(M2RelationalProperties.schemas).size());
        Assert.assertEquals(1, db.getValueForMetaPropertyToMany(M2RelationalProperties.schemas).getFirst().getValueForMetaPropertyToMany(M2RelationalProperties.tables).size());
        Assert.assertEquals(1, db.getValueForMetaPropertyToMany(M2RelationalProperties.schemas).getFirst().getValueForMetaPropertyToMany(M2RelationalProperties.tables).getFirst().getValueForMetaPropertyToMany(M2RelationalProperties.columns).size());
        Assert.assertEquals(dataType.toUpperCase(), db.getValueForMetaPropertyToMany(M2RelationalProperties.schemas).getFirst().getValueForMetaPropertyToMany(M2RelationalProperties.tables).getFirst().getValueForMetaPropertyToMany(M2RelationalProperties.columns).getFirst().getValueForMetaPropertyToOne("type").getClassifier().getName().toUpperCase());
    }

    @Test
    public void testDatabaseWithSmallIntColumnDef()
    {
        String dataType = "SMALLINT";

        String dbDef = String.format("###Relational\n " +
                "Database db\n " +
                "(\n " +
                "   Table myTable\n " +
                "   (\n " +
                "       name %s PRIMARY KEY \n " +
                "   )\n " +
                ")\n ", dataType);
        this.runtime.createInMemorySource("sourceId.pure", dbDef);
        this.runtime.createInMemorySource("userId.pure", "function test():Boolean[1]{assert(1 == db->meta::relational::metamodel::schema('default').tables->size(), |'');}");

        this.runtime.compile();

        CoreInstance db = processorSupport.package_getByUserPath("db");
        Assert.assertEquals(1, db.getValueForMetaPropertyToMany(M2RelationalProperties.schemas).size());
        Assert.assertEquals(1, db.getValueForMetaPropertyToMany(M2RelationalProperties.schemas).getFirst().getValueForMetaPropertyToMany(M2RelationalProperties.tables).size());
        Assert.assertEquals(1, db.getValueForMetaPropertyToMany(M2RelationalProperties.schemas).getFirst().getValueForMetaPropertyToMany(M2RelationalProperties.tables).getFirst().getValueForMetaPropertyToMany(M2RelationalProperties.columns).size());
        Assert.assertEquals(dataType.toUpperCase(), db.getValueForMetaPropertyToMany(M2RelationalProperties.schemas).getFirst().getValueForMetaPropertyToMany(M2RelationalProperties.tables).getFirst().getValueForMetaPropertyToMany(M2RelationalProperties.columns).getFirst().getValueForMetaPropertyToOne("type").getClassifier().getName().toUpperCase());
    }

    @Test
    public void testDatabaseWithIntegerIntColumnDef()
    {
        String dataType = "INTEGER";

        String dbDef = String.format("###Relational\n " +
                "Database db\n " +
                "(\n " +
                "   Table myTable\n " +
                "   (\n " +
                "       name %s PRIMARY KEY \n " +
                "   )\n " +
                ")\n ", dataType);
        this.runtime.createInMemorySource("sourceId.pure", dbDef);
        this.runtime.createInMemorySource("userId.pure", "function test():Boolean[1]{assert(1 == db->meta::relational::metamodel::schema('default').tables->size(), |'');}");

        this.runtime.compile();

        CoreInstance db = processorSupport.package_getByUserPath("db");
        Assert.assertEquals(1, db.getValueForMetaPropertyToMany(M2RelationalProperties.schemas).size());
        Assert.assertEquals(1, db.getValueForMetaPropertyToMany(M2RelationalProperties.schemas).getFirst().getValueForMetaPropertyToMany(M2RelationalProperties.tables).size());
        Assert.assertEquals(1, db.getValueForMetaPropertyToMany(M2RelationalProperties.schemas).getFirst().getValueForMetaPropertyToMany(M2RelationalProperties.tables).getFirst().getValueForMetaPropertyToMany(M2RelationalProperties.columns).size());
        Assert.assertEquals(dataType.toUpperCase(), db.getValueForMetaPropertyToMany(M2RelationalProperties.schemas).getFirst().getValueForMetaPropertyToMany(M2RelationalProperties.tables).getFirst().getValueForMetaPropertyToMany(M2RelationalProperties.columns).getFirst().getValueForMetaPropertyToOne("type").getClassifier().getName().toUpperCase());
    }

    @Test
    public void testDatabaseWithFloatIntColumnDef()
    {
        String dataType = "FLOAT";

        String dbDef = String.format("###Relational\n " +
                "Database db\n " +
                "(\n " +
                "   Table myTable\n " +
                "   (\n " +
                "       name %s PRIMARY KEY \n " +
                "   )\n " +
                ")\n ", dataType);
        this.runtime.createInMemorySource("sourceId.pure", dbDef);
        this.runtime.createInMemorySource("userId.pure", "function test():Boolean[1]{assert(1 == db->meta::relational::metamodel::schema('default').tables->size(), |'');}");

        this.runtime.compile();

        CoreInstance db = processorSupport.package_getByUserPath("db");
        Assert.assertEquals(1, db.getValueForMetaPropertyToMany(M2RelationalProperties.schemas).size());
        Assert.assertEquals(1, db.getValueForMetaPropertyToMany(M2RelationalProperties.schemas).getFirst().getValueForMetaPropertyToMany(M2RelationalProperties.tables).size());
        Assert.assertEquals(1, db.getValueForMetaPropertyToMany(M2RelationalProperties.schemas).getFirst().getValueForMetaPropertyToMany(M2RelationalProperties.tables).getFirst().getValueForMetaPropertyToMany(M2RelationalProperties.columns).size());
        Assert.assertEquals(dataType.toUpperCase(), db.getValueForMetaPropertyToMany(M2RelationalProperties.schemas).getFirst().getValueForMetaPropertyToMany(M2RelationalProperties.tables).getFirst().getValueForMetaPropertyToMany(M2RelationalProperties.columns).getFirst().getValueForMetaPropertyToOne("type").getClassifier().getName().toUpperCase());
    }

    @Test
    public void testDatabaseWithDoubleIntColumnDef()
    {
        String dataType = "DOUBLE";

        String dbDef = String.format("###Relational\n " +
                "Database db\n " +
                "(\n " +
                "   Table myTable\n " +
                "   (\n " +
                "       name %s PRIMARY KEY \n " +
                "   )\n " +
                ")\n ", dataType);
        this.runtime.createInMemorySource("sourceId.pure", dbDef);
        this.runtime.createInMemorySource("userId.pure", "function test():Boolean[1]{assert(1 == db->meta::relational::metamodel::schema('default').tables->size(), |'');}");

        this.runtime.compile();

        CoreInstance db = processorSupport.package_getByUserPath("db");
        Assert.assertEquals(1, db.getValueForMetaPropertyToMany(M2RelationalProperties.schemas).size());
        Assert.assertEquals(1, db.getValueForMetaPropertyToMany(M2RelationalProperties.schemas).getFirst().getValueForMetaPropertyToMany(M2RelationalProperties.tables).size());
        Assert.assertEquals(1, db.getValueForMetaPropertyToMany(M2RelationalProperties.schemas).getFirst().getValueForMetaPropertyToMany(M2RelationalProperties.tables).getFirst().getValueForMetaPropertyToMany(M2RelationalProperties.columns).size());
        Assert.assertEquals(dataType.toUpperCase(), db.getValueForMetaPropertyToMany(M2RelationalProperties.schemas).getFirst().getValueForMetaPropertyToMany(M2RelationalProperties.tables).getFirst().getValueForMetaPropertyToMany(M2RelationalProperties.columns).getFirst().getValueForMetaPropertyToOne("type").getClassifier().getName().toUpperCase());
    }

    @Test
    public void testDatabaseWithVarcharColumnDef()
    {
        String dataType = "VARCHAR";
        int size = 200;
        String dbDef = String.format("###Relational\n " +
                "Database db\n " +
                "(\n " +
                "   Table myTable\n " +
                "   (\n " +
                "       name %s(%d) PRIMARY KEY \n " +
                "   )\n " +
                ")\n ", dataType, size);
        this.runtime.createInMemorySource("sourceId.pure", dbDef);
        this.runtime.createInMemorySource("userId.pure", "function test():Boolean[1]{assert(1 == db->meta::relational::metamodel::schema('default').tables->size(), |'');}");

        this.runtime.compile();

        CoreInstance db = processorSupport.package_getByUserPath("db");
        Assert.assertEquals(1, db.getValueForMetaPropertyToMany(M2RelationalProperties.schemas).size());
        Assert.assertEquals(1, db.getValueForMetaPropertyToMany(M2RelationalProperties.schemas).getFirst().getValueForMetaPropertyToMany(M2RelationalProperties.tables).size());
        Assert.assertEquals(1, db.getValueForMetaPropertyToMany(M2RelationalProperties.schemas).getFirst().getValueForMetaPropertyToMany(M2RelationalProperties.tables).getFirst().getValueForMetaPropertyToMany(M2RelationalProperties.columns).size());
        Assert.assertEquals(dataType.toUpperCase(), db.getValueForMetaPropertyToMany(M2RelationalProperties.schemas).getFirst().getValueForMetaPropertyToMany(M2RelationalProperties.tables).getFirst().getValueForMetaPropertyToMany(M2RelationalProperties.columns).getFirst().getValueForMetaPropertyToOne("type").getClassifier().getName().toUpperCase());
        Assert.assertEquals(size, PrimitiveUtilities.getIntegerValue(db.getValueForMetaPropertyToMany(M2RelationalProperties.schemas).getFirst().getValueForMetaPropertyToMany(M2RelationalProperties.tables).getFirst().getValueForMetaPropertyToMany(M2RelationalProperties.columns).getFirst().getValueForMetaPropertyToOne("type").getValueForMetaPropertyToMany("size").getFirst()));
    }

    @Test
    public void testDatabaseWithCharColumnDef()
    {
        String dataType = "CHAR";
        int size = 200;
        String dbDef = String.format("###Relational\n " +
                "Database db\n " +
                "(\n " +
                "   Table myTable\n " +
                "   (\n " +
                "       name %s(%d) PRIMARY KEY \n " +
                "   )\n " +
                ")\n ", dataType, size);
        this.runtime.createInMemorySource("sourceId.pure", dbDef);
        this.runtime.createInMemorySource("userId.pure", "function test():Boolean[1]{assert(1 == db->meta::relational::metamodel::schema('default').tables->size(), |'');}");

        this.runtime.compile();

        CoreInstance db = processorSupport.package_getByUserPath("db");
        Assert.assertEquals(1, db.getValueForMetaPropertyToMany(M2RelationalProperties.schemas).size());
        Assert.assertEquals(1, db.getValueForMetaPropertyToMany(M2RelationalProperties.schemas).getFirst().getValueForMetaPropertyToMany(M2RelationalProperties.tables).size());
        Assert.assertEquals(1, db.getValueForMetaPropertyToMany(M2RelationalProperties.schemas).getFirst().getValueForMetaPropertyToMany(M2RelationalProperties.tables).getFirst().getValueForMetaPropertyToMany(M2RelationalProperties.columns).size());
        Assert.assertEquals(dataType.toUpperCase(), db.getValueForMetaPropertyToMany(M2RelationalProperties.schemas).getFirst().getValueForMetaPropertyToMany(M2RelationalProperties.tables).getFirst().getValueForMetaPropertyToMany(M2RelationalProperties.columns).getFirst().getValueForMetaPropertyToOne("type").getClassifier().getName().toUpperCase());
        Assert.assertEquals(size, PrimitiveUtilities.getIntegerValue(db.getValueForMetaPropertyToMany(M2RelationalProperties.schemas).getFirst().getValueForMetaPropertyToMany(M2RelationalProperties.tables).getFirst().getValueForMetaPropertyToMany(M2RelationalProperties.columns).getFirst().getValueForMetaPropertyToOne("type").getValueForMetaPropertyToMany("size").getFirst()));
    }

    @Test
    public void testDatabaseWithVarbinaryColumnDef()
    {
        String dataType = "VARBINARY";
        int size = 200;
        String dbDef = String.format("###Relational\n " +
                "Database db\n " +
                "(\n " +
                "   Table myTable\n " +
                "   (\n " +
                "       name %s(%d) PRIMARY KEY \n " +
                "   )\n " +
                ")\n ", dataType, size);
        this.runtime.createInMemorySource("sourceId.pure", dbDef);
        this.runtime.createInMemorySource("userId.pure", "function test():Boolean[1]{assert(1 == db->meta::relational::metamodel::schema('default').tables->size(), |'');}");

        this.runtime.compile();

        CoreInstance db = processorSupport.package_getByUserPath("db");
        Assert.assertEquals(1, db.getValueForMetaPropertyToMany(M2RelationalProperties.schemas).size());
        Assert.assertEquals(1, db.getValueForMetaPropertyToMany(M2RelationalProperties.schemas).getFirst().getValueForMetaPropertyToMany(M2RelationalProperties.tables).size());
        Assert.assertEquals(1, db.getValueForMetaPropertyToMany(M2RelationalProperties.schemas).getFirst().getValueForMetaPropertyToMany(M2RelationalProperties.tables).getFirst().getValueForMetaPropertyToMany(M2RelationalProperties.columns).size());
        Assert.assertEquals(dataType.toUpperCase(), db.getValueForMetaPropertyToMany(M2RelationalProperties.schemas).getFirst().getValueForMetaPropertyToMany(M2RelationalProperties.tables).getFirst().getValueForMetaPropertyToMany(M2RelationalProperties.columns).getFirst().getValueForMetaPropertyToOne("type").getClassifier().getName().toUpperCase());
        Assert.assertEquals(size, PrimitiveUtilities.getIntegerValue(db.getValueForMetaPropertyToMany(M2RelationalProperties.schemas).getFirst().getValueForMetaPropertyToMany(M2RelationalProperties.tables).getFirst().getValueForMetaPropertyToMany(M2RelationalProperties.columns).getFirst().getValueForMetaPropertyToOne("type").getValueForMetaPropertyToMany("size").getFirst()));
    }

    @Test
    public void testDatabaseWithBinaryColumnDef()
    {
        String dataType = "BINARY";
        int size = 16;
        String dbDef = String.format("###Relational\n " +
                "Database db\n " +
                "(\n " +
                "   Table myTable\n " +
                "   (\n " +
                "       name %s(%d) PRIMARY KEY \n " +
                "   )\n " +
                ")\n ", dataType, size);
        this.runtime.createInMemorySource("sourceId.pure", dbDef);
        this.runtime.createInMemorySource("userId.pure", "function test():Boolean[1]{assert(1 == db->meta::relational::metamodel::schema('default').tables->size(), |'');}");

        this.runtime.compile();

        CoreInstance db = processorSupport.package_getByUserPath("db");
        Assert.assertEquals(1, db.getValueForMetaPropertyToMany(M2RelationalProperties.schemas).size());
        Assert.assertEquals(1, db.getValueForMetaPropertyToMany(M2RelationalProperties.schemas).getFirst().getValueForMetaPropertyToMany(M2RelationalProperties.tables).size());
        Assert.assertEquals(1, db.getValueForMetaPropertyToMany(M2RelationalProperties.schemas).getFirst().getValueForMetaPropertyToMany(M2RelationalProperties.tables).getFirst().getValueForMetaPropertyToMany(M2RelationalProperties.columns).size());
        Assert.assertEquals(dataType.toUpperCase(), db.getValueForMetaPropertyToMany(M2RelationalProperties.schemas).getFirst().getValueForMetaPropertyToMany(M2RelationalProperties.tables).getFirst().getValueForMetaPropertyToMany(M2RelationalProperties.columns).getFirst().getValueForMetaPropertyToOne("type").getClassifier().getName().toUpperCase());
        Assert.assertEquals(size, PrimitiveUtilities.getIntegerValue(db.getValueForMetaPropertyToMany(M2RelationalProperties.schemas).getFirst().getValueForMetaPropertyToMany(M2RelationalProperties.tables).getFirst().getValueForMetaPropertyToMany(M2RelationalProperties.columns).getFirst().getValueForMetaPropertyToOne("type").getValueForMetaPropertyToMany("size").getFirst()));
    }

    @Test
    public void testDatabaseWithDecimalColumnDef()
    {
        String dataType = "DECIMAL";
        int precision = 200;
        int scale = 5;
        String dbDef = String.format("###Relational\n " +
                "Database db\n " +
                "(\n " +
                "   Table myTable\n " +
                "   (\n " +
                "       name %s(%d, %d) PRIMARY KEY \n " +
                "   )\n " +
                ")\n ", dataType, precision, scale);
        this.runtime.createInMemorySource("sourceId.pure", dbDef);
        this.runtime.createInMemorySource("userId.pure", "function test():Boolean[1]{assert(1 == db->meta::relational::metamodel::schema('default').tables->size(), |'');}");

        this.runtime.compile();

        CoreInstance db = processorSupport.package_getByUserPath("db");
        Assert.assertEquals(1, db.getValueForMetaPropertyToMany(M2RelationalProperties.schemas).size());
        Assert.assertEquals(1, db.getValueForMetaPropertyToMany(M2RelationalProperties.schemas).getFirst().getValueForMetaPropertyToMany(M2RelationalProperties.tables).size());
        Assert.assertEquals(1, db.getValueForMetaPropertyToMany(M2RelationalProperties.schemas).getFirst().getValueForMetaPropertyToMany(M2RelationalProperties.tables).getFirst().getValueForMetaPropertyToMany(M2RelationalProperties.columns).size());
        Assert.assertEquals(dataType.toUpperCase(), db.getValueForMetaPropertyToMany(M2RelationalProperties.schemas).getFirst().getValueForMetaPropertyToMany(M2RelationalProperties.tables).getFirst().getValueForMetaPropertyToMany(M2RelationalProperties.columns).getFirst().getValueForMetaPropertyToOne("type").getClassifier().getName().toUpperCase());
        Assert.assertEquals(precision, PrimitiveUtilities.getIntegerValue(db.getValueForMetaPropertyToMany(M2RelationalProperties.schemas).getFirst().getValueForMetaPropertyToMany(M2RelationalProperties.tables).getFirst().getValueForMetaPropertyToMany(M2RelationalProperties.columns).getFirst().getValueForMetaPropertyToOne("type").getValueForMetaPropertyToMany("precision").getFirst()));
        Assert.assertEquals(scale, PrimitiveUtilities.getIntegerValue(db.getValueForMetaPropertyToMany(M2RelationalProperties.schemas).getFirst().getValueForMetaPropertyToMany(M2RelationalProperties.tables).getFirst().getValueForMetaPropertyToMany(M2RelationalProperties.columns).getFirst().getValueForMetaPropertyToOne("type").getValueForMetaPropertyToMany("scale").getFirst()));
    }

    @Test
    public void testDatabaseWithNumericColumnDef()
    {
        String dataType = "NUMERIC";
        int precision = 200;
        int scale = 5;
        String dbDef = String.format("###Relational\n " +
                "Database db\n " +
                "(\n " +
                "   Table myTable\n " +
                "   (\n " +
                "       name %s(%d, %d) PRIMARY KEY \n " +
                "   )\n " +
                ")\n ", dataType, precision, scale);
        this.runtime.createInMemorySource("sourceId.pure", dbDef);
        this.runtime.createInMemorySource("userId.pure", "function test():Boolean[1]{assert(1 == db->meta::relational::metamodel::schema('default').tables->size(), |'');}");

        this.runtime.compile();

        CoreInstance db = processorSupport.package_getByUserPath("db");
        Assert.assertEquals(1, db.getValueForMetaPropertyToMany(M2RelationalProperties.schemas).size());
        Assert.assertEquals(1, db.getValueForMetaPropertyToMany(M2RelationalProperties.schemas).getFirst().getValueForMetaPropertyToMany(M2RelationalProperties.tables).size());
        Assert.assertEquals(1, db.getValueForMetaPropertyToMany(M2RelationalProperties.schemas).getFirst().getValueForMetaPropertyToMany(M2RelationalProperties.tables).getFirst().getValueForMetaPropertyToMany(M2RelationalProperties.columns).size());
        Assert.assertEquals(dataType.toUpperCase(), db.getValueForMetaPropertyToMany(M2RelationalProperties.schemas).getFirst().getValueForMetaPropertyToMany(M2RelationalProperties.tables).getFirst().getValueForMetaPropertyToMany(M2RelationalProperties.columns).getFirst().getValueForMetaPropertyToOne("type").getClassifier().getName().toUpperCase());
        Assert.assertEquals(precision, PrimitiveUtilities.getIntegerValue(db.getValueForMetaPropertyToMany(M2RelationalProperties.schemas).getFirst().getValueForMetaPropertyToMany(M2RelationalProperties.tables).getFirst().getValueForMetaPropertyToMany(M2RelationalProperties.columns).getFirst().getValueForMetaPropertyToOne("type").getValueForMetaPropertyToMany("precision").getFirst()));
        Assert.assertEquals(scale, PrimitiveUtilities.getIntegerValue(db.getValueForMetaPropertyToMany(M2RelationalProperties.schemas).getFirst().getValueForMetaPropertyToMany(M2RelationalProperties.tables).getFirst().getValueForMetaPropertyToMany(M2RelationalProperties.columns).getFirst().getValueForMetaPropertyToOne("type").getValueForMetaPropertyToMany("scale").getFirst()));
    }

}
