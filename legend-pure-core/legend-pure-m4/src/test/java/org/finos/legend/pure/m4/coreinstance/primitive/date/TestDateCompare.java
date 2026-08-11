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

import org.eclipse.collections.api.factory.Lists;
import org.eclipse.collections.api.list.ListIterable;
import org.eclipse.collections.api.list.MutableList;
import org.junit.Assert;
import org.junit.Test;

import java.time.LocalDateTime;
import java.util.EnumSet;
import java.util.function.BiConsumer;

/**
 * Tests for {@link DateFunctions#compare(PureDate, PureDate)}, which also backs
 * {@link PureDate#compareTo(PureDate)}.
 *
 * <p><b>The rule under test, in one sentence:</b> the date that starts earlier sorts first, and if
 * both start at the same instant the wider one sorts first. {@link DateFunctions#compare} explains
 * why, and is the place to look first; this class checks it.
 *
 * <p>The reason width comes into it is that a Pure date is a span of time, not an instant: it stops
 * at whatever granularity it was written with and stands for everything it leaves unsaid, so
 * {@code 2014} covers the whole year and {@code 2014-06} the whole month. Comparing dates of
 * different granularities means comparing spans of different widths, and that is where the
 * surprises live.
 *
 * <p>The tests come in three layers.
 *
 * <ol>
 *     <li><b>Worked examples</b>, one granularity at a time and then across granularities, which
 *     say in concrete terms what the ordering does.</li>
 *     <li><b>Three equivalent characterizations</b>, each checked over every ordered pair of
 *     {@link #DATES}: against the Allen interval relation between the two spans
 *     ({@link #testCompareMatchesAllenCharacterization()}), against "start ascending, end
 *     descending" ({@link #testCompareIsStartAscendingThenEndDescending()}), and against "starts
 *     earlier, or properly contains"
 *     ({@link #testCompareIsNegativeIffStartsEarlierOrProperlyContains()}). Two tempting but wrong
 *     readings are pinned as counterexamples.</li>
 *     <li><b>Order properties</b>: antisymmetry, reflexivity, transitivity, consistency with
 *     {@code equals}, and that only -1, 0 and 1 are ever returned.</li>
 * </ol>
 *
 * <p>Layer 2 uses Allen's interval algebra as its machinery, via {@link AllenRelation} and
 * {@link #allenRelation(PureDate, PureDate)}, which derive the relation from the two spans'
 * endpoints independently of {@code compare}. Only eleven of the thirteen relations can arise:
 * granularity boundaries always subdivide the boundaries above them, so two Pure date spans are
 * either disjoint or nested, never partially overlapping. That is what makes the ordering
 * well defined, and {@link #testDateIntervalsNeverPartiallyOverlap()} asserts it.
 */
public class TestDateCompare
{
    /**
     * A corpus spanning every granularity, chosen so that all eleven realizable Allen relations occur
     * between some pair (see {@link #testAllRealizableAllenRelationsAreCovered()}).
     */
    private static final ListIterable<String> DATE_STRINGS = Lists.immutable.with(
            // years
            "-1", "0", "1", "2013", "2014", "2015", "2016",
            // year-months
            "2013-12", "2014-01", "2014-03", "2014-12", "2015-01", "2016-02",
            // strict dates
            "2013-12-31", "2014-01-01", "2014-03-01", "2014-03-10", "2014-03-11", "2014-03-31", "2014-12-31", "2016-02-29",
            // dates with hour
            "2013-12-31T23", "2014-03-10T00", "2014-03-10T16", "2014-03-10T23",
            // dates with minute
            "2014-03-10T16:00", "2014-03-10T16:12", "2014-03-10T16:13", "2014-03-10T16:59",
            // dates with second
            "2014-03-10T16:12:00", "2014-03-10T16:12:35", "2014-03-10T16:12:36", "2014-03-10T16:12:59",
            // dates with subsecond, including nested subsecond precisions
            "2014-03-10T16:12:35.0", "2014-03-10T16:12:35.07", "2014-03-10T16:12:35.070",
            "2014-03-10T16:12:35.0700", "2014-03-10T16:12:35.070004235",
            "2014-03-10T16:12:35.08", "2014-03-10T16:12:35.9", "2014-03-10T16:12:35.999999999");

    private static final ListIterable<PureDate> DATES = DATE_STRINGS.collect(DateFunctions::parsePureDate);

    /**
     * Allen relations that {@code compare} maps to -1: everything where a starts first, plus the
     * cases where a and b start together and a is the wider (containing) interval.
     */
    private static final EnumSet<AllenRelation> LESS_THAN_RELATIONS =
            EnumSet.of(AllenRelation.BEFORE, AllenRelation.MEETS, AllenRelation.CONTAINS, AllenRelation.FINISHED_BY, AllenRelation.STARTED_BY);

    /**
     * Allen relations that {@code compare} maps to 1: the converses of {@link #LESS_THAN_RELATIONS}.
     */
    private static final EnumSet<AllenRelation> GREATER_THAN_RELATIONS =
            EnumSet.of(AllenRelation.AFTER, AllenRelation.MET_BY, AllenRelation.DURING, AllenRelation.FINISHES, AllenRelation.STARTS);

    // Worked examples, one granularity at a time

    @Test
    public void testCompareYears()
    {
        assertCompare(0, "2014", "2014");
        assertCompare(-1, "2013", "2014");

        // negative and zero years
        assertCompare(-1, "-1", "0");
        assertCompare(-1, "0", "1");

        // an arbitrarily large gap still yields exactly -1 / 1
        assertCompare(-1, "-999999999", "999999999");
    }

    @Test
    public void testCompareYearMonths()
    {
        assertCompare(0, "2014-03", "2014-03");
        assertCompare(-1, "2014-02", "2014-03");

        // year dominates month
        assertCompare(-1, "2013-12", "2014-01");
    }

    @Test
    public void testCompareStrictDates()
    {
        assertCompare(0, "2014-03-10", "2014-03-10");
        assertCompare(-1, "2014-03-09", "2014-03-10");

        // month dominates day
        assertCompare(-1, "2014-02-28", "2014-03-01");
        // year dominates month and day
        assertCompare(-1, "2013-12-31", "2014-01-01");
    }

    @Test
    public void testCompareDatesWithHour()
    {
        assertCompare(0, "2014-03-10T16", "2014-03-10T16");
        assertCompare(-1, "2014-03-10T15", "2014-03-10T16");

        // day dominates hour
        assertCompare(-1, "2014-03-10T23", "2014-03-11T00");
    }

    @Test
    public void testCompareDatesWithMinute()
    {
        assertCompare(0, "2014-03-10T16:12", "2014-03-10T16:12");
        assertCompare(-1, "2014-03-10T16:11", "2014-03-10T16:12");

        // hour dominates minute
        assertCompare(-1, "2014-03-10T15:59", "2014-03-10T16:00");
    }

    @Test
    public void testCompareDatesWithSecond()
    {
        assertCompare(0, "2014-03-10T16:12:35", "2014-03-10T16:12:35");
        assertCompare(-1, "2014-03-10T16:12:34", "2014-03-10T16:12:35");

        // minute dominates second
        assertCompare(-1, "2014-03-10T16:11:59", "2014-03-10T16:12:00");
    }

    @Test
    public void testCompareSubsecondsOfEqualPrecision()
    {
        assertCompare(0, "2014-03-10T16:12:35.070004235", "2014-03-10T16:12:35.070004235");
        assertCompare(-1, "2014-03-10T16:12:35.070004234", "2014-03-10T16:12:35.070004235");

        // second dominates subsecond
        assertCompare(-1, "2014-03-10T16:12:34.999", "2014-03-10T16:12:35.000");
    }

    // Worked examples across granularities

    /**
     * Subseconds of different digit counts are different granularities: ".07" is a ten millisecond
     * span, ".070" a one millisecond span nested inside it. The comparison is a decimal expansion
     * comparison, not a string or numeric comparison of the digits.
     */
    @Test
    public void testCompareSubsecondsOfDifferentPrecision()
    {
        // ".0" = [.0, .1) contains ".070" = [.070, .071), so the wider value sorts first
        assertCompare(-1, "2014-03-10T16:12:35.0", "2014-03-10T16:12:35.070");

        // ".07" = [.07, .08) is inside ".0" = [.0, .1) but starts later, so it sorts after
        assertCompare(1, "2014-03-10T16:12:35.07", "2014-03-10T16:12:35.0");

        // a trailing zero narrows the span without moving its start, so the wider one sorts first
        assertCompare(-1, "2014-03-10T16:12:35.070", "2014-03-10T16:12:35.0700");

        // digit counts do not decide disjoint cases: ".07" is entirely before ".9"
        assertCompare(-1, "2014-03-10T16:12:35.07", "2014-03-10T16:12:35.9");

        // ".9" = [.9, 1.0) contains ".999999999"
        assertCompare(-1, "2014-03-10T16:12:35.9", "2014-03-10T16:12:35.999999999");
    }

    /**
     * Along a chain of ever finer granularities all starting at the same instant, each value sorts
     * before every finer value below it: the container always precedes the contained.
     */
    @Test
    public void testCompareGranularityLadderSharingAStart()
    {
        ListIterable<PureDate> ladder = Lists.immutable.with(
                parse("2014"),
                parse("2014-01"),
                parse("2014-01-01"),
                parse("2014-01-01T00"),
                parse("2014-01-01T00:00"),
                parse("2014-01-01T00:00:00"),
                parse("2014-01-01T00:00:00.0"),
                parse("2014-01-01T00:00:00.00"),
                parse("2014-01-01T00:00:00.000000000"));
        for (int i = 0; i < ladder.size(); i++)
        {
            PureDate coarser = ladder.get(i);
            Assert.assertEquals(coarser.toString(), intervalStart(ladder.get(0)), intervalStart(coarser));
            for (int j = i + 1; j < ladder.size(); j++)
            {
                PureDate finer = ladder.get(j);
                String message = coarser + " vs " + finer;
                Assert.assertEquals(message, -1, DateFunctions.compare(coarser, finer));
                Assert.assertEquals(message, 1, DateFunctions.compare(finer, coarser));
                Assert.assertEquals(message, AllenRelation.STARTED_BY, allenRelation(coarser, finer));
            }
        }
    }

    /**
     * The mirror image of {@link #testCompareGranularityLadderSharingAStart()}: a chain of ever
     * finer granularities all ending at the same instant. The container still precedes the
     * contained, even though here it is the container that starts earlier.
     */
    @Test
    public void testCompareGranularityLadderSharingAnEnd()
    {
        ListIterable<PureDate> ladder = Lists.immutable.with(
                parse("2014"),
                parse("2014-12"),
                parse("2014-12-31"),
                parse("2014-12-31T23"),
                parse("2014-12-31T23:59"),
                parse("2014-12-31T23:59:59"),
                parse("2014-12-31T23:59:59.9"),
                parse("2014-12-31T23:59:59.999999999"));
        for (int i = 0; i < ladder.size(); i++)
        {
            PureDate coarser = ladder.get(i);
            Assert.assertEquals(coarser.toString(), intervalEnd(ladder.get(0)), intervalEnd(coarser));
            for (int j = i + 1; j < ladder.size(); j++)
            {
                PureDate finer = ladder.get(j);
                String message = coarser + " vs " + finer;
                Assert.assertEquals(message, -1, DateFunctions.compare(coarser, finer));
                Assert.assertEquals(message, 1, DateFunctions.compare(finer, coarser));
                Assert.assertEquals(message, AllenRelation.FINISHED_BY, allenRelation(coarser, finer));
            }
        }
    }

    /**
     * A coarse date that properly contains a finer one sorts before it, wherever inside it the
     * finer one falls.
     */
    @Test
    public void testCompareContainingIntervalSortsFirst()
    {
        PureDate year = parse("2014");
        ListIterable<PureDate> contained = Lists.immutable.with(
                parse("2014-06"),
                parse("2014-06-15"),
                parse("2014-06-15T12"),
                parse("2014-06-15T12:30"),
                parse("2014-06-15T12:30:45"),
                parse("2014-06-15T12:30:45.070004235"));
        for (PureDate date : contained)
        {
            String message = year + " vs " + date;
            Assert.assertEquals(message, AllenRelation.CONTAINS, allenRelation(year, date));
            Assert.assertEquals(message, -1, DateFunctions.compare(year, date));
            Assert.assertEquals(message, 1, DateFunctions.compare(date, year));
        }

        // and one level down: the month contains everything below it too
        PureDate month = parse("2014-06");
        Assert.assertEquals(-1, DateFunctions.compare(month, parse("2014-06-15T12:30")));
        Assert.assertEquals(1, DateFunctions.compare(parse("2014-06-15T12:30"), month));
    }

    /**
     * When the intervals are disjoint, granularity is irrelevant: whichever comes first on the time
     * line sorts first.
     */
    @Test
    public void testCompareDisjointIntervalsAcrossGranularities()
    {
        assertStrictlyIncreasing(Lists.immutable.with(
                parse("2013-12-31T23:59:59.999"),
                parse("2014"),
                parse("2015-01-01T00"),
                parse("2015-01-01T00:00:00.5"),
                parse("2016-02"),
                parse("2016-03-01")));

        assertStrictlyIncreasing(Lists.immutable.with(
                parse("2014-03"),
                parse("2014-04-01T00:00"),
                parse("2014-05"),
                parse("2014-06-15T12:30:45.070004235"),
                parse("2014-07")));
    }

    // The characterization itself

    /**
     * The full pairwise check: {@code compare} agrees with the Allen relation between the two
     * intervals, for every ordered pair of the corpus, mixing granularities freely.
     */
    @Test
    public void testCompareMatchesAllenCharacterization()
    {
        forEachPair((date1, date2) ->
        {
            AllenRelation relation = allenRelation(date1, date2);
            int expected = LESS_THAN_RELATIONS.contains(relation) ? -1 : (GREATER_THAN_RELATIONS.contains(relation) ? 1 : 0);
            Assert.assertEquals(date1 + " vs " + date2 + " (" + relation + ")", expected, DateFunctions.compare(date1, date2));
            Assert.assertEquals(date1 + " vs " + date2, relation == AllenRelation.EQUALS, DateFunctions.compare(date1, date2) == 0);
        });
    }

    /**
     * Equivalent phrasing: {@code compare} sorts by interval start ascending, tie-broken by
     * interval end <em>descending</em>. That is, it is the lexicographic comparison of
     * {@code (start(a), -end(a))} against {@code (start(b), -end(b))}.
     */
    @Test
    public void testCompareIsStartAscendingThenEndDescending()
    {
        forEachPair((date1, date2) ->
        {
            int startComparison = intervalStart(date1).compareTo(intervalStart(date2));
            int endComparison = intervalEnd(date1).compareTo(intervalEnd(date2));
            int expected = (startComparison != 0) ? Integer.signum(startComparison) : -Integer.signum(endComparison);
            Assert.assertEquals(date1 + " vs " + date2, expected, DateFunctions.compare(date1, date2));
        });
    }

    /**
     * Equivalent phrasing: a sorts before b exactly when a starts before the starting of b, or a
     * properly contains b.
     */
    @Test
    public void testCompareIsNegativeIffStartsEarlierOrProperlyContains()
    {
        forEachPair((date1, date2) ->
        {
            boolean startsEarlier = intervalStart(date1).isBefore(intervalStart(date2));
            boolean properlyContains = !intervalStart(date1).isAfter(intervalStart(date2)) &&
                    !intervalEnd(date1).isBefore(intervalEnd(date2)) &&
                    !date1.equals(date2);
            Assert.assertEquals(date1 + " vs " + date2, startsEarlier || properlyContains, DateFunctions.compare(date1, date2) < 0);
        });
    }

    /**
     * {@code compare(a, b) == -1} is <em>not</em> "a ends before the ending of b". The year 2014
     * contains June 2014 and sorts first, yet it ends six months later. Ordering by interval end
     * would reverse this pair.
     */
    @Test
    public void testCompareIsNotOrderingByIntervalEnd()
    {
        PureDate year = parse("2014");
        PureDate month = parse("2014-06");

        assertCompare(-1, "2014", "2014-06");
        Assert.assertEquals(AllenRelation.CONTAINS, allenRelation(year, month));
        Assert.assertTrue("the containing year ends after the contained month", intervalEnd(year).isAfter(intervalEnd(month)));
    }

    /**
     * Neither is {@code compare(a, b) == -1} simply "a starts before the starting of b": the year
     * 2014 and January 2014 start at the same instant, and the year still sorts first.
     */
    @Test
    public void testCompareIsNotOrderingByIntervalStartAlone()
    {
        PureDate year = parse("2014");
        PureDate january = parse("2014-01");

        assertCompare(-1, "2014", "2014-01");
        Assert.assertEquals(AllenRelation.STARTED_BY, allenRelation(year, january));
        Assert.assertEquals("the year and January start together", intervalStart(year), intervalStart(january));
    }

    /**
     * Pure date intervals are nested: any two are disjoint or one contains the other, so Allen's
     * {@code overlaps} and {@code overlapped by} never arise. This is what keeps the ordering above
     * well defined; a partially overlapping pair would have no consistent answer.
     */
    @Test
    public void testDateIntervalsNeverPartiallyOverlap()
    {
        forEachPair((date1, date2) ->
        {
            AllenRelation relation = allenRelation(date1, date2);
            Assert.assertNotEquals(date1 + " vs " + date2, AllenRelation.OVERLAPS, relation);
            Assert.assertNotEquals(date1 + " vs " + date2, AllenRelation.OVERLAPPED_BY, relation);
        });
    }

    /**
     * Guards the corpus: every Allen relation other than the two impossible ones must actually be
     * exercised by {@link #testCompareMatchesAllenCharacterization()}.
     */
    @Test
    public void testAllRealizableAllenRelationsAreCovered()
    {
        EnumSet<AllenRelation> observed = EnumSet.noneOf(AllenRelation.class);
        forEachPair((date1, date2) -> observed.add(allenRelation(date1, date2)));

        EnumSet<AllenRelation> expected = EnumSet.allOf(AllenRelation.class);
        expected.remove(AllenRelation.OVERLAPS);
        expected.remove(AllenRelation.OVERLAPPED_BY);
        Assert.assertEquals(expected, observed);
    }

    // General properties of the ordering

    @Test
    public void testCompareReturnsOnlyMinusOneZeroOrOne()
    {
        forEachPair((date1, date2) ->
        {
            int result = DateFunctions.compare(date1, date2);
            Assert.assertTrue(date1 + " vs " + date2 + ": " + result, (result == -1) || (result == 0) || (result == 1));
        });
    }

    @Test
    public void testCompareIsAntisymmetric()
    {
        forEachPair((date1, date2) -> Assert.assertEquals(date1 + " vs " + date2, -DateFunctions.compare(date1, date2), DateFunctions.compare(date2, date1)));
    }

    @Test
    public void testCompareIsReflexive()
    {
        for (PureDate date : DATES)
        {
            Assert.assertEquals(date.toString(), 0, DateFunctions.compare(date, date));
            // not just the identity short circuit
            PureDate copy = DateFunctions.parsePureDate(date.toString());
            Assert.assertEquals(date.toString(), 0, DateFunctions.compare(date, copy));
        }
    }

    @Test
    public void testCompareIsTransitive()
    {
        for (PureDate date1 : DATES)
        {
            for (PureDate date2 : DATES)
            {
                if (DateFunctions.compare(date1, date2) < 0)
                {
                    for (PureDate date3 : DATES)
                    {
                        if (DateFunctions.compare(date2, date3) < 0)
                        {
                            Assert.assertTrue(date1 + " < " + date2 + " < " + date3, DateFunctions.compare(date1, date3) < 0);
                        }
                    }
                }
            }
        }
    }

    @Test
    public void testCompareIsConsistentWithEquals()
    {
        forEachPair((date1, date2) ->
        {
            boolean equal = DateFunctions.compare(date1, date2) == 0;
            Assert.assertEquals(date1 + " vs " + date2, equal, date1.equals(date2));
            if (equal)
            {
                Assert.assertEquals(date1 + " vs " + date2, date1.hashCode(), date2.hashCode());
            }
        });
    }

    @Test
    public void testCompareToDelegatesToCompare()
    {
        forEachPair((date1, date2) -> Assert.assertEquals(date1 + " vs " + date2, DateFunctions.compare(date1, date2), date1.compareTo(date2)));
    }

    @Test
    public void testSortingMixedGranularities()
    {
        MutableList<PureDate> sorted = Lists.mutable.with(
                parse("2014-06-15T12:30:45.070004235"),
                parse("2014"),
                parse("2015"),
                parse("2014-06-15"),
                parse("2013-12-31"),
                parse("2014-06"),
                parse("2014-06-15T12:30:45"),
                parse("2014-06-15T12:30"),
                parse("2014-07"),
                parse("2014-06-15T12")).sortThis();
        Assert.assertEquals(
                Lists.mutable.with(
                        "2013-12-31",
                        "2014",
                        "2014-06",
                        "2014-06-15",
                        "2014-06-15T12",
                        "2014-06-15T12:30+0000",
                        "2014-06-15T12:30:45+0000",
                        "2014-06-15T12:30:45.070004235+0000",
                        "2014-07",
                        "2015"),
                sorted.collect(PureDate::toString));
    }

    // LatestDate

    /**
     * {@link LatestDate} has no components to compare and no fixed position on the time line, so it
     * is ordered after every other date. That is a sorting convention, adopted so that any two dates
     * can be ordered, rather than a claim about when {@code %latest} falls.
     */
    @Test
    public void testLatestDateSortsAfterEveryOtherDate()
    {
        Assert.assertEquals(0, DateFunctions.compare(LatestDate.instance, LatestDate.instance));
        Assert.assertEquals(0, LatestDate.instance.compareTo(LatestDate.instance));

        for (PureDate date : DATES)
        {
            String message = date.toString();
            Assert.assertEquals(message, 1, DateFunctions.compare(LatestDate.instance, date));
            Assert.assertEquals(message, -1, DateFunctions.compare(date, LatestDate.instance));
            Assert.assertEquals(message, 1, LatestDate.instance.compareTo(date));
            Assert.assertEquals(message, -1, date.compareTo(LatestDate.instance));
        }

        // including the latest date the platform can represent
        PureDate maxDate = DateFunctions.newPureDate(java.time.Year.MAX_VALUE);
        Assert.assertEquals(1, DateFunctions.compare(LatestDate.instance, maxDate));
        Assert.assertEquals(-1, DateFunctions.compare(maxDate, LatestDate.instance));
    }

    /**
     * {@link LatestDate} is equal only to itself, and answers from either side rather than throwing.
     * Ordering it after every other date is only useful if {@code equals} agrees that it is not any
     * of them, and {@code equals} has to be symmetric even where {@code %latest} has no components
     * to offer.
     */
    @Test
    public void testLatestDateEqualsOnlyItself()
    {
        Assert.assertEquals(LatestDate.instance, LatestDate.instance);

        for (PureDate date : DATES)
        {
            String message = date.toString();
            Assert.assertNotEquals(message, date, LatestDate.instance);
            Assert.assertNotEquals(message, LatestDate.instance, date);
            Assert.assertNotEquals(message, 0, DateFunctions.compare(date, LatestDate.instance));
        }
    }

    @Test
    public void testSortingWithLatestDate()
    {
        MutableList<PureDate> sorted = Lists.mutable.<PureDate>with(
                parse("2015"),
                LatestDate.instance,
                parse("2014-06-15"),
                parse("2013-12-31")).sortThis();
        Assert.assertEquals(
                Lists.mutable.with("2013-12-31", "2014-06-15", "2015", "%latest"),
                sorted.collect(PureDate::toString));
    }

    // Helpers

    private static PureDate parse(String string)
    {
        return DateFunctions.parsePureDate(string);
    }

    /**
     * Assert how the two dates compare, in both directions: comparing the second with the first must
     * give the negation. Since {@code compare} is antisymmetric everywhere (see
     * {@link #testCompareIsAntisymmetric()}), checking the mirror costs nothing and halves what each
     * case has to spell out.
     */
    private static void assertCompare(int expected, String date1, String date2)
    {
        PureDate first = parse(date1);
        PureDate second = parse(date2);
        Assert.assertEquals(date1 + " vs " + date2, expected, DateFunctions.compare(first, second));
        Assert.assertEquals(date2 + " vs " + date1, -expected, DateFunctions.compare(second, first));
    }

    private static void assertStrictlyIncreasing(ListIterable<PureDate> dates)
    {
        for (int i = 0; i < dates.size(); i++)
        {
            for (int j = i + 1; j < dates.size(); j++)
            {
                String message = dates.get(i) + " vs " + dates.get(j);
                Assert.assertEquals(message, -1, DateFunctions.compare(dates.get(i), dates.get(j)));
                Assert.assertEquals(message, 1, DateFunctions.compare(dates.get(j), dates.get(i)));
            }
        }
    }

    private static void forEachPair(BiConsumer<PureDate, PureDate> consumer)
    {
        for (PureDate date1 : DATES)
        {
            for (PureDate date2 : DATES)
            {
                consumer.accept(date1, date2);
            }
        }
    }

    /**
     * The inclusive start of the interval denoted by the given date: every unspecified component
     * takes its minimum value.
     */
    private static LocalDateTime intervalStart(PureDate date)
    {
        return LocalDateTime.of(
                date.getYear(),
                date.hasMonth() ? date.getMonth() : 1,
                date.hasDay() ? date.getDay() : 1,
                date.hasHour() ? date.getHour() : 0,
                date.hasMinute() ? date.getMinute() : 0,
                date.hasSecond() ? date.getSecond() : 0,
                date.hasSubsecond() ? subsecondNanos(date.getSubsecond()) : 0);
    }

    /**
     * The exclusive end of the interval denoted by the given date: its start advanced by one unit
     * of its granularity.
     */
    private static LocalDateTime intervalEnd(PureDate date)
    {
        LocalDateTime start = intervalStart(date);
        if (date.hasSubsecond())
        {
            return start.plusNanos(subsecondWidthInNanos(date.getSubsecond()));
        }
        if (date.hasSecond())
        {
            return start.plusSeconds(1);
        }
        if (date.hasMinute())
        {
            return start.plusMinutes(1);
        }
        if (date.hasHour())
        {
            return start.plusHours(1);
        }
        if (date.hasDay())
        {
            return start.plusDays(1);
        }
        if (date.hasMonth())
        {
            return start.plusMonths(1);
        }
        return start.plusYears(1);
    }

    private static int subsecondNanos(String subsecond)
    {
        Assert.assertTrue("this test models subseconds with LocalDateTime, so at most 9 digits: " + subsecond, subsecond.length() <= 9);
        StringBuilder builder = new StringBuilder(subsecond);
        while (builder.length() < 9)
        {
            builder.append('0');
        }
        return Integer.parseInt(builder.toString());
    }

    private static long subsecondWidthInNanos(String subsecond)
    {
        Assert.assertTrue("this test models subseconds with LocalDateTime, so at most 9 digits: " + subsecond, subsecond.length() <= 9);
        long width = 1_000_000_000L;
        for (int i = 0; i < subsecond.length(); i++)
        {
            width /= 10L;
        }
        return width;
    }

    /**
     * The Allen relation from the first interval to the second, computed from the interval
     * endpoints alone (with half-open intervals, so {@code meets} means that one ends exactly where
     * the next begins).
     */
    private static AllenRelation allenRelation(PureDate date1, PureDate date2)
    {
        LocalDateTime start1 = intervalStart(date1);
        LocalDateTime end1 = intervalEnd(date1);
        LocalDateTime start2 = intervalStart(date2);
        LocalDateTime end2 = intervalEnd(date2);

        int startComparison = start1.compareTo(start2);
        int endComparison = end1.compareTo(end2);

        if (startComparison == 0)
        {
            if (endComparison == 0)
            {
                return AllenRelation.EQUALS;
            }
            return (endComparison < 0) ? AllenRelation.STARTS : AllenRelation.STARTED_BY;
        }

        if (startComparison < 0)
        {
            int comparison = end1.compareTo(start2);
            if (comparison < 0)
            {
                return AllenRelation.BEFORE;
            }
            if (comparison == 0)
            {
                return AllenRelation.MEETS;
            }
            if (endComparison < 0)
            {
                return AllenRelation.OVERLAPS;
            }
            return (endComparison == 0) ? AllenRelation.FINISHED_BY : AllenRelation.CONTAINS;
        }

        int comparison = end2.compareTo(start1);
        if (comparison < 0)
        {
            return AllenRelation.AFTER;
        }
        if (comparison == 0)
        {
            return AllenRelation.MET_BY;
        }
        if (endComparison > 0)
        {
            return AllenRelation.OVERLAPPED_BY;
        }
        return (endComparison == 0) ? AllenRelation.FINISHES : AllenRelation.DURING;
    }

    /**
     * The thirteen relations of Allen's interval algebra. {@code OVERLAPS} and {@code OVERLAPPED_BY}
     * cannot arise between Pure dates.
     */
    private enum AllenRelation
    {
        BEFORE,
        MEETS,
        OVERLAPS,
        FINISHED_BY,
        CONTAINS,
        STARTS,
        EQUALS,
        STARTED_BY,
        DURING,
        FINISHES,
        OVERLAPPED_BY,
        MET_BY,
        AFTER
    }
}
