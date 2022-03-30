package org.finos.legend.pure.runtime.java.compiled.serialization.model;

import org.eclipse.collections.api.factory.Lists;
import org.eclipse.collections.api.list.ListIterable;
import org.eclipse.collections.impl.list.fixed.ArrayAdapter;
import org.finos.legend.pure.m4.coreinstance.SourceInformation;
import org.junit.Assert;
import org.junit.Test;

import java.util.Arrays;

public class TestObj
{
    @Test
    public void testSingleObjMerge()
    {
        String classifier = "meta::some::Classifier";
        String identifier = "my_id";
        String name = "some_name";
        SourceInformation sourceInfo = new SourceInformation("/some/source0.pure", 1, 2, 3, 4, 5, 6);
        ListIterable<PropertyValue> propertyValues = Lists.mutable.with(newPrimitivePropertyValue("prop1", "val1"), newPrimitivePropertyValue("prop2", 1, 2, 3));

        Obj sourceInfoObj = Obj.newObj(classifier, identifier, name, propertyValues, sourceInfo, false);
        Assert.assertSame(sourceInfoObj, Obj.merge(sourceInfoObj));

        Obj sourceInfoEnum = Obj.newObj(classifier, identifier, name, propertyValues, sourceInfo, true);
        Assert.assertSame(sourceInfoEnum, Obj.merge(sourceInfoEnum));

        Obj noSourceInfoObj = Obj.newObj(classifier, identifier, name, propertyValues, null, false);
        Assert.assertSame(noSourceInfoObj, Obj.merge(noSourceInfoObj));

        Obj noSourceInfoEnum = Obj.newObj(classifier, identifier, name, propertyValues, null, true);
        Assert.assertSame(noSourceInfoEnum, Obj.merge(noSourceInfoEnum));
    }

    @Test
    public void testSimpleMerge()
    {
        String classifier = "meta::some::other::Classifier";
        String identifier = "an_id";
        String name = "other_name";
        SourceInformation sourceInfo = new SourceInformation("/some/source1.pure", 6, 5, 4, 3, 2, 1);
        String prop1 = "prop1";
        String prop2 = "prop2";

        Obj first = Obj.newObj(classifier, identifier, name, Lists.mutable.with(newPrimitivePropertyValue(prop1, "a", "b")), sourceInfo, false);
        Obj second = Obj.newObj(classifier, identifier, null, Lists.mutable.with(newPrimitivePropertyValue(prop1, "c", "d"), newPrimitivePropertyValue(prop2, 7)), null, false);
        Assert.assertEquals(
                Obj.newObj(classifier, identifier, name, Lists.mutable.with(newPrimitivePropertyValue(prop1, "a", "b", "c", "d"), newPrimitivePropertyValue(prop2, 7)), sourceInfo, false),
                Obj.merge(first, second));
        Assert.assertEquals(
                Obj.newObj(classifier, identifier, name, Lists.mutable.with(newPrimitivePropertyValue(prop1, "c", "d", "a", "b"), newPrimitivePropertyValue(prop2, 7)), sourceInfo, false),
                Obj.merge(second, first));
    }

    @Test
    public void testSimpleEnumMerge()
    {
        String classifier = "model::MyEnumeration";
        String identifier = "enum_value";
        String name = "enum_value";
        SourceInformation sourceInfo = new SourceInformation("/some/enumSource.pure", 4, 5, 6, 3, 2, 1);
        String prop1 = "prop1";

        Obj first = Obj.newObj(classifier, identifier, name, Lists.mutable.empty(), sourceInfo, true);
        Obj second = Obj.newObj(classifier, identifier, null, Lists.mutable.with(newPrimitivePropertyValue(prop1, "c", "d")), null, true);
        Assert.assertEquals(
                Obj.newObj(classifier, identifier, name, Lists.mutable.with(newPrimitivePropertyValue(prop1, "c", "d")), sourceInfo, true),
                Obj.merge(first, second));
        Assert.assertEquals(
                Obj.newObj(classifier, identifier, name, Lists.mutable.with(newPrimitivePropertyValue(prop1, "c", "d")), sourceInfo, true),
                Obj.merge(second, first));
    }

    @Test
    public void testComplexMerge()
    {
        String classifier = "meta::yet::another::Classifier";
        String identifier = "one_more_id";
        String name = "name";
        SourceInformation sourceInfo = new SourceInformation("/some/source2.pure", 1, 3, 5, 2, 4, 6);
        String prop1 = "prop1";
        String prop2 = "prop2";
        String prop3 = "prop3";

        Obj first = Obj.newObj(classifier, identifier, null, Lists.mutable.with(newPrimitivePropertyValue(prop1, "a", "b")), sourceInfo, false);
        Obj second = Obj.newObj(classifier, identifier, name, Lists.mutable.with(newPrimitivePropertyValue(prop1, "c"), newPrimitivePropertyValue(prop2, 7)), null, false);
        Obj third = Obj.newObj(classifier, identifier, null, Lists.mutable.with(newPrimitivePropertyValue(prop1, "d", "e", "f", "g"), newPrimitivePropertyValue(prop3, true, false)), null, false);
        Obj fourth = Obj.newObj(classifier, identifier, name, Lists.mutable.with(newPrimitivePropertyValue(prop2, 8), newPrimitivePropertyValue(prop1, "a", "c"), newPrimitivePropertyValue(prop3, false)), sourceInfo, false);

        Assert.assertEquals(
                Obj.newObj(classifier, identifier, name, Lists.mutable.with(newPrimitivePropertyValue(prop1, "a", "b", "c", "d", "e", "f", "g"), newPrimitivePropertyValue(prop2, 7, 8), newPrimitivePropertyValue(prop3, true, false)), sourceInfo, false),
                Obj.merge(first, second, third, fourth));

        Assert.assertEquals(
                Obj.newObj(classifier, identifier, name, Lists.mutable.with(newPrimitivePropertyValue(prop2, 8, 7), newPrimitivePropertyValue(prop1, "a", "c", "d", "e", "f", "g", "b"), newPrimitivePropertyValue(prop3, false, true)), sourceInfo, false),
                Obj.merge(fourth, third, second, first));

        Assert.assertEquals(
                Obj.newObj(classifier, identifier, name, Lists.mutable.with(newPrimitivePropertyValue(prop2, 8, 7), newPrimitivePropertyValue(prop1, "a", "c", "b", "d", "e", "f", "g"), newPrimitivePropertyValue(prop3, false, true)), sourceInfo, false),
                Obj.merge(fourth, first, third, second));

        Assert.assertEquals(
                Obj.newObj(classifier, identifier, name, Lists.mutable.with(newPrimitivePropertyValue(prop1, "d", "e", "f", "g", "c", "a", "b"), newPrimitivePropertyValue(prop3, true, false), newPrimitivePropertyValue(prop2, 7, 8)), sourceInfo, false),
                Obj.merge(third, second, fourth, first));
    }

    @Test
    public void testInvalidMerge()
    {
        String classifier1 = "meta::yet::another::Classifier1";
        String classifier2 = "meta::yet::another::Classifier2";
        String identifier1 = "id1";
        String identifier2 = "id2";
        String name1 = "name1";
        String name2 = "name2";
        SourceInformation sourceInfo1 = new SourceInformation("/some/source1.pure", 1, 6, 5, 2, 3, 4);
        SourceInformation sourceInfo2 = new SourceInformation("/some/source2.pure", 4, 3, 2, 5, 6, 1);

        // Classifier mismatch
        IllegalArgumentException classifierMismatch = Assert.assertThrows(IllegalArgumentException.class, () -> Obj.merge(
                Obj.newObj(classifier1, identifier1, name1, Lists.immutable.empty(), sourceInfo1, false),
                Obj.newObj(classifier2, identifier1, name1, Lists.immutable.empty(), sourceInfo1, false)));
        Assert.assertEquals("Cannot merge, classifier mismatch: meta::yet::another::Classifier1 vs meta::yet::another::Classifier2", classifierMismatch.getMessage());

        // Identifier mismatch
        IllegalArgumentException idMismatch = Assert.assertThrows(IllegalArgumentException.class, () -> Obj.merge(
                Obj.newObj(classifier1, identifier1, name1, Lists.immutable.empty(), sourceInfo1, false),
                Obj.newObj(classifier1, identifier2, name1, Lists.immutable.empty(), sourceInfo1, false)));
        Assert.assertEquals("Cannot merge, identifier mismatch: id1 vs id2", idMismatch.getMessage());

        // Name mismatch
        IllegalArgumentException nameMismatch = Assert.assertThrows(IllegalArgumentException.class, () -> Obj.merge(
                Obj.newObj(classifier1, identifier1, name1, Lists.immutable.empty(), sourceInfo1, false),
                Obj.newObj(classifier1, identifier1, name2, Lists.immutable.empty(), sourceInfo1, false)));
        Assert.assertEquals("Cannot merge, name mismatch: name1 vs name2", nameMismatch.getMessage());

        // SourceInformation mismatch
        IllegalArgumentException sourceInfoMismatch = Assert.assertThrows(IllegalArgumentException.class, () -> Obj.merge(
                Obj.newObj(classifier1, identifier1, name1, Lists.immutable.empty(), sourceInfo1, false),
                Obj.newObj(classifier1, identifier1, name1, Lists.immutable.empty(), sourceInfo2, false)));
        Assert.assertEquals("Cannot merge, source information mismatch: /some/source1.pure:1c6-3c4 vs /some/source2.pure:4c3-6c1", sourceInfoMismatch.getMessage());

        // isEnum mismatch
        IllegalArgumentException isEnumMismatch = Assert.assertThrows(IllegalArgumentException.class, () -> Obj.merge(
                Obj.newObj(classifier1, identifier1, name1, Lists.immutable.empty(), sourceInfo1, true),
                Obj.newObj(classifier1, identifier1, name1, Lists.immutable.empty(), sourceInfo1, false)));
        Assert.assertEquals("Cannot merge, isEnum mismatch: true vs false", isEnumMismatch.getMessage());

        // No Objs
        IllegalArgumentException empty = Assert.assertThrows(IllegalArgumentException.class, Obj::merge);
        Assert.assertEquals("No Objs to merge", empty.getMessage());
    }

    private PropertyValue newPrimitivePropertyValue(String property, Object... values)
    {
        return newPropertyValue(property, Arrays.stream(values).map(Primitive::new).toArray(RValue[]::new));
    }

    private PropertyValue newPropertyValue(String property, RValue... values)
    {
        return (values.length == 1) ? new PropertyValueOne(property, values[0]) : new PropertyValueMany(property, ArrayAdapter.adapt(values).asUnmodifiable());
    }
}
