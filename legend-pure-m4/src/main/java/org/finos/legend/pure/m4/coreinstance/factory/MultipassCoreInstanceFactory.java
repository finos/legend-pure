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

package org.finos.legend.pure.m4.coreinstance.factory;

import org.finos.legend.pure.m4.coreinstance.CoreInstance;
import org.finos.legend.pure.m4.ModelRepository;
import org.finos.legend.pure.m4.coreinstance.SourceInformation;

/**
 * Core Instance Factory methods that are only used during deserialization
 */
public interface MultipassCoreInstanceFactory extends CoreInstanceFactory
{
    boolean supports(String classifierPath);

    CoreInstance createCoreInstance(String name, int internalSyntheticId, SourceInformation sourceInformation, String classifierPath, String typeInfo, ModelRepository repository, boolean persistent);

    boolean supports(int classifierSyntheicId);

    CoreInstance createCoreInstance(String name, int internalSyntheticId, SourceInformation sourceInformation, int classifierSyntheticId, ModelRepository repository, boolean persistent);
}
