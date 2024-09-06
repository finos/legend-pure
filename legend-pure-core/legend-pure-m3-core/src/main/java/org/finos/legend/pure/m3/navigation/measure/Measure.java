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

package org.finos.legend.pure.m3.navigation.measure;

import org.finos.legend.pure.m3.coreinstance.meta.pure.metamodel._import.ImportStub;
import org.finos.legend.pure.m3.coreinstance.meta.pure.metamodel.type.Any;
import org.finos.legend.pure.m3.coreinstance.meta.pure.metamodel.type.Unit;
import org.finos.legend.pure.m3.coreinstance.meta.pure.metamodel.type.generics.GenericType;
import org.finos.legend.pure.m3.coreinstance.meta.pure.metamodel.valuespecification.InstanceValue;
import org.finos.legend.pure.m3.navigation.Instance;
import org.finos.legend.pure.m3.navigation.M3Paths;
import org.finos.legend.pure.m3.navigation.M3Properties;
import org.finos.legend.pure.m3.navigation.PackageableElement.PackageableElement;
import org.finos.legend.pure.m3.navigation.PrimitiveUtilities;
import org.finos.legend.pure.m3.navigation.ProcessorSupport;
import org.finos.legend.pure.m4.coreinstance.AbstractCoreInstanceWrapper;
import org.finos.legend.pure.m4.coreinstance.CoreInstance;
import org.finos.legend.pure.m4.coreinstance.indexing.IndexSpecifications;
import org.finos.legend.pure.m4.tools.SafeAppendable;

/**
 * The structure of a unit instance is an InstanceValue wrapping an InstanceValue in its values field,
 * where the inner InstanceValue's values field points to the numeric value instance representing the value of the instance.
 * Thus, in InstanceValueExecutor, we do not flatten the deep structure all the way down to the leaf values property.
 *
 * e.g. let x = 5 Kilogram  --> ^InstanceValue{ genericType = ^GenericType(rawType = ^ImportStub( ^GenericType(rawType =Kilogram)), values = ^InstanceValue(genericType = ^ImportStub( ^GenericType(rawType =Kilogram)), values=^IntegerCoreInstance(val=5)}
 *
 * The expectation during processing is that this structure is immutable, however the ImportStub will be cleaned during bind/unbind.
 *
 * For InstanceValueProcessor, we do not copy over the genericType and multiplicity from a unit instance's values property,
 * since otherwise the matcher will match both the outer and inner InstanceValue of the instance, and in the inner instance, copy over the value type (e.g. Integer).
 *
 * With above, for ValueSpecificationUnbind, we do not remove the genericType and multiplicity from a unit instance,
 * as we do not recover them in InstanceValueProcessor.
 *
 * For ValueSpecificationBootstrap, we do not wrap a unit instance with another layer of InstanceValue because of the
 * change for the flattening logic above. For some native functions e.g. GenericType, for unit instances we do not
 * extract the classifier as we do for PrimitiveType instances but extract the generic type's raw type.
 * Thus these methods.
 */
public class Measure
{
    public static boolean isUnitOrMeasureInstance(CoreInstance instance, ProcessorSupport processorSupport)
    {
        if (!isNonEmptyInstanceValue(instance, processorSupport))
        {
            return false;
        }

        CoreInstance rawType = Instance.getValueForMetaPropertyToOneResolved(instance, M3Properties.genericType, M3Properties.rawType, processorSupport);
        return isUnit(rawType, processorSupport) || isMeasure(rawType, processorSupport);
    }

    public static boolean isUnitInstance(CoreInstance instance, ProcessorSupport processorSupport)
    {
        return isNonEmptyInstanceValue(instance, processorSupport) &&
                isUnit(Instance.getValueForMetaPropertyToOneResolved(instance, M3Properties.genericType, M3Properties.rawType, processorSupport), processorSupport);
    }

    // safe for use in unbinders
    public static boolean isUnitInstanceValueNoResolution(InstanceValue instanceValue)
    {
        if (instanceValue._valuesCoreInstance().notEmpty())
        {
            GenericType genericType = instanceValue._genericType();
            if (genericType != null)
            {
                CoreInstance rawType = genericType._rawTypeCoreInstance();
                if (rawType instanceof Unit)
                {
                    return true;
                }
                if (rawType instanceof ImportStub)
                {
                    ImportStub importStub = (ImportStub) rawType;
                    CoreInstance resolvedRawType = importStub._resolvedNodeCoreInstance();
                    if (resolvedRawType == null)
                    {
                        String idOrPath = importStub._idOrPath();
                        return (idOrPath != null) && (idOrPath.indexOf('~') != -1);
                    }
                    return (resolvedRawType instanceof Unit);
                }
            }
        }
        return false;
    }

    public static boolean isUnit(CoreInstance instance, ProcessorSupport processorSupport)
    {
        if (instance == null)
        {
            return false;
        }
        if (instance instanceof Unit)
        {
            return true;
        }
        return (!(instance instanceof Any) || (instance instanceof AbstractCoreInstanceWrapper)) && processorSupport.instance_instanceOf(instance, M3Paths.Unit);
    }

    public static boolean isMeasure(CoreInstance instance, ProcessorSupport processorSupport)
    {
        if (instance == null)
        {
            return false;
        }
        if (instance instanceof org.finos.legend.pure.m3.coreinstance.meta.pure.metamodel.type.Measure)
        {
            return true;
        }
        return (!(instance instanceof Any) || (instance instanceof AbstractCoreInstanceWrapper)) && processorSupport.instance_instanceOf(instance, M3Paths.Measure);
    }

    @Deprecated
    public static boolean isUnitGenericType(CoreInstance genericType, ProcessorSupport processorSupport)
    {
        return (genericType != null) && isUnit(Instance.getValueForMetaPropertyToOneResolved(genericType, M3Properties.rawType, processorSupport), processorSupport);
    }

    private static boolean isNonEmptyInstanceValue(CoreInstance instance, ProcessorSupport processorSupport)
    {
        return isInstanceValue(instance, processorSupport) && instance.getValueForMetaPropertyToMany(M3Properties.values).notEmpty();
    }

    private static boolean isInstanceValue(CoreInstance instance, ProcessorSupport processorSupport)
    {
        if (instance == null)
        {
            return false;
        }
        if (instance instanceof InstanceValue)
        {
            return true;
        }
        return (!(instance instanceof Any) || (instance instanceof AbstractCoreInstanceWrapper)) && processorSupport.instance_instanceOf(instance, M3Paths.InstanceValue);
    }

    public static CoreInstance findUnit(CoreInstance measure, String name)
    {
        CoreInstance canonicalUnit = measure.getValueForMetaPropertyToOne(M3Properties.canonicalUnit);
        return ((canonicalUnit != null) && name.equals(PrimitiveUtilities.getStringValue(canonicalUnit.getValueForMetaPropertyToOne(M3Properties.name)))) ?
                canonicalUnit :
                measure.getValueInValueForMetaPropertyToManyByIDIndex(M3Properties.nonCanonicalUnits, IndexSpecifications.getPropertyValueNameIndexSpec(M3Properties.name), name);
    }


    public static CoreInstance getUnitByUserPath(String path, ProcessorSupport processorSupport)
    {
        int tilde = path.indexOf('~');
        if (tilde == -1)
        {
            throw new IllegalArgumentException("Invalid unit path '" + path + "'");
        }
        String measurePath = path.substring(0, tilde);
        CoreInstance measure = processorSupport.package_getByUserPath(measurePath);
        if (measure == null)
        {
            throw new RuntimeException("Error finding unit '" + path + "': cannot find measure '" + measurePath + "'");
        }
        String unitName = path.substring(tilde + 1);
        CoreInstance unit = findUnit(measure, unitName);
        if (unit == null)
        {
            throw new RuntimeException("Error finding unit '" + path + "': cannot find unit '" + unitName + "' in measure '" + measurePath + "'");
        }
        return unit;
    }

    public static String getSystemPathForUnit(CoreInstance unit)
    {
        return getSystemPathForUnit(unit, null);
    }

    public static String getSystemPathForUnit(CoreInstance unit, String separator)
    {
        return writeSystemPathForUnit(new StringBuilder(64), unit, separator).toString();
    }

    public static String getUserPathForUnit(CoreInstance unit)
    {
        return getUserPathForUnit(unit, null);
    }

    public static String getUserPathForUnit(CoreInstance unit, String separator)
    {
        return writeUserPathForUnit(new StringBuilder(64), unit, separator).toString();
    }

    public static <T extends Appendable> T writeSystemPathForUnit(T appendable, CoreInstance unit)
    {
        return writeSystemPathForUnit(appendable, unit, null);
    }

    public static <T extends Appendable> T writeSystemPathForUnit(T appendable, CoreInstance unit, String separator)
    {
        CoreInstance measure = unit.getValueForMetaPropertyToOne(M3Properties.measure);
        CoreInstance pkg = measure.getValueForMetaPropertyToOne(M3Properties._package);
        SafeAppendable safeAppendable = SafeAppendable.wrap(appendable);
        if (pkg != null)
        {
            PackageableElement.writeSystemPathForPackageableElement(safeAppendable, pkg)
                    .append((separator == null) ? PackageableElement.DEFAULT_PATH_SEPARATOR : separator);
        }
        safeAppendable.append(unit.getName());
        return appendable;
    }

    public static <T extends Appendable> T writeUserPathForUnit(T appendable, CoreInstance unit)
    {
        return writeUserPathForUnit(appendable, unit, null);
    }

    public static <T extends Appendable> T writeUserPathForUnit(T appendable, CoreInstance unit, String separator)
    {
        CoreInstance measure = unit.getValueForMetaPropertyToOne(M3Properties.measure);
        CoreInstance pkg = measure.getValueForMetaPropertyToOne(M3Properties._package);
        SafeAppendable safeAppendable = SafeAppendable.wrap(appendable);
        if (pkg != null)
        {
            PackageableElement.writeUserPathForPackageableElement(safeAppendable, pkg, separator)
                    .append((separator == null) ? PackageableElement.DEFAULT_PATH_SEPARATOR : separator);
        }
        safeAppendable.append(unit.getName());
        return appendable;
    }

    public static <T extends Appendable> T printUnit(T appendable, CoreInstance unit, boolean fullPaths)
    {
        if (fullPaths)
        {
            return writeUserPathForUnit(appendable, unit);
        }

        SafeAppendable.wrap(appendable).append(unit.getName());
        return appendable;
    }
}
