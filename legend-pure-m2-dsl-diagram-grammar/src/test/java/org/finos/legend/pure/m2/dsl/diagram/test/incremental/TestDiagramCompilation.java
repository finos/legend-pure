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
import org.finos.legend.pure.m3.compiler.postprocessing.processor.milestoning.MilestoningFunctions;
import org.finos.legend.pure.m3.navigation.Instance;
import org.finos.legend.pure.m3.navigation.M3Properties;
import org.finos.legend.pure.m4.coreinstance.CoreInstance;
import org.finos.legend.pure.m4.exception.PureCompilationException;
import org.eclipse.collections.api.list.ListIterable;
import org.eclipse.collections.api.map.ImmutableMap;
import org.eclipse.collections.api.map.MutableMap;
import org.eclipse.collections.impl.factory.Maps;
import org.eclipse.collections.impl.list.mutable.FastList;
import org.eclipse.collections.impl.test.Verify;
import org.junit.After;
import org.junit.Assert;
import org.junit.BeforeClass;
import org.junit.Test;

public class TestDiagramCompilation extends AbstractPureTestWithCoreCompiled
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
    }

    @Test
    public void testTypeViewWithNonExistentType()
    {
        compileTestSource("testDiagram.pure",
                "###Diagram\n" +
                        "Diagram test::pure::TestDiagram(width=10.0, height=10.0)\n" +
                        "{\n" +
                        "    TypeView TestClass1(type=test::pure::TestClass1, stereotypesVisible=true, attributesVisible=true,\n" +
                        "                        attributeStereotypesVisible=true, attributeTypesVisible=true,\n" +
                        "                        color=#FFFFCC, lineWidth=1.0,\n" +
                        "                        position=(874.0, 199.46875), width=353.0, height=57.1875)\n" +
                        "}\n");
        CoreInstance diagram = this.runtime.getCoreInstance("test::pure::TestDiagram");
        Assert.assertNotNull(diagram);
        Verify.assertEmpty(Instance.getValueForMetaPropertyToManyResolved(diagram, M3Properties.typeViews, this.processorSupport));
        Verify.assertEmpty(Instance.getValueForMetaPropertyToManyResolved(diagram, M3Properties.associationViews, this.processorSupport));
        Verify.assertEmpty(Instance.getValueForMetaPropertyToManyResolved(diagram, M3Properties.propertyViews, this.processorSupport));
        Verify.assertEmpty(Instance.getValueForMetaPropertyToManyResolved(diagram, M3Properties.generalizationViews, this.processorSupport));
    }

    @Test
    public void testAssociationViewWithNonExistentAssociation()
    {
        compileTestSource("testModel.pure",
                "Class test::pure::TestClass1 {}\n" +
                        "Class test::pure::TestClass2 {}\n");
        compileTestSource("testDiagram.pure",
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
        CoreInstance diagram = this.runtime.getCoreInstance("test::pure::TestDiagram");
        Assert.assertNotNull(diagram);
        Verify.assertSize(2, Instance.getValueForMetaPropertyToManyResolved(diagram, M3Properties.typeViews, this.processorSupport));
        Verify.assertEmpty(Instance.getValueForMetaPropertyToManyResolved(diagram, M3Properties.associationViews, this.processorSupport));
        Verify.assertEmpty(Instance.getValueForMetaPropertyToManyResolved(diagram, M3Properties.propertyViews, this.processorSupport));
        Verify.assertEmpty(Instance.getValueForMetaPropertyToManyResolved(diagram, M3Properties.generalizationViews, this.processorSupport));
    }

    @Test
    public void testPropertyViewWithNonExistentProperty()
    {
        compileTestSource("testModel.pure",
                "Class test::pure::TestClass1 {}\n" +
                        "Class test::pure::TestClass2 {}\n");
        compileTestSource("testDiagram.pure",
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
        CoreInstance diagram = this.runtime.getCoreInstance("test::pure::TestDiagram");
        Assert.assertNotNull(diagram);
        Verify.assertSize(2, Instance.getValueForMetaPropertyToManyResolved(diagram, M3Properties.typeViews, this.processorSupport));
        Verify.assertEmpty(Instance.getValueForMetaPropertyToManyResolved(diagram, M3Properties.associationViews, this.processorSupport));
        Verify.assertEmpty(Instance.getValueForMetaPropertyToManyResolved(diagram, M3Properties.propertyViews, this.processorSupport));
        Verify.assertEmpty(Instance.getValueForMetaPropertyToManyResolved(diagram, M3Properties.generalizationViews, this.processorSupport));
    }

    @Test
    public void testGeneralizationViewWithNonExistentTarget()
    {
        compileTestSource("testModel.pure",
                "Class test::pure::TestClass1 {}\n");
        compileTestSource("testDiagram.pure",
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
        CoreInstance diagram = this.runtime.getCoreInstance("test::pure::TestDiagram");
        Assert.assertNotNull(diagram);
        Verify.assertSize(1, Instance.getValueForMetaPropertyToManyResolved(diagram, M3Properties.typeViews, this.processorSupport));
        Verify.assertEmpty(Instance.getValueForMetaPropertyToManyResolved(diagram, M3Properties.associationViews, this.processorSupport));
        Verify.assertEmpty(Instance.getValueForMetaPropertyToManyResolved(diagram, M3Properties.propertyViews, this.processorSupport));
        Verify.assertEmpty(Instance.getValueForMetaPropertyToManyResolved(diagram, M3Properties.generalizationViews, this.processorSupport));
    }

    @Test
    public void testGeneralizationViewWithNonExistentGeneralization()
    {
        compileTestSource("testModel.pure",
                "Class test::pure::TestClass1 {}\n" +
                        "Class test::pure::TestClass2 {}\n");
        compileTestSource("testDiagram.pure",
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
        CoreInstance diagram = this.runtime.getCoreInstance("test::pure::TestDiagram");
        Assert.assertNotNull(diagram);
        Verify.assertSize(2, Instance.getValueForMetaPropertyToManyResolved(diagram, M3Properties.typeViews, this.processorSupport));
        Verify.assertEmpty(Instance.getValueForMetaPropertyToManyResolved(diagram, M3Properties.associationViews, this.processorSupport));
        Verify.assertEmpty(Instance.getValueForMetaPropertyToManyResolved(diagram, M3Properties.propertyViews, this.processorSupport));
        Verify.assertEmpty(Instance.getValueForMetaPropertyToManyResolved(diagram, M3Properties.generalizationViews, this.processorSupport));
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
        compileTestSource("testDiagram.pure",
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
        CoreInstance diagram = this.runtime.getCoreInstance("test::pure::TestDiagram");
        Assert.assertNotNull(diagram);
        Verify.assertSize(2, Instance.getValueForMetaPropertyToManyResolved(diagram, M3Properties.typeViews, this.processorSupport));
        Verify.assertEmpty(Instance.getValueForMetaPropertyToManyResolved(diagram, M3Properties.associationViews, this.processorSupport));
        Verify.assertEmpty(Instance.getValueForMetaPropertyToManyResolved(diagram, M3Properties.propertyViews, this.processorSupport));
        Verify.assertEmpty(Instance.getValueForMetaPropertyToManyResolved(diagram, M3Properties.generalizationViews, this.processorSupport));
    }

    @Test
    public void testAssociationViewWithInvalidTypeViewId()
    {
        try
        {
            compileTestSource("testModel.pure",
                    "Class test::pure::TestClass1 {}\n" +
                            "Class test::pure::TestClass2 {}\n" +
                            "Association test::pure::TestAssociation\n" +
                            "{\n" +
                            "    prop1:test::pure::TestClass1[0..1];\n" +
                            "    prop2:test::pure::TestClass2[1..*];\n" +
                            "}\n");
            compileTestSource("testDiagram.pure",
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
                            "                                    target=TestAssociation,\n" +
                            "                                    sourcePropertyPosition=(132.5, 76.2),\n" +
                            "                                    sourceMultiplicityPosition=(132.5, 80.0),\n" +
                            "                                    targetPropertyPosition=(155.2, 76.2),\n" +
                            "                                    targetMultiplicityPosition=(155.2, 80.0))\n" +
                            "}\n");
            Assert.fail("Expected a compilation error");
        }
        catch (Exception e)
        {
            assertPureException(PureCompilationException.class, "Object with id 'TestAssociation' is not a TypeView", "testDiagram.pure", 12, 5, 12, 21, 21, 77, e);
        }
    }

    @Test
    public void testDiagramWithIdConflict()
    {
        try
        {
            compileTestSource("testModel.pure",
                    "Class test::pure::TestClass1 {}\n" +
                            "Class test::pure::TestClass2 {}\n");
            compileTestSource("testDiagram.pure",
                    "###Diagram\n" +
                            "Diagram test::pure::TestDiagram(width=10.0, height=10.0)\n" +
                            "{\n" +
                            "    TypeView TestClass(type=test::pure::TestClass1, stereotypesVisible=true, attributesVisible=true,\n" +
                            "                       attributeStereotypesVisible=true, attributeTypesVisible=true,\n" +
                            "                       color=#FFFFCC, lineWidth=1.0,\n" +
                            "                       position=(874.0, 199.46875), width=353.0, height=57.1875)\n" +
                            "    TypeView TestClass(type=test::pure::TestClass2, stereotypesVisible=true, attributesVisible=true,\n" +
                            "                       attributeStereotypesVisible=true, attributeTypesVisible=true,\n" +
                            "                       color=#FFFFCC, lineWidth=1.0,\n" +
                            "                       position=(75.0, 97.1875), width=113.0, height=57.1875)\n" +
                            "}\n");
            Assert.fail("Expected a compilation error");
        }
        catch (Exception e)
        {
            assertPureException(PureCompilationException.class, "Id 'TestClass' is used more than once", "testDiagram.pure", 2, 1, 2, 21, 12, 1, e);
        }
    }

    @Test
    public void testAssociationViewWithWrongSourceType()
    {
        try
        {
            compileTestSource("testModel.pure", "Class test::pure::TestClass1 {}\n" +
                    "Class test::pure::TestClass2 {}\n" +
                    "Association test::pure::TestAssociation\n" +
                    "{\n" +
                    "    prop1:test::pure::TestClass1[0..1];\n" +
                    "    prop2:test::pure::TestClass2[1..*];\n" +
                    "}\n");
            compileTestSource("testDiagram.pure", "###Diagram\n" +
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
                    "                                    source=TestClass2,\n" +
                    "                                    target=TestClass2,\n" +
                    "                                    sourcePropertyPosition=(132.5, 76.2),\n" +
                    "                                    sourceMultiplicityPosition=(132.5, 80.0),\n" +
                    "                                    targetPropertyPosition=(155.2, 76.2),\n" +
                    "                                    targetMultiplicityPosition=(155.2, 80.0))\n" +
                    "}\n");
            Assert.fail("Expected a compilation error");
        }
        catch (Exception e)
        {
            assertPureException(PureCompilationException.class, "Source type for AssociationView TestAssociation (test::pure::TestClass2) does not match the source type of the association test::pure::TestAssociation (test::pure::TestClass1)", "testDiagram.pure", 12, 5, 12, 21, 21, 77, e);
        }
    }

    @Test
    public void testAssociationViewWithWrongTargetType()
    {
        try
        {
            compileTestSource("testModel.pure", "Class test::pure::TestClass1 {}\n" +
                    "Class test::pure::TestClass2 {}\n" +
                    "Association test::pure::TestAssociation\n" +
                    "{\n" +
                    "    prop1:test::pure::TestClass1[0..1];\n" +
                    "    prop2:test::pure::TestClass2[1..*];\n" +
                    "}\n");
            compileTestSource("testDiagram.pure", "###Diagram\n" +
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
                    "                                    target=TestClass1,\n" +
                    "                                    sourcePropertyPosition=(132.5, 76.2),\n" +
                    "                                    sourceMultiplicityPosition=(132.5, 80.0),\n" +
                    "                                    targetPropertyPosition=(155.2, 76.2),\n" +
                    "                                    targetMultiplicityPosition=(155.2, 80.0))\n" +
                    "}\n");
            Assert.fail("Expected a compilation error");
        }
        catch (Exception e)
        {
            assertPureException(PureCompilationException.class, "Target type for AssociationView TestAssociation (test::pure::TestClass1) does not match the target type of the association test::pure::TestAssociation (test::pure::TestClass2)", "testDiagram.pure", 12, 5, 12, 21, 21, 77, e);
        }
    }

    @Test
    public void testPropertyViewWithWrongSourceType()
    {
        try
        {
            compileTestSource("testModel.pure", "Class test::pure::TestClass1\n" +
                    "{\n" +
                    "    prop:test::pure::TestClass2[1];\n" +
                    "}\n" +
                    "Class test::pure::TestClass2 {}\n");
            compileTestSource("testDiagram.pure", "###Diagram\n" +
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
                    "    PropertyView TestClass1_prop(property=test::pure::TestClass1.prop, stereotypesVisible=true, nameVisible=false,\n" +
                    "                                 color=#000000, lineWidth=1.0,\n" +
                    "                                 lineStyle=SIMPLE, points=[(132.5, 77.0), (155.2, 77.0)],\n" +
                    "                                 label='TestClass1.prop',\n" +
                    "                                 source=TestClass2,\n" +
                    "                                 target=TestClass2,\n" +
                    "                                 propertyPosition=(132.5, 76.2),\n" +
                    "                                 multiplicityPosition=(132.5, 80.0))\n" +
                    "}\n");
            Assert.fail("Expected a compilation error");
        }
        catch (Exception e)
        {
            assertPureException(PureCompilationException.class, "Source type for PropertyView TestClass1_prop (test::pure::TestClass2) does not match the owner of the property test::pure::TestClass1.prop (test::pure::TestClass1)", "testDiagram.pure", 12, 5, 12, 18, 19, 68, e);
        }
    }

    @Test
    public void testPropertyViewWithAssociationProperty()
    {
        try
        {
            compileTestSource("testModel.pure",
                    "Class test::pure::TestClass1 {}\n" +
                            "Class test::pure::TestClass2 {}\n" +
                            "Association test::pure::TestAssoc\n" +
                            "{\n" +
                            "  prop1:test::pure::TestClass1[*];\n" +
                            "  prop2:test::pure::TestClass2[*];\n" +
                            "}\n");
            compileTestSource("testDiagram.pure",
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
                            "    PropertyView TestClass1_testProperty(property=test::pure::TestClass1.prop2, stereotypesVisible=true, nameVisible=false,\n" +
                            "                                         color=#000000, lineWidth=1.0,\n" +
                            "                                         lineStyle=SIMPLE, points=[(132.5, 77.0), (155.2, 77.0)],\n" +
                            "                                         label='Employment',\n" +
                            "                                         source=TestClass1,\n" +
                            "                                         target=TestClass2,\n" +
                            "                                         propertyPosition=(132.5, 76.2),\n" +
                            "                                         multiplicityPosition=(132.5, 80.0))\n" +
                            "}\n");
            Assert.fail("Expected a compilation error");
        }
        catch (Exception e)
        {
            assertPureException(PureCompilationException.class, "Source type for PropertyView TestClass1_testProperty (test::pure::TestClass1) does not match the owner of the property test::pure::TestAssoc.prop2 (test::pure::TestAssoc)", "testDiagram.pure", 12, 5, 12, 18, 19, 76, e);
        }
    }

    @Test
    public void testPropertyViewWithWrongTargetType()
    {
        try
        {
            compileTestSource("testModel.pure", "Class test::pure::TestClass1\n" +
                    "{\n" +
                    "    prop:test::pure::TestClass2[1];\n" +
                    "}\n" +
                    "Class test::pure::TestClass2 {}\n");
            compileTestSource("testDiagram.pure", "###Diagram\n" +
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
                    "    PropertyView TestClass1_prop(property=test::pure::TestClass1.prop, stereotypesVisible=true, nameVisible=false,\n" +
                    "                                 color=#000000, lineWidth=1.0,\n" +
                    "                                 lineStyle=SIMPLE, points=[(132.5, 77.0), (155.2, 77.0)],\n" +
                    "                                 label='TestClass1.prop',\n" +
                    "                                 source=TestClass1,\n" +
                    "                                 target=TestClass1,\n" +
                    "                                 propertyPosition=(132.5, 76.2),\n" +
                    "                                 multiplicityPosition=(132.5, 80.0))\n" +
                    "}\n");
            Assert.fail("Expected a compilation error");
        }
        catch (Exception e)
        {
            assertPureException(PureCompilationException.class, "Target type for PropertyView TestClass1_prop (test::pure::TestClass1) does not match the target type of the property test::pure::TestClass1.prop (test::pure::TestClass2)", "testDiagram.pure", 12, 5, 12, 18, 19, 68, e);
        }
    }

    private static final String TEST_MODEL_SOURCE_ID = "testModel.pure";
    private static final String TEST_DIAGRAM_SOURCE_ID = "testDiagram.pure";
    private static final ImmutableMap<String, String> TEST_SOURCES = Maps.immutable.with(
            TEST_MODEL_SOURCE_ID,
            "import model::test::*;\n" +
                    "Class model::test::A\n" +
                    "{\n" +
                    "  prop:model::test::B[0..1];\n" +
                    "}\n" +
                    "Class model::test::B extends A {}\n" +
                    "Association model::test::A2B\n" +
                    "{\n" +
                    "  a : A[1];\n" +
                    "  b : B[*];\n" +
                    "}\n",
            TEST_DIAGRAM_SOURCE_ID,
            "###Diagram\n" +
                    "import model::test::*;" +
                    "\n" +
                    "Diagram model::test::TestDiagram(width=5000.3, height=2700.6)\n" +
                    "{\n" +
                    "    TypeView A(type=model::test::A, stereotypesVisible=true, attributesVisible=true,\n" +
                    "               attributeStereotypesVisible=true, attributeTypesVisible=true,\n" +
                    "               color=#FFFFCC, lineWidth=1.0,\n" +
                    "               position=(874.0, 199.46875), width=353.0, height=57.1875)\n" +
                    "    TypeView B(type=model::test::B, stereotypesVisible=true, attributesVisible=true,\n" +
                    "               attributeStereotypesVisible=true, attributeTypesVisible=true,\n" +
                    "               color=#FFFFCC, lineWidth=1.0,\n" +
                    "               position=(75.0, 97.1875), width=113.0, height=57.1875)\n" +
                    "    AssociationView A2B(association=model::test::A2B, stereotypesVisible=true, nameVisible=false,\n" +
                    "                        color=#000000, lineWidth=1.0,\n" +
                    "                        lineStyle=SIMPLE, points=[(132.5, 77.0), (155.2, 77.0)],\n" +
                    "                        label='A to B',\n" +
                    "                        source=A,\n" +
                    "                        target=B,\n" +
                    "                        sourcePropertyPosition=(132.5, 76.2),\n" +
                    "                        sourceMultiplicityPosition=(132.5, 80.0),\n" +
                    "                        targetPropertyPosition=(155.2, 76.2),\n" +
                    "                        targetMultiplicityPosition=(155.2, 80.0))\n" +
                    "    PropertyView A_prop(property=A.prop, stereotypesVisible=true, nameVisible=false,\n" +
                    "                        color=#000000, lineWidth=1.0,\n" +
                    "                        lineStyle=SIMPLE, points=[(132.5, 77.0), (155.2, 77.0)],\n" +
                    "                        label='A.prop',\n" +
                    "                        source=A,\n" +
                    "                        target=B,\n" +
                    "                        propertyPosition=(132.5, 76.2),\n" +
                    "                        multiplicityPosition=(132.5, 80.0))\n" +
                    "    GeneralizationView B_A(color=#000000, lineWidth=1.0,\n" +
                    "                           lineStyle=SIMPLE, points=[(132.5, 77.0), (155.2, 77.0)],\n" +
                    "                           label='',\n" +
                    "                           source=B,\n" +
                    "                           target=A)\n" +
                    "}\n"
    );

    @Test
    public void testMilestonedPropertiesQualifiedPropertiesAreAvailableInTheDiagrams() throws Exception
    {
        MutableMap<String, String> MILESTONED_TEST_SOURCES = Maps.mutable.with(TEST_MODEL_SOURCE_ID,
                "import model::test::*;\n" +
                        "Class model::test::A\n" +
                        "{\n" +
                        "  prop:model::test::B[0..1];\n" +
                        "}\n" +
                        "Class <<temporal.businesstemporal>> model::test::B {}\n" +
                        "Association model::test::A2B\n" +
                        "{\n" +
                        "  a : A[1];\n" +
                        "  prop2 : B[*];\n" +
                        "}\n", TEST_DIAGRAM_SOURCE_ID, TEST_SOURCES.get(TEST_DIAGRAM_SOURCE_ID));


        this.runtime.createInMemoryAndCompile(MILESTONED_TEST_SOURCES);
        CoreInstance a = this.runtime.getCoreInstance("model::test::A");
        CoreInstance edgePointPropertyFromAssociations = a.getValueForMetaPropertyToOne(M3Properties.properties);
        Assert.assertTrue(MilestoningFunctions.isEdgePointProperty(edgePointPropertyFromAssociations, processorSupport));
        Assert.assertEquals("propAllVersions", edgePointPropertyFromAssociations.getName());
        CoreInstance testDiagram = this.runtime.getCoreInstance("model::test::TestDiagram");
        CoreInstance edgePointPropertyInDiagram = Instance.getValueForMetaPropertyToOneResolved(testDiagram, M3Properties.propertyViews, M3Properties.property, processorSupport);
        Assert.assertTrue(MilestoningFunctions.isGeneratedQualifiedPropertyWithWithAllMilestoningDatesSpecified(edgePointPropertyInDiagram, processorSupport));
        Assert.assertEquals("prop", edgePointPropertyInDiagram.getValueForMetaPropertyToOne(M3Properties.functionName).getName());

        CoreInstance association = Instance.getValueForMetaPropertyToOneResolved(testDiagram, M3Properties.associationViews, M3Properties.association, processorSupport);
        ListIterable<String> associationPropertyNames = association.getValueForMetaPropertyToMany(M3Properties.properties).collect(CoreInstance.GET_NAME);
        Assert.assertEquals(FastList.newListWith("a", "prop2AllVersions"), associationPropertyNames);
        ListIterable<? extends CoreInstance> qualifiedPropertyNames = Instance.getValueForMetaPropertyToManyResolved(association, M3Properties.qualifiedProperties, processorSupport);
        Assert.assertEquals("prop2", Instance.getValueForMetaPropertyToOneResolved(qualifiedPropertyNames.getFirst(), M3Properties.functionName, processorSupport).getName());
        Assert.assertEquals("prop2AllVersionsInRange", Instance.getValueForMetaPropertyToOneResolved(qualifiedPropertyNames.getLast(), M3Properties.functionName, processorSupport).getName());
    }
}
