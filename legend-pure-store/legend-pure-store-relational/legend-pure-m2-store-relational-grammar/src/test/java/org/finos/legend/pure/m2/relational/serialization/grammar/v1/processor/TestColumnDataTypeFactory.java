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

package org.finos.legend.pure.m2.relational.serialization.grammar.v1.processor;

import org.finos.legend.pure.m2.relational.serialization.grammar.v1.processor.ColumnDataTypeFactory.ColumnDataTypeException;
import org.junit.Assert;
import org.junit.Test;

import java.util.Locale;

public class TestColumnDataTypeFactory
{
    @Test
    public void testConstructorString() throws ColumnDataTypeException
    {
        Assert.assertEquals("^meta::relational::metamodel::datatype::Varchar(size=200)", ColumnDataTypeFactory.pureDataTypeConstructorString("VARCHAR", "200"));
        Assert.assertEquals("^meta::relational::metamodel::datatype::Decimal(precision=10,scale=2)", ColumnDataTypeFactory.pureDataTypeConstructorString("decimal", "10", "2"));
    }

    @Test
    public void testWrongNumberOfValues()
    {
        ColumnDataTypeException e = Assert.assertThrows(ColumnDataTypeException.class, () -> ColumnDataTypeFactory.pureDataTypeConstructorString("VARCHAR"));
        Assert.assertEquals("The data type VARCHAR requires 1 values but 0 were provided", e.getMessage());
    }

    @Test
    public void testWrongNumberOfValuesWithArabicDefaultLocale()
    {
        // With a default locale such as ar-EG, String.format writes Arabic-Indic digits
        Locale defaultLocale = Locale.getDefault();
        Locale displayLocale = Locale.getDefault(Locale.Category.DISPLAY);
        Locale formatLocale = Locale.getDefault(Locale.Category.FORMAT);
        Locale.setDefault(Locale.forLanguageTag("ar-EG"));
        ColumnDataTypeException e;
        try
        {
            e = Assert.assertThrows(ColumnDataTypeException.class, () -> ColumnDataTypeFactory.pureDataTypeConstructorString("DECIMAL", "10"));
        }
        finally
        {
            Locale.setDefault(defaultLocale);
            Locale.setDefault(Locale.Category.DISPLAY, displayLocale);
            Locale.setDefault(Locale.Category.FORMAT, formatLocale);
        }
        Assert.assertEquals("The data type DECIMAL requires 2 values but 1 were provided", e.getMessage());
    }
}
