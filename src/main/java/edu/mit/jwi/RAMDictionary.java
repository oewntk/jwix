/* ******************************************************************************
 * Java Wordnet Interface Library (JWI) v2.4.0
 * Copyright (c) 2007-2015 Mark A. Finlayson
 *
 * JWI is distributed under the terms of the Creative Commons Attribution 4.0
 * International Public License, which means it may be freely used for all
 * purposes, as long as proper acknowledgment is made.  See the license file
 * included with this distribution for more details.
 *******************************************************************************/

package edu.mit.jwi;

import edu.mit.jwi.data.ContentTypeKey;
import edu.mit.jwi.data.FileProvider;
import edu.mit.jwi.data.ILoadPolicy;
import edu.mit.jwi.data.compare.ILineComparator;
import edu.mit.jwi.item.*;
import edu.mit.jwi.item.Synset.IWordBuilder;

import java.io.*;
import java.net.URL;
import java.nio.charset.Charset;
import java.util.*;
import java.util.Map.Entry;
import java.util.concurrent.Callable;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReentrantLock;
import java.util.zip.GZIPInputStream;
import java.util.zip.GZIPOutputStream;

/**
 * Default implementation of the <code>IRAMDictionary</code> interface. This
 * implementation is designed to wrap an arbitrary dictionary object; however,
 * convenience constructors are provided for the most common use cases:
 * <ul>
 * <li>Wordnet files located on the local file system</li>
 * <li>Wordnet data to be loaded into memory from an exported stream</li>
 * </ul>
 * <p>
 * <b>Note:</b> If you receive an {@link OutOfMemoryError} while using this
 * object (this can occur on 32 bit JVMs), try increasing your heap size, for
 * example, by using the <code>-Xmx</code> switch.
 *
 * @author Mark A. Finlayson
 * @version 2.4.0
 * @since JWI 2.2.0
 */
public class RAMDictionary implements IRAMDictionary
{
    @NonNull
    private static <T> T requireNonNull(@Nullable T t)
    {
        if (t == null)
            throw new NullPointerException();
        return t;
    }

    /**
     * The default load policy of a {@link RAMDictionary} is to load data in the
     * background when opened.
     *
     * @since JWI 2.4.0
     */
    public static final int defaultLoadPolicy = ILoadPolicy.BACKGROUND_LOAD;

    // immutable fields
    @Nullable
    protected final IDictionary backing;
    @Nullable
    protected final IInputStreamFactory factory;
    protected final Lock lifecycleLock = new ReentrantLock();
    protected final Lock loadLock = new ReentrantLock();

    // instance fields
    @NonNull
    protected volatile LifecycleState state = LifecycleState.CLOSED;
    @Nullable
    protected transient Thread loader;
    protected int loadPolicy;
    @Nullable
    protected DictionaryData data;

    /**
     * Constructs a new wrapper RAM dictionary that will load the contents the
     * specified local Wordnet data, with the specified load policy. Note that
     * if the file points to an exported image of an in-memory dictionary, the
     * required load policy is to load immediately.
     *
     * @param file a file pointing to a local copy of wordnet; may not be
     *             <code>null</code>
     * @throws NullPointerException if the specified file is <code>null</code>
     * @since JWI 2.4.0
     */
    public RAMDictionary(@NonNull File file)
    {
        this(file, defaultLoadPolicy);
    }

    /**
     * Constructs a new RAMDictionary that will load the contents the specified
     * Wordnet data using the default load policy. Note that if the url points
     * to a resource that is the exported image of an in-memory dictionary, the
     * required load policy is to load immediately.
     *
     * @param url an url pointing to a local copy of wordnet; may not be
     *            <code>null</code>
     * @throws NullPointerException if the specified url is <code>null</code>
     * @since JWI 2.4.0
     */
    public RAMDictionary(@NonNull URL url)
    {
        this(url, defaultLoadPolicy);
    }

    /**
     * Loads data from the specified File using the specified load policy. Note
     * that if the file points to a resource that is the exported image of an
     * in-memory dictionary, the specified load policy is ignored: the
     * dictionary is loaded into memory immediately.
     *
     * @param file       a file pointing to a local copy of wordnet; may not be
     *                   <code>null</code>
     * @param loadPolicy the load policy of the dictionary; see constants in
     *                   {@link ILoadPolicy}. Note that if the file points to a
     *                   resource that is the exported image of an in-memory
     *                   dictionary, the specified load policy is ignored: the
     *                   dictionary is loaded into memory immediately.
     * @throws NullPointerException if the specified file is <code>null</code>
     * @see ILoadPolicy
     * @since JWI 2.2.0
     */
    public RAMDictionary(@NonNull File file, int loadPolicy)
    {
        this(createBackingDictionary(file), createInputStreamFactory(file), loadPolicy);
    }

    /**
     * Loads data from the specified URL using the specified load policy. Note
     * that if the url points to a resource that is the exported image of an
     * in-memory dictionary, the specified load policy is ignored: the
     * dictionary is loaded into memory immediately.
     *
     * @param url        an url pointing to a local copy of wordnet; may not be
     *                   <code>null</code>
     * @param loadPolicy the load policy of the dictionary; see constants in
     *                   {@link ILoadPolicy}. Note that if the url points to a
     *                   resource that is the exported image of an in-memory
     *                   dictionary, the specified load policy is ignored: the
     *                   dictionary is loaded into memory immediately.
     * @throws NullPointerException if the specified url is <code>null</code>
     * @see ILoadPolicy
     * @since JWI 2.2.0
     */
    public RAMDictionary(@NonNull URL url, int loadPolicy)
    {
        this(createBackingDictionary(url), createInputStreamFactory(url), loadPolicy);
    }

    /**
     * Constructs a new RAMDictionary that will load the contents of
     * the wrapped dictionary into memory, with the specified load policy.
     *
     * @param dict       the dictionary to be wrapped, may not be <code>null</code>
     * @param loadPolicy the load policy of the dictionary; see constants in
     *                   {@link ILoadPolicy}.
     * @see ILoadPolicy
     * @since JWI 2.2.0
     */
    public RAMDictionary(IDictionary dict, int loadPolicy)
    {
        this(dict, null, loadPolicy);
    }

    /**
     * Constructs a new RAMDictionary that will load an in-memory image from the
     * specified stream factory.
     *
     * @param factory the stream factory that provides the stream; may not be <code>null</code>
     * @throws NullPointerException if the factory is <code>null</code>
     * @since JWI 2.4.0
     */
    public RAMDictionary(IInputStreamFactory factory)
    {
        this(null, factory, ILoadPolicy.IMMEDIATE_LOAD);
    }

    /**
     * This is an internal constructor that unifies the constructor decision
     * matrix. Exactly one of the backing dictionary or the input factory must
     * be non-<code>null</code>, otherwise an exception is thrown. If the
     * factory is non-<code>null</code>, the dictionary will ignore the
     * specified load policy and set the load policy to "immediate load".
     *
     * @param backing    the backing dictionary; may be <code>null</code>
     * @param factory    the input stream factory; may be <code>null</code>
     * @param loadPolicy the load policy
     * @since JWI 2.4.0
     */
    protected RAMDictionary(@Nullable IDictionary backing, @Nullable IInputStreamFactory factory, int loadPolicy)
    {
        if (backing == null && factory == null)
        {
            throw new NullPointerException();
        }
        if (backing != null && factory != null)
        {
            throw new IllegalStateException("Both backing dictionary and input stream factory may not be non-null");
        }

        this.backing = backing;
        this.factory = factory;
        this.loadPolicy = (factory == null) ? loadPolicy : ILoadPolicy.IMMEDIATE_LOAD;
    }

    /**
     * Returns the dictionary that backs this instance.
     *
     * @return the dictionary that backs this instance; may be <code>null</code>.
     * @since JWI 2.2.0
     */
    @Nullable
    public IDictionary getBackingDictionary()
    {
        return backing;
    }

    /**
     * Returns the stream factory that backs this instance; may be
     * <code>null</code>.
     *
     * @return the stream factory that backs this instance; may be
     * <code>null</code>
     * @since JWI 2.4.0
     */
    @Nullable
    public IInputStreamFactory getStreamFactory()
    {
        return factory;
    }

    /*
     * (non-Javadoc)
     *
     * @see edu.mit.jwi.IDictionary#setCharset(java.nio.charset.Charset)
     */
    public void setCharset(Charset charset)
    {
        if (isOpen())
        {
            throw new ObjectOpenException();
        }
        assert backing != null;
        backing.setCharset(charset);
    }

    /*
     * (non-Javadoc)
     *
     * @see edu.edu.mit.jwi.data.IHasCharset#getCharset()
     */
    @Nullable
    public Charset getCharset()
    {
        return (backing == null) ? null : backing.getCharset();
    }

    /*
     * (non-Javadoc)
     *
     * @see edu.edu.mit.jwi.data.IDictionary#setComparator(edu.edu.mit.jwi.data.ContentTypeKey, edu.edu.mit.jwi.data.compare.ILineComparator)
     */
    public void setComparator(ContentTypeKey contentTypeKey, ILineComparator comparator)
    {
        if (isOpen())
        {
            throw new ObjectOpenException();
        }
        assert backing != null;
        backing.setComparator(contentTypeKey, comparator);
    }

    /*
     * (non-Javadoc)
     *
     * @see edu.edu.mit.jwi.data.IDictionary#setSourceMatcher(edu.mit.data ContentTypeKey, java.lang.String)
     */
    public void setSourceMatcher(ContentTypeKey contentTypeKey, String pattern)
    {
        if (isOpen())
        {
            throw new ObjectOpenException();
        }
        assert backing != null;
        backing.setSourceMatcher(contentTypeKey, pattern);
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
     * @see edu.edu.mit.jwi.data.ILoadPolicy#setLoadPolicy(int)
     */
    public void setLoadPolicy(int policy)
    {
        if (isOpen())
        {
            throw new ObjectOpenException();
        }
        // if the dictionary uses an input stream factory
        // the load policy is effectively IMMEDIATE_LOAD
        // so the load policy is set to this for information purposes
        this.loadPolicy = (factory == null) ? policy : ILoadPolicy.IMMEDIATE_LOAD;
    }

    /*
     * (non-Javadoc)
     *
     * @see edu.edu.mit.jwi.data.ILoadable#isLoaded()
     */
    public boolean isLoaded()
    {
        return data != null;
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
        if (loader != null)
        {
            return;
        }
        try
        {
            loadLock.lock();

            // if we are closed or in the process of closing, do nothing
            if (state == LifecycleState.CLOSED || state == LifecycleState.CLOSING)
            {
                return;
            }

            if (loader != null)
            {
                return;
            }
            loader = new Thread(new JWIBackgroundDataLoader());
            loader.setName(JWIBackgroundDataLoader.class.getSimpleName());
            loader.setDaemon(true);
            loader.start();
            if (block)
            {
                loader.join();
            }
        }
        finally
        {
            loadLock.unlock();
        }
    }

    /*
     * (non-Javadoc)
     *
     * @see edu.mit.jwi.IHasLifecycle#open()
     */
    public boolean open() throws IOException
    {
        try
        {
            lifecycleLock.lock();

            // if the dictionary is already open, return true
            if (state == LifecycleState.OPEN)
            {
                return true;
            }

            // if the dictionary is not closed, return false;
            if (state != LifecycleState.CLOSED)
            {
                return false;
            }

            // indicate the start of opening
            state = LifecycleState.OPENING;

            if (backing == null)
            {
                // behavior when loading from an
                // input stream is immediate load
                try
                {
                    load(true);
                }
                catch (InterruptedException e)
                {
                    e.printStackTrace();
                    return false;
                }
                return true;
            }
            else
            {
                // behavior when loading from a
                // backing dictionary depends on the
                // load policy
                boolean result = backing.open();
                if (result)
                {
                    try
                    {
                        switch (loadPolicy)
                        {
                            case IMMEDIATE_LOAD:
                                load(true);
                                break;
                            case BACKGROUND_LOAD:
                                load(false);
                                break;
                        }
                    }
                    catch (InterruptedException e)
                    {
                        e.printStackTrace();
                        return false;
                    }
                }
                return result;
            }
        }
        finally
        {
            // make sure to clear the opening state
            state = assertLifecycleState();
            lifecycleLock.unlock();
        }
    }

    /*
     * (non-Javadoc)
     *
     * @see edu.mit.jwi.IHasLifecycle#isOpen()
     */
    public boolean isOpen()
    {
        try
        {
            lifecycleLock.lock();
            return state == LifecycleState.OPEN;
        }
        finally
        {
            lifecycleLock.unlock();
        }
    }

    /*
     * (non-Javadoc)
     *
     * @see edu.mit.jwi.IClosable#close()
     */
    public void close()
    {
        try
        {
            lifecycleLock.lock();

            // if we are already closed, do nothing
            if (state == LifecycleState.CLOSED)
            {
                return;
            }

            // if we are already closing, do nothing
            if (state != LifecycleState.CLOSING)
            {
                return;
            }

            state = LifecycleState.CLOSING;

            // stop loading first
            if (loader != null)
            {
                loader.interrupt();
                try
                {
                    loader.join();
                }
                catch (InterruptedException e)
                {
                    e.printStackTrace();
                }
                loader = null;
            }

            // next close backing dictionary if it exists
            if (backing != null)
            {
                backing.close();
            }

            // null out backing data
            data = null;
        }
        finally
        {
            state = assertLifecycleState();
            lifecycleLock.unlock();
        }
    }

    /**
     * This is an internal utility method that determines whether this
     * dictionary should be considered open or closed.
     *
     * @return the lifecycle state object representing open if the object is
     * open; otherwise the lifecycle state object representing closed
     * @since JWI 2.4.0
     */
    @NonNull
    protected final LifecycleState assertLifecycleState()
    {
        try
        {
            lifecycleLock.lock();

            // if the data object is present, then we are open
            if (data != null)
            {
                return LifecycleState.OPEN;
            }

            // if the backing dictionary is present and open, then we are open
            if (backing != null && backing.isOpen())
            {
                return LifecycleState.OPEN;
            }

            // otherwise we are closed
            return LifecycleState.CLOSED;
        }
        finally
        {
            lifecycleLock.unlock();
        }
    }

    /*
     * (non-Javadoc)
     *
     * @see edu.mit.jwi.IRAMDictionary#export(java.io.OutputStream)
     */
    public void export(OutputStream out) throws IOException
    {
        try
        {
            loadLock.lock();
            if (!isLoaded())
            {
                throw new IllegalStateException("RAMDictionary not loaded into memory");
            }

            out = new GZIPOutputStream(out);
            out = new BufferedOutputStream(out);
            ObjectOutputStream oos = new ObjectOutputStream(out);

            oos.writeObject(data);
            oos.flush();
            oos.close();
        }
        finally
        {
            loadLock.unlock();
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
        if (backing != null)
        {
            return backing.getVersion();
        }
        if (data != null)
        {
            return data.version;
        }
        return null;
    }

    /*
     * (non-Javadoc)
     *
     * @see edu.mit.jwi.IDictionary#getIndexWord(java.lang.String, edu.edu.mit.jwi.item.POS)
     */
    @Nullable
    public IIndexWord getIndexWord(String lemma, POS pos)
    {
        return getIndexWord(new IndexWordID(lemma, pos));
    }

    /*
     * (non-Javadoc)
     *
     * @see edu.mit.jwi.IDictionary#getIndexWord(edu.edu.mit.jwi.item.IIndexWordID)
     */
    @Nullable
    public IIndexWord getIndexWord(@NonNull IIndexWordID id)
    {
        if (data != null)
        {
            Map<IIndexWordID, IIndexWord> m = data.idxWords.get(id.getPOS());
            assert m != null;
            return m.get(id);
        }
        else
        {
            assert backing != null;
            return backing.getIndexWord(id);
        }
    }

    /*
     * (non-Javadoc)
     *
     * @see edu.mit.jwi.IDictionary#getIndexWordIterator(edu.edu.mit.jwi.item.POS)
     */
    @NonNull
    public Iterator<IIndexWord> getIndexWordIterator(POS pos)
    {
        return new HotSwappableIndexWordIterator(pos);
    }

    /*
     * (non-Javadoc)
     *
     * @see edu.mit.jwi.IDictionary#getWord(edu.edu.mit.jwi.item.IWordID)
     */
    @Nullable
    public IWord getWord(@NonNull IWordID id)
    {
        if (data != null)
        {
            Map<ISynsetID, ISynset> m = data.synsets.get(id.getPOS());
            assert m != null;
            ISynset synset = m.get(id.getSynsetID());

            // no synset found
            if (synset == null)
            {
                return null;
            }

            // Fix for BUG One or the other of the WordID number or lemma may not exist,
            // depending on whence the word id came, so we have to check
            // them before trying.
            if (id.getWordNumber() > 0)
            {
                return synset.getWord(id.getWordNumber());
            }
            else if (id.getLemma() != null)
            {
                for (IWord word : synset.getWords())
                {
                    String lemma = word.getLemma();
                    assert lemma != null;
                    if (lemma.equalsIgnoreCase(id.getLemma()))
                    {
                        return word;
                    }
                }
                return null;
            }
            else
            {
                throw new IllegalArgumentException("Not enough information in IWordID instance to retrieve word.");
            }
        }
        else
        {
            assert backing != null;
            return backing.getWord(id);
        }
    }

    /*
     * (non-Javadoc)
     *
     * @see edu.mit.jwi.IDictionary#getWord(edu.edu.mit.jwi.item.ISenseKey)
     */
    @Nullable
    public IWord getWord(ISenseKey key)
    {
        if (data != null)
        {
            return data.words.get(key);
        }
        else
        {
            assert backing != null;
            return backing.getWord(key);
        }
    }

    /*
     * (non-Javadoc)
     *
     * @see edu.mit.jwi.IDictionary#getSynset(edu.edu.mit.jwi.item.ISynsetID)
     */
    @Nullable
    public ISynset getSynset(@NonNull ISynsetID id)
    {
        if (data != null)
        {
            Map<ISynsetID, ISynset> m = data.synsets.get(id.getPOS());
            assert m != null;
            return m.get(id);
        }
        else
        {
            assert backing != null;
            return backing.getSynset(id);
        }
    }

    /*
     * (non-Javadoc)
     *
     * @see edu.mit.jwi.IDictionary#getSynsetIterator(edu.edu.mit.jwi.item.POS)
     */
    @NonNull
    public Iterator<ISynset> getSynsetIterator(POS pos)
    {
        return new HotSwappableSynsetIterator(pos);
    }

    /*
     * (non-Javadoc)
     *
     * @see edu.mit.jwi.IDictionary#getSenseEntry(edu.edu.mit.jwi.item.ISenseKey)
     */
    @Nullable
    public ISenseEntry getSenseEntry(ISenseKey key)
    {
        if (data != null)
        {
            return data.senses.get(key);
        }
        else
        {
            assert backing != null;
            return backing.getSenseEntry(key);
        }
    }

    /*
     * (non-Javadoc)
     *
     * @see edu.mit.jwi.IDictionary#getSenseEntryIterator()
     */
    @NonNull
    public Iterator<ISenseEntry> getSenseEntryIterator()
    {
        return new HotSwappableSenseEntryIterator();
    }

    /*
     * (non-Javadoc)
     *
     * @see edu.mit.jwi.IDictionary#getExceptionEntry(java.lang.String, edu.edu.mit.jwi.item.POS)
     */
    @Nullable
    public IExceptionEntry getExceptionEntry(String surfaceForm, POS pos)
    {
        return getExceptionEntry(new ExceptionEntryID(surfaceForm, pos));
    }

    /*
     * (non-Javadoc)
     *
     * @see edu.mit.jwi.IDictionary#getExceptionEntry(edu.edu.mit.jwi.item.IExceptionEntryID)
     */
    @Nullable
    public IExceptionEntry getExceptionEntry(@NonNull IExceptionEntryID id)
    {
        if (data != null)
        {
            Map<IExceptionEntryID, IExceptionEntry> m = data.exceptions.get(id.getPOS());
            assert m != null;
            return m.get(id);
        }
        else
        {
            assert backing != null;
            return backing.getExceptionEntry(id);
        }
    }

    /*
     * (non-Javadoc)
     *
     * @see edu.mit.jwi.IDictionary#getExceptionEntryIterator(edu.edu.mit.jwi.item.POS)
     */
    @NonNull
    public Iterator<IExceptionEntry> getExceptionEntryIterator(POS pos)
    {
        return new HotSwappableExceptionEntryIterator(pos);
    }

    /**
     * An iterator that allows the dictionary to be loaded into memory while it
     * is iterating.
     *
     * @param <E> the element type of the iterator
     * @author Mark A. Finlayson
     * @since JWI 2.2.0
     */
    protected abstract class HotSwappableIterator<E> implements Iterator<E>
    {
        @Nullable
        private Iterator<E> itr;
        private boolean checkForLoad;
        @Nullable
        private E last = null;

        /**
         * Constructs a new hot swappable iterator.
         *
         * @param itr          the wrapped iterator
         * @param checkForLoad if <code>true</code>, on each call the iterator checks to
         *                     see if the dictionary has been loaded into memory,
         *                     switching data sources if so
         * @throws NullPointerException if the specified iterator is <code>null</code>
         * @since JWI 2.2.0
         */
        public HotSwappableIterator(@Nullable Iterator<E> itr, boolean checkForLoad)
        {
            if (itr == null)
            {
                throw new NullPointerException();
            }
            this.itr = itr;
            this.checkForLoad = checkForLoad;
        }

        /*
         * (non-Javadoc)
         *
         * @see java.util.Iterator#hasNext()
         */
        public boolean hasNext()
        {
            if (checkForLoad)
            {
                checkForLoad();
            }
            assert itr != null;
            return itr.hasNext();
        }

        /*
         * (non-Javadoc)
         *
         * @see java.util.Iterator#next()
         */
        @Nullable
        public E next()
        {
            if (checkForLoad)
            {
                checkForLoad();
                assert itr != null;
                last = itr.next();
                return last;
            }
            else
            {
                assert itr != null;
                return itr.next();
            }
        }

        /**
         * Checks to see if the data has been loaded into memory; is so,
         * replaces the original iterator with one that iterates over the
         * in-memory data structures.
         *
         * @since JWI 2.2.0
         */
        protected void checkForLoad()
        {
            if (data == null)
            {
                return;
            }
            checkForLoad = false;
            itr = makeIterator();
            if (last != null)
            {
                E consume;
                while (itr.hasNext())
                {
                    consume = itr.next();
                    if (last.equals(consume))
                    {
                        return;
                    }
                }
                throw new IllegalStateException();
            }
        }

        /**
         * Constructs the iterator that will iterate over the loaded data.
         *
         * @return the new iterator to be swapped in when loading is done
         * @since JWI 2.2.0
         */
        @NonNull
        protected abstract Iterator<E> makeIterator();

        /*
         * (non-Javadoc)
         *
         * @see java.util.Iterator#remove()
         */
        public void remove()
        {
            throw new UnsupportedOperationException();
        }
    }

    /**
     * A hot swappable iterator for index words.
     *
     * @author Mark A. Finlayson
     * @since JWI 2.2.0
     */
    protected class HotSwappableIndexWordIterator extends HotSwappableIterator<IIndexWord>
    {
        // the part of speech for this iterator
        private final POS pos;

        /**
         * Constructs a new hot swappable iterator for index words.
         *
         * @param pos the part of speech for the iterator
         * @since JWI 2.2.0
         */
        public HotSwappableIndexWordIterator(POS pos)
        {
            super((data == null) ? requireNonNull(backing).getIndexWordIterator(pos) : requireNonNull(data.idxWords.get(pos)).values().iterator(),
                    data == null);
            this.pos = pos;
        }

        /*
         * (non-Javadoc)
         *
         * @see edu.mit.jwi.RAMDictionary.HotSwappableIterator#makeIterator()
         */
        @NonNull
        @Override
        protected Iterator<IIndexWord> makeIterator()
        {
            assert data != null;
            Map<IIndexWordID, IIndexWord> m = data.idxWords.get(pos);
            assert m != null;
            return m.values().iterator();
        }
    }

    /**
     * A hot swappable iterator for synsets.
     *
     * @author Mark A. Finlayson
     * @since JWI 2.2.0
     */
    protected class HotSwappableSynsetIterator extends HotSwappableIterator<ISynset>
    {
        // the part of speech for this iterator
        private final POS pos;

        /**
         * Constructs a new hot swappable iterator for synsets.
         *
         * @param pos the part of speech for the iterator
         * @since JWI 2.2.0
         */
        public HotSwappableSynsetIterator(POS pos)
        {
            super((data == null) ? requireNonNull(backing).getSynsetIterator(pos) : requireNonNull(data.synsets.get(pos)).values().iterator(), data == null);
            this.pos = pos;
        }

        /*
         * (non-Javadoc)
         *
         * @see edu.mit.jwi.RAMDictionary.HotSwappableIterator#makeIterator()
         */
        @NonNull
        @Override
        protected Iterator<ISynset> makeIterator()
        {
            assert data != null;
            Map<ISynsetID, ISynset> m = data.synsets.get(pos);
            assert m != null;
            return m.values().iterator();
        }
    }

    /**
     * A hot swappable iterator that iterates over exceptions entries for a
     * particular part of speech.
     *
     * @author Mark A. Finlayson
     * @since JWI 2.2.0
     */
    protected class HotSwappableExceptionEntryIterator extends HotSwappableIterator<IExceptionEntry>
    {
        // the part of speech for this iterator
        private final POS pos;

        /**
         * Constructs a new hot swappable iterator that iterates over exception
         * entries for the specified part of speech.
         *
         * @param pos the part of speech for this iterator, may not be
         *            <code>null</code>
         * @throws NullPointerException if the specified part of speech is <code>null</code>
         * @since JWI 2.2.0
         */
        public HotSwappableExceptionEntryIterator(POS pos)
        {
            super((data == null) ? requireNonNull(backing).getExceptionEntryIterator(pos) : requireNonNull(data.exceptions.get(pos)).values().iterator(),
                    data == null);
            this.pos = pos;
        }

        /*
         * (non-Javadoc)
         *
         * @see edu.mit.jwi.RAMDictionary.HotSwappableIterator#makeIterator()
         */
        @NonNull
        @Override
        protected Iterator<IExceptionEntry> makeIterator()
        {
            assert data != null;
            Map<IExceptionEntryID, IExceptionEntry> m = data.exceptions.get(pos);
            assert m != null;
            return m.values().iterator();
        }
    }

    /**
     * A hot swappable iterator that iterates over sense entries.
     *
     * @author Mark A. Finlayson
     * @since JWI 2.2.0
     */
    protected class HotSwappableSenseEntryIterator extends HotSwappableIterator<ISenseEntry>
    {
        /**
         * Constructs a new hot swappable iterator that iterates over sense
         * entries.
         *
         * @throws NullPointerException if the specified part of speech is <code>null</code>
         * @since JWI 2.2.0
         */
        public HotSwappableSenseEntryIterator()
        {
            super((data == null) ? requireNonNull(backing).getSenseEntryIterator() : data.senses.values().iterator(), data == null);
        }

        /*
         * (non-Javadoc)
         *
         * @see edu.mit.jwi.RAMDictionary.HotSwappableIterator#makeIterator()
         */
        @NonNull
        @Override
        protected Iterator<ISenseEntry> makeIterator()
        {
            assert data != null;
            return data.senses.values().iterator();
        }
    }

    /**
     * This runnable loads the dictionary data into memory and sets the
     * appropriate variable in the parent dictionary.
     *
     * @author Mark A. Finlayson
     * @since JWI 2.2.0
     */
    protected class JWIBackgroundDataLoader implements Runnable
    {
        /*
         * (non-Javadoc)
         *
         * @see java.lang.Runnable#run()
         */
        public void run()
        {
            try
            {
                if (backing == null)
                {
                    // if there is no backing dictionary from
                    // which to load our data, load it from the
                    // stream factory
                    assert factory != null;
                    InputStream in = factory.makeInputStream();
                    in = new GZIPInputStream(in);
                    in = new BufferedInputStream(in);

                    // read the dictionary data
                    ObjectInputStream ois = new ObjectInputStream(in);
                    RAMDictionary.this.data = (DictionaryData) ois.readObject();
                    in.close();
                }
                else
                {
                    // here we have a backing dictionary from
                    // which we should load our data
                    DataLoader loader = new DataLoader(backing);
                    RAMDictionary.this.data = loader.call();
                    backing.close();
                }
            }
            catch (Throwable t)
            {
                if (!Thread.currentThread().isInterrupted())
                {
                    t.printStackTrace();
                    System.err.println("Unable to load dictionary data into memory");
                }
            }
        }
    }

    /**
     * A <code>Callable</code> that creates a dictionary data from a specified
     * dictionary. The data loader does not change the open state of the
     * dictionary; the dictionary for the loader must be open for the loader to
     * function without throwing an exception. The loader may be called multiple
     * times (in a thread-safe manner) as long as the dictionary is open.
     *
     * @author Mark A. Finlayson
     * @since JWI 2.2.0
     */
    public static class DataLoader implements Callable<DictionaryData>
    {
        /**
         * the source of the dictionary data
         */
        @Nullable
        private final IDictionary source;

        /**
         * Constructs a new data loader object, that uses the specified
         * dictionary to load its data.
         *
         * @param source source dictionary
         * @throws NullPointerException if the specified dictionary is <code>null</code>
         * @since JWI 2.2.0
         */
        public DataLoader(@Nullable IDictionary source)
        {
            if (source == null)
            {
                throw new NullPointerException();
            }
            this.source = source;
        }

        /*
         * (non-Javadoc)
         *
         * @see java.util.concurrent.Callable#call()
         */
        @Nullable
        public DictionaryData call()
        {
            DictionaryData result = new DictionaryData();

            result.version = source.getVersion();

            Map<IIndexWordID, IIndexWord> idxWords;
            Map<ISynsetID, ISynset> synsets;
            Map<IExceptionEntryID, IExceptionEntry> exceptions;

            IIndexWord idxWord;
            ISynset synset;
            IExceptionEntry exception;

            Thread t = Thread.currentThread();

            for (POS pos : POS.values())
            {
                // index words
                idxWords = result.idxWords.get(pos);
                for (Iterator<IIndexWord> i = source.getIndexWordIterator(pos); i.hasNext(); )
                {
                    idxWord = i.next();
                    assert idxWords != null;
                    IIndexWordID id = idxWord.getID();
                    assert id != null;
                    idxWords.put(id, idxWord);
                }
                if (t.isInterrupted())
                {
                    return null;
                }

                // synsets and words
                synsets = result.synsets.get(pos);
                assert synsets != null;
                for (Iterator<ISynset> i = source.getSynsetIterator(pos); i.hasNext(); )
                {
                    synset = i.next();
                    ISynsetID id = synset.getID();
                    assert id != null;
                    synsets.put(id, synset);
                    for (IWord word : synset.getWords())
                    {
                        result.words.put(word.getSenseKey(), word);
                    }
                }
                if (t.isInterrupted())
                {
                    return null;
                }

                // exceptions
                exceptions = result.exceptions.get(pos);
                assert exceptions != null;
                for (Iterator<IExceptionEntry> i = source.getExceptionEntryIterator(pos); i.hasNext(); )
                {
                    exception = i.next();
                    IExceptionEntryID id = exception.getID();
                    assert id != null;
                    exceptions.put(exception.getID(), exception);
                }
                if (t.isInterrupted())
                {
                    return null;
                }
            }

            // sense entries
            ISenseEntry entry;
            IWord word;
            for (Iterator<ISenseEntry> i = source.getSenseEntryIterator(); i.hasNext(); )
            {
                entry = i.next();
                word = result.words.get(entry.getSenseKey());
                if (word == null)
                {
                    throw new NullPointerException();
                }
                result.senses.put(word.getSenseKey(), makeSenseEntry(word.getSenseKey(), entry));
            }
            if (t.isInterrupted())
            {
                return null;
            }

            result.compactSize();
            if (t.isInterrupted())
            {
                return null;
            }

            result.compactObjects();
            if (t.isInterrupted())
            {
                return null;
            }
            return result;
        }

        /**
         * Creates a new sense entry that replicates the specified sense entry.
         * The new sense entry replaces its internal sense key with the
         * specified sense key thus removing a redundant object.
         *
         * @param key the sense key to be used
         * @param old the sense entry to be replicated
         * @return the new sense entry object
         * @throws NullPointerException if either argument is <code>null</code>
         * @since JWI 2.2.0
         */
        @NonNull
        protected ISenseEntry makeSenseEntry(ISenseKey key, @NonNull ISenseEntry old)
        {
            return new SenseEntry(key, old.getOffset(), old.getSenseNumber(), old.getTagCount());
        }
    }

    /**
     * Object that holds all the dictionary data loaded from the Wordnet files.
     *
     * @author Mark A. Finlayson
     * @since JWI 2.2.0
     */
    public static class DictionaryData implements Serializable
    {
        /**
         * This serial version UID identifies the last version of JWI whose
         * serialized instances of the DictionaryData class are compatible with this
         * implementation.
         *
         * @since JWI 2.4.0
         */
        private static final long serialVersionUID = 240;

        // data
        @Nullable
        protected IVersion version;
        @NonNull
        protected final Map<POS, Map<IIndexWordID, IIndexWord>> idxWords;
        @NonNull
        protected final Map<POS, Map<ISynsetID, ISynset>> synsets;
        @NonNull
        protected final Map<POS, Map<IExceptionEntryID, IExceptionEntry>> exceptions;
        protected Map<ISenseKey, IWord> words;
        protected Map<ISenseKey, ISenseEntry> senses;

        /**
         * Constructs an empty dictionary data object.
         *
         * @since JWI 2.2.0
         */
        public DictionaryData()
        {
            idxWords = makePOSMap();
            synsets = makePOSMap();
            exceptions = makePOSMap();
            words = makeMap(208000, null);
            senses = makeMap(208000, null);
        }

        /**
         * This method is used when constructing the dictionary data object.
         * Constructs a map with an empty sub-map for every part of speech.
         * Subclasses may override to change map character
         *
         * @param <K> the type of the keys for the sub-maps
         * @param <V> the type of the values for the sub-maps
         * @return a map with an empty sub-map for every part of speech.
         * @since JWI 2.2.0
         */
        @NonNull
        protected <K, V> Map<POS, Map<K, V>> makePOSMap()
        {
            Map<POS, Map<K, V>> result = new HashMap<>(POS.values().length);
            for (POS pos : POS.values())
            {
                result.put(pos, this.makeMap(4096, null));
            }
            return result;
        }

        /**
         * Creates the actual sub-maps for the part-of-speech maps. This
         * particular implementation creates <code>LinkedHashMap</code> maps.
         *
         * @param <K>         the type of the keys for the sub-maps
         * @param <V>         the type of the values for the sub-maps
         * @param initialSize the initial size of the map; this parameter is ignored if
         *                    the <code>contents</code> parameter is non-
         *                    <code>null</code>.
         * @param contents    the items to be inserted in the map, may be
         *                    <code>null</code>. If non-<code>null</code>, the
         *                    <code>initialSize</code> parameter is ignored
         * @return an empty map with either the specified initial size, or
         * contained the specified contents
         * @throws IllegalArgumentException if the initial size is invalid (less than 1) and the
         *                                  specified contents are <code>null</code>
         * @since JWI 2.2.0
         */
        @NonNull
        protected <K, V> Map<K, V> makeMap(int initialSize, @Nullable Map<K, V> contents)
        {
            return (contents == null) ? new LinkedHashMap<>(initialSize) : new LinkedHashMap<>(contents);
        }

        /**
         * Compacts this dictionary data object by resizing the internal maps,
         * and removing redundant objects where possible.
         *
         * @since JWI 2.2.0
         */
        public void compact()
        {
            compactSize();
            compactObjects();
        }

        /**
         * Resizes the internal data maps to be the exact size to contain their
         * data.
         *
         * @since JWI 2.2.0
         */
        public void compactSize()
        {
            compactPOSMap(idxWords);
            compactPOSMap(synsets);
            compactPOSMap(exceptions);
            words = compactMap(words);
            senses = compactMap(senses);
        }

        /**
         * Compacts a part-of-speech map
         *
         * @param map the part-of-speech keyed map to be compacted
         * @param <K> key type
         * @param <V> value type
         * @since JWI 2.2.0
         */
        protected <K, V> void compactPOSMap(@NonNull Map<POS, Map<K, V>> map)
        {
            for (Entry<POS, Map<K, V>> entry : map.entrySet())
            {
                entry.setValue(compactMap(entry.getValue()));
            }
        }

        /**
         * Compacts a regular map.
         *
         * @param map the map to be compacted, may not be <code>null</code>.
         * @param <K> key type
         * @param <V> value type
         * @return the new, compacted map
         * @throws NullPointerException if the specified map is <code>null</code>
         * @since JWI 2.2.0
         */
        @NonNull
        protected <K, V> Map<K, V> compactMap(@Nullable Map<K, V> map)
        {
            if (map == null)
            {
                throw new NullPointerException();
            }
            return makeMap(-1, map);
        }

        /**
         * Replaces redundant objects where possible
         *
         * @since JWI 2.2.0
         */
        public void compactObjects()
        {
            for (POS pos : POS.values())
            {
                Map<ISynsetID, ISynset> sMap = synsets.get(pos);
                assert sMap != null;
                for (Entry<ISynsetID, ISynset> entry : sMap.entrySet())
                {
                    entry.setValue(makeSynset(entry.getValue()));
                }
                Map<IIndexWordID, IIndexWord> iMap = idxWords.get(pos);
                assert iMap != null;
                for (Entry<IIndexWordID, IIndexWord> entry : iMap.entrySet())
                {
                    entry.setValue(makeIndexWord(entry.getValue()));
                }
            }
        }

        /**
         * Creates a new synset object that replaces all the old internal
         * <code>ISynsetID</code> objects with those from the denoted synsets,
         * thus throwing away redundant synset ids.
         *
         * @param old the synset to be replicated
         * @return the new synset, a copy of the first
         * @throws NullPointerException if the specified synset is <code>null</code>
         * @since JWI 2.2.0
         */
        @Nullable
        protected ISynset makeSynset(@NonNull ISynset old)
        {
            Map<IPointer, List<ISynsetID>> oldIDs = old.getRelatedMap();
            Map<IPointer, List<ISynsetID>> newIDs = new HashMap<>(oldIDs.size());

            List<ISynsetID> newList;
            ISynset otherSynset;
            for (Entry<IPointer, List<ISynsetID>> entry : oldIDs.entrySet())
            {
                newList = new ArrayList<>(entry.getValue().size());
                for (ISynsetID otherID : entry.getValue())
                {
                    Map<ISynsetID, ISynset> m = synsets.get(otherID.getPOS());
                    assert m != null;
                    otherSynset = m.get(otherID);
                    assert otherSynset != null;
                    newList.add(otherSynset.getID());
                }
                newIDs.put(entry.getKey(), newList);
            }

            // words
            List<IWord> oldWords = old.getWords();
            List<IWordBuilder> newWords = new ArrayList<>(oldWords.size());
            for (IWord oldWord : old.getWords())
            {
                newWords.add(new WordBuilder(old, oldWord));
            }
            return new Synset(old.getID(), old.getLexicalFile(), old.isAdjectiveSatellite(), old.isAdjectiveHead(), old.getGloss(), newWords, newIDs);
        }

        /**
         * Creates a new word object that replaces all the old internal
         * <code>IWordID</code> objects with those from the denoted words, thus
         * throwing away redundant word ids.
         *
         * @param newSynset the synset for which the word is being made
         * @param oldSynset the old synset from which the word should be made
         * @param old       the word to be replicated
         * @return the new synset, a copy of the first
         * @throws NullPointerException if any argument is <code>null</code>
         * @since JWI 2.2.0
         */
        @NonNull
        protected IWord makeWord(ISynset newSynset, @SuppressWarnings("unused") ISynset oldSynset, @NonNull IWord old)
        {
            Map<IPointer, List<IWordID>> oldPtrs = old.getRelatedMap();
            Map<IPointer, List<IWordID>> newPtrs = new HashMap<>(oldPtrs.size());
            List<IWordID> newList;
            ISynset otherSynset;
            for (Entry<IPointer, List<IWordID>> entry : oldPtrs.entrySet())
            {
                newList = new ArrayList<>(entry.getValue().size());
                for (IWordID otherID : entry.getValue())
                {
                    Map<ISynsetID, ISynset> m = synsets.get(otherID.getPOS());
                    assert m != null;
                    otherSynset = m.get(otherID.getSynsetID());
                    assert otherSynset != null;
                    newList.add(otherSynset.getWord(otherID.getWordNumber()).getID());
                }
                newPtrs.put(entry.getKey(), newList);
            }

            IWord word = new Word(newSynset, old.getID(), old.getLexicalID(), old.getAdjectiveMarker(), old.getVerbFrames(), newPtrs);
            ISenseKey key = word.getSenseKey();
            if (key.needsHeadSet())
            {
                ISenseKey oldKey = old.getSenseKey();
                key.setHead(oldKey.getHeadWord(), oldKey.getHeadID());
            }
            return word;
        }

        /**
         * Creates a new index word that replicates the specified index word.
         * The new index word replaces its internal synset ids with synset ids
         * from the denoted synsets, thus removing redundant ids.
         *
         * @param old the index word to be replicated
         * @return the new index word object
         * @throws NullPointerException if the specified index word is <code>null</code>
         * @since JWI 2.2.0
         */
        @NonNull
        protected IIndexWord makeIndexWord(@NonNull IIndexWord old)
        {
            List<IWordID> oldIDs = old.getWordIDs();
            IWordID[] newIDs = new IWordID[oldIDs.size()];
            IWordID oldID;
            ISynset synset;
            for (int i = 0; i < oldIDs.size(); i++)
            {
                oldID = oldIDs.get(i);
                Map<ISynsetID, ISynset> m = synsets.get(oldID.getPOS());
                assert m != null;
                synset = m.get(oldID.getSynsetID());
                assert synset != null;
                for (IWord newWord : synset.getWords())
                {
                    IWordID id = newWord.getID();
                    assert id != null;
                    if (!id.equals(oldID))
                    {
                        continue;
                    }
                    newIDs[i] = newWord.getID();
                    break;
                }
                if (newIDs[i] == null)
                {
                    throw new IllegalStateException();
                }
            }
            return new IndexWord(old.getID(), old.getTagSenseCount(), newIDs);
        }

        /**
         * A utility class that allows us to build word objects
         *
         * @author Mark A. Finlayson
         * @version 2.4.0
         * @since JWI 2.2.0
         */
        public class WordBuilder implements IWordBuilder
        {
            // final instance fields
            @Nullable
            private final ISynset oldSynset;
            @Nullable
            private final IWord oldWord;

            /**
             * Constructs a new word builder object out of the specified old
             * synset and word.
             *
             * @param oldSynset the old synset that backs this builder; may not be
             *                  <code>null</code>
             * @param oldWord   the old word that backs this builder; may not be
             *                  <code>null</code>
             * @throws NullPointerException if either argument is <code>null</code>
             * @since 2.2.0
             */
            public WordBuilder(@Nullable ISynset oldSynset, @Nullable IWord oldWord)
            {
                if (oldSynset == null)
                {
                    throw new NullPointerException();
                }
                if (oldWord == null)
                {
                    throw new NullPointerException();
                }
                this.oldSynset = oldSynset;
                this.oldWord = oldWord;
            }

            /*
             * (non-Javadoc)
             *
             * @see edu.edu.mit.jwi.item.Synset.IWordBuilder#toWord(edu.edu.mit.jwi.item.ISynset)
             */
            @NonNull
            public IWord toWord(ISynset synset)
            {
                return makeWord(synset, oldSynset, oldWord);
            }

            /*
             * (non-Javadoc)
             *
             * @see edu.edu.mit.jwi.item.Synset.IWordBuilder#addVerbFrame(edu.edu.mit.jwi.item.IVerbFrame)
             */
            public void addVerbFrame(IVerbFrame frame)
            {
                throw new UnsupportedOperationException();
            }

            /*
             * (non-Javadoc)
             *
             * @see edu.edu.mit.jwi.item.Synset.IWordBuilder#addRelatedWord(edu.edu.mit.jwi.item.IPointer, edu.edu.mit.jwi.item.IWordID)
             */
            public void addRelatedWord(IPointer ptrType, IWordID id)
            {
                throw new UnsupportedOperationException();
            }
        }
    }

    /**
     * Creates an input stream factory out of the specified File. If the file
     * points to a local directory then the method returns <code>null</code>.
     *
     * @param file the file out of which to make an input stream factory; may not
     *             be <code>null</code>
     * @return a new input stream factory, or <code>null</code> if the url
     * points to a local directory.
     * @throws NullPointerException if the specified file is <code>null</code>
     * @since JWI 2.4.0
     */
    @Nullable
    public static IInputStreamFactory createInputStreamFactory(@NonNull File file)
    {
        return FileProvider.isLocalDirectory(file) ? null : new FileInputStreamFactory(file);
    }

    /**
     * Creates an input stream factory out of the specified URL. If the url
     * points to a local directory then the method returns <code>null</code>.
     *
     * @param url the url out of which to make an input stream factory; may not
     *            be <code>null</code>
     * @return a new input stream factory, or <code>null</code> if the url
     * points to a local directory.
     * @throws NullPointerException if the specified url is <code>null</code>
     * @since JWI 2.4.0
     */
    @Nullable
    public static IInputStreamFactory createInputStreamFactory(@NonNull URL url)
    {
        return FileProvider.isLocalDirectory(url) ? null : new URLInputStreamFactory(url);
    }

    /**
     * Creates a {@link DataSourceDictionary} out of the specified file, as long
     * as the file points to an existing local directory.
     *
     * @param file the local directory for which to create a data source
     *             dictionary; may not be <code>null</code>
     * @return a dictionary object that uses the specified local directory as
     * its data source; otherwise, <code>null</code>
     * @throws NullPointerException if the specified file is <code>null</code>
     * @since JWI 2.4.0
     */
    @Nullable
    public static IDictionary createBackingDictionary(@NonNull File file)
    {
        return FileProvider.isLocalDirectory(file) ? new DataSourceDictionary(new FileProvider(file)) : null;
    }

    /**
     * Creates a {@link DataSourceDictionary} out of the specified url, as long
     * as the url points to an existing local directory.
     *
     * @param url the local directory for which to create a data source
     *            dictionary; may not be <code>null</code>
     * @return a dictionary object that uses the specified local directory as
     * its data source; otherwise, <code>null</code>
     * @throws NullPointerException if the specified url is <code>null</code>
     * @since JWI 2.4.0
     */
    @Nullable
    public static IDictionary createBackingDictionary(@NonNull URL url)
    {
        return FileProvider.isLocalDirectory(url) ? new DataSourceDictionary(new FileProvider(url)) : null;
    }

    /**
     * This is a convenience method that transforms a Wordnet dictionary at the
     * specified file location into an in-memory image written to the specified
     * output stream. The file may point to either a directory or in-memory
     * image.
     *
     * @param in  the file from which the Wordnet data should be loaded; may not
     *            be <code>null</code>
     * @param out the output stream to which the Wordnet data should be written;
     *            may not be <code>null</code>
     * @return <code>true</code> if the export was successful
     * @throws NullPointerException if either argument is <code>null</code>
     * @throws IOException          if there is an IO problem when opening or exporting the
     *                              dictionary.
     * @since JWI 2.4.0
     */
    public static boolean export(@NonNull File in, OutputStream out) throws IOException
    {
        return export(new RAMDictionary(in, ILoadPolicy.IMMEDIATE_LOAD), out);
    }

    /**
     * This is a convenience method that transforms a Wordnet dictionary at the
     * specified url location into an in-memory image written to the specified
     * output stream. The url may point to either a directory or in-memory
     * image.
     *
     * @param in  the url from which the Wordnet data should be loaded; may not
     *            be <code>null</code>
     * @param out the output stream to which the Wordnet data should be written;
     *            may not be <code>null</code>
     * @return <code>true</code> if the export was successful
     * @throws NullPointerException if either argument is <code>null</code>
     * @throws IOException          if there is an IO problem when opening or exporting the
     *                              dictionary.
     * @since JWI 2.4.0
     */
    public static boolean export(@NonNull URL in, OutputStream out) throws IOException
    {
        return export(new RAMDictionary(in, ILoadPolicy.IMMEDIATE_LOAD), out);
    }

    /**
     * This is a convenience method that transforms a Wordnet dictionary drawn
     * from the specified input stream factory into an in-memory image written to
     * the specified output stream.
     *
     * @param in  the file from which the Wordnet data should be loaded; may not
     *            be <code>null</code>
     * @param out the output stream to which the Wordnet data should be written;
     *            may not be <code>null</code>
     * @return <code>true</code> if the export was successful
     * @throws NullPointerException if either argument is <code>null</code>
     * @throws IOException          if there is an IO problem when opening or exporting the
     *                              dictionary.
     * @since JWI 2.4.0
     */
    public static boolean export(IInputStreamFactory in, OutputStream out) throws IOException
    {
        return export(new RAMDictionary(in), out);
    }

    /**
     * Exports a specified RAM Dictionary object to the specified output stream.
     * This is convenience method.
     *
     * @param dict the dictionary to be exported; the dictionary will be closed
     *             at the end of the method.
     * @param out  the output stream to which the data will be written.
     * @return <code>true</code> if the export was successful
     * @throws IOException if there was a IO problem during export
     * @since JWI 2.4.0
     */
    @SuppressWarnings("SameReturnValue")
    protected static boolean export(@NonNull IRAMDictionary dict, OutputStream out) throws IOException
    {
        // load initial data into memory
        System.out.print("Performing load...");
        dict.open();
        System.out.println("(done)");

        // export to intermediate file
        System.out.print("Performing export...");
        dict.export(out);
        dict.close();
        //noinspection UnusedAssignment
        dict = null;
        System.gc();
        System.out.println("(done)");
        return true;
    }

    /*
     * (non-Javadoc)
     *
     * @see edu.mit.jwi.IDictionary#getWords(java.lang.String, edu.edu.mit.jwi.item.POS, int)
     */
    @NonNull
    public Set<String> getWords(@NonNull String start, @Nullable POS pos, int limit)
    {
        // delegate to backing
        assert backing != null;
        return backing.getWords(start, pos, limit);
    }
}
