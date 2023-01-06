// Copyright 2022 Goldman Sachs
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

package org.finos.legend.pure.m4.tools;

import java.io.IOException;
import java.io.UncheckedIOException;

public interface SafeAppendable extends Appendable
{
    @Override
    SafeAppendable append(CharSequence csq);

    @Override
    SafeAppendable append(CharSequence csq, int start, int end);

    @Override
    SafeAppendable append(char c);

    default SafeAppendable append(String s)
    {
        return append((CharSequence) ((s == null) ? "null" : s));
    }

    default SafeAppendable append(boolean b)
    {
        return append(Boolean.toString(b));
    }

    default SafeAppendable append(int i)
    {
        return append(Integer.toString(i));
    }

    default SafeAppendable append(long l)
    {
        return append(Long.toString(l));
    }

    default SafeAppendable append(float f)
    {
        return append(Float.toString(f));
    }

    default SafeAppendable append(double d)
    {
        return append(Double.toString(d));
    }

    default SafeAppendable append(Object o)
    {
        return append(String.valueOf(o));
    }

    static SafeAppendable wrap(Appendable appendable)
    {
        if (appendable instanceof SafeAppendable)
        {
            return (SafeAppendable) appendable;
        }
        if (appendable instanceof StringBuilder)
        {
            return wrap((StringBuilder) appendable);
        }
        return new SafeAppendable()
        {
            @Override
            public SafeAppendable append(CharSequence csq)
            {
                try
                {
                    appendable.append(csq);
                }
                catch (IOException e)
                {
                    throw new UncheckedIOException(e);
                }
                return this;
            }

            @Override
            public SafeAppendable append(CharSequence csq, int start, int end)
            {
                try
                {
                    appendable.append(csq, start, end);
                }
                catch (IOException e)
                {
                    throw new UncheckedIOException(e);
                }
                return this;
            }

            @Override
            public SafeAppendable append(char c)
            {
                try
                {
                    appendable.append(c);
                }
                catch (IOException e)
                {
                    throw new UncheckedIOException(e);
                }
                return this;
            }
        };
    }

    static SafeAppendable wrap(StringBuilder builder)
    {
        return new SafeAppendable()
        {
            @Override
            public SafeAppendable append(CharSequence csq)
            {
                builder.append(csq);
                return this;
            }

            @Override
            public SafeAppendable append(CharSequence csq, int start, int end)
            {
                builder.append(csq);
                return this;
            }

            @Override
            public SafeAppendable append(char c)
            {
                builder.append(c);
                return this;
            }

            @Override
            public SafeAppendable append(String s)
            {
                builder.append(s);
                return this;
            }

            @Override
            public SafeAppendable append(boolean b)
            {
                builder.append(b);
                return this;
            }

            @Override
            public SafeAppendable append(int i)
            {
                builder.append(i);
                return this;
            }

            @Override
            public SafeAppendable append(long l)
            {
                builder.append(l);
                return this;
            }

            @Override
            public SafeAppendable append(float f)
            {
                builder.append(f);
                return this;
            }

            @Override
            public SafeAppendable append(double d)
            {
                builder.append(d);
                return this;
            }

            @Override
            public SafeAppendable append(Object o)
            {
                builder.append(o);
                return this;
            }
        };
    }
}
