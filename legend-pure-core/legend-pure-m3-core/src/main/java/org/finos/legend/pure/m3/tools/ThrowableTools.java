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

package org.finos.legend.pure.m3.tools;

public class ThrowableTools
{
    private ThrowableTools()
    {
        // Utility class
    }

    /**
     * Find the root cause of the given throwable.  If the throwable
     * has no cause, then this is the throwable itself.  Otherwise,
     * it is the root cause of the throwable's cause.
     *
     * @param throwable throwable
     * @return root cause throwable
     */
    public static Throwable findRootThrowable(Throwable throwable)
    {
        Throwable previous = throwable;
        Throwable t = throwable.getCause();
        while (t != null)
        {
            previous = t;
            t = t.getCause();
        }
        return previous;
    }

    /**
     * Find the top instance of throwableClass in the causal chain
     * of throwable.  If throwable is itself an instance of
     * throwableClass, then it is returned.  Otherwise, this
     * function searches down the causal chain until it finds an
     * instance of throwableClass.  If no instance can be found,
     * it returns null.
     *
     * @param throwable      throwable
     * @param throwableClass subclass of Throwable to search for
     * @param <T>            subclass of Throwable to search for
     * @return top throwable of the given class or null
     */
    public static <T extends Throwable> T findTopThrowableOfClass(Throwable throwable, Class<T> throwableClass)
    {
        for (Throwable t = throwable; t != null; t = t.getCause())
        {
            if (throwableClass.isInstance(t))
            {
                return throwableClass.cast(t);
            }
        }
        return null;
    }

    /**
     * Returns whether there is an instance of throwableClass in
     * the causal chain of throwable.
     *
     * @param throwable      throwable
     * @param throwableClass subclass of Throwable to search for
     * @return whether an instance of throwableClass can be found in the causal chain of throwable
     */
    public static boolean canFindThrowableOfClass(Throwable throwable, Class<? extends Throwable> throwableClass)
    {
        for (Throwable t = throwable; t != null; t = t.getCause())
        {
            if (throwableClass.isInstance(t))
            {
                return true;
            }
        }
        return false;
    }
}
