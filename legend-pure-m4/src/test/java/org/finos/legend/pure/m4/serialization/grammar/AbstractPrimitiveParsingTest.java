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

package org.finos.legend.pure.m4.serialization.grammar;

import org.eclipse.collections.api.list.MutableList;
import org.finos.legend.pure.m4.coreinstance.CoreInstance;
import org.finos.legend.pure.m4.ModelRepository;
import org.finos.legend.pure.m4.serialization.grammar.M4Parser;
import org.finos.legend.pure.m4.statelistener.M4StateListener;
import org.finos.legend.pure.m4.statelistener.VoidM4StateListener;
import org.junit.Assert;

abstract class AbstractPrimitiveParsingTest
{
    abstract protected String getPrimitiveTypeName();

    protected void assertParsesTo(String expectedName, String string)
    {
        CoreInstance value = parsePrimitiveValue(string);
        Assert.assertNotNull(value);
        Assert.assertNotNull(value.getClassifier());
        Assert.assertEquals(getPrimitiveTypeName(), value.getClassifier().getName());
        Assert.assertEquals(expectedName, value.getName());
    }

    protected void assertFailsToParse(String dateString)
    {
        assertFailsToParse(null, null, dateString);
    }

    protected void assertFailsToParse(String expectedMessage, String dateString)
    {
        assertFailsToParse(expectedMessage, null, dateString);
    }

    protected void assertFailsToParse(Class<? extends Throwable> expectedExceptionClass, String dateString)
    {
        assertFailsToParse(null, expectedExceptionClass, dateString);
    }

    protected void assertFailsToParse(String expectedMessage, Class<? extends Throwable> expectedExceptionClass, String string)
    {
        boolean exception = true;
        try
        {
            parsePrimitiveValue(string);
            exception = false;
        }
        catch (Throwable t)
        {
            if (expectedExceptionClass != null)
            {
                if (!expectedExceptionClass.isInstance(t))
                {
                    Assert.fail("Expected an exception of class: " + expectedExceptionClass.getSimpleName() + ", got: " + t.getClass().getSimpleName());
                }
            }
            if (expectedMessage != null)
            {
                Assert.assertEquals(expectedMessage, t.getMessage());
            }
        }
        if (!exception)
        {
            Assert.fail("Expected exception parsing: \"" + string + "\"");
        }
    }

    private CoreInstance parsePrimitiveValue(String string)
    {
        ModelRepository repository = new ModelRepository();
        M4StateListener listener = new VoidM4StateListener();
        MutableList<CoreInstance> instances = new M4Parser().parse("\n" +
                "^package.children[Class] Class\n" +
                "{\n" +
                "    package.children[Class].properties[properties] :\n" +
                "    [\n" +
                "        ^Property properties\n" +
                "        {\n" +
                "            Property.properties[type] : Property\n" +
                "        },\n" +
                "        ^Property value\n" +
                "        {\n" +
                "            Property.value[type] : " + getPrimitiveTypeName() + "\n" +
                "        }\n" +
                "    ]\n" +
                "}\n" +
                "^Class TestClass\n" +
                "{\n" +
                "    Class.properties[value] : " + string + "\n" +
                "}\n", repository, listener);
        Assert.assertEquals(2, instances.size());
        CoreInstance testClass = instances.get(1);
        return testClass.getValueForMetaPropertyToOne("value");
    }
}
