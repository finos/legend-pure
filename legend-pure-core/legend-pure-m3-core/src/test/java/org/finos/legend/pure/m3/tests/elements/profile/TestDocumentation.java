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
import org.finos.legend.pure.m3.coreinstance.meta.pure.metamodel.function.property.Property;
import org.finos.legend.pure.m3.coreinstance.meta.pure.metamodel.type.Class;
import org.finos.legend.pure.m3.coreinstance.meta.pure.metamodel.type.Enumeration;
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
 * A leading {@code '''...'''} on a declaration is sugar over the {@code meta::pure::profiles::doc}
 * {@code doc} tagged value, so every assertion here is against the tagged value it produces.
 * <p>
 * Documentation is a parser rule listed explicitly at each declaration that accepts it, not a
 * lexer-level construct. That is what keeps the same literal in expression position an ordinary
 * string, and it is the distinction the {@code declaration versus expression} group below pins.
 */
public class TestDocumentation extends AbstractPureTestWithCoreCompiledPlatform
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
    public void testDocumentationOnClass()
    {
        compile(DOC + "Class test::A\n{\n}");
        Assert.assertEquals("Documented.", doc("test::A"));
    }

    @Test
    public void testDocumentationOnClassWithStereotypesAndOtherTaggedValues()
    {
        compile("Profile test::P\n{\n  stereotypes: [s];\n  tags: [t];\n}\n" +
                DOC + "Class <<test::P.s>> {test::P.t = 'v'} test::A\n{\n}");
        Assert.assertEquals("Documented.", doc("test::A"));
    }

    @Test
    public void testDocumentationOnProperty()
    {
        compile("Class test::A\n{\n  '''\n  Given name.\n  '''\n  firstName: String[1];\n}");
        Assert.assertEquals("Given name.", docOf(firstProperty("test::A")));
    }

    @Test
    public void testDocumentationOnQualifiedProperty()
    {
        compile("Class test::A\n{\n  name: String[1];\n" +
                "  '''\n  Upper-cased name.\n  '''\n  loud() {$this.name->toUpper()}: String[1];\n}");
        AnnotatedElement qp = (AnnotatedElement) ListHelper.wrapListIterable(((Class<?>) runtime.getCoreInstance("test::A"))._qualifiedProperties()).getFirst();
        Assert.assertEquals("Upper-cased name.", docOf(qp));
    }

    @Test
    public void testDocumentationOnEnumerationAndEnumValue()
    {
        compile("'''\nA colour.\n'''\nEnum test::Colour\n{\n  '''\n  The red one.\n  '''\n  RED,\n  GREEN\n}");
        Assert.assertEquals("A colour.", doc("test::Colour"));
        @SuppressWarnings("unchecked")
        Enumeration<CoreInstance> colour = (Enumeration<CoreInstance>) runtime.getCoreInstance("test::Colour");
        Assert.assertEquals("The red one.", docOf((AnnotatedElement) ListHelper.wrapListIterable(colour._values()).getFirst()));
    }

    /** A docstring on the value after a COMMA is a distinct loop position from the first one. */
    @Test
    public void testDocumentationOnEnumValueAfterAComma()
    {
        compile("Enum test::Colour\n{\n  RED,\n  '''\n  The green one.\n  '''\n  GREEN\n}");
        @SuppressWarnings("unchecked")
        Enumeration<CoreInstance> colour = (Enumeration<CoreInstance>) runtime.getCoreInstance("test::Colour");
        Assert.assertEquals("The green one.", docOf((AnnotatedElement) ListHelper.wrapListIterable(colour._values()).getLast()));
    }

    @Test
    public void testDocumentationOnAssociation()
    {
        compile("Class test::A\n{\n}\nClass test::B\n{\n}\n" +
                "'''\nLinks A and B.\n'''\nAssociation test::AB\n{\n  a: test::A[1];\n  b: test::B[1];\n}");
        Assert.assertEquals("Links A and B.", doc("test::AB"));
    }

    @Test
    public void testDocumentationOnFunction()
    {
        compile("'''\nAdds one.\n'''\nfunction test::inc(i: Integer[1]): Integer[1]\n{\n  $i + 1\n}");
        Assert.assertEquals("Adds one.", docOf((AnnotatedElement) runtime.getCoreInstance("test::inc_Integer_1__Integer_1_")));
    }

    @Test
    public void testDocumentationOnAssociationProperty()
    {
        compile("Class test::A\n{\n}\nClass test::B\n{\n}\n" +
                "Association test::AB\n{\n  '''\n  The A end.\n  '''\n  a: test::A[1];\n  b: test::B[1];\n}");
        Property<?, ?> a = ListHelper.wrapListIterable(((Class<?>) runtime.getCoreInstance("test::B"))._propertiesFromAssociations()).getFirst();
        Assert.assertEquals("The A end.", docOf(a));
    }

    @Test
    public void testDocumentationOnMeasure()
    {
        compile("'''\nA mass.\n'''\nMeasure test::Mass\n{\n  *Gram: x -> $x;\n  Kilogram: x -> $x * 1000;\n}");
        Assert.assertEquals("A mass.", doc("test::Mass"));
    }

    @Test
    public void testDocumentationOnPrimitive()
    {
        compile("'''\nA constrained integer.\n'''\nPrimitive test::PositiveInt extends Integer");
        Assert.assertEquals("A constrained integer.", doc("test::PositiveInt"));
    }

    @Test
    public void testDocumentationOnNativeFunction()
    {
        compile("'''\nNative.\n'''\nnative function test::myNative(s: String[1]): String[1];");
        Assert.assertEquals("Native.", docOf((AnnotatedElement) runtime.getCoreInstance("test::myNative_String_1__String_1_")));
    }

    /**
     * The shape used throughout platform/pure: a stereotype and a multi-line tagged-value block sit
     * between 'native function' and the signature, so the element context spans several lines.
     */
    @Test
    public void testDocumentationOnNativeFunctionWithMultiLineStereotypeAndTagBlock()
    {
        compile("Profile test::P\n{\n  stereotypes: [func];\n  tags: [grammarDoc];\n}\n" +
                "'''\n" +
                "Summary line.\n" +
                "\n" +
                "- bullet one\n" +
                "- bullet two\n" +
                "'''\n" +
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
    public void testDocumentationOnProfile()
    {
        compile("'''\nA documented profile.\n'''\nProfile test::P\n{\n  tags: [t];\n}");
        Assert.assertEquals("A documented profile.", doc("test::P"));
    }

    // ------------------------------------------------- declaration versus expression position

    /**
     * The same literal one line into a function body is an ordinary String expression. Nothing on
     * the function is documented, and the body still evaluates to the string. This separation is
     * the whole reason documentation is a parser rule rather than a lexer channel.
     */
    @Test
    public void testMultilineStringInAFunctionBodyIsAnExpressionNotDocumentation()
    {
        compile("function test::f(): String[1]\n{\n  '''\n  not documentation\n  '''\n}");
        Assert.assertNull(docOf((AnnotatedElement) runtime.getCoreInstance("test::f__String_1_")));
    }

    /** Same again for a derived property body, where the literal is the property's return value. */
    @Test
    public void testMultilineStringInAQualifiedPropertyBodyIsAnExpressionNotDocumentation()
    {
        compile("Class test::A\n{\n  q() {'''\n  not documentation\n  '''}: String[1];\n}");
        AnnotatedElement qp = (AnnotatedElement) ListHelper.wrapListIterable(((Class<?>) runtime.getCoreInstance("test::A"))._qualifiedProperties()).getFirst();
        Assert.assertNull(docOf(qp));
    }

    /**
     * A property whose default value is a multi-line string, immediately followed by the next
     * property's documentation. The sharpest probe that the properties loop stays deterministic
     * across defaultValue, since both positions accept the same token.
     */
    @Test
    public void testMultilineStringDefaultValueFollowedByADocumentedProperty()
    {
        compile("Class test::A\n{\n  a: String[1] = '''\nv\n''';\n  '''\n  Documented.\n  '''\n  b: String[1];\n}");
        ListIterable<? extends Property<?, ?>> properties = ListHelper.wrapListIterable(((Class<?>) runtime.getCoreInstance("test::A"))._properties());
        Assert.assertNull(docOf(properties.get(0)));
        Assert.assertEquals("Documented.", docOf(properties.get(1)));
    }

    /** Loop-exit decision: the documented property is the last one before the closing brace. */
    @Test
    public void testDocumentationOnTheLastProperty()
    {
        compile("Class test::A\n{\n  a: String[1];\n  '''\n  Documented.\n  '''\n  b: String[1];\n}");
        Assert.assertEquals("Documented.", docOf(ListHelper.wrapListIterable(((Class<?>) runtime.getCoreInstance("test::A"))._properties()).getLast()));
    }

    /** A quoted property name is a STRING; documentation is a MULTILINE_STRING. Not confusable. */
    @Test
    public void testDocumentationOnAQuotedPropertyName()
    {
        compile("Class test::A\n{\n  '''\n  Documented.\n  '''\n  'first name': String[1];\n}");
        Property<?, ?> property = firstProperty("test::A");
        Assert.assertEquals("first name", property._name());
        Assert.assertEquals("Documented.", docOf(property));
    }

    @Test
    public void testTwoDocumentationLiteralsOnOneElementIsAParseError()
    {
        Assert.assertThrows(PureParserException.class, () -> compile(DOC + DOC + "Class test::A\n{\n}"));
    }

    /** Was silently ignored when documentation lived on a hidden lexer channel. */
    @Test
    public void testTrailingDocumentationAtEndOfFileIsAParseError()
    {
        Assert.assertThrows(PureParserException.class, () -> compile("Class test::A\n{\n}\n" + DOC));
    }

    @Test
    public void testDocumentationDoesNotLeakToTheFollowingElement()
    {
        compile(DOC + "Class test::A\n{\n}\nClass test::B\n{\n}");
        Assert.assertEquals("Documented.", doc("test::A"));
        Assert.assertNull(doc("test::B"));
    }

    /**
     * Attachment is syntactic, so intervening whitespace and comments are simply skipped. Both
     * cases detached silently under the hidden-channel design, which was a wart: a note written
     * between the documentation and the element made the documentation disappear with no error.
     */
    @Test
    public void testBlankLineBetweenDocumentationAndElementStillAttaches()
    {
        compile(DOC + "\n\nClass test::A\n{\n}");
        Assert.assertEquals("Documented.", doc("test::A"));
    }

    @Test
    public void testInterveningLineCommentDoesNotDetachDocumentation()
    {
        compile(DOC + "// a note\nClass test::A\n{\n}");
        Assert.assertEquals("Documented.", doc("test::A"));
    }

    /**
     * Guards the removal of the doc-comment token: a Javadoc-shaped block comment is once again
     * just a comment, and carries no meaning.
     */
    @Test
    public void testBlockCommentIsNotDocumentation()
    {
        compile("/** Just a comment. */\nClass test::A\n{\n}");
        Assert.assertNull(doc("test::A"));
    }

    // ---------------------------------------------------------------- content

    @Test
    public void testMarkdownBulletListSurvives()
    {
        compile("'''\nOptions:\n* first\n* second\n'''\nClass test::A\n{\n}");
        Assert.assertEquals("Options:\n* first\n* second", doc("test::A"));
    }

    @Test
    public void testIndentedCodeBlockKeepsRelativeIndentation()
    {
        compile("'''\n  Example:\n\n      $x->toOne()\n  '''\nClass test::A\n{\n}");
        Assert.assertEquals("Example:\n\n    $x->toOne()", doc("test::A"));
    }

    /**
     * Documentation is prose, so its content is literal. A string literal shares the layout but
     * then processes escapes, which would turn a regex or a Windows path into something else.
     */
    @Test
    public void testContentIsLiteralAndNotUnescaped()
    {
        compile("'''\na\\nb\n'''\nClass test::A\n{\n}");
        Assert.assertEquals("a\\nb", doc("test::A"));
    }

    @Test
    public void testRegexAndWindowsPathSurviveIntact()
    {
        compile("'''\nMatches \\d+ under C:\\users\n'''\nClass test::A\n{\n}");
        Assert.assertEquals("Matches \\d+ under C:\\users", doc("test::A"));
    }

    @Test
    public void testEmptyDocumentation()
    {
        compile("'''\n'''\nClass test::A\n{\n}");
        Assert.assertEquals("", doc("test::A"));
    }

    /**
     * The two spellings differ in one respect, and it is deliberate. A tagged value is a string
     * literal, so it keeps text-block semantics: a closing delimiter on its own line means a
     * trailing newline. Documentation drops its surrounding blank lines instead, so that how the
     * author lays the literal out does not change the documentation.
     */
    @Test
    public void testDocumentationDropsSurroundingBlankLinesWhereATaggedValueKeepsThem()
    {
        compile("Class {meta::pure::profiles::doc.doc = '''\nExplicit.\n'''} test::A\n{\n}\n" +
                "'''\nDocumented.\n'''\nClass test::B\n{\n}");
        Assert.assertEquals("Explicit.\n", doc("test::A"));
        Assert.assertEquals("Documented.", doc("test::B"));
    }

    // ---------------------------------------------------------------- conflict

    @Test
    public void testDocumentationAndExplicitDocTaggedValueIsAnError()
    {
        PureParserException e = Assert.assertThrows(PureParserException.class,
                () -> compile(DOC + "Class {meta::pure::profiles::doc.doc = 'From the tagged value.'} test::A\n{\n}"));
        Assert.assertTrue(e.getMessage(), e.getMessage().contains("Element has both documentation and an explicit doc.doc tagged value"));
    }

    @Test
    public void testDocumentationAndImportedDocTaggedValueIsAnError()
    {
        PureParserException e = Assert.assertThrows(PureParserException.class,
                () -> compile("import meta::pure::profiles::*;\n" + DOC +
                        "Class {doc.doc = 'From the tagged value.'} test::A\n{\n}"));
        Assert.assertTrue(e.getMessage(), e.getMessage().contains("Element has both documentation and an explicit doc.doc tagged value"));
    }

    /** Same profile, different tag - not a conflict. */
    @Test
    public void testDocTodoAlongsideDocumentationIsFine()
    {
        compile(DOC + "Class {meta::pure::profiles::doc.todo = 'later'} test::A\n{\n}");
        Assert.assertEquals("Documented.", doc("test::A"));
    }

    /** A user profile whose own name happens to be 'doc' is a different profile entirely. */
    @Test
    public void testUnrelatedProfileNamedDocIsNotAConflict()
    {
        compile("Profile test::doc\n{\n  tags: [doc];\n}\n" +
                DOC + "Class {test::doc.doc = 'unrelated'} test::A\n{\n}");
        Assert.assertEquals("Documented.", doc("test::A"));
    }

    @Test
    public void testExplicitDocTaggedValueAloneStillWorks()
    {
        compile("Class {meta::pure::profiles::doc.doc = 'Explicit.'} test::A\n{\n}");
        Assert.assertEquals("Explicit.", doc("test::A"));
    }

    @Test
    public void testUnrelatedTaggedValueAlongsideDocumentationIsFine()
    {
        compile("Profile test::P\n{\n  tags: [other];\n}\n" +
                DOC + "Class {test::P.other = 'x'} test::A\n{\n}");
        Assert.assertEquals("Documented.", doc("test::A"));
    }

    // ---------------------------------------------------------------- source information

    /**
     * The synthesized tagged value must sit inside the owning element's source range.
     * {@code ReferenceIdGenerator} only assigns a reference id to instances the element's source
     * information subsumes, so an unsubsumed tagged value is unreferenceable and breaks PAR
     * serialization of any repository that uses the feature. Documentation being a child of the
     * declaration satisfies this by construction.
     */
    @Test
    public void testDocTaggedValueIsAnchoredInsideTheElement()
    {
        compile(DOC + "Class test::A\n{\n  '''\n  A property.\n  '''\n  name: String[1];\n}");

        CoreInstance cls = runtime.getCoreInstance("test::A");
        assertSubsumed(cls, taggedValues("test::A").getFirst());
        assertSubsumed(cls, ListHelper.wrapListIterable(firstProperty("test::A")._taggedValues()).getFirst());
    }

    /** Native function is the shape the PAR serialization failure actually surfaced on. */
    @Test
    public void testDocTaggedValueOnNativeFunctionIsAnchoredInsideTheElement()
    {
        compile(DOC + "native function test::myNative(s: String[1]): String[1];");
        CoreInstance fn = runtime.getCoreInstance("test::myNative_String_1__String_1_");
        assertSubsumed(fn, ListHelper.wrapListIterable(((AnnotatedElement) fn)._taggedValues()).getFirst());
    }

    /** Exercises the association-projection anchor, a separate branch from the association body. */
    @Test
    public void testDocTaggedValueOnAnAssociationIsAnchoredInsideTheElement()
    {
        compile("Class test::A\n{\n}\nClass test::B\n{\n}\n" +
                DOC + "Association test::AB\n{\n  a: test::A[1];\n  b: test::B[1];\n}");
        CoreInstance association = runtime.getCoreInstance("test::AB");
        assertSubsumed(association, taggedValues("test::AB").getFirst());
    }

    /** Documentation alongside a surviving explicit tagged value: both anchored, still distinct. */
    @Test
    public void testDocAndExplicitTaggedValuesAreBothAnchoredInsideTheElement()
    {
        compile("Profile test::P\n{\n  stereotypes: [func];\n  tags: [grammarDoc];\n}\n" + DOC +
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

    /**
     * A documented element's source range starts at its documentation, and
     * {@code processPackageableElement} requires the stored start to equal the start of the parse
     * context on every recompile - it derives a line offset from the difference. Anchoring the
     * element to its keyword instead would make that offset negative by the height of the
     * documentation, silently corrupting every location in the element. No other test can see
     * this: the anchors involved are identical whenever an element carries no documentation.
     */
    @Test
    public void testDocumentedElementSurvivesIncrementalRecompilation()
    {
        String source = DOC + "native function test::myNative(s: String[1]): String[1];\n" +
                DOC + "Association test::AB\n{\n  a: test::A[1];\n  b: test::B[1];\n}\n" +
                "Class test::A\n{\n}\nClass test::B\n{\n}\n";
        compile(source);
        SourceInformation functionBefore = runtime.getCoreInstance("test::myNative_String_1__String_1_").getSourceInformation();
        SourceInformation associationBefore = runtime.getCoreInstance("test::AB").getSourceInformation();

        runtime.modify("/test/testSource.pure", source + "Class test::C\n{\n}\n");
        runtime.compile();

        CoreInstance function = runtime.getCoreInstance("test::myNative_String_1__String_1_");
        CoreInstance association = runtime.getCoreInstance("test::AB");
        Assert.assertEquals(functionBefore.getStartLine(), function.getSourceInformation().getStartLine());
        Assert.assertEquals(associationBefore.getStartLine(), association.getSourceInformation().getStartLine());
        assertSubsumed(function, ListHelper.wrapListIterable(((AnnotatedElement) function)._taggedValues()).getFirst());
        assertSubsumed(association, ListHelper.wrapListIterable(((AnnotatedElement) association)._taggedValues()).getFirst());
    }

    /**
     * Documentation is a real token in the parse tree now, not a comment on a hidden channel, so
     * its source information has to be right: an IDE will navigate to it. The span must cover the
     * whole literal, closing delimiter included.
     * <p>
     * The default {@code getPureSourceInformation(a, b, b)} path cannot express this. It takes the
     * end line from the end token's *start* line while taking the end column from the token's full
     * text length, which for a multi-line token names a column past the end of the first line.
     */
    @Test
    public void testDocumentationSourceInformationSpansTheWholeLiteral()
    {
        //  1: '''
        //  2: Documented.
        //  3: '''
        //  4: Class test::A
        compile(DOC + "Class test::A\n{\n}");
        assertSourceInformation(1, 1, 3, 3, taggedValues("test::A").getFirst().getSourceInformation());
    }

    @Test
    public void testIndentedDocumentationSourceInformationSpansTheWholeLiteral()
    {
        //  1: Class test::A
        //  2: {
        //  3:   '''
        //  4:   Given name.
        //  5:   '''
        //  6:   firstName: String[1];
        compile("Class test::A\n{\n  '''\n  Given name.\n  '''\n  firstName: String[1];\n}");
        assertSourceInformation(3, 3, 5, 5, ListHelper.wrapListIterable(firstProperty("test::A")._taggedValues()).getFirst().getSourceInformation());
    }

    /** The same defect, reachable on an explicit multi-line tagged value since multi-line strings landed. */
    @Test
    public void testMultilineTaggedValueSourceInformationSpansTheWholeLiteral()
    {
        //  1: Class {meta::pure::profiles::doc.doc = '''
        //  2: Documented.
        //  3: '''} test::A
        // Starts at the tag reference in column 8, not at 'Class'; ends at the closing delimiter.
        compile("Class {meta::pure::profiles::doc.doc = '''\nDocumented.\n'''} test::A\n{\n}");
        assertSourceInformation(1, 8, 3, 3, taggedValues("test::A").getFirst().getSourceInformation());
    }

    private static void assertSourceInformation(int startLine, int startColumn, int endLine, int endColumn, SourceInformation actual)
    {
        Assert.assertNotNull("no source information", actual);
        Assert.assertEquals("start line", startLine, actual.getStartLine());
        Assert.assertEquals("start column", startColumn, actual.getStartColumn());
        Assert.assertEquals("end line", endLine, actual.getEndLine());
        Assert.assertEquals("end column", endColumn, actual.getEndColumn());
    }

    private static void assertSubsumed(CoreInstance element, TaggedValue taggedValue)
    {
        SourceInformation elementInfo = element.getSourceInformation();
        SourceInformation tagInfo = taggedValue.getSourceInformation();
        Assert.assertNotNull("tagged value has no source information", tagInfo);
        Assert.assertTrue(elementInfo + " does not subsume " + tagInfo, elementInfo.subsumes(tagInfo));
    }

    // ---------------------------------------------------------------- helpers

    private static final String DOC = "'''\nDocumented.\n'''\n";

    private void compile(String code)
    {
        compileTestSource("/test/testSource.pure", code);
    }

    private Property<?, ?> firstProperty(String path)
    {
        return ListHelper.wrapListIterable(((Class<?>) runtime.getCoreInstance(path))._properties()).getFirst();
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
