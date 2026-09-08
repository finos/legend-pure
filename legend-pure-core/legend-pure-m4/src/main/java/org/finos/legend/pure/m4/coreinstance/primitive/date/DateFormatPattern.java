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

import org.finos.legend.pure.m4.tools.SafeAppendable;
import org.finos.legend.pure.m4.tools.time.TimeZones;

import java.time.ZoneId;
import java.time.ZonedDateTime;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Objects;
import java.util.function.Predicate;
import java.util.function.ToIntFunction;

/**
 * A date format string, parsed into the sequence of elements it describes. Rendering a date is then
 * a walk of that sequence, with nothing left to work out about the format string itself.
 *
 * <p>A pattern can be had in either of two ways, and they build the same thing. {@link
 * #parse(String)} reads one out of a format string, which is what {@link DateFormat#format} does on
 * every call. {@link #builder()} assembles one directly, for a caller that knows what it wants
 * written and has no reason to spell it as a string first:
 *
 * <pre>
 * DateFormatPattern.builder()
 *         .year().literal('-').month().literal('-').day()
 *         .literal('T')
 *         .hour24().literal(':').minute().literal(':').second()
 *         .literal('.').subsecond().exactly(3).endSubsecond()
 *         .iso8601TimeZoneOffset()
 *         .build();
 * </pre>
 *
 * <p>The builder reaches everything a format string reaches and a little it does not: a literal may
 * hold any text at all, where a format string has to quote it and escape the quotes within it. Every
 * sub-second field {@link SubsecondBuilder} describes has a spelling in the format string, so the
 * two reach the same fields by different routes.
 *
 * <p>{@link #pattern()} goes the other way, writing the format string a pattern stands for. Every
 * pattern {@link #parse} produces writes a string {@code parse} takes back, and reading that back
 * gives an equal pattern.
 *
 * <p>A pattern is immutable and holds nothing across renderings, so one may be built once and used
 * from any number of threads.
 */
public final class DateFormatPattern
{
    private final Element[] elements;
    private final ZoneId timeZone;
    private final String timeZoneId;

    private DateFormatPattern(Element[] elements, ZoneId timeZone, String timeZoneId)
    {
        this.elements = elements;
        this.timeZone = timeZone;
        this.timeZoneId = timeZoneId;
    }

    /**
     * Write a date to an appendable in the form this pattern describes.
     *
     * @param appendable appendable to write to
     * @param date       date to write
     * @param <T>        appendable type
     * @return the appendable
     * @throws IllegalArgumentException if the pattern asks for something the date does not have
     */
    public <T extends Appendable> T render(T appendable, PureDate date)
    {
        SafeAppendable safeAppendable = SafeAppendable.wrap(appendable);
        ZonedDateTime zoned = shift(date);
        for (Element element : this.elements)
        {
            element.render(safeAppendable, date, zoned, this.timeZoneId);
        }
        return appendable;
    }

    /**
     * Write a date to a new string in the form this pattern describes.
     *
     * @param date date to write
     * @return the date written out
     * @throws IllegalArgumentException if the pattern asks for something the date does not have
     */
    public String render(PureDate date)
    {
        return render(new StringBuilder(32), date).toString();
    }

    /**
     * Return whether every element of this pattern can write the given date, which is whether
     * {@link #render} will succeed on it. The two ask the same question of each element.
     *
     * @param date date to test
     * @return whether the date can be written
     */
    public boolean canRender(PureDate date)
    {
        for (Element element : this.elements)
        {
            if (!element.canRender(date))
            {
                return false;
            }
        }
        return true;
    }

    /**
     * Write the format string this pattern stands for, which {@link #parse} reads back into an
     * equal pattern. Where a pattern can be spelled in more than one way this writes one of them,
     * so the string need not be the one the pattern came from: {@code yyyy} comes back as it went
     * in, but {@code "a"" b"} comes back as {@code "a" "b"}.
     *
     * <p>A sub-second field is never written as a run of letters, since every field that run can
     * spell has a width form meaning precisely the same thing: {@code SSS} comes back as
     * {@code S<3} and {@code SSSS} as {@code S*}. Reading a format string and writing it back is
     * therefore the rewrite from the older spelling to the newer one.
     *
     * @param appendable appendable to write to
     * @param <T>        appendable type
     * @return the appendable
     */
    public <T extends Appendable> T appendPattern(T appendable)
    {
        SafeAppendable safeAppendable = SafeAppendable.wrap(appendable);
        if (this.timeZoneId != null)
        {
            appendTimeZoneId(safeAppendable, this.timeZoneId);
        }
        for (Element element : this.elements)
        {
            element.appendPattern(safeAppendable);
        }
        return appendable;
    }

    /**
     * Get the format string this pattern stands for.
     *
     * @return format string
     * @see #appendPattern(Appendable)
     */
    public String pattern()
    {
        return appendPattern(new StringBuilder(32)).toString();
    }

    /**
     * Get the time zone this pattern renders in, or null if it renders dates as the UTC times they
     * are.
     *
     * @return time zone, or null
     */
    public ZoneId getTimeZone()
    {
        return this.timeZone;
    }

    @Override
    public boolean equals(Object other)
    {
        if (this == other)
        {
            return true;
        }
        if (!(other instanceof DateFormatPattern))
        {
            return false;
        }
        DateFormatPattern that = (DateFormatPattern) other;
        return Arrays.equals(this.elements, that.elements) && Objects.equals(this.timeZoneId, that.timeZoneId);
    }

    @Override
    public int hashCode()
    {
        return (31 * Arrays.hashCode(this.elements)) + Objects.hash(this.timeZoneId);
    }

    @Override
    public String toString()
    {
        StringBuilder builder = new StringBuilder("DateFormatPattern{");
        if (this.timeZoneId != null)
        {
            builder.append('[').append(this.timeZoneId).append("] ");
        }
        for (int i = 0; i < this.elements.length; i++)
        {
            if (i > 0)
            {
                builder.append(", ");
            }
            builder.append(this.elements[i]);
        }
        return builder.append('}').toString();
    }

    /**
     * A Pure date is always understood as UTC, so a zone shifts it to the instant it stands for as
     * that zone reads it. A date with no hour is not an instant and so is never shifted.
     *
     * @param date date being rendered
     * @return the date as this pattern's zone reads it, or null if there is nothing to shift
     */
    private ZonedDateTime shift(PureDate date)
    {
        return ((this.timeZone == null) || !date.hasHour()) ? null : PureDateToJava.start().toInstant(date).atZone(this.timeZone);
    }

    /**
     * Parse a format string into a pattern.
     *
     * @param formatString format string
     * @return pattern
     * @throws IllegalArgumentException if the format string is malformed, names a time zone that
     *                                  cannot be resolved, or sets a time zone anywhere but at the
     *                                  start
     */
    public static DateFormatPattern parse(String formatString)
    {
        return parse(formatString, 0, formatString.length());
    }

    /**
     * Parse a portion of a string into a pattern, which is how a format string held inside a larger
     * one is read without first cutting it out.
     *
     * @param formatString string holding the format string
     * @param start        start index of the format string (inclusive)
     * @param end          end index of the format string (exclusive)
     * @return pattern
     * @throws IllegalArgumentException if the format string is malformed, names a time zone that
     *                                  cannot be resolved, or sets a time zone anywhere but at the
     *                                  start
     */
    public static DateFormatPattern parse(String formatString, int start, int end)
    {
        return new Parser(formatString, start, end).parse();
    }

    /**
     * Get a builder for assembling a pattern element by element.
     *
     * @return builder
     */
    public static Builder builder()
    {
        return new Builder();
    }

    /**
     * One thing a pattern writes. An element either always writes - a literal does - or writes only
     * a date carrying what it asks for, which is what {@link #canRender} answers.
     */
    private abstract static class Element
    {
        /**
         * Return whether this element can write the given date.
         *
         * @param date date to test
         * @return whether the date can be written
         */
        abstract boolean canRender(PureDate date);

        /**
         * Write the given date.
         *
         * @param appendable appendable to write to
         * @param date       date to write
         * @param zoned      the date as the pattern's time zone reads it, or null if it is not
         *                   shifted
         * @param timeZoneId the pattern's time zone as it was named, or null if none was
         */
        abstract void render(SafeAppendable appendable, PureDate date, ZonedDateTime zoned, String timeZoneId);

        /**
         * Write the part of a format string this element stands for.
         *
         * @param appendable appendable to write to
         */
        abstract void appendPattern(SafeAppendable appendable);
    }

    /**
     * Text written out as it stands: a separator, a quoted run of a format string, or anything at
     * all where the pattern was built rather than parsed.
     */
    private static final class Literal extends Element
    {
        private final String text;

        Literal(String text)
        {
            this.text = text;
        }

        @Override
        boolean canRender(PureDate date)
        {
            return true;
        }

        @Override
        void render(SafeAppendable appendable, PureDate date, ZonedDateTime zoned, String timeZoneId)
        {
            appendable.append(this.text);
        }

        @Override
        void appendPattern(SafeAppendable appendable)
        {
            if (this.text.isEmpty())
            {
                // an empty literal writes nothing, and still has to be written: it is how an
                // alternative that renders nothing is spelled, and writing nothing for it would
                // leave an alternative with no elements, which is not a format string at all
                appendable.append("\"\"");
                return;
            }

            // separators stand for themselves, so text made only of them is written bare; anything
            // else is quoted whole rather than split into runs, which would turn " at " into the
            // three pieces " "at" " and make a literal harder to read the more ordinary it is
            int length = this.text.length();
            for (int i = 0; i < length; i++)
            {
                if (!isSeparator(this.text.charAt(i)))
                {
                    appendQuoted(appendable);
                    return;
                }
            }
            appendable.append(this.text);
        }

        private void appendQuoted(SafeAppendable appendable)
        {
            appendable.append('"');
            for (int i = 0; i < this.text.length(); i++)
            {
                char character = this.text.charAt(i);
                if ((character == '"') || (character == '\\'))
                {
                    appendable.append('\\');
                }
                appendable.append(character);
            }
            appendable.append('"');
        }

        @Override
        public boolean equals(Object other)
        {
            return (this == other) || ((other instanceof Literal) && this.text.equals(((Literal) other).text));
        }

        @Override
        public int hashCode()
        {
            return this.text.hashCode();
        }

        @Override
        public String toString()
        {
            return "Literal(\"" + this.text + "\")";
        }
    }

    /**
     * The year in full, however many digits it takes, with a minus sign before the era. A date
     * always has one, so this always writes.
     */
    private static final class Year extends Element
    {
        static final Year INSTANCE = new Year();

        private Year()
        {
        }

        @Override
        boolean canRender(PureDate date)
        {
            return true;
        }

        @Override
        void render(SafeAppendable appendable, PureDate date, ZonedDateTime zoned, String timeZoneId)
        {
            appendable.append((zoned == null) ? date.getYear() : zoned.getYear());
        }

        @Override
        void appendPattern(SafeAppendable appendable)
        {
            appendable.append("yyyy");
        }

        @Override
        public String toString()
        {
            return "Year";
        }
    }

    /**
     * The last two digits of the year, which a year before the era has as much as one after it: it
     * drops the sign along with the century, as it already drops the difference between 1914 and
     * 2014.
     */
    private static final class TwoDigitYear extends Element
    {
        static final TwoDigitYear INSTANCE = new TwoDigitYear();

        private TwoDigitYear()
        {
        }

        @Override
        boolean canRender(PureDate date)
        {
            return true;
        }

        @Override
        void render(SafeAppendable appendable, PureDate date, ZonedDateTime zoned, String timeZoneId)
        {
            int year = (zoned == null) ? date.getYear() : zoned.getYear();
            DateFormat.appendNonNegTwoDigitInt(appendable, Math.abs(year % 100));
        }

        @Override
        void appendPattern(SafeAppendable appendable)
        {
            appendable.append("yy");
        }

        @Override
        public String toString()
        {
            return "TwoDigitYear";
        }
    }

    /**
     * A component the date carries as a number, written zero padded to a minimum width.
     */
    private static final class NumericComponent extends Element
    {
        private final Component component;
        private final int minDigits;

        NumericComponent(Component component, int minDigits)
        {
            this.component = component;
            this.minDigits = minDigits;
        }

        @Override
        boolean canRender(PureDate date)
        {
            return this.component.has(date);
        }

        @Override
        void render(SafeAppendable appendable, PureDate date, ZonedDateTime zoned, String timeZoneId)
        {
            this.component.requireOf(date);
            DateFormat.appendZeroPaddedInt(appendable, this.component.get(date, zoned), this.minDigits);
        }

        @Override
        void appendPattern(SafeAppendable appendable)
        {
            for (int i = 0; i < this.minDigits; i++)
            {
                appendable.append(this.component.getControlCharacter());
            }
        }

        @Override
        public boolean equals(Object other)
        {
            if (this == other)
            {
                return true;
            }
            if (!(other instanceof NumericComponent))
            {
                return false;
            }
            NumericComponent that = (NumericComponent) other;
            return (this.component == that.component) && (this.minDigits == that.minDigits);
        }

        @Override
        public int hashCode()
        {
            return (31 * this.component.hashCode()) + this.minDigits;
        }

        @Override
        public String toString()
        {
            return this.component + "(" + this.minDigits + ")";
        }
    }

    /**
     * Whether the hour is before or after noon.
     */
    private static final class AmPm extends Element
    {
        static final AmPm INSTANCE = new AmPm();

        private AmPm()
        {
        }

        @Override
        boolean canRender(PureDate date)
        {
            return Component.HOUR_24.has(date);
        }

        @Override
        void render(SafeAppendable appendable, PureDate date, ZonedDateTime zoned, String timeZoneId)
        {
            Component.HOUR_24.requireOf(date);
            appendable.append((Component.HOUR_24.get(date, zoned) < 12) ? "AM" : "PM");
        }

        @Override
        void appendPattern(SafeAppendable appendable)
        {
            appendable.append('a');
        }

        @Override
        public String toString()
        {
            return "AmPm";
        }
    }

    /**
     * The name of the pattern's time zone, as the pattern was given it, or {@code GMT} where no
     * zone was named. The pattern already holds that name, so this reads it from the rendering
     * rather than keeping a copy, and one of these is like every other.
     */
    private static final class TimeZoneName extends Element
    {
        static final TimeZoneName INSTANCE = new TimeZoneName();

        private TimeZoneName()
        {
        }

        @Override
        boolean canRender(PureDate date)
        {
            return true;
        }

        @Override
        void render(SafeAppendable appendable, PureDate date, ZonedDateTime zoned, String timeZoneId)
        {
            appendable.append((timeZoneId == null) ? "GMT" : timeZoneId);
        }

        @Override
        void appendPattern(SafeAppendable appendable)
        {
            appendable.append('z');
        }

        @Override
        public String toString()
        {
            return "TimeZoneName";
        }
    }

    /**
     * The offset from UTC the date is at, in the RFC 822 notation, which is a sign, two digits of
     * hours, and two of minutes, with nothing left out: UTC is written {@code +0000}. An offset
     * belongs to an instant, so a date with no hour has none to write.
     */
    private static final class RFC822TimeZoneOffset extends Element
    {
        static final RFC822TimeZoneOffset INSTANCE = new RFC822TimeZoneOffset();

        private RFC822TimeZoneOffset()
        {
        }

        @Override
        boolean canRender(PureDate date)
        {
            return date.hasHour();
        }

        @Override
        void render(SafeAppendable appendable, PureDate date, ZonedDateTime zoned, String timeZoneId)
        {
            requireHourForOffset(date, 'Z');
            DateFormat.appendRFC822TimeZone(appendable, DateFormat.getOffsetInMinutes(zoned));
        }

        @Override
        void appendPattern(SafeAppendable appendable)
        {
            appendable.append('Z');
        }

        @Override
        public String toString()
        {
            return "RFC822TimeZoneOffset";
        }
    }

    /**
     * The offset from UTC the date is at, in the ISO 8601 notation, which writes UTC as {@code Z}
     * and leaves out the minutes of an offset that has none. An offset belongs to an instant, so a
     * date with no hour has none to write.
     */
    private static final class ISO8601TimeZoneOffset extends Element
    {
        static final ISO8601TimeZoneOffset INSTANCE = new ISO8601TimeZoneOffset();

        private ISO8601TimeZoneOffset()
        {
        }

        @Override
        boolean canRender(PureDate date)
        {
            return date.hasHour();
        }

        @Override
        void render(SafeAppendable appendable, PureDate date, ZonedDateTime zoned, String timeZoneId)
        {
            requireHourForOffset(date, 'X');
            DateFormat.appendISO8601TimeZone(appendable, DateFormat.getOffsetInMinutes(zoned));
        }

        @Override
        void appendPattern(SafeAppendable appendable)
        {
            appendable.append('X');
        }

        @Override
        public String toString()
        {
            return "ISO8601TimeZoneOffset";
        }
    }

    /**
     * The fraction of a second the date carries, written at a width this describes.
     * {@link SubsecondBuilder} is how one is asked for.
     *
     * <p>A sub-second field is a <em>width</em>, and says nothing about whether the date has a
     * fraction at all: it always fails on a date that has none, exactly as asking for the month
     * fails on a date with no month. What to write for a date with no fraction is a question for
     * whatever surrounds the field rather than for the field.
     *
     * <p>Given the digits the date stores, the field writes, in this order:
     *
     * <ol>
     * <li>nothing, failing, where the date has no fraction at all;</li>
     * <li>the first maximum digits, where there are more than that, or nothing, failing, where the
     * field fails above its maximum;</li>
     * <li>followed by enough of the fill character to reach the minimum, where it falls short of
     * it, or nothing, failing, where the field fails below its minimum.</li>
     * </ol>
     *
     * <p>Truncating and padding are not two sides of one thing. A Pure date is a span of time
     * rather than an instant, and its stored digit count is the precision of that span, so
     * {@code .07} <em>contains</em> {@code .070}, {@code .071}, and {@code .0712}. Truncating
     * widens the span, which loses precision but stays true; padding narrows it, which claims a
     * precision the date does not have. Both are worth having, and only one is worth being careful
     * with.
     */
    private static final class Subsecond extends Element
    {
        static final int UNBOUNDED = Integer.MAX_VALUE;

        private static final char DEFAULT_FILL = '0';

        private final int minimum;
        private final int maximum;
        private final boolean failBelow;
        private final boolean failAbove;
        private final char fill;

        Subsecond(int minimum, int maximum, boolean failBelow, boolean failAbove, char fill)
        {
            this.minimum = minimum;
            this.maximum = maximum;
            this.failBelow = failBelow;
            this.failAbove = failAbove;
            this.fill = fill;
        }

        @Override
        boolean canRender(PureDate date)
        {
            if (!date.hasSubsecond())
            {
                return false;
            }
            int length = date.getSubsecond().length();
            return !(this.failAbove && (length > this.maximum)) &&
                    !(this.failBelow && (Math.min(length, this.maximum) < this.minimum));
        }

        @Override
        void render(SafeAppendable appendable, PureDate date, ZonedDateTime zoned, String timeZoneId)
        {
            if (!date.hasSubsecond())
            {
                throw new IllegalArgumentException("Date has no sub-second: " + date);
            }

            String subsecond = date.getSubsecond();
            int length = subsecond.length();
            if (length > this.maximum)
            {
                if (this.failAbove)
                {
                    throw new IllegalArgumentException("Date has a " + length + " digit sub-second, but at most " + this.maximum + " may be written: " + date);
                }
                length = this.maximum;
            }
            // fill goes on the right, since sub-second digits run most significant first: .07
            // padded to three digits is .070, not .007
            int padding = 0;
            if (length < this.minimum)
            {
                if (this.failBelow)
                {
                    throw new IllegalArgumentException("Date has a " + length + " digit sub-second, but " + this.minimum + " are required: " + date);
                }
                padding = this.minimum - length;
            }

            appendable.append(subsecond, 0, length);
            for (int i = 0; i < padding; i++)
            {
                appendable.append(this.fill);
            }
        }

        @Override
        void appendPattern(SafeAppendable appendable)
        {
            // a shorthand says in two or three characters what the general form spells out, so a
            // field one of them covers exactly is written that way; the repetition form is never
            // written, since every field it can spell has a shorthand that means precisely the same
            // thing and it is the form to be deprecated
            if (this.fill == DEFAULT_FILL)
            {
                if (!this.failBelow && !this.failAbove)
                {
                    if (this.minimum == 0)
                    {
                        appendable.append('S');
                        if (this.maximum == UNBOUNDED)
                        {
                            appendable.append('*');
                        }
                        else
                        {
                            appendable.append('<').append(this.maximum);
                        }
                        return;
                    }
                    if (this.maximum == UNBOUNDED)
                    {
                        appendable.append("S>").append(this.minimum);
                        return;
                    }
                    if (this.minimum == this.maximum)
                    {
                        appendable.append('S').append(this.minimum);
                        return;
                    }
                }
                else if (this.failBelow && !this.failAbove && (this.minimum == this.maximum))
                {
                    appendable.append("S!").append(this.minimum);
                    return;
                }
            }

            appendable.append("S(").append(this.minimum);
            if (this.failBelow)
            {
                appendable.append('!');
            }
            appendable.append(',');
            if (this.maximum == UNBOUNDED)
            {
                appendable.append('*');
            }
            else
            {
                appendable.append(this.maximum);
            }
            if (this.failAbove)
            {
                appendable.append('!');
            }
            if (this.fill != DEFAULT_FILL)
            {
                appendable.append(",\"");
                if ((this.fill == '"') || (this.fill == '\\'))
                {
                    appendable.append('\\');
                }
                appendable.append(this.fill).append('"');
            }
            appendable.append(')');
        }

        @Override
        public boolean equals(Object other)
        {
            if (this == other)
            {
                return true;
            }
            if (!(other instanceof Subsecond))
            {
                return false;
            }
            Subsecond that = (Subsecond) other;
            return (this.minimum == that.minimum) &&
                    (this.maximum == that.maximum) &&
                    (this.failBelow == that.failBelow) &&
                    (this.failAbove == that.failAbove) &&
                    (this.fill == that.fill);
        }

        @Override
        public int hashCode()
        {
            int hashCode = this.minimum;
            hashCode = (31 * hashCode) + this.maximum;
            hashCode = (31 * hashCode) + (this.failBelow ? 1 : 0);
            hashCode = (31 * hashCode) + (this.failAbove ? 1 : 0);
            return (31 * hashCode) + this.fill;
        }

        @Override
        public String toString()
        {
            StringBuilder builder = new StringBuilder("Subsecond(").append(this.minimum);
            if (this.failBelow)
            {
                builder.append('!');
            }
            builder.append(", ");
            if (this.maximum == UNBOUNDED)
            {
                builder.append('*');
            }
            else
            {
                builder.append(this.maximum);
            }
            if (this.failAbove)
            {
                builder.append('!');
            }
            if (this.fill != DEFAULT_FILL)
            {
                builder.append(", '").append(this.fill).append('\'');
            }
            return builder.append(')').toString();
        }

        /**
         * Return what is wrong with a pair of bounds, or null where some width satisfies both. This
         * is the only thing a field is rejected for: the policies and the fill are markers rather
         * than requirements, and cannot make a field that means nothing.
         *
         * @param minimum fewest digits to write
         * @param maximum most digits to write
         * @return what is wrong with the bounds, or null
         */
        static String widthProblem(int minimum, int maximum)
        {
            if (minimum < 0)
            {
                return "Sub-second minimum may not be negative: " + minimum;
            }
            if (maximum < 1)
            {
                return "Sub-second maximum must be at least 1: " + maximum;
            }
            if (minimum > maximum)
            {
                return "Sub-second minimum " + minimum + " exceeds maximum " + maximum;
            }
            return null;
        }

        /**
         * Check that some width satisfies both bounds, which is what a caller with nothing further
         * to say about where the bounds came from does.
         *
         * @param minimum fewest digits to write
         * @param maximum most digits to write
         */
        static void checkWidth(int minimum, int maximum)
        {
            String problem = widthProblem(minimum, maximum);
            if (problem != null)
            {
                throw new IllegalArgumentException(problem);
            }
        }
    }

    /**
     * A run of the pattern written where the date can carry it and left out where it cannot, with
     * alternatives to choose between.
     *
     * <p>A section writes the first of its alternatives every element of which can write the date,
     * and writes nothing at all where none of them can. It therefore never fails, whatever it holds:
     * a section is the one construct that turns a date the pattern cannot carry into silence rather
     * than into an error. That is also what it costs - a throwing sub-second bound inside a section
     * selects the next alternative where outside one it would raise.
     *
     * <p>A section nested inside another asks the date for nothing on its own account, since it can
     * always write. {@code ?[" at "?[HH:mm]]} on a date with no hour therefore writes {@code " at "}
     * and stops: the outer alternative holds a literal and a section, and both of those can always
     * write. Hoisting the literal out, as {@code ?[" at "HH:mm]}, is what makes the two rise and
     * fall together. The alternative - letting a nested section refuse on its parent's behalf -
     * would make viability depend on a whole subtree, and would make {@code ?[X]} and
     * {@code ?[X|""]} mean different things.
     */
    private static final class OptionalSection extends Element
    {
        private final Element[][] alternatives;

        OptionalSection(Element[][] alternatives)
        {
            this.alternatives = alternatives;
        }

        @Override
        boolean canRender(PureDate date)
        {
            return true;
        }

        @Override
        void render(SafeAppendable appendable, PureDate date, ZonedDateTime zoned, String timeZoneId)
        {
            for (Element[] alternative : this.alternatives)
            {
                if (canRenderAll(alternative, date))
                {
                    for (Element element : alternative)
                    {
                        element.render(appendable, date, zoned, timeZoneId);
                    }
                    return;
                }
            }
        }

        @Override
        void appendPattern(SafeAppendable appendable)
        {
            appendable.append("?[");
            for (int i = 0; i < this.alternatives.length; i++)
            {
                if (i > 0)
                {
                    appendable.append('|');
                }
                for (Element element : this.alternatives[i])
                {
                    element.appendPattern(appendable);
                }
            }
            appendable.append(']');
        }

        @Override
        public boolean equals(Object other)
        {
            return (this == other) ||
                    ((other instanceof OptionalSection) &&
                            Arrays.deepEquals(this.alternatives, ((OptionalSection) other).alternatives));
        }

        @Override
        public int hashCode()
        {
            return Arrays.deepHashCode(this.alternatives);
        }

        @Override
        public String toString()
        {
            StringBuilder builder = new StringBuilder("OptionalSection(");
            for (int i = 0; i < this.alternatives.length; i++)
            {
                if (i > 0)
                {
                    builder.append(" | ");
                }
                for (int j = 0; j < this.alternatives[i].length; j++)
                {
                    if (j > 0)
                    {
                        builder.append(", ");
                    }
                    builder.append(this.alternatives[i][j]);
                }
            }
            return builder.append(')').toString();
        }

        /**
         * Return whether every element of an alternative can write the given date, which is what
         * makes the alternative the one to write.
         *
         * @param elements elements of the alternative
         * @param date     date to write
         * @return whether the alternative can be written
         */
        private static boolean canRenderAll(Element[] elements, PureDate date)
        {
            for (Element element : elements)
            {
                if (!element.canRender(date))
                {
                    return false;
                }
            }
            return true;
        }
    }

    /**
     * Assembles a pattern element by element. A builder may be built from more than once, and each
     * pattern it gives out stands on its own.
     *
     * <p>A sub-second field is described by a builder of its own, which {@link #subsecond()} gives
     * out and {@link SubsecondBuilder#endSubsecond()} hands back. Nothing reaches the pattern until
     * the field is finished, so a field is either wholly described or not there at all, and a
     * pattern may hold as many of them as it likes.
     *
     * <p>An optional section is described in place instead, since everything it can hold is what
     * the builder already writes: {@link #optional()} opens one, {@link #or()} begins its next
     * alternative, and {@link #endOptional()} closes it. Elements added while a section is open
     * belong to the alternative being described, and sections may be nested.
     */
    public static final class Builder
    {
        private static final int DEFAULT_DIGITS = 2;

        private final List<Element> elements = new ArrayList<>();
        private final List<OpenSection> openSections = new ArrayList<>();
        private final StringBuilder pendingLiteral = new StringBuilder();
        private SubsecondBuilder openSubsecond;
        private String timeZoneId;
        private ZoneId timeZone;

        private Builder()
        {
        }

        /**
         * Render dates in the given time zone, rather than as the UTC times they are. A date with
         * no hour is not an instant and is never shifted, whatever zone is named here.
         *
         * <p>The zone may be named in any form {@link TimeZones#parse(String)} takes - a region
         * such as {@code America/New_York}, one of the three letter abbreviations such as
         * {@code EST}, or an offset such as {@code GMT+5} - and the name as given here is what
         * {@link #timeZoneName} writes. Naming a zone twice keeps the second, and the name is
         * resolved when the pattern is built.
         *
         * @param timeZoneId time zone name
         * @return this builder
         */
        public Builder timeZone(String timeZoneId)
        {
            requireNoOpenSubsecond();
            this.timeZoneId = timeZoneId;
            if (timeZoneId == null)
            {
                this.timeZone = null;
            }
            else if ((this.timeZone != null) && !timeZoneId.equals(this.timeZone.getId()))
            {
                this.timeZone = null;
            }
            return this;
        }

        /**
         * Render dates in the given time zone, rather than as the UTC times they are.
         *
         * @param timeZone time zone
         * @return this builder
         */
        public Builder timeZone(ZoneId timeZone)
        {
            requireNoOpenSubsecond();
            if (timeZone == null)
            {
                this.timeZone = null;
                this.timeZoneId = null;
            }
            else
            {
                this.timeZone = timeZone;
                this.timeZoneId = timeZone.getId();
            }
            return this;
        }

        /**
         * Write the year in full, however many digits it takes, with a minus sign before the era.
         *
         * @return this builder
         */
        public Builder year()
        {
            return element(Year.INSTANCE);
        }

        /**
         * Write the last two digits of the year, which drop the sign along with the century.
         *
         * @return this builder
         */
        public Builder twoDigitYear()
        {
            return element(TwoDigitYear.INSTANCE);
        }

        /**
         * Write the month, zero padded to two digits.
         *
         * @return this builder
         */
        public Builder month()
        {
            return month(DEFAULT_DIGITS);
        }

        /**
         * Write the month, zero padded to the given width.
         *
         * @param minDigits fewest digits to write
         * @return this builder
         */
        public Builder month(int minDigits)
        {
            return component(Component.MONTH, minDigits);
        }

        /**
         * Write the day of the month, zero padded to two digits.
         *
         * @return this builder
         */
        public Builder day()
        {
            return day(DEFAULT_DIGITS);
        }

        /**
         * Write the day of the month, zero padded to the given width.
         *
         * @param minDigits fewest digits to write
         * @return this builder
         */
        public Builder day(int minDigits)
        {
            return component(Component.DAY, minDigits);
        }

        /**
         * Write the hour on the 24 hour clock, zero padded to two digits.
         *
         * @return this builder
         */
        public Builder hour24()
        {
            return hour24(DEFAULT_DIGITS);
        }

        /**
         * Write the hour on the 24 hour clock, zero padded to the given width.
         *
         * @param minDigits fewest digits to write
         * @return this builder
         */
        public Builder hour24(int minDigits)
        {
            return component(Component.HOUR_24, minDigits);
        }

        /**
         * Write the hour on the 12 hour clock, zero padded to two digits, with midnight and noon
         * both written as 12.
         *
         * @return this builder
         */
        public Builder hour12()
        {
            return hour12(DEFAULT_DIGITS);
        }

        /**
         * Write the hour on the 12 hour clock, zero padded to the given width, with midnight and
         * noon both written as 12.
         *
         * @param minDigits fewest digits to write
         * @return this builder
         */
        public Builder hour12(int minDigits)
        {
            return component(Component.HOUR_12, minDigits);
        }

        /**
         * Write whether the hour is before or after noon, as {@code AM} or {@code PM}.
         *
         * @return this builder
         */
        public Builder amPm()
        {
            return element(AmPm.INSTANCE);
        }

        /**
         * Write the minute, zero padded to two digits.
         *
         * @return this builder
         */
        public Builder minute()
        {
            return minute(DEFAULT_DIGITS);
        }

        /**
         * Write the minute, zero padded to the given width.
         *
         * @param minDigits fewest digits to write
         * @return this builder
         */
        public Builder minute(int minDigits)
        {
            return component(Component.MINUTE, minDigits);
        }

        /**
         * Write the second, zero padded to two digits.
         *
         * @return this builder
         */
        public Builder second()
        {
            return second(DEFAULT_DIGITS);
        }

        /**
         * Write the second, zero padded to the given width.
         *
         * @param minDigits fewest digits to write
         * @return this builder
         */
        public Builder second(int minDigits)
        {
            return component(Component.SECOND, minDigits);
        }

        /**
         * Begin a sub-second field, which the builder this gives out describes and its
         * {@link SubsecondBuilder#endSubsecond()} adds. Nothing reaches the pattern until then, so
         * a field is either wholly described or not there at all.
         *
         * <p>As every sub-second field does, the field this begins fails on a date carrying no
         * fraction at all, whatever width it ends up with.
         *
         * @return builder for the field
         */
        public SubsecondBuilder subsecond()
        {
            requireNoOpenSubsecond();
            this.openSubsecond = new SubsecondBuilder(this);
            return this.openSubsecond;
        }

        /**
         * Write the name of the pattern's time zone, as {@link #timeZone} was given it, or
         * {@code GMT} where no zone was named. The zone may be named before or after this.
         *
         * @return this builder
         */
        public Builder timeZoneName()
        {
            return element(TimeZoneName.INSTANCE);
        }

        /**
         * Write the offset from UTC in the RFC 822 notation, which is a sign, two digits of hours,
         * and two of minutes, with nothing left out: UTC is written {@code +0000}. An offset
         * belongs to an instant, so this fails on a date with no hour.
         *
         * @return this builder
         */
        public Builder rfc822TimeZoneOffset()
        {
            return element(RFC822TimeZoneOffset.INSTANCE);
        }

        /**
         * Write the offset from UTC in the ISO 8601 notation, which writes UTC as {@code Z} and
         * leaves out the minutes of an offset that has none. An offset belongs to an instant, so
         * this fails on a date with no hour.
         *
         * @return this builder
         */
        public Builder iso8601TimeZoneOffset()
        {
            return element(ISO8601TimeZoneOffset.INSTANCE);
        }

        /**
         * Write the given character as it stands.
         *
         * @param character character to write
         * @return this builder
         */
        public Builder literal(char character)
        {
            requireNoOpenSubsecond();
            this.pendingLiteral.append(character);
            return this;
        }

        /**
         * Write the given text as it stands. Unlike a format string, which has to quote text and
         * escape the quotes within it, this takes any text at all.
         *
         * @param text text to write
         * @return this builder
         */
        public Builder literal(String text)
        {
            requireNoOpenSubsecond();
            this.pendingLiteral.append(text);
            return this;
        }

        /**
         * Open an optional section, which writes the first of its alternatives the date can carry
         * and nothing at all where the date can carry none of them. Elements added from here on
         * belong to the section's first alternative, {@link #or()} begins the next one, and
         * {@link #endOptional()} closes the section. Sections may be nested.
         *
         * <pre>
         * builder.year().optional().literal('-').month().endOptional()
         * </pre>
         *
         * @return this builder
         * @throws IllegalStateException if a sub-second field is still open
         */
        public Builder optional()
        {
            requireNoOpenSubsecond();
            flushLiteral();
            this.openSections.add(new OpenSection());
            return this;
        }

        /**
         * End the alternative being described and begin the next one.
         *
         * @return this builder
         * @throws IllegalStateException if a sub-second field is still open, if no optional section
         *                               is open, or if the alternative being ended holds nothing
         */
        public Builder or()
        {
            requireNoOpenSubsecond();
            endAlternative(requireOpenSection("or()"));
            return this;
        }

        /**
         * Close the optional section being described, adding it to whatever surrounds it.
         *
         * @return this builder
         * @throws IllegalStateException if a sub-second field is still open, if no optional section
         *                               is open, or if the alternative being ended holds nothing
         */
        public Builder endOptional()
        {
            requireNoOpenSubsecond();
            OpenSection section = requireOpenSection("endOptional()");
            endAlternative(section);
            this.openSections.remove(this.openSections.size() - 1);
            return add(new OptionalSection(section.alternatives.toArray(new Element[0][])));
        }

        /**
         * Build the pattern.
         *
         * @return pattern
         * @throws IllegalArgumentException if a time zone was named that cannot be resolved
         * @throws IllegalStateException if a sub-second field or an optional section is still open
         */
        public DateFormatPattern build()
        {
            requireNoOpenSubsecond();
            if (!this.openSections.isEmpty())
            {
                throw new IllegalStateException("An optional section is still open: call endOptional() before anything else");
            }
            flushLiteral();
            if ((this.timeZoneId != null) && (this.timeZone == null))
            {
                this.timeZone = TimeZones.parse(this.timeZoneId);
            }
            return new DateFormatPattern(this.elements.toArray(new Element[0]), this.timeZone, this.timeZoneId);
        }

        private Builder component(Component component, int minDigits)
        {
            if (minDigits < 1)
            {
                throw new IllegalArgumentException("Width must be at least 1 digit: " + minDigits);
            }
            return element(new NumericComponent(component, minDigits));
        }

        private Builder element(Element element)
        {
            requireNoOpenSubsecond();
            return add(element);
        }

        /**
         * Add an element without asking whether a sub-second field is open, which is how the one
         * call that ends such a field adds it.
         *
         * @param element element to add
         * @return this builder
         */
        private Builder add(Element element)
        {
            flushLiteral();
            currentElements().add(element);
            return this;
        }

        /**
         * Refuse to do anything else while a sub-second field is part way through being described.
         * Quietly ending it would commit a field its author may not have finished writing, and
         * there is no reading of the half-written chain that says which they meant.
         */
        private void requireNoOpenSubsecond()
        {
            if (this.openSubsecond != null)
            {
                throw new IllegalStateException("A sub-second field is still open: call endSubsecond() before anything else");
            }
        }

        private OpenSection requireOpenSection(String call)
        {
            if (this.openSections.isEmpty())
            {
                throw new IllegalStateException("No optional section is open: call optional() before " + call);
            }
            return this.openSections.get(this.openSections.size() - 1);
        }

        /**
         * End the alternative a section is describing. An alternative holding nothing is refused
         * rather than dropped: one at the front would make the whole section write nothing, which
         * is a silent bug, and one at the back says what the section without it already says.
         *
         * @param section section being described
         */
        private void endAlternative(OpenSection section)
        {
            flushLiteral();
            if (section.current.isEmpty())
            {
                throw new IllegalStateException("An optional section may not hold an alternative with nothing in it");
            }
            section.alternatives.add(section.current.toArray(new Element[0]));
            section.current.clear();
        }

        /**
         * Return the elements being added to, which is the alternative of the innermost open
         * section, or the pattern itself where no section is open.
         *
         * @return elements being added to
         */
        private List<Element> currentElements()
        {
            return this.openSections.isEmpty()
                    ? this.elements
                    : this.openSections.get(this.openSections.size() - 1).current;
        }

        private boolean currentAlternativeIsEmpty()
        {
            return (this.pendingLiteral.length() == 0) && currentElements().isEmpty();
        }

        private void flushLiteral()
        {
            if (this.pendingLiteral.length() > 0)
            {
                currentElements().add(new Literal(this.pendingLiteral.toString()));
                this.pendingLiteral.setLength(0);
            }
        }
    }

    /**
     * An optional section part way through being described: the alternatives already ended, and the
     * elements of the one being described now.
     */
    private static final class OpenSection
    {
        private final List<Element[]> alternatives = new ArrayList<>();
        private final List<Element> current = new ArrayList<>();
    }

    /**
     * Describes one sub-second field, and adds it to the pattern it came from when
     * {@link #endSubsecond()} is called. Nothing it is told reaches that pattern before then, so a
     * field is either wholly described or not there at all.
     *
     * <p>A field with nothing said about its width writes however many digits the date has, which
     * is what {@link #asStored()} says outright. One of {@link #asStored()}, {@link #exactly},
     * {@link #atMost}, {@link #atLeast} and {@link #between} sets the width, the last such call
     * winning; {@link #failBelowMinimum()}, {@link #failAboveMaximum()} and {@link #padWith} then
     * say what to do about a date the width does not fit.
     *
     * <p>While a field is open the pattern it belongs to will do nothing else, so the two cannot be
     * interleaved, and a field cannot be ended twice.
     *
     * <pre>
     * builder.literal('.').subsecond().between(3, 9).failBelowMinimum().endSubsecond()
     * </pre>
     */
    public static final class SubsecondBuilder
    {
        private final Builder parent;
        private int minimum;
        private int maximum;
        private boolean failBelow;
        private boolean failAbove;
        private char fill;
        private boolean ended;

        private SubsecondBuilder(Builder parent)
        {
            this.parent = parent;
            this.minimum = 0;
            this.maximum = Subsecond.UNBOUNDED;
            this.fill = Subsecond.DEFAULT_FILL;
        }

        /**
         * Write however many digits of the fraction of a second the date has, which is what a field
         * writes where nothing is said about its width.
         *
         * @return this builder
         * @throws IllegalStateException if the field has already been ended
         */
        public SubsecondBuilder asStored()
        {
            return between(0, Subsecond.UNBOUNDED);
        }

        /**
         * Write exactly the given number of digits, cutting down a date carrying more and padding
         * out one carrying fewer.
         *
         * @param digits digits to write
         * @return this builder
         * @throws IllegalStateException if the field has already been ended
         */
        public SubsecondBuilder exactly(int digits)
        {
            return between(digits, digits);
        }

        /**
         * Write at most the given number of digits, and fewer where that is all the date has.
         *
         * @param maximum most digits to write
         * @return this builder
         * @throws IllegalStateException if the field has already been ended
         */
        public SubsecondBuilder atMost(int maximum)
        {
            return between(0, maximum);
        }

        /**
         * Write at least the given number of digits, padding out a date carrying fewer, and however
         * many more the date has.
         *
         * @param minimum fewest digits to write
         * @return this builder
         * @throws IllegalStateException if the field has already been ended
         */
        public SubsecondBuilder atLeast(int minimum)
        {
            return between(minimum, Subsecond.UNBOUNDED);
        }

        /**
         * Write between the given numbers of digits, cutting down a date carrying more and padding
         * out one carrying fewer.
         *
         * @param minimum fewest digits to write
         * @param maximum most digits to write
         * @return this builder
         * @throws IllegalArgumentException if no width satisfies both bounds
         * @throws IllegalStateException if the field has already been ended
         */
        public SubsecondBuilder between(int minimum, int maximum)
        {
            requireNotEnded();
            Subsecond.checkWidth(minimum, maximum);
            this.minimum = minimum;
            this.maximum = maximum;
            return this;
        }

        /**
         * Fail rather than pad out a fraction shorter than the minimum. A field with no minimum has
         * nothing to fail on.
         *
         * @return this builder
         * @throws IllegalStateException if the field has already been ended
         */
        public SubsecondBuilder failBelowMinimum()
        {
            requireNotEnded();
            this.failBelow = true;
            return this;
        }

        /**
         * Fail rather than cut down a fraction longer than the maximum. A field with no maximum has
         * nothing to fail on.
         *
         * @return this builder
         * @throws IllegalStateException if the field has already been ended
         */
        public SubsecondBuilder failAboveMaximum()
        {
            requireNotEnded();
            this.failAbove = true;
            return this;
        }

        /**
         * Pad a fraction shorter than the minimum with the given character rather than with zeros.
         *
         * @param fill fill character
         * @return this builder
         * @throws IllegalStateException if the field has already been ended
         */
        public SubsecondBuilder padWith(char fill)
        {
            requireNotEnded();
            this.fill = fill;
            return this;
        }

        /**
         * Add the field to the pattern, and give back the builder it belongs to. The field is
         * finished with, and nothing more may be said about it.
         *
         * @return the builder this field was begun from
         * @throws IllegalStateException if the field has already been ended
         */
        public Builder endSubsecond()
        {
            requireNotEnded();
            this.ended = true;
            this.parent.openSubsecond = null;
            return this.parent.add(new Subsecond(this.minimum, this.maximum, this.failBelow, this.failAbove, this.fill));
        }

        private void requireNotEnded()
        {
            if (this.ended)
            {
                throw new IllegalStateException("This sub-second field has already been ended");
            }
        }
    }

    /**
     * A component the date carries as a number, with the two ways of reading it - from the date as
     * it stands, and from the date as a time zone reads it - what to say where the date does not
     * carry it at all, and the character a format string asks for it by.
     */
    private enum Component
    {
        MONTH("Month", "month", 'M', PureDate::hasMonth, PureDate::getMonth, ZonedDateTime::getMonthValue),
        DAY("Day", "day", 'd', PureDate::hasDay, PureDate::getDay, ZonedDateTime::getDayOfMonth),
        HOUR_24("Hour24", "hour", 'H', PureDate::hasHour, PureDate::getHour, ZonedDateTime::getHour),
        HOUR_12("Hour12", "hour", 'h', PureDate::hasHour, date -> onTwelveHourClock(date.getHour()), zoned -> onTwelveHourClock(zoned.getHour())),
        MINUTE("Minute", "minute", 'm', PureDate::hasMinute, PureDate::getMinute, ZonedDateTime::getMinute),
        SECOND("Second", "second", 's', PureDate::hasSecond, PureDate::getSecond, ZonedDateTime::getSecond);

        private final String label;
        private final String name;
        private final char controlCharacter;
        private final Predicate<PureDate> present;
        private final ToIntFunction<PureDate> fromDate;
        private final ToIntFunction<ZonedDateTime> fromZoned;

        Component(String label, String name, char controlCharacter, Predicate<PureDate> present, ToIntFunction<PureDate> fromDate, ToIntFunction<ZonedDateTime> fromZoned)
        {
            this.label = label;
            this.name = name;
            this.controlCharacter = controlCharacter;
            this.present = present;
            this.fromDate = fromDate;
            this.fromZoned = fromZoned;
        }

        boolean has(PureDate date)
        {
            return this.present.test(date);
        }

        void requireOf(PureDate date)
        {
            if (!this.present.test(date))
            {
                throw new IllegalArgumentException("Date has no " + this.name + ": " + date);
            }
        }

        int get(PureDate date, ZonedDateTime zoned)
        {
            return (zoned == null) ? this.fromDate.applyAsInt(date) : this.fromZoned.applyAsInt(zoned);
        }

        char getControlCharacter()
        {
            return this.controlCharacter;
        }

        @Override
        public String toString()
        {
            return this.label;
        }

        private static int onTwelveHourClock(int hour)
        {
            return (hour == 0) ? 12 : ((hour > 12) ? (hour - 12) : hour);
        }
    }

    /**
     * Reads a format string, or a portion of one, into a pattern. Everything the parser reports is
     * a property of the format string alone, so a string that parses will render any date carrying
     * the components it names.
     */
    private static final class Parser
    {
        private final String formatString;
        private final int start;
        private final int end;
        private final Builder builder = new Builder();
        private int index;
        private int sectionDepth;

        Parser(String formatString, int start, int end)
        {
            this.formatString = formatString;
            this.start = start;
            this.end = end;
            this.index = start;
        }

        DateFormatPattern parse()
        {
            while (this.index < this.end)
            {
                char character = this.formatString.charAt(this.index++);
                switch (character)
                {
                    case '[':
                    {
                        parseTimeZone();
                        break;
                    }
                    case 'y':
                    {
                        // the year is written in full only from four letters on, and is not padded
                        this.builder.element((runLength(character) < 4) ? TwoDigitYear.INSTANCE : Year.INSTANCE);
                        break;
                    }
                    case 'M':
                    {
                        this.builder.component(Component.MONTH, runLength(character));
                        break;
                    }
                    case 'd':
                    {
                        this.builder.component(Component.DAY, runLength(character));
                        break;
                    }
                    case 'h':
                    {
                        this.builder.component(Component.HOUR_12, runLength(character));
                        break;
                    }
                    case 'H':
                    {
                        this.builder.component(Component.HOUR_24, runLength(character));
                        break;
                    }
                    case 'm':
                    {
                        this.builder.component(Component.MINUTE, runLength(character));
                        break;
                    }
                    case 's':
                    {
                        this.builder.component(Component.SECOND, runLength(character));
                        break;
                    }
                    case 'a':
                    {
                        runLength(character);
                        this.builder.amPm();
                        break;
                    }
                    case 'S':
                    {
                        parseSubsecond();
                        break;
                    }
                    case 'z':
                    {
                        runLength(character);
                        this.builder.timeZoneName();
                        break;
                    }
                    case 'Z':
                    {
                        runLength(character);
                        this.builder.rfc822TimeZoneOffset();
                        break;
                    }
                    case 'X':
                    {
                        runLength(character);
                        this.builder.iso8601TimeZoneOffset();
                        break;
                    }
                    case '-':
                    case '/':
                    case ':':
                    case '.':
                    case ' ':
                    case '\t':
                    {
                        this.builder.literal(character);
                        break;
                    }
                    case '"':
                    {
                        parseQuotedText();
                        break;
                    }
                    case '?':
                    {
                        if (!takeIf('['))
                        {
                            throw new IllegalArgumentException("Expected '[' after '?' in format string: " + text());
                        }
                        this.builder.optional();
                        this.sectionDepth++;
                        break;
                    }
                    case '|':
                    {
                        if (this.sectionDepth == 0)
                        {
                            throw new IllegalArgumentException("'|' outside an optional section in format string: " + text());
                        }
                        requireNonEmptyAlternative();
                        this.builder.or();
                        break;
                    }
                    case ']':
                    {
                        if (this.sectionDepth == 0)
                        {
                            throw new IllegalArgumentException("Unmatched ']' in format string: " + text());
                        }
                        requireNonEmptyAlternative();
                        this.builder.endOptional();
                        this.sectionDepth--;
                        break;
                    }
                    default:
                    {
                        throw new IllegalArgumentException("Invalid format control character '" + character + "' in format string: " + text());
                    }
                }
            }
            if (this.sectionDepth > 0)
            {
                throw new IllegalArgumentException("Missing closing bracket for optional section in format string: " + text());
            }
            return this.builder.build();
        }

        /**
         * Refuse an alternative with nothing in it, which the builder would refuse too, so that the
         * error names the format string rather than the half-built pattern.
         */
        private void requireNonEmptyAlternative()
        {
            if (this.builder.currentAlternativeIsEmpty())
            {
                throw new IllegalArgumentException("Empty alternative in optional section in format string: " + text());
            }
        }

        /**
         * Consume the run of the given character from the current index, one of them having already
         * been taken, and return how many there were in all.
         *
         * @param character character being repeated
         * @return length of the run, counting the one already taken
         */
        private int runLength(char character)
        {
            int count = 1;
            while ((this.index < this.end) && (this.formatString.charAt(this.index) == character))
            {
                this.index++;
                count++;
            }
            return count;
        }

        /**
         * Read a sub-second field, the {@code S} having been taken. What follows it decides which of
         * the three spellings this is: a bracket opens the general form, one of the bound characters
         * opens a shorthand, and anything else - another {@code S} included - is the run of letters
         * the language has always had.
         */
        private void parseSubsecond()
        {
            if (this.index < this.end)
            {
                switch (this.formatString.charAt(this.index))
                {
                    case '(':
                    {
                        this.index++;
                        parseSubsecondGeneralForm();
                        return;
                    }
                    case '*':
                    {
                        this.index++;
                        addSubsecond(0, Subsecond.UNBOUNDED, false, false, Subsecond.DEFAULT_FILL);
                        return;
                    }
                    case '<':
                    {
                        this.index++;
                        addSubsecond(0, parseDigits("S<"), false, false, Subsecond.DEFAULT_FILL);
                        return;
                    }
                    case '>':
                    {
                        this.index++;
                        addSubsecond(parseDigits("S>"), Subsecond.UNBOUNDED, false, false, Subsecond.DEFAULT_FILL);
                        return;
                    }
                    case '!':
                    {
                        this.index++;
                        int digits = parseDigits("S!");
                        addSubsecond(digits, digits, true, false, Subsecond.DEFAULT_FILL);
                        return;
                    }
                    default:
                    {
                        if (isDigit(this.formatString.charAt(this.index)))
                        {
                            int digits = parseDigits("S");
                            addSubsecond(digits, digits, false, false, Subsecond.DEFAULT_FILL);
                            return;
                        }
                        break;
                    }
                }
            }

            // up to three letters cut the fraction down to that many digits; from four on the count
            // stops meaning anything and the whole fraction is written
            int count = runLength('S');
            addSubsecond(0, (count < 4) ? count : Subsecond.UNBOUNDED, false, false, Subsecond.DEFAULT_FILL);
        }

        /**
         * Read the body of a general form sub-second field, {@code S(} having been taken.
         */
        private void parseSubsecondGeneralForm()
        {
            int minimum = parseDigits("S(");
            boolean failBelow = takeIf('!');
            if (!takeIf(','))
            {
                throw new IllegalArgumentException("Expected ',' after the sub-second minimum in format string: " + text());
            }

            int maximum;
            if (takeIf('*'))
            {
                maximum = Subsecond.UNBOUNDED;
            }
            else if ((this.index < this.end) && isDigit(this.formatString.charAt(this.index)))
            {
                maximum = parseDigits(",");
            }
            else
            {
                throw new IllegalArgumentException("Expected a digit count or '*' for the sub-second maximum in format string: " + text());
            }
            boolean failAbove = takeIf('!');

            char fill = takeIf(',') ? parseSubsecondFill() : Subsecond.DEFAULT_FILL;
            if (!takeIf(')'))
            {
                throw new IllegalArgumentException("Missing closing parenthesis in format string: " + text());
            }
            addSubsecond(minimum, maximum, failBelow, failAbove, fill);
        }

        /**
         * Read the quoted fill character of a general form sub-second field. A backslash escapes the
         * character after it, so every character can be a fill, the quote and the backslash
         * included.
         *
         * @return fill character
         */
        private char parseSubsecondFill()
        {
            if (!takeIf('"'))
            {
                throw new IllegalArgumentException("Expected a quoted sub-second fill character in format string: " + text());
            }

            char fill = Subsecond.DEFAULT_FILL;
            int count = 0;
            boolean done = false;
            while (!done && (this.index < this.end))
            {
                char next = this.formatString.charAt(this.index++);
                if (next == '"')
                {
                    done = true;
                }
                else
                {
                    if ((next == '\\') && (this.index < this.end))
                    {
                        next = this.formatString.charAt(this.index++);
                    }
                    fill = next;
                    count++;
                }
            }
            if (!done)
            {
                throw new IllegalArgumentException("Missing closing quote in format string: " + text());
            }
            if (count != 1)
            {
                throw new IllegalArgumentException("Sub-second fill must be a single character in format string: " + text());
            }
            return fill;
        }

        /**
         * Read a run of digits as a count.
         *
         * @param after what the count was expected after, for the error where there is none
         * @return the count
         */
        private int parseDigits(String after)
        {
            if ((this.index >= this.end) || !isDigit(this.formatString.charAt(this.index)))
            {
                throw new IllegalArgumentException("Expected a digit count after '" + after + "' in format string: " + text());
            }

            long count = 0;
            while ((this.index < this.end) && isDigit(this.formatString.charAt(this.index)))
            {
                count = (count * 10) + (this.formatString.charAt(this.index++) - '0');
                if (count >= Subsecond.UNBOUNDED)
                {
                    // a bound of its own is what an unbounded maximum is written as, so a count that
                    // reaches the number standing for one cannot be told apart from it
                    throw new IllegalArgumentException("Sub-second digit count is too large in format string: " + text());
                }
            }
            return (int) count;
        }

        /**
         * Add a sub-second field, having read one, checking the bounds here so that the error names
         * the format string the caller wrote rather than the numbers it was read as.
         *
         * @param minimum   fewest digits to write
         * @param maximum   most digits to write
         * @param failBelow whether to fail rather than pad below the minimum
         * @param failAbove whether to fail rather than truncate above the maximum
         * @param fill      character to pad with
         */
        private void addSubsecond(int minimum, int maximum, boolean failBelow, boolean failAbove, char fill)
        {
            String problem = Subsecond.widthProblem(minimum, maximum);
            if (problem != null)
            {
                throw new IllegalArgumentException(problem + " in format string: " + text());
            }
            this.builder.element(new Subsecond(minimum, maximum, failBelow, failAbove, fill));
        }

        /**
         * Take the given character where it is the next one, and say whether it was.
         *
         * @param character character to take
         * @return whether it was there
         */
        private boolean takeIf(char character)
        {
            if ((this.index < this.end) && (this.formatString.charAt(this.index) == character))
            {
                this.index++;
                return true;
            }
            return false;
        }

        private void parseTimeZone()
        {
            if (this.index > (this.start + 1))
            {
                throw new IllegalArgumentException("Time zone can only be set at the beginning of the format string");
            }

            StringBuilder timeZoneId = new StringBuilder();
            boolean done = false;
            boolean escaped = false;
            boolean inQuotes = false;
            while (!done && (this.index < this.end))
            {
                char next = this.formatString.charAt(this.index++);
                if (escaped)
                {
                    timeZoneId.append(next);
                    escaped = false;
                }
                else if (next == '"')
                {
                    inQuotes = !inQuotes;
                }
                else if ((next == ']') && !inQuotes)
                {
                    done = true;
                }
                else if (next == '\\')
                {
                    escaped = true;
                }
                else
                {
                    timeZoneId.append(next);
                }
            }
            if (inQuotes)
            {
                throw new IllegalArgumentException("Missing closing quotes in time zone definition: " + text());
            }
            if (!done)
            {
                throw new IllegalArgumentException("Missing closing bracket in format string: " + text());
            }
            this.builder.timeZone(timeZoneId.toString());
        }

        private void parseQuotedText()
        {
            StringBuilder literal = new StringBuilder();
            boolean done = false;
            boolean escaped = false;
            while (!done && (this.index < this.end))
            {
                char next = this.formatString.charAt(this.index++);
                if (escaped)
                {
                    literal.append(next);
                    escaped = false;
                }
                else if (next == '"')
                {
                    done = true;
                }
                else if (next == '\\')
                {
                    escaped = true;
                }
                else
                {
                    literal.append(next);
                }
            }
            if (!done)
            {
                throw new IllegalArgumentException("Missing closing quote in format string: " + text());
            }
            if (literal.length() == 0)
            {
                // an empty literal writes nothing and is an element all the same, since it is how an
                // alternative that renders nothing is spelled
                this.builder.element(new Literal(""));
            }
            else
            {
                this.builder.literal(literal.toString());
            }
        }

        private String text()
        {
            return this.formatString.substring(this.start, this.end);
        }
    }

    /**
     * Refuse to write an offset for a date that has no hour. An offset belongs to an instant, and a
     * zone can be keeping more than one over a date that broad, so which one it is keeping is just
     * what knowing the time would have told us.
     *
     * @param date             date being rendered
     * @param controlCharacter character the offset was asked for by
     */
    private static void requireHourForOffset(PureDate date, char controlCharacter)
    {
        if (!date.hasHour())
        {
            throw new IllegalArgumentException("Date has no hour (required for " + controlCharacter + "): " + date);
        }
    }

    /**
     * Return whether a character stands for itself in a format string, rather than having to be
     * quoted.
     *
     * @param character character to test
     * @return whether the character is a separator
     */
    private static boolean isDigit(char character)
    {
        return (character >= '0') && (character <= '9');
    }

    private static boolean isSeparator(char character)
    {
        return (character == '-') || (character == '/') || (character == ':') ||
                (character == '.') || (character == ' ') || (character == '\t');
    }

    /**
     * Write a time zone name in the square brackets a format string opens with, escaping what would
     * otherwise close them.
     *
     * @param appendable appendable to write to
     * @param timeZoneId time zone name
     */
    private static void appendTimeZoneId(SafeAppendable appendable, String timeZoneId)
    {
        appendable.append('[');
        for (int i = 0; i < timeZoneId.length(); i++)
        {
            char character = timeZoneId.charAt(i);
            if ((character == ']') || (character == '"') || (character == '\\'))
            {
                appendable.append('\\');
            }
            appendable.append(character);
        }
        appendable.append(']');
    }
}
