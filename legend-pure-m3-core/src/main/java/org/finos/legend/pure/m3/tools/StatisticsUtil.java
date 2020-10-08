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

public class StatisticsUtil
{
    private StatisticsUtil()
    {
        // utility class
    }

    public static double standardDeviation(double[] values, boolean isBiasCorrected)
    {
        return Math.sqrt(variance(values, isBiasCorrected));
    }

    public static double variance(double[] values, boolean isBiasCorrected)
    {
        int length = values.length;
        if (length == 1)
        {
            if (isBiasCorrected)
            {
                //calculating sample variance for only 1 number is not allowed
                throw new IllegalArgumentException("calculating sample variance for only 1 number is not allowed");
            }
            else
            {
                //population variance for only 1 number
                return 0.0;
            }
        }

        double mean = mean(values);
        double val = 0.0;
        for (int i = 0; i < length; i++)
        {
            double value = values[i];
            double diff = value - mean;
            val += diff * diff;
        }
        if (isBiasCorrected)
        {
            return val / (length - 1);
        }
        else
        {
            return val / length;
        }
    }

    public static double mean(double[] values)
    {
        int length = values.length;
        if (length == 0)
        {
            throw new IllegalArgumentException("Cannot compute mean with no values");
        }

        double sum = values[0];
        for (int i = 1; i < length; i++)
        {
            sum += values[i];
        }
        return sum / length;
    }
}
