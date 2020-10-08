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

package org.finos.legend.pure.m3.serialization.grammar.m3parser.antlr;

import org.eclipse.collections.api.tuple.Pair;
import org.finos.legend.pure.m3.coreinstance.meta.pure.metamodel.multiplicity.Multiplicity;
import org.finos.legend.pure.m3.coreinstance.meta.pure.metamodel.type.generics.GenericType;
import org.finos.legend.pure.m4.coreinstance.CoreInstance;
import org.finos.legend.pure.m4.coreinstance.SourceInformation;

public class TemporaryPurePropertyMapping
{
    public SourceInformation sourceInformation;
    public String property;
    public CoreInstance expression;
    public String sourceMappingId;
    public String targetMappingId;
    public boolean localMappingProperty;
    public GenericType localMappingPropertyType;
    public Multiplicity localMappingPropertyMultiplicity;
    public boolean explodeProperty;
    public Pair<String, SourceInformation> enumerationMappingInformation;


    TemporaryPurePropertyMapping(SourceInformation sourceInformation, boolean localMappingProperty, GenericType localMappingPropertyType, Multiplicity localMappingPropertyMultiplicity, String property, CoreInstance expression, String sourceMappingId, String targetMappingId, boolean explodeProperty, Pair<String, SourceInformation> enumerationMappingInformation)
    {
        this.sourceInformation = sourceInformation;
        this.localMappingProperty = localMappingProperty;
        this.localMappingPropertyType = localMappingPropertyType;
        this.localMappingPropertyMultiplicity = localMappingPropertyMultiplicity;
        this.property = property;
        this.expression = expression;
        this.targetMappingId = targetMappingId;
        this.sourceMappingId = sourceMappingId;
        this.explodeProperty = explodeProperty;
        this.enumerationMappingInformation = enumerationMappingInformation;
    }

    public static TemporaryPurePropertyMapping build(SourceInformation sourceInformation, boolean localMappingProperty, GenericType localMappingPropertyType, Multiplicity localMappingPropertyMultiplicity, String property, CoreInstance expression, String sourceMappingId, String targetMappingId, boolean explodeProperty, Pair<String, SourceInformation> enumerationMappingInformation)
    {
        return new TemporaryPurePropertyMapping(sourceInformation, localMappingProperty, localMappingPropertyType, localMappingPropertyMultiplicity, property, expression, sourceMappingId, targetMappingId, explodeProperty, enumerationMappingInformation);
    }
}
