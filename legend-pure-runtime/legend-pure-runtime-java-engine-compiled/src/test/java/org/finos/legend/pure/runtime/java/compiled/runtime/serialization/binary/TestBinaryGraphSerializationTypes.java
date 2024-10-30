// Copyright 2023 Goldman Sachs
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

package org.finos.legend.pure.runtime.java.compiled.runtime.serialization.binary;

import org.eclipse.collections.api.factory.Lists;
import org.eclipse.collections.api.list.ListIterable;
import org.finos.legend.pure.m4.coreinstance.SourceInformation;
import org.finos.legend.pure.runtime.java.compiled.serialization.binary.BinaryGraphSerializationTypes;
import org.finos.legend.pure.runtime.java.compiled.serialization.model.Obj;
import org.finos.legend.pure.runtime.java.compiled.serialization.model.Primitive;
import org.finos.legend.pure.runtime.java.compiled.serialization.model.PropertyValue;
import org.finos.legend.pure.runtime.java.compiled.serialization.model.PropertyValueOne;
import org.junit.Assert;
import org.junit.Test;

public class TestBinaryGraphSerializationTypes
{
    @Test
    public void testObjSerializationCode()
    {
        String classifier = "meta::pure::some::classifier";
        String id = "some_id";
        String name = "name";
        SourceInformation sourceInfo = new SourceInformation("file.pure", 1, 2, 3, 4, 5, 6);
        ListIterable<PropertyValue> properties = Lists.immutable.with(new PropertyValueOne("property", new Primitive("value")));

        byte codeNone = BinaryGraphSerializationTypes.getObjSerializationCode(Obj.newObj(classifier, id, null, properties, null, false));
        Assert.assertFalse(BinaryGraphSerializationTypes.isEnum(codeNone));
        Assert.assertFalse(BinaryGraphSerializationTypes.hasName(codeNone));
        Assert.assertFalse(BinaryGraphSerializationTypes.hasSourceInfo(codeNone));

        byte codeEnum = BinaryGraphSerializationTypes.getObjSerializationCode(Obj.newObj(classifier, id, null, properties, null, true));
        Assert.assertTrue(BinaryGraphSerializationTypes.isEnum(codeEnum));
        Assert.assertFalse(BinaryGraphSerializationTypes.hasName(codeEnum));
        Assert.assertFalse(BinaryGraphSerializationTypes.hasSourceInfo(codeEnum));

        byte codeEnumName = BinaryGraphSerializationTypes.getObjSerializationCode(Obj.newObj(classifier, id, name, properties, null, true));
        Assert.assertTrue(BinaryGraphSerializationTypes.isEnum(codeEnumName));
        Assert.assertTrue(BinaryGraphSerializationTypes.hasName(codeEnumName));
        Assert.assertFalse(BinaryGraphSerializationTypes.hasSourceInfo(codeEnumName));

        byte codeEnumSource = BinaryGraphSerializationTypes.getObjSerializationCode(Obj.newObj(classifier, id, null, properties, sourceInfo, true));
        Assert.assertTrue(BinaryGraphSerializationTypes.isEnum(codeEnumSource));
        Assert.assertFalse(BinaryGraphSerializationTypes.hasName(codeEnumSource));
        Assert.assertTrue(BinaryGraphSerializationTypes.hasSourceInfo(codeEnumSource));

        byte codeEnumNameSource = BinaryGraphSerializationTypes.getObjSerializationCode(Obj.newObj(classifier, id, name, properties, sourceInfo, true));
        Assert.assertTrue(BinaryGraphSerializationTypes.isEnum(codeEnumNameSource));
        Assert.assertTrue(BinaryGraphSerializationTypes.hasName(codeEnumNameSource));
        Assert.assertTrue(BinaryGraphSerializationTypes.hasSourceInfo(codeEnumNameSource));

        byte codeName = BinaryGraphSerializationTypes.getObjSerializationCode(Obj.newObj(classifier, id, name, properties, null, false));
        Assert.assertFalse(BinaryGraphSerializationTypes.isEnum(codeName));
        Assert.assertTrue(BinaryGraphSerializationTypes.hasName(codeName));
        Assert.assertFalse(BinaryGraphSerializationTypes.hasSourceInfo(codeName));

        byte codeNameSource = BinaryGraphSerializationTypes.getObjSerializationCode(Obj.newObj(classifier, id, name, properties, sourceInfo, false));
        Assert.assertFalse(BinaryGraphSerializationTypes.isEnum(codeNameSource));
        Assert.assertTrue(BinaryGraphSerializationTypes.hasName(codeNameSource));
        Assert.assertTrue(BinaryGraphSerializationTypes.hasSourceInfo(codeNameSource));

        byte codeSource = BinaryGraphSerializationTypes.getObjSerializationCode(Obj.newObj(classifier, id, null, properties, sourceInfo, false));
        Assert.assertFalse(BinaryGraphSerializationTypes.isEnum(codeSource));
        Assert.assertFalse(BinaryGraphSerializationTypes.hasName(codeSource));
        Assert.assertTrue(BinaryGraphSerializationTypes.hasSourceInfo(codeSource));
    }
}
