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

package org.finos.legend.pure.m3.compiler.postprocessing;

import org.eclipse.collections.api.factory.Sets;
import org.eclipse.collections.api.factory.Stacks;
import org.eclipse.collections.api.list.ListIterable;
import org.eclipse.collections.api.set.MutableSet;
import org.eclipse.collections.api.set.SetIterable;
import org.eclipse.collections.api.stack.MutableStack;
import org.eclipse.collections.api.tuple.Pair;
import org.eclipse.collections.impl.tuple.Tuples;
import org.finos.legend.pure.m3.SourceMutation;
import org.finos.legend.pure.m3.compiler.postprocessing.inference.PrintTypeInferenceObserver;
import org.finos.legend.pure.m3.compiler.postprocessing.inference.TestTypeInferenceObserver;
import org.finos.legend.pure.m3.compiler.postprocessing.inference.TypeInferenceContext;
import org.finos.legend.pure.m3.compiler.postprocessing.inference.TypeInferenceObserver;
import org.finos.legend.pure.m3.compiler.postprocessing.inference.VoidTypeInferenceObserver;
import org.finos.legend.pure.m3.compiler.postprocessing.processor.milestoning.MilestoningDates;
import org.finos.legend.pure.m3.navigation.ProcessorSupport;
import org.finos.legend.pure.m3.serialization.filesystem.usercodestorage.CodeStorage;
import org.finos.legend.pure.m3.serialization.grammar.ParserLibrary;
import org.finos.legend.pure.m3.serialization.grammar.m3parser.inlinedsl.InlineDSLLibrary;
import org.finos.legend.pure.m3.serialization.runtime.Message;
import org.finos.legend.pure.m3.serialization.runtime.pattern.URLPatternLibrary;
import org.finos.legend.pure.m3.tools.matcher.MatcherState;
import org.finos.legend.pure.m4.coreinstance.CoreInstance;

public class ProcessorState extends MatcherState
{
    private VariableContext variableContext;
    private final MutableSet<String> variables = Sets.mutable.empty();
    private final MutableStack<TypeInferenceContext> typeInferenceContext = Stacks.mutable.empty();
    private final MutableStack<Pair<ListIterable<String>, MilestoningDates>> milestoningDates = Stacks.mutable.empty();
    private final MutableSet<CoreInstance> functionDefinitions = Sets.mutable.empty();
    private final Message message;
    private int count;
    private final ParserLibrary parserLibrary;
    private final InlineDSLLibrary inlineDSLLibrary;
    private final TypeInferenceObserver observer;
    private final URLPatternLibrary URLPatternLibrary;
    private final SourceMutation sourceMutation = new SourceMutation();
    private final CodeStorage codeStorage;

    public ProcessorState(VariableContext variableContext, ParserLibrary parserLibrary, InlineDSLLibrary inlineDSLLibrary, ProcessorSupport processorSupport, URLPatternLibrary URLPatternLibrary, CodeStorage codeStorage, Message message)
    {
        super(processorSupport);
        this.variableContext = (variableContext == null) ? VariableContext.newVariableContext() : variableContext;
        this.message = message;
        this.parserLibrary = parserLibrary;
        this.inlineDSLLibrary = inlineDSLLibrary;
        if (Boolean.getBoolean("pure.typeinference.print"))
        {
            this.observer = new PrintTypeInferenceObserver(processorSupport, this);
        }
        else if (Boolean.getBoolean("pure.typeinference.test"))
        {
            this.observer = new TestTypeInferenceObserver(processorSupport, this);
        }
        else
        {
            this.observer = new VoidTypeInferenceObserver();
        }
        this.URLPatternLibrary = URLPatternLibrary;
        this.newTypeInferenceContext(null);
        this.codeStorage = codeStorage;
    }

    public CodeStorage getCodeStorage()
    {
        return this.codeStorage;
    }

    public VariableContext getVariableContext()
    {
        return this.variableContext;
    }

    public void pushVariableContext()
    {
        this.variableContext = VariableContext.newVariableContext(this.variableContext);
    }

    public void popVariableContext()
    {
        this.variableContext = this.variableContext.getParent();
        if (this.variableContext == null)
        {
            this.variableContext = VariableContext.newVariableContext();
        }
    }

    public void addFunctionDefinition(CoreInstance lambda)
    {
        this.functionDefinitions.add(lambda);
    }

    public SetIterable<CoreInstance> getFunctionDefinitions()
    {
        return this.functionDefinitions.asUnmodifiable();
    }

    public void resetVariableContext()
    {
        this.variableContext = VariableContext.newVariableContext();
    }

    public void noteProcessed(CoreInstance instance)
    {
        instance.markProcessed();
    }

    public void incAndPushCount()
    {
        if (this.message != null)
        {
            this.count++;
            this.message.setMessage(String.format("Binding (%,d)", this.count));
        }
    }

    public URLPatternLibrary getURLPatternLibrary()
    {
        return this.URLPatternLibrary;
    }

    public void pushTypeInferenceContext()
    {
        if (this.typeInferenceContext.peek().isAhead() && !this.typeInferenceContext.peek().isAheadConsumed())
        {
            this.typeInferenceContext.peek().aheadConsumed();
        }
        else
        {
            this.observer.shiftTab();
            TypeInferenceContext tc = this.typeInferenceContext.pop();
            this.typeInferenceContext.push(new TypeInferenceContext(tc, this.processorSupport));
        }
    }

    public void pushTypeInferenceContextAhead()
    {
        pushTypeInferenceContext();
        this.typeInferenceContext.peek().setAhead();

    }

    public void popTypeInferenceContextAhead()
    {
        this.observer.unShiftTab();
        TypeInferenceContext tc = this.typeInferenceContext.pop();
        if (tc.getParent() != null)
        {
            this.typeInferenceContext.push(tc.getParent());
        }

    }

    public void popTypeInferenceContext()
    {
        if (!this.typeInferenceContext.peek().isAhead())
        {
            this.popTypeInferenceContextAhead();
        }
    }

    public TypeInferenceContext getTypeInferenceContext()
    {
        return this.typeInferenceContext.peek();
    }

    public void newTypeInferenceContext(CoreInstance owner)
    {
        this.observer.resetTab();
        this.typeInferenceContext.push(new TypeInferenceContext(owner, this.processorSupport));
    }

    public void deleteTypeInferenceContext()
    {
        this.observer.resetTab();
        this.typeInferenceContext.pop();
    }

    public TypeInferenceObserver getObserver()
    {
        return this.observer;
    }

    public Message getMessage()
    {
        return this.message;
    }

    public ParserLibrary getParserLibrary()
    {
        return this.parserLibrary;
    }

    public InlineDSLLibrary getInlineDSLLibrary()
    {
        return this.inlineDSLLibrary;
    }

    public void resetVariables()
    {
        this.variables.clear();
    }

    public boolean hasVariable(String variable)
    {
        return this.variables.contains(variable);
    }

    public void addVariable(String variable)
    {
        this.variables.add(variable);
    }

    public SourceMutation getSourceMutation()
    {
        return this.sourceMutation;
    }

    @Override
    public void removeVisited(CoreInstance instance)
    {
        super.removeVisited(instance);
        this.functionDefinitions.remove(instance);
    }

    public MilestoningDates getMilestoningDates(String varName)
    {
        Pair<ListIterable<String>, MilestoningDates> inputVarDates = this.milestoningDates.detect(p -> p.getOne().contains(varName));
        return (inputVarDates == null) ? null : inputVarDates.getTwo();
    }

    public void pushMilestoneDateContext(MilestoningDates milestonedDates, ListIterable<String> varNames)
    {
        this.milestoningDates.push(Tuples.pair(varNames, milestonedDates));
    }

    public void popMilestoneDateContext()
    {
        this.milestoningDates.pop();
    }
}
