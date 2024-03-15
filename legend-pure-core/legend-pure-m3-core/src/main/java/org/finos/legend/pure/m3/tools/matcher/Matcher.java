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

package org.finos.legend.pure.m3.tools.matcher;

import org.eclipse.collections.api.list.ListIterable;
import org.eclipse.collections.api.multimap.list.MutableListMultimap;
import org.eclipse.collections.impl.multimap.list.FastListMultimap;
import org.finos.legend.pure.m3.navigation.M3Paths;
import org.finos.legend.pure.m3.compiler.Context;
import org.finos.legend.pure.m3.navigation.type.Type;
import org.finos.legend.pure.m3.navigation.ProcessorSupport;
import org.finos.legend.pure.m4.coreinstance.CoreInstance;
import org.finos.legend.pure.m4.ModelRepository;
import org.finos.legend.pure.m4.exception.PureCompilationException;
import org.finos.legend.pure.m4.exception.PureException;

public class Matcher
{
    private final ModelRepository modelRepository;
    private final Context context;
    private final MutableListMultimap<CoreInstance, MatchRunner> runnersByType = FastListMultimap.newMultimap();
    private final ProcessorSupport processorSupport;
    private final CoreInstance anyType;

    public Matcher(ModelRepository modelRepository, Context context, ProcessorSupport processorSupport)
    {
        this.modelRepository = modelRepository;
        this.context = context;
        this.processorSupport = processorSupport;
        this.anyType = processorSupport.package_getByUserPath(M3Paths.Any);
    }

    public void addMatch(MatchRunner matchRunner)
    {
        String typeName = matchRunner.getClassName();
        CoreInstance type = processorSupport.package_getByUserPath(typeName);
        if (type == null)
        {
            throw new PureCompilationException(null, "The type '" + typeName + "' is unknown!");
        }
        this.runnersByType.put(type, matchRunner);
    }

    public void addMatchIfTypeIsKnown(MatchRunner matchRunner)
    {
        String typeName = matchRunner.getClassName();
        CoreInstance type = processorSupport.package_getByUserPath(typeName);
        if (type != null)
        {
            this.runnersByType.put(type, matchRunner);
        }
    }

    public boolean match(CoreInstance instance, MatcherState state) throws PureCompilationException
    {
        if (instance == null)
        {
            throw new IllegalArgumentException("Cannot match null instance");
        }
        if (state.noteVisited(instance))
        {
            try
            {
                boolean nonAnyRunnersExecuted = false;
                ListIterable<CoreInstance> types = Type.getGeneralizationResolutionOrder(instance.getClassifier(), this.processorSupport);
                if (types.getLast() != this.anyType)
                {
                    // This should not happen, but just in case ...
                    types = types.toList().with(this.anyType);
                }
                for (CoreInstance type : (state.mostGeneralRunnersFirst() ? types.asReversed() : types))
                {
                    ListIterable<MatchRunner> typeRunners = this.runnersByType.get(type);
                    if (typeRunners.notEmpty())
                    {
                        for (MatchRunner runner : typeRunners)
                        {
                            runner.run(instance, state, this, this.modelRepository, this.context);
                        }
                        nonAnyRunnersExecuted = nonAnyRunnersExecuted || (type != this.anyType);
                    }
                }
                return nonAnyRunnersExecuted;
            }
            catch (PureCompilationException e)
            {
                if (e.getSourceInformation() == null)
                {
                    throw new PureCompilationException(instance.getSourceInformation(), e.getInfo(), e);
                }
                throw e;
            }
            catch (RuntimeException e)
            {
                PureException pureException = PureException.findPureException(e);
                if (pureException == null)
                {
                    String message = e.getMessage();
                    if (message == null)
                    {
                        message = "Error processing " + instance;
                    }
                    throw new PureCompilationException(instance.getSourceInformation(), message, e);
                }
                if (pureException instanceof PureCompilationException)
                {
                    PureCompilationException compilationException = (PureCompilationException)pureException;
                    if (compilationException.getSourceInformation() == null)
                    {
                        throw new PureCompilationException(instance.getSourceInformation(), compilationException.getInfo(), e);
                    }
                    throw compilationException;
                }
                throw new PureCompilationException(instance.getSourceInformation(), pureException.getInfo(), e);
            }
        }
        return true;
    }

    /**
     * Flag something for full matching. This will fire all matchers including the walkers. Use this when you
     * need to recursively walk an object for unbinding.
     * If you only need to unbind the instance then you can just call matcherState.addInstance()
     * @param instance
     * @param state
     * @throws PureCompilationException
     */
    public void fullMatch(CoreInstance instance, MatcherState state) throws PureCompilationException
    {
        if (!this.match(instance, state))
        {
            throw new RuntimeException("No match found for the type " + instance);
        }
    }
}
