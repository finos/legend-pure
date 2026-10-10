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

package org.finos.legend.pure.rust;

import org.eclipse.collections.api.factory.Lists;
import org.eclipse.collections.api.factory.Maps;
import org.eclipse.collections.api.list.ListIterable;
import org.eclipse.collections.api.list.MutableList;
import org.eclipse.collections.api.map.MutableMap;
import org.finos.legend.pure.m3.navigation.Instance;
import org.finos.legend.pure.m3.navigation.M3Paths;
import org.finos.legend.pure.m3.navigation.PackageableElement.PackageableElement;
import org.finos.legend.pure.m3.navigation.PrimitiveUtilities;
import org.finos.legend.pure.m3.navigation.ProcessorSupport;
import org.finos.legend.pure.m3.navigation._package._Package;
import org.finos.legend.pure.m3.navigation.enumeration.Enumeration;
import org.finos.legend.pure.m4.ModelRepository;
import org.finos.legend.pure.m4.coreinstance.CoreInstance;
import org.finos.legend.pure.m4.coreinstance.primitive.BooleanCoreInstance;
import org.finos.legend.pure.m4.coreinstance.primitive.ByteCoreInstance;
import org.finos.legend.pure.m4.coreinstance.primitive.DateCoreInstance;
import org.finos.legend.pure.m4.coreinstance.primitive.DecimalCoreInstance;
import org.finos.legend.pure.m4.coreinstance.primitive.FloatCoreInstance;
import org.finos.legend.pure.m4.coreinstance.primitive.IntegerCoreInstance;
import org.finos.legend.pure.m4.coreinstance.primitive.StrictTimeCoreInstance;
import org.finos.legend.pure.m4.coreinstance.primitive.StringCoreInstance;
import org.finos.legend.pure.m4.coreinstance.primitive.date.DateFunctions;
import org.finos.legend.pure.m4.coreinstance.primitive.date.LatestDate;
import org.finos.legend.pure.m4.coreinstance.primitive.date.PureDate;
import org.finos.legend.pure.m4.coreinstance.primitive.strictTime.PureStrictTime;
import org.finos.legend.pure.m4.coreinstance.primitive.strictTime.StrictTimeFunctions;

import java.util.IdentityHashMap;

/**
 * The Java half of the node bridge. Rust holds one global reference to it
 * and calls its instance methods to describe graph nodes, read their
 * properties, and materialise Rust values as {@link CoreInstance}s. Every
 * method only reads the Java graph; none re-enters Rust.
 */
final class RustNodeBridge
{
    private final ModelRepository repository;
    private final ProcessorSupport processorSupport;
    private final PureRustEvaluator owner;
    private final IdentityHashMap<CoreInstance, Long> ids = new IdentityHashMap<>();
    private final MutableList<CoreInstance> nodes = Lists.mutable.empty();
    private final MutableMap<Long, RustCoreInstance> proxies = Maps.mutable.empty();

    RustNodeBridge(ModelRepository repository, ProcessorSupport processorSupport, PureRustEvaluator owner)
    {
        this.repository = repository;
        this.processorSupport = processorSupport;
        this.owner = owner;
    }

    void clear()
    {
        this.ids.clear();
        this.nodes.clear();
        this.proxies.clear();
    }

    MutableList<RustCoreInstance> proxies()
    {
        return Lists.mutable.withAll(this.proxies.values());
    }

    void forgetProxy(long handle)
    {
        this.proxies.remove(handle);
    }

    private long idOf(CoreInstance node)
    {
        Long id = this.ids.get(node);
        if (id == null)
        {
            id = (long) this.nodes.size();
            this.nodes.add(node);
            this.ids.put(node, id);
        }
        return id;
    }

    private static String userPath(CoreInstance element)
    {
        return PackageableElement.getUserPathForPackageableElement(element, "::");
    }

    // ---- called from Rust: Java -> Rust ----

    NodeDescription describe(CoreInstance ci)
    {
        NodeDescription d = new NodeDescription();
        if (ci instanceof RustCoreInstance)
        {
            d.kind = NodeDescription.RUST_OBJECT;
            d.id = ((RustCoreInstance) ci).handle();
            return d;
        }
        CoreInstance classifier = ci.getClassifier();
        d.classifier = (classifier == null) ? null : userPath(classifier);
        d.name = ci.getName();
        if (ci instanceof BooleanCoreInstance)
        {
            d.kind = NodeDescription.BOOLEAN;
            d.payload = Boolean.toString(PrimitiveUtilities.getBooleanValue(ci));
        }
        else if (ci instanceof IntegerCoreInstance)
        {
            d.kind = NodeDescription.INTEGER;
            d.payload = PrimitiveUtilities.getIntegerValue(ci).toString();
        }
        else if (ci instanceof FloatCoreInstance)
        {
            d.kind = NodeDescription.FLOAT;
            d.payload = PrimitiveUtilities.getFloatValue(ci).toPlainString();
        }
        else if (ci instanceof DecimalCoreInstance)
        {
            d.kind = NodeDescription.DECIMAL;
            d.payload = PrimitiveUtilities.getDecimalValue(ci).toPlainString();
        }
        else if (ci instanceof StringCoreInstance)
        {
            d.kind = NodeDescription.STRING;
            d.payload = PrimitiveUtilities.getStringValue(ci);
        }
        else if (ci instanceof DateCoreInstance)
        {
            describeDate(d, PrimitiveUtilities.getDateValue(ci));
        }
        else if (ci instanceof StrictTimeCoreInstance)
        {
            describeStrictTime(d, PrimitiveUtilities.getStrictTimeValue(ci));
        }
        else if (ci instanceof ByteCoreInstance)
        {
            d.kind = NodeDescription.BYTE;
        }
        else if ((classifier != null) && Instance.instanceOf(classifier, M3Paths.Enumeration, this.processorSupport))
        {
            d.kind = NodeDescription.ENUM_VALUE;
            d.id = idOf(ci);
        }
        else if (Instance.instanceOf(ci, M3Paths.PackageableElement, this.processorSupport))
        {
            d.kind = NodeDescription.PACKAGEABLE_ELEMENT;
            d.id = idOf(ci);
            d.userPath = userPath(ci);
            d.keys = ci.getKeys().toArray(new String[0]);
        }
        else
        {
            d.kind = NodeDescription.NODE;
            d.id = idOf(ci);
            d.keys = ci.getKeys().toArray(new String[0]);
        }
        return d;
    }

    private static void describeDate(NodeDescription d, PureDate date)
    {
        if (LatestDate.isLatestDate(date))
        {
            d.kind = NodeDescription.LATEST_DATE;
            return;
        }
        d.kind = NodeDescription.DATE;
        d.year = date.getYear();
        d.precision = 0;
        if (date.hasMonth())
        {
            d.month = date.getMonth();
            d.precision = 1;
        }
        if (date.hasDay())
        {
            d.day = date.getDay();
            d.precision = 2;
        }
        if (date.hasHour())
        {
            d.hour = date.getHour();
            d.precision = 3;
        }
        if (date.hasMinute())
        {
            d.minute = date.getMinute();
            d.precision = 4;
        }
        if (date.hasSecond())
        {
            d.second = date.getSecond();
            d.precision = 5;
        }
        if (date.hasSubsecond())
        {
            d.subsecond = date.getSubsecond();
            d.precision = 6;
        }
    }

    private static void describeStrictTime(NodeDescription d, PureStrictTime time)
    {
        d.kind = NodeDescription.STRICT_TIME;
        d.hour = time.getHour();
        d.minute = time.getMinute();
        d.precision = 4;
        if (time.hasSecond())
        {
            d.second = time.getSecond();
            d.precision = 5;
        }
        if (time.hasSubsecond())
        {
            d.subsecond = time.getSubsecond();
            d.precision = 6;
        }
    }

    NodeDescription[] propertyValues(long id, String key)
    {
        CoreInstance node = this.nodes.get((int) id);
        ListIterable<? extends CoreInstance> values = Instance.getValueForMetaPropertyToManyResolved(node, key, this.processorSupport);
        NodeDescription[] out = new NodeDescription[values.size()];
        for (int i = 0; i < out.length; i++)
        {
            out[i] = describe(values.get(i));
        }
        return out;
    }

    // ---- called from Rust: Rust -> Java ----

    CoreInstance newBoolean(boolean value)
    {
        return this.repository.newBooleanCoreInstance(value);
    }

    CoreInstance newInteger(long value)
    {
        return this.repository.newIntegerCoreInstance(value);
    }

    CoreInstance newFloat(String text)
    {
        return this.repository.newFloatCoreInstance(text);
    }

    CoreInstance newDecimal(String text)
    {
        return this.repository.newDecimalCoreInstance(text);
    }

    CoreInstance newString(String value)
    {
        return this.repository.newStringCoreInstance(value);
    }

    CoreInstance newDate(int year, int month, int day, int hour, int minute, int second, String subsecond, int precision)
    {
        PureDate date;
        switch (precision)
        {
            case 0:
                date = DateFunctions.newPureDate(year);
                break;
            case 1:
                date = DateFunctions.newPureDate(year, month);
                break;
            case 2:
                date = DateFunctions.newPureDate(year, month, day);
                break;
            case 3:
                date = DateFunctions.newPureDate(year, month, day, hour);
                break;
            case 4:
                date = DateFunctions.newPureDate(year, month, day, hour, minute);
                break;
            case 5:
                date = DateFunctions.newPureDate(year, month, day, hour, minute, second);
                break;
            default:
                date = DateFunctions.newPureDate(year, month, day, hour, minute, second, subsecond);
                break;
        }
        return this.repository.newDateCoreInstance(date);
    }

    CoreInstance newLatestDate()
    {
        return this.repository.newLatestDateCoreInstance();
    }

    CoreInstance newStrictTime(int hour, int minute, int second, String subsecond, int precision)
    {
        StringBuilder text = new StringBuilder(String.format("%02d:%02d", hour, minute));
        if (precision >= 5)
        {
            text.append(String.format(":%02d", second));
        }
        if ((precision >= 6) && (subsecond != null) && !subsecond.isEmpty())
        {
            text.append('.').append(subsecond);
        }
        return this.repository.newStrictTimeCoreInstance(StrictTimeFunctions.parsePureStrictTime(text.toString()));
    }

    CoreInstance element(String userPath)
    {
        return _Package.getByUserPath(userPath, this.processorSupport);
    }

    CoreInstance enumValue(String enumPath, String member)
    {
        CoreInstance enumeration = _Package.getByUserPath(enumPath, this.processorSupport);
        return (enumeration == null) ? null : Enumeration.findEnum(enumeration, member);
    }

    CoreInstance node(long id)
    {
        return this.nodes.get((int) id);
    }

    CoreInstance rustObject(long handle, String classifierPath)
    {
        RustCoreInstance proxy = this.proxies.get(handle);
        if (proxy == null)
        {
            proxy = new RustCoreInstance(this.owner, handle, classifierPath);
            this.proxies.put(handle, proxy);
        }
        return proxy;
    }
}
