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

import org.eclipse.collections.api.list.ListIterable;
import org.finos.legend.pure.m3.compiler.Context;
import org.finos.legend.pure.m3.coreinstance.meta.pure.metamodel.relationship.Generalization;
import org.finos.legend.pure.m3.coreinstance.meta.pure.metamodel.type.Type;
import org.finos.legend.pure.m3.coreinstance.meta.pure.metamodel.type.generics.GenericType;
import org.finos.legend.pure.m3.navigation.M3Paths;
import org.finos.legend.pure.m3.navigation.M3Properties;
import org.finos.legend.pure.m3.navigation.ProcessorSupport;
import org.finos.legend.pure.m3.navigation.importstub.ImportStub;
import org.finos.legend.pure.m3.tools.matcher.MatchRunner;
import org.finos.legend.pure.m3.tools.matcher.Matcher;
import org.finos.legend.pure.m3.tools.matcher.MatcherState;
import org.finos.legend.pure.m4.ModelRepository;
import org.finos.legend.pure.m4.coreinstance.CoreInstance;
import org.finos.legend.pure.m4.coreinstance.indexing.IndexSpecifications;
import org.finos.legend.pure.m4.exception.PureCompilationException;

public class TypeUnbind implements MatchRunner<Type>
{
    @Override
    public String getClassName()
    {
        return M3Paths.Type;
    }

    @Override
    public void run(Type type, MatcherState state, Matcher matcher, ModelRepository modelRepository, Context context) throws PureCompilationException
    {
        ProcessorSupport processorSupport = state.getProcessorSupport();

        for (Generalization generalization : type._generalizations())
        {
            GenericType general = generalization._general();
            Type generalRawType = null;
            try
            {
                generalRawType = (Type)ImportStub.withImportStubByPass(general._rawTypeCoreInstance(), processorSupport);
            }
            catch (PureCompilationException ex)
            {
                //Ignore - if we can't resolve this type was probably already unbound
            }

            if (generalRawType != null)
            {
                ListIterable<? extends CoreInstance> specializationsToRemove = generalRawType.getValueInValueForMetaPropertyToManyByIndex(M3Properties.specializations, IndexSpecifications.getPropertyValueIndexSpec(M3Properties.specific), type);
                for (CoreInstance specialization : specializationsToRemove.toList())
                {
                    generalRawType._specializationsRemove((Generalization)specialization);
                }
                if (generalRawType.getValueForMetaPropertyToMany(M3Properties.specializations).isEmpty())
                {
                    generalRawType._specializationsRemove();
                }
            }

            Shared.cleanUpGenericType(general, (UnbindState)state, processorSupport);
        }
    }
}
