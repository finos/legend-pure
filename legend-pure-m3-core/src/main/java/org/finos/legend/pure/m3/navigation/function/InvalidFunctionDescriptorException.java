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

package org.finos.legend.pure.m3.navigation.function;

/**
 * Exception thrown when a function descriptor is invalid.
 */
public class InvalidFunctionDescriptorException extends Exception
{
    public InvalidFunctionDescriptorException(String invalidDescriptor)
    {
        super(createMessage(invalidDescriptor));
    }

    public InvalidFunctionDescriptorException(String invalidDescriptor, Throwable cause)
    {
        super(createMessage(invalidDescriptor), cause);
    }

    private static String createMessage(String invalidDescriptor)
    {
        StringBuilder builder = new StringBuilder("Invalid function descriptor: ");
        if (invalidDescriptor == null)
        {
            builder.append((String) null);
        }
        else
        {
            builder.append('\'').append(invalidDescriptor).append('\'');
        }
        return builder.toString();
    }
}
