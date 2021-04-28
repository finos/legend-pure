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

package org.finos.legend.pure.m3.compiler.postprocessing.processor;

import org.finos.legend.pure.m3.compiler.Context;
import org.finos.legend.pure.m3.coreinstance.meta.pure.metamodel.extension.AnnotatedElement;
import org.finos.legend.pure.m3.coreinstance.meta.pure.metamodel.extension.AnnotatedElementCoreInstanceWrapper;
import org.finos.legend.pure.m3.coreinstance.meta.pure.metamodel.extension.Stereotype;
import org.finos.legend.pure.m3.coreinstance.meta.pure.metamodel.extension.Tag;
import org.finos.legend.pure.m3.coreinstance.meta.pure.metamodel.extension.TaggedValue;
import org.finos.legend.pure.m3.navigation.M3Paths;
import org.finos.legend.pure.m3.navigation.ProcessorSupport;
import org.finos.legend.pure.m3.navigation.importstub.ImportStub;
import org.finos.legend.pure.m3.tools.ListHelper;
import org.finos.legend.pure.m3.tools.matcher.MatchRunner;
import org.finos.legend.pure.m3.tools.matcher.Matcher;
import org.finos.legend.pure.m3.tools.matcher.MatcherState;
import org.finos.legend.pure.m4.ModelRepository;
import org.finos.legend.pure.m4.coreinstance.CoreInstance;
import org.finos.legend.pure.m4.exception.PureCompilationException;

public class AnnotatedElementProcessor implements MatchRunner<AnnotatedElement>
{
    @Override
    public String getClassName()
    {
        return M3Paths.AnnotatedElement;
    }

    @Override
    public void run(AnnotatedElement instance, MatcherState state, Matcher matcher, ModelRepository modelRepository, Context context) throws PureCompilationException
    {
        noteModelElementForAnnotations(instance, state.getProcessorSupport());
    }

    public static void noteModelElementForAnnotations(CoreInstance instance, ProcessorSupport processorSupport)
    {
        noteModelElementForAnnotations(AnnotatedElementCoreInstanceWrapper.toAnnotatedElement(instance), processorSupport);
    }

    public static void noteModelElementForAnnotations(AnnotatedElement annotatedElement, ProcessorSupport processorSupport)
    {
        for (CoreInstance stereotype : ImportStub.withImportStubByPasses(ListHelper.wrapListIterable(annotatedElement._stereotypesCoreInstance()), processorSupport))
        {
            ((Stereotype) stereotype)._modelElementsAdd(annotatedElement);
        }
        for (TaggedValue taggedValue : annotatedElement._taggedValues())
        {
            Tag tag = (Tag) ImportStub.withImportStubByPass(taggedValue._tagCoreInstance(), processorSupport);
            tag._modelElementsAdd(annotatedElement);
        }
    }
}
