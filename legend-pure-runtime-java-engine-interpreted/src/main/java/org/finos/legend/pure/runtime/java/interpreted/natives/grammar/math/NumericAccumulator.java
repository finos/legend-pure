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

package org.finos.legend.pure.runtime.java.interpreted.natives.grammar.math;

import java.math.BigDecimal;
import java.math.BigInteger;
import java.math.MathContext;

/**
 * Numeric accumulator for performing arithmetic operations.
 */
public class NumericAccumulator
{
    private NumberWrapper number;

    private NumericAccumulator(NumberWrapper number)
    {
        this.number = number;
    }

    /**
     * Get the current value of the accumulator.
     *
     * @return accumulator value
     */
    public Number getValue()
    {
        return this.number.getValue();
    }

    /**
     * Add the given number to the accumulator.
     *
     * @param number number to add
     */
    public void add(Number number)
    {
        if (number == null)
        {
            throw new NullPointerException("Cannot add null");
        }
        else if (number instanceof BigDecimal)
        {
            add((BigDecimal)number);
        }
        else if (number instanceof Double)
        {
            add(number.doubleValue());
        }
        else if (number instanceof Float)
        {
            add(number.floatValue());
        }
        else if (number instanceof BigInteger)
        {
            add((BigInteger)number);
        }
        else if (number instanceof Long)
        {
            add(number.longValue());
        }
        else if (number instanceof Integer)
        {
            add(number.intValue());
        }
        else
        {
            throw new IllegalArgumentException("Unhandled number: " + number);
        }
    }

    /**
     * Add the given number to the accumulator.
     *
     * @param number number to add
     */
    public void add(BigDecimal number)
    {
        this.number = this.number.add(number);
    }

    /**
     * Add the given number to the accumulator.
     *
     * @param number number to add
     */
    public void add(double number)
    {
        this.number = this.number.add(number);
    }

    /**
     * Add the given number to the accumulator.
     *
     * @param number number to add
     */
    public void add(float number)
    {
        this.number = this.number.add(number);
    }

    /**
     * Add the given number to the accumulator.
     *
     * @param number number to add
     */
    public void add(BigInteger number)
    {
        this.number = this.number.add(number);
    }

    /**
     * Add the given number to the accumulator.
     *
     * @param number number to add
     */
    public void add(long number)
    {
        this.number = this.number.add(number);
    }

    /**
     * Add the given number to the accumulator.
     *
     * @param number number to add
     */
    public void add(int number)
    {
        this.number = this.number.add(number);
    }

    /**
     * Subtract the given number from the accumulator.
     *
     * @param number number to subtract
     */
    public void subtract(Number number)
    {
        if (number == null)
        {
            throw new NullPointerException("Cannot subtract null");
        }
        else if (number instanceof BigDecimal)
        {
            subtract((BigDecimal)number);
        }
        else if (number instanceof Double)
        {
            subtract(number.doubleValue());
        }
        else if (number instanceof Float)
        {
            subtract(number.floatValue());
        }
        else if (number instanceof BigInteger)
        {
            subtract((BigInteger)number);
        }
        else if (number instanceof Long)
        {
            subtract(number.longValue());
        }
        else if (number instanceof Integer)
        {
            subtract(number.intValue());
        }
        else
        {
            throw new IllegalArgumentException("Unhandled number: " + number);
        }
    }

    /**
     * Subtract the given number from the accumulator.
     *
     * @param number number to subtract
     */
    public void subtract(BigDecimal number)
    {
        this.number = this.number.subtract(number);
    }

    /**
     * Subtract the given number from the accumulator.
     *
     * @param number number to subtract
     */
    public void subtract(double number)
    {
        this.number = this.number.subtract(number);
    }

    /**
     * Subtract the given number from the accumulator.
     *
     * @param number number to subtract
     */
    public void subtract(float number)
    {
        this.number = this.number.subtract(number);
    }

    /**
     * Subtract the given number from the accumulator.
     *
     * @param number number to subtract
     */
    public void subtract(BigInteger number)
    {
        this.number = this.number.subtract(number);
    }

    /**
     * Subtract the given number from the accumulator.
     *
     * @param number number to subtract
     */
    public void subtract(long number)
    {
        this.number = this.number.subtract(number);
    }

    /**
     * Subtract the given number from the accumulator.
     *
     * @param number number to subtract
     */
    public void subtract(int number)
    {
        this.number = this.number.subtract(number);
    }

    /**
     * Multiply the accumulator by the given number.
     *
     * @param number number to multiply by
     */
    public void multiply(Number number)
    {
        if (number == null)
        {
            throw new NullPointerException("Cannot multiply by null");
        }
        else if (number instanceof BigDecimal)
        {
            multiply((BigDecimal)number);
        }
        else if (number instanceof Double)
        {
            multiply(number.doubleValue());
        }
        else if (number instanceof Float)
        {
            multiply(number.floatValue());
        }
        else if (number instanceof BigInteger)
        {
            multiply((BigInteger)number);
        }
        else if (number instanceof Long)
        {
            multiply(number.longValue());
        }
        else if (number instanceof Integer)
        {
            multiply(number.intValue());
        }
        else
        {
            throw new IllegalArgumentException("Unhandled number: " + number);
        }
    }

    /**
     * Multiply the accumulator by the given number.
     *
     * @param number number to multiply by
     */
    public void multiply(BigDecimal number)
    {
        this.number = this.number.multiply(number);
    }

    /**
     * Multiply the accumulator by the given number.
     *
     * @param number number to multiply by
     */
    public void multiply(double number)
    {
        this.number = this.number.multiply(number);
    }

    /**
     * Multiply the accumulator by the given number.
     *
     * @param number number to multiply by
     */
    public void multiply(float number)
    {
        this.number = this.number.multiply(number);
    }

    /**
     * Multiply the accumulator by the given number.
     *
     * @param number number to multiply by
     */
    public void multiply(BigInteger number)
    {
        this.number = this.number.multiply(number);
    }

    /**
     * Multiply the accumulator by the given number.
     *
     * @param number number to multiply by
     */
    public void multiply(long number)
    {
        this.number = this.number.multiply(number);
    }

    /**
     * Multiply the accumulator by the given number.
     *
     * @param number number to multiply by
     */
    public void multiply(int number)
    {
        this.number = this.number.multiply(number);
    }

    /**
     * Divide the accumulator by the given number.
     *
     * @param number number to divide by
     */
    public void divide(Number number)
    {
        if (number == null)
        {
            throw new NullPointerException("Cannot divide by null");
        }
        else if (number instanceof BigDecimal)
        {
            divide((BigDecimal)number);
        }
        else if (number instanceof Double)
        {
            divide(number.doubleValue());
        }
        else if (number instanceof Float)
        {
            divide(number.floatValue());
        }
        else if (number instanceof BigInteger)
        {
            divide((BigInteger)number);
        }
        else if (number instanceof Long)
        {
            divide(number.longValue());
        }
        else if (number instanceof Integer)
        {
            divide(number.intValue());
        }
        else
        {
            throw new IllegalArgumentException("Unhandled number: " + number);
        }
    }

    /**
     * Divide the accumulator by the given number.
     *
     * @param number number to divide by
     */
    public void divide(BigDecimal number)
    {
        this.number = this.number.divide(number);
    }

    /**
     * Divide the accumulator by the given number.
     *
     * @param number number to divide by
     */
    public void divide(double number)
    {
        this.number = this.number.divide(number);
    }

    /**
     * Divide the accumulator by the given number.
     *
     * @param number number to divide by
     */
    public void divide(float number)
    {
        this.number = this.number.divide(number);
    }

    /**
     * Divide the accumulator by the given number.
     *
     * @param number number to divide by
     */
    public void divide(BigInteger number)
    {
        this.number = this.number.divide(number);
    }

    /**
     * Divide the accumulator by the given number.
     *
     * @param number number to divide by
     */
    public void divide(long number)
    {
        this.number = this.number.divide(number);
    }

    /**
     * Divide the accumulator by the given number.
     *
     * @param number number to divide by
     */
    public void divide(int number)
    {
        this.number = this.number.divide(number);
    }

    /**
     * Returns the value of the accumulator raised to the power of the second argument.
     *
     * @param number exponent
     */
    public void pow(Number number)
    {
        if (number == null)
        {
            throw new NullPointerException("Cannot divide by null");
        }
        else if (number instanceof BigDecimal)
        {
            pow((BigDecimal)number);
        }
        else if (number instanceof Double)
        {
            pow(number.doubleValue());
        }
        else if (number instanceof Float)
        {
            pow(number.floatValue());
        }
        else if (number instanceof BigInteger)
        {
            pow((BigInteger)number);
        }
        else if (number instanceof Long)
        {
            pow(number.longValue());
        }
        else if (number instanceof Integer)
        {
            pow(number.intValue());
        }
        else
        {
            throw new IllegalArgumentException("Unhandled number: " + number);
        }
    }

    /**
     * Raise the accumulator to the power by the given number.
     *
     * @param number number to raise to
     */
    public void pow(BigDecimal number)
    {
        this.number = this.number.pow(number);
    }

    /**
     * Raise the accumulator to the power by the given number.
     *
     * @param number number to raise to
     */
    public void pow(double number)
    {
        this.number = this.number.pow(number);
    }

    /**
     * Raise the accumulator to the power by the given number.
     *
     * @param number number to raise to
     */
    public void pow(float number)
    {
        this.number = this.number.pow(number);
    }

    /**
     * Raise the accumulator to the power by the given number.
     *
     * @param number number to raise to
     */
    public void pow(BigInteger number)
    {
        this.number = this.number.pow(number);
    }

    /**
     * Raise the accumulator to the power by the given number.
     *
     * @param number number to raise to
     */
    public void pow(long number)
    {
        this.number = this.number.pow(number);
    }

    /**
     * Raise the accumulator to the power by the given number.
     *
     * @param number number to raise to
     */
    public void pow(int number)
    {
        this.number = this.number.pow(number);
    }

    // Factory methods

    /**
     * Create a new numeric accumulator with the given
     * initial value.
     *
     * @param initialValue initial value for the accumulator
     * @return numeric accumulator
     */
    public static NumericAccumulator newAccumulator(Number initialValue)
    {
        if (initialValue == null)
        {
            throw new NullPointerException("Initial value cannot be null");
        }
        else if (initialValue instanceof BigDecimal)
        {
            return newAccumulator((BigDecimal)initialValue);
        }
        else if (initialValue instanceof Double)
        {
            return newAccumulator(initialValue.doubleValue());
        }
        else if (initialValue instanceof Float)
        {
            return newAccumulator(initialValue.floatValue());
        }
        else if (initialValue instanceof BigInteger)
        {
            return newAccumulator((BigInteger)initialValue);
        }
        else if (initialValue instanceof Long)
        {
            return newAccumulator(initialValue.longValue());
        }
        else if (initialValue instanceof Integer)
        {
            return newAccumulator(initialValue.intValue());
        }
        else
        {
            throw new IllegalArgumentException("Unhandled number: " + initialValue);
        }
    }

    /**
     * Create a new numeric accumulator with the given
     * initial value.
     *
     * @param initialValue initial value for the accumulator
     * @return numeric accumulator
     */
    public static NumericAccumulator newAccumulator(BigDecimal initialValue)
    {
        if (initialValue == null)
        {
            throw new NullPointerException("Initial value cannot be null");
        }
        return new NumericAccumulator(new BigDecimalWrapper(initialValue));
    }

    /**
     * Create a new numeric accumulator with the given
     * initial value.
     *
     * @param initialValue initial value for the accumulator
     * @return numeric accumulator
     */
    public static NumericAccumulator newAccumulator(double initialValue)
    {
        return new NumericAccumulator(new DoubleWrapper(initialValue));
    }

    /**
     * Create a new numeric accumulator with the given
     * initial value.
     *
     * @param initialValue initial value for the accumulator
     * @return numeric accumulator
     */
    public static NumericAccumulator newAccumulator(float initialValue)
    {
        return newAccumulator((double)initialValue);
    }

    /**
     * Create a new numeric accumulator with the given
     * initial value.
     *
     * @param initialValue initial value for the accumulator
     * @return numeric accumulator
     */
    public static NumericAccumulator newAccumulator(BigInteger initialValue)
    {
        if (initialValue == null)
        {
            throw new NullPointerException("Initial value cannot be null");
        }
        return new NumericAccumulator(new BigIntegerWrapper(initialValue));
    }

    /**
     * Create a new numeric accumulator with the given
     * initial value.
     *
     * @param initialValue initial value for the accumulator
     * @return numeric accumulator
     */
    public static NumericAccumulator newAccumulator(long initialValue)
    {
        return new NumericAccumulator(new LongWrapper(initialValue));
    }

    /**
     * Create a new numeric accumulator with the given
     * initial value.
     *
     * @param initialValue initial value for the accumulator
     * @return numeric accumulator
     */
    public static NumericAccumulator newAccumulator(int initialValue)
    {
        return newAccumulator((long)initialValue);
    }

    // Number wrappers

    /**
     * A wrapper around a number, which can allow for conversion to a type
     * with greater precision when necessary.
     */
    private static abstract class NumberWrapper
    {
        abstract Number getValue();

        abstract NumberWrapper negate();

        abstract NumberWrapper add(BigDecimal number);

        abstract NumberWrapper add(double number);

        NumberWrapper add(float number)
        {
            return add((double)number);
        }

        abstract NumberWrapper add(BigInteger number);

        abstract NumberWrapper add(long number);

        abstract NumberWrapper add(int number);

        abstract NumberWrapper subtract(BigDecimal number);

        abstract NumberWrapper subtract(double number);

        NumberWrapper subtract(float number)
        {
            return subtract((double)number);
        }

        abstract NumberWrapper subtract(BigInteger number);

        abstract NumberWrapper subtract(long number);

        abstract NumberWrapper subtract(int number);

        abstract NumberWrapper multiply(BigDecimal number);

        abstract NumberWrapper multiply(double number);

        NumberWrapper multiply(float number)
        {
            return multiply((double)number);
        }

        abstract NumberWrapper multiply(BigInteger number);

        abstract NumberWrapper multiply(long number);

        abstract NumberWrapper multiply(int number);

        abstract NumberWrapper divide(BigDecimal number);

        abstract NumberWrapper divide(double number);

        NumberWrapper divide(float number)
        {
            return divide((double)number);
        }

        abstract NumberWrapper divide(BigInteger number);

        abstract NumberWrapper divide(long number);

        abstract NumberWrapper divide(int number);

        abstract NumberWrapper pow(int number);

        abstract NumberWrapper pow(long number);

        NumberWrapper pow(float number)
        {
            return pow((double)number);
        }

        abstract NumberWrapper pow(double number);

        abstract NumberWrapper pow(BigInteger number);

        abstract NumberWrapper pow(BigDecimal number);
    }

    private static class BigDecimalWrapper extends NumberWrapper
    {
        private BigDecimal value;

        private BigDecimalWrapper(BigDecimal initialValue)
        {
            this.value = initialValue;
        }

        private BigDecimalWrapper(double initialValue)
        {
            this(BigDecimal.valueOf(initialValue));
        }

        private BigDecimalWrapper(BigInteger initialValue)
        {
            this(new BigDecimal(initialValue));
        }

        private BigDecimalWrapper(long initialValue)
        {
            this(new BigDecimal(initialValue));
        }

        @Override
        Number getValue()
        {
            return this.value;
        }

        @Override
        BigDecimalWrapper negate()
        {
            this.value = this.value.negate();
            return this;
        }

        @Override
        NumberWrapper add(BigDecimal number)
        {
            this.value = this.value.add(number);
            return this;
        }

        @Override
        NumberWrapper add(double number)
        {
            return add(BigDecimal.valueOf(number));
        }

        @Override
        NumberWrapper add(BigInteger number)
        {
            return add(new BigDecimal(number));
        }

        @Override
        NumberWrapper add(long number)
        {
            return add(new BigDecimal(number));
        }

        @Override
        NumberWrapper add(int number)
        {
            return add(new BigDecimal(number));
        }

        @Override
        NumberWrapper subtract(BigDecimal number)
        {
            this.value = this.value.subtract(number);
            return this;
        }

        @Override
        NumberWrapper subtract(double number)
        {
            return subtract(BigDecimal.valueOf(number));
        }

        @Override
        NumberWrapper subtract(BigInteger number)
        {
            return subtract(new BigDecimal(number));
        }

        @Override
        NumberWrapper subtract(long number)
        {
            return this.subtract(new BigDecimal(number));
        }

        @Override
        NumberWrapper subtract(int number)
        {
            return subtract(new BigDecimal(number));
        }

        @Override
        NumberWrapper multiply(BigDecimal number)
        {
            this.value = this.value.multiply(number);
            return this;
        }

        @Override
        NumberWrapper multiply(double number)
        {
            return multiply(BigDecimal.valueOf(number));
        }

        @Override
        NumberWrapper multiply(BigInteger number)
        {
            return multiply(new BigDecimal(number));
        }

        @Override
        NumberWrapper multiply(long number)
        {
            return multiply(new BigDecimal(number));
        }

        @Override
        NumberWrapper multiply(int number)
        {
            return multiply(new BigDecimal(number));
        }

        @Override
        NumberWrapper divide(BigDecimal number)
        {
            try
            {
                this.value = this.value.divide(number);
            }
            catch (ArithmeticException e)
            {
                // The result has a non-terminating decimal representation, so we have to round it to a finite precision.
                this.value = this.value.divide(number, MathContext.DECIMAL128);
            }
            return this;
        }

        @Override
        NumberWrapper divide(double number)
        {
            return divide(BigDecimal.valueOf(number));
        }

        @Override
        NumberWrapper divide(BigInteger number)
        {
            return divide(new BigDecimal(number));
        }

        @Override
        NumberWrapper divide(long number)
        {
            return divide(new BigDecimal(number));
        }

        @Override
        NumberWrapper divide(int number)
        {
            return divide(new BigDecimal(number));
        }

        @Override
        NumberWrapper pow(BigDecimal number)
        {
            this.value = new BigDecimal(StrictMath.pow(this.value.doubleValue(), number.doubleValue())); //NOSONAR Ensure we have a proper error message when double is Infinity or NaN
            return this;
        }

        @Override
        NumberWrapper pow(int number)
        {
            return this.pow(new BigDecimal(number));
        }

        @Override
        NumberWrapper pow(long number)
        {
            return this.pow(new BigDecimal(number));
        }

        @Override
        NumberWrapper pow(double number)
        {
            return this.pow(BigDecimal.valueOf(number));
        }

        @Override
        NumberWrapper pow(BigInteger number)
        {
            return this.pow(new BigDecimal(number));
        }
    }

    private static class DoubleWrapper extends NumberWrapper
    {
        private double value;

        private DoubleWrapper(double number)
        {
            this.value = number;
        }

        @Override
        Double getValue()
        {
            return this.value;
        }

        @Override
        NumberWrapper negate()
        {
            double newValue = -this.value;
            if (Double.isInfinite(newValue)) // is this even possible?
            {
                return new BigDecimalWrapper(this.value).negate();
            }
            else
            {
                this.value = newValue;
                return this;
            }
        }

        @Override
        NumberWrapper add(BigDecimal number)
        {
            return new BigDecimalWrapper(this.value).add(number);
        }

        @Override
        NumberWrapper add(double number)
        {
            double newValue = this.value + number;
            if (Double.isInfinite(newValue))
            {
                return new BigDecimalWrapper(this.value).add(number);
            }
            else
            {
                this.value = newValue;
                return this;
            }
        }

        @Override
        NumberWrapper add(BigInteger number)
        {
            return new BigDecimalWrapper(this.value).add(number);
        }

        @Override
        NumberWrapper add(long number)
        {
            double newValue = this.value + number;
            if (Double.isInfinite(newValue))
            {
                return new BigDecimalWrapper(this.value).add(number);
            }
            else
            {
                this.value = newValue;
                return this;
            }
        }

        @Override
        NumberWrapper add(int number)
        {
            double newValue = this.value + number;
            if (Double.isInfinite(newValue))
            {
                return new BigDecimalWrapper(this.value).add(number);
            }
            else
            {
                this.value = newValue;
                return this;
            }
        }

        @Override
        NumberWrapper subtract(BigDecimal number)
        {
            return new BigDecimalWrapper(this.value).subtract(number);
        }

        @Override
        NumberWrapper subtract(double number)
        {
            double newValue = this.value - number;
            if (Double.isInfinite(newValue))
            {
                return new BigDecimalWrapper(this.value).subtract(number);
            }
            else
            {
                this.value = newValue;
                return this;
            }
        }

        @Override
        NumberWrapper subtract(BigInteger number)
        {
            return new BigDecimalWrapper(this.value).subtract(number);
        }

        @Override
        NumberWrapper subtract(long number)
        {
            double newValue = this.value - number;
            if (Double.isInfinite(newValue))
            {
                return new BigDecimalWrapper(this.value).subtract(number);
            }
            else
            {
                this.value = newValue;
                return this;
            }
        }

        @Override
        NumberWrapper subtract(int number)
        {
            double newValue = this.value - number;
            if (Double.isInfinite(newValue))
            {
                return new BigDecimalWrapper(this.value).subtract(number);
            }
            else
            {
                this.value = newValue;
                return this;
            }
        }

        @Override
        NumberWrapper multiply(BigDecimal number)
        {
            return new BigDecimalWrapper(this.value).multiply(number);
        }

        @Override
        NumberWrapper multiply(double number)
        {
            double newValue = this.value * number;
            if (Double.isInfinite(newValue))
            {
                return new BigDecimalWrapper(this.value).multiply(number);
            }
            else
            {
                this.value = newValue;
                return this;
            }
        }

        @Override
        NumberWrapper multiply(BigInteger number)
        {
            return new BigDecimalWrapper(this.value).multiply(number);
        }

        @Override
        NumberWrapper multiply(long number)
        {
            double newValue = this.value * number;
            if (Double.isInfinite(newValue))
            {
                return new BigDecimalWrapper(this.value).multiply(number);
            }
            else
            {
                this.value = newValue;
                return this;
            }
        }

        @Override
        NumberWrapper multiply(int number)
        {
            double newValue = this.value * number;
            if (Double.isInfinite(newValue))
            {
                return new BigDecimalWrapper(this.value).multiply(number);
            }
            else
            {
                this.value = newValue;
                return this;
            }
        }

        @Override
        NumberWrapper divide(BigDecimal number)
        {
            return new BigDecimalWrapper(this.value).divide(number);
        }

        @Override
        NumberWrapper divide(double number)
        {
            // TODO check for underflow and overflow
            this.value /= number;
            return this;
        }

        @Override
        NumberWrapper divide(BigInteger number)
        {
            return new BigDecimalWrapper(this.value).divide(number);
        }

        @Override
        NumberWrapper divide(long number)
        {
            // TODO check for underflow and overflow
            this.value /= number;
            return this;
        }

        @Override
        NumberWrapper divide(int number)
        {
            // TODO check for underflow and overflow
            this.value /= number;
            return this;
        }

        @Override
        NumberWrapper pow(BigDecimal number)
        {
            return this.pow(number.doubleValue());
        }

        @Override
        NumberWrapper pow(int number)
        {
            return this.pow(new BigDecimal(number));
        }

        @Override
        NumberWrapper pow(long number)
        {
            return this.pow(new BigDecimal(number));
        }

        @Override
        NumberWrapper pow(double number)
        {
            this.value = StrictMath.pow(this.value, number);
            return this;
        }

        @Override
        NumberWrapper pow(BigInteger number)
        {
            return this.pow(new BigDecimal(number));
        }
    }

    private static class BigIntegerWrapper extends NumberWrapper
    {
        private BigInteger value;

        private BigIntegerWrapper(BigInteger initialValue)
        {
            this.value = initialValue;
        }

        private BigIntegerWrapper(long initialValue)
        {
            this(new BigInteger(Long.toString(initialValue)));
        }

        @Override
        BigInteger getValue()
        {
            return this.value;
        }

        @Override
        NumberWrapper negate()
        {
            this.value = this.value.negate();
            return this;
        }

        @Override
        NumberWrapper add(BigDecimal number)
        {
            return new BigDecimalWrapper(this.value).add(number);
        }

        @Override
        NumberWrapper add(double number)
        {
            return new BigDecimalWrapper(this.value).add(number);
        }

        @Override
        NumberWrapper add(BigInteger number)
        {
            this.value = this.value.add(number);
            return this;
        }

        @Override
        NumberWrapper add(long number)
        {
            return add(new BigInteger(Long.toString(number)));
        }

        @Override
        NumberWrapper add(int number)
        {
            return add(new BigInteger(Integer.toString(number)));
        }

        @Override
        NumberWrapper subtract(BigDecimal number)
        {
            return new BigDecimalWrapper(this.value).subtract(number);
        }

        @Override
        NumberWrapper subtract(double number)
        {
            return new BigDecimalWrapper(this.value).subtract(number);
        }

        @Override
        NumberWrapper subtract(BigInteger number)
        {
            this.value = this.value.subtract(number);
            return this;
        }

        @Override
        NumberWrapper subtract(long number)
        {
            return subtract(new BigInteger(Long.toString(number)));
        }

        @Override
        NumberWrapper subtract(int number)
        {
            return subtract(new BigInteger(Integer.toString(number)));
        }

        @Override
        NumberWrapper multiply(BigDecimal number)
        {
            return new BigDecimalWrapper(this.value).multiply(number);
        }

        @Override
        NumberWrapper multiply(double number)
        {
            return new BigDecimalWrapper(this.value).multiply(number);
        }

        @Override
        NumberWrapper multiply(BigInteger number)
        {
            this.value = this.value.multiply(number);
            return this;
        }

        @Override
        NumberWrapper multiply(long number)
        {
            return multiply(new BigInteger(Long.toString(number)));
        }

        @Override
        NumberWrapper multiply(int number)
        {
            return multiply(new BigInteger(Integer.toString(number)));
        }

        @Override
        NumberWrapper divide(BigDecimal number)
        {
            return new BigDecimalWrapper(this.value).divide(number);
        }

        @Override
        NumberWrapper divide(double number)
        {
            return new BigDecimalWrapper(this.value).divide(number);
        }

        @Override
        NumberWrapper divide(BigInteger number)
        {
            return new BigDecimalWrapper(this.value).divide(number);
        }

        @Override
        NumberWrapper divide(long number)
        {
            return divide(new BigDecimal(Long.toString(number)));
        }

        @Override
        NumberWrapper divide(int number)
        {
            return divide(new BigDecimal(Integer.toString(number)));
        }

        @Override
        NumberWrapper pow(BigDecimal number)
        {
            return new BigDecimalWrapper(this.value).pow(number);
        }

        @Override
        NumberWrapper pow(int number)
        {
            return new BigIntegerWrapper(this.value.pow(number));
        }

        @Override
        NumberWrapper pow(long number)
        {
            return this.pow(new BigDecimal(number));
        }

        @Override
        NumberWrapper pow(double number)
        {
            return this.pow(BigDecimal.valueOf(number));
        }

        @Override
        NumberWrapper pow(BigInteger number)
        {
            return this.pow(new BigDecimal(number));
        }
    }

    private static class LongWrapper extends NumberWrapper
    {
        private long value;

        private LongWrapper(long initialValue)
        {
            this.value = initialValue;
        }

        @Override
        Long getValue()
        {
            return this.value;
        }

        @Override
        NumberWrapper negate()
        {
            if (this.value == Long.MIN_VALUE)
            {
                return new BigIntegerWrapper(this.value).negate();
            }
            else
            {
                this.value = -this.value;
                return this;
            }
        }

        @Override
        NumberWrapper add(BigDecimal number)
        {
            return new BigDecimalWrapper(this.value).add(number);
        }

        @Override
        NumberWrapper add(double number)
        {
            return new BigDecimalWrapper(this.value).add(number);
        }

        @Override
        NumberWrapper add(BigInteger number)
        {
            return new BigIntegerWrapper(this.value).add(number);
        }

        @Override
        NumberWrapper add(long number)
        {
            long newValue = this.value + number;
            if (additionHasOverflowed(this.value, number, newValue))
            {
                return new BigIntegerWrapper(this.value).add(number);
            }
            else
            {
                this.value = newValue;
                return this;
            }
        }

        @Override
        NumberWrapper add(int number)
        {
            return add((long)number);
        }

        @Override
        NumberWrapper subtract(BigDecimal number)
        {
            return new BigDecimalWrapper(this.value).subtract(number);
        }

        @Override
        NumberWrapper subtract(double number)
        {
            return new BigDecimalWrapper(this.value).subtract(number);
        }

        @Override
        NumberWrapper subtract(BigInteger number)
        {
            return new BigIntegerWrapper(this.value).subtract(number);
        }

        @Override
        NumberWrapper subtract(long number)
        {
            long newValue = this.value - number;
            if (subtractionHasUnderflowed(this.value, number, newValue))
            {
                return new BigIntegerWrapper(this.value).subtract(number);
            }
            else
            {
                this.value = newValue;
                return this;
            }
        }

        @Override
        NumberWrapper subtract(int number)
        {
            return subtract((long)number);
        }

        @Override
        NumberWrapper multiply(BigDecimal number)
        {
            return new BigDecimalWrapper(this.value).multiply(number);
        }

        @Override
        NumberWrapper multiply(double number)
        {
            return new BigDecimalWrapper(this.value).multiply(number);
        }

        @Override
        NumberWrapper multiply(BigInteger number)
        {
            return new BigIntegerWrapper(this.value).multiply(number);
        }

        @Override
        NumberWrapper multiply(long number)
        {
            if (isMultiplicationSafe(this.value, number))
            {
                this.value *= number;
                return this;
            }
            else
            {
                return new BigIntegerWrapper(this.value).multiply(number);
            }
        }

        @Override
        NumberWrapper multiply(int number)
        {
            return multiply((long)number);
        }

        @Override
        NumberWrapper divide(BigDecimal number)
        {
            return new BigDecimalWrapper(this.value).divide(number);
        }

        @Override
        NumberWrapper divide(double number)
        {
            return new BigDecimalWrapper(this.value).divide(number);
        }

        @Override
        NumberWrapper divide(BigInteger number)
        {
            return new BigDecimalWrapper(this.value).divide(number);
        }

        @Override
        NumberWrapper divide(long number)
        {
            if ((this.value % number) == 0)
            {
                this.value /= number;
                return this;
            }
            else
            {
                return new BigDecimalWrapper(this.value).divide(number);
            }
        }

        @Override
        NumberWrapper divide(int number)
        {
            return divide((long)number);
        }

        @Override
        NumberWrapper pow(BigDecimal number)
        {
            return new BigDecimalWrapper(this.value).pow(number);
        }

        @Override
        NumberWrapper pow(int number)
        {
            return this.pow((long)number);
        }

        @Override
        NumberWrapper pow(long number)
        {
            return new BigIntegerWrapper(this.value).pow(number);
        }

        @Override
        NumberWrapper pow(double number)
        {
            return new BigDecimalWrapper(this.value).pow(number);
        }

        @Override
        NumberWrapper pow(BigInteger number)
        {
            return new BigIntegerWrapper(this.value).pow(number);
        }

        private static boolean additionHasOverflowed(long x, long y, long result)
        {
            if (x > 0)
            {
                return (y > 0) && (result < 0);
            }
            else if (x < 0)
            {
                return (y < 0) && (result > 0);
            }
            else
            {
                return false;
            }
        }

        private static boolean subtractionHasUnderflowed(long x, long y, long result)
        {
            if (x > 0)
            {
                return (y < 0) && (result < 0);
            }
            else if (x < 0)
            {
                return (y > 0) && (result > 0);
            }
            else
            {
                return false;
            }
        }

        /**
         * Return whether multiplying x and y is safe.  I.e., whether it
         * can be done without over- or under-flow.
         *
         * @param x long value
         * @param y long value
         * @return whether x*y is safe
         */
        private static boolean isMultiplicationSafe(long x, long y)
        {
            if ((x == 0) || (y == 0))
            {
                return true;
            }
            else if (x > 0)
            {
                if (y > 0)
                {
                    // x, y positive
                    return x <= (Long.MAX_VALUE / y);
                }
                else
                {
                    // x positive, y negative
                    return x <= (Long.MIN_VALUE / y);
                }
            }
            else if (y > 0)
            {
                // x negative, y positive
                return x >= (Long.MIN_VALUE / y);
            }
            else
            {
                // x, y negative
                return x >= (Long.MAX_VALUE / y);
            }
        }
    }
}
