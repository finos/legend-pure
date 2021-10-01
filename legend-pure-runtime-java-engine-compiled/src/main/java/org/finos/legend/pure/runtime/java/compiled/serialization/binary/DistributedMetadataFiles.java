package org.finos.legend.pure.runtime.java.compiled.serialization.binary;

class DistributedMetadataFiles
{
    private static final String META_DATA_DIRNAME = "metadata/";
    private static final String BIN_FILE_EXTENSION = ".bin";
    private static final String INDEX_FILE_EXTENSION = ".idx";

    // Metadata

    static String getMetadataClassifierIndexFilePath(String metadataName, String classifierName)
    {
        return (metadataName == null) ?
                (META_DATA_DIRNAME + "classifiers/" + classifierName.replace("::", "/") + INDEX_FILE_EXTENSION) :
                (META_DATA_DIRNAME + metadataName + "/classifiers/" + classifierName.replace("::", "/") + INDEX_FILE_EXTENSION);
    }

    static String getMetadataPartitionBinFilePath(String metadataName, int partitionId)
    {
        return (metadataName == null) ?
                (META_DATA_DIRNAME + partitionId + BIN_FILE_EXTENSION) :
                (META_DATA_DIRNAME + metadataName + "/" + partitionId + BIN_FILE_EXTENSION);
    }

    // Strings

    static String getClassifierIdStringsIndexFilePath(String metadataName)
    {
        return (metadataName == null) ?
                (META_DATA_DIRNAME + "strings/classifiers" + INDEX_FILE_EXTENSION) :
                (META_DATA_DIRNAME + metadataName + "/strings/classifiers" + INDEX_FILE_EXTENSION);
    }

    static String getOtherStringsIndexFilePath(String metadataName)
    {
        return (metadataName == null) ?
                (META_DATA_DIRNAME + "strings/other" + INDEX_FILE_EXTENSION) :
                (META_DATA_DIRNAME + metadataName + "/strings/other" + INDEX_FILE_EXTENSION);
    }

    static String getOtherStringsIndexPartitionFilePath(String metadataName, int partitionId)
    {
        return (metadataName == null) ?
                (META_DATA_DIRNAME + "strings/other-" + partitionId + INDEX_FILE_EXTENSION) :
                (META_DATA_DIRNAME + metadataName + "/strings/other-" + partitionId + INDEX_FILE_EXTENSION);
    }
}
