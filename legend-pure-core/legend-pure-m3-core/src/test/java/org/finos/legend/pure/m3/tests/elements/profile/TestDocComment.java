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

package org.finos.legend.pure.m3.tests.elements.profile;

import org.eclipse.collections.api.RichIterable;
import org.eclipse.collections.api.factory.Lists;
import org.eclipse.collections.api.list.ListIterable;
import org.finos.legend.pure.m3.coreinstance.meta.pure.metamodel.PackageableElement;
import org.finos.legend.pure.m3.coreinstance.meta.pure.metamodel.extension.AnnotatedElement;
import org.finos.legend.pure.m3.coreinstance.meta.pure.metamodel.extension.TaggedValue;
import org.finos.legend.pure.m3.coreinstance.meta.pure.metamodel.type.Class;
import org.finos.legend.pure.m3.coreinstance.meta.pure.metamodel.type.Enumeration;
import org.finos.legend.pure.m3.coreinstance.meta.pure.metamodel.function.property.Property;
import org.finos.legend.pure.m3.serialization.filesystem.repository.CodeRepository;
import org.finos.legend.pure.m3.serialization.filesystem.repository.CodeRepositoryProviderHelper;
import org.finos.legend.pure.m3.serialization.filesystem.repository.GenericCodeRepository;
import org.finos.legend.pure.m3.serialization.filesystem.usercodestorage.classpath.ClassLoaderCodeStorage;
import org.finos.legend.pure.m3.serialization.filesystem.usercodestorage.composite.CompositeCodeStorage;
import org.finos.legend.pure.m3.tests.AbstractPureTestWithCoreCompiledPlatform;
import org.finos.legend.pure.m3.tools.ListHelper;
import org.finos.legend.pure.m4.coreinstance.CoreInstance;
import org.finos.legend.pure.m4.coreinstance.SourceInformation;
import org.finos.legend.pure.m4.serialization.grammar.antlr.PureParserException;
import org.junit.After;
import org.junit.Assert;
import org.junit.BeforeClass;
import org.junit.Test;

/**
 * Documentation comments are sugar over the {@code meta::pure::profiles::doc} {@code doc}
 * tagged value, so every assertion here is against the tagged value the comment produces.
 */
public class TestDocComment extends AbstractPureTestWithCoreCompiledPlatform
{
    @BeforeClass
    public static void setUp()
    {
        setUpRuntime(getFunctionExecution(), new CompositeCodeStorage(new ClassLoaderCodeStorage(getCodeRepositories())), getFactoryRegistryOverride(), getOptions(), getExtra());
    }

    protected static RichIterable<? extends CodeRepository> getCodeRepositories()
    {
        return Lists.immutable.with(CodeRepositoryProviderHelper.findPlatformCodeRepository(),
                GenericCodeRepository.build("system", "((meta)|(system)|(apps::pure))(::.*)?", "platform"),
                GenericCodeRepository.build("test", "test(::.*)?", "platform", "system"));
    }

    @After
    public void cleanRuntime()
    {
        runtime.delete("/test/testSource.pure");
        runtime.compile();
    }

    // ---------------------------------------------------------------- attachment targets

    @Test
    public void testDocCommentOnClass()
    {
        compile("/** A documented class. */\nClass test::A\n{\n}");
        Assert.assertEquals("A documented class.", doc("test::A"));
    }

    @Test
    public void testDocCommentOnClassWithStereotypesAndOtherTaggedValues()
    {
        compile("Profile test::P\n{\n  stereotypes: [s];\n  tags: [other];\n}\n" +
                "/** Documented. */\nClass <<test::P.s>> {test::P.other = 'x'} test::A\n{\n}");
        ListIterable<? extends TaggedValue> taggedValues = taggedValues("test::A");
        Assert.assertEquals(2, taggedValues.size());
        Assert.assertEquals("Documented.", taggedValues.get(0)._value());
        Assert.assertEquals("x", taggedValues.get(1)._value());
    }

    @Test
    public void testDocCommentOnProperty()
    {
        compile("Class test::A\n{\n  /** Given name. */\n  firstName: String[1];\n}");
        Property<?, ?> property = ListHelper.wrapListIterable(((Class<?>) runtime.getCoreInstance("test::A"))._properties()).getFirst();
        Assert.assertEquals("Given name.", docOf(property));
    }

    @Test
    public void testDocCommentOnQualifiedProperty()
    {
        compile("Class test::A\n{\n  name: String[1];\n" +
                "  /** Upper-cased name. */\n  loud() {$this.name->toUpper()}: String[1];\n}");
        AnnotatedElement qp = (AnnotatedElement) ListHelper.wrapListIterable(((Class<?>) runtime.getCoreInstance("test::A"))._qualifiedProperties()).getFirst();
        Assert.assertEquals("Upper-cased name.", docOf(qp));
    }

    @Test
    public void testDocCommentOnEnumerationAndEnumValue()
    {
        compile("/** A colour. */\nEnum test::Colour\n{\n  /** The red one. */\n  RED,\n  GREEN\n}");
        Assert.assertEquals("A colour.", doc("test::Colour"));
        @SuppressWarnings("unchecked")
        Enumeration<CoreInstance> colour = (Enumeration<CoreInstance>) runtime.getCoreInstance("test::Colour");
        Assert.assertEquals("The red one.", docOf((AnnotatedElement) ListHelper.wrapListIterable(colour._values()).getFirst()));
    }

    @Test
    public void testDocCommentOnAssociation()
    {
        compile("Class test::A\n{\n}\nClass test::B\n{\n}\n" +
                "/** Links A and B. */\nAssociation test::AB\n{\n  a: test::A[1];\n  b: test::B[1];\n}");
        Assert.assertEquals("Links A and B.", doc("test::AB"));
    }

    @Test
    public void testDocCommentOnFunction()
    {
        compile("/** Adds one. */\nfunction test::inc(i: Integer[1]): Integer[1]\n{\n  $i + 1\n}");
        Assert.assertEquals("Adds one.", docOf((AnnotatedElement) runtime.getCoreInstance("test::inc_Integer_1__Integer_1_")));
    }

    @Test
    public void testDocCommentOnAssociationProperty()
    {
        compile("Class test::A\n{\n}\nClass test::B\n{\n}\n" +
                "Association test::AB\n{\n  /** The A end. */\n  a: test::A[1];\n  b: test::B[1];\n}");
        Property<?, ?> a = ListHelper.wrapListIterable(((Class<?>) runtime.getCoreInstance("test::B"))._propertiesFromAssociations()).getFirst();
        Assert.assertEquals("The A end.", docOf(a));
    }

    @Test
    public void testDocCommentOnMeasure()
    {
        compile("/** A mass. */\nMeasure test::Mass\n{\n  *Gram: x -> $x;\n  Kilogram: x -> $x * 1000;\n}");
        Assert.assertEquals("A mass.", doc("test::Mass"));
    }

    @Test
    public void testDocCommentOnPrimitive()
    {
        compile("/** A constrained integer. */\nPrimitive test::PositiveInt extends Integer");
        Assert.assertEquals("A constrained integer.", doc("test::PositiveInt"));
    }

    @Test
    public void testDocCommentOnNativeFunction()
    {
        compile("/** Native. */\nnative function test::myNative(s: String[1]): String[1];");
        Assert.assertEquals("Native.", docOf((AnnotatedElement) runtime.getCoreInstance("test::myNative_String_1__String_1_")));
    }

    /**
     * The shape used throughout platform/pure: a stereotype and a multi-line tagged-value
     * block sit between 'native function' and the signature, so the element context spans
     * several lines. See platform/pure/grammar/functions/collection/iteration/map.pure.
     */
    @Test
    public void testDocCommentOnNativeFunctionWithMultiLineStereotypeAndTagBlock()
    {
        compile("Profile test::P\n{\n  stereotypes: [func];\n  tags: [grammarDoc];\n}\n" +
                "/**\n" +
                " * Summary line.\n" +
                " *\n" +
                " * - bullet one\n" +
                " * - bullet two\n" +
                " */\n" +
                "native function\n" +
                "    <<test::P.func>>\n" +
                "    {\n" +
                "        test::P.grammarDoc='a grammar note'\n" +
                "    }\n" +
                "    test::myDocumented(value:String[*]):String[*];");
        Assert.assertEquals("Summary line.\n\n- bullet one\n- bullet two",
                docOf((AnnotatedElement) runtime.getCoreInstance("test::myDocumented_String_MANY__String_MANY_")));
    }

    @Test
    public void testDocCommentOnProfile()
    {
        compile("/** A documented profile. */\nProfile test::P\n{\n  tags: [t];\n}");
        Assert.assertEquals("A documented profile.", doc("test::P"));
    }

    // ---------------------------------------------------------------- adjacency

    @Test
    public void testBlankLineDetachesDocComment()
    {
        compile("/** Not attached. */\n\nClass test::A\n{\n}");
        Assert.assertNull(doc("test::A"));
    }

    @Test
    public void testInterveningLineCommentDetachesDocComment()
    {
        // A line comment is not whitespace, so it breaks the "separated by whitespace" rule.
        compile("/** Not attached. */\n// a note\nClass test::A\n{\n}");
        Assert.assertNull(doc("test::A"));
    }

    @Test
    public void testPrecedingLineCommentDoesNotDetach()
    {
        compile("// note\n/** Attached. */\nClass test::A\n{\n}");
        Assert.assertEquals("Attached.", doc("test::A"));
    }

    @Test
    public void testDocCommentInsideFunctionBodyIsIgnored()
    {
        compile("function test::f(): Integer[1]\n{\n  /** not documentation */\n  1 + 1\n}");
        Assert.assertNull(docOf((AnnotatedElement) runtime.getCoreInstance("test::f__Integer_1_")));
    }

    @Test
    public void testNearestDocCommentWins()
    {
        compile("/** Ignored. */\n/** Attached. */\nClass test::A\n{\n}");
        Assert.assertEquals("Attached.", doc("test::A"));
    }

    @Test
    public void testDocCommentDoesNotLeakToTheFollowingElement()
    {
        compile("/** On A. */\nClass test::A\n{\n}\nClass test::B\n{\n}");
        Assert.assertEquals("On A.", doc("test::A"));
        Assert.assertNull(doc("test::B"));
    }

    @Test
    public void testTrailingDocCommentAtEndOfFileIsIgnored()
    {
        compile("Class test::A\n{\n}\n/** Dangling. */\n");
        Assert.assertNull(doc("test::A"));
    }

    @Test
    public void testOrdinaryBlockCommentIsNotDocumentation()
    {
        compile("/* Just a comment. */\nClass test::A\n{\n}");
        Assert.assertNull(doc("test::A"));
    }

    @Test
    public void testEmptyBlockCommentIsNotAnUnterminatedDocComment()
    {
        // Were '/**/' read as an unterminated doc comment it would swallow the rest of the file.
        compile("/**/\nClass test::A\n{\n}\nClass test::B\n{\n}");
        Assert.assertNull(doc("test::A"));
        Assert.assertNotNull(runtime.getCoreInstance("test::B"));
    }

    // ---------------------------------------------------------------- content

    @Test
    public void testMultiLineDocCommentWithStars()
    {
        compile("/**\n * A person.\n *\n * Identified by name.\n */\nClass test::A\n{\n}");
        Assert.assertEquals("A person.\n\nIdentified by name.", doc("test::A"));
    }

    @Test
    public void testMarkdownBulletListSurvives()
    {
        compile("/**\nOptions:\n* first\n* second\n*/\nClass test::A\n{\n}");
        Assert.assertEquals("Options:\n* first\n* second", doc("test::A"));
    }

    @Test
    public void testIndentedCodeBlockKeepsRelativeIndentation()
    {
        compile("/**\n  Example:\n\n      $x->toOne()\n  */\nClass test::A\n{\n}");
        Assert.assertEquals("Example:\n\n    $x->toOne()", doc("test::A"));
    }

    @Test
    public void testNestedBlockCommentDoesNotTerminateDocComment()
    {
        compile("/**\nSample:\n/* inner */\nEnd.\n*/\nClass test::A\n{\n}");
        Assert.assertEquals("Sample:\n/* inner */\nEnd.", doc("test::A"));
    }

    /** Nesting absorbs balanced pairs only, and there is no escape for a lone closing delimiter. */
    @Test
    public void testLoneClosingDelimiterInContentTerminatesTheComment()
    {
        Assert.assertThrows(PureParserException.class,
                () -> compile("/** before */ after */\nClass test::A\n{\n}"));
    }

    @Test
    public void testContentIsLiteralAndNotUnescaped()
    {
        compile("/** a\\nb */\nClass test::A\n{\n}");
        Assert.assertEquals("a\\nb", doc("test::A"));
    }

    @Test
    public void testEmptyDocComment()
    {
        compile("/** */\nClass test::A\n{\n}");
        Assert.assertEquals("", doc("test::A"));
    }

    // ---------------------------------------------------------------- conflict

    @Test
    public void testDocCommentAndExplicitDocTaggedValueIsAnError()
    {
        PureParserException e = Assert.assertThrows(PureParserException.class,
                () -> compile("/** From the comment. */\n" +
                        "Class {meta::pure::profiles::doc.doc = 'From the tagged value.'} test::A\n{\n}"));
        Assert.assertTrue(e.getMessage(), e.getMessage().contains("Element has both documentation and an explicit doc.doc tagged value"));
    }

    @Test
    public void testDocCommentAndImportedDocTaggedValueIsAnError()
    {
        PureParserException e = Assert.assertThrows(PureParserException.class,
                () -> compile("import meta::pure::profiles::*;\n" +
                        "/** From the comment. */\n" +
                        "Class {doc.doc = 'From the tagged value.'} test::A\n{\n}"));
        Assert.assertTrue(e.getMessage(), e.getMessage().contains("Element has both documentation and an explicit doc.doc tagged value"));
    }

    /** Same profile, different tag - not a conflict. */
    @Test
    public void testDocTodoAlongsideDocCommentIsFine()
    {
        compile("/** Documented. */\nClass {meta::pure::profiles::doc.todo = 'later'} test::A\n{\n}");
        Assert.assertEquals("Documented.", doc("test::A"));
    }

    /** The comment does not attach, so there is nothing to conflict with. */
    @Test
    public void testDetachedDocCommentWithExplicitDocTaggedValueIsFine()
    {
        compile("/** Not attached. */\n\nClass {meta::pure::profiles::doc.doc = 'Explicit.'} test::A\n{\n}");
        Assert.assertEquals("Explicit.", doc("test::A"));
    }

    /** A user profile whose own name happens to be 'doc' is a different profile entirely. */
    @Test
    public void testUnrelatedProfileNamedDocIsNotAConflict()
    {
        compile("Profile test::doc\n{\n  tags: [doc];\n}\n" +
                "/** Documented. */\nClass {test::doc.doc = 'unrelated'} test::A\n{\n}");
        Assert.assertEquals("Documented.", doc("test::A"));
    }

    @Test
    public void testExplicitDocTaggedValueAloneStillWorks()
    {
        compile("Class {meta::pure::profiles::doc.doc = 'Explicit.'} test::A\n{\n}");
        Assert.assertEquals("Explicit.", doc("test::A"));
    }

    @Test
    public void testUnrelatedTaggedValueAlongsideDocCommentIsFine()
    {
        compile("Profile test::P\n{\n  tags: [other];\n}\n" +
                "/** Documented. */\nClass {test::P.other = 'x'} test::A\n{\n}");
        Assert.assertEquals("Documented.", doc("test::A"));
    }

    // ---------------------------------------------------------------- serializability

    /**
     * The synthesized tagged value must sit inside the owning element's source range.
     * {@code ReferenceIdGenerator} only assigns a reference id to instances the element's source
     * information subsumes, so anchoring to the comment - which precedes the element - leaves it
     * unreferenceable and breaks PAR serialization of any repository that uses the feature.
     */
    @Test
    public void testDocTaggedValueIsAnchoredInsideTheElement()
    {
        compile("/**\n * Documented.\n */\nClass test::A\n{\n  /** A property. */\n  name: String[1];\n}");

        CoreInstance cls = runtime.getCoreInstance("test::A");
        assertSubsumed(cls, taggedValues("test::A").getFirst());

        Property<?, ?> property = ListHelper.wrapListIterable(((Class<?>) cls)._properties()).getFirst();
        assertSubsumed(cls, ListHelper.wrapListIterable(property._taggedValues()).getFirst());
    }

    /** Native function is the shape the PAR serialization failure actually surfaced on. */
    @Test
    public void testDocTaggedValueOnNativeFunctionIsAnchoredInsideTheElement()
    {
        compile("/**\n * Documented.\n */\nnative function test::myNative(s: String[1]): String[1];");
        CoreInstance fn = runtime.getCoreInstance("test::myNative_String_1__String_1_");
        assertSubsumed(fn, ListHelper.wrapListIterable(((AnnotatedElement) fn)._taggedValues()).getFirst());
    }

    /**
     * The platform/pure shape: a documentation comment alongside a surviving explicit tagged
     * value. Both must be anchored inside the element, and they must remain distinguishable.
     */
    @Test
    public void testDocAndExplicitTaggedValuesAreBothAnchoredInsideTheElement()
    {
        compile("Profile test::P\n{\n  stereotypes: [func];\n  tags: [grammarDoc];\n}\n" +
                "/**\n * Documented.\n */\n" +
                "native function\n" +
                "    <<test::P.func>>\n" +
                "    {\n" +
                "        test::P.grammarDoc='a grammar note'\n" +
                "    }\n" +
                "    test::myDocumented(value:String[*]):String[*];");

        CoreInstance fn = runtime.getCoreInstance("test::myDocumented_String_MANY__String_MANY_");
        ListIterable<? extends TaggedValue> tvs = ListHelper.wrapListIterable(((AnnotatedElement) fn)._taggedValues());
        Assert.assertEquals(2, tvs.size());
        tvs.forEach(tv -> assertSubsumed(fn, tv));
        Assert.assertNotEquals("the two tagged values must not collapse onto one source location",
                tvs.get(0).getSourceInformation(), tvs.get(1).getSourceInformation());
    }

    private static void assertSubsumed(CoreInstance element, TaggedValue taggedValue)
    {
        SourceInformation elementInfo = element.getSourceInformation();
        SourceInformation tagInfo = taggedValue.getSourceInformation();
        Assert.assertNotNull("tagged value has no source information", tagInfo);
        Assert.assertTrue(elementInfo + " does not subsume " + tagInfo, elementInfo.subsumes(tagInfo));
    }

    // ---------------------------------------------------------------- helpers

    private void compile(String code)
    {
        compileTestSource("/test/testSource.pure", code);
    }

    private ListIterable<? extends TaggedValue> taggedValues(String path)
    {
        CoreInstance element = runtime.getCoreInstance(path);
        Assert.assertNotNull("no such element: " + path, element);
        return ListHelper.wrapListIterable(((AnnotatedElement) element)._taggedValues());
    }

    private String doc(String path)
    {
        return docOf((AnnotatedElement) runtime.getCoreInstance(path));
    }

    private static String docOf(AnnotatedElement element)
    {
        Assert.assertNotNull(element);
        for (TaggedValue taggedValue : ListHelper.wrapListIterable(element._taggedValues()))
        {
            if ("doc".equals(taggedValue._tag()._value()) && "doc".equals(((PackageableElement) taggedValue._tag()._profile())._name()))
            {
                return taggedValue._value();
            }
        }
        return null;
    }
}
