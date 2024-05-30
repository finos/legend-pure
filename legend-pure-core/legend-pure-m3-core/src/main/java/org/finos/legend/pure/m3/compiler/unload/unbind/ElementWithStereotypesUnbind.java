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
import org.finos.legend.pure.m3.coreinstance.meta.pure.metamodel.extension.ElementWithStereotypes;
import org.finos.legend.pure.m3.coreinstance.meta.pure.metamodel.extension.ElementWithStereotypesCoreInstanceWrapper;
import org.finos.legend.pure.m3.coreinstance.meta.pure.metamodel.extension.Stereotype;
import org.finos.legend.pure.m3.navigation.M3Paths;
import org.finos.legend.pure.m3.navigation.ProcessorSupport;
import org.finos.legend.pure.m3.navigation.importstub.ImportStub;
import org.finos.legend.pure.m3.tools.matcher.MatchRunner;
import org.finos.legend.pure.m3.tools.matcher.Matcher;
import org.finos.legend.pure.m3.tools.matcher.MatcherState;
import org.finos.legend.pure.m4.ModelRepository;
import org.finos.legend.pure.m4.coreinstance.CoreInstance;
import org.finos.legend.pure.m4.exception.PureCompilationException;

public class ElementWithStereotypesUnbind implements MatchRunner
{
    @Override
    public String getClassName()
    {
        return M3Paths.ElementWithStereotypes;
    }

    @Override
    public void run(CoreInstance element, MatcherState state, Matcher matcher, ModelRepository modelRepository, Context context) throws PureCompilationException
    {
        ElementWithStereotypes typedElement = ElementWithStereotypesCoreInstanceWrapper.toElementWithStereotypes(element);
        ProcessorSupport processorSupport = state.getProcessorSupport();
        for (CoreInstance embeddedStereotype : typedElement._stereotypesCoreInstance())
        {
            Stereotype stereotype;
            try
            {
                stereotype = (Stereotype)ImportStub.withImportStubByPass(embeddedStereotype, processorSupport);
            }
            catch (PureCompilationException e)
            {
                // Exception is ok here
                stereotype = null;
            }
            if (stereotype != null)
            {
                Shared.cleanImportStub(embeddedStereotype, processorSupport);
                stereotype._modelElementsRemove(AnnotatedElementCoreInstanceWrapper.toAnnotatedElement(element));
                if (stereotype._modelElements().isEmpty())
                {
                    stereotype._modelElementsRemove();
                }
            }
        }
    }
}
