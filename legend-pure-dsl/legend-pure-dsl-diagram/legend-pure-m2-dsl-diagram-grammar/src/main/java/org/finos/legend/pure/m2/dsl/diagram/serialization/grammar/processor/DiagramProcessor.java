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

package org.finos.legend.pure.m2.dsl.diagram.serialization.grammar.processor;

import org.eclipse.collections.api.RichIterable;
import org.eclipse.collections.api.map.MapIterable;
import org.eclipse.collections.api.map.MutableMap;
import org.eclipse.collections.impl.factory.Maps;
import org.eclipse.collections.impl.utility.LazyIterate;
import org.finos.legend.pure.m2.dsl.diagram.M2DiagramPaths;
import org.finos.legend.pure.m3.navigation.M3Paths;
import org.finos.legend.pure.m3.navigation.M3Properties;
import org.finos.legend.pure.m3.SourceMutation;
import org.finos.legend.pure.m3.compiler.Context;
import org.finos.legend.pure.m3.compiler.postprocessing.ProcessorState;
import org.finos.legend.pure.m3.compiler.postprocessing.processor.Processor;
import org.finos.legend.pure.m3.navigation._package._Package;
import org.finos.legend.pure.m3.navigation.importstub.ImportStub;
import org.finos.legend.pure.m3.coreinstance.meta.pure.diagram.AbstractPathView;
import org.finos.legend.pure.m3.coreinstance.meta.pure.diagram.AssociationView;
import org.finos.legend.pure.m3.coreinstance.meta.pure.diagram.Diagram;
import org.finos.legend.pure.m3.coreinstance.meta.pure.diagram.DiagramNode;
import org.finos.legend.pure.m3.coreinstance.meta.pure.diagram.GeneralizationView;
import org.finos.legend.pure.m3.coreinstance.meta.pure.diagram.PropertyView;
import org.finos.legend.pure.m3.coreinstance.meta.pure.diagram.TypeView;
import org.finos.legend.pure.m3.coreinstance.meta.pure.metamodel.type.Type;
import org.finos.legend.pure.m3.coreinstance.meta.pure.tools.GrammarInfoStub;
import org.finos.legend.pure.m3.navigation.ProcessorSupport;
import org.finos.legend.pure.m3.tools.matcher.Matcher;
import org.finos.legend.pure.m4.coreinstance.CoreInstance;
import org.finos.legend.pure.m4.ModelRepository;
import org.finos.legend.pure.m4.exception.PureCompilationException;

public class DiagramProcessor extends Processor<Diagram>
{
    @Override
    public String getClassName()
    {
        return M2DiagramPaths.Diagram;
    }

    @Override
    public void process(Diagram diagram, ProcessorState state, Matcher matcher, ModelRepository repository, Context context, ProcessorSupport processorSupport)
    {
        RichIterable<? extends CoreInstance> typeViews = diagram._typeViewsCoreInstance();
        RichIterable<? extends CoreInstance> associationViews = diagram._associationViews();
        RichIterable<? extends CoreInstance> propertyViews = diagram._propertyViews();
        RichIterable<? extends CoreInstance> generalizationViews = diagram._generalizationViews();

        // Register view references
        this.registerViewReferences(typeViews, state, processorSupport);
        this.registerViewReferences(associationViews, state, processorSupport);
        this.registerViewReferences(propertyViews, state, processorSupport);

        // Resolve ids
        MapIterable<String, CoreInstance> viewsById = this.indexViewsById(diagram,
                LazyIterate.concatenate((Iterable<CoreInstance>)typeViews,
                        (Iterable<CoreInstance>)associationViews,
                        (Iterable<CoreInstance>)propertyViews,
                        (Iterable<CoreInstance>)generalizationViews).reject(SourceMutation.IS_MARKED_FOR_DELETION));
        for (CoreInstance view : LazyIterate.concatenate((Iterable<CoreInstance>)associationViews, (Iterable<CoreInstance>)propertyViews, (Iterable<CoreInstance>)generalizationViews).reject(SourceMutation.IS_MARKED_FOR_DELETION))
        {
            this.resolvePathViewSourceAndTarget((AbstractPathView)view, viewsById, state, repository, processorSupport);
        }

        // Special handling for generalization views
        for (CoreInstance each : generalizationViews.reject(SourceMutation.IS_MARKED_FOR_DELETION))
        {
            GeneralizationView generalizationView = (GeneralizationView)each;
            Type source = (Type)ImportStub.withImportStubByPass(((TypeView)((GrammarInfoStub)generalizationView._sourceCoreInstance())._value())._typeCoreInstance(), processorSupport);
            Type target = (Type)ImportStub.withImportStubByPass(((TypeView)((GrammarInfoStub)generalizationView._targetCoreInstance())._value())._typeCoreInstance(), processorSupport);
            if (!org.finos.legend.pure.m3.navigation.type.Type.directSubTypeOf(source, target, processorSupport))
            {
                state.getSourceMutation().delete(generalizationView);
            }
        }
    }

    @Override
    public void populateReferenceUsages(Diagram diagram, ModelRepository repository, ProcessorSupport processorSupport)
    {
        // type
        for (CoreInstance view : LazyIterate.reject(diagram._typeViewsCoreInstance(), SourceMutation.IS_MARKED_FOR_DELETION))
        {
            this.addReferenceUsageForToOneProperty(view, ((TypeView)view)._typeCoreInstance(), M3Properties.type, repository, processorSupport, ((TypeView)view)._typeCoreInstance().getSourceInformation());
        }
        // association
        for (AssociationView view : LazyIterate.reject(diagram._associationViews(), SourceMutation.IS_MARKED_FOR_DELETION))
        {
            this.addReferenceUsageForToOneProperty(view, view._associationCoreInstance(), M3Properties.association, repository, processorSupport, view._associationCoreInstance().getSourceInformation());
        }
        // property
        for (PropertyView view : LazyIterate.reject(diagram._propertyViews(), SourceMutation.IS_MARKED_FOR_DELETION))
        {
            this.addReferenceUsageForToOneProperty(view, view._propertyCoreInstance(), M3Properties.property, repository, processorSupport);
        }
    }

    private void registerViewReferences(Iterable<? extends CoreInstance> views, ProcessorState state, ProcessorSupport processorSupport)
    {
        for (CoreInstance view : views)
        {
            this.registerViewReference(view, state, processorSupport);
        }
    }

    private void registerViewReference(CoreInstance view, ProcessorState state, ProcessorSupport processorSupport)
    {
        try
        {
            if (view instanceof TypeView)
            {
                ImportStub.withImportStubByPass(((TypeView)view)._typeCoreInstance(), processorSupport);
            }
            else if (view instanceof AssociationView)
            {
                ImportStub.withImportStubByPass(((AssociationView)view)._associationCoreInstance(), processorSupport);
            }
            else if (view instanceof PropertyView)
            {
                ImportStub.withImportStubByPass(((PropertyView)view)._propertyCoreInstance(), processorSupport);
            }
        }
        catch (PureCompilationException e)
        {
            state.getSourceMutation().delete(view);
        }
    }

    private void resolvePathViewSourceAndTarget(AbstractPathView view, MapIterable<String, CoreInstance> viewsById, ProcessorState state, ModelRepository repository, ProcessorSupport processorSupport)
    {
        GrammarInfoStub sourceViewStub = view._sourceCoreInstance() != null ? (GrammarInfoStub)repository.newAnonymousCoreInstance(view._sourceCoreInstance().getSourceInformation(), _Package.getByUserPath("meta::pure::tools::GrammarInfoStub", state.getProcessorSupport())) : null;
        GrammarInfoStub targetViewStub = view._targetCoreInstance() != null ? (GrammarInfoStub)repository.newAnonymousCoreInstance(view._targetCoreInstance().getSourceInformation(), _Package.getByUserPath("meta::pure::tools::GrammarInfoStub", state.getProcessorSupport())) : null;

        String sourceViewId = view._sourceCoreInstance() != null ? view._sourceCoreInstance().getName() : null;
        String targetViewId = view._targetCoreInstance() != null ? view._targetCoreInstance().getName() : null;

        CoreInstance sourceView = viewsById.get(sourceViewId);
        CoreInstance targetView = viewsById.get(targetViewId);

        // source view
        if (sourceView == null)
        {
            state.getSourceMutation().delete(view);
            return;
        }
        if (!(sourceView instanceof TypeView))
        {
            throw new PureCompilationException(view.getSourceInformation(), "Object with id '" + sourceViewId + "' is not a TypeView");
        }
        if (sourceViewStub != null)
        {
            sourceViewStub._original(view._sourceCoreInstance());
            sourceViewStub._value(sourceView);
        }
        view._sourceCoreInstance(sourceViewStub);

        // target view
        if (targetView == null)
        {
            state.getSourceMutation().delete(view);
            return;
        }
        if (!(targetView instanceof TypeView))
        {
            throw new PureCompilationException(view.getSourceInformation(), "Object with id '" + targetViewId + "' is not a TypeView");
        }
        if (targetViewStub != null)
        {
            targetViewStub._original(view._targetCoreInstance());
            targetViewStub._value(targetView);
        }
        view._targetCoreInstance(targetViewStub);
    }

    private MapIterable<String, CoreInstance> indexViewsById(CoreInstance diagram, RichIterable<? extends CoreInstance> views) throws PureCompilationException
    {
        MutableMap<String, CoreInstance> result = Maps.mutable.with();
        for (CoreInstance view : views)
        {
            String id = ((DiagramNode)view)._id();
            if (id == null)
            {
                throw new PureCompilationException(view.getSourceInformation(), "Missing id in diagram view");
            }
            if (result.containsKey(id))
            {
                throw new PureCompilationException(diagram.getSourceInformation(), "Id '" + id + "' is used more than once");
            }
            result.put(id, view);
        }
        return result;
    }
}
