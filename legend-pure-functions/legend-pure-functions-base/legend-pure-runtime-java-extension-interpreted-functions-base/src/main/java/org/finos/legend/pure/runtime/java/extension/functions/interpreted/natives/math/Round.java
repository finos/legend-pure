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

package org.finos.legend.pure.runtime.java.extension.functions.interpreted.natives.math;

import org.finos.legend.pure.m4.ModelRepository;
import org.finos.legend.pure.runtime.java.interpreted.FunctionExecutionInterpreted;

public class Round extends AbstractRoundFunction
{
    public Round(FunctionExecutionInterpreted functionExecution, ModelRepository repository)
    {
        super(repository);
    }

    @Override
    protected String roundPositive(String integerString, String decimalString)
    {
        switch (decimalString.charAt(0))
        {
            case '5':
            {
                // Round to nearest even number
                return isIntegerStringEven(integerString) ? integerString : addOneToIntegerString(integerString);
            }
            case '6':
            case '7':
            case '8':
            case '9':
            {
                // Round up
                return addOneToIntegerString(integerString);
            }
            default:
            {
                // Round down
                return integerString;
            }
        }
    }

    @Override
    protected String roundNegative(String integerString, String decimalString)
    {
        switch (decimalString.charAt(0))
        {
            case '5':
            {
                // Round to nearest even number
                return isIntegerStringEven(integerString) ? integerString : subtractOneFromIntegerString(integerString);
            }
            case '6':
            case '7':
            case '8':
            case '9':
            {
                // Round down
                return subtractOneFromIntegerString(integerString);
            }
            default:
            {
                // Round up
                return integerString;
            }
        }
    }

    private boolean isIntegerStringEven(String integerString)
    {
        char last = integerString.charAt(integerString.length() - 1);
        return (last == '0') || (last == '2') || (last == '4') || (last == '6') || (last == '8');
    }
}
