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

public class TimePrinter
{
    public static String makeItHuman(long delta)
    {
        long deltaNano = delta % 1000;
        long deltaMicro = (delta / 1000) % 1000;
        long deltaMillis = (delta / 1000000) % 1000;
        long deltaSecond = (delta / 1000000000);
        return String.format("%d:%02d:%02d.%03d.%03d.%03d", deltaSecond / 3600, (deltaSecond / 60) % 60, deltaSecond % 60, deltaMillis, deltaMicro, deltaNano);
    }
}
