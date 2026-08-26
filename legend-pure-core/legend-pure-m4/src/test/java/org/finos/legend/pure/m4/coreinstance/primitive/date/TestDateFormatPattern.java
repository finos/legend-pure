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

package org.finos.legend.pure.m4.coreinstance.primitive.date;

import org.finos.legend.pure.m4.coreinstance.primitive.date.DateFormatPattern.Builder;
import org.finos.legend.pure.m4.coreinstance.primitive.date.DateFormatPattern.SubsecondBuilder;
import org.junit.Assert;
import org.junit.Test;

import java.time.ZoneId;
import java.util.function.Consumer;

/**
 * Tests for {@link DateFormatPattern}: the two ways of getting one, the way back to the format
 * string, and what each element does with a date.
 *
 * <p>{@link TestDateFormat} covers what a format string means, which is the same question asked
 * through {@link DateFormat#format}. These tests go at the pattern itself, so they can reach the
 * builder, the range overload, and the sub-second widths and policies that no format string spells
 * yet.
 */
public class TestDateFormatPattern
{
    private static final PureDate NANO = DateFunctions.newPureDate(2014, 3, 10, 16, 12, 35, "070004235");
    private static final PureDate MILLI = DateFunctions.newPureDate(2014, 3, 10, 16, 12, 35, "070");
    private static final PureDate ONE_MILLI = DateFunctions.newPureDate(2014, 3, 10, 16, 12, 35, "001");
    private static final PureDate HUNDREDTH = DateFunctions.newPureDate(2014, 3, 10, 16, 12, 35, "07");
    private static final PureDate ZERO = DateFunctions.newPureDate(2014, 3, 10, 16, 12, 35, "000");
    private static final PureDate SECOND = DateFunctions.newPureDate(2014, 3, 10, 16, 12, 35);
    private static final PureDate DAY = DateFunctions.newPureDate(2014, 3, 10);
    private static final PureDate YEAR = DateFunctions.newPureDate(2014);

    /**
     * Format strings covering every element, and every element more than once, for the tests that
     * hold over all of them.
     */
    private static final String[] FORMAT_STRINGS = {
            "",
            "yyyy",
            "yy",
            "yyyy-MM-dd",
            "yyyy-MM-dd\"T\"HH:mm:ss.SSSZ",
            "yyyyMMdd\"T\"HHmmssX",
            "M/d/yyyy h:mm a",
            "HH:mm:ss.S",
            "HH:mm:ss.SS",
            "HH:mm:ss.SSSS",
            "MMMM/dddd",
            "[EST]yyyy-MM-dd HH:mm:ss z Z",
            "[America/New_York]HH:mm X",
            "[+05:30]HH:mmZ",
            "\"at\" \"T\" HH",
            "\"a\\\"b\" HH",
            "yyyy-.-",
            "yyyy\t MM",
            "z",
            "yyyy-MM-dd HH:mm:ss.SSS yy/MM/dd hh:mm a z Z X",
            "SSS.S",
            "HH:mm:ss.S*",
            "HH:mm:ss.S<6",
            "HH:mm:ss.S>3",
            "HH:mm:ss.S9",
            "HH:mm:ss.S!3",
            "HH:mm:ss.S(3!,9!,\"_\")",
            "S<3.S*",
            "yyyy yyyy yy",
            "z z Z Z X X",
            "\"one\" HH \"two\" HH \"three\""
    };

    // Building a pattern

    /**
     * A pattern built element by element renders what the same format string renders, and is equal
     * to the pattern that string parses into. The builder is the same model reached another way,
     * not a second one.
     */
    @Test
    public void testTheBuilderAndTheParserAgree()
    {
        assertSameAs("yyyy-MM-dd", builder()
                .year().literal('-').month().literal('-').day());

        assertSameAs("yyyy-MM-dd\"T\"HH:mm:ss.SSSX", builder()
                .year().literal('-').month().literal('-').day()
                .literal('T')
                .hour24().literal(':').minute().literal(':').second()
                .literal('.').subsecond().atMost(3).endSubsecond()
                .iso8601TimeZoneOffset());

        assertSameAs("yy/M/d h:mm a", builder()
                .twoDigitYear().literal('/').month(1).literal('/').day(1)
                .literal(' ').hour12(1).literal(':').minute().literal(' ').amPm());

        assertSameAs("[America/New_York]yyyy-MM-dd HH:mm:ss z Z", builder()
                .timeZone("America/New_York")
                .year().literal('-').month().literal('-').day()
                .literal(' ').hour24().literal(':').minute().literal(':').second()
                .literal(' ').timeZoneName().literal(' ').rfc822TimeZoneOffset());

        assertSameAs("", builder());
    }

    /**
     * The builder writes text a format string would have to quote, or could not carry at all.
     */
    @Test
    public void testTheBuilderTakesAnyLiteralText()
    {
        Assert.assertEquals(
                "on 2014-03-10 at 16:12 (\"local\")",
                builder()
                        .literal("on ").year().literal('-').month().literal('-').day()
                        .literal(" at ").hour24().literal(':').minute()
                        .literal(" (\"local\")")
                        .build()
                        .render(NANO));

        // including the characters a format string reads as control characters
        Assert.assertEquals("yyyy=2014", builder().literal("yyyy=").year().build().render(NANO));
    }

    /**
     * A builder may be built from more than once, and each pattern it gives out stands on its own.
     */
    @Test
    public void testABuilderCanBeBuiltFromTwice()
    {
        Builder builder = builder().year().literal('-').month();
        DateFormatPattern first = builder.build();
        DateFormatPattern second = builder.literal('-').day().build();

        Assert.assertEquals("2014-03", first.render(NANO));
        Assert.assertEquals("2014-03-10", second.render(NANO));
        Assert.assertEquals("2014-03", first.render(NANO));
    }

    /**
     * The time zone belongs to the pattern rather than to a position within it, so it may be named
     * after the element that writes its name.
     */
    @Test
    public void testTheTimeZoneMayBeNamedAtAnyPoint()
    {
        Assert.assertEquals(
                "11:12 EST",
                builder()
                        .hour24().literal(':').minute().literal(' ').timeZoneName()
                        .timeZone("EST")
                        .build()
                        .render(NANO));

        // with no zone named, the general notation still answers, and says UTC
        Assert.assertEquals(
                "16:12 GMT",
                builder()
                        .hour24().literal(':').minute().literal(' ').timeZoneName()
                        .build()
                        .render(NANO));

        // naming one twice keeps the second
        Assert.assertEquals(
                "17:12 CET",
                builder()
                        .timeZone("EST")
                        .hour24().literal(':').minute().literal(' ').timeZoneName()
                        .timeZone("CET")
                        .build()
                        .render(NANO));
    }

    @Test
    public void testTheBuilderTakesAZoneIdAsWellAsAName()
    {
        DateFormatPattern pattern = builder()
                .timeZone(ZoneId.of("America/New_York"))
                .hour24().literal(':').minute()
                .build();
        Assert.assertEquals("12:12", pattern.render(NANO));
        Assert.assertEquals(ZoneId.of("America/New_York"), pattern.getTimeZone());
        Assert.assertNull(DateFormatPattern.parse("HH:mm").getTimeZone());
    }

    @Test
    public void testTheBuilderRejectsWhatItCannotBuild()
    {
        assertBuilderFails("Width must be at least 1 digit: 0", () -> builder().month(0));
        assertBuilderFails("Width must be at least 1 digit: -1", () -> builder().second(-1));
        assertBuilderFails("Unknown time zone: Europe/Lissabon", () -> builder().timeZone("Europe/Lissabon").build());
    }

    /**
     * A sub-second field reaches the pattern when it is ended and not before, and until then the
     * builder it belongs to will do nothing else at all. Ending it quietly, at the next call or at
     * {@code build}, would commit a field its author may not have finished writing, and nothing in
     * the half-written chain says which they meant.
     */
    @Test
    public void testABuilderRefusesToWorkAroundAnOpenSubsecondField()
    {
        Assert.assertEquals(
                "2014.070",
                builder().year().literal('.').subsecond().exactly(3).endSubsecond().build().render(NANO));

        assertOpenSubsecondRefuses(Builder::year);
        assertOpenSubsecondRefuses(Builder::twoDigitYear);
        assertOpenSubsecondRefuses(Builder::month);
        assertOpenSubsecondRefuses(builder -> builder.day(1));
        assertOpenSubsecondRefuses(Builder::hour24);
        assertOpenSubsecondRefuses(Builder::hour12);
        assertOpenSubsecondRefuses(Builder::amPm);
        assertOpenSubsecondRefuses(Builder::minute);
        assertOpenSubsecondRefuses(Builder::second);
        assertOpenSubsecondRefuses(builder -> builder.literal('x'));
        assertOpenSubsecondRefuses(builder -> builder.literal("x"));
        assertOpenSubsecondRefuses(builder -> builder.timeZone("EST"));
        assertOpenSubsecondRefuses(builder -> builder.timeZone(ZoneId.of("America/New_York")));
        assertOpenSubsecondRefuses(Builder::timeZoneName);
        assertOpenSubsecondRefuses(Builder::rfc822TimeZoneOffset);
        assertOpenSubsecondRefuses(Builder::iso8601TimeZoneOffset);
        assertOpenSubsecondRefuses(Builder::subsecond);
        assertOpenSubsecondRefuses(Builder::build);
    }

    /**
     * A field is finished with once it has been ended, so it can neither be told anything more nor
     * added to the pattern a second time.
     */
    @Test
    public void testASubsecondFieldCannotBeUsedAfterItIsEnded()
    {
        assertEndedSubsecondRefuses(SubsecondBuilder::asStored);
        assertEndedSubsecondRefuses(field -> field.exactly(6));
        assertEndedSubsecondRefuses(field -> field.atMost(6));
        assertEndedSubsecondRefuses(field -> field.atLeast(6));
        assertEndedSubsecondRefuses(field -> field.between(2, 6));
        assertEndedSubsecondRefuses(SubsecondBuilder::failBelowMinimum);
        assertEndedSubsecondRefuses(SubsecondBuilder::failAboveMaximum);
        assertEndedSubsecondRefuses(field -> field.padWith('_'));
        assertEndedSubsecondRefuses(SubsecondBuilder::endSubsecond);

        // and the pattern holds the one field the one time it was ended
        Builder builder = builder().year();
        SubsecondBuilder field = builder.subsecond().exactly(3);
        field.endSubsecond();
        Assert.assertEquals("2014070", builder.build().render(NANO));
    }

    // Elements appearing more than once

    /**
     * Everything but the time zone name may be asked for as often as a pattern likes, and each
     * asking is answered on its own terms. The time zone itself is the one thing a pattern has only
     * one of, since it is a property of the pattern rather than of a position in it.
     */
    @Test
    public void testAnElementMayAppearMoreThanOnce()
    {
        Assert.assertEquals(
                "2014-03-10 16:12:35.070 14/03/10 04:12 PM GMT +0000 Z",
                DateFormat.format(new StringBuilder(), "yyyy-MM-dd HH:mm:ss.SSS yy/MM/dd hh:mm a z Z X", NANO).toString());

        // and in a zone, where each asking sees the same shifted reading
        Assert.assertEquals(
                "2014-03-10 11:12:35.070 14/03/10 11:12 AM EST -0500 -05",
                DateFormat.format(new StringBuilder(), "[EST]yyyy-MM-dd HH:mm:ss.SSS yy/MM/dd hh:mm a z Z X", NANO).toString());

        // sub-second fields of different widths, side by side
        Assert.assertEquals("070.0", DateFormat.format(new StringBuilder(), "SSS.S", NANO).toString());
        Assert.assertEquals("07.070004235", DateFormat.format(new StringBuilder(), "SS.SSSS", NANO).toString());
    }

    /**
     * The same holds of a pattern the builder made, where the sub-second fields need not agree on
     * anything at all.
     */
    @Test
    public void testTheBuilderTakesAnElementMoreThanOnce()
    {
        Assert.assertEquals(
                "070|070004235|07",
                builder()
                        .subsecond().exactly(3).endSubsecond()
                        .literal('|')
                        .subsecond().asStored().endSubsecond()
                        .literal('|')
                        .subsecond().atMost(2).endSubsecond()
                        .build()
                        .render(NANO));

        // one field failing is the whole pattern failing, wherever it sits
        DateFormatPattern pattern = builder()
                .subsecond().atMost(3).endSubsecond()
                .literal('|')
                .subsecond().exactly(3).failBelowMinimum().endSubsecond()
                .build();
        Assert.assertTrue(pattern.canRender(MILLI));
        Assert.assertFalse(pattern.canRender(HUNDREDTH));

        // and the time zone name may be written as often as a pattern likes, however late the zone
        // it names is settled
        Assert.assertEquals(
                "EST 11:12 EST",
                builder()
                        .timeZoneName().literal(' ').hour24().literal(':').minute().literal(' ').timeZoneName()
                        .timeZone("EST")
                        .build()
                        .render(NANO));
    }

    // Parsing a portion of a string

    /**
     * The overload taking a range parses a format string out of the middle of a larger one, which
     * is how the payload of a {@code %t{...}} specifier is read without first cutting it out.
     */
    @Test
    public void testParseWithinALargerString()
    {
        String string = "on %t{yyyy-MM-dd} at %t{HH:mm}";
        Assert.assertEquals("2014-03-10", DateFormatPattern.parse(string, 6, 16).render(NANO));
        Assert.assertEquals("16:12", DateFormatPattern.parse(string, 24, 29).render(NANO));

        // the range decides what is parsed, not the string
        Assert.assertEquals("2014", DateFormatPattern.parse("yyyy-MM-dd", 0, 4).render(NANO));
        Assert.assertEquals("03-10", DateFormatPattern.parse("yyyy-MM-dd", 5, 10).render(NANO));
        Assert.assertEquals("", DateFormatPattern.parse("yyyy", 2, 2).render(NANO));

        // the whole string and its full range are the same thing
        Assert.assertEquals(DateFormatPattern.parse("yyyy-MM-dd"), DateFormatPattern.parse("yyyy-MM-dd", 0, 10));
    }

    /**
     * A time zone opens the range it is parsed from, not the string that range came out of, and the
     * error names the range rather than the string around it.
     */
    @Test
    public void testParseWithinALargerStringPositionsAndReportsAgainstTheRange()
    {
        String string = "xx[EST]yyyy-MM-ddxx";
        Assert.assertEquals("2014-03-10", DateFormatPattern.parse(string, 2, 17).render(DAY));

        // the same zone one character further in is no longer at the start of the format string
        Assert.assertEquals(
                "Time zone can only be set at the beginning of the format string",
                Assert.assertThrows(IllegalArgumentException.class, () -> DateFormatPattern.parse("y[EST]yyyy", 0, 10)).getMessage());

        // and an error quotes what it was given to parse
        Assert.assertEquals(
                "Invalid format control character 'Q' in format string: MM-Q",
                Assert.assertThrows(IllegalArgumentException.class, () -> DateFormatPattern.parse("yyyy-MM-Q-dd", 5, 9)).getMessage());
    }

    @Test
    public void testFormatWithinALargerString()
    {
        String string = "on %t{yyyy-MM-dd} at %t{HH:mm}";
        Assert.assertEquals("2014-03-10", DateFormat.format(new StringBuilder(), string, 6, 16, NANO).toString());
        Assert.assertEquals("16:12", DateFormat.format(new StringBuilder(), string, 24, 29, NANO).toString());

        StringBuilder builder = new StringBuilder("date: ");
        Assert.assertSame(builder, DateFormat.format(builder, string, 6, 16, NANO));
        Assert.assertEquals("date: 2014-03-10", builder.toString());
    }

    // Validation

    /**
     * Validation asks only what the format string is, so it takes no date and passes anything a
     * date of the right granularity could be written by.
     */
    @Test
    public void testValidate()
    {
        for (String formatString : FORMAT_STRINGS)
        {
            DateFormat.validate(formatString);
        }
        DateFormat.validate("on %t{yyyy-MM-dd} at", 6, 16);

        assertValidationFails("Invalid format control character 'Q' in format string: yyyy-Q", "yyyy-Q");
        assertValidationFails("Missing closing quote in format string: \"abc", "\"abc");
        assertValidationFails("Missing closing bracket in format string: [EST", "[EST");
        assertValidationFails("Missing closing quotes in time zone definition: [\"EST]yyyy", "[\"EST]yyyy");
        assertValidationFails("Time zone can only be set at the beginning of the format string", "yyyy[EST]");
        assertValidationFails("Unknown time zone: Europe/Lissabon", "[Europe/Lissabon]yyyy");
    }

    /**
     * A structural error is found before anything is written, so a caller passing its own appendable
     * gets it back untouched. A component the date does not have is not structural, and is found
     * where it is reached.
     */
    @Test
    public void testAStructuralErrorLeavesTheAppendableAlone()
    {
        StringBuilder builder = new StringBuilder("date: ");
        Assert.assertThrows(IllegalArgumentException.class, () -> DateFormat.format(builder, "yyyy-Q", NANO));
        Assert.assertEquals("date: ", builder.toString());

        Assert.assertThrows(IllegalArgumentException.class, () -> DateFormat.format(builder, "yyyy-MM", YEAR));
        Assert.assertEquals("date: 2014-", builder.toString());
    }

    // Writing the format string back out

    /**
     * A pattern writes the format string it stands for, and reading that back gives an equal
     * pattern. Writing it again gives the same string, so the spelling a pattern chooses is settled
     * rather than merely one of several.
     */
    @Test
    public void testPatternRoundTrips()
    {
        for (String formatString : FORMAT_STRINGS)
        {
            DateFormatPattern pattern = DateFormatPattern.parse(formatString);
            String written = pattern.pattern();
            Assert.assertEquals(formatString, pattern, DateFormatPattern.parse(written));
            Assert.assertEquals(formatString, written, DateFormatPattern.parse(written).pattern());
        }
    }

    /**
     * Where a format string can be written in more than one way, the pattern picks one, so a
     * spelling that says nothing extra comes back in its plainest form.
     */
    @Test
    public void testPatternNormalizesWhatSaysTheSameThing()
    {
        Assert.assertEquals("yyyy-MM-dd", DateFormatPattern.parse("yyyy-MM-dd").pattern());

        // a year of three letters or fewer is the two digit form, and four or more is the whole one
        Assert.assertEquals("yy", DateFormatPattern.parse("y").pattern());
        Assert.assertEquals("yy", DateFormatPattern.parse("yyy").pattern());
        Assert.assertEquals("yyyy", DateFormatPattern.parse("yyyyy").pattern());

        // a repeated character with no width to widen writes itself once
        Assert.assertEquals("a", DateFormatPattern.parse("aaa").pattern());
        Assert.assertEquals("z", DateFormatPattern.parse("zzz").pattern());
        Assert.assertEquals("Z", DateFormatPattern.parse("ZZ").pattern());
        Assert.assertEquals("X", DateFormatPattern.parse("XX").pattern());

        // a separator stands for itself, quoted or not, and adjacent text is one run
        Assert.assertEquals("-", DateFormatPattern.parse("\"-\"").pattern());
        Assert.assertEquals("\"at\" \"T\" HH", DateFormatPattern.parse("\"at\" \"T\" HH").pattern());
        Assert.assertEquals("\"abc\"", builder().literal('a').literal("b").literal('c').build().pattern());
    }

    /**
     * A time zone is written back into the brackets a format string opens with, escaping whatever
     * would otherwise close them, and a quoted name comes back unquoted.
     */
    @Test
    public void testPatternWritesTheTimeZoneBack()
    {
        Assert.assertEquals("[EST]HH:mm z", DateFormatPattern.parse("[EST]HH:mm z").pattern());
        Assert.assertEquals("[EST]HH:mm z", DateFormatPattern.parse("[\"EST\"]HH:mm z").pattern());
        Assert.assertEquals("[EST]HH:mm z", DateFormatPattern.parse("[E\\ST]HH:mm z").pattern());
        Assert.assertEquals("[+05:30]HH:mm", DateFormatPattern.parse("[+05:30]HH:mm").pattern());
        Assert.assertEquals("[America/New_York]X", DateFormatPattern.parse("[America/New_York]X").pattern());
    }

    /**
     * A sub-second field a shorthand covers exactly writes itself that way, and everything else
     * writes the general form. The repetition form is never written: every field it can spell has a
     * shorthand meaning precisely the same thing, so a pattern read from a legacy string writes the
     * modern spelling of what it already meant.
     */
    @Test
    public void testPatternWritesSubsecondFields()
    {
        Assert.assertEquals("S*", builder().subsecond().asStored().endSubsecond().build().pattern());
        Assert.assertEquals("S*", builder().subsecond().endSubsecond().build().pattern());
        Assert.assertEquals("S<1", builder().subsecond().atMost(1).endSubsecond().build().pattern());
        Assert.assertEquals("S<3", builder().subsecond().atMost(3).endSubsecond().build().pattern());
        Assert.assertEquals("S<6", builder().subsecond().atMost(6).endSubsecond().build().pattern());
        Assert.assertEquals("S>3", builder().subsecond().atLeast(3).endSubsecond().build().pattern());
        Assert.assertEquals("S3", builder().subsecond().exactly(3).endSubsecond().build().pattern());
        Assert.assertEquals("S!3", builder().subsecond().exactly(3).failBelowMinimum().endSubsecond().build().pattern());

        // and the general form carries what no shorthand can say
        Assert.assertEquals("S(2,4)", builder().subsecond().between(2, 4).endSubsecond().build().pattern());
        Assert.assertEquals("S(0,3!)", builder().subsecond().atMost(3).failAboveMaximum().endSubsecond().build().pattern());
        Assert.assertEquals("S(3!,*)", builder().subsecond().atLeast(3).failBelowMinimum().endSubsecond().build().pattern());
        Assert.assertEquals("S(4,4,\"_\")", builder().subsecond().exactly(4).padWith('_').endSubsecond().build().pattern());
        Assert.assertEquals(
                "S(3!,9!,\"_\")",
                builder().subsecond().between(3, 9).failBelowMinimum().failAboveMaximum().padWith('_').endSubsecond().build().pattern());

        // a marker with nothing to act on is kept rather than erased, so the field says what it was
        // asked for even where a shorter spelling would render the same
        Assert.assertEquals("S(0!,3)", builder().subsecond().atMost(3).failBelowMinimum().endSubsecond().build().pattern());
        Assert.assertEquals("S(0,*!)", builder().subsecond().asStored().failAboveMaximum().endSubsecond().build().pattern());
        Assert.assertEquals("S(0,3,\"_\")", builder().subsecond().atMost(3).padWith('_').endSubsecond().build().pattern());
    }

    @Test
    public void testAppendPatternReturnsTheAppendableItWasGiven()
    {
        StringBuilder builder = new StringBuilder("format: ");
        Assert.assertSame(builder, DateFormatPattern.parse("yyyy-MM-dd").appendPattern(builder));
        Assert.assertEquals("format: yyyy-MM-dd", builder.toString());
    }

    // canRender

    /**
     * A pattern can write a date exactly when every element of it can, which is exactly when
     * rendering succeeds.
     */
    @Test
    public void testCanRender()
    {
        assertCanRender("yyyy", true, true, true, true);
        assertCanRender("yyyy-MM", false, true, true, true);
        assertCanRender("yyyy-MM-dd", false, true, true, true);
        assertCanRender("yyyy-MM-dd HH:mm:ss", false, false, true, true);
        assertCanRender("HH:mm:ss.SSS", false, false, false, true);
        assertCanRender("a", false, false, true, true);
        assertCanRender("Z", false, false, true, true);
        assertCanRender("X", false, false, true, true);

        // a literal, and the general time zone notation, ask the date for nothing
        assertCanRender("\"any date at all\"", true, true, true, true);
        assertCanRender("[EST]z", true, true, true, true);
    }

    // Sub-second widths and policies

    /**
     * The widths a format string spells today: at most N digits for one to three letters, and
     * however many the date has from four on. Neither pads.
     */
    @Test
    public void testSubsecondWidthsAFormatStringSpells()
    {
        assertSubsecond(builder().subsecond().atMost(1).endSubsecond(), "0", "0", "0", "0");
        assertSubsecond(builder().subsecond().atMost(2).endSubsecond(), "07", "07", "07", "00");
        assertSubsecond(builder().subsecond().atMost(3).endSubsecond(), "070", "070", "07", "000");
        assertSubsecond(builder().subsecond().asStored().endSubsecond(), "070004235", "070", "07", "000");

        // which is what the parser builds them into
        Assert.assertEquals(builder().subsecond().atMost(1).endSubsecond().build(), DateFormatPattern.parse("S"));
        Assert.assertEquals(builder().subsecond().atMost(3).endSubsecond().build(), DateFormatPattern.parse("SSS"));
        Assert.assertEquals(builder().subsecond().asStored().endSubsecond().build(), DateFormatPattern.parse("SSSS"));
        Assert.assertEquals(builder().subsecond().asStored().endSubsecond().build(), DateFormatPattern.parse("SSSSSSSSS"));
    }

    /**
     * A minimum pads a fraction the date has too few digits for. Padding narrows the span of time
     * the date stands for, so it claims a precision the date does not have, which is why it has to
     * be asked for rather than being what a width means.
     */
    @Test
    public void testSubsecondMinimum()
    {
        assertSubsecond(builder().subsecond().exactly(3).endSubsecond(), "070", "070", "070", "000");
        assertSubsecond(builder().subsecond().exactly(9).endSubsecond(), "070004235", "070000000", "070000000", "000000000");
        assertSubsecond(builder().subsecond().atLeast(3).endSubsecond(), "070004235", "070", "070", "000");
        assertSubsecond(builder().subsecond().between(2, 4).endSubsecond(), "0700", "070", "07", "000");

        // the fill need not be a zero
        assertSubsecond(builder().subsecond().exactly(4).padWith('_').endSubsecond(), "0700", "070_", "07__", "000_");
    }

    /**
     * A field may refuse a fraction outside its width rather than making one fit. Refusing below
     * the minimum is refusing to invent precision; refusing above the maximum is refusing to be
     * given more than was asked for.
     */
    @Test
    public void testSubsecondPolicies()
    {
        assertSubsecond(builder().subsecond().exactly(3).failBelowMinimum().endSubsecond(), "070", "070", null, "000");
        assertSubsecond(builder().subsecond().atMost(3).failAboveMaximum().endSubsecond(), null, "070", "07", "000");
        assertSubsecond(builder().subsecond().exactly(3).failBelowMinimum().failAboveMaximum().endSubsecond(), null, "070", null, "000");
        assertSubsecond(builder().subsecond().atLeast(3).failBelowMinimum().endSubsecond(), "070004235", "070", null, "000");

        Assert.assertEquals(
                "Date has a 2 digit sub-second, but 3 are required: 2014-03-10T16:12:35.07+0000",
                Assert.assertThrows(
                        IllegalArgumentException.class,
                        () -> builder().subsecond().exactly(3).failBelowMinimum().endSubsecond().build().render(HUNDREDTH)).getMessage());
        Assert.assertEquals(
                "Date has a 9 digit sub-second, but at most 3 may be written: 2014-03-10T16:12:35.070004235+0000",
                Assert.assertThrows(
                        IllegalArgumentException.class,
                        () -> builder().subsecond().atMost(3).failAboveMaximum().endSubsecond().build().render(NANO)).getMessage());
    }

    /**
     * Whether a sub-second field can write a date is the same question rendering it asks, including
     * where the answer turns on how many digits the date has rather than on whether it has any.
     */
    @Test
    public void testSubsecondCanRender()
    {
        Assert.assertFalse(builder().subsecond().endSubsecond().build().canRender(SECOND));
        Assert.assertFalse(builder().subsecond().exactly(3).endSubsecond().build().canRender(DAY));
        Assert.assertTrue(builder().subsecond().endSubsecond().build().canRender(HUNDREDTH));

        Assert.assertTrue(builder().subsecond().exactly(3).endSubsecond().build().canRender(HUNDREDTH));
        Assert.assertFalse(builder().subsecond().exactly(3).failBelowMinimum().endSubsecond().build().canRender(HUNDREDTH));
        Assert.assertTrue(builder().subsecond().exactly(3).failBelowMinimum().endSubsecond().build().canRender(MILLI));

        Assert.assertTrue(builder().subsecond().atMost(3).endSubsecond().build().canRender(NANO));
        Assert.assertFalse(builder().subsecond().atMost(3).failAboveMaximum().endSubsecond().build().canRender(NANO));
        Assert.assertTrue(builder().subsecond().atMost(3).failAboveMaximum().endSubsecond().build().canRender(MILLI));
    }

    /**
     * A width nothing satisfies is rejected where it is asked for, which is the only thing a
     * sub-second field is rejected for: the policies and the fill are markers rather than
     * requirements, and cannot make a field that means nothing.
     */
    @Test
    public void testSubsecondRejectsWidthsNothingSatisfies()
    {
        assertBuilderFails("Sub-second minimum 5 exceeds maximum 3", () -> builder().subsecond().between(5, 3));
        assertBuilderFails("Sub-second maximum must be at least 1: 0", () -> builder().subsecond().between(3, 0));
        assertBuilderFails("Sub-second maximum must be at least 1: 0", () -> builder().subsecond().atMost(0));
        assertBuilderFails("Sub-second minimum may not be negative: -1", () -> builder().subsecond().atLeast(-1));

        // and the last width asked for is the one the field takes
        Assert.assertEquals("S(2,4)", builder().subsecond().exactly(9).between(2, 4).endSubsecond().build().pattern());
    }

    // The sub-second syntax

    /**
     * Each shorthand is the field the builder describes the same way, reached through the format
     * string rather than through Java.
     */
    @Test
    public void testTheSubsecondShorthands()
    {
        assertSameAs("S*", builder().subsecond().asStored().endSubsecond());
        assertSameAs("S<3", builder().subsecond().atMost(3).endSubsecond());
        assertSameAs("S>3", builder().subsecond().atLeast(3).endSubsecond());
        assertSameAs("S3", builder().subsecond().exactly(3).endSubsecond());
        assertSameAs("S!3", builder().subsecond().exactly(3).failBelowMinimum().endSubsecond());

        // a width is a number rather than a digit, and a field is an element like any other
        assertSameAs("S12", builder().subsecond().exactly(12).endSubsecond());
        assertSameAs("S<10", builder().subsecond().atMost(10).endSubsecond());
        assertSameAs("HH:mm:ss.S<6", builder()
                .hour24().literal(':').minute().literal(':').second()
                .literal('.').subsecond().atMost(6).endSubsecond());
        assertSameAs("S<3.S*", builder()
                .subsecond().atMost(3).endSubsecond()
                .literal('.').subsecond().asStored().endSubsecond());
    }

    /**
     * The general form says both bounds, both policies, and the fill at once, which is the whole of
     * the model and the nine fields it makes.
     */
    @Test
    public void testTheSubsecondGeneralForm()
    {
        assertSameAs("S(0,*)", builder().subsecond().asStored().endSubsecond());
        assertSameAs("S(0,3)", builder().subsecond().atMost(3).endSubsecond());
        assertSameAs("S(0,3!)", builder().subsecond().atMost(3).failAboveMaximum().endSubsecond());
        assertSameAs("S(3,*)", builder().subsecond().atLeast(3).endSubsecond());
        assertSameAs("S(3!,*)", builder().subsecond().atLeast(3).failBelowMinimum().endSubsecond());
        assertSameAs("S(3,9)", builder().subsecond().between(3, 9).endSubsecond());
        assertSameAs("S(3,9!)", builder().subsecond().between(3, 9).failAboveMaximum().endSubsecond());
        assertSameAs("S(3!,9)", builder().subsecond().between(3, 9).failBelowMinimum().endSubsecond());
        assertSameAs("S(3!,9!)", builder().subsecond().between(3, 9).failBelowMinimum().failAboveMaximum().endSubsecond());

        // a marker with nothing to act on is legal and does nothing, since a format string may be
        // built at run time and its validity must not turn on how it was written
        assertSameAs("S(1!,3)", builder().subsecond().between(1, 3).failBelowMinimum().endSubsecond());
        assertSameAs("S(3,*!)", builder().subsecond().atLeast(3).failAboveMaximum().endSubsecond());
        assertSameAs("S(0,3,\"_\")", builder().subsecond().atMost(3).padWith('_').endSubsecond());
    }

    /**
     * The fill is one character, which a backslash escapes, so every character can be a fill -
     * including the quote that ends it and the parenthesis that ends the field.
     */
    @Test
    public void testTheSubsecondFill()
    {
        assertSameAs("S(3,3,\"_\")", builder().subsecond().exactly(3).padWith('_').endSubsecond());
        assertSameAs("S(3,3,\" \")", builder().subsecond().exactly(3).padWith(' ').endSubsecond());
        assertSameAs("S(3,3,\")\")", builder().subsecond().exactly(3).padWith(')').endSubsecond());
        assertSameAs("S(3,3,\",\")", builder().subsecond().exactly(3).padWith(',').endSubsecond());
        assertSameAs("S(3,3,\"\\\"\")", builder().subsecond().exactly(3).padWith('"').endSubsecond());
        assertSameAs("S(3,3,\"\\\\\")", builder().subsecond().exactly(3).padWith('\\').endSubsecond());

        Assert.assertEquals("07_", DateFormatPattern.parse("S(3,3,\"_\")").render(HUNDREDTH));
        Assert.assertEquals("07\"", DateFormatPattern.parse("S(3,3,\"\\\"\")").render(HUNDREDTH));

        // an escape is only needed for the two characters that would otherwise end it
        Assert.assertEquals("07_", DateFormatPattern.parse("S(3,3,\"\\_\")").render(HUNDREDTH));
    }

    /**
     * Every field of the worked table in the design, against the fractions it works. The last column
     * of that table is left out because it is uniform: no field decides for itself what a date with
     * no fraction at all looks like, which {@link #assertWorkedRow} asserts of every row.
     */
    @Test
    public void testEverySubsecondFieldOfTheWorkedTable()
    {
        assertWorkedRow("S3", "070", "001", "070");
        assertWorkedRow("S9", "070004235", "001000000", "070000000");
        assertWorkedRow("S!3", "070", "001", null);
        assertWorkedRow("S<3", "070", "001", "07");
        assertWorkedRow("S>3", "070004235", "001", "070");
        assertWorkedRow("S*", "070004235", "001", "07");
        assertWorkedRow("S(0,3!)", null, "001", "07");
        assertWorkedRow("S(3!,*)", "070004235", "001", null);
        assertWorkedRow("S(3,3!)", null, "001", "070");
        assertWorkedRow("S(3!,3!)", null, "001", null);
        assertWorkedRow("SSS", "070", "001", "07");
        assertWorkedRow("SSSSSSSSS", "070004235", "001", "07");
    }

    /**
     * Every form the repetition spelling can take has a shorthand that means precisely the same
     * thing, so the older form can be deprecated without anything losing a way to be said. That
     * equivalence is also what makes {@code pattern()} a migration: it writes the modern spelling of
     * a field it read from a legacy one.
     */
    @Test
    public void testEveryLegacySubsecondFormHasAModernSpellingThatMeansTheSame()
    {
        assertSameField("S", "S<1");
        assertSameField("SS", "S<2");
        assertSameField("SSS", "S<3");
        assertSameField("SSSS", "S*");
        assertSameField("SSSSS", "S*");
        assertSameField("SSSSSSSSS", "S*");

        // the discontinuity the modern spellings make visible: the count stops meaning a width at
        // four letters, where S<3 turns into S*
        Assert.assertNotEquals(DateFormatPattern.parse("SSS"), DateFormatPattern.parse("SSSS"));

        // and the hazard the design names: S3 is exactly three digits where SSS is at most three
        Assert.assertNotEquals(DateFormatPattern.parse("S3"), DateFormatPattern.parse("SSS"));
        Assert.assertEquals("070", DateFormatPattern.parse("S3").render(HUNDREDTH));
        Assert.assertEquals("07", DateFormatPattern.parse("SSS").render(HUNDREDTH));
    }

    /**
     * A sub-second field is rejected where it is incoherent or ambiguous, and nowhere else.
     */
    @Test
    public void testSubsecondSyntaxErrors()
    {
        assertValidationFails("Expected a digit count after 'S<' in format string: S<", "S<");
        assertValidationFails("Expected a digit count after 'S>' in format string: S>", "S>");
        assertValidationFails("Expected a digit count after 'S!' in format string: S!", "S!");
        assertValidationFails("Expected a digit count after 'S!' in format string: S!x", "S!x");
        assertValidationFails("Expected a digit count after 'S(' in format string: S(", "S(");
        assertValidationFails("Expected a digit count after 'S(' in format string: S(-1,3)", "S(-1,3)");
        assertValidationFails("Expected ',' after the sub-second minimum in format string: S(3", "S(3");
        assertValidationFails("Expected ',' after the sub-second minimum in format string: S(3)", "S(3)");
        assertValidationFails("Expected a digit count or '*' for the sub-second maximum in format string: S(3,", "S(3,");
        assertValidationFails("Expected a digit count or '*' for the sub-second maximum in format string: S(3,x)", "S(3,x)");
        assertValidationFails("Missing closing parenthesis in format string: S(3,3", "S(3,3");
        assertValidationFails("Missing closing parenthesis in format string: S(3,3,\"_\"", "S(3,3,\"_\"");
        assertValidationFails("Expected a quoted sub-second fill character in format string: S(3,3,x)", "S(3,3,x)");
        assertValidationFails("Missing closing quote in format string: S(3,3,\"_", "S(3,3,\"_");
        assertValidationFails("Sub-second fill must be a single character in format string: S(3,3,\"\")", "S(3,3,\"\")");
        assertValidationFails("Sub-second fill must be a single character in format string: S(3,3,\"ab\")", "S(3,3,\"ab\")");

        // a width nothing satisfies, named against the string the caller wrote
        assertValidationFails("Sub-second minimum 5 exceeds maximum 3 in format string: S(5,3)", "S(5,3)");
        assertValidationFails("Sub-second maximum must be at least 1: 0 in format string: S<0", "S<0");
        assertValidationFails("Sub-second maximum must be at least 1: 0 in format string: S0", "S0");
        assertValidationFails("Sub-second maximum must be at least 1: 0 in format string: S(0,0)", "S(0,0)");
        assertValidationFails("Sub-second maximum must be at least 1: 0 in format string: S!0", "S!0");

        // a count too large to tell from the number standing for an unbounded maximum
        assertValidationFails("Sub-second digit count is too large in format string: S99999999999", "S99999999999");
        assertValidationFails("Sub-second digit count is too large in format string: S<2147483647", "S<2147483647");
        DateFormat.validate("S<2147483646");

        // a width follows the S rather than a run of them, so a run and then a width is neither
        assertValidationFails("Invalid format control character '3' in format string: SS3", "SS3");
    }

    /**
     * Every sub-second field the builder can reach writes a format string the parser takes back as
     * the same field, and writing that field again gives the same string. This is the round trip the
     * sub-second syntax exists to close: before it, the builder could describe fields no format
     * string could spell.
     */
    @Test
    public void testEverySubsecondFieldTheBuilderCanReachRoundTrips()
    {
        int[] widths = {0, 1, 2, 3, 9, 12};
        char[] fills = {'0', '_', ' ', '"', '\\', ')', '(', ',', '*', '!', 'S', '\t', '\n', '3'};
        int round = 0;
        for (int minimum : widths)
        {
            for (int maximum : widths)
            {
                if ((maximum >= 1) && (minimum <= maximum))
                {
                    for (boolean failBelow : new boolean[]{false, true})
                    {
                        for (boolean failAbove : new boolean[]{false, true})
                        {
                            char fill = fills[round++ % fills.length];
                            assertRoundTrips(field(minimum, maximum, failBelow, failAbove, fill));
                            assertRoundTrips(field(minimum, -1, failBelow, failAbove, fill));
                        }
                    }
                }
            }
        }

        // sub-second precision is unbounded, so a width is capped only by what a number can carry
        assertRoundTrips(field(0, Integer.MAX_VALUE - 1, false, false, '0'));
    }

    // Equality and description

    @Test
    public void testPatternIsAValue()
    {
        Assert.assertEquals(DateFormatPattern.parse("yyyy-MM-dd"), DateFormatPattern.parse("yyyy-MM-dd"));
        Assert.assertEquals(DateFormatPattern.parse("yyyy-MM-dd").hashCode(), DateFormatPattern.parse("yyyy-MM-dd").hashCode());
        Assert.assertNotEquals(DateFormatPattern.parse("yyyy-MM-dd"), DateFormatPattern.parse("yyyy/MM/dd"));
        Assert.assertNotEquals(DateFormatPattern.parse("yyyy"), DateFormatPattern.parse("yy"));
        Assert.assertNotEquals(DateFormatPattern.parse("HH"), DateFormatPattern.parse("hh"));

        // the zone is part of the pattern, and the two numeric notations are not the same element
        Assert.assertNotEquals(DateFormatPattern.parse("[EST]HH"), DateFormatPattern.parse("[CET]HH"));
        Assert.assertNotEquals(DateFormatPattern.parse("[EST]z"), DateFormatPattern.parse("[CET]z"));
        Assert.assertNotEquals(DateFormatPattern.parse("[EST]z"), DateFormatPattern.parse("z"));
        Assert.assertNotEquals(DateFormatPattern.parse("Z"), DateFormatPattern.parse("X"));

        // a quoted GMT is text that happens to read like the general notation, and is not it
        Assert.assertNotEquals(DateFormatPattern.parse("z"), DateFormatPattern.parse("\"GMT\""));

        // and every marker a sub-second field carries tells one from another
        Assert.assertEquals(builder().subsecond().exactly(3).endSubsecond().build(), builder().subsecond().between(3, 3).endSubsecond().build());
        Assert.assertNotEquals(builder().subsecond().exactly(3).endSubsecond().build(), builder().subsecond().atMost(3).endSubsecond().build());
        Assert.assertNotEquals(
                builder().subsecond().exactly(3).endSubsecond().build(),
                builder().subsecond().exactly(3).failBelowMinimum().endSubsecond().build());
        Assert.assertNotEquals(
                builder().subsecond().exactly(3).endSubsecond().build(),
                builder().subsecond().exactly(3).padWith('_').endSubsecond().build());
    }

    @Test
    public void testToStringDescribesTheElements()
    {
        Assert.assertEquals(
                "DateFormatPattern{Year, Literal(\"-\"), Month(2), Literal(\"-\"), Day(2)}",
                DateFormatPattern.parse("yyyy-MM-dd").toString());
        Assert.assertEquals(
                "DateFormatPattern{[EST] Hour24(2), Literal(\":\"), Minute(2), Literal(\" \"), TimeZoneName}",
                DateFormatPattern.parse("[EST]HH:mm z").toString());
        Assert.assertEquals(
                "DateFormatPattern{TwoDigitYear, Literal(\"-\"), RFC822TimeZoneOffset, Literal(\"-\"), ISO8601TimeZoneOffset, Literal(\"-\"), AmPm}",
                DateFormatPattern.parse("yy-Z-X-a").toString());
        Assert.assertEquals(
                "DateFormatPattern{Subsecond(3!, 9!, '_')}",
                builder().subsecond().between(3, 9).failBelowMinimum().failAboveMaximum().padWith('_').endSubsecond().build().toString());
    }

    /**
     * Runs of separators and quoted text collapse into one element, so a pattern holds no more
     * elements than it has things to write.
     */
    @Test
    public void testAdjacentLiteralsAreOneElement()
    {
        Assert.assertEquals(
                "DateFormatPattern{Year, Literal(\"-.-\")}",
                DateFormatPattern.parse("yyyy-.-").toString());
        Assert.assertEquals(
                "DateFormatPattern{Literal(\"at T \"), Hour24(2)}",
                DateFormatPattern.parse("\"at\" \"T\" HH").toString());
        Assert.assertEquals(
                "DateFormatPattern{Literal(\"abc\")}",
                builder().literal('a').literal("b").literal('c').build().toString());
    }

    // Helpers

    private static Builder builder()
    {
        return DateFormatPattern.builder();
    }

    /**
     * Assert that a pattern holding one sub-second field writes what is expected of each of the
     * four fractions the tests use, a null standing for a field that refuses the date rather than
     * writing it.
     *
     * @param field     builder holding the field to render
     * @param nano      what it writes for a nine digit fraction
     * @param milli     what it writes for {@code .070}
     * @param hundredth what it writes for {@code .07}
     * @param zero      what it writes for {@code .000}
     */
    private static void assertSubsecond(Builder field, String nano, String milli, String hundredth, String zero)
    {
        DateFormatPattern pattern = field.build();
        assertSubsecond(pattern, NANO, nano);
        assertSubsecond(pattern, MILLI, milli);
        assertSubsecond(pattern, HUNDREDTH, hundredth);
        assertSubsecond(pattern, ZERO, zero);

        // and no field decides for itself what a date with no fraction at all looks like
        assertSubsecond(pattern, SECOND, null);
        assertSubsecond(pattern, DAY, null);
    }

    /**
     * Assert that a format string holding one sub-second field writes what the design's worked
     * table says of it, a null standing for a field that refuses the date rather than writing it.
     * The table's last column is not a parameter because it is uniform: no field decides for itself
     * what a date with no fraction at all looks like.
     *
     * @param formatString format string holding the field
     * @param nano         what it writes for {@code .070004235}
     * @param oneMilli     what it writes for {@code .001}
     * @param hundredth    what it writes for {@code .07}
     */
    private static void assertWorkedRow(String formatString, String nano, String oneMilli, String hundredth)
    {
        DateFormatPattern pattern = DateFormatPattern.parse(formatString);
        assertSubsecond(pattern, NANO, nano);
        assertSubsecond(pattern, ONE_MILLI, oneMilli);
        assertSubsecond(pattern, HUNDREDTH, hundredth);
        assertSubsecond(pattern, SECOND, null);
        assertSubsecond(pattern, DAY, null);
    }

    /**
     * Assert that two format strings hold the same field, and that the second is the spelling the
     * pattern writes for it.
     *
     * @param legacy format string in the repetition form
     * @param modern format string in the form that replaces it
     */
    private static void assertSameField(String legacy, String modern)
    {
        DateFormatPattern read = DateFormatPattern.parse(legacy);
        Assert.assertEquals(legacy, DateFormatPattern.parse(modern), read);
        Assert.assertEquals(legacy, DateFormatPattern.parse(modern).hashCode(), read.hashCode());
        Assert.assertEquals(legacy, modern, read.pattern());
    }

    /**
     * Build a pattern holding one sub-second field.
     *
     * @param minimum   fewest digits to write
     * @param maximum   most digits to write, or a negative number for an unbounded maximum
     * @param failBelow whether to fail rather than pad below the minimum
     * @param failAbove whether to fail rather than truncate above the maximum
     * @param fill      character to pad with
     * @return pattern holding the field
     */
    private static DateFormatPattern field(int minimum, int maximum, boolean failBelow, boolean failAbove, char fill)
    {
        SubsecondBuilder field = builder().subsecond();
        if (maximum < 0)
        {
            field.atLeast(minimum);
        }
        else
        {
            field.between(minimum, maximum);
        }
        if (failBelow)
        {
            field.failBelowMinimum();
        }
        if (failAbove)
        {
            field.failAboveMaximum();
        }
        return field.padWith(fill).endSubsecond().build();
    }

    /**
     * Assert that a pattern writes a format string the parser takes back as the same pattern, that
     * writing it again gives the same string, and that the string renders through {@code format}
     * what the pattern renders directly.
     *
     * @param pattern pattern to round trip
     */
    private static void assertRoundTrips(DateFormatPattern pattern)
    {
        String written = pattern.pattern();
        DateFormatPattern read = DateFormatPattern.parse(written);
        Assert.assertEquals(written, pattern, read);
        Assert.assertEquals(written, pattern.hashCode(), read.hashCode());
        Assert.assertEquals(written, written, read.pattern());
        for (PureDate date : new PureDate[]{NANO, MILLI, ONE_MILLI, HUNDREDTH, ZERO, SECOND})
        {
            String context = written + " on " + date;
            Assert.assertEquals(context, pattern.canRender(date), read.canRender(date));
            if (pattern.canRender(date))
            {
                Assert.assertEquals(
                        context,
                        pattern.render(date),
                        DateFormat.format(new StringBuilder(), written, date).toString());
            }
        }
    }

    private static void assertSubsecond(DateFormatPattern pattern, PureDate date, String expected)
    {
        String context = pattern.pattern() + " on " + date;
        if (expected == null)
        {
            Assert.assertFalse(context, pattern.canRender(date));
            Assert.assertThrows(context, IllegalArgumentException.class, () -> pattern.render(date));
        }
        else
        {
            Assert.assertTrue(context, pattern.canRender(date));
            Assert.assertEquals(context, expected, pattern.render(date));
        }
    }

    private static void assertSameAs(String formatString, Builder builder)
    {
        DateFormatPattern parsed = DateFormatPattern.parse(formatString);
        DateFormatPattern built = builder.build();
        Assert.assertEquals(formatString, parsed, built);
        Assert.assertEquals(formatString, parsed.hashCode(), built.hashCode());
        Assert.assertEquals(formatString, parsed.pattern(), built.pattern());
        for (PureDate date : new PureDate[]{NANO, MILLI, HUNDREDTH, SECOND, DAY, YEAR})
        {
            String context = formatString + " on " + date;
            if (parsed.canRender(date))
            {
                Assert.assertEquals(context, DateFormat.format(new StringBuilder(), formatString, date).toString(), built.render(date));
            }
            else
            {
                Assert.assertFalse(context, built.canRender(date));
                Assert.assertThrows(context, IllegalArgumentException.class, () -> built.render(date));
            }
        }
    }

    private static void assertCanRender(String formatString, boolean year, boolean day, boolean second, boolean subsecond)
    {
        DateFormatPattern pattern = DateFormatPattern.parse(formatString);
        assertCanRender(pattern, YEAR, year);
        assertCanRender(pattern, DAY, day);
        assertCanRender(pattern, SECOND, second);
        assertCanRender(pattern, NANO, subsecond);
    }

    private static void assertCanRender(DateFormatPattern pattern, PureDate date, boolean expected)
    {
        String context = pattern + " on " + date;
        Assert.assertEquals(context, expected, pattern.canRender(date));
        if (expected)
        {
            pattern.render(date);
        }
        else
        {
            Assert.assertThrows(context, IllegalArgumentException.class, () -> pattern.render(date));
        }
    }

    /**
     * Assert that a builder with a sub-second field part way through refuses the given call.
     *
     * @param call call to make on the builder
     */
    private static void assertOpenSubsecondRefuses(Consumer<Builder> call)
    {
        Builder builder = builder().year();
        builder.subsecond().exactly(3);
        Assert.assertEquals(
                "A sub-second field is still open: call endSubsecond() before anything else",
                Assert.assertThrows(IllegalStateException.class, () -> call.accept(builder)).getMessage());
    }

    /**
     * Assert that a sub-second field that has been ended refuses the given call.
     *
     * @param call call to make on the field
     */
    private static void assertEndedSubsecondRefuses(Consumer<SubsecondBuilder> call)
    {
        SubsecondBuilder field = builder().subsecond();
        field.endSubsecond();
        Assert.assertEquals(
                "This sub-second field has already been ended",
                Assert.assertThrows(IllegalStateException.class, () -> call.accept(field)).getMessage());
    }

    private static void assertBuilderFails(String expectedMessage, Runnable builderCall)
    {
        Assert.assertEquals(
                expectedMessage,
                Assert.assertThrows(expectedMessage, IllegalArgumentException.class, builderCall::run).getMessage());
    }

    private static void assertValidationFails(String expectedMessage, String formatString)
    {
        Assert.assertEquals(
                formatString,
                expectedMessage,
                Assert.assertThrows(formatString, IllegalArgumentException.class, () -> DateFormat.validate(formatString)).getMessage());
    }
}
