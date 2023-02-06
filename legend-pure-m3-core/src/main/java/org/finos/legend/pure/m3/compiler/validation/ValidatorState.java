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

package org.finos.legend.pure.m3.compiler.validation;

import org.eclipse.collections.api.map.MutableMap;
import org.eclipse.collections.impl.factory.Maps;
import org.finos.legend.pure.m3.navigation.Instance;
import org.finos.legend.pure.m3.navigation.M3Properties;
import org.finos.legend.pure.m3.navigation.PackageableElement.PackageableElement;
import org.finos.legend.pure.m3.navigation.ProcessorSupport;
import org.finos.legend.pure.m3.serialization.filesystem.usercodestorage.CodeStorage;
import org.finos.legend.pure.m3.serialization.grammar.m3parser.inlinedsl.InlineDSLLibrary;
import org.finos.legend.pure.m3.tools.matcher.MatcherState;
import org.finos.legend.pure.m4.coreinstance.CoreInstance;
import org.finos.legend.pure.m4.coreinstance.SourceInformation;
import org.finos.legend.pure.m4.exception.PureCompilationException;

public class ValidatorState extends MatcherState
{
    private final ValidationType validationType;
    private final CodeStorage codeStorage;
    private CoreInstance rootMapping;
    private final MutableMap<String, CoreInstance> setImplementationsById = Maps.mutable.empty();
    private final MutableMap<String, CoreInstance> enumerationMappingsById = Maps.mutable.empty();
    private final InlineDSLLibrary inlineDSLLibrary;

    public ValidatorState(ValidationType validationType, CodeStorage codeStorage, InlineDSLLibrary inlineDSLLibrary, ProcessorSupport processorSupport)
    {
        super(processorSupport);
        this.validationType = validationType;
        this.codeStorage = codeStorage;
        this.inlineDSLLibrary = inlineDSLLibrary;
    }

    public ValidationType getValidationType()
    {
        return this.validationType;
    }

    public CodeStorage getCodeStorage()
    {
        return this.codeStorage;
    }

    public CoreInstance getRootMapping()
    {
        return this.rootMapping;
    }

    public void setRootMapping(CoreInstance rootMapping)
    {
        this.rootMapping = rootMapping;
    }

    // TODO move this logic elsewhere
    public void validateSetImplementation(CoreInstance mappedInstance, boolean isClassMapping)
    {
        // Note: this validation may not work (especially in deep validation) depending on the order in which things are validated
        if (this.rootMapping != null)
        {
            if (isClassMapping)
            {
                String id = mappedInstance.getValueForMetaPropertyToOne(M3Properties.id).getName();
                CoreInstance old = this.setImplementationsById.put(id, mappedInstance);
                if ((old != null) && (old != mappedInstance))
                {
                    throwDuplicateClassMappingException(mappedInstance, id);
                }
            }
            else
            {
                String name = mappedInstance.getValueForMetaPropertyToOne(M3Properties.name).getName();
                CoreInstance old = this.enumerationMappingsById.put(name, mappedInstance);
                if ((old != null) && (old != mappedInstance))
                {
                    SourceInformation sourceInfo = mappedInstance.getSourceInformation();
                    if ((sourceInfo == null) || (sourceInfo.getColumn() == 0))
                    {
                        sourceInfo = mappedInstance.getValueForMetaPropertyToOne(M3Properties.enumeration).getSourceInformation();
                    }
                    throw new PureCompilationException(sourceInfo, "Duplicate mapping found with id: '" + name + "' in mapping " + PackageableElement.getUserPathForPackageableElement(this.rootMapping));
                }
            }
        }
    }

    // TODO move this logic elsewhere
    private void throwDuplicateClassMappingException(CoreInstance mappedInstance, String id)
    {
        SourceInformation sourceInfo = mappedInstance.getSourceInformation();
        if ((sourceInfo == null) || (sourceInfo.getColumn() == 0))
        {
            if (Instance.instanceOf(mappedInstance, "meta::pure::mapping::SetImplementation", this.processorSupport))
            {
                sourceInfo = mappedInstance.getValueForMetaPropertyToOne(M3Properties._class).getSourceInformation();
            }
            else
            {
                sourceInfo = mappedInstance.getValueForMetaPropertyToOne(M3Properties.association).getSourceInformation();
            }
        }
        throw new PureCompilationException(sourceInfo, "Duplicate mapping found with id: '" + id + "' in mapping " + PackageableElement.getUserPathForPackageableElement(this.rootMapping));

    }

    public void resetSetImplementationList()
    {
        this.setImplementationsById.clear();
        this.enumerationMappingsById.clear();
    }

    @Override
    public InlineDSLLibrary getInlineDSLLibrary()
    {
        return this.inlineDSLLibrary;
    }

}
