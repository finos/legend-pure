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

package org.finos.legend.pure.lsp;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import org.eclipse.lsp4j.Location;
import org.eclipse.lsp4j.Position;
import org.eclipse.lsp4j.Range;
import org.eclipse.collections.api.list.ListIterable;
import org.finos.legend.pure.m3.navigation.M3Properties;
import org.finos.legend.pure.m3.navigation.importstub.ImportStub;
import org.finos.legend.pure.m3.serialization.runtime.PureRuntime;
import org.finos.legend.pure.m3.serialization.runtime.Source;
import org.finos.legend.pure.m4.coreinstance.CoreInstance;
import org.finos.legend.pure.m4.coreinstance.SourceInformation;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class ReferencesProvider
{
    private static final Logger LOGGER = LoggerFactory.getLogger(ReferencesProvider.class);
    // Silent cap: results beyond this are dropped rather than reported as truncated to the client.
    private static final int MAX_REFERENCES = 1000;

    public static List<Location> references(PureRuntime runtime, UriMapper uriMapper,
                                            String sourceId, int line, int column,
                                            boolean includeDeclaration)
    {
        Source source = runtime.getSourceById(sourceId);
        if (source == null)
        {
            return Collections.emptyList();
        }

        CoreInstance raw = source.navigate(line, column, runtime.getProcessorSupport());
        if (raw == null)
        {
            return Collections.emptyList();
        }

        CoreInstance element = ImportStub.withImportStubByPass(raw, runtime.getProcessorSupport());
        if (element == null)
        {
            return Collections.emptyList();
        }

        String classifierName = element.getClassifier() != null
                ? element.getClassifier().getName() : "";

        // Deduplicate by source location string (sourceId:startLine:startCol)
        Set<String> seen = new HashSet<>();
        List<Location> locations = new ArrayList<>();

        if (includeDeclaration)
        {
            SourceInformation defSi = element.getSourceInformation();
            if (defSi != null)
            {
                addLocation(locations, seen, defSi, uriMapper);
            }
        }

        // Check classifier name (not Java instanceof) — Pure graph objects are plain CoreInstance.
        // Property/QualifiedProperty are invocable Functions too (via .prop or ->prop()), so call
        // sites are tracked through 'applications' the same way as ConcreteFunctionDefinition/NativeFunction;
        // Association end properties are plain Property/QualifiedProperty instances, so no separate case is needed.
        if ("ConcreteFunctionDefinition".equals(classifierName)
                || "NativeFunction".equals(classifierName)
                || "Property".equals(classifierName)
                || "QualifiedProperty".equals(classifierName))
        {
            ListIterable<? extends CoreInstance> applications =
                    element.getValueForMetaPropertyToMany(M3Properties.applications);
            if (applications != null)
            {
                for (CoreInstance app : applications)
                {
                    if (locations.size() >= MAX_REFERENCES)
                    {
                        break;
                    }
                    SourceInformation appSi = app.getSourceInformation();
                    if (appSi != null)
                    {
                        addLocation(locations, seen, appSi, uriMapper);
                    }
                }
            }
        }

        // Enum literal (e.g. Country.US): the literal carries no back-references of its own —
        // Pure compiles the access to a call taking the Enumeration and the literal's name as a
        // plain string (see Source.navigate()'s "extractEnumValue" handling for the resolve-side
        // counterpart), so a reference usage only tells us the Enumeration was accessed. Walk the
        // Enumeration's own referenceUsages and filter down to accesses naming this specific literal.
        CoreInstance elementClassifier = element.getClassifier();
        if (elementClassifier != null && elementClassifier.getClassifier() != null
                && "Enumeration".equals(elementClassifier.getClassifier().getName()))
        {
            addEnumLiteralUsages(locations, seen, elementClassifier, element.getName(), uriMapper);
        }

        // For all elements: read 'referenceUsages' (structural type references)
        ListIterable<? extends CoreInstance> refUsages =
                element.getValueForMetaPropertyToMany(M3Properties.referenceUsages);
        if (refUsages != null)
        {
            for (CoreInstance refUsage : refUsages)
            {
                if (locations.size() >= MAX_REFERENCES)
                {
                    LspLog.debug("references: truncated at " + MAX_REFERENCES + " results");
                    break;
                }

                // PureIdeLight: if sourceInformation is empty on the refUsage itself,
                // fall back to the owner's sourceInformation
                SourceInformation refSi = refUsage.getSourceInformation();
                if (refSi != null)
                {
                    addLocation(locations, seen, refSi, uriMapper);
                }
                else
                {
                    CoreInstance owner = refUsage.getValueForMetaPropertyToOne(M3Properties.owner);
                    if (owner != null)
                    {
                        SourceInformation ownerSi = owner.getSourceInformation();
                        if (ownerSi != null)
                        {
                            addLocation(locations, seen, ownerSi, uriMapper);
                        }
                    }
                }
            }
        }

        return locations;
    }

    // Mirrors the approach of meta::pure::ide::findusages::findUsagesForEnum (legend-engine, Pure-level):
    // walk the Enumeration's referenceUsages, then for each usage owned by an InstanceValue used as a
    // function parameter, check whether the enclosing call is the compiled enum accessor and whether its
    // literal-name argument matches the target value — that's the only way to tell which literal was used.
    private static void addEnumLiteralUsages(List<Location> locations, Set<String> seen,
                                             CoreInstance enumeration, String enumValueName, UriMapper uriMapper)
    {
        if (enumValueName == null)
        {
            return;
        }
        try
        {
            ListIterable<? extends CoreInstance> refUsages =
                    enumeration.getValueForMetaPropertyToMany(M3Properties.referenceUsages);
            if (refUsages == null)
            {
                return;
            }
            for (CoreInstance refUsage : refUsages)
            {
                if (locations.size() >= MAX_REFERENCES)
                {
                    LspLog.debug("references: truncated at " + MAX_REFERENCES + " results");
                    break;
                }
                SourceInformation accessSi = matchEnumLiteralAccess(refUsage, enumValueName);
                if (accessSi != null)
                {
                    addLocation(locations, seen, accessSi, uriMapper);
                }
            }
        }
        catch (Exception ignored)
        {
            // Reference usage shape not as expected; skip enum-literal-specific matching
        }
    }

    private static SourceInformation matchEnumLiteralAccess(CoreInstance refUsage, String enumValueName)
    {
        try
        {
            CoreInstance owner = refUsage.getValueForMetaPropertyToOne(M3Properties.owner);
            if (owner == null || owner.getClassifier() == null || !"InstanceValue".equals(owner.getClassifier().getName()))
            {
                return null;
            }
            CoreInstance usageContext = owner.getValueForMetaPropertyToOne(M3Properties.usageContext);
            if (usageContext == null || usageContext.getClassifier() == null
                    || !"ParameterValueSpecificationContext".equals(usageContext.getClassifier().getName()))
            {
                return null;
            }
            CoreInstance functionExpression = usageContext.getValueForMetaPropertyToOne(M3Properties.functionExpression);
            if (functionExpression == null || functionExpression.getClassifier() == null
                    || !"SimpleFunctionExpression".equals(functionExpression.getClassifier().getName()))
            {
                return null;
            }
            ListIterable<? extends CoreInstance> params =
                    functionExpression.getValueForMetaPropertyToMany(M3Properties.parametersValues);
            if (params == null || params.size() < 2)
            {
                return null;
            }
            ListIterable<? extends CoreInstance> nameValues = params.get(1).getValueForMetaPropertyToMany(M3Properties.values);
            if (nameValues == null || nameValues.size() != 1 || !enumValueName.equals(nameValues.get(0).getName()))
            {
                return null;
            }
            return functionExpression.getSourceInformation();
        }
        catch (Exception ignored)
        {
            return null;
        }
    }

    private static void addLocation(List<Location> locations, Set<String> seen,
                                    SourceInformation si, UriMapper uriMapper)
    {
        // Deduplicate by exact source position
        String key = si.getSourceId() + ":" + si.getStartLine() + ":" + si.getStartColumn();
        if (!seen.add(key))
        {
            return;
        }

        String uri = uriMapper.toUri(si.getSourceId());
        if (uri != null)
        {
            locations.add(SourceInfoUtil.toLocation(si, uri));
        }
    }
}
