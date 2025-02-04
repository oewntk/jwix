/* ******************************************************************************
 * Java Wordnet Interface Library (JWI) v2.4.0
 * Copyright (c) 2007-2015 Mark A. Finlayson
 *
 * JWI is distributed under the terms of the Creative Commons Attribution 4.0
 * International Public License, which means it may be freely used for all
 * purposes, as long as proper acknowledgment is made.  See the license file
 * included with this distribution for more details.
 *******************************************************************************/

package edu.mit.jwi.data;

import edu.mit.jwi.NonNull;
import edu.mit.jwi.Nullable;
import edu.mit.jwi.RAMDictionary;
import edu.mit.jwi.data.compare.ILineComparator;
import edu.mit.jwi.data.parse.ILineParser;
import edu.mit.jwi.item.ISynset;
import edu.mit.jwi.item.IVersion;
import edu.mit.jwi.item.POS;
import edu.mit.jwi.item.Synset;

import java.io.File;
import java.io.IOException;
import java.io.UnsupportedEncodingException;
import java.net.URI;
import java.net.URISyntaxException;
import java.net.URL;
import java.net.URLDecoder;
import java.nio.charset.Charset;
import java.util.*;
import java.util.Map.Entry;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReentrantLock;

/**
 * <p>
 * Implementation of a data provider for Wordnet that uses files in the file
 * system to back instances of its data sources. This implementation takes a
 * <code>URL</code> to a file system directory as its path argument, and uses
 * the resource hints from the data types and parts of speech for its content
 * types to examine the filenames in the that directory to determine which files
 * contain which data.
 * </p>
 * <p>
 * This implementation supports loading the wordnet files into memory,
 * but this is actually not that beneficial for speed. This is because the
 * implementation loads the file data into memory uninterpreted, and on modern
 * machines, the time to interpret a line of data (i.e., parse it into a Java
 * object) is much larger than the time it takes to load the line from disk.
 * Those wishing to achieve speed increases from loading Wordnet into memory
 * should rely on the implementation in {@link RAMDictionary}, or something
 * similar, which pre-processes the Wordnet data into objects before caching
 * them.
 * </p>
 *
 * @author Mark A. Finlayson
 * @version 2.4.0
 * @since JWI 1.0
 */
public class FileProvider implements IDataProvider, ILoadable, ILoadPolicy
{
    @SuppressWarnings("CanBeFinal")
    public static boolean verbose = false;

    // final instance fields
    private final Lock lifecycleLock = new ReentrantLock();
    private final Lock loadingLock = new ReentrantLock();
    @NonNull
    private final Map<ContentTypeKey, IContentType<?>> prototypeMap;

    // instance fields
    @Nullable
    private URL url;
    @Nullable
    private IVersion version = null;
    @Nullable
    private Map<IContentType<?>, ILoadableDataSource<?>> fileMap = null;
    private int loadPolicy;
    @Nullable
    private transient JWIBackgroundLoader loader = null;
    @NonNull
    private final Collection<? extends IContentType<?>> defaultContentTypes;
    @Nullable
    private Charset charset = null;
    @NonNull
    private final Map<ContentTypeKey, String> sourceMatcher = new HashMap<>();

    /**
     * Constructs the file provider pointing to the resource indicated by the
     * path.  This file provider has an initial {@link ILoadPolicy#NO_LOAD} load policy.
     *
     * @param file A file pointing to the wordnet directory, may not be
     *             <code>null</code>
     * @throws NullPointerException if the specified file is <code>null</code>
     * @since JWI 1.0
     */
    public FileProvider(File file)
    {
        this(toURL(file));
    }

    /**
     * Constructs the file provider pointing to the resource indicated by the
     * path, with the specified load policy.
     *
     * @param file       A file pointing to the wordnet directory, may not be
     *                   <code>null</code>
     * @param loadPolicy the load policy for this provider; this provider supports the
     *                   three values defined in <code>ILoadPolicy</code>.
     * @throws NullPointerException if the specified file is <code>null</code>
     * @since JWI 2.2.0
     */
    public FileProvider(File file, int loadPolicy)
    {
        this(toURL(file), loadPolicy, ContentType.values());
    }

    /**
     * Constructs the file provider pointing to the resource indicated by the
     * path, with the specified load policy, looking for the specified content
     * type.s
     *
     * @param file       A file pointing to the wordnet directory, may not be
     *                   <code>null</code>
     * @param loadPolicy the load policy for this provider; this provider supports the
     *                   three values defined in <code>ILoadPolicy</code>.
     * @param types      the content types this provider will look for when it loads
     *                   its data; may not be <code>null</code> or empty
     * @throws NullPointerException     if the file or content type collection is <code>null</code>
     * @throws IllegalArgumentException if the set of types is empty
     * @since JWI 2.2.0
     */
    public FileProvider(File file, int loadPolicy, @NonNull Collection<? extends IContentType<?>> types)
    {
        this(toURL(file), loadPolicy, types);
    }

    /**
     * Constructs the file provider pointing to the resource indicated by the
     * path.  This file provider has an initial {@link ILoadPolicy#NO_LOAD} load policy.
     *
     * @param url A file URL in UTF-8 decodable format, may not be
     *            <code>null</code>
     * @throws NullPointerException if the specified URL is <code>null</code>
     * @since JWI 1.0
     */
    public FileProvider(URL url)
    {
        this(url, NO_LOAD);
    }

    /**
     * Constructs the file provider pointing to the resource indicated by the
     * path, with the specified load policy.
     *
     * @param url        A file URL in UTF-8 decodable format, may not be
     *                   <code>null</code>
     * @param loadPolicy the load policy for this provider; this provider supports the
     *                   three values defined in <code>ILoadPolicy</code>.
     * @throws NullPointerException if the specified URL is <code>null</code>
     * @since JWI 2.2.0
     */
    public FileProvider(URL url, int loadPolicy)
    {
        this(url, loadPolicy, ContentType.values());
    }

    /**
     * Constructs the file provider pointing to the resource indicated by the
     * path, with the specified load policy, looking for the specified content
     * type.s
     *
     * @param url          A file URL in UTF-8 decodable format, may not be
     *                     <code>null</code>
     * @param loadPolicy   the load policy for this provider; this provider supports the
     *                     three values defined in <code>ILoadPolicy</code>.
     * @param contentTypes the content types this provider will look for when it loads
     *                     its data; may not be <code>null</code> or empty
     * @throws NullPointerException     if the url or content type collection is <code>null</code>
     * @throws IllegalArgumentException if the set of types is empty
     * @since JWI 2.2.0
     */
    public FileProvider(@Nullable URL url, int loadPolicy, @NonNull Collection<? extends IContentType<?>> contentTypes)
    {
        if (url == null)
        {
            throw new NullPointerException();
        }
        if (contentTypes.isEmpty())
        {
            throw new IllegalArgumentException();
        }
        this.url = url;
        this.loadPolicy = loadPolicy;
        this.defaultContentTypes = contentTypes;

        Map<ContentTypeKey, IContentType<?>> prototypeMap = new LinkedHashMap<>(contentTypes.size());
        for (IContentType<?> contentType : contentTypes)
        {
            ContentTypeKey key = contentType.getKey();
            prototypeMap.put(key, contentType);
        }
        this.prototypeMap = prototypeMap;
    }

    @Nullable
    private IContentType<?> getDefault(ContentTypeKey key)
    {
        for (IContentType<?> contentType : this.defaultContentTypes)
        {
            if (contentType.getKey().equals(key))
            {
                return contentType;
            }
        }
        // this should not happen
        return null;
    }

    /*
     * (non-Javadoc)
     *
     * @see edu.edu.mit.jwi.data.IDataProvider#getSource()
     */
    @Nullable
    public URL getSource()
    {
        return url;
    }

    /*
     * (non-Javadoc)
     *
     * @see edu.edu.mit.jwi.data.ILoadPolicy#getLoadPolicy()
     */
    public int getLoadPolicy()
    {
        return loadPolicy;
    }

    /*
     * (non-Javadoc)
     *
     * @see edu.edu.mit.jwi.data.IDataProvider#setSource(java.net.URL)
     */
    public void setSource(@Nullable URL url)
    {
        if (isOpen())
        {
            throw new IllegalStateException("provider currently open");
        }
        if (url == null)
        {
            throw new NullPointerException();
        }
        this.url = url;
    }

    /*
     * (non-Javadoc)
     *
     * @see edu.edu.mit.jwi.data.ILoadPolicy#setLoadPolicy(int)
     */
    public void setLoadPolicy(int policy)
    {
        try
        {
            loadingLock.lock();
            this.loadPolicy = policy;
        }
        finally
        {
            loadingLock.unlock();
        }
    }

    /*
     * (non-Javadoc)
     *
     * @see edu.edu.mit.jwi.item.IHasVersion#getVersion()
     */
    @Nullable
    public IVersion getVersion()
    {
        checkOpen();
        if (version == null)
        {
            assert fileMap != null;
            version = determineVersion(fileMap.values());
        }
        if (version == IVersion.NO_VERSION)
        {
            return null;
        }
        return version;
    }

    /**
     * Determines a version from the set of data sources, if possible, otherwise
     * returns {@link IVersion#NO_VERSION}
     *
     * @param srcs the data sources to be used to determine the version
     * @return the single version that describes these data sources, or
     * {@link IVersion#NO_VERSION} if there is none
     * @since JWI 2.1.0
     */
    @Nullable
    protected IVersion determineVersion(@NonNull Collection<? extends IDataSource<?>> srcs)
    {
        IVersion ver = IVersion.NO_VERSION;
        for (IDataSource<?> dataSrc : srcs)
        {
            // if no version to set, ignore
            if (dataSrc.getVersion() == null)
            {
                continue;
            }

            // init version
            if (ver == IVersion.NO_VERSION)
            {
                ver = dataSrc.getVersion();
                continue;
            }

            // if version different from current
            if (!ver.equals(dataSrc.getVersion()))
            {
                return IVersion.NO_VERSION;
            }
        }
        return ver;
    }

    /*
     * (non-Javadoc)
     *
     * @see edu.edu.mit.jwi.data.IHasCharset#getCharset()
     */
    @Nullable
    public Charset getCharset()
    {
        return charset;
    }

    /*
     * (non-Javadoc)
     *
     * @see edu.edu.mit.jwi.data.IDataProvider#setCharset(java.nio.charset.Charset)
     */
    public void setCharset(@Nullable Charset charset)
    {
        if (verbose)
        {
            System.out.printf("Charset: %s%n", charset);
        }
        try
        {
            lifecycleLock.lock();
            if (isOpen())
            {
                throw new IllegalStateException("provider currently open");
            }
            for (Entry<ContentTypeKey, IContentType<?>> e : prototypeMap.entrySet())
            {
                ContentTypeKey key = e.getKey();
                IContentType<?> value = e.getValue();
                if (charset == null)
                {
                    // if we get a null charset, reset to the prototype value but preserve line comparator
                    IContentType<?> defaultContentType = getDefault(key);
                    assert defaultContentType != null;
                    e.setValue(new ContentType<>(key, value.getLineComparator(), defaultContentType.getCharset()));
                }
                else
                {
                    // if we get a non-null charset, generate new  type using the new charset but preserve line comparator
                    e.setValue(new ContentType<>(key, value.getLineComparator(), charset));
                }
            }
            this.charset = charset;
        }
        finally
        {
            lifecycleLock.unlock();
        }
    }

    /*
     * (non-Javadoc)
     *
     * @see edu.edu.mit.jwi.data.IDataProvider#setComparator(edu.edu.mit.jwi.data.IContentType, edu.edu.mit.jwi.data.compare.ILineComparator)
     */
    public void setComparator(@NonNull ContentTypeKey key, @Nullable ILineComparator comparator)
    {
        if (verbose)
        {
            assert comparator != null;
            System.out.printf("Comparator for %s %s%n", key, comparator.getClass().getName());
        }
        try
        {
            lifecycleLock.lock();
            if (isOpen())
            {
                throw new IllegalStateException("provider currently open");
            }
            IContentType<?> value = prototypeMap.get(key);
            if (comparator == null)
            {
                // if we get a null comparator, reset to the prototype but preserve charset
                IContentType<?> defaultContentType = getDefault(key);
                assert defaultContentType != null;
                assert value != null;
                prototypeMap.put(key, new ContentType<>(key, defaultContentType.getLineComparator(), value.getCharset()));
            }
            else
            {
                // if we get a non-null comparator, generate a new type using the new comparator but preserve charset
                assert value != null;
                prototypeMap.put(key, new ContentType<>(key, comparator, value.getCharset()));
            }
        }
        finally
        {
            lifecycleLock.unlock();
        }
    }

    /*
     * (non-Javadoc)
     *
     * @see edu.edu.mit.jwi.data.IDataProvider#setSourceMatcher(edu.mit.data ContentTypeKey, java.lang.String)
     */
    public void setSourceMatcher(@NonNull ContentTypeKey key, @Nullable String pattern)
    {
        if (verbose)
        {
            System.out.printf("Matcher for %s: '%s'%n", key, pattern);
        }
        try
        {
            lifecycleLock.lock();
            if (isOpen())
            {
                throw new IllegalStateException("provider currently open");
            }
            if (pattern == null)
            {
                this.sourceMatcher.remove(key);
            }
            else
            {
                this.sourceMatcher.put(key, pattern);
            }
        }
        finally
        {
            lifecycleLock.unlock();
        }
    }

    /*
     * (non-Javadoc)
     *
     * @see edu.edu.mit.jwi.data.IDataProvider#resolveContentType(edu.edu.mit.jwi.data.IDataType, edu.edu.mit.jwi.item.POS)
     */
    @SuppressWarnings("unchecked")
    public <T> IContentType<T> resolveContentType(IDataType<T> dt, POS pos)
    {
        for (Entry<ContentTypeKey, IContentType<?>> e : prototypeMap.entrySet())
        {
            if (e.getKey().getDataType().equals(dt) && e.getKey().getPOS() == pos)
            {
                return (IContentType<T>) e.getValue();
            }
        }
        return null;
    }

    /*
     * (non-Javadoc)
     *
     * @see edu.edu.mit.jwi.data.IHasLifecycle#open()
     */
    public boolean open() throws IOException
    {
        try
        {
            lifecycleLock.lock();
            loadingLock.lock();

            int policy = getLoadPolicy();

            // make sure directory exists
            assert url != null;
            File directory = toFile(url);
            if (!directory.exists())
            {
                throw new IOException("Dictionary directory does not exist: " + directory);
            }

            // get files in directory
            File[] fileArray = directory.listFiles(File::isFile);
            if (fileArray == null || fileArray.length == 0)
            {
                throw new IOException("No files found in " + directory);
            }
            List<File> files = new ArrayList<>(Arrays.asList(fileArray));
            if (files.isEmpty())
            {
                throw new IOException("No files found in " + directory);
            }

            // sort them
            files.sort(Comparator.comparing(File::getName));

            // make the source map
            Map<IContentType<?>, ILoadableDataSource<?>> hiddenMap = createSourceMap(files, policy);
            if (hiddenMap.isEmpty())
            {
                return false;
            }

            // determine if it's already unmodifiable, wrap if not
            Map<?, ?> map = Collections.emptyMap();
            if (hiddenMap.getClass() != map.getClass())
            {
                hiddenMap = Collections.unmodifiableMap(hiddenMap);
            }
            this.fileMap = hiddenMap;

            // do load
            try
            {
                switch (loadPolicy)
                {
                    case BACKGROUND_LOAD:
                        load(false);
                        break;
                    case IMMEDIATE_LOAD:
                        load(true);
                        break;
                    default:
                        // do nothing
                }
            }
            catch (InterruptedException e)
            {
                e.printStackTrace();
            }
            return true;
        }
        finally
        {
            lifecycleLock.unlock();
            loadingLock.unlock();
        }
    }

    /*
     * (non-Javadoc)
     *
     * @see edu.edu.mit.jwi.data.ILoadable#load()
     */
    public void load()
    {
        try
        {
            load(false);
        }
        catch (InterruptedException e)
        {
            e.printStackTrace();
        }
    }

    /*
     * (non-Javadoc)
     *
     * @see edu.edu.mit.jwi.data.ILoadable#load(boolean)
     */
    public void load(boolean block) throws InterruptedException
    {
        try
        {
            loadingLock.lock();
            checkOpen();
            if (isLoaded())
            {
                return;
            }
            if (loader != null)
            {
                return;
            }
            loader = new JWIBackgroundLoader();
            loader.start();
            if (block)
            {
                loader.join();
            }
        }
        finally
        {
            loadingLock.lock();
        }
    }

    /*
     * (non-Javadoc)
     *
     * @see edu.edu.mit.jwi.data.ILoadable#isLoaded()
     */
    public boolean isLoaded()
    {
        if (!isOpen())
        {
            throw new IllegalStateException("provider not open");
        }
        try
        {
            loadingLock.lock();
            assert fileMap != null;
            for (ILoadableDataSource<?> source : fileMap.values())
            {
                if (!source.isLoaded())
                {
                    return false;
                }
            }
            return true;
        }
        finally
        {
            loadingLock.unlock();
        }
    }

    /**
     * Creates the map that contains the content types mapped to the data
     * sources. The method should return a non-null result, but it may be empty
     * if no data sources can be created. Subclasses may override this method.
     *
     * @param files  the files from which the data sources should be created, may
     *               not be <code>null</code>
     * @param policy the load policy of the provider
     * @return a map, possibly empty, but not <code>null</code>, of content
     * types mapped to data sources
     * @throws NullPointerException if the file list is <code>null</code>
     * @throws IOException          if there is a problem creating the data source
     * @since JWI 2.2.0
     */
    @NonNull
    protected Map<IContentType<?>, ILoadableDataSource<?>> createSourceMap(@NonNull List<File> files, int policy) throws IOException
    {
        Map<IContentType<?>, ILoadableDataSource<?>> result = new HashMap<>();
        for (IContentType<?> contentType : prototypeMap.values())
        {
            File file = null;

            // give first chance to matcher
            if (sourceMatcher.containsKey(contentType.getKey()))
            {
                String regex = sourceMatcher.get(contentType.getKey());
                assert regex != null;
                file = match(regex, files);
            }

            // if it failed fall back on data types
            if (file == null)
            {
                IDataType<?> dataType = contentType.getDataType();
                file = DataType.find(dataType, contentType.getPOS(), files);
            }

            // if it failed continue
            if (file == null)
            {
                continue;
            }

            // do not remove file from possible choices as both content types may use the same file
            if (!contentType.getKey().equals(ContentTypeKey.SENSE) && //
                    !contentType.getKey().equals(ContentTypeKey.SENSES) && //
                    !contentType.getKey().equals(ContentTypeKey.INDEX_ADJECTIVE) && //
                    !contentType.getKey().equals(ContentTypeKey.INDEX_ADVERB) && //
                    !contentType.getKey().equals(ContentTypeKey.INDEX_NOUN) && //
                    !contentType.getKey().equals(ContentTypeKey.INDEX_VERB)
            )
            {
                files.remove(file);
            }

            result.put(contentType, createDataSource(file, contentType, policy));
            if (verbose)
            {
                System.out.printf("%s %s%n", contentType, file.getName());
            }
        }
        return result;
    }

    @Nullable
    private File match(@NonNull String pattern, @NonNull List<File> files)
    {
        for (File file : files)
        {
            String name = file.getName();
            if (name.matches(pattern))
            {
                return file;
            }
        }
        return null;
    }

    /**
     * Creates the actual data source implementations.
     *
     * @param <T>         the content type of the data source
     * @param file        the file from which the data source should be created, may not
     *                    be <code>null</code>
     * @param contentType the content type of the data source
     * @param policy      the load policy to follow when creating the data source
     * @return the created data source
     * @throws NullPointerException if any argument is <code>null</code>
     * @throws IOException          if there is an IO problem when creating the data source
     * @since JWI 2.2.0
     */
    protected <T> ILoadableDataSource<T> createDataSource(@NonNull File file, @NonNull IContentType<T> contentType, int policy) throws IOException
    {
        ILoadableDataSource<T> src;
        if (contentType.getDataType() == DataType.DATA)
        {
            src = createDirectAccess(file, contentType);
            src.open();
            if (policy == IMMEDIATE_LOAD)
            {
                try
                {
                    src.load(true);
                }
                catch (InterruptedException e)
                {
                    e.printStackTrace();
                }
            }

            // check to see if direct access works with the file
            // often people will extract the files incorrectly on Windows machines
            // and the binary files will be corrupted with extra CRs

            // get first line
            Iterator<String> itr = src.iterator();
            String firstLine = itr.next();
            if (firstLine == null)
            {
                return src;
            }

            // extract key
            ILineParser<T> parser = contentType.getDataType().getParser();
            assert parser != null;
            ISynset s = (ISynset) parser.parseLine(firstLine);
            assert s != null;
            String key = Synset.zeroFillOffset(s.getOffset());

            // try to find line by direct access
            String soughtLine = src.getLine(key);
            if (soughtLine != null)
            {
                return src;
            }

            POS pos = contentType.getPOS();
            assert pos != null;
            System.err.println(System.currentTimeMillis() + " - Error on direct access in " + pos + " data file: check CR/LF endings");
        }

        src = createBinarySearch(file, contentType);
        src.open();
        if (policy == IMMEDIATE_LOAD)
        {
            try
            {
                src.load(true);
            }
            catch (InterruptedException e)
            {
                e.printStackTrace();
            }
        }
        return src;
    }

    /**
     * Creates a direct access data source for the specified type, using the
     * specified file.
     *
     * @param <T>         the parameter of the content type
     * @param file        the file on which the data source is based; may not be
     *                    <code>null</code>
     * @param contentType the data type for the data source; may not be
     *                    <code>null</code>
     * @return the data source
     * @throws NullPointerException if either argument is <code>null</code>
     * @since JWI 2.2.0
     */
    @NonNull
    protected <T> ILoadableDataSource<T> createDirectAccess(@NonNull File file, IContentType<T> contentType)
    {
        return new DirectAccessWordnetFile<>(file, contentType);
    }

    /**
     * Creates a binary search data source for the specified type, using the
     * specified file.
     *
     * @param <T>         the parameter of the content type
     * @param file        the file on which the data source is based; may not be
     *                    <code>null</code>
     * @param contentType the data type for the data source; may not be
     *                    <code>null</code>
     * @return the data source
     * @throws NullPointerException if either argument is <code>null</code>
     * @since JWI 2.2.0
     */
    @NonNull
    protected <T> ILoadableDataSource<T> createBinarySearch(@NonNull File file, IContentType<T> contentType)
    {
        return "Word".equals(contentType.getDataType().toString()) ?
                new BinaryStartSearchWordnetFile<>(file, contentType) :
                new BinarySearchWordnetFile<>(file, contentType);
    }

    /*
     * (non-Javadoc)
     *
     * @see edu.edu.mit.jwi.data.IHasLifecycle#isOpen()
     */
    public boolean isOpen()
    {
        try
        {
            lifecycleLock.lock();
            return fileMap != null;
        }
        finally
        {
            lifecycleLock.unlock();
        }
    }

    /*
     * (non-Javadoc)
     *
     * @see edu.edu.mit.jwi.data.IClosable#close()
     */
    public void close()
    {
        try
        {
            lifecycleLock.lock();
            if (!isOpen())
            {
                return;
            }
            if (loader != null)
            {
                loader.cancel();
            }
            assert fileMap != null;
            for (IDataSource<?> source : fileMap.values())
            {
                source.close();
            }
            fileMap = null;
        }
        finally
        {
            lifecycleLock.unlock();
        }
    }

    /**
     * Convenience method that throws an exception if the provider is closed.
     *
     * @throws ObjectClosedException if the provider is closed
     * @since JWI 1.1
     */
    protected void checkOpen()
    {
        if (!isOpen())
        {
            throw new ObjectClosedException();
        }
    }

    /*
     * (non-Javadoc)
     *
     * @see edu.edu.mit.jwi.data.IDataProvider#getSource(edu.edu.mit.jwi.data.IContentType)
     */
    // no way to safely cast; must rely on registerSource method to assure compliance
    @Nullable
    @SuppressWarnings("unchecked")
    public <T> ILoadableDataSource<T> getSource(@NonNull IContentType<T> contentType)
    {
        checkOpen();

        // assume at first this the prototype
        IContentType<?> actualType = prototypeMap.get(contentType.getKey());

        // if this does not map to an adjusted type, we will check under it directly
        if (actualType == null)
        {
            actualType = contentType;
        }
        assert fileMap != null;
        return (ILoadableDataSource<T>) fileMap.get(actualType);
    }

    /*
     * (non-Javadoc)
     *
     * @see edu.edu.mit.jwi.data.IDataProvider#getTypes()
     */
    @NonNull
    public Set<? extends IContentType<?>> getTypes()
    {
        try
        {
            lifecycleLock.lock();
            return new LinkedHashSet<>(prototypeMap.values());
        }
        finally
        {
            lifecycleLock.unlock();
        }
    }

    /**
     * A thread class which tries to load each data source in this provider.
     *
     * @author Mark A. Finlayson
     * @version 2.4.0
     * @since JWI 2.2.0
     */
    protected class JWIBackgroundLoader extends Thread
    {
        // cancel flag
        private transient boolean cancel = false;

        /**
         * Constructs a new background loader that operates
         * on the internal data structures of this provider.
         *
         * @since JWI 2.2.0
         */
        public JWIBackgroundLoader()
        {
            setName(JWIBackgroundLoader.class.getSimpleName());
            setDaemon(true);
        }

        /*
         * (non-Javadoc)
         *
         * @see java.lang.Thread#run()
         */
        @Override
        public void run()
        {
            try
            {
                assert fileMap != null;
                for (ILoadableDataSource<?> source : fileMap.values())
                {
                    if (!cancel && !source.isLoaded())
                    {
                        try
                        {
                            source.load(true);
                        }
                        catch (InterruptedException e)
                        {
                            e.printStackTrace();
                        }
                    }
                }
            }
            finally
            {
                loader = null;
            }
        }

        /**
         * Sets the cancel flag for this loader.
         *
         * @since JWI 2.2.0
         */
        public void cancel()
        {
            cancel = true;
            try
            {
                join();
            }
            catch (InterruptedException e)
            {
                e.printStackTrace();
            }
        }
    }

    /**
     * Transforms a URL into a File. The URL must use the 'file' protocol and
     * must be in a UTF-8 compatible format as specified in
     * {@link URLDecoder}.
     *
     * @param url url
     * @return a file pointing to the same place as the url
     * @throws NullPointerException     if the url is <code>null</code>
     * @throws IllegalArgumentException if the url does not use the 'file' protocol
     * @since JWI 1.0
     */
    @NonNull
    public static File toFile(@NonNull URL url)
    {
        if (!url.getProtocol().equals("file"))
        {
            throw new IllegalArgumentException("URL source must use 'file' protocol");
        }
        try
        {
            return new File(URLDecoder.decode(url.getPath(), "UTF-8"));
        }
        catch (UnsupportedEncodingException e)
        {
            throw new RuntimeException(e);
        }
    }

    /**
     * Transforms a file into a URL.
     *
     * @param file the file to be transformed
     * @return a URL representing the file
     * @throws NullPointerException if the specified file is <code>null</code>
     * @since JWI 2.2.0
     */
    @Nullable
    public static URL toURL(@Nullable File file)
    {
        if (file == null)
        {
            throw new NullPointerException();
        }
        try
        {
            URI uri = new URI("file", "//", file.toURI().toURL().getPath(), null);
            return new URL("file", null, uri.getRawPath());
        }
        catch (@NonNull IOException | URISyntaxException e)
        {
            e.printStackTrace();
            return null;
        }
    }

    /**
     * A utility method for checking whether a file represents an existing local
     * directory.
     *
     * @param url the url object to check, may not be <code>null</code>
     * @return <code>true</code> if the url object represents a local directory
     * which exists; <code>false</code> otherwise.
     * @throws NullPointerException if the specified url object is <code>null</code>
     * @since JWI 2.4.0
     */
    public static boolean isLocalDirectory(@NonNull URL url)
    {
        if (!url.getProtocol().equals("file"))
        {
            return false;
        }
        File file = FileProvider.toFile(url);
        return isLocalDirectory(file);
    }

    /**
     * A utility method for checking whether a file represents an existing local
     * directory.
     *
     * @param dir the file object to check, may not be <code>null</code>
     * @return <code>true</code> if the file object represents a local directory
     * which exist; <code>false</code> otherwise.
     * @throws NullPointerException if the specified file object is <code>null</code>
     * @since JWI 2.4.0
     */
    public static boolean isLocalDirectory(@NonNull File dir)
    {
        return dir.exists() && dir.isDirectory();
    }
}
