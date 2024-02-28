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

package org.finos.legend.pure.m2.dsl.diagram.serialization.grammar.unloader;

import org.eclipse.collections.api.RichIterable;
import org.eclipse.collections.api.block.procedure.Procedure2;
import org.finos.legend.pure.m2.dsl.diagram.M2DiagramPaths;
import org.finos.legend.pure.m3.navigation.M3Paths;
import org.finos.legend.pure.m3.compiler.Context;
import org.finos.legend.pure.m3.compiler.unload.unbind.Shared;
import org.finos.legend.pure.m3.coreinstance.meta.pure.diagram.AbstractPathView;
import org.finos.legend.pure.m3.coreinstance.meta.pure.diagram.AssociationView;
import org.finos.legend.pure.m3.coreinstance.meta.pure.diagram.Diagram;
import org.finos.legend.pure.m3.coreinstance.meta.pure.diagram.GeneralizationView;
import org.finos.legend.pure.m3.coreinstance.meta.pure.diagram.PropertyView;
import org.finos.legend.pure.m3.coreinstance.meta.pure.diagram.TypeView;
import org.finos.legend.pure.m3.coreinstance.meta.pure.tools.GrammarInfoStub;
import org.finos.legend.pure.m3.navigation.ProcessorSupport;
import org.finos.legend.pure.m3.tools.matcher.MatchRunner;
import org.finos.legend.pure.m3.tools.matcher.Matcher;
import org.finos.legend.pure.m3.tools.matcher.MatcherState;
import org.finos.legend.pure.m4.coreinstance.CoreInstance;
import org.finos.legend.pure.m4.ModelRepository;
import org.finos.legend.pure.m4.exception.PureCompilationException;

public class DiagramUnbind implements MatchRunner<Diagram>
{
    private static final Procedure2<CoreInstance, ProcessorSupport> CLEAN_UP_TYPE_VIEW_REFERENCE = new Procedure2<CoreInstance, ProcessorSupport>()
    {
        @Override
        public void value(CoreInstance typeView, ProcessorSupport processorSupport)
        {
            CoreInstance referenceStub = ((TypeView)typeView)._typeCoreInstance();
            Shared.cleanUpReferenceUsage(referenceStub, typeView, processorSupport);
            Shared.cleanImportStub(referenceStub, processorSupport);
        }
    };

    private static final Procedure2<AssociationView, ProcessorSupport> CLEAN_UP_ASSOCIATION_VIEW_REFERENCE = new Procedure2<AssociationView, ProcessorSupport>()
    {
        @Override
        public void value(AssociationView associationView, ProcessorSupport processorSupport)
        {
            CoreInstance referenceStub = associationView._associationCoreInstance();
            Shared.cleanUpReferenceUsage(referenceStub, associationView, processorSupport);
            Shared.cleanImportStub(referenceStub, processorSupport);
        }
    };

    private static final Procedure2<PropertyView, ProcessorSupport> CLEAN_UP_PROPERTY_VIEW_REFERENCE = new Procedure2<PropertyView, ProcessorSupport>()
    {
        @Override
        public void value(PropertyView propertyView, ProcessorSupport processorSupport)
        {
            CoreInstance referenceStub = propertyView._propertyCoreInstance();
            Shared.cleanUpReferenceUsage(referenceStub, propertyView, processorSupport);
            Shared.cleanPropertyStub(referenceStub, processorSupport);
        }
    };

    private static final Procedure2<AbstractPathView, ProcessorSupport> RESET_PATH_VIEW_SOURCE_TARGET = new Procedure2<AbstractPathView, ProcessorSupport>()
    {
        @Override
        public void value(AbstractPathView abstractPathView, ProcessorSupport processorSupport)
        {
            if (abstractPathView._sourceCoreInstance() != null && !M3Paths.String.equals(abstractPathView._sourceCoreInstance().getClassifier().getName()))
            {
                GrammarInfoStub sourceTypeView = (GrammarInfoStub)abstractPathView._sourceCoreInstance();
                abstractPathView._sourceRemove();
                abstractPathView._sourceCoreInstance(sourceTypeView._originalCoreInstance());
            }

            if (abstractPathView._targetCoreInstance() != null && !M3Paths.String.equals(abstractPathView._targetCoreInstance().getClassifier().getName()))
            {
                GrammarInfoStub targetTypeView = (GrammarInfoStub)abstractPathView._targetCoreInstance();
                abstractPathView._targetRemove();
                abstractPathView._targetCoreInstance(targetTypeView._originalCoreInstance());
            }
        }
    };

    @Override
    public String getClassName()
    {
        return M2DiagramPaths.Diagram;
    }

    @Override
    public void run(Diagram diagram, MatcherState state, Matcher matcher, ModelRepository modelRepository, Context context) throws PureCompilationException
    {
        ProcessorSupport processorSupport = state.getProcessorSupport();

        RichIterable<? extends CoreInstance> typeViews = diagram._typeViewsCoreInstance();
        RichIterable<? extends AssociationView> associationViews = diagram._associationViews();
        RichIterable<? extends PropertyView> propertyViews = diagram._propertyViews();
        RichIterable<? extends GeneralizationView> generalizationViews = diagram._generalizationViews();

        typeViews.forEachWith(CLEAN_UP_TYPE_VIEW_REFERENCE, processorSupport);
        associationViews.forEachWith(CLEAN_UP_ASSOCIATION_VIEW_REFERENCE, processorSupport);
        propertyViews.forEachWith(CLEAN_UP_PROPERTY_VIEW_REFERENCE, processorSupport);

        associationViews.forEachWith(RESET_PATH_VIEW_SOURCE_TARGET, processorSupport);
        propertyViews.forEachWith(RESET_PATH_VIEW_SOURCE_TARGET, processorSupport);
        generalizationViews.forEachWith(RESET_PATH_VIEW_SOURCE_TARGET, processorSupport);
    }
}
