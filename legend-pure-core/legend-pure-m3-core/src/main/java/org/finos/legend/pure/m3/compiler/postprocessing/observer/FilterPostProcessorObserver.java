// Copyright 2022 Goldman Sachs
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

package org.finos.legend.pure.m3.compiler.postprocessing.observer;

import org.finos.legend.pure.m3.coreinstance.meta.pure.metamodel.PackageableElement;
import org.finos.legend.pure.m4.coreinstance.CoreInstance;

import java.util.function.Predicate;

public class FilterPostProcessorObserver<T extends PostProcessorObserver> implements PostProcessorObserver
{
    private final T observer;
    private final Predicate<? super CoreInstance> filter;

    private FilterPostProcessorObserver(T observer, Predicate<? super CoreInstance> filter)
    {
        this.observer = observer;
        this.filter = filter;
    }

    @Override
    public void startProcessing(CoreInstance instance)
    {
        if (this.filter.test(instance))
        {
            this.observer.startProcessing(instance);
        }
    }

    @Override
    public void finishProcessing(CoreInstance instance)
    {
        if (this.filter.test(instance))
        {
            this.observer.finishProcessing(instance);
        }
    }

    @Override
    public void finishProcessing(CoreInstance instance, Exception e)
    {
        if (this.filter.test(instance))
        {
            this.observer.finishProcessing(instance, e);
        }
    }

    public T getObserver()
    {
        return this.observer;
    }

    public Predicate<? super CoreInstance> getFilter()
    {
        return this.filter;
    }

    public static <T extends PostProcessorObserver> FilterPostProcessorObserver<T> filter(T observer, Predicate<? super CoreInstance> filter)
    {
        return new FilterPostProcessorObserver<>(observer, filter);
    }

    public static <T extends PostProcessorObserver> FilterPostProcessorObserver<T> onlyPackagedElements(T observer)
    {
        return filter(observer, e -> (e instanceof PackageableElement) && (((PackageableElement) e)._package() != null));
    }
}
