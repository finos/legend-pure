// Copyright 2026 Goldman Sachs
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

package org.finos.legend.pure.m2.dsl.mapping.serialization.grammar.v1.unloader;

import org.eclipse.collections.api.factory.Lists;
import org.eclipse.collections.api.factory.Sets;
import org.eclipse.collections.api.list.ListIterable;
import org.eclipse.collections.api.list.MutableList;
import org.eclipse.collections.api.set.MutableSet;
import org.finos.legend.pure.m2.dsl.mapping.M2MappingPaths;
import org.finos.legend.pure.m2.dsl.mapping.serialization.grammar.v1.processor.ModelJoinProcessor;
import org.finos.legend.pure.m2.dsl.mapping.serialization.grammar.v1.processor.ModelJoinShared;
import org.finos.legend.pure.m3.compiler.Context;
import org.finos.legend.pure.m3.compiler.unload.unbind.Shared;
import org.finos.legend.pure.m3.coreinstance.meta.pure.mapping.PropertyMapping;
import org.finos.legend.pure.m3.coreinstance.meta.pure.mapping.modelJoin.ModelJoinAssociationImplementation;
import org.finos.legend.pure.m3.coreinstance.meta.pure.mapping.modelJoin.ModelJoinPropertyMapping;
import org.finos.legend.pure.m3.coreinstance.meta.pure.metamodel.function.LambdaFunction;
import org.finos.legend.pure.m3.coreinstance.meta.pure.metamodel.function.property.Property;
import org.finos.legend.pure.m3.coreinstance.meta.pure.metamodel.relationship.Association;
import org.finos.legend.pure.m3.navigation.ProcessorSupport;
import org.finos.legend.pure.m3.navigation.importstub.ImportStub;
import org.finos.legend.pure.m3.tools.matcher.MatchRunner;
import org.finos.legend.pure.m3.tools.matcher.Matcher;
import org.finos.legend.pure.m3.tools.matcher.MatcherState;
import org.finos.legend.pure.m4.ModelRepository;
import org.finos.legend.pure.m4.coreinstance.CoreInstance;
import org.finos.legend.pure.m4.exception.PureCompilationException;

public class ModelJoinUnbind implements MatchRunner<ModelJoinAssociationImplementation>
{
    @Override
    public String getClassName()
    {
        return M2MappingPaths.ModelJoinAssociationImplementation;
    }

    @Override
    public void run(ModelJoinAssociationImplementation instance, MatcherState state, Matcher matcher, ModelRepository modelRepository, Context context) throws PureCompilationException
    {
        ProcessorSupport processorSupport = state.getProcessorSupport();

        // Resolve association BEFORE cleaning stubs — need property names to reverse param rename.
        // Resolution may fail if the source containing the association has been deleted (incremental),
        // in which case we skip the param rename (the lambda will be discarded anyway).
        String propName1 = null;
        String propName2 = null;
        try
        {
            Association association = (Association) ImportStub.withImportStubByPass(instance._associationCoreInstance(), processorSupport);
            ListIterable<? extends Property<?, ?>> assocProps = association._properties().toList();
            propName1 = !assocProps.isEmpty() ? assocProps.get(0)._name() : null;
            propName2 = assocProps.size() >= 2 ? assocProps.get(1)._name() : null;
        }
        catch (Exception ignored)
        {
            // Association no longer resolvable — source was deleted
        }

        Shared.cleanImportStub(instance._associationCoreInstance(), processorSupport);
        Shared.cleanImportStub(instance._parentCoreInstance(), processorSupport);

        // Unbind all property mappings (including dynamically generated clones)
        for (PropertyMapping propertyMapping : instance._propertyMappings())
        {
            ModelJoinPropertyMapping mjPm = (ModelJoinPropertyMapping) propertyMapping;
            Shared.cleanPropertyStub(mjPm._propertyCoreInstance(), processorSupport);
            if (mjPm._joinCondition() != null)
            {
                matcher.fullMatch(mjPm._joinCondition(), state);
            }
        }

        // Keep only the original 2 template PMs (first PM per distinct property stub).
        MutableList<PropertyMapping> originals = Lists.mutable.empty();
        MutableSet<CoreInstance> seenProperties = Sets.mutable.empty();
        for (PropertyMapping pm : instance._propertyMappings())
        {
            if (seenProperties.add(pm._propertyCoreInstance()))
            {
                originals.add(pm);
            }
        }
        instance._propertyMappings(originals);

        if (propName1 != null && propName2 != null)
        {
            for (PropertyMapping pm : originals)
            {
                ModelJoinPropertyMapping mjPm = (ModelJoinPropertyMapping) pm;
                restoreParamNames(mjPm, propName1, propName2, processorSupport);
            }
        }
    }

    /**
     * Reverses {@code ModelJoinProcessor.replaceLambdaParams} symbolically:
     * renames {@code _mj_tgt} back to the user's original target-side param name (which
     * is the association property name corresponding to this PM) and {@code _mj_src}
     * back to the other association property's name. Generic types are cleared.
     */
    private void restoreParamNames(ModelJoinPropertyMapping pm, String propName1, String propName2, ProcessorSupport processorSupport)
    {
        LambdaFunction<?> lambda = pm._joinCondition();

        // Determine which association property name is the target side for THIS PM.
        // Convention: the PM's own _propertyCoreInstance is the property mapped to the
        // target side. The other association property is the source side.
        String tgtName;
        String srcName;
        CoreInstance pmPropertyCI = pm._propertyCoreInstance();
        Property<?, ?> pmProperty = pmPropertyCI == null
                ? null
                : (Property<?, ?>) ImportStub.withImportStubByPass(pmPropertyCI, processorSupport);
        if (pmProperty != null && propName1.equals(pmProperty._name()))
        {
            tgtName = propName1;
            srcName = propName2;
        }
        else if (pmProperty != null && propName2.equals(pmProperty._name()))
        {
            tgtName = propName2;
            srcName = propName1;
        }
        else
        {
            // Property reference unresolvable (e.g., partial state) — fall back to position-based
            // restore. Better than skipping entirely; rebind will surface any mismatch.
            tgtName = propName2;
            srcName = propName1;
        }

        // _mj_src → srcName, _mj_tgt → tgtName, types cleared (null) on both decls and body refs.
        ModelJoinShared.renameLambdaParamsAndBody(lambda,
                ModelJoinProcessor.MJ_SOURCE_VAR, srcName, null,
                ModelJoinProcessor.MJ_TARGET_VAR, tgtName, null,
                processorSupport);
    }
}
