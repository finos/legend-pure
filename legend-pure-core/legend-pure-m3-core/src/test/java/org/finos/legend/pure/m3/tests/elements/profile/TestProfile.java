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

package org.finos.legend.pure.m3.tests.elements.profile;

import org.eclipse.collections.api.RichIterable;
import org.eclipse.collections.api.factory.Lists;
import org.eclipse.collections.api.list.ListIterable;
import org.finos.legend.pure.m3.coreinstance.meta.pure.metamodel.extension.Profile;
import org.finos.legend.pure.m3.coreinstance.meta.pure.metamodel.extension.Stereotype;
import org.finos.legend.pure.m3.coreinstance.meta.pure.metamodel.extension.Tag;
import org.finos.legend.pure.m3.navigation.M3Paths;
import org.finos.legend.pure.m3.navigation.generictype.GenericType;
import org.finos.legend.pure.m3.serialization.filesystem.repository.CodeRepository;
import org.finos.legend.pure.m3.serialization.filesystem.repository.CodeRepositoryProviderHelper;
import org.finos.legend.pure.m3.serialization.filesystem.repository.GenericCodeRepository;
import org.finos.legend.pure.m3.serialization.filesystem.usercodestorage.classpath.ClassLoaderCodeStorage;
import org.finos.legend.pure.m3.serialization.filesystem.usercodestorage.composite.CompositeCodeStorage;
import org.finos.legend.pure.m3.tests.AbstractPureTestWithCoreCompiledPlatform;
import org.finos.legend.pure.m3.tools.ListHelper;
import org.junit.After;
import org.junit.Assert;
import org.junit.BeforeClass;
import org.junit.Test;

public class TestProfile extends AbstractPureTestWithCoreCompiledPlatform
{
    @BeforeClass
    public static void setUp()
    {
        setUpRuntime(getFunctionExecution(), new CompositeCodeStorage(new ClassLoaderCodeStorage(getCodeRepositories())), getFactoryRegistryOverride(), getOptions(), getExtra());
    }

    protected static RichIterable<? extends CodeRepository> getCodeRepositories()
    {
        return Lists.immutable.with(CodeRepositoryProviderHelper.findPlatformCodeRepository(),
                GenericCodeRepository.build("system", "((meta)|(system)|(apps::pure))(::.*)?", "platform"),
                GenericCodeRepository.build("test", "test(::.*)?", "platform", "system"));
    }

    @After
    public void cleanRuntime()
    {
        runtime.delete("/test/testSource.pure");
        runtime.delete("fromString.pure");
        runtime.compile();
    }

    @Test
    public void testProfile()
    {
        compileTestSource("fromString.pure", "Profile test::myProfile\n" +
                "{\n" +
                "  stereotypes: [st1, st2];\n" +
                "  tags: [t1, t2, t3];\n" +
                "}");

        Profile profile = (Profile) runtime.getCoreInstance("test::myProfile");
        Assert.assertNotNull(profile);

        Assert.assertEquals(M3Paths.Profile, GenericType.print(profile._classifierGenericType(), true, processorSupport));

        ListIterable<? extends Stereotype> stereotypes = ListHelper.wrapListIterable(profile._p_stereotypes());
        Assert.assertEquals(Lists.mutable.with("st1", "st2"), stereotypes.collect(Stereotype::_value));
        stereotypes.forEach(st -> Assert.assertSame(st._value(), profile, st._profile()));

        ListIterable<? extends Tag> tags = ListHelper.wrapListIterable(profile._p_tags());
        Assert.assertEquals(Lists.mutable.with("t1", "t2", "t3"), tags.collect(Tag::_value));
        tags.forEach(t -> Assert.assertSame(t._value(), profile, t._profile()));
    }

    @Test
    public void testStereotypes()
    {
        compileTestSource("/test/testSource.pure",
                "Profile test::myProfile\n" +
                        "{\n" +
                        "  stereotypes: [st1, st2, st3];\n" +
                        "}");

        Profile profile = (Profile) runtime.getCoreInstance("test::myProfile");
        Assert.assertNotNull(profile);

        ListIterable<? extends Stereotype> stereotypes = ListHelper.wrapListIterable(profile._p_stereotypes());
        Assert.assertEquals(Lists.mutable.with("st1", "st2", "st3"), stereotypes.collect(Stereotype::_value));

        Stereotype st1 = (Stereotype) org.finos.legend.pure.m3.navigation.profile.Profile.findStereotype(profile, "st1");
        Assert.assertNotNull(st1);
        Assert.assertEquals("st1", st1._value());
        Assert.assertSame(profile, st1._profile());
        assertSourceInformation("/test/testSource.pure", 3, 17, 3, 17, 3, 19, st1.getSourceInformation());

        Stereotype st2 = (Stereotype) org.finos.legend.pure.m3.navigation.profile.Profile.findStereotype(profile, "st2");
        Assert.assertNotNull(st1);
        Assert.assertEquals("st2", st2._value());
        Assert.assertSame(profile, st2._profile());
        assertSourceInformation("/test/testSource.pure", 3, 22, 3, 22, 3, 24, st2.getSourceInformation());

        Stereotype st3 = (Stereotype) org.finos.legend.pure.m3.navigation.profile.Profile.findStereotype(profile, "st3");
        Assert.assertNotNull(st1);
        Assert.assertEquals("st3", st3._value());
        Assert.assertSame(profile, st3._profile());
        assertSourceInformation("/test/testSource.pure", 3, 27, 3, 27, 3, 29, st3.getSourceInformation());
    }

    @Test
    public void testTags()
    {
        compileTestSource("/test/testSource.pure",
                "Profile test::myProfile\n" +
                        "{\n" +
                        "  stereotypes: [st1, st2];\n" +
                        "  tags: [t1, t2, t3];\n" +
                        "}");

        Profile profile = (Profile) runtime.getCoreInstance("test::myProfile");
        Assert.assertNotNull(profile);

        ListIterable<? extends Tag> tags = ListHelper.wrapListIterable(profile._p_tags());
        Assert.assertEquals(Lists.mutable.with("t1", "t2", "t3"), tags.collect(Tag::_value));

        Tag t1 = (Tag) org.finos.legend.pure.m3.navigation.profile.Profile.findTag(profile, "t1");
        Assert.assertNotNull(t1);
        Assert.assertEquals("t1", t1._value());
        Assert.assertSame(profile, t1._profile());
        assertSourceInformation("/test/testSource.pure", 4, 10, 4, 10, 4, 11, t1.getSourceInformation());

        Tag t2 = (Tag) org.finos.legend.pure.m3.navigation.profile.Profile.findTag(profile, "t2");
        Assert.assertNotNull(t2);
        Assert.assertEquals("t2", t2._value());
        Assert.assertSame(profile, t2._profile());
        assertSourceInformation("/test/testSource.pure", 4, 14, 4, 14, 4, 15, t2.getSourceInformation());

        Tag t3 = (Tag) org.finos.legend.pure.m3.navigation.profile.Profile.findTag(profile, "t3");
        Assert.assertNotNull(t3);
        Assert.assertEquals("t3", t3._value());
        Assert.assertSame(profile, t3._profile());
        assertSourceInformation("/test/testSource.pure", 4, 18, 4, 18, 4, 19, t3.getSourceInformation());
    }
}
