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

package org.finos.legend.pure.m3.compiler.unload.unbind;

import org.finos.legend.pure.m3.compiler.Context;
import org.finos.legend.pure.m3.coreinstance.meta.pure.metamodel.extension.AnnotatedElementCoreInstanceWrapper;
import org.finos.legend.pure.m3.coreinstance.meta.pure.metamodel.extension.ElementWithTaggedValues;
import org.finos.legend.pure.m3.coreinstance.meta.pure.metamodel.extension.ElementWithTaggedValuesCoreInstanceWrapper;
import org.finos.legend.pure.m3.coreinstance.meta.pure.metamodel.extension.Tag;
import org.finos.legend.pure.m3.coreinstance.meta.pure.metamodel.extension.TaggedValue;
import org.finos.legend.pure.m3.navigation.M3Paths;
import org.finos.legend.pure.m3.navigation.ProcessorSupport;
import org.finos.legend.pure.m3.navigation.importstub.ImportStub;
import org.finos.legend.pure.m3.tools.matcher.MatchRunner;
import org.finos.legend.pure.m3.tools.matcher.Matcher;
import org.finos.legend.pure.m3.tools.matcher.MatcherState;
import org.finos.legend.pure.m4.ModelRepository;
import org.finos.legend.pure.m4.coreinstance.CoreInstance;
import org.finos.legend.pure.m4.exception.PureCompilationException;

public class ElementWithTaggedValuesUnbind implements MatchRunner
{
    @Override
    public String getClassName()
    {
        return M3Paths.ElementWithTaggedValues;
    }

    @Override
    public void run(CoreInstance element, MatcherState state, Matcher matcher, ModelRepository modelRepository, Context context) throws PureCompilationException
    {
        ElementWithTaggedValues elementWithTaggedValues = ElementWithTaggedValuesCoreInstanceWrapper.toElementWithTaggedValues(element);
        UnbindState unbindState = (UnbindState)state;
        ProcessorSupport processorSupport = unbindState.getProcessorSupport();
        for (TaggedValue taggedValue : elementWithTaggedValues._taggedValues())
        {
            unbindState.freeValidated(taggedValue);
            CoreInstance embeddedTag = taggedValue._tagCoreInstance();
            Tag tag;
            try
            {
                tag = (Tag)ImportStub.withImportStubByPass(embeddedTag, processorSupport);
            }
            catch (PureCompilationException e)
            {
                // Exception is ok here
                tag = null;
            }
            if (tag != null)
            {
                Shared.cleanImportStub(embeddedTag, processorSupport);
                tag._modelElementsRemove(AnnotatedElementCoreInstanceWrapper.toAnnotatedElement(elementWithTaggedValues));
                if (tag._modelElements().isEmpty())
                {
                    tag._modelElementsRemove();
                }
            }
        }
    }
}
