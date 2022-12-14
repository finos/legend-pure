package org.finos.legend.pure.ide.light.api;

import io.swagger.annotations.Api;
import org.eclipse.collections.api.list.ListIterable;
import org.eclipse.collections.api.list.MutableList;
import org.eclipse.collections.impl.factory.Lists;
import org.finos.legend.pure.ide.light.helpers.response.ExceptionTranslation;
import org.finos.legend.pure.ide.light.session.PureSession;
import org.finos.legend.pure.m3.coreinstance.Package;
import org.finos.legend.pure.m3.coreinstance.meta.pure.metamodel.function.PackageableFunction;
import org.finos.legend.pure.m3.coreinstance.meta.pure.metamodel.function.property.Property;
import org.finos.legend.pure.m3.coreinstance.meta.pure.metamodel.type.Class;
import org.finos.legend.pure.m3.navigation.M3Properties;
import org.finos.legend.pure.m3.navigation.PackageableElement.PackageableElement;
import org.finos.legend.pure.m3.navigation.ProcessorSupport;
import org.finos.legend.pure.m3.navigation.function.Function;
import org.finos.legend.pure.m3.serialization.runtime.PureRuntime;
import org.finos.legend.pure.m4.coreinstance.CoreInstance;
import org.json.simple.JSONValue;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.ws.rs.POST;
import javax.ws.rs.Path;
import javax.ws.rs.core.Context;
import javax.ws.rs.core.Response;
import javax.ws.rs.core.StreamingOutput;
import java.io.ByteArrayOutputStream;
import java.util.Arrays;
import java.util.List;

@Api(tags = "Suggestion")
@Path("/")
public class Suggestion
{
    // NOTE: this is the list of auto-import packages defined in m3.pure
    private static final List<String> AUTO_IMPORTS = Arrays.asList(
            "meta::pure::metamodel",
            "meta::pure::metamodel::type",
            "meta::pure::metamodel::type::generics",
            "meta::pure::metamodel::relationship",
            "meta::pure::metamodel::valuespecification",
            "meta::pure::metamodel::multiplicity",
            "meta::pure::metamodel::function",
            "meta::pure::metamodel::function::property",
            "meta::pure::metamodel::extension",
            "meta::pure::metamodel::import",
            "meta::pure::functions::date",
            "meta::pure::functions::string",
            "meta::pure::functions::collection",
            "meta::pure::functions::meta",
            "meta::pure::functions::constraints",
            "meta::pure::functions::lang",
            "meta::pure::functions::boolean",
            "meta::pure::functions::tools",
            "meta::pure::functions::io",
            "meta::pure::functions::math",
            "meta::pure::functions::asserts",
            "meta::pure::functions::test",
            "meta::pure::functions::multiplicity",
            "meta::pure::router",
            "meta::pure::service",
            "meta::pure::tds",
            "meta::pure::tools",
            "meta::pure::profiles"
    );
    private final PureSession session;

    public Suggestion(PureSession session)
    {
        this.session = session;
    }

    @POST
    @Path("suggestion/incompletePath")
    public Response getSuggestionsForIncompletePath(
            @Context HttpServletRequest request,
            IncompletePathSuggestionInput input,
            @Context HttpServletResponse response)
    {
        PureRuntime runtime = this.session.getPureRuntime();
        ProcessorSupport processorSupport = runtime.getProcessorSupport();
        try
        {
            CoreInstance coreInstance = runtime.getCoreInstance(input.path);
            if (coreInstance instanceof Package)
            {
                ListIterable<? extends CoreInstance> children = coreInstance.getValueForMetaPropertyToMany(M3Properties.children);
                return Response.ok((StreamingOutput) outputStream ->
                {
                    outputStream.write("[".getBytes());
                    for (int i = 0; i < children.size(); i++)
                    {
                        CoreInstance child = children.get(i);
                        String pureName = child instanceof PackageableFunction ? child.getValueForMetaPropertyToOne(M3Properties.functionName).getName() : child.getValueForMetaPropertyToOne(M3Properties.name).getName();
                        String text = child instanceof PackageableFunction ? Function.prettyPrint(child, processorSupport) : child.getValueForMetaPropertyToOne(M3Properties.name).getName();

                        outputStream.write("{\"pureType\":\"".getBytes());
                        outputStream.write(JSONValue.escape(child.getClassifier().getName()).getBytes());
                        outputStream.write("\",\"pureName\":\"".getBytes());
                        outputStream.write(JSONValue.escape(pureName).getBytes());
                        outputStream.write("\",\"pureId\":\"".getBytes());
                        outputStream.write(JSONValue.escape(PackageableElement.getUserPathForPackageableElement(child)).getBytes());
                        outputStream.write("\",\"text\":\"".getBytes());
                        outputStream.write(JSONValue.escape(text).getBytes());
                        outputStream.write("\"}".getBytes());

                        if (i != children.size() - 1)
                        {
                            outputStream.write(",".getBytes());
                        }
                    }
                    outputStream.write("]".getBytes());
                    outputStream.close();
                }).build();
            }
            return Response.ok((StreamingOutput) outputStream ->
            {
                outputStream.write("[]".getBytes());
                outputStream.close();
            }).build();
        }
        catch (Exception e)
        {
            return Response.status(Response.Status.BAD_REQUEST).entity((StreamingOutput) outputStream ->
            {
                outputStream.write(("\"" + JSONValue.escape(ExceptionTranslation.buildExceptionMessage(session, e, new ByteArrayOutputStream()).getText()) + "\"").getBytes());
                outputStream.close();
            }).build();
        }
    }

    public static class IncompletePathSuggestionInput
    {
        public String path;
    }

    @POST
    @Path("suggestion/identifier")
    public Response getSuggestionsForIdentifier(@Context HttpServletRequest request,
                                                IdentifierSuggestionInput input,
                                                @Context HttpServletResponse response)
    {
        PureRuntime runtime = this.session.getPureRuntime();
        ProcessorSupport processorSupport = runtime.getProcessorSupport();
        // NOTE: here we take into account: first, the imported packages in scope, then the root package (::) and lastly
        // the auto imported packages in the global scope
        MutableList<String> allPackagePaths = Lists.adapt(input.importPaths).with("::").withAll(AUTO_IMPORTS).distinct();

        try
        {
            MutableList<Package> packages = allPackagePaths.collect(runtime::getCoreInstance).selectInstancesOf(Package.class);
            return Response.ok((StreamingOutput) outputStream ->
            {
                List<? extends CoreInstance> children = packages.flatCollect(pack -> pack.getValueForMetaPropertyToMany(M3Properties.children))
                        // we do not need to get the packages here
                        .select(child -> !(child instanceof Package))
                        .select(child -> input.types == null || input.types.isEmpty() || input.types.contains(child.getClassifier().getName()));

                outputStream.write("[".getBytes());
                for (int i = 0; i < children.size(); i++)
                {
                    CoreInstance child = children.get(i);
                    String pureName = child instanceof PackageableFunction ? child.getValueForMetaPropertyToOne(M3Properties.functionName).getName() : child.getValueForMetaPropertyToOne(M3Properties.name).getName();
                    String text = child instanceof PackageableFunction ? Function.prettyPrint(child, processorSupport) : child.getValueForMetaPropertyToOne(M3Properties.name).getName();

                    outputStream.write("{\"pureType\":\"".getBytes());
                    outputStream.write(JSONValue.escape(child.getClassifier().getName()).getBytes());
                    outputStream.write("\",\"pureName\":\"".getBytes());
                    outputStream.write(JSONValue.escape(pureName).getBytes());
                    outputStream.write("\",\"pureId\":\"".getBytes());
                    outputStream.write(JSONValue.escape(PackageableElement.getUserPathForPackageableElement(child)).getBytes());
                    outputStream.write("\",\"text\":\"".getBytes());
                    outputStream.write(JSONValue.escape(text).getBytes());
                    outputStream.write("\"}".getBytes());

                    if (i != children.size() - 1)
                    {
                        outputStream.write(",".getBytes());
                    }
                }
                outputStream.write("]".getBytes());
                outputStream.close();
            }).build();
        }
        catch (Exception e)
        {
            return Response.status(Response.Status.BAD_REQUEST).entity((StreamingOutput) outputStream ->
            {
                outputStream.write(("\"" + JSONValue.escape(ExceptionTranslation.buildExceptionMessage(session, e, new ByteArrayOutputStream()).getText()) + "\"").getBytes());
                outputStream.close();
            }).build();
        }
    }

    public static class IdentifierSuggestionInput
    {
        public List<String> importPaths;
        public List<String> types;
    }

    @POST
    @Path("suggestion/class")
    public Response getSuggestionsForAttribute(@Context HttpServletRequest request,
                                               ClassSuggestionInput input,
                                               @Context HttpServletResponse response)
    {
        PureRuntime runtime = this.session.getPureRuntime();
        ProcessorSupport processorSupport = runtime.getProcessorSupport();
        MutableList<String> packagePaths = input.packagePath != null ? Lists.mutable.of(input.packagePath) : Lists.adapt(input.importPaths).withAll(AUTO_IMPORTS).distinct();

        try
        {
            MutableList<Class> classes = packagePaths.collect(runtime::getCoreInstance)
                    .flatCollect(pkg -> pkg.getValueForMetaPropertyToMany(M3Properties.children))
                    .selectInstancesOf(Class.class);
            return Response.ok((StreamingOutput) outputStream ->
            {
                outputStream.write("[".getBytes());
                for (int i = 0; i < classes.size(); i++)
                {
                    Class<?> cls = classes.get(i);

                    MutableList<Property> requiredProperties = org.finos.legend.pure.m3.navigation.property.Property.getAllProperties(cls, processorSupport)
                            // NOTE: make sure to only consider required (non-qualified) properties: i.e. multiplicity lower bound != 0
                            .selectInstancesOf(Property.class).select(prop ->
                                    {
                                        CoreInstance lowerBound = prop.getValueForMetaPropertyToOne(M3Properties.multiplicity).getValueForMetaPropertyToOne(M3Properties.lowerBound);
                                        // NOTE: here the lower bound can be nullish when there's multiplicity parameter being used
                                        // but we skip that case for now
                                        return lowerBound != null && !lowerBound.getValueForMetaPropertyToOne(M3Properties.value).getName().equals("0");
                                    }
                            ).toList();

                    outputStream.write("{\"pureName\":\"".getBytes());
                    outputStream.write(JSONValue.escape(cls.getValueForMetaPropertyToOne(M3Properties.name).getName()).getBytes());
                    outputStream.write("\",\"pureId\":\"".getBytes());
                    outputStream.write(JSONValue.escape(PackageableElement.getUserPathForPackageableElement(cls)).getBytes());
                    outputStream.write("\",\"requiredProperties\":[".getBytes());

                    for (int j = 0; j < requiredProperties.size(); j++)
                    {
                        outputStream.write("\"".getBytes());
                        outputStream.write(JSONValue.escape(requiredProperties.get(j).getValueForMetaPropertyToOne(M3Properties.name).getName()).getBytes());
                        outputStream.write("\"".getBytes());

                        if (j != requiredProperties.size() - 1)
                        {
                            outputStream.write(",".getBytes());
                        }
                    }

                    outputStream.write("]}".getBytes());

                    if (i != classes.size() - 1)
                    {
                        outputStream.write(",".getBytes());
                    }
                }

                outputStream.write("]".getBytes());
                outputStream.close();
            }).build();
        }
        catch (Exception e)
        {
            return Response.status(Response.Status.BAD_REQUEST).entity((StreamingOutput) outputStream ->
            {
                outputStream.write(("\"" + JSONValue.escape(ExceptionTranslation.buildExceptionMessage(session, e, new ByteArrayOutputStream()).getText()) + "\"").getBytes());
                outputStream.close();
            }).build();
        }
    }

    public static class ClassSuggestionInput
    {
        public List<String> importPaths;
        public String packagePath;
    }
}
