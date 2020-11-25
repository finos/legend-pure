package org.finos.legend.pure.ide.light;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;

@JsonIgnoreProperties(
        ignoreUnknown = true
)
public class SourceLocationConfiguration {
    public String welcomeFileDirectory;
    public String coreFilesLocation;
    public String ideFilesLocation;
}
