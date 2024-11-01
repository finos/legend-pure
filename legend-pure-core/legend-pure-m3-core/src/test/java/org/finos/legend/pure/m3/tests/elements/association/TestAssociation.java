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

package org.finos.legend.pure.m3.tests.elements.association;

import org.finos.legend.pure.m3.tests.AbstractPureTestWithCoreCompiledPlatform;
import org.finos.legend.pure.m3.tools.test.ToFix;
import org.finos.legend.pure.m4.exception.PureCompilationException;
import org.junit.After;
import org.junit.Assert;
import org.junit.BeforeClass;
import org.junit.Ignore;
import org.junit.Test;

public class TestAssociation extends AbstractPureTestWithCoreCompiledPlatform
{
    @BeforeClass
    public static void setUp()
    {
        setUpRuntime(getExtra());
    }

    @After
    public void clearRuntime()
    {
        runtime.delete("fromString.pure");
        runtime.delete("fromString2.pure");
        runtime.delete("fromString3.pure");
        runtime.delete("fromString4.pure");
        runtime.compile();
    }

    @Test
    public void testAssociationNotEnoughProperties()
    {
        PureCompilationException e = Assert.assertThrows(PureCompilationException.class, () -> compileTestSource(
                "fromString.pure",
                "Class Product\n" +
                        "{\n" +
                        "   name : String[1];\n" +
                        "}\n" +
                        "\n" +
                        "Association ProdSyn\n" +
                        "{\n" +
                        "   product : Product[1];\n" +
                        "}\n" +
                        "\n" +
                        "Class Synonym\n" +
                        "{\n" +
                        "   name : String[1];\n" +
                        "}\n"));
        assertPureException(PureCompilationException.class, "Expected 2 properties for association 'ProdSyn', found 1", "fromString.pure", 6, 13, e);
    }

    @Test
    public void testAssociationTooManyProperties()
    {
        PureCompilationException e = Assert.assertThrows(PureCompilationException.class, () -> compileTestSource(
                "fromString.pure",
                "Class Product\n" +
                        "{\n" +
                        "   name : String[1];\n" +
                        "}\n" +
                        "\n" +
                        "Association ProdSyn\n" +
                        "{\n" +
                        "   product : Product[1];\n" +
                        "   synonyms : Synonym[*];\n" +
                        "   moreSynonyms : Synonym[*];\n" +
                        "}\n" +
                        "\n" +
                        "Class Synonym\n" +
                        "{\n" +
                        "   name : String[1];\n" +
                        "}\n"));
        assertPureException(PureCompilationException.class, "Expected 2 properties for association 'ProdSyn', found 3", "fromString.pure", 6, 13, e);
    }

    @Test
    public void testAssociationWithWrongTypes()
    {
        PureCompilationException e = Assert.assertThrows(PureCompilationException.class, () -> compileTestSource(
                "fromString.pure",
                "Class Product\n" +
                        "{\n" +
                        "   name : String[1];\n" +
                        "}\n" +
                        "\n" +
                        "Association ProdSyn\n" +
                        "{\n" +
                        "   product : Product[1];\n" +
                        "   synonyms : SynonymErr[*];\n" +
                        "}"));
        assertPureException(PureCompilationException.class, "SynonymErr has not been defined!", "fromString.pure", 9, 15, e);
    }

    @Test
    public void testAssociationWithWrongTypesInGeneric()
    {
        PureCompilationException e = Assert.assertThrows(PureCompilationException.class, () -> compileTestSource(
                "fromString.pure",
                "Class Product\n" +
                        "{\n" +
                        "   name : String[1];\n" +
                        "}\n" +
                        "\n" +
                        "Association ProdSyn\n" +
                        "{\n" +
                        "   product : Product[1];\n" +
                        "   synonyms : Synonym<Error>[*];\n" +
                        "}\n" +
                        "\n" +
                        "Class Synonym<T>\n" +
                        "{\n" +
                        "   name : T[1];\n" +
                        "}"));
        assertPureException(PureCompilationException.class, "Error has not been defined!", "fromString.pure", 9, 23, e);
    }

    @Test
    public void testAssociationWithWrongGenericTypeArgs()
    {
        PureCompilationException e = Assert.assertThrows(PureCompilationException.class, () -> compileTestSource(
                "fromString.pure",
                "Class Product\n" +
                        "{\n" +
                        "   name : String[1];\n" +
                        "}\n" +
                        "\n" +
                        "Association ProdSyn\n" +
                        "{\n" +
                        "   product : Product[1];\n" +
                        "   synonyms : Synonym[*];\n" +
                        "}\n" +
                        "\n" +
                        "Class Synonym<T>\n" +
                        "{\n" +
                        "   name : T[1];\n" +
                        "}"));
        assertPureException(PureCompilationException.class, "Type argument mismatch for the class Synonym<T> (expected 1, got 0): Synonym", "fromString.pure", 9, 15, e);
    }

    @Test
    public void testAssociationWithValidQualifiedPropertyIsProcessedWithoutError()
    {
        compileTestSource("fromString.pure", "Class Product\n" +
                "{\n" +
                "   name : String[1];\n" +
                "}\n" +
                "\n" +
                "Association ProdSyn\n" +
                "{\n" +
                "   product : Product[1];\n" +
                "   synonyms : Synonym[*];\n" +
                "   synonymsByName(st:String[1])\n" +
                "   {\n" +
                "     $this.synonyms->filter(s|$s.name == $st)->toOne()\n" +
                "   }: Synonym[1];\n" +
                "}\n" +
                "\n" +
                "Class Synonym\n" +
                "{\n" +
                "   name : String[1];\n" +
                "}");
    }

    @Test
    public void testAssociationWithInvalidQualifiedPropertySpecificationNoFilter()
    {
        PureCompilationException e = Assert.assertThrows(PureCompilationException.class, () -> compileTestSource(
                "fromString.pure",
                "Class Product\n" +
                        "{\n" +
                        "   name : String[1];\n" +
                        "}\n" +
                        "\n" +
                        "Association ProdSyn\n" +
                        "{\n" +
                        "   product : Product[1];\n" +
                        "   synonyms : Synonym[*];\n" +
                        "   newSynonym(strings:String[*])\n" +
                        "   {\n" +
                        "     ^Synonym();\n" +
                        "   }:Synonym[*];\n" +
                        "}\n" +
                        "\n" +
                        "Class Synonym\n" +
                        "{\n" +
                        "   name : String[1];\n" +
                        "}"));
        assertPureException(PureCompilationException.class, "Association Qualified Properties must follow the following pattern '$this.<<associationProperty>>->filter(p|<<lambdaExpression>>)'. Qualified property: 'newSynonym_0' in association: 'ProdSyn'  does not use the 'filter' function", "fromString.pure", 10, 4, e);
    }

    @Test
    public void testAssociationWithInvalidQualifiedPropertySpecificationMultipleExpressions()
    {
        PureCompilationException e = Assert.assertThrows(PureCompilationException.class, () -> compileTestSource(
                "fromString.pure",
                "Class Product\n" +
                        "{\n" +
                        "   name : String[1];\n" +
                        "}\n" +
                        "\n" +
                        "Association ProdSyn\n" +
                        "{\n" +
                        "   product : Product[1];\n" +
                        "   synonyms : Synonym[*];\n" +
                        "   synonymsByName(st:String[1])\n" +
                        "   {\n" +
                        "     $this.synonyms->filter(s|$s.name == $st)->toOne(); ^Synonym();\n" +
                        "   }: Synonym[1];\n" +
                        "}\n" +
                        "\n" +
                        "Class Synonym\n" +
                        "{\n" +
                        "   name : String[1];\n" +
                        "}"));
        assertPureException(PureCompilationException.class, "Association Qualified Properties must follow the following pattern '$this.<<associationProperty>>->filter(p|<<lambdaExpression>>)'. Qualified property: 'synonymsByName_0' in association: 'ProdSyn'  has more than one Expression Sequence", "fromString.pure", 10, 4, e);
    }

    @Test
    public void testAssociationWithQualifiedPropertyWithInvalidReturnType()
    {
        PureCompilationException e = Assert.assertThrows(PureCompilationException.class, () -> compileTestSource(
                "fromString.pure",
                "Class Product\n" +
                        "{\n" +
                        "   name : String[1];\n" +
                        "   orderVersions : OrderVersion[*];\n" +
                        "}\n" +
                        "\n" +
                        "Association ProdSyn\n" +
                        "{\n" +
                        "   product : Product[1];\n" +
                        "   synonyms : Synonym[*];\n" +
                        "   orderVersionById(id:String[1])\n" +
                        "   {\n" +
                        "     $this.orderVersions->filter(o|$o.id == $id)->toOne()\n" +
                        "   }: OrderVersion[1];\n" +
                        "}\n" +
                        "\n" +
                        "Class Synonym\n" +
                        "{\n" +
                        "   name : String[1];\n" +
                        "}" +
                        "Class OrderVersion\n" +
                        "{\n" +
                        "   id : String[1];\n" +
                        "}"));
        assertPureException(PureCompilationException.class, "Qualified property: 'orderVersionById_0' in association: 'ProdSyn' has returnType of : OrderVersion it should be one of Association: 'ProdSyn' properties' return types: [Synonym, Product]", "fromString.pure", 11, 4, e);
    }

    @Test
    public void testAssociationWithQualifiedPropertyReturnTypeNotConsistentWithLhsOfFilter()
    {
        PureCompilationException e = Assert.assertThrows(PureCompilationException.class, () -> compileTestSource(
                "fromString.pure",
                "Class Product\n" +
                        "{\n" +
                        "   name : String[1];\n" +
                        "}\n" +
                        "\n" +
                        "Association ProdSyn\n" +
                        "{\n" +
                        "   product : Product[1];\n" +
                        "   synonyms : Synonym[*];\n" +
                        "   synonymsByName(st:String[1])\n" +
                        "   {\n" +
                        "     $this.synonyms->filter(s|$s.name == $st)->map(s|^Product(name=''))\n" +
                        "   }: Product[*];\n" +
                        "}\n" +
                        "\n" +
                        "Class Synonym\n" +
                        "{\n" +
                        "   name : String[1];\n" +
                        "}"));
        assertPureException(PureCompilationException.class, "Qualified property: 'synonymsByName_0' in association: 'ProdSyn' should return a subset of property: 'synonyms' (left side of filter) and consequently should have a returnType of : 'Synonym'", "fromString.pure", 10, 4, e);
    }

    @Test
    public void testAssociationWithQualifiedPropertyWhichConflictsWithQualifiedPropertyOnOwningClass()
    {
        PureCompilationException e = Assert.assertThrows(PureCompilationException.class, () -> compileTestSource(
                "fromString.pure",
                "Class Product\n" +
                        "{\n" +
                        "   name : String[1];\n" +
                        "   synonymsByName(st:String[1]){$this.synonyms->filter(s | $s.name == $st)->toOne()}: Synonym[1];\n" +
                        "}\n" +
                        "\n" +
                        "Association ProdSyn\n" +
                        "{\n" +
                        "   product : Product[1];\n" +
                        "   synonyms : Synonym[*];\n" +
                        "   synonymsByName(st:String[1])\n" +
                        "   {\n" +
                        "     $this.synonyms->filter(s|$s.name == $st)->toOne();\n" +
                        "   }: Synonym[1];\n" +
                        "}\n" +
                        "\n" +
                        "Class Synonym\n" +
                        "{\n" +
                        "   name : String[1];\n" +
                        "}"));
        assertPureException(PureCompilationException.class, "Property conflict on class Product: qualified property 'synonymsByName' defined more than once", "fromString.pure", 1, 7, e);
    }


    @Test
    @Ignore
    @ToFix
    public void testAssociationWithDuplicatePropertyNames()
    {
        // TODO consider whether we want to allow this case
        PureCompilationException e = Assert.assertThrows(PureCompilationException.class, () -> compileTestSource(
                "fromString.pure",
                "Class Class1 {}\n" +
                        "Class Class2 {}\n" +
                        "Association Association12\n" +
                        "{\n" +
                        "    prop:Class1[*];\n" +
                        "    prop:Class2[0..1];\n" +
                        "}\n"));
        assertPureException(PureCompilationException.class, "Property conflict on association Association12: property 'prop' defined more than once", "testSource.pure", 3, 1, 3, 13, 7, 1, e);
    }

    @Test
    public void testAssociationWithDuplicatePropertyNamesAndTargetTypes()
    {
        PureCompilationException e = Assert.assertThrows(PureCompilationException.class, () -> compileTestSource(
                "fromString.pure",
                "Class Class1 {}\n" +
                        "Association Association12\n" +
                        "{\n" +
                        "    prop:Class1[*];\n" +
                        "    prop:Class1[0..1];\n" +
                        "}\n"));
        assertPureException(PureCompilationException.class, "Property conflict on association Association12: property 'prop' defined more than once with the same target type", "fromString.pure", 2, 1, 2, 13, 6, 1, e);
    }

    @Test
    public void testAssociationWithPropertyNameConflict()
    {
        PureCompilationException e = Assert.assertThrows(PureCompilationException.class, () -> compileTestSource(
                "fromString.pure",
                "Class Class1\n" +
                        "{\n" +
                        "  prop:Class2[*];\n" +
                        "}\n" +
                        "Class Class2 {}\n" +
                        "Association Association12\n" +
                        "{\n" +
                        "  prop:Class2[*];\n" +
                        "  prop2:Class1[1];\n" +
                        "}"));
        assertPureException(PureCompilationException.class, "Property conflict on class Class1: property 'prop' defined more than once", "fromString.pure", 1, 1, 1, 7, 4, 1, e);
    }

    @Test
    public void testAssociationWithPropertyNameConflictInOtherSource()
    {
        compileTestSource("fromString.pure",
                "Class Class1\n" +
                        "{\n" +
                        "  prop:Class2[*];\n" +
                        "}\n" +
                        "Class Class2 {}\n");
        PureCompilationException e = Assert.assertThrows(PureCompilationException.class, () -> compileTestSource(
                "fromString2.pure",
                    "Association Association12\n" +
                            "{\n" +
                            "  prop:Class2[*];\n" +
                            "  prop2:Class1[1];\n" +
                            "}"));
        assertPureException(PureCompilationException.class, "Property conflict on class Class1: property 'prop' defined more than once", "fromString.pure", 1, 1, 1, 7, 4, 1, e);
    }

    @Test
    public void testAssociationWithPropertyNameConflictFromOtherAssociation()
    {
        PureCompilationException e = Assert.assertThrows(PureCompilationException.class, () -> compileTestSource(
                "fromString.pure",
                "Class Class1\n" +
                        "{\n" +
                        "}\n" +
                        "Class Class2\n" +
                        "{\n" +
                        "}\n" +
                        "Association Association12\n" +
                        "{\n" +
                        "  prop:Class2[*];\n" +
                        "  prop2:Class1[1];\n" +
                        "}\n" +
                        "Association Association21\n" +
                        "{\n" +
                        "  prop:Class2[*];\n" +
                        "  prop3:Class1[1];\n" +
                        "}"));
        assertPureException(PureCompilationException.class, "Property conflict on class Class1: property 'prop' defined more than once", "fromString.pure", 1, 1, 1, 7, 3, 1, e);
    }

    @Test
    public void testAssociationWithNonClass()
    {
        compileTestSource("fromString.pure", "Class Class1 {}");
        PureCompilationException e1 = Assert.assertThrows(PureCompilationException.class, () -> compileTestSource(
                "fromString2.pure",
                    "Association Association1\n" +
                            "{\n" +
                            "  prop1 : Class1[1];\n" +
                            "  prop2 : String[1];\n" +
                            "}"));
        assertPureException(PureCompilationException.class, "Association 'Association1' can only be applied to Classes; 'String' is not a Class", "fromString2.pure", 1, 1, 1, 13, 5, 1, e1);

        PureCompilationException e2 = Assert.assertThrows(PureCompilationException.class, () -> compileTestSource(
                "fromString3.pure",
                    "Association Association2\n" +
                            "{\n" +
                            "  prop1 : Integer[1];\n" +
                            "  prop2 : Class1[1];\n" +
                            "}"));
        assertPureException(PureCompilationException.class, "Association 'Association2' can only be applied to Classes; 'Integer' is not a Class", "fromString3.pure", 1, 1, 1, 13, 5, 1, e2);

        PureCompilationException e3 = Assert.assertThrows(PureCompilationException.class, () -> compileTestSource(
                "fromString4.pure",
                    "Association Association3\n" +
                            "{\n" +
                            "  prop1 : Integer[1];\n" +
                            "  prop2 : Date[1];\n" +
                            "}"));
        assertPureException(PureCompilationException.class, "Association 'Association3' can only be applied to Classes; 'Date' is not a Class", "fromString4.pure", 1, 1, 1, 13, 5, 1, e3);
    }
}
