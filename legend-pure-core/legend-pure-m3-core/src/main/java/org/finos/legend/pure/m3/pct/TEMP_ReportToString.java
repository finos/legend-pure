// Copyright 2024 Goldman Sachs
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

package org.finos.legend.pure.m3.pct;

import org.eclipse.collections.api.list.MutableList;
import org.eclipse.collections.api.multimap.list.MutableListMultimap;
import org.eclipse.collections.api.tuple.Pair;
import org.eclipse.collections.impl.tuple.Tuples;
import org.eclipse.collections.impl.utility.ListIterate;
import org.finos.legend.pure.m3.pct.model.ReportScope;
import org.finos.legend.pure.m3.pct.model.Report;

public class TEMP_ReportToString
{
    public static void print(Report report, ReportScope reportScope)
    {
        MutableList<Pair<Pair<String, String>, String>> res = ListIterate.collect(report.functions, c ->
        {
            String src = c.sourceId.substring(reportScope.filePath.length(), c.sourceId.lastIndexOf('/'));
            return Tuples.pair((src.indexOf('/') == -1 ? Tuples.pair(src, "") : Tuples.pair(src.substring(0, src.indexOf('/')), src.substring(src.indexOf('/') + 1))), c.name);
        });

        MutableListMultimap<String, Pair<Pair<String, String>, String>> first = res.groupBy(x -> x.getOne().getOne());

        first.forEachKey(k ->
        {
            System.out.println(k);
            MutableListMultimap<String, Pair<Pair<String, String>, String>> second = first.get(k).groupBy(z -> z.getOne().getTwo());
            second.forEachKey(kk ->
                    {
                        System.out.println("   " + second.get(kk).collect(Pair::getTwo).makeString(", "));
                    }
            );
            System.out.println();
        });
    }
}


//        System.out.println(testCollection.getAllTestFunctions().collect(x -> x.getSourceInformation().getSourceId()).distinct().sortThis().makeString("\n"));
//        System.out.println(
//                Lists.mutable.withAll(
//                        testCollection.getAllTestFunctions().collect(x ->
//                                {
//                                    PackageableFunction<?> f = (PackageableFunction<?>) x;
//                                    return f.getSourceInformation().getSourceId() + " " + PackageableElement.getUserPathForPackageableElement(f._package()) + " " + f.getName();
//                                }
//                        )
//                ).makeString("\n")
//        );