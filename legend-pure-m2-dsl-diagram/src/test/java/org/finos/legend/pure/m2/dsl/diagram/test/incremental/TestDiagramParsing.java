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

import org.finos.legend.pure.m3.AbstractPureTestWithCoreCompiled;
import org.finos.legend.pure.m3.coreinstance.meta.pure.diagram.AssociationView;
import org.finos.legend.pure.m3.coreinstance.meta.pure.diagram.TypeView;
import org.finos.legend.pure.m3.coreinstance.meta.pure.metamodel.ReferenceUsage;
import org.finos.legend.pure.m3.coreinstance.meta.pure.metamodel.relationship.Association;
import org.finos.legend.pure.m3.coreinstance.meta.pure.metamodel.type.Class;
import org.finos.legend.pure.m3.navigation.Instance;
import org.finos.legend.pure.m3.navigation.M3Properties;
import org.finos.legend.pure.m3.serialization.grammar.Parser;
import org.finos.legend.pure.m3.serialization.grammar.ParserLibrary;
import org.finos.legend.pure.m4.coreinstance.CoreInstance;
import org.finos.legend.pure.m4.coreinstance.SourceInformation;
import org.finos.legend.pure.m4.serialization.grammar.antlr.PureParserException;
import org.eclipse.collections.api.RichIterable;
import org.eclipse.collections.api.block.predicate.Predicate;
import org.junit.After;
import org.junit.Assert;
import org.junit.BeforeClass;
import org.junit.Test;

import java.lang.reflect.Field;

public class TestDiagramParsing extends AbstractPureTestWithCoreCompiled
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

    private Parser getRuntimeDiagramParser() throws NoSuchFieldException, IllegalAccessException
    {
        Field field = this.runtime.getSourceRegistry().getClass().getDeclaredField("parserLibrary");
        field.setAccessible(true);
        ParserLibrary parserLibrary = (ParserLibrary)field.get(this.runtime.getSourceRegistry());
        Parser parser = parserLibrary.getParser("Diagram");

        return parser;
    }

    @Test
    public void testDiagramWithInvalidGeometry() throws NoSuchFieldException, IllegalAccessException
    {
        Parser parser = getRuntimeDiagramParser();
        // Empty geometry
        try
        {
            compileTestSource("###Diagram\nDiagram test::pure::TestDiagram() {}");
            Assert.fail("Expected a parser error");
        }
        catch (Exception e)
        {
            assertPureException(PureParserException.class, "expected: one of {WIDTH, HEIGHT} found: ')'", 2, 33, e);
        }

        // Width but no height
        try
        {
            compileTestSource("###Diagram\nDiagram test::pure::TestDiagram(width=10.0) {}");
            Assert.fail("Expected a parser error");
        }
        catch (Exception e)
        {
            assertPureException(PureParserException.class, "expected: ',' found: ')'", 2, 43, e);
        }

        // Height but no width
        try
        {
            compileTestSource("###Diagram\nDiagram test::pure::TestDiagram(height=10.0) {}");
            Assert.fail("Expected a parser error");
        }
        catch (Exception e)
        {
            assertPureException(PureParserException.class, "expected: ',' found: ')'", 2, 44, e);
        }

        // Wrong property first
        try
        {
            compileTestSource("###Diagram\nDiagram test::pure::TestDiagram(junk=10.0) {}");
            Assert.fail("Expected a parser error");
        }
        catch (Exception e)
        {
            assertPureException(PureParserException.class, "expected: one of {WIDTH, HEIGHT} found: 'junk'", 2, 33, e);
        }

        // Wrong property second
        try
        {
            compileTestSource("###Diagram\nDiagram test::pure::TestDiagram(width=10.0, junk=10.0) {}");
            Assert.fail("Expected a parser error");
        }
        catch (Exception e)
        {
            assertPureException(PureParserException.class, "expected: HEIGHT found: 'junk'", 2, 45, e);
        }

        // Extra property
        try
        {
            compileTestSource("###Diagram\nDiagram test::pure::TestDiagram(width=10.0, height=10.0, junk=13.2) {}");
            Assert.fail("Expected a parser error");
        }
        catch (Exception e)
        {
            assertPureException(PureParserException.class, "expected: ')' found: ','", 2, 56, e);
        }
    }

    @Test
    public void testDiagramWithInvalidTypeView() throws NoSuchFieldException, IllegalAccessException
    {
        Parser parser = getRuntimeDiagramParser();
        // Empty type view
        try
        {
            compileTestSource("###Diagram\n" +
                    "Diagram test::pure::TestDiagram(width=10.0, height=10.0)\n" +
                    "{\n" +
                    "    TypeView TestClass_1()\n" +
                    "}\n");
            Assert.fail("Expected a parser error");
        }
        catch (Exception e)
        {
            assertPureException(PureParserException.class, "expected: one of {TYPE, STEREOTYPES_VISIBLE, ATTRIBUTES_VISIBLE, ATTRIBUTE_STEREOTYPES_VISIBLE, ATTRIBUTE_TYPES_VISIBLE, COLOR, LINE_WIDTH, POSITION, WIDTH, HEIGHT} found: ')'", 4, 26, e);
        }

        // Type view with one bogus property
        try
        {
            compileTestSource("###Diagram\n" +
                    "Diagram test::pure::TestDiagram(width=10.0, height=10.0)\n" +
                    "{\n" +
                    "    TypeView TestClass_1(junk=13.6)\n" +
                    "}\n");
            Assert.fail("Expected a parser error");
        }
        catch (Exception e)
        {
            assertPureException(PureParserException.class, "expected: one of {TYPE, STEREOTYPES_VISIBLE, ATTRIBUTES_VISIBLE, ATTRIBUTE_STEREOTYPES_VISIBLE, ATTRIBUTE_TYPES_VISIBLE, COLOR, LINE_WIDTH, POSITION, WIDTH, HEIGHT} found: 'junk'", 4, 26, e);
        }

        // Type view with all valid properties but one
        try
        {
            compileTestSource("###Diagram\n" +
                    "Diagram test::pure::TestDiagram(width=10.0, height=10.0)\n" +
                    "{\n" +
                    "    TypeView TestClass_1(stereotypesVisible=true, attributesVisible=true,\n" +
                    "                         attributeStereotypesVisible=true, attributeTypesVisible=true,\n" +
                    "                         color=#FFFFCC, lineWidth=1.0,\n" +
                    "                         position=(874.0, 199.46875), width=353.0, height=57.1875)\n" +
                    "}\n");
            Assert.fail("Expected a parser error");
        }
        catch (Exception e)
        {
            assertPureException(PureParserException.class, "Missing value for property 'type' on TypeView TestClass_1", 4, 14, e);
        }

        try
        {
            compileTestSource("###Diagram\n" +
                    "Diagram test::pure::TestDiagram(width=10.0, height=10.0)\n" +
                    "{\n" +
                    "    TypeView TestClass_1(type=test::pure::TestClass, stereotypesVisible=true, attributesVisible=true,\n" +
                    "                         attributeStereotypesVisible=true, attributeTypesVisible=true,\n" +
                    "                         color=#FFFFCC,\n" +
                    "                         position=(874.0, 199.46875), width=353.0, height=57.1875)\n" +
                    "}\n");
            Assert.fail("Expected a parser error");
        }
        catch (Exception e)
        {
            assertPureException(PureParserException.class, "Missing value for property 'lineWidth' on TypeView TestClass_1", 4, 14, e);
        }
    }

    @Test
    public void testDiagramWithInvalidAssociationView() throws NoSuchFieldException, IllegalAccessException
    {
        Parser parser = getRuntimeDiagramParser();
        // Empty association view
        try
        {
            compileTestSource("###Diagram\n" +
                    "Diagram test::pure::TestDiagram(width=10.0, height=10.0)\n" +
                    "{\n" +
                    "    AssociationView TestAssociation_1()\n" +
                    "}\n");
            Assert.fail("Expected a parser error");
        }
        catch (Exception e)
        {
            assertPureException(PureParserException.class, "expected: one of {ASSOCIATION, STEREOTYPES_VISIBLE, NAME_VISIBLE, COLOR, LINE_WIDTH, LINE_STYLE, POINTS, LABEL, SOURCE, TARGET, SOURCE_PROP_POSITION, SOURCE_MULT_POSITION, TARGET_PROP_POSITION, TARGET_MULT_POSITION} found: ')'", 4, 39, e);
        }

        // Association view with one bogus property
        try
        {
            compileTestSource("###Diagram\n" +
                    "Diagram test::pure::TestDiagram(width=10.0, height=10.0)\n" +
                    "{\n" +
                    "    AssociationView TestAssociation_1(junk=13.6)\n" +
                    "}\n");
            Assert.fail("Expected a parser error");
        }
        catch (Exception e)
        {
            assertPureException(PureParserException.class, "expected: one of {ASSOCIATION, STEREOTYPES_VISIBLE, NAME_VISIBLE, COLOR, LINE_WIDTH, LINE_STYLE, POINTS, LABEL, SOURCE, TARGET, SOURCE_PROP_POSITION, SOURCE_MULT_POSITION, TARGET_PROP_POSITION, TARGET_MULT_POSITION} found: 'junk'", 4, 39, e);
        }

        // Association view with all valid properties but one
        try
        {
            compileTestSource("###Diagram\n" +
                    "Diagram test::pure::TestDiagram(width=10.0, height=10.0)\n" +
                    "{\n" +
                    "    TypeView TestClass1_1(type=test::pure::TestClass1, stereotypesVisible=true, attributesVisible=true,\n" +
                    "                          attributeStereotypesVisible=true, attributeTypesVisible=true,\n" +
                    "                          color=#FFFFCC, lineWidth=1.0,\n" +
                    "                          position=(874.0, 199.46875), width=353.0, height=57.1875)\n" +
                    "    TypeView TestClass2_2(type=test::pure::TestClass2, stereotypesVisible=true, attributesVisible=true,\n" +
                    "                          attributeStereotypesVisible=true, attributeTypesVisible=true,\n" +
                    "                          color=#FFFFCC, lineWidth=1.0,\n" +
                    "                          position=(75.0, 97.1875), width=113.0, height=57.1875)\n" +
                    "    AssociationView TestAssociation_1(stereotypesVisible=true, nameVisible=false,\n" +
                    "                                      color=#000000, lineWidth=1.0,\n" +
                    "                                      lineStyle=SIMPLE, points=[(132.5, 77.0), (155.2, 77.0)],\n" +
                    "                                      label='Employment',\n" +
                    "                                      source=TestClass1_1,\n" +
                    "                                      target=TestClass2_2,\n" +
                    "                                      sourcePropertyPosition=(132.5, 76.2),\n" +
                    "                                      sourceMultiplicityPosition=(132.5, 80.0),\n" +
                    "                                      targetPropertyPosition=(155.2, 76.2),\n" +
                    "                                      targetMultiplicityPosition=(155.2, 80.0))\n" +
                    "}\n");
            Assert.fail("Expected a parser error");
        }
        catch (Exception e)
        {
            assertPureException(PureParserException.class, "Missing value for property 'association' on AssociationView TestAssociation_1", 12, 21, e);
        }

        try
        {
            compileTestSource("###Diagram\n" +
                    "Diagram test::pure::TestDiagram(width=10.0, height=10.0)\n" +
                    "{\n" +
                    "    TypeView TestClass1_1(type=test::pure::TestClass1, stereotypesVisible=true, attributesVisible=true,\n" +
                    "                          attributeStereotypesVisible=true, attributeTypesVisible=true,\n" +
                    "                          color=#FFFFCC, lineWidth=1.0,\n" +
                    "                          position=(874.0, 199.46875), width=353.0, height=57.1875)\n" +
                    "    TypeView TestClass2_2(type=test::pure::TestClass2, stereotypesVisible=true, attributesVisible=true,\n" +
                    "                          attributeStereotypesVisible=true, attributeTypesVisible=true,\n" +
                    "                          color=#FFFFCC, lineWidth=1.0,\n" +
                    "                          position=(75.0, 97.1875), width=113.0, height=57.1875)\n" +
                    "    AssociationView TestAssociation_1(association=test::pure::TestAssociation, stereotypesVisible=true, nameVisible=false,\n" +
                    "                                      color=#000000,\n" +
                    "                                      lineStyle=SIMPLE, points=[(132.5, 77.0), (155.2, 77.0)],\n" +
                    "                                      label='Employment',\n" +
                    "                                      source=TestClass1_1,\n" +
                    "                                      target=TestClass2_2,\n" +
                    "                                      sourcePropertyPosition=(132.5, 76.2),\n" +
                    "                                      sourceMultiplicityPosition=(132.5, 80.0),\n" +
                    "                                      targetPropertyPosition=(155.2, 76.2),\n" +
                    "                                      targetMultiplicityPosition=(155.2, 80.0))\n" +
                    "}\n");
            Assert.fail("Expected a parser error");
        }
        catch (Exception e)
        {
            assertPureException(PureParserException.class, "Missing value for property 'lineWidth' on AssociationView TestAssociation_1", 12, 21, e);
        }
    }

    @Test
    public void testDiagramWithInvalidPropertyView() throws NoSuchFieldException, IllegalAccessException
    {
        Parser parser = getRuntimeDiagramParser();
        // Create class with property
        compileTestSource("Class test::pure::TestClass1\n" +
                "{\n" +
                "    testProperty : test::pure::TestClass2[1];\n" +
                "}\n" +
                "Class test::pure::TestClass2\n" +
                "{\n" +
                "}\n");

        // Empty property view
        try
        {
            compileTestSource("###Diagram\n" +
                    "Diagram test::pure::TestDiagram(width=10.0, height=10.0)\n" +
                    "{\n" +
                    "    PropertyView TestClass1_testProperty_1()\n" +
                    "}\n");
            Assert.fail("Expected a parser error");
        }
        catch (Exception e)
        {
            assertPureException(PureParserException.class, "expected: one of {PROPERTY, STEREOTYPES_VISIBLE, NAME_VISIBLE, COLOR, LINE_WIDTH, LINE_STYLE, POINTS, LABEL, SOURCE, TARGET, PROP_POSITION, MULT_POSITION} found: ')'", 4, 44, e);
        }

        // Property view with one bogus property
        try
        {
            compileTestSource("###Diagram\n" +
                    "Diagram test::pure::TestDiagram(width=10.0, height=10.0)\n" +
                    "{\n" +
                    "    PropertyView TestClass1_testProperty_1(junk=13.6)\n" +
                    "}\n");
            Assert.fail("Expected a parser error");
        }
        catch (Exception e)
        {
            assertPureException(PureParserException.class, "expected: one of {PROPERTY, STEREOTYPES_VISIBLE, NAME_VISIBLE, COLOR, LINE_WIDTH, LINE_STYLE, POINTS, LABEL, SOURCE, TARGET, PROP_POSITION, MULT_POSITION} found: 'junk'", 4, 44, e);
        }

        // Property view with all valid properties but one
        try
        {
            compileTestSource("###Diagram\n" +
                    "Diagram test::pure::TestDiagram(width=10.0, height=10.0)\n" +
                    "{\n" +
                    "    TypeView TestClass1_1(type=test::pure::TestClass1, stereotypesVisible=true, attributesVisible=true,\n" +
                    "                          attributeStereotypesVisible=true, attributeTypesVisible=true,\n" +
                    "                          color=#FFFFCC, lineWidth=1.0,\n" +
                    "                          position=(874.0, 199.46875), width=353.0, height=57.1875)\n" +
                    "    TypeView TestClass2_2(type=test::pure::TestClass2, stereotypesVisible=true, attributesVisible=true,\n" +
                    "                          attributeStereotypesVisible=true, attributeTypesVisible=true,\n" +
                    "                          color=#FFFFCC, lineWidth=1.0,\n" +
                    "                          position=(75.0, 97.1875), width=113.0, height=57.1875)\n" +
                    "    PropertyView TestClass1_testProperty_1(stereotypesVisible=true, nameVisible=false,\n" +
                    "                                           color=#000000, lineWidth=1.0,\n" +
                    "                                           lineStyle=SIMPLE, points=[(132.5, 77.0), (155.2, 77.0)],\n" +
                    "                                           label='Employment',\n" +
                    "                                           source=TestClass1_1,\n" +
                    "                                           target=TestClass2_2,\n" +
                    "                                           propertyPosition=(132.5, 76.2),\n" +
                    "                                           multiplicityPosition=(132.5, 80.0))\n" +
                    "}\n");
            Assert.fail("Expected a parser error");
        }
        catch (Exception e)
        {
            assertPureException(PureParserException.class, "Missing value for property 'property' on PropertyView TestClass1_testProperty_1", 12, 18, e);
        }

        try
        {
            compileTestSource("###Diagram\n" +
                    "Diagram test::pure::TestDiagram(width=10.0, height=10.0)\n" +
                    "{\n" +
                    "    TypeView TestClass1_1(type=test::pure::TestClass1, stereotypesVisible=true, attributesVisible=true,\n" +
                    "                          attributeStereotypesVisible=true, attributeTypesVisible=true,\n" +
                    "                          color=#FFFFCC, lineWidth=1.0,\n" +
                    "                          position=(874.0, 199.46875), width=353.0, height=57.1875)\n" +
                    "    TypeView TestClass2_2(type=test::pure::TestClass2, stereotypesVisible=true, attributesVisible=true,\n" +
                    "                          attributeStereotypesVisible=true, attributeTypesVisible=true,\n" +
                    "                          color=#FFFFCC, lineWidth=1.0,\n" +
                    "                          position=(75.0, 97.1875), width=113.0, height=57.1875)\n" +
                    "    PropertyView TestClass1_testProperty_1(property=test::pure::TestClass1.testProperty, stereotypesVisible=true, nameVisible=false,\n" +
                    "                                           color=#000000,\n" +
                    "                                           lineStyle=SIMPLE, points=[(132.5, 77.0), (155.2, 77.0)],\n" +
                    "                                           label='Employment',\n" +
                    "                                           source=TestClass1_1,\n" +
                    "                                           target=TestClass2_2,\n" +
                    "                                           propertyPosition=(132.5, 76.2),\n" +
                    "                                           multiplicityPosition=(132.5, 80.0))\n" +
                    "}\n");
            Assert.fail("Expected a parser error");
        }
        catch (Exception e)
        {
            assertPureException(PureParserException.class, "Missing value for property 'lineWidth' on PropertyView TestClass1_testProperty_1", 12, 18, e);
        }
    }

    @Test
    public void testDiagramWithInvalidGeneralizationView() throws NoSuchFieldException, IllegalAccessException
    {
        Parser parser = getRuntimeDiagramParser();
        // Empty generalization view
        try
        {
            compileTestSource("###Diagram\n" +
                    "Diagram test::pure::TestDiagram(width=10.0, height=10.0)\n" +
                    "{\n" +
                    "    GeneralizationView TestClass1_TestClass2_1()\n" +
                    "}\n");
            Assert.fail("Expected a parser error");
        }
        catch (Exception e)
        {
            assertPureException(PureParserException.class, "expected: one of {COLOR, LINE_WIDTH, LINE_STYLE, POINTS, LABEL, SOURCE, TARGET} found: ')'", 4, 48, e);
        }

        // Association view with one bogus property
        try
        {
            compileTestSource("###Diagram\n" +
                    "Diagram test::pure::TestDiagram(width=10.0, height=10.0)\n" +
                    "{\n" +
                    "    GeneralizationView TestClass1_TestClass2_1(junk=13.6)\n" +
                    "}\n");
            Assert.fail("Expected a parser error");
        }
        catch (Exception e)
        {
            assertPureException(PureParserException.class, "expected: one of {COLOR, LINE_WIDTH, LINE_STYLE, POINTS, LABEL, SOURCE, TARGET} found: 'junk'", 4, 48, e);
        }

        // Association view with all valid properties but one
        try
        {
            compileTestSource("###Diagram\n" +
                    "Diagram test::pure::TestDiagram(width=10.0, height=10.0)\n" +
                    "{\n" +
                    "    TypeView TestClass1_1(type=test::pure::TestClass1, stereotypesVisible=true, attributesVisible=true,\n" +
                    "                          attributeStereotypesVisible=true, attributeTypesVisible=true,\n" +
                    "                          color=#FFFFCC, lineWidth=1.0,\n" +
                    "                          position=(874.0, 199.46875), width=353.0, height=57.1875)\n" +
                    "    TypeView TestClass2_2(type=test::pure::TestClass2, stereotypesVisible=true, attributesVisible=true,\n" +
                    "                          attributeStereotypesVisible=true, attributeTypesVisible=true,\n" +
                    "                          color=#FFFFCC, lineWidth=1.0,\n" +
                    "                          position=(75.0, 97.1875), width=113.0, height=57.1875)\n" +
                    "    GeneralizationView TestClass1_TestClass2_1(lineWidth=1.0,\n" +
                    "                                               lineStyle=SIMPLE, points=[(132.5, 77.0), (155.2, 77.0)],\n" +
                    "                                               label='',\n" +
                    "                                               source=TestClass1_1,\n" +
                    "                                               target=TestClass2_2)\n" +
                    "}\n");
            Assert.fail("Expected a parser error");
        }
        catch (Exception e)
        {
            assertPureException(PureParserException.class, "Missing value for property 'color' on GeneralizationView TestClass1_TestClass2_1", 12, 24, e);
        }

        try
        {
            compileTestSource("###Diagram\n" +
                    "Diagram test::pure::TestDiagram(width=10.0, height=10.0)\n" +
                    "{\n" +
                    "    TypeView TestClass1_1(type=test::pure::TestClass1, stereotypesVisible=true, attributesVisible=true,\n" +
                    "                          attributeStereotypesVisible=true, attributeTypesVisible=true,\n" +
                    "                          color=#FFFFCC, lineWidth=1.0,\n" +
                    "                          position=(874.0, 199.46875), width=353.0, height=57.1875)\n" +
                    "    TypeView TestClass2_2(type=test::pure::TestClass2, stereotypesVisible=true, attributesVisible=true,\n" +
                    "                          attributeStereotypesVisible=true, attributeTypesVisible=true,\n" +
                    "                          color=#FFFFCC, lineWidth=1.0,\n" +
                    "                          position=(75.0, 97.1875), width=113.0, height=57.1875)\n" +
                    "    GeneralizationView TestClass1_TestClass2_1(color=#000000,\n" +
                    "                                               lineStyle=SIMPLE, points=[(132.5, 77.0), (155.2, 77.0)],\n" +
                    "                                               label='',\n" +
                    "                                               source=TestClass1_1,\n" +
                    "                                               target=TestClass2_2)\n" +
                    "}\n");
            Assert.fail("Expected a parser error");
        }
        catch (Exception e)
        {
            assertPureException(PureParserException.class, "Missing value for property 'lineWidth' on GeneralizationView TestClass1_TestClass2_1", 12, 24, e);
        }
    }

    @Test
    public void testDiagramWithNoGeometry()
    {
        compileTestSource("###Diagram\n" +
                "Diagram test::pure::TestDiagram {}\n");
        CoreInstance diagram = this.runtime.getCoreInstance("test::pure::TestDiagram");
        Assert.assertNotNull(diagram);

        CoreInstance geometry = Instance.getValueForMetaPropertyToOneResolved(diagram, "rectangleGeometry", this.processorSupport);
        Assert.assertNotNull(diagram);

        CoreInstance width = Instance.getValueForMetaPropertyToOneResolved(geometry, "width", this.processorSupport);
        Assert.assertNotNull(width);
        Assert.assertTrue(Instance.instanceOf(width, "Float", this.processorSupport));
        Assert.assertEquals("0.0", width.getName());

        CoreInstance height = Instance.getValueForMetaPropertyToOneResolved(geometry, "height", this.processorSupport);
        Assert.assertNotNull(height);
        Assert.assertTrue(Instance.instanceOf(height, "Float", this.processorSupport));
        Assert.assertEquals("0.0", height.getName());
    }

    @Test
    public void testPropertyViewWithPropertyNamedPosition()
    {
        compileTestSource("testModel.pure",
                          "Class test::pure::TestClass1\n" +
                          "{\n" +
                          "    position:test::pure::TestClass2[1];\n" +
                          "}\n" +
                          "\n" +
                          "Class test::pure::TestClass2\n" +
                          "{\n" +
                          "}\n");
        compileTestSource("testDiagram.pure",
                          "###Diagram\n" +
                          "Diagram test::pure::TestDiagram(width=10.0, height=10.0)\n" +
                          "{\n" +
                          "    TypeView TestClass1_1(type=test::pure::TestClass1, stereotypesVisible=true, attributesVisible=true,\n" +
                          "                          attributeStereotypesVisible=true, attributeTypesVisible=true,\n" +
                          "                          color=#FFFFCC, lineWidth=1.0,\n" +
                          "                          position=(874.0, 199.46875), width=353.0, height=57.1875)\n" +
                          "    TypeView TestClass2_2(type=test::pure::TestClass2, stereotypesVisible=true, attributesVisible=true,\n" +
                          "                          attributeStereotypesVisible=true, attributeTypesVisible=true,\n" +
                          "                          color=#FFFFCC, lineWidth=1.0,\n" +
                          "                          position=(75.0, 97.1875), width=113.0, height=57.1875)\n" +
                          "    PropertyView TestClass1_position_1(property=test::pure::TestClass1.position, stereotypesVisible=true, nameVisible=false,\n" +
                          "                                       color=#000000, lineWidth=1.0,\n" +
                          "                                       lineStyle=SIMPLE, points=[(132.5, 77.0), (155.2, 77.0)],\n" +
                          "                                       label='Employment',\n" +
                          "                                       source=TestClass1_1,\n" +
                          "                                       target=TestClass2_2,\n" +
                          "                                       propertyPosition=(132.5, 76.2),\n" +
                          "                                       multiplicityPosition=(132.5, 80.0))\n" +
                          "}\n");
        CoreInstance testClass1 = this.runtime.getCoreInstance("test::pure::TestClass1");
        CoreInstance positionProp = this.processorSupport.class_findPropertyUsingGeneralization(testClass1, "position");
        Assert.assertNotNull(positionProp);

        CoreInstance diagram = this.runtime.getCoreInstance("test::pure::TestDiagram");
        CoreInstance propView = Instance.getValueForMetaPropertyToOneResolved(diagram, M3Properties.propertyViews, this.processorSupport);
        CoreInstance viewProp = Instance.getValueForMetaPropertyToOneResolved(propView, M3Properties.property, this.processorSupport);
        Assert.assertSame(positionProp, viewProp);
    }

    @Test
    public void testDiagramWithVariousWhiteSpace()
    {
        compileTestSource("testModel.pure",
                          "Class test::pure::TestClass1\n" +
                          "{\n" +
                          "    position:test::pure::TestClass2[1];\n" +
                          "}\n" +
                          "\n" +
                          "Class test::pure::TestClass2\n" +
                          "{\n" +
                          "}\n");
        compileTestSource("testDiagram.pure",
                          "###Diagram\n" +
                          "Diagram test::pure::TestDiagram(width=10.0, height=10.0)\n" +
                          "{\n" +
                          "    TypeView TestClass1_1(type=test::pure::TestClass1, stereotypesVisible=\n\r" +
                          "                                                     true, attributesVisible=true,\n" +
                          "                          attributeStereotypesVisible     =    true, attributeTypesVisible=true,\n" +
                          "                                    color=#FFFFCC, lineWidth=\r" +
                          "                                                            1.0,\n" +
                          "                                    position \n\r" +
                          "                                       =(874.0, 199.46875), width   \r\n" +
                          "                                         =353.0, height=57.1875)\n" +
                          "    TypeView TestClass2_2(type=test::pure::TestClass2, stereotypesVisible=true, attributesVisible=true,\n" +
                          "                                    attributeStereotypesVisible=true, attributeTypesVisible=true,\n" +
                          "                                    color=#FFFFCC, lineWidth=1.0,\n" +
                          "                                    position=(75.0, 97.1875), width=113.0, height=57.1875)\n" +
                          "    PropertyView TestClass1_position_1(property=test::pure::TestClass1.position, stereotypesVisible=true, nameVisible=false,\n" +
                          "                                                 color=#000000, lineWidth=1.0,\n" +
                          "                                                 lineStyle=SIMPLE, points=[(132.5, 77.0), (155.2, 77.0)],\n" +
                          "                                                 label='Employment',\n" +
                          "                                                 source=TestClass1_1,\n" +
                          "                                                 target=TestClass2_2,\n" +
                          "                                                 propertyPosition=(132.5, 76.2),\n" +
                          "                                                 multiplicityPosition=(132.5, 80.0))\n" +
                          "}\n");
        CoreInstance testClass1 = this.runtime.getCoreInstance("test::pure::TestClass1");
        CoreInstance positionProp = this.processorSupport.class_findPropertyUsingGeneralization(testClass1, "position");
        Assert.assertNotNull(positionProp);

        CoreInstance diagram = this.runtime.getCoreInstance("test::pure::TestDiagram");
        CoreInstance propView = Instance.getValueForMetaPropertyToOneResolved(diagram, M3Properties.propertyViews, this.processorSupport);
        CoreInstance viewProp = Instance.getValueForMetaPropertyToOneResolved(propView, M3Properties.property, this.processorSupport);
        Assert.assertSame(positionProp, viewProp);
    }

    @Test
    public void testDiagramModelDiagram()
    {
        final String source = "###Diagram\n" +
                "Diagram meta::pure::diagram::DiagramDiagram(width=924.0, height=798.0)\n" +
                "{\n" +
                "    TypeView AbstractPathView(type=meta::pure::diagram::AbstractPathView,\n" +
                "                              stereotypesVisible=true,\n" +
                "                              attributesVisible=true,\n" +
                "                              attributeStereotypesVisible=true,\n" +
                "                              attributeTypesVisible=true,\n" +
                "                              color=#FFFFCC,\n" +
                "                              lineWidth=1.0,\n" +
                "                              position=(599.0, 278.0),\n" +
                "                              width=123.0,\n" +
                "                              height=57.1875)\n" +
                "    TypeView AssociationView(type=meta::pure::diagram::AssociationView,\n" +
                "                             stereotypesVisible=true,\n" +
                "                             attributesVisible=true,\n" +
                "                             attributeStereotypesVisible=true,\n" +
                "                             attributeTypesVisible=true,\n" +
                "                             color=#FFFFCC,\n" +
                "                             lineWidth=1.0,\n" +
                "                             position=(402.0, 476.0),\n" +
                "                             width=115.0,\n" +
                "                             height=42.09375)\n" +
                "    TypeView Diagram(type=meta::pure::diagram::Diagram,\n" +
                "                     stereotypesVisible=true,\n" +
                "                     attributesVisible=true,\n" +
                "                     attributeStereotypesVisible=true,\n" +
                "                     attributeTypesVisible=true,\n" +
                "                     color=#FFFFCC,\n" +
                "                     lineWidth=1.0,\n" +
                "                     position=(37.5, 476.0),\n" +
                "                     width=68.0,\n" +
                "                     height=42.09375)\n" +
                "    TypeView DiagramNode(type=meta::pure::diagram::DiagramNode,\n" +
                "                         stereotypesVisible=true,\n" +
                "                         attributesVisible=true,\n" +
                "                         attributeStereotypesVisible=true,\n" +
                "                         attributeTypesVisible=true,\n" +
                "                         color=#FFFFCC,\n" +
                "                         lineWidth=1.0,\n" +
                "                         position=(310.0, 124.0),\n" +
                "                         width=299.0,\n" +
                "                         height=57.1875)\n" +
                "    TypeView GeneralizationView(type=meta::pure::diagram::GeneralizationView,\n" +
                "                                stereotypesVisible=true,\n" +
                "                                attributesVisible=true,\n" +
                "                                attributeStereotypesVisible=true,\n" +
                "                                attributeTypesVisible=true,\n" +
                "                                color=#FFFFCC,\n" +
                "                                lineWidth=1.0,\n" +
                "                                position=(599.0, 476.0),\n" +
                "                                width=129.0,\n" +
                "                                height=42.09375)\n" +
                "    TypeView PropertyView(type=meta::pure::diagram::PropertyView,\n" +
                "                          stereotypesVisible=true,\n" +
                "                          attributesVisible=true,\n" +
                "                          attributeStereotypesVisible=true,\n" +
                "                          attributeTypesVisible=true,\n" +
                "                          color=#FFFFCC,\n" +
                "                          lineWidth=1.0,\n" +
                "                          position=(786.0, 476.0),\n" +
                "                          width=97.0,\n" +
                "                          height=42.09375)\n" +
                "    TypeView TypeView(type=meta::pure::diagram::TypeView,\n" +
                "                      stereotypesVisible=true,\n" +
                "                      attributesVisible=true,\n" +
                "                      attributeStereotypesVisible=true,\n" +
                "                      attributeTypesVisible=true,\n" +
                "                      color=#FFFFCC,\n" +
                "                      lineWidth=1.0,\n" +
                "                      position=(216.0, 286.0),\n" +
                "                      width=75.0,\n" +
                "                      height=42.09375)\n" +
                "    PropertyView AbstractPathView_source(property=meta::pure::diagram::AbstractPathView.source,\n" +
                "                                         stereotypesVisible=true,\n" +
                "                                         nameVisible=true,\n" +
                "                                         color=#000000,\n" +
                "                                         lineWidth=-1.0,\n" +
                "                                         lineStyle=SIMPLE,\n" +
                "                                         points=[(600.23, 320.0), (290.25, 320.0)],\n" +
                "                                         label='',\n" +
                "                                         source=AbstractPathView,\n" +
                "                                         target=TypeView,\n" +
                "                                         propertyPosition=(297.5, 320.453125),\n" +
                "                                         multiplicityPosition=(356.0, 320.453125))\n" +
                "    PropertyView AbstractPathView_target(property=meta::pure::diagram::AbstractPathView.target,\n" +
                "                                         stereotypesVisible=true,\n" +
                "                                         nameVisible=true,\n" +
                "                                         color=#000000,\n" +
                "                                         lineWidth=-1.0,\n" +
                "                                         lineStyle=SIMPLE,\n" +
                "                                         points=[(600.23, 292.0), (424.0, 292.0), (290.0, 293.0)],\n" +
                "                                         label='',\n" +
                "                                         source=AbstractPathView,\n" +
                "                                         target=TypeView,\n" +
                "                                         propertyPosition=(299.13357281145454, 278.2436741823325),\n" +
                "                                         multiplicityPosition=(358.8651741778389, 278.31183889983697))\n" +
                "    GeneralizationView AbstractPathView_DiagramNode(color=#000000,\n" +
                "                                                    lineWidth=-1.0,\n" +
                "                                                    lineStyle=SIMPLE,\n" +
                "                                                    points=[(459.5, 152.59375), (459.5, 229.59375), (660.5, 229.59375), (660.5, 306.59375)],\n" +
                "                                                    label='',\n" +
                "                                                    source=AbstractPathView,\n" +
                "                                                    target=DiagramNode)\n" +
                "    GeneralizationView AssociationView_AbstractPathView(color=#000000,\n" +
                "                                                        lineWidth=-1.0,\n" +
                "                                                        lineStyle=SIMPLE,\n" +
                "                                                        points=[(660.5, 306.59375), (660.5, 405.59375), (459.5, 405.59375), (459.5, 497.046875)],\n" +
                "                                                        label='',\n" +
                "                                                        source=AssociationView,\n" +
                "                                                        target=AbstractPathView)\n" +
                "    GeneralizationView GeneralizationView_AbstractPathView(color=#000000,\n" +
                "                                                           lineWidth=-1.0,\n" +
                "                                                           lineStyle=SIMPLE,\n" +
                "                                                           points=[(660.5, 306.59375), (660.5, 405.59375), (663.5, 405.59375), (663.5, 497.046875)],\n" +
                "                                                           label='',\n" +
                "                                                           source=GeneralizationView,\n" +
                "                                                           target=AbstractPathView)\n" +
                "    GeneralizationView PropertyView_AbstractPathView(color=#000000,\n" +
                "                                                     lineWidth=-1.0,\n" +
                "                                                     lineStyle=SIMPLE,\n" +
                "                                                     points=[(660.5, 306.59375), (660.5, 405.59375), (834.5, 405.59375), (834.5, 497.046875)],\n" +
                "                                                     label='',\n" +
                "                                                     source=PropertyView,\n" +
                "                                                     target=AbstractPathView)\n" +
                "    GeneralizationView TypeView_DiagramNode(color=#000000,\n" +
                "                                            lineWidth=-1.0,\n" +
                "                                            lineStyle=SIMPLE,\n" +
                "                                            points=[(459.5, 152.59375), (459.5, 229.59375), (253.5, 229.59375), (253.5, 307.046875)],\n" +
                "                                            label='',\n" +
                "                                            source=TypeView,\n" +
                "                                            target=DiagramNode)\n" +
                "}\n" +
                "Diagram meta::pure::diagram::DiagramDiagram1(width=924.0, height=798.0)\n" +
                "{\n" +
                "    TypeView AbstractPathView(type=meta::pure::diagram::AbstractPathView,\n" +
                "                              stereotypesVisible=true,\n" +
                "                              attributesVisible=true,\n" +
                "                              attributeStereotypesVisible=true,\n" +
                "                              attributeTypesVisible=true,\n" +
                "                              color=#FFFFCC,\n" +
                "                              lineWidth=1.0,\n" +
                "                              position=(599.0, 278.0),\n" +
                "                              width=123.0,\n" +
                "                              height=57.1875)\n" +
                "    TypeView AssociationView(type=meta::pure::diagram::AssociationView,\n" +
                "                             stereotypesVisible=true,\n" +
                "                             attributesVisible=true,\n" +
                "                             attributeStereotypesVisible=true,\n" +
                "                             attributeTypesVisible=true,\n" +
                "                             color=#FFFFCC,\n" +
                "                             lineWidth=1.0,\n" +
                "                             position=(402.0, 476.0),\n" +
                "                             width=115.0,\n" +
                "                             height=42.09375)\n" +
                "    TypeView Diagram(type=meta::pure::diagram::Diagram,\n" +
                "                     stereotypesVisible=true,\n" +
                "                     attributesVisible=true,\n" +
                "                     attributeStereotypesVisible=true,\n" +
                "                     attributeTypesVisible=true,\n" +
                "                     color=#FFFFCC,\n" +
                "                     lineWidth=1.0,\n" +
                "                     position=(37.5, 476.0),\n" +
                "                     width=68.0,\n" +
                "                     height=42.09375)\n" +
                "    TypeView DiagramNode(type=meta::pure::diagram::DiagramNode,\n" +
                "                         stereotypesVisible=true,\n" +
                "                         attributesVisible=true,\n" +
                "                         attributeStereotypesVisible=true,\n" +
                "                         attributeTypesVisible=true,\n" +
                "                         color=#FFFFCC,\n" +
                "                         lineWidth=1.0,\n" +
                "                         position=(310.0, 124.0),\n" +
                "                         width=299.0,\n" +
                "                         height=57.1875)\n" +
                "    TypeView GeneralizationView(type=meta::pure::diagram::GeneralizationView,\n" +
                "                                stereotypesVisible=true,\n" +
                "                                attributesVisible=true,\n" +
                "                                attributeStereotypesVisible=true,\n" +
                "                                attributeTypesVisible=true,\n" +
                "                                color=#FFFFCC,\n" +
                "                                lineWidth=1.0,\n" +
                "                                position=(599.0, 476.0),\n" +
                "                                width=129.0,\n" +
                "                                height=42.09375)\n" +
                "    TypeView PropertyView(type=meta::pure::diagram::PropertyView,\n" +
                "                          stereotypesVisible=true,\n" +
                "                          attributesVisible=true,\n" +
                "                          attributeStereotypesVisible=true,\n" +
                "                          attributeTypesVisible=true,\n" +
                "                          color=#FFFFCC,\n" +
                "                          lineWidth=1.0,\n" +
                "                          position=(786.0, 476.0),\n" +
                "                          width=97.0,\n" +
                "                          height=42.09375)\n" +
                "    TypeView TypeView(type=meta::pure::diagram::TypeView,\n" +
                "                      stereotypesVisible=true,\n" +
                "                      attributesVisible=true,\n" +
                "                      attributeStereotypesVisible=true,\n" +
                "                      attributeTypesVisible=true,\n" +
                "                      color=#FFFFCC,\n" +
                "                      lineWidth=1.0,\n" +
                "                      position=(216.0, 286.0),\n" +
                "                      width=75.0,\n" +
                "                      height=42.09375)\n" +
                "    PropertyView AbstractPathView_source(property=meta::pure::diagram::AbstractPathView.source,\n" +
                "                                         stereotypesVisible=true,\n" +
                "                                         nameVisible=true,\n" +
                "                                         color=#000000,\n" +
                "                                         lineWidth=-1.0,\n" +
                "                                         lineStyle=SIMPLE,\n" +
                "                                         points=[(600.23, 320.0), (290.25, 320.0)],\n" +
                "                                         label='',\n" +
                "                                         source=AbstractPathView,\n" +
                "                                         target=TypeView,\n" +
                "                                         propertyPosition=(297.5, 320.453125),\n" +
                "                                         multiplicityPosition=(356.0, 320.453125))\n" +
                "    PropertyView AbstractPathView_target(property=meta::pure::diagram::AbstractPathView.target,\n" +
                "                                         stereotypesVisible=true,\n" +
                "                                         nameVisible=true,\n" +
                "                                         color=#000000,\n" +
                "                                         lineWidth=-1.0,\n" +
                "                                         lineStyle=SIMPLE,\n" +
                "                                         points=[(600.23, 292.0), (424.0, 292.0), (290.0, 293.0)],\n" +
                "                                         label='',\n" +
                "                                         source=AbstractPathView,\n" +
                "                                         target=TypeView,\n" +
                "                                         propertyPosition=(299.13357281145454, 278.2436741823325),\n" +
                "                                         multiplicityPosition=(358.8651741778389, 278.31183889983697))\n" +
                "    GeneralizationView AbstractPathView_DiagramNode(color=#000000,\n" +
                "                                                    lineWidth=-1.0,\n" +
                "                                                    lineStyle=SIMPLE,\n" +
                "                                                    points=[(459.5, 152.59375), (459.5, 229.59375), (660.5, 229.59375), (660.5, 306.59375)],\n" +
                "                                                    label='',\n" +
                "                                                    source=AbstractPathView,\n" +
                "                                                    target=DiagramNode)\n" +
                "    GeneralizationView AssociationView_AbstractPathView(color=#000000,\n" +
                "                                                        lineWidth=-1.0,\n" +
                "                                                        lineStyle=SIMPLE,\n" +
                "                                                        points=[(660.5, 306.59375), (660.5, 405.59375), (459.5, 405.59375), (459.5, 497.046875)],\n" +
                "                                                        label='',\n" +
                "                                                        source=AssociationView,\n" +
                "                                                        target=AbstractPathView)\n" +
                "    GeneralizationView GeneralizationView_AbstractPathView(color=#000000,\n" +
                "                                                           lineWidth=-1.0,\n" +
                "                                                           lineStyle=SIMPLE,\n" +
                "                                                           points=[(660.5, 306.59375), (660.5, 405.59375), (663.5, 405.59375), (663.5, 497.046875)],\n" +
                "                                                           label='',\n" +
                "                                                           source=GeneralizationView,\n" +
                "                                                           target=AbstractPathView)\n" +
                "    GeneralizationView PropertyView_AbstractPathView(color=#000000,\n" +
                "                                                     lineWidth=-1.0,\n" +
                "                                                     lineStyle=SIMPLE,\n" +
                "                                                     points=[(660.5, 306.59375), (660.5, 405.59375), (834.5, 405.59375), (834.5, 497.046875)],\n" +
                "                                                     label='',\n" +
                "                                                     source=PropertyView,\n" +
                "                                                     target=AbstractPathView)\n" +
                "    GeneralizationView TypeView_DiagramNode(color=#000000,\n" +
                "                                            lineWidth=-1.0,\n" +
                "                                            lineStyle=SIMPLE,\n" +
                "                                            points=[(459.5, 152.59375), (459.5, 229.59375), (253.5, 229.59375), (253.5, 307.046875)],\n" +
                "                                            label='',\n" +
                "                                            source=TypeView,\n" +
                "                                            target=DiagramNode)\n" +
                "}";
        compileTestSource("testDiagram.pure",
                source);

        Class typeViewClass = (Class)this.runtime.getCoreInstance("meta::pure::diagram::TypeView");
        RichIterable<? extends ReferenceUsage> typeViewReferenceUsages = typeViewClass._referenceUsages().select(new Predicate<ReferenceUsage>()
                {
                    @Override
                    public boolean accept(ReferenceUsage usage)
                    {
                        return usage._owner() instanceof TypeView;
                    }
                });

        String[] lines = source.split("\n");
        for (ReferenceUsage referenceUsage : typeViewReferenceUsages)
        {
            SourceInformation sourceInformation = referenceUsage.getSourceInformation();
            Assert.assertEquals("TypeView", lines[sourceInformation.getLine()-1].substring(sourceInformation.getColumn() -1, sourceInformation.getColumn() + "TypeView".length() - 1));
        }
    }

    @Test
    public void testDiagramModelDiagramWithAssociationView()
    {
        final String source = "###Diagram\n" +
                "Diagram meta::pure::diagram::DiagramDiagram(width=924.0, height=798.0)\n" +
                "{\n" +
                "    TypeView AssociationView(type=meta::pure::diagram::AssociationView,\n" +
                "                             stereotypesVisible=true,\n" +
                "                             attributesVisible=true,\n" +
                "                             attributeStereotypesVisible=true,\n" +
                "                             attributeTypesVisible=true,\n" +
                "                             color=#FFFFCC,\n" +
                "                             lineWidth=1.0,\n" +
                "                             position=(402.0, 476.0),\n" +
                "                             width=115.0,\n" +
                "                             height=42.09375)\n" +
                "    TypeView Diagram(type=meta::pure::diagram::Diagram,\n" +
                "                     stereotypesVisible=true,\n" +
                "                     attributesVisible=true,\n" +
                "                     attributeStereotypesVisible=true,\n" +
                "                     attributeTypesVisible=true,\n" +
                "                     color=#FFFFCC,\n" +
                "                     lineWidth=1.0,\n" +
                "                     position=(37.5, 476.0),\n" +
                "                     width=68.0,\n" +
                "                     height=42.09375)\n" +
                "    TypeView TypeView(type=meta::pure::diagram::TypeView,\n" +
                "                      stereotypesVisible=true,\n" +
                "                      attributesVisible=true,\n" +
                "                      attributeStereotypesVisible=true,\n" +
                "                      attributeTypesVisible=true,\n" +
                "                      color=#FFFFCC,\n" +
                "                      lineWidth=1.0,\n" +
                "                      position=(216.0, 286.0),\n" +
                "                      width=75.0,\n" +
                "                      height=42.09375)\n" +
                "   AssociationView aview_1(association = meta::pure::diagram::DiagramTypeViews,\n" +
                "                            stereotypesVisible=true,\n" +
                "                            nameVisible=false,\n" +
                "                            color=#000000,\n" +
                "                            lineWidth=-1.0,\n" +
                "                            lineStyle=SIMPLE,\n" +
                "                            points=[(745.13726,491.24757),(444.11680,471.43059)],\n" +
                "                            label='DiagramTypeViews',\n" +
                "                            source=Diagram,\n" +
                "                            target=TypeView,\n" +
                "                            sourcePropertyPosition=(462.85152, 296.10994),\n" +
                "                            sourceMultiplicityPosition=(555.18941, 275.10994),\n" +
                "                            targetPropertyPosition=(450.33789, 270.70564),\n" +
                "                            targetMultiplicityPosition=(450.33789, 291.70564))\n" +
                "    AssociationView aview_2(association = meta::pure::diagram::DiagramAssociationViews,\n" +
                "                            stereotypesVisible=true,\n" +
                "                            nameVisible=false,\n" +
                "                            color=#000000,\n" +
                "                            lineWidth=-1.0,\n" +
                "                            lineStyle=SIMPLE,\n" +
                "                            points=[(745.13726,491.24757),(444.11680,471.43059)],\n" +
                "                            label='DiagramAssociationViews',\n" +
                "                            source=Diagram,\n" +
                "                            target=AssociationView,\n" +
                "                            sourcePropertyPosition=(462.85152, 296.10994),\n" +
                "                            sourceMultiplicityPosition=(555.18941, 275.10994),\n" +
                "                            targetPropertyPosition=(450.33789, 270.70564),\n" +
                "                            targetMultiplicityPosition=(450.33789, 291.70564))\n" +
                "   } ";
        compileTestSource("testDiagram.pure", source);
        String[] lines = source.split("\n");

        Association associationView = (Association)this.runtime.getCoreInstance("meta::pure::diagram::DiagramAssociationViews");
        RichIterable<? extends ReferenceUsage> associationViewReferenceUsages = associationView._referenceUsages().select(new Predicate<ReferenceUsage>()
        {
            @Override
            public boolean accept(ReferenceUsage usage)
            {
                return usage._owner() instanceof AssociationView;
            }
        });
        for (ReferenceUsage referenceUsage : associationViewReferenceUsages)
        {
            SourceInformation sourceInformation = referenceUsage.getSourceInformation();
            Assert.assertEquals("DiagramAssociationViews", lines[sourceInformation.getLine() - 1].substring(sourceInformation.getColumn() - 1, sourceInformation.getColumn() + "DiagramAssociationViews".length() - 1));
        }

        associationView = (Association)this.runtime.getCoreInstance("meta::pure::diagram::DiagramTypeViews");
        associationViewReferenceUsages = associationView._referenceUsages().select(new Predicate<ReferenceUsage>()
        {
            @Override
            public boolean accept(ReferenceUsage usage)
            {
                return usage._owner() instanceof AssociationView;
            }
        });
        for (ReferenceUsage referenceUsage : associationViewReferenceUsages)
        {
            SourceInformation sourceInformation = referenceUsage.getSourceInformation();
            Assert.assertEquals("DiagramTypeViews", lines[sourceInformation.getLine() - 1].substring(sourceInformation.getColumn() - 1, sourceInformation.getColumn() + "DiagramTypeViews".length() - 1));
        }
    }
}
