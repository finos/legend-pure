// Copyright 2024 Goldman Sachs
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

package org.finos.legend.pure.m3.navigation.property;

import org.eclipse.collections.api.tuple.Pair;
import org.eclipse.collections.impl.tuple.Tuples;
import org.finos.legend.pure.m3.navigation.graph.GraphPath;
import org.finos.legend.pure.m3.tests.AbstractPureTestWithCoreCompiled;
import org.finos.legend.pure.m4.coreinstance.CoreInstance;
import org.junit.Assert;
import org.junit.BeforeClass;
import org.junit.Test;

public class TestProperty extends AbstractPureTestWithCoreCompiled
{
    @BeforeClass
    public static void setUp()
    {
        setUpRuntime(getFunctionExecution(), getExtra());
    }

    public static Pair<String, String> getExtra()
    {
        return Tuples.pair(
                "test.pure",
                "import test::*;\n" +
                        "\n" +
                        "Class test::SimpleClass\n" +
                        "{\n" +
                        "  name : String[1];\n" +
                        "  id : Integer[1];\n" +
                        "}\n" +
                        "\n" +
                        "Class test::Left\n" +
                        "{\n" +
                        "}\n" +
                        "\n" +
                        "Class test::Right\n" +
                        "{\n" +
                        "}\n" +
                        "\n" +
                        "Association test::LeftRight\n" +
                        "{\n" +
                        "  toLeft : Left[1];\n" +
                        "  toRight : Right[1];\n" +
                        "}\n" +
                        "\n" +
                        "Profile test::SimpleProfile\n" +
                        "{\n" +
                        "  stereotypes : [st1, st2];\n" +
                        "  tags : [t1, t2, t3];\n" +
                        "}\n" +
                        "\n" +
                        "Enum test::SimpleEnumeration\n" +
                        "{\n" +
                        "  VAL1, VAL2\n" +
                        "}\n" +
                        "\n" +
                        "Class test::BothSides extends Left, Right\n" +
                        "{\n" +
                        "  leftCount : Integer[1];\n" +
                        "  rightCount : Integer[1];\n" +
                        "}\n" +
                        "\n" +
                        "Class <<doc.deprecated>> {doc.doc = 'Deprecated class with annotations'} test::ClassWithAnnotations\n" +
                        "{\n" +
                        "  <<doc.deprecated>> deprecated : String[0..1];\n" +
                        "  <<doc.deprecated>> {doc.doc = 'Deprecated: don\\'t use this'} alsoDeprecated : String[0..1];\n" +
                        "  {doc.doc = 'Time must be specified', doc.todo = 'Change this to DateTime'} date : Date[1];\n" +
                        "}\n" +
                        "\n" +
                        "Class test::ClassWithTypeAndMultParams<T,V|m,n>\n" +
                        "{\n" +
                        "  propT : T[m];\n" +
                        "  propV : V[n];\n" +
                        "}\n" +
                        "\n" +
                        "Class test::ClassWithQualifiedProperties\n" +
                        "{\n" +
                        "  names : String[1..*];\n" +
                        "  title : String[0..1];\n" +
                        "  nameCount()\n" +
                        "  {\n" +
                        "     $this.names->size()\n" +
                        "  }:Integer[1];\n" +
                        "  firstName()\n" +
                        "  {\n" +
                        "     $this.name(0)\n" +
                        "  }:String[1];\n" +
                        "  lastName()\n" +
                        "  {\n" +
                        "     let count = $this.nameCount();\n" +
                        "     if($count == 1, |[], |$this.name($count - 1));\n" +
                        "  }:String[0..1];\n" +
                        "  name(i:Integer[1])\n" +
                        "  {\n" +
                        "     $this.names->at($i)\n" +
                        "  }:String[1];\n" +
                        "  fullName()\n" +
                        "  {\n" +
                        "    $this.fullName(false)\n" +
                        "  }:String[1];\n" +
                        "  fullName(withTitle:Boolean[1])\n" +
                        "  {\n" +
                        "     $this.fullName($withTitle, [])\n" +
                        "  }:String[1];\n" +
                        "  fullName(withTitle:Boolean[1], defaultTitle:String[0..1])\n" +
                        "  {\n" +
                        "     let titleString = if(!$withTitle, |'', |if(!$this.title->isEmpty(), |$this.title->toOne() + ' ', |if(!$defaultTitle->isEmpty(), |$defaultTitle->toOne() + ' ', |'')));\n" +
                        "     $this.names->joinStrings($titleString, ' ', '');\n" +
                        "  }:String[1];\n" +
                        "}\n" +
                        "\n" +
                        "function test::testFunc<T|m>(col:T[m], func:Function<{T[1]->String[1]}>[0..1]):String[m]\n" +
                        "{\n" +
                        "  let toStringFunc = if($func->isEmpty(), |{x:T[1] | $x->toString()}, |$func->toOne());\n" +
                        "  $col->map(x | $toStringFunc->eval($x));\n" +
                        "}\n"
        );
    }

    @Test
    public void testIsProperty()
    {
        Assert.assertFalse(Property.isProperty(null, processorSupport));

        Assert.assertFalse(Property.isProperty(resolveInstance("test::SimpleClass"), processorSupport));
        Assert.assertTrue(Property.isProperty(resolveInstance("test::SimpleClass.properties['name']"), processorSupport));
        Assert.assertTrue(Property.isProperty(resolveInstance("test::SimpleClass.properties['id']"), processorSupport));

        Assert.assertFalse(Property.isProperty(resolveInstance("test::Left"), processorSupport));
        Assert.assertTrue(Property.isProperty(resolveInstance("test::Left.propertiesFromAssociations['toRight']"), processorSupport));
        Assert.assertFalse(Property.isProperty(resolveInstance("test::Right"), processorSupport));
        Assert.assertTrue(Property.isProperty(resolveInstance("test::Right.propertiesFromAssociations['toLeft']"), processorSupport));
        Assert.assertFalse(Property.isProperty(resolveInstance("test::LeftRight"), processorSupport));
        Assert.assertTrue(Property.isProperty(resolveInstance("test::LeftRight.properties['toLeft']"), processorSupport));
        Assert.assertTrue(Property.isProperty(resolveInstance("test::LeftRight.properties['toRight']"), processorSupport));

        Assert.assertFalse(Property.isProperty(resolveInstance("test::SimpleProfile"), processorSupport));
        Assert.assertFalse(Property.isProperty(resolveInstance("test::SimpleProfile.p_stereotypes[value='st1']"), processorSupport));
        Assert.assertFalse(Property.isProperty(resolveInstance("test::SimpleProfile.p_stereotypes[value='st2']"), processorSupport));
        Assert.assertFalse(Property.isProperty(resolveInstance("test::SimpleProfile.p_tags[value='t1']"), processorSupport));
        Assert.assertFalse(Property.isProperty(resolveInstance("test::SimpleProfile.p_tags[value='t2']"), processorSupport));
        Assert.assertFalse(Property.isProperty(resolveInstance("test::SimpleProfile.p_tags[value='t3']"), processorSupport));

        Assert.assertFalse(Property.isProperty(resolveInstance("test::SimpleEnumeration"), processorSupport));
        Assert.assertFalse(Property.isProperty(resolveInstance("test::SimpleEnumeration.values['VAL1']"), processorSupport));
        Assert.assertFalse(Property.isProperty(resolveInstance("test::SimpleEnumeration.values['VAL2']"), processorSupport));

        Assert.assertFalse(Property.isProperty(resolveInstance("test::BothSides"), processorSupport));
        Assert.assertTrue(Property.isProperty(resolveInstance("test::BothSides.properties['leftCount']"), processorSupport));
        Assert.assertTrue(Property.isProperty(resolveInstance("test::BothSides.properties['rightCount']"), processorSupport));

        Assert.assertFalse(Property.isProperty(resolveInstance("test::ClassWithAnnotations"), processorSupport));
        Assert.assertTrue(Property.isProperty(resolveInstance("test::ClassWithAnnotations.properties['deprecated']"), processorSupport));
        Assert.assertFalse(Property.isProperty(resolveInstance("test::ClassWithAnnotations.properties['deprecated'].stereotypes[0]"), processorSupport));
        Assert.assertTrue(Property.isProperty(resolveInstance("test::ClassWithAnnotations.properties['alsoDeprecated']"), processorSupport));
        Assert.assertFalse(Property.isProperty(resolveInstance("test::ClassWithAnnotations.properties['alsoDeprecated'].stereotypes[0]"), processorSupport));
        Assert.assertTrue(Property.isProperty(resolveInstance("test::ClassWithAnnotations.properties['date']"), processorSupport));
        Assert.assertFalse(Property.isProperty(resolveInstance("test::ClassWithAnnotations.properties['date'].taggedValues[0]"), processorSupport));
        Assert.assertFalse(Property.isProperty(resolveInstance("test::ClassWithAnnotations.properties['date'].taggedValues[1]"), processorSupport));

        Assert.assertFalse(Property.isProperty(resolveInstance("test::ClassWithTypeAndMultParams"), processorSupport));
        Assert.assertTrue(Property.isProperty(resolveInstance("test::ClassWithTypeAndMultParams.properties['propT']"), processorSupport));
        Assert.assertTrue(Property.isProperty(resolveInstance("test::ClassWithTypeAndMultParams.properties['propV']"), processorSupport));

        Assert.assertFalse(Property.isProperty(resolveInstance("test::ClassWithQualifiedProperties"), processorSupport));
        Assert.assertTrue(Property.isProperty(resolveInstance("test::ClassWithQualifiedProperties.properties['names']"), processorSupport));
        Assert.assertTrue(Property.isProperty(resolveInstance("test::ClassWithQualifiedProperties.properties['title']"), processorSupport));
        Assert.assertFalse(Property.isProperty(resolveInstance("test::ClassWithQualifiedProperties.qualifiedProperties[id='nameCount()']"), processorSupport));
        Assert.assertTrue(Property.isProperty(resolveInstance("test::ClassWithQualifiedProperties.qualifiedProperties[id='nameCount()'].expressionSequence[0].parametersValues[0].func"), processorSupport));
        Assert.assertFalse(Property.isProperty(resolveInstance("test::ClassWithQualifiedProperties.qualifiedProperties[id='firstName()']"), processorSupport));
        Assert.assertFalse(Property.isProperty(resolveInstance("test::ClassWithQualifiedProperties.qualifiedProperties[id='firstName()'].expressionSequence[0].func"), processorSupport));
        Assert.assertFalse(Property.isProperty(resolveInstance("test::ClassWithQualifiedProperties.qualifiedProperties[id='lastName()']"), processorSupport));
        Assert.assertFalse(Property.isProperty(resolveInstance("test::ClassWithQualifiedProperties.qualifiedProperties[id='name(Integer[1])']"), processorSupport));
        Assert.assertFalse(Property.isProperty(resolveInstance("test::ClassWithQualifiedProperties.qualifiedProperties[id='fullName()']"), processorSupport));
        Assert.assertFalse(Property.isProperty(resolveInstance("test::ClassWithQualifiedProperties.qualifiedProperties[id='fullName(Boolean[1])']"), processorSupport));
        Assert.assertFalse(Property.isProperty(resolveInstance("test::ClassWithQualifiedProperties.qualifiedProperties[id='fullName(Boolean[1],String[0..1])']"), processorSupport));

        Assert.assertFalse(Property.isProperty(resolveInstance("test::testFunc_T_m__Function_$0_1$__String_m_"), processorSupport));
    }

    @Test
    public void testIsQualifiedProperty()
    {
        Assert.assertFalse(Property.isQualifiedProperty(null, processorSupport));

        Assert.assertFalse(Property.isQualifiedProperty(resolveInstance("test::SimpleClass"), processorSupport));
        Assert.assertFalse(Property.isQualifiedProperty(resolveInstance("test::SimpleClass.properties['name']"), processorSupport));
        Assert.assertFalse(Property.isQualifiedProperty(resolveInstance("test::SimpleClass.properties['id']"), processorSupport));

        Assert.assertFalse(Property.isQualifiedProperty(resolveInstance("test::Left"), processorSupport));
        Assert.assertFalse(Property.isQualifiedProperty(resolveInstance("test::Left.propertiesFromAssociations['toRight']"), processorSupport));
        Assert.assertFalse(Property.isQualifiedProperty(resolveInstance("test::Right"), processorSupport));
        Assert.assertFalse(Property.isQualifiedProperty(resolveInstance("test::Right.propertiesFromAssociations['toLeft']"), processorSupport));
        Assert.assertFalse(Property.isQualifiedProperty(resolveInstance("test::LeftRight"), processorSupport));
        Assert.assertFalse(Property.isQualifiedProperty(resolveInstance("test::LeftRight.properties['toLeft']"), processorSupport));
        Assert.assertFalse(Property.isQualifiedProperty(resolveInstance("test::LeftRight.properties['toRight']"), processorSupport));

        Assert.assertFalse(Property.isQualifiedProperty(resolveInstance("test::SimpleProfile"), processorSupport));
        Assert.assertFalse(Property.isQualifiedProperty(resolveInstance("test::SimpleProfile.p_stereotypes[value='st1']"), processorSupport));
        Assert.assertFalse(Property.isQualifiedProperty(resolveInstance("test::SimpleProfile.p_stereotypes[value='st2']"), processorSupport));
        Assert.assertFalse(Property.isQualifiedProperty(resolveInstance("test::SimpleProfile.p_tags[value='t1']"), processorSupport));
        Assert.assertFalse(Property.isQualifiedProperty(resolveInstance("test::SimpleProfile.p_tags[value='t2']"), processorSupport));
        Assert.assertFalse(Property.isQualifiedProperty(resolveInstance("test::SimpleProfile.p_tags[value='t3']"), processorSupport));

        Assert.assertFalse(Property.isQualifiedProperty(resolveInstance("test::SimpleEnumeration"), processorSupport));
        Assert.assertFalse(Property.isQualifiedProperty(resolveInstance("test::SimpleEnumeration.values['VAL1']"), processorSupport));
        Assert.assertFalse(Property.isQualifiedProperty(resolveInstance("test::SimpleEnumeration.values['VAL2']"), processorSupport));

        Assert.assertFalse(Property.isQualifiedProperty(resolveInstance("test::BothSides"), processorSupport));
        Assert.assertFalse(Property.isQualifiedProperty(resolveInstance("test::BothSides.properties['leftCount']"), processorSupport));
        Assert.assertFalse(Property.isQualifiedProperty(resolveInstance("test::BothSides.properties['rightCount']"), processorSupport));

        Assert.assertFalse(Property.isQualifiedProperty(resolveInstance("test::ClassWithAnnotations"), processorSupport));
        Assert.assertFalse(Property.isQualifiedProperty(resolveInstance("test::ClassWithAnnotations.properties['deprecated']"), processorSupport));
        Assert.assertFalse(Property.isQualifiedProperty(resolveInstance("test::ClassWithAnnotations.properties['deprecated'].stereotypes[0]"), processorSupport));
        Assert.assertFalse(Property.isQualifiedProperty(resolveInstance("test::ClassWithAnnotations.properties['alsoDeprecated']"), processorSupport));
        Assert.assertFalse(Property.isQualifiedProperty(resolveInstance("test::ClassWithAnnotations.properties['alsoDeprecated'].stereotypes[0]"), processorSupport));
        Assert.assertFalse(Property.isQualifiedProperty(resolveInstance("test::ClassWithAnnotations.properties['date']"), processorSupport));
        Assert.assertFalse(Property.isQualifiedProperty(resolveInstance("test::ClassWithAnnotations.properties['date'].taggedValues[0]"), processorSupport));
        Assert.assertFalse(Property.isQualifiedProperty(resolveInstance("test::ClassWithAnnotations.properties['date'].taggedValues[1]"), processorSupport));

        Assert.assertFalse(Property.isQualifiedProperty(resolveInstance("test::ClassWithTypeAndMultParams"), processorSupport));
        Assert.assertFalse(Property.isQualifiedProperty(resolveInstance("test::ClassWithTypeAndMultParams.properties['propT']"), processorSupport));
        Assert.assertFalse(Property.isQualifiedProperty(resolveInstance("test::ClassWithTypeAndMultParams.properties['propV']"), processorSupport));

        Assert.assertFalse(Property.isQualifiedProperty(resolveInstance("test::ClassWithQualifiedProperties"), processorSupport));
        Assert.assertFalse(Property.isQualifiedProperty(resolveInstance("test::ClassWithQualifiedProperties.properties['names']"), processorSupport));
        Assert.assertFalse(Property.isQualifiedProperty(resolveInstance("test::ClassWithQualifiedProperties.properties['title']"), processorSupport));
        Assert.assertTrue(Property.isQualifiedProperty(resolveInstance("test::ClassWithQualifiedProperties.qualifiedProperties[id='nameCount()']"), processorSupport));
        Assert.assertFalse(Property.isQualifiedProperty(resolveInstance("test::ClassWithQualifiedProperties.qualifiedProperties[id='nameCount()'].expressionSequence[0].parametersValues[0].func"), processorSupport));
        Assert.assertTrue(Property.isQualifiedProperty(resolveInstance("test::ClassWithQualifiedProperties.qualifiedProperties[id='firstName()']"), processorSupport));
        Assert.assertTrue(Property.isQualifiedProperty(resolveInstance("test::ClassWithQualifiedProperties.qualifiedProperties[id='firstName()'].expressionSequence[0].func"), processorSupport));
        Assert.assertTrue(Property.isQualifiedProperty(resolveInstance("test::ClassWithQualifiedProperties.qualifiedProperties[id='lastName()']"), processorSupport));
        Assert.assertTrue(Property.isQualifiedProperty(resolveInstance("test::ClassWithQualifiedProperties.qualifiedProperties[id='name(Integer[1])']"), processorSupport));
        Assert.assertTrue(Property.isQualifiedProperty(resolveInstance("test::ClassWithQualifiedProperties.qualifiedProperties[id='fullName()']"), processorSupport));
        Assert.assertTrue(Property.isQualifiedProperty(resolveInstance("test::ClassWithQualifiedProperties.qualifiedProperties[id='fullName(Boolean[1])']"), processorSupport));
        Assert.assertTrue(Property.isQualifiedProperty(resolveInstance("test::ClassWithQualifiedProperties.qualifiedProperties[id='fullName(Boolean[1],String[0..1])']"), processorSupport));

        Assert.assertFalse(Property.isQualifiedProperty(resolveInstance("test::testFunc_T_m__Function_$0_1$__String_m_"), processorSupport));
    }

    @Test
    public void testGetPropertyId()
    {
        Assert.assertEquals("name", Property.getPropertyId(resolveInstance("test::SimpleClass.properties['name']"), processorSupport));
        Assert.assertEquals("id", Property.getPropertyId(resolveInstance("test::SimpleClass.properties['id']"), processorSupport));

        Assert.assertEquals("toLeft", Property.getPropertyId(resolveInstance("test::LeftRight.properties['toLeft']"), processorSupport));
        Assert.assertEquals("toRight", Property.getPropertyId(resolveInstance("test::LeftRight.properties['toRight']"), processorSupport));

        Assert.assertEquals("leftCount", Property.getPropertyId(resolveInstance("test::BothSides.properties['leftCount']"), processorSupport));
        Assert.assertEquals("rightCount", Property.getPropertyId(resolveInstance("test::BothSides.properties['rightCount']"), processorSupport));

        Assert.assertEquals("deprecated", Property.getPropertyId(resolveInstance("test::ClassWithAnnotations.properties['deprecated']"), processorSupport));
        Assert.assertEquals("alsoDeprecated", Property.getPropertyId(resolveInstance("test::ClassWithAnnotations.properties['alsoDeprecated']"), processorSupport));
        Assert.assertEquals("date", Property.getPropertyId(resolveInstance("test::ClassWithAnnotations.properties['date']"), processorSupport));

        Assert.assertEquals("propT", Property.getPropertyId(resolveInstance("test::ClassWithTypeAndMultParams.properties['propT']"), processorSupport));
        Assert.assertEquals("propV", Property.getPropertyId(resolveInstance("test::ClassWithTypeAndMultParams.properties['propV']"), processorSupport));

        Assert.assertEquals("names", Property.getPropertyId(resolveInstance("test::ClassWithQualifiedProperties.properties['names']"), processorSupport));
        Assert.assertEquals("title", Property.getPropertyId(resolveInstance("test::ClassWithQualifiedProperties.properties['title']"), processorSupport));
        Assert.assertEquals("nameCount()", Property.getPropertyId(resolveInstance("test::ClassWithQualifiedProperties.qualifiedProperties[id='nameCount()']"), processorSupport));
        Assert.assertEquals("firstName()", Property.getPropertyId(resolveInstance("test::ClassWithQualifiedProperties.qualifiedProperties[id='firstName()']"), processorSupport));
        Assert.assertEquals("lastName()", Property.getPropertyId(resolveInstance("test::ClassWithQualifiedProperties.qualifiedProperties[id='lastName()']"), processorSupport));
        Assert.assertEquals("name(Integer[1])", Property.getPropertyId(resolveInstance("test::ClassWithQualifiedProperties.qualifiedProperties[id='name(Integer[1])']"), processorSupport));
        Assert.assertEquals("fullName()", Property.getPropertyId(resolveInstance("test::ClassWithQualifiedProperties.qualifiedProperties[id='fullName()']"), processorSupport));
        Assert.assertEquals("fullName(Boolean[1])", Property.getPropertyId(resolveInstance("test::ClassWithQualifiedProperties.qualifiedProperties[id='fullName(Boolean[1])']"), processorSupport));
        Assert.assertEquals("fullName(Boolean[1],String[0..1])", Property.getPropertyId(resolveInstance("test::ClassWithQualifiedProperties.qualifiedProperties[id='fullName(Boolean[1],String[0..1])']"), processorSupport));
    }

    @Test
    public void testComputeQualifiedPropertyId()
    {
        Assert.assertEquals("nameCount()", Property.computeQualifiedPropertyId(resolveInstance("test::ClassWithQualifiedProperties.qualifiedProperties[id='nameCount()']"), processorSupport));
        Assert.assertEquals("firstName()", Property.computeQualifiedPropertyId(resolveInstance("test::ClassWithQualifiedProperties.qualifiedProperties[id='firstName()']"), processorSupport));
        Assert.assertEquals("lastName()", Property.computeQualifiedPropertyId(resolveInstance("test::ClassWithQualifiedProperties.qualifiedProperties[id='lastName()']"), processorSupport));
        Assert.assertEquals("name(Integer[1])", Property.computeQualifiedPropertyId(resolveInstance("test::ClassWithQualifiedProperties.qualifiedProperties[id='name(Integer[1])']"), processorSupport));
        Assert.assertEquals("fullName()", Property.computeQualifiedPropertyId(resolveInstance("test::ClassWithQualifiedProperties.qualifiedProperties[id='fullName()']"), processorSupport));
        Assert.assertEquals("fullName(Boolean[1])", Property.computeQualifiedPropertyId(resolveInstance("test::ClassWithQualifiedProperties.qualifiedProperties[id='fullName(Boolean[1])']"), processorSupport));
        Assert.assertEquals("fullName(Boolean[1],String[0..1])", Property.computeQualifiedPropertyId(resolveInstance("test::ClassWithQualifiedProperties.qualifiedProperties[id='fullName(Boolean[1],String[0..1])']"), processorSupport));
    }

    @Test
    public void testGetPropertyName()
    {
        Assert.assertEquals("name", Property.getPropertyName(resolveInstance("test::SimpleClass.properties['name']")));
        Assert.assertEquals("id", Property.getPropertyName(resolveInstance("test::SimpleClass.properties['id']")));

        Assert.assertEquals("toLeft", Property.getPropertyName(resolveInstance("test::LeftRight.properties['toLeft']")));
        Assert.assertEquals("toRight", Property.getPropertyName(resolveInstance("test::LeftRight.properties['toRight']")));

        Assert.assertEquals("leftCount", Property.getPropertyName(resolveInstance("test::BothSides.properties['leftCount']")));
        Assert.assertEquals("rightCount", Property.getPropertyName(resolveInstance("test::BothSides.properties['rightCount']")));

        Assert.assertEquals("deprecated", Property.getPropertyName(resolveInstance("test::ClassWithAnnotations.properties['deprecated']")));
        Assert.assertEquals("alsoDeprecated", Property.getPropertyName(resolveInstance("test::ClassWithAnnotations.properties['alsoDeprecated']")));
        Assert.assertEquals("date", Property.getPropertyName(resolveInstance("test::ClassWithAnnotations.properties['date']")));

        Assert.assertEquals("propT", Property.getPropertyName(resolveInstance("test::ClassWithTypeAndMultParams.properties['propT']")));
        Assert.assertEquals("propV", Property.getPropertyName(resolveInstance("test::ClassWithTypeAndMultParams.properties['propV']")));

        Assert.assertEquals("names", Property.getPropertyName(resolveInstance("test::ClassWithQualifiedProperties.properties['names']")));
        Assert.assertEquals("title", Property.getPropertyName(resolveInstance("test::ClassWithQualifiedProperties.properties['title']")));
        Assert.assertEquals("nameCount", Property.getPropertyName(resolveInstance("test::ClassWithQualifiedProperties.qualifiedProperties[id='nameCount()']")));
        Assert.assertEquals("firstName", Property.getPropertyName(resolveInstance("test::ClassWithQualifiedProperties.qualifiedProperties[id='firstName()']")));
        Assert.assertEquals("lastName", Property.getPropertyName(resolveInstance("test::ClassWithQualifiedProperties.qualifiedProperties[id='lastName()']")));
        Assert.assertEquals("name", Property.getPropertyName(resolveInstance("test::ClassWithQualifiedProperties.qualifiedProperties[id='name(Integer[1])']")));
        Assert.assertEquals("fullName", Property.getPropertyName(resolveInstance("test::ClassWithQualifiedProperties.qualifiedProperties[id='fullName()']")));
        Assert.assertEquals("fullName", Property.getPropertyName(resolveInstance("test::ClassWithQualifiedProperties.qualifiedProperties[id='fullName(Boolean[1])']")));
        Assert.assertEquals("fullName", Property.getPropertyName(resolveInstance("test::ClassWithQualifiedProperties.qualifiedProperties[id='fullName(Boolean[1],String[0..1])']")));
    }

    private static CoreInstance resolveInstance(String path)
    {
        return GraphPath.parse(path).resolve(processorSupport);
    }
}
