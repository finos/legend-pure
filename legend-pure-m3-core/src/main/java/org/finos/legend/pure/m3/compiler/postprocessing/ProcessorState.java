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
import org.eclipse.collections.api.set.MutableSet;
import org.eclipse.collections.api.set.SetIterable;
import org.eclipse.collections.api.stack.MutableStack;
import org.finos.legend.pure.m3.SourceMutation;
import org.finos.legend.pure.m3.compiler.postprocessing.inference.PrintTypeInferenceObserver;
import org.finos.legend.pure.m3.compiler.postprocessing.inference.TestTypeInferenceObserver;
import org.finos.legend.pure.m3.compiler.postprocessing.inference.TypeInferenceContext;
import org.finos.legend.pure.m3.compiler.postprocessing.inference.TypeInferenceObserver;
import org.finos.legend.pure.m3.compiler.postprocessing.inference.VoidTypeInferenceObserver;
import org.finos.legend.pure.m3.compiler.postprocessing.observer.CombinedPostProcessorObserver;
import org.finos.legend.pure.m3.compiler.postprocessing.observer.PostProcessorObserver;
import org.finos.legend.pure.m3.compiler.postprocessing.observer.VoidPostProcessorObserver;
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
    private final MutableStack<MilestoneDateContext> milestoningDates = Stacks.mutable.empty();
    private final MutableSet<CoreInstance> functionDefinitions = Sets.mutable.empty();
    private final Message message;
    private final ParserLibrary parserLibrary;
    private final InlineDSLLibrary inlineDSLLibrary;
    private final PostProcessorObserver postProcessorObserver;
    private final TypeInferenceObserver typeInferenceObserver;
    private final URLPatternLibrary URLPatternLibrary;
    private final SourceMutation sourceMutation = new SourceMutation();
    private final CodeStorage codeStorage;

    public ProcessorState(VariableContext variableContext, ParserLibrary parserLibrary, InlineDSLLibrary inlineDSLLibrary, ProcessorSupport processorSupport, URLPatternLibrary URLPatternLibrary, CodeStorage codeStorage, Message message, PostProcessorObserver observer)
    {
        super(processorSupport);
        this.variableContext = (variableContext == null) ? VariableContext.newVariableContext() : variableContext;
        this.parserLibrary = parserLibrary;
        this.message = message;
        this.inlineDSLLibrary = inlineDSLLibrary;
        this.postProcessorObserver = getPostProcessorObserver(message, observer);
        this.typeInferenceObserver = getTypeInferenceObserver(this);
        this.URLPatternLibrary = URLPatternLibrary;
        this.newTypeInferenceContext(null);
        this.codeStorage = codeStorage;
    }

    public ProcessorState(VariableContext variableContext, ParserLibrary parserLibrary, InlineDSLLibrary inlineDSLLibrary, ProcessorSupport processorSupport, URLPatternLibrary URLPatternLibrary, CodeStorage codeStorage, Message message)
    {
        this(variableContext, parserLibrary, inlineDSLLibrary, processorSupport, URLPatternLibrary, codeStorage, message, null);
    }

    public CodeStorage getCodeStorage()
    {
        return this.codeStorage;
    }

    public VariableContext getVariableContext()
    {
        return this.variableContext;
    }

    @Deprecated
    public void pushVariableContext()
    {
        pushNewVariableContext();
    }

    @Deprecated
    public void popVariableContext()
    {
        popVariableContext(this.variableContext);
    }

    void resetVariableContext()
    {
        this.variableContext = VariableContext.newVariableContext();
    }

    private VariableContext pushNewVariableContext()
    {
        return this.variableContext = VariableContext.newVariableContext(this.variableContext);
    }

    private void popVariableContext(VariableContext context)
    {
        if (this.variableContext != context)
        {
            // TODO should we throw or just not do anything?
            throw new IllegalStateException("Variable context mismatch");
        }
        VariableContext parent = context.getParent();
        this.variableContext = (parent == null) ? VariableContext.newVariableContext() : parent;
    }

    public void addFunctionDefinition(CoreInstance lambda)
    {
        this.functionDefinitions.add(lambda);
    }

    public SetIterable<CoreInstance> getFunctionDefinitions()
    {
        return this.functionDefinitions.asUnmodifiable();
    }

    public void noteProcessed(CoreInstance instance)
    {
        instance.markProcessed();
    }

    public void startProcessing(CoreInstance instance)
    {
        this.postProcessorObserver.startProcessing(instance);
        noteProcessed(instance);
    }

    public void finishProcessing(CoreInstance instance, Exception e)
    {
        this.postProcessorObserver.finishProcessing(instance, e);
    }

    public void finishProcessing(CoreInstance instance)
    {
        finishProcessing(instance, null);
    }

    @Deprecated
    public void incAndPushCount()
    {
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
            this.typeInferenceObserver.shiftTab();
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
        this.typeInferenceObserver.unShiftTab();
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
        this.typeInferenceObserver.resetTab();
        this.typeInferenceContext.push(new TypeInferenceContext(owner, this.processorSupport));
    }

    public void deleteTypeInferenceContext()
    {
        this.typeInferenceObserver.resetTab();
        this.typeInferenceContext.pop();
    }

    public TypeInferenceObserver getObserver()
    {
        return this.typeInferenceObserver;
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
        MilestoneDateContext context = this.milestoningDates.detect(c -> c.varNames.contains(varName));
        return (context == null) ? null : context.milestoningDates;
    }

    @Deprecated
    public void pushMilestoneDateContext(MilestoningDates milestonedDates, Iterable<String> varNames)
    {
        pushMilestoneDateContext(newMilestoneDateContext(milestonedDates, varNames));
    }

    @Deprecated
    public void popMilestoneDateContext()
    {
        this.milestoningDates.pop();
    }

    private void pushMilestoneDateContext(MilestoneDateContext milestoneDateContext)
    {
        if (milestoneDateContext != null)
        {
            this.milestoningDates.push(milestoneDateContext);
        }
    }

    private void popMilestoneDateContext(MilestoneDateContext milestoneDateContext)
    {
        if (milestoneDateContext != null)
        {
            MilestoneDateContext popped = this.milestoningDates.pop();
            if (popped != milestoneDateContext)
            {
                throw new IllegalStateException("Unexpected milestone date context");
            }
        }
    }

    public VariableContextScope withNewVariableContext()
    {
        return new VariableContextScope();
    }

    public class VariableContextScope implements AutoCloseable
    {
        private final VariableContext variableContext;

        private VariableContextScope()
        {
            this.variableContext = pushNewVariableContext();
        }

        @Override
        public void close()
        {
            popVariableContext(this.variableContext);
        }
    }

    public MilestoningDateContextScope withMilestoningDateContext(MilestoningDates milestonedDates, Iterable<String> varNames)
    {
        return new MilestoningDateContextScope(newMilestoneDateContext(milestonedDates, varNames));
    }

    public class MilestoningDateContextScope implements AutoCloseable
    {
        private final MilestoneDateContext milestoneDateContext;

        private MilestoningDateContextScope(MilestoneDateContext milestoneDateContext)
        {
            this.milestoneDateContext = milestoneDateContext;
            pushMilestoneDateContext(this.milestoneDateContext);
        }

        @Override
        public void close()
        {
            popMilestoneDateContext(this.milestoneDateContext);
        }
    }

    private static MilestoneDateContext newMilestoneDateContext(MilestoningDates milestonedDates, Iterable<String> varNames)
    {
        return (milestonedDates == null) ? null : new MilestoneDateContext(milestonedDates, varNames);
    }

    private static class MilestoneDateContext
    {
        private final MilestoningDates milestoningDates;
        private final SetIterable<String> varNames;

        private MilestoneDateContext(MilestoningDates milestoningDates, Iterable<String> varNames)
        {
            this.milestoningDates = milestoningDates;
            this.varNames = Sets.immutable.withAll(varNames);
        }
    }

    private static TypeInferenceObserver getTypeInferenceObserver(ProcessorState processorState)
    {
        if (Boolean.getBoolean("pure.typeinference.print"))
        {
            return new PrintTypeInferenceObserver(processorState);
        }
        if (Boolean.getBoolean("pure.typeinference.test"))
        {
            return new TestTypeInferenceObserver(processorState);
        }
        return new VoidTypeInferenceObserver();
    }

    private static PostProcessorObserver getPostProcessorObserver(Message message, PostProcessorObserver observer)
    {
        if (message == null)
        {
            return (observer == null) ? new VoidPostProcessorObserver() : observer;
        }

        MessageObserver messageObserver = new MessageObserver(message);
        return (observer == null) ? messageObserver : CombinedPostProcessorObserver.combine(messageObserver, observer);
    }

    private static class MessageObserver implements PostProcessorObserver
    {
        private final Message message;
        private int count = 0;

        private MessageObserver(Message message)
        {
            this.message = message;
        }

        @Override
        public void startProcessing(CoreInstance instance)
        {
            this.message.setMessage(String.format("Binding (%,d)", ++this.count));
        }
    }
}
