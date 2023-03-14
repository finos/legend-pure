package org.finos.legend.pure.m3.serialization.filesystem.usercodestorage.welcome;

import org.eclipse.collections.api.RichIterable;
import org.eclipse.collections.api.list.MutableList;
import org.eclipse.collections.impl.factory.Lists;
import org.finos.legend.pure.m3.serialization.filesystem.repository.CodeRepository;
import org.finos.legend.pure.m3.serialization.filesystem.usercodestorage.CodeStorageNode;
import org.finos.legend.pure.m3.serialization.filesystem.usercodestorage.MutableRepositoryCodeStorage;
import org.finos.legend.pure.m3.serialization.filesystem.usercodestorage.RepositoryCodeStorage;
import org.finos.legend.pure.m3.serialization.filesystem.usercodestorage.composite.CompositeCodeStorage;
import org.finos.legend.pure.m3.serialization.runtime.Message;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;

public class WelcomeCodeStorage implements MutableRepositoryCodeStorage
{
    private final Path root;

    private final MutableList<CodeRepository> repos = Lists.mutable.with(CodeRepository.newScratchCodeRepository(WELCOME_FILE_NAME));

    public static final String WELCOME_FILE_NAME = "welcome.pure";
    public static final String WELCOME_FILE_PATH = RepositoryCodeStorage.ROOT_PATH + WELCOME_FILE_NAME;
    private static final String WELCOME_RESOURCE_NAME = "/org/finos/legend/pure/m3/serialization/filesystem/welcome.pure";


    public WelcomeCodeStorage(Path root)
    {
        this.root = root;
    }

    @Override
    public void initialize(Message message)
    {
        // Create /welcome.pure if it does not exist
        Path welcomeFile = resolveWelcomePath();
        if (Files.notExists(welcomeFile))
        {
            try (OutputStream outStream = Files.newOutputStream(welcomeFile);
                 InputStream inStream = getClass().getResourceAsStream(WELCOME_RESOURCE_NAME))
            {
                byte[] buffer = new byte[2048];
                for (int read = inStream.read(buffer); read != -1; read = inStream.read(buffer))
                {
                    outStream.write(buffer, 0, read);
                }
            }
            catch (IOException e)
            {
                throw new RuntimeException("Error creating /" + WELCOME_FILE_NAME, e);
            }
        }
    }

    @Override
    public RichIterable<CodeRepository> getAllRepositories()
    {
        return repos;
    }

    @Override
    public CodeRepository getRepository(String name)
    {
        return WELCOME_FILE_NAME.equals(name) ? repos.getFirst() : null;
    }

    @Override
    public CodeRepository getRepositoryForPath(String path)
    {
        return WELCOME_FILE_NAME.equals(path) ? repos.getFirst() : null;
    }

    @Override
    public CodeStorageNode getNode(String path)
    {
        // Special case: /welcome.pure
        if (isWelcomePath(path))
        {
            if (!welcomeExists())
            {
                throw new IllegalArgumentException("Cannot find " + path);
            }
            return getWelcomeNode();
        }
        throw new RuntimeException("!");
    }

    @Override
    public RichIterable<CodeStorageNode> getFiles(String path)
    {
        if (isWelcomePath(path))
        {
            return Lists.mutable.with(getWelcomeNode());
        }
        return Lists.mutable.empty();
    }

    @Override
    public RichIterable<String> getUserFiles()
    {
        if (welcomeExists())
        {
            return Lists.mutable.with(WELCOME_FILE_PATH);
        }
        return Lists.mutable.empty();
    }

    @Override
    public RichIterable<String> getFileOrFiles(String path)
    {
        // Special case: /welcome.pure
        if (isWelcomePath(path))
        {
            if (!welcomeExists())
            {
                throw new IllegalArgumentException("Cannot get files for " + path);
            }
            return Lists.immutable.with(WELCOME_FILE_PATH);
        }
        throw new RuntimeException("!");
    }

    @Override
    public InputStream getContent(String path)
    {
        // Special case: /welcome.pure
        if (isWelcomePath(path))
        {
            if (!welcomeExists())
            {
                throw new RuntimeException("Error getting content for " + path + ": no such file");
            }
            try
            {
                return Files.newInputStream(resolveWelcomePath());
            }
            catch (IOException e)
            {
                throw new RuntimeException("Error getting content for " + path, e);
            }
        }
        throw new RuntimeException("!");
    }

    @Override
    public byte[] getContentAsBytes(String path)
    {
        // Special case: /welcome.pure
        if (isWelcomePath(path))
        {
            if (!welcomeExists())
            {
                throw new RuntimeException("Error getting content as bytes for " + path + ": no such file");
            }
            try
            {
                return Files.readAllBytes(resolveWelcomePath());
            }
            catch (IOException e)
            {
                throw new RuntimeException("Error getting content as bytes for " + path, e);
            }
        }
        throw new RuntimeException("!");
    }

    @Override
    public String getContentAsText(String path)
    {
        // Special case: /welcome.pure
        if (isWelcomePath(path))
        {
            if (!welcomeExists())
            {
                throw new RuntimeException("Error getting content as text for " + path + ": no such file");
            }
            try
            {
                return new String(Files.readAllBytes(resolveWelcomePath()), StandardCharsets.UTF_8);
            }
            catch (IOException e)
            {
                throw new RuntimeException("Error getting content as text for " + path, e);
            }
        }
        throw new RuntimeException("!");
    }

    @Override
    public boolean exists(String path)
    {
        // Special case: /welcome.pure
        if (isWelcomePath(path))
        {
            return welcomeExists();
        }
        throw new RuntimeException("!");
    }

    @Override
    public boolean isFile(String path)
    {
        // Special case: /welcome.pure
        if (isWelcomePath(path))
        {
            return welcomeExists();
        }
        throw new RuntimeException("!");
    }

    @Override
    public boolean isFolder(String path)
    {
        // Special case: /welcome.pure
        if (isWelcomePath(path))
        {
            return false;
        }
        throw new RuntimeException("!");
    }

    @Override
    public boolean isEmptyFolder(String path)
    {
        // Special case: /welcome.pure
        if (isWelcomePath(path))
        {
            return false;
        }
        throw new RuntimeException("!");
    }

    @Override
    public OutputStream writeContent(String path)
    {
        // Special case: /welcome.pure
        if (isWelcomePath(path))
        {
            if (this.root == null)
            {
                throw new RuntimeException("Error trying to get output stream for " + path + ": no such file");
            }
            try
            {
                return Files.newOutputStream(resolveWelcomePath());
            }
            catch (IOException e)
            {
                throw new RuntimeException("Error trying to get output stream for " + path, e);
            }
        }
        throw new RuntimeException("!");
    }

    @Override
    public void writeContent(String path, String content)
    {
        // Special case: /welcome.pure
        if (isWelcomePath(path))
        {
            if (this.root == null)
            {
                throw new RuntimeException("Error trying to write content to " + path + ": no such file");
            }
            try
            {
                Files.write(resolveWelcomePath(), content.getBytes(StandardCharsets.UTF_8));
                return;
            }
            catch (IOException e)
            {
                throw new RuntimeException("Error trying to write content to " + path, e);
            }
        }
        throw new RuntimeException("!");
    }

    @Override
    public void createFile(String filePath)
    {

    }

    @Override
    public void createFolder(String folderPath)
    {

    }

    @Override
    public void deleteFile(String filePath)
    {

    }

    @Override
    public void moveFile(String sourcePath, String destinationPath)
    {

    }


    private Path resolveWelcomePath()
    {
        return this.root.resolve(WELCOME_FILE_NAME);
    }

    private boolean welcomeExists()
    {
        return this.root != null && Files.exists(resolveWelcomePath());
    }

    private boolean isWelcomePath(String path)
    {
        return WELCOME_FILE_PATH.equals(path) || WELCOME_FILE_NAME.equals(path);
    }

    private CodeStorageNode getWelcomeNode()
    {
        return new CompositeCodeStorage.RootCodeStorageNode(WELCOME_FILE_NAME, false);
    }
}
