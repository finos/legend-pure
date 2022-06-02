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

package org.finos.legend.pure.m4.serialization;

import org.finos.legend.pure.m4.ModelRepository;
import org.finos.legend.pure.m4.serialization.binary.BinaryRepositorySerializer;
import org.finos.legend.pure.m4.serialization.grammar.M4Parser;
import org.finos.legend.pure.m4.statelistener.M4StateListener;
import org.finos.legend.pure.m4.statelistener.VoidM4StateListener;
import org.junit.Assert;
import org.junit.Test;

public class TestSerialization
{
    @Test
    public void testSerial()
    {
        ModelRepository repository = new ModelRepository(370);
        M4StateListener listener = new VoidM4StateListener();

        new M4Parser().parse("^package.children[deep].children[Class] PrimitiveType\n" +
                             "{\n" +
                             "}\n" +
                             "\n" +
                             "^PrimitiveType String\n" +
                             "{\n" +
                             "}\n" +
                             "\n" +
                             "^Package package\n" +
                             "{\n" +
                             "    Package.properties[children] :\n" +
                             "        [\n" +
                             "            ^Package deep\n" +
                             "            {\n" +
                             "                  Package.properties[children] :\n" +
                             "                      [\n" +
                             "                          package.children[deep].children[Class]\n" +
                             "                      ]\n" +
                             "            }\n" +
                             "        ]\n" +
                             "}\n" +
                             "^package.children[deep].children[Class] Class ?[a/b/c/file.txt:1,3,1,9,45,89]? @package.children[deep].children\n" +
                             "{\n" +
                             "    Element.properties[name] : 'Class',\n" +
                             "    package.children[deep].children[Class].properties[properties] :\n" +
                             "        [\n" +
                             "            ^Property properties\n" +
                             "                {\n" +
                             "                    Property.properties[type] : Property\n" +
                             "                }\n" +
                             "        ]\n" +
                             "}\n" +
                             "\n" +
                             "^package.children[deep].children[Class] Element ?[e/f/file2.txt:5,13,5,13,12,16]?\n" +
                             "{\n" +
                             "    Element.properties[name] : 'Element\\u2022',\n" +
                             "    package.children[deep].children[Class].properties[properties] :\n" +
                             "        [\n" +
                             "            ^Property name\n" +
                             "                {\n" +
                             "                    Property.properties[type] : String\n" +
                             "                }\n" +
                             "        ]\n" +
                             "}\n" +
                             "\n" +
                             "^package.children[deep].children[Class] Package ?[t/y/file4.txt:1,2,1,2,12,13]?\n" +
                             "{\n" +
                             "    package.children[deep].children[Class].properties[properties] :\n" +
                             "        [\n" +
                             "            ^Property children\n" +
                             "            {\n" +
                             "                Property.properties[type] : package.children[deep].children[Class]\n" +
                             "            }\n" +
                             "        ]\n" +
                             "}\n" +
                             "\n" +
                             "^package.children[deep].children[Class] Property\n" +
                             "{\n" +
                             "    Element.properties[name] : 'Property',\n" +
                             "    package.children[deep].children[Class].properties[properties] :\n" +
                             "        [\n" +
                             "            ^Property type\n" +
                             "                {\n" +
                             "                    Property.properties[type] : package.children[deep].children[Class]\n" +
                             "                }\n" +
                             "        ]\n" +
                             "}\n", repository, new VoidM4StateListener());

        repository.validate(listener);
        byte[] res = repository.serialize();

        ModelRepository newRepository = new ModelRepository();
        BinaryRepositorySerializer.build(res, newRepository);
        newRepository.validate(listener);

        Assert.assertEquals(6, newRepository.getTopLevels().size());
        Assert.assertEquals("PrimitiveType instance Class(a/b/c/file.txt:1,3,1,9,45,89)",newRepository.getTopLevel("PrimitiveType").print("", 10));
        Assert.assertEquals("String instance PrimitiveType",newRepository.getTopLevel("String").print("", 10));
        Assert.assertEquals("package instance Package(t/y/file4.txt:1,2,1,2,12,13)\n" +
                            "    children(Property):\n" +
                            "        deep instance Package(t/y/file4.txt:1,2,1,2,12,13)\n" +
                            "            children(Property):\n" +
                            "                Class(a/b/c/file.txt:1,3,1,9,45,89) instance Class(a/b/c/file.txt:1,3,1,9,45,89)\n" +
                            "                    name(Property):\n" +
                            "                        Class instance String\n" +
                            "                    properties(Property):\n" +
                            "                        properties instance Property\n" +
                            "                            type(Property):\n" +
                            "                                Property instance Class(a/b/c/file.txt:1,3,1,9,45,89)",newRepository.getTopLevel("package").print("", 10));
        Assert.assertEquals("Package(t/y/file4.txt:1,2,1,2,12,13) instance Class(a/b/c/file.txt:1,3,1,9,45,89)\n" +
                            "    properties(Property):\n" +
                            "        children instance Property\n" +
                            "            type(Property):\n" +
                            "                Class(a/b/c/file.txt:1,3,1,9,45,89) instance Class(a/b/c/file.txt:1,3,1,9,45,89)\n" +
                            "                    name(Property):\n" +
                            "                        Class instance String\n" +
                            "                    properties(Property):\n" +
                            "                        properties instance Property\n" +
                            "                            type(Property):\n" +
                            "                                Property instance Class(a/b/c/file.txt:1,3,1,9,45,89)",newRepository.getTopLevel("Package").print("", 10));
        Assert.assertEquals("Property instance Class(a/b/c/file.txt:1,3,1,9,45,89)\n" +
                            "    name(Property):\n" +
                            "        Property instance String\n" +
                            "    properties(Property):\n" +
                            "        type instance Property\n" +
                            "            type(Property):\n" +
                            "                Class(a/b/c/file.txt:1,3,1,9,45,89) instance Class(a/b/c/file.txt:1,3,1,9,45,89)\n" +
                            "                    name(Property):\n" +
                            "                        Class instance String\n" +
                            "                    properties(Property):\n" +
                            "                        properties instance Property\n" +
                            "                            type(Property):\n" +
                            "                                Property instance Class(a/b/c/file.txt:1,3,1,9,45,89)",newRepository.getTopLevel("Property").print("", 10));
        Assert.assertEquals("Element(e/f/file2.txt:5,13,5,13,12,16) instance Class(a/b/c/file.txt:1,3,1,9,45,89)\n" +
                            "    name(Property):\n" +
                            "        Element\u2022 instance String\n" +
                            "    properties(Property):\n" +
                            "        name instance Property\n" +
                            "            type(Property):\n" +
                            "                String instance PrimitiveType",newRepository.getTopLevel("Element").print("", 10));

    }
}
