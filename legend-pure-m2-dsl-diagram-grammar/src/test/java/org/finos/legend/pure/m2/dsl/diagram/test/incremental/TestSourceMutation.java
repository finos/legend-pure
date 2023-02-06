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

package org.finos.legend.pure.m2.dsl.diagram.test.incremental;

import org.finos.legend.pure.m3.tests.AbstractPureTestWithCoreCompiled;
import org.finos.legend.pure.m3.SourceMutation;
import org.finos.legend.pure.m3.navigation.Instance;
import org.finos.legend.pure.m3.navigation.M3Properties;
import org.finos.legend.pure.m4.coreinstance.CoreInstance;
import org.eclipse.collections.impl.factory.Sets;
import org.eclipse.collections.impl.test.Verify;
import org.junit.After;
import org.junit.Assert;
import org.junit.BeforeClass;
import org.junit.Test;

public class TestSourceMutation extends AbstractPureTestWithCoreCompiled
{
    @BeforeClass
    public static void setUp() {
        setUpRuntime();
    }

    @After
    public void cleanRuntime()
    {
        runtime.delete("testDiagram.pure");
        runtime.delete("testModel.pure");
        runtime.delete("testFile.pure");
    }

    @Test
    public void testTypeViewWithNonExistentType()
    {
        SourceMutation m = compileTestSource("testDiagram.pure",
                "###Diagram\n" +
                        "Diagram test::pure::TestDiagram(width=10.0, height=10.0)\n" +
                        "{\n" +
                        "    TypeView TestClass1(type=test::pure::TestClass1, stereotypesVisible=true, attributesVisible=true,\n" +
                        "                        attributeStereotypesVisible=true, attributeTypesVisible=true,\n" +
                        "                        color=#FFFFCC, lineWidth=1.0,\n" +
                        "                        position=(874.0, 199.46875), width=353.0, height=57.1875)\n" +
                        "}\n");

        Verify.assertSetsEqual(Sets.mutable.with("testDiagram.pure"), m.getModifiedFiles().toSet());
        Verify.assertSize(1, m.getLineRangesToRemoveByFile().get("testDiagram.pure"));
        Assert.assertEquals(4, m.getLineRangesToRemoveByFile().get("testDiagram.pure").get(0).getOne());
        Assert.assertEquals(7, m.getLineRangesToRemoveByFile().get("testDiagram.pure").get(0).getTwo());

        CoreInstance testDiagram = this.runtime.getCoreInstance("test::pure::TestDiagram");
        Assert.assertNotNull(testDiagram);
        Verify.assertEmpty(Instance.getValueForMetaPropertyToManyResolved(testDiagram, M3Properties.typeViews, this.processorSupport));
        Verify.assertEmpty(Instance.getValueForMetaPropertyToManyResolved(testDiagram, M3Properties.associationViews, this.processorSupport));
        Verify.assertEmpty(Instance.getValueForMetaPropertyToManyResolved(testDiagram, M3Properties.propertyViews, this.processorSupport));
        Verify.assertEmpty(Instance.getValueForMetaPropertyToManyResolved(testDiagram, M3Properties.generalizationViews, this.processorSupport));
    }

    @Test
    public void testAssociationViewWithNonExistentAssociation()
    {
        compileTestSource("testModel.pure",
                "Class test::pure::TestClass1 {}\n" +
                        "Class test::pure::TestClass2 {}\n");
        SourceMutation m = compileTestSource("testDiagram.pure",
                "###Diagram\n" +
                        "Diagram test::pure::TestDiagram(width=10.0, height=10.0)\n" +
                        "{\n" +
                        "    TypeView TestClass1(type=test::pure::TestClass1, stereotypesVisible=true, attributesVisible=true,\n" +
                        "                        attributeStereotypesVisible=true, attributeTypesVisible=true,\n" +
                        "                        color=#FFFFCC, lineWidth=1.0,\n" +
                        "                        position=(874.0, 199.46875), width=353.0, height=57.1875)\n" +
                        "    TypeView TestClass2(type=test::pure::TestClass2, stereotypesVisible=true, attributesVisible=true,\n" +
                        "                        attributeStereotypesVisible=true, attributeTypesVisible=true,\n" +
                        "                        color=#FFFFCC, lineWidth=1.0,\n" +
                        "                        position=(75.0, 97.1875), width=113.0, height=57.1875)\n" +
                        "    AssociationView TestAssociation(association=test::pure::TestAssociation, stereotypesVisible=true, nameVisible=false,\n" +
                        "                                    color=#000000, lineWidth=1.0,\n" +
                        "                                    lineStyle=SIMPLE, points=[(132.5, 77.0), (155.2, 77.0)],\n" +
                        "                                    label='TestAssociation',\n" +
                        "                                    source=TestClass1,\n" +
                        "                                    target=TestClass2,\n" +
                        "                                    sourcePropertyPosition=(132.5, 76.2),\n" +
                        "                                    sourceMultiplicityPosition=(132.5, 80.0),\n" +
                        "                                    targetPropertyPosition=(155.2, 76.2),\n" +
                        "                                    targetMultiplicityPosition=(155.2, 80.0))\n" +
                        "}\n");
        Verify.assertSetsEqual(Sets.mutable.with("testDiagram.pure"), m.getModifiedFiles().toSet());
        Verify.assertSize(1, m.getLineRangesToRemoveByFile().get("testDiagram.pure"));
        Assert.assertEquals(12, m.getLineRangesToRemoveByFile().get("testDiagram.pure").get(0).getOne());
        Assert.assertEquals(21, m.getLineRangesToRemoveByFile().get("testDiagram.pure").get(0).getTwo());

        CoreInstance testDiagram = this.runtime.getCoreInstance("test::pure::TestDiagram");
        Assert.assertNotNull(testDiagram);
        Verify.assertSize(2, Instance.getValueForMetaPropertyToManyResolved(testDiagram, M3Properties.typeViews, this.processorSupport));
        Verify.assertEmpty(Instance.getValueForMetaPropertyToManyResolved(testDiagram, M3Properties.associationViews, this.processorSupport));
        Verify.assertEmpty(Instance.getValueForMetaPropertyToManyResolved(testDiagram, M3Properties.propertyViews, this.processorSupport));
        Verify.assertEmpty(Instance.getValueForMetaPropertyToManyResolved(testDiagram, M3Properties.generalizationViews, this.processorSupport));
    }

    @Test
    public void testPropertyViewWithNonExistentProperty()
    {
        compileTestSource("testModel.pure",
                "Class test::pure::TestClass1 {}\n" +
                        "Class test::pure::TestClass2 {}\n");
        SourceMutation m = compileTestSource("testDiagram.pure",
                "###Diagram\n" +
                        "Diagram test::pure::TestDiagram(width=10.0, height=10.0)\n" +
                        "{\n" +
                        "    TypeView TestClass1(type=test::pure::TestClass1, stereotypesVisible=true, attributesVisible=true,\n" +
                        "                        attributeStereotypesVisible=true, attributeTypesVisible=true,\n" +
                        "                        color=#FFFFCC, lineWidth=1.0,\n" +
                        "                        position=(874.0, 199.46875), width=353.0, height=57.1875)\n" +
                        "    TypeView TestClass2(type=test::pure::TestClass2, stereotypesVisible=true, attributesVisible=true,\n" +
                        "                        attributeStereotypesVisible=true, attributeTypesVisible=true,\n" +
                        "                        color=#FFFFCC, lineWidth=1.0,\n" +
                        "                        position=(75.0, 97.1875), width=113.0, height=57.1875)\n" +
                        "    PropertyView TestClass1_testProperty(property=test::pure::TestClass1.testProperty, stereotypesVisible=true, nameVisible=false,\n" +
                        "                                         color=#000000, lineWidth=1.0,\n" +
                        "                                         lineStyle=SIMPLE, points=[(132.5, 77.0), (155.2, 77.0)],\n" +
                        "                                         label='Employment',\n" +
                        "                                         source=TestClass1,\n" +
                        "                                         target=TestClass2,\n" +
                        "                                         propertyPosition=(132.5, 76.2),\n" +
                        "                                         multiplicityPosition=(132.5, 80.0))\n" +
                        "}\n");
        Verify.assertSetsEqual(Sets.mutable.with("testDiagram.pure"), m.getModifiedFiles().toSet());
        Verify.assertSize(1, m.getLineRangesToRemoveByFile().get("testDiagram.pure"));
        Assert.assertEquals(12, m.getLineRangesToRemoveByFile().get("testDiagram.pure").get(0).getOne());
        Assert.assertEquals(19, m.getLineRangesToRemoveByFile().get("testDiagram.pure").get(0).getTwo());

        CoreInstance testDiagram = this.runtime.getCoreInstance("test::pure::TestDiagram");
        Assert.assertNotNull(testDiagram);
        Verify.assertSize(2, Instance.getValueForMetaPropertyToManyResolved(testDiagram, M3Properties.typeViews, this.processorSupport));
        Verify.assertEmpty(Instance.getValueForMetaPropertyToManyResolved(testDiagram, M3Properties.associationViews, this.processorSupport));
        Verify.assertEmpty(Instance.getValueForMetaPropertyToManyResolved(testDiagram, M3Properties.propertyViews, this.processorSupport));
        Verify.assertEmpty(Instance.getValueForMetaPropertyToManyResolved(testDiagram, M3Properties.generalizationViews, this.processorSupport));
    }

    @Test
    public void testGeneralizationViewWithNonExistentTarget()
    {
        compileTestSource("testModel.pure",
                "Class test::pure::TestClass1 {}\n");
        SourceMutation m = compileTestSource("testDiagram.pure",
                "###Diagram\n" +
                        "Diagram test::pure::TestDiagram(width=10.0, height=10.0)\n" +
                        "{\n" +
                        "    TypeView TestClass1(type=test::pure::TestClass1, stereotypesVisible=true, attributesVisible=true,\n" +
                        "                        attributeStereotypesVisible=true, attributeTypesVisible=true,\n" +
                        "                        color=#FFFFCC, lineWidth=1.0,\n" +
                        "                        position=(874.0, 199.46875), width=353.0, height=57.1875)\n" +
                        "    TypeView TestClass2(type=test::pure::TestClass2, stereotypesVisible=true, attributesVisible=true,\n" +
                        "                        attributeStereotypesVisible=true, attributeTypesVisible=true,\n" +
                        "                        color=#FFFFCC, lineWidth=1.0,\n" +
                        "                        position=(75.0, 97.1875), width=113.0, height=57.1875)\n" +
                        "    GeneralizationView TestClass1_TestClass2(color=#000000, lineWidth=1.0,\n" +
                        "                                             lineStyle=SIMPLE, points=[(132.5, 77.0), (155.2, 77.0)],\n" +
                        "                                             label='',\n" +
                        "                                             source=TestClass1,\n" +
                        "                                             target=TestClass2)\n" +
                        "}\n");
        Verify.assertSetsEqual(Sets.mutable.with("testDiagram.pure"), m.getModifiedFiles().toSet());
        Verify.assertSize(2, m.getLineRangesToRemoveByFile().get("testDiagram.pure"));
        Assert.assertEquals(8, m.getLineRangesToRemoveByFile().get("testDiagram.pure").get(0).getOne());
        Assert.assertEquals(11, m.getLineRangesToRemoveByFile().get("testDiagram.pure").get(0).getTwo());
        Assert.assertEquals(12, m.getLineRangesToRemoveByFile().get("testDiagram.pure").get(1).getOne());
        Assert.assertEquals(16, m.getLineRangesToRemoveByFile().get("testDiagram.pure").get(1).getTwo());

        CoreInstance testDiagram = this.runtime.getCoreInstance("test::pure::TestDiagram");
        Assert.assertNotNull(testDiagram);
        Verify.assertSize(1, Instance.getValueForMetaPropertyToManyResolved(testDiagram, M3Properties.typeViews, this.processorSupport));
        Verify.assertEmpty(Instance.getValueForMetaPropertyToManyResolved(testDiagram, M3Properties.associationViews, this.processorSupport));
        Verify.assertEmpty(Instance.getValueForMetaPropertyToManyResolved(testDiagram, M3Properties.propertyViews, this.processorSupport));
        Verify.assertEmpty(Instance.getValueForMetaPropertyToManyResolved(testDiagram, M3Properties.generalizationViews, this.processorSupport));
    }

    @Test
    public void testGeneralizationViewWithNonExistentGeneralization()
    {
        compileTestSource("testModel.pure",
                "Class test::pure::TestClass1 {}\n" +
                        "Class test::pure::TestClass2 {}\n");
        SourceMutation m = compileTestSource("testDiagram.pure",
                "###Diagram\n" +
                        "Diagram test::pure::TestDiagram(width=10.0, height=10.0)\n" +
                        "{\n" +
                        "    TypeView TestClass1(type=test::pure::TestClass1, stereotypesVisible=true, attributesVisible=true,\n" +
                        "                        attributeStereotypesVisible=true, attributeTypesVisible=true,\n" +
                        "                        color=#FFFFCC, lineWidth=1.0,\n" +
                        "                        position=(874.0, 199.46875), width=353.0, height=57.1875)\n" +
                        "    TypeView TestClass2(type=test::pure::TestClass2, stereotypesVisible=true, attributesVisible=true,\n" +
                        "                        attributeStereotypesVisible=true, attributeTypesVisible=true,\n" +
                        "                        color=#FFFFCC, lineWidth=1.0,\n" +
                        "                        position=(75.0, 97.1875), width=113.0, height=57.1875)\n" +
                        "    GeneralizationView TestClass1_TestClass2(color=#000000, lineWidth=1.0,\n" +
                        "                                             lineStyle=SIMPLE, points=[(132.5, 77.0), (155.2, 77.0)],\n" +
                        "                                             label='',\n" +
                        "                                             source=TestClass1,\n" +
                        "                                             target=TestClass2)\n" +
                        "}\n");
        Verify.assertSetsEqual(Sets.mutable.with("testDiagram.pure"), m.getModifiedFiles().toSet());
        Verify.assertSize(1, m.getLineRangesToRemoveByFile().get("testDiagram.pure"));
        Assert.assertEquals(12, m.getLineRangesToRemoveByFile().get("testDiagram.pure").get(0).getOne());
        Assert.assertEquals(16, m.getLineRangesToRemoveByFile().get("testDiagram.pure").get(0).getTwo());

        CoreInstance testDiagram = this.runtime.getCoreInstance("test::pure::TestDiagram");
        Assert.assertNotNull(testDiagram);
        Verify.assertSize(2, Instance.getValueForMetaPropertyToManyResolved(testDiagram, M3Properties.typeViews, this.processorSupport));
        Verify.assertEmpty(Instance.getValueForMetaPropertyToManyResolved(testDiagram, M3Properties.associationViews, this.processorSupport));
        Verify.assertEmpty(Instance.getValueForMetaPropertyToManyResolved(testDiagram, M3Properties.propertyViews, this.processorSupport));
        Verify.assertEmpty(Instance.getValueForMetaPropertyToManyResolved(testDiagram, M3Properties.generalizationViews, this.processorSupport));
    }

    @Test
    public void testAssociationViewWithNonExistentTypeViewId()
    {
        compileTestSource("testModel.pure",
                "Class test::pure::TestClass1 {}\n" +
                        "Class test::pure::TestClass2 {}\n" +
                        "Association test::pure::TestAssociation\n" +
                        "{\n" +
                        "    prop1:test::pure::TestClass1[0..1];\n" +
                        "    prop2:test::pure::TestClass2[1..*];\n" +
                        "}\n");
        SourceMutation m = compileTestSource("testDiagram.pure",
                "###Diagram\n" +
                        "Diagram test::pure::TestDiagram(width=10.0, height=10.0)\n" +
                        "{\n" +
                        "    TypeView TestClass1(type=test::pure::TestClass1, stereotypesVisible=true, attributesVisible=true,\n" +
                        "                        attributeStereotypesVisible=true, attributeTypesVisible=true,\n" +
                        "                        color=#FFFFCC, lineWidth=1.0,\n" +
                        "                        position=(874.0, 199.46875), width=353.0, height=57.1875)\n" +
                        "    TypeView TestClass2(type=test::pure::TestClass2, stereotypesVisible=true, attributesVisible=true,\n" +
                        "                        attributeStereotypesVisible=true, attributeTypesVisible=true,\n" +
                        "                        color=#FFFFCC, lineWidth=1.0,\n" +
                        "                        position=(75.0, 97.1875), width=113.0, height=57.1875)\n" +
                        "    AssociationView TestAssociation(association=test::pure::TestAssociation, stereotypesVisible=true, nameVisible=false,\n" +
                        "                                    color=#000000, lineWidth=1.0,\n" +
                        "                                    lineStyle=SIMPLE, points=[(132.5, 77.0), (155.2, 77.0)],\n" +
                        "                                    label='TestAssociation',\n" +
                        "                                    source=TestClass1,\n" +
                        "                                    target=TestClass3,\n" +
                        "                                    sourcePropertyPosition=(132.5, 76.2),\n" +
                        "                                    sourceMultiplicityPosition=(132.5, 80.0),\n" +
                        "                                    targetPropertyPosition=(155.2, 76.2),\n" +
                        "                                    targetMultiplicityPosition=(155.2, 80.0))\n" +
                        "}\n");
        // TODO consider whether this is the correct behavior
        Verify.assertSetsEqual(Sets.mutable.with("testDiagram.pure"), m.getModifiedFiles().toSet());
        Verify.assertSize(1, m.getLineRangesToRemoveByFile().get("testDiagram.pure"));
        Assert.assertEquals(12, m.getLineRangesToRemoveByFile().get("testDiagram.pure").get(0).getOne());
        Assert.assertEquals(21, m.getLineRangesToRemoveByFile().get("testDiagram.pure").get(0).getTwo());

        CoreInstance testDiagram = this.runtime.getCoreInstance("test::pure::TestDiagram");
        Assert.assertNotNull(testDiagram);
        Verify.assertSize(2, Instance.getValueForMetaPropertyToManyResolved(testDiagram, M3Properties.typeViews, this.processorSupport));
        Verify.assertEmpty(Instance.getValueForMetaPropertyToManyResolved(testDiagram, M3Properties.associationViews, this.processorSupport));
        Verify.assertEmpty(Instance.getValueForMetaPropertyToManyResolved(testDiagram, M3Properties.propertyViews, this.processorSupport));
        Verify.assertEmpty(Instance.getValueForMetaPropertyToManyResolved(testDiagram, M3Properties.generalizationViews, this.processorSupport));
    }

    @Test
    public void testAssociationViewWithSourceViewWithNonExistentTypeInTheSameFile()
    {
        SourceMutation m1 = compileTestSource("testFile.pure",
                "Class test::pure::TestClass1 {}\n" +
                        "Class test::pure::TestClass2 {}\n" +
                        "Association test::pure::TestAssociation\n" +
                        "{\n" +
                        "  toTC1_1 : test::pure::TestClass1[*];\n" +
                        "  toTC2_1 : test::pure::TestClass2[*];\n" +
                        "}\n" +
                        "" +
                        "###Diagram\n" +
                        "Diagram test::pure::TestDiagram(width=10.0, height=10.0)\n" +
                        "{\n" +
                        "    TypeView TestClass1(type=test::pure::TestClass1, stereotypesVisible=true, attributesVisible=true,\n" +
                        "                        attributeStereotypesVisible=true, attributeTypesVisible=true,\n" +
                        "                        color=#FFFFCC, lineWidth=1.0,\n" +
                        "                        position=(874.0, 199.46875), width=353.0, height=57.1875)\n" +
                        "    TypeView TestClass2(type=test::pure::TestClass2, stereotypesVisible=true, attributesVisible=true,\n" +
                        "                        attributeStereotypesVisible=true, attributeTypesVisible=true,\n" +
                        "                        color=#FFFFCC, lineWidth=1.0,\n" +
                        "                        position=(75.0, 97.1875), width=113.0, height=57.1875)\n" +
                        "    AssociationView TestAssociation(association=test::pure::TestAssociation, stereotypesVisible=true, nameVisible=false,\n" +
                        "                                    color=#000000, lineWidth=1.0,\n" +
                        "                                    lineStyle=SIMPLE, points=[(132.5, 77.0), (155.2, 77.0)],\n" +
                        "                                    label='TestAssociation',\n" +
                        "                                    source=TestClass1,\n" +
                        "                                    target=TestClass2,\n" +
                        "                                    sourcePropertyPosition=(132.5, 76.2),\n" +
                        "                                    sourceMultiplicityPosition=(132.5, 80.0),\n" +
                        "                                    targetPropertyPosition=(155.2, 76.2),\n" +
                        "                                    targetMultiplicityPosition=(155.2, 80.0))\n" +
                        "}\n");
        Verify.assertEmpty(m1.getLineRangesToRemoveByFile());
        Verify.assertEmpty(m1.getMarkedForDeletion());
        Verify.assertEmpty(m1.getModifiedFiles());

        this.runtime.modify("testFile.pure",
                "Class test::pure::TestClass1 {}\n" +
                        "Class test::pure::TestClass3 {}\n" +
                        "Association test::pure::TestAssociation\n" +
                        "{\n" +
                        "  toTC1_1 : test::pure::TestClass1[*];\n" +
                        "  toTC2_1 : test::pure::TestClass3[*];\n" +
                        "}\n" +
                        "\n" +
                        "###Diagram\n" +
                        "Diagram test::pure::TestDiagram(width=10.0, height=10.0)\n" +
                        "{\n" +
                        "    TypeView TestClass1(type=test::pure::TestClass1, stereotypesVisible=true, attributesVisible=true,\n" +
                        "                        attributeStereotypesVisible=true, attributeTypesVisible=true,\n" +
                        "                        color=#FFFFCC, lineWidth=1.0,\n" +
                        "                        position=(874.0, 199.46875), width=353.0, height=57.1875)\n" +
                        "    TypeView TestClass2(type=test::pure::TestClass2, stereotypesVisible=true, attributesVisible=true,\n" +
                        "                        attributeStereotypesVisible=true, attributeTypesVisible=true,\n" +
                        "                        color=#FFFFCC, lineWidth=1.0,\n" +
                        "                        position=(75.0, 97.1875), width=113.0, height=57.1875)\n" +
                        "    AssociationView TestAssociation(association=test::pure::TestAssociation, stereotypesVisible=true, nameVisible=false,\n" +
                        "                                    color=#000000, lineWidth=1.0,\n" +
                        "                                    lineStyle=SIMPLE, points=[(132.5, 77.0), (155.2, 77.0)],\n" +
                        "                                    label='TestAssociation',\n" +
                        "                                    source=TestClass1,\n" +
                        "                                    target=TestClass2,\n" +
                        "                                    sourcePropertyPosition=(132.5, 76.2),\n" +
                        "                                    sourceMultiplicityPosition=(132.5, 80.0),\n" +
                        "                                    targetPropertyPosition=(155.2, 76.2),\n" +
                        "                                    targetMultiplicityPosition=(155.2, 80.0))\n" +
                        "}\n");
        SourceMutation m2 = this.runtime.compile();

        Verify.assertSetsEqual(Sets.mutable.with("testFile.pure"), m2.getModifiedFiles().toSet());
        Verify.assertSize(2, m2.getLineRangesToRemoveByFile().get("testFile.pure"));
        Assert.assertEquals(16, m2.getLineRangesToRemoveByFile().get("testFile.pure").get(0).getOne());
        Assert.assertEquals(19, m2.getLineRangesToRemoveByFile().get("testFile.pure").get(0).getTwo());
        Assert.assertEquals(20, m2.getLineRangesToRemoveByFile().get("testFile.pure").get(1).getOne());
        Assert.assertEquals(29, m2.getLineRangesToRemoveByFile().get("testFile.pure").get(1).getTwo());

        CoreInstance testDiagram = this.runtime.getCoreInstance("test::pure::TestDiagram");
        Assert.assertNotNull(testDiagram);
        Verify.assertSize(1, Instance.getValueForMetaPropertyToManyResolved(testDiagram, M3Properties.typeViews, this.processorSupport));
        Verify.assertEmpty(Instance.getValueForMetaPropertyToManyResolved(testDiagram, M3Properties.associationViews, this.processorSupport));
        Verify.assertEmpty(Instance.getValueForMetaPropertyToManyResolved(testDiagram, M3Properties.propertyViews, this.processorSupport));
        Verify.assertEmpty(Instance.getValueForMetaPropertyToManyResolved(testDiagram, M3Properties.generalizationViews, this.processorSupport));
    }
}
