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
import org.finos.legend.pure.m4.ModelRepository;
import org.finos.legend.pure.m4.coreinstance.CoreInstance;
import org.finos.legend.pure.m4.exception.PureException;
import org.finos.legend.pure.m4.statelistener.M4StateListener;
import org.finos.legend.pure.m4.statelistener.VoidM4StateListener;
import org.junit.Assert;

abstract class AbstractPrimitiveParsingTest
{
    protected abstract String getPrimitiveTypeName();

    protected void assertParsesTo(String expectedName, String string)
    {
        CoreInstance value = parsePrimitiveValue(string);
        Assert.assertNotNull(string, value);
        Assert.assertNotNull(string, value.getClassifier());
        Assert.assertEquals(string, getPrimitiveTypeName(), value.getClassifier().getName());
        Assert.assertEquals(string, expectedName, value.getName());
    }

    protected void assertFailsToParse(String string)
    {
        assertFailsToParse(null, null, string);
    }

    protected void assertFailsToParse(String expectedMessage, String string)
    {
        assertFailsToParse(expectedMessage, null, string);
    }

    protected void assertFailsToParse(Class<? extends PureException> expectedExceptionClass, String dateString)
    {
        assertFailsToParse(null, expectedExceptionClass, dateString);
    }

    protected void assertFailsToParse(String expectedMessage, Class<? extends PureException> expectedExceptionClass, String string)
    {
        PureException e = (expectedExceptionClass == null) ?
                      Assert.assertThrows(PureException.class, () -> parsePrimitiveValue(string)) :
                      Assert.assertThrows(expectedExceptionClass, () -> parsePrimitiveValue(string));
//        e.printStackTrace();
        if (expectedMessage != null)
        {
            Assert.assertEquals(expectedMessage, e.getMessage());
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
