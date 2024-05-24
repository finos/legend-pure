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

package org.finos.legend.pure.runtime.java.interpreted.profiler;

import org.eclipse.collections.api.block.HashingStrategy;
import org.eclipse.collections.api.factory.Lists;
import org.eclipse.collections.api.factory.Maps;
import org.eclipse.collections.api.factory.Stacks;
import org.eclipse.collections.api.list.MutableList;
import org.eclipse.collections.api.map.MutableMap;
import org.eclipse.collections.api.stack.MutableStack;
import org.eclipse.collections.impl.map.strategy.mutable.UnifiedMapWithHashingStrategy;
import org.finos.legend.pure.m3.navigation.Instance;
import org.finos.legend.pure.m3.navigation.M3Properties;
import org.finos.legend.pure.m3.navigation.ProcessorSupport;
import org.finos.legend.pure.m3.navigation.property.Property;
import org.finos.legend.pure.m3.tools.SpacePrinter;
import org.finos.legend.pure.m3.tools.TimePrinter;
import org.finos.legend.pure.m3.tools.TimeTracker;
import org.finos.legend.pure.m3.tools.tree.TreeNode;
import org.finos.legend.pure.m3.tools.tree.TreePrinter;
import org.finos.legend.pure.m4.coreinstance.CoreInstance;

public class ActiveProfiler implements Profiler
{
    private static final HashingStrategy<CoreInstance> IDENTITY_HASHING_STRATEGY = new IdentityHashingStrategy();
    MutableMap<CoreInstance, MethodStat> treeNodeCache = UnifiedMapWithHashingStrategy.newMap(IDENTITY_HASHING_STRATEGY);

    StringBuffer buffer = new StringBuffer();
    MutableStack<TimeTracker> stack = Stacks.mutable.empty();
    MutableMap<String, MethodStat> map = Maps.mutable.empty();

    MethodStat root;

    private final ProcessorSupport processorSupport;

    private final boolean showTime;

    public ActiveProfiler(ProcessorSupport processorSupport, boolean showTime)
    {
        this.processorSupport = processorSupport;
        this.showTime = showTime;
    }


    @Override
    public void start(CoreInstance coreInstance)
    {
        this.buffer.append("\n################################################## Profiler report ##################################################\n");
        this.stack.push(new TimeTracker("T"));
        this.root = new MethodStat(Instance.getValueForMetaPropertyToOneResolved(coreInstance, M3Properties.func, M3Properties.name, this.processorSupport).getName(), this.showTime);
        this.treeNodeCache.put(coreInstance, this.root);
    }

    @Override
    public void startExecutingFunctionExpression(CoreInstance instance, CoreInstance parent)
    {
        MethodStat stat = this.treeNodeCache.get(instance);
        if (stat == null)
        {
            stat = new MethodStat(Instance.getValueForMetaPropertyToOneResolved(instance, M3Properties.func, M3Properties.name, this.processorSupport).getName() + (Property.isProperty(Instance.getValueForMetaPropertyToOneResolved(instance, M3Properties.func, processorSupport), this.processorSupport) ? "(P)" : ""), this.showTime);
            this.treeNodeCache.put(instance, stat);
            MethodStat parentStat = this.treeNodeCache.get(parent);
            parentStat.addChild(stat);
        }
        String funcName = Instance.getValueForMetaPropertyToOneResolved(instance, M3Properties.func, M3Properties.name, this.processorSupport).getName();
        this.stack.push(new TimeTracker(funcName));
    }

    @Override
    public void finishedExecutingFunctionExpression(CoreInstance instance)
    {
        String funcName = Instance.getValueForMetaPropertyToOneResolved(instance, M3Properties.func, M3Properties.name, this.processorSupport).getName();
        long res = new TimeTracker(funcName).diffLong(this.stack.pop());
        MethodStat stat = this.map.getIfAbsentPut(funcName, () -> new MethodStat(funcName, this.showTime));
        stat.incCount();
        stat.addTime(res);

        MethodStat treeStat = this.treeNodeCache.get(instance);
        treeStat.incCount();
        treeStat.addTime(res);
    }

    @Override
    public void end(CoreInstance coreInstance)
    {
        long res = new TimeTracker("T").diffLong(this.stack.pop());

        MethodStat treeStat = this.treeNodeCache.get(coreInstance);
        treeStat.incCount();
        treeStat.addTime(res);

        if (this.showTime)
        {
            int max = this.map.valuesView().collectInt(m -> m.method.length()).maxIfEmpty(0) + 2;
            this.buffer.append(this.map.valuesView().toSortedList((o1, o2) -> Long.compare(o2.time, o1.time))
                    .collect(methodStat -> "       " + SpacePrinter.print(methodStat.method, max) + " " + TimePrinter.makeItHuman(methodStat.time) + "  " + methodStat.count).makeString("\n"));
            this.buffer.append("\n\n");
        }
        this.buffer.append(TreePrinter.printTree(this.root, "       "));
        this.buffer.append("\n################################################## Finished Report ##################################################").append(this.showTime ? ": " + res : "").append("\n");
    }

    public String getReport()
    {
        return this.buffer.toString();
    }

    private static class MethodStat implements TreeNode<MethodStat>
    {
        private final MutableList<MethodStat> children = Lists.mutable.empty();

        private final String method;
        private int count;
        private long time;
        private final boolean showTime;

        private MethodStat(String method, boolean showTime)
        {
            this.method = method;
            this.showTime = showTime;
        }

        public void incCount()
        {
            this.count++;
        }

        public void addTime(long res)
        {
            this.time = this.time + res;
        }

        public void addChild(MethodStat child)
        {
            this.children.add(child);
        }

        @Override
        public MutableList<MethodStat> getChildren()
        {
            return this.children;
        }

        @Override
        public MethodStat getChildAt(int index)
        {
            return this.children.get(index);
        }

        @Override
        public boolean isLeaf()
        {
            return this.children.isEmpty();
        }

        @Override
        public int indexOf(MethodStat node)
        {
            return this.children.indexOf(node);
        }

        public String toString()
        {
            return this.count + " " + this.method + (this.showTime ? " " + TimePrinter.makeItHuman(this.time) : "");
        }
    }
}
