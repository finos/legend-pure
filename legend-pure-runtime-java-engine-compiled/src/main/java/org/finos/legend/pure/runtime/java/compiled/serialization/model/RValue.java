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

package org.finos.legend.pure.runtime.java.compiled.serialization.model;

import org.eclipse.collections.api.block.function.Function2;
import org.eclipse.collections.api.block.procedure.Procedure2;

public interface RValue
{
    Function2<RValue, RValueVisitor, Object> VISIT = new Function2<RValue, RValueVisitor, Object>()
    {
        @Override
        public Object value(RValue value, RValueVisitor visitor)
        {
            return value.visit(visitor);
        }
    };

    Procedure2<RValue, RValueVisitor> VISIT_PROCEDURE = new Procedure2<RValue, RValueVisitor>()
    {
        @Override
        public void value(RValue value, RValueVisitor visitor)
        {
            value.visit(visitor);
        }
    };

    Object visit(RValueVisitor visitor);
}
