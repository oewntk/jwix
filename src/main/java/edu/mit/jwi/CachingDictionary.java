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
import edu.mit.jwi.data.compare.ILineComparator;
import edu.mit.jwi.item.*;

import java.io.IOException;
import java.nio.charset.Charset;
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReentrantLock;

/**
 * A dictionary that caches the results of another dictionary
 *
 * @author Mark A. Finlayson
 * @version 2.4.0
 * @since JWI 2.2.0
 */
public class CachingDictionary implements ICachingDictionary
{
    // final instance fields
    @Nullable
    private final IDictionary backing;
    @NonNull
    private final IItemCache cache;

    /**
     * Constructs a new caching dictionary that caches the results of the
     * specified backing dictionary
     *
     * @param backing the dictionary whose results should be cached
     * @since JWI 2.2.0
     */
    public CachingDictionary(@Nullable IDictionary backing)
    {
        if (backing == null)
        {
            throw new NullPointerException();
        }
        this.cache = createCache();
        this.backing = backing;
    }

    /**
     * Returns the dictionary that is wrapped by this dictionary; will never
     * return <code>null</code>
     *
     * @return the dictionary that is wrapped by this dictionary
     * @since JWI 2.2.0
     */
    @Nullable
    public IDictionary getBackingDictionary()
    {
        return backing;
    }

    /**
     * This operation creates the cache that is used by the dictionary. It is
     * set inside its own method for ease of subclassing. It is called only
     * when an instance of this class is created. It is marked protected for
     * ease of subclassing.
     *
     * @return the item cache to be used by this dictionary
     * @since JWI 2.2.0
     */
    @NonNull
    @SuppressWarnings("WeakerAccess")
    protected IItemCache createCache()
    {
        return new ItemCache();
    }

    /**
     * An internal method for assuring compliance with the dictionary interface
     * that says that methods will throw {@code ObjectClosedException}s if
     * the dictionary has not yet been opened.
     *
     * @throws ObjectClosedException if the dictionary is closed.
     * @since JWI 2.2.0
     */
    @SuppressWarnings("WeakerAccess")
    protected void checkOpen()
    {
        if (isOpen())
        {
            if (!getCache().isOpen())
            {
                try
                {
                    getCache().open();
                }
                catch (IOException e)
                {
                    throw new ObjectClosedException(e);
                }
            }
        }
        else
        {
            if (getCache().isOpen())
            {
                getCache().close();
            }
            throw new ObjectClosedException();
        }
    }

    /*
     * (non-Javadoc)
     *
     * @see edu.mit.jwi.ICachingDictionary#getCache()
     */
    @NonNull
    public IItemCache getCache()
    {
        return cache;
    }

    /*
     * (non-Javadoc)
     *
     * @see edu.mit.jwi.IDictionary#setCharset(java.nio.charset.Charset)
     */
    public void setCharset(Charset charset)
    {
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
        assert backing != null;
        return backing.getCharset();
    }

    /*
     * (non-Javadoc)
     *
     * @see edu.edu.mit.jwi.data.IDictionary#setComparator(edu.edu.mit.jwi.data.ContentTypeKey, edu.edu.mit.jwi.data.compare.ILineComparator)
     */
    public void setComparator(ContentTypeKey contentType, ILineComparator comparator)
    {
        assert backing != null;
        backing.setComparator(contentType, comparator);
    }

    /*
     * (non-Javadoc)
     *
     * @see edu.edu.mit.jwi.data.IDictionary#setSourceMatcher(edu.mit.data ContentTypeKey, java.lang.String)
     */
    public void setSourceMatcher(ContentTypeKey contentTypeKey, String pattern)
    {
        assert backing != null;
        backing.setSourceMatcher(contentTypeKey, pattern);
    }

    /*
     * (non-Javadoc)
     *
     * @see edu.mit.jwi.IHasLifecycle#open()
     */
    public boolean open() throws IOException
    {
        if (isOpen())
        {
            return true;
        }
        cache.open();
        assert backing != null;
        return backing.open();
    }

    /*
     * (non-Javadoc)
     *
     * @see edu.mit.jwi.IHasLifecycle#isOpen()
     */
    public boolean isOpen()
    {
        assert backing != null;
        return backing.isOpen();
    }

    /*
     * (non-Javadoc)
     *
     * @see edu.mit.jwi.IClosable#close()
     */
    public void close()
    {
        if (!isOpen())
        {
            return;
        }
        getCache().close();
        assert backing != null;
        backing.close();
    }

    /*
     * (non-Javadoc)
     *
     * @see edu.edu.mit.jwi.item.IHasVersion#getVersion()
     */
    @Nullable
    public IVersion getVersion()
    {
        assert backing != null;
        return backing.getVersion();
    }

    /*
     * (non-Javadoc)
     *
     * @see edu.mit.jwi.IDictionary#getIndexWord(java.lang.String, edu.edu.mit.jwi.item.POS)
     */
    @Nullable
    public IIndexWord getIndexWord(String lemma, POS pos)
    {
        checkOpen();
        IIndexWordID id = new IndexWordID(lemma, pos);
        IIndexWord item = getCache().retrieveItem(id);
        if (item == null)
        {
            assert backing != null;
            item = backing.getIndexWord(id);
            if (item != null)
            {
                getCache().cacheItem(item);
            }
        }
        return item;
    }

    /*
     * (non-Javadoc)
     *
     * @see edu.mit.jwi.IDictionary#getIndexWord(edu.edu.mit.jwi.item.IIndexWordID)
     */
    @Nullable
    public IIndexWord getIndexWord(IIndexWordID id)
    {
        checkOpen();
        IIndexWord item = getCache().retrieveItem(id);
        if (item == null)
        {
            assert backing != null;
            item = backing.getIndexWord(id);
            if (item != null)
            {
                getCache().cacheItem(item);
            }
        }
        return item;
    }

    /*
     * (non-Javadoc)
     *
     * @see edu.mit.jwi.IDictionary#getIndexWordIterator(edu.edu.mit.jwi.item.POS)
     */
    public Iterator<IIndexWord> getIndexWordIterator(POS pos)
    {
        assert backing != null;
        return backing.getIndexWordIterator(pos);
    }

    /*
     * (non-Javadoc)
     *
     * @see edu.mit.jwi.IDictionary#getWord(edu.edu.mit.jwi.item.IWordID)
     */
    @Nullable
    public IWord getWord(IWordID id)
    {
        checkOpen();
        IWord item = getCache().retrieveItem(id);
        if (item == null)
        {
            assert backing != null;
            item = backing.getWord(id);
            if (item != null)
            {
                Synset s = (Synset) item.getSynset();
                assert s != null;
                cacheSynset(s);
            }
        }
        return item;
    }

    /*
     * (non-Javadoc)
     *
     * @see edu.mit.jwi.IDictionary#getWord(edu.edu.mit.jwi.item.ISenseKey)
     */
    @Nullable
    public IWord getWord(ISenseKey key)
    {
        checkOpen();
        IWord item = getCache().retrieveWord(key);
        if (item == null)
        {
            assert backing != null;
            item = backing.getWord(key);
            if (item != null)
            {
                Synset s = (Synset) item.getSynset();
                assert s != null;
                cacheSynset(s);
            }
        }
        return item;
    }

    /*
     * (non-Javadoc)
     *
     * @see edu.mit.jwi.IDictionary#getSynset(edu.edu.mit.jwi.item.ISynsetID)
     */
    @Nullable
    public ISynset getSynset(ISynsetID id)
    {
        checkOpen();
        ISynset item = getCache().retrieveItem(id);
        if (item == null)
        {
            assert backing != null;
            item = backing.getSynset(id);
            if (item != null)
            {
                cacheSynset(item);
            }
        }
        return item;
    }

    /**
     * Caches the specified synset and its words.
     *
     * @param synset the synset to be cached; may not be <code>null</code>
     * @throws NullPointerException if the specified synset is <code>null</code>
     * @since JWI 2.2.0
     */
    @SuppressWarnings("WeakerAccess")
    protected void cacheSynset(@NonNull ISynset synset)
    {
        IItemCache cache = getCache();
        cache.cacheItem(synset);
        for (IWord word : synset.getWords())
        {
            cache.cacheItem(word);
            cache.cacheWordByKey(word);
        }
    }

    /*
     * (non-Javadoc)
     *
     * @see edu.mit.jwi.IDictionary#getSynsetIterator(edu.edu.mit.jwi.item.POS)
     */
    public Iterator<ISynset> getSynsetIterator(POS pos)
    {
        assert backing != null;
        return backing.getSynsetIterator(pos);
    }

    /*
     * (non-Javadoc)
     *
     * @see edu.mit.jwi.IDictionary#getSenseEntry(edu.edu.mit.jwi.item.ISenseKey)
     */
    @Nullable
    public ISenseEntry getSenseEntry(ISenseKey key)
    {
        checkOpen();
        ISenseEntry entry = getCache().retrieveSenseEntry(key);
        if (entry == null)
        {
            assert backing != null;
            entry = backing.getSenseEntry(key);
            if (entry != null)
            {
                getCache().cacheSenseEntry(entry);
            }
        }
        return entry;
    }

    /*
     * (non-Javadoc)
     *
     * @see edu.mit.jwi.IDictionary#getSenseEntryIterator()
     */
    public Iterator<ISenseEntry> getSenseEntryIterator()
    {
        assert backing != null;
        return backing.getSenseEntryIterator();
    }

    /*
     * (non-Javadoc)
     *
     * @see edu.mit.jwi.IDictionary#getExceptionEntry(java.lang.String, edu.edu.mit.jwi.item.POS)
     */
    @Nullable
    public IExceptionEntry getExceptionEntry(String surfaceForm, POS pos)
    {
        checkOpen();
        IExceptionEntryID id = new ExceptionEntryID(surfaceForm, pos);
        IExceptionEntry item = getCache().retrieveItem(id);
        if (item == null)
        {
            assert backing != null;
            item = backing.getExceptionEntry(id);
            if (item != null)
            {
                getCache().cacheItem(item);
            }
        }
        return item;
    }

    /*
     * (non-Javadoc)
     *
     * @see edu.mit.jwi.IDictionary#getExceptionEntry(edu.edu.mit.jwi.item.IExceptionEntryID)
     */
    @Nullable
    public IExceptionEntry getExceptionEntry(IExceptionEntryID id)
    {
        checkOpen();
        IExceptionEntry item = getCache().retrieveItem(id);
        if (item == null)
        {
            assert backing != null;
            item = backing.getExceptionEntry(id);
            if (item != null)
            {
                getCache().cacheItem(item);
            }
        }
        return item;
    }

    /*
     * (non-Javadoc)
     *
     * @see edu.mit.jwi.IDictionary#getExceptionEntryIterator(edu.edu.mit.jwi.item.POS)
     */
    public Iterator<IExceptionEntry> getExceptionEntryIterator(POS pos)
    {
        assert backing != null;
        return backing.getExceptionEntryIterator(pos);
    }

    /**
     * An LRU cache for objects in JWI.
     *
     * @author Mark A. Finlayson
     * @version 2.4.0
     * @since JWI 2.2.0
     */
    public static class ItemCache implements IItemCache
    {
        // default configuration
        public static final int DEFAULT_INITIAL_CAPACITY = 16;
        public static final int DEFAULT_MAXIMUM_CAPACITY = 512;
        public static final float DEFAULT_LOAD_FACTOR = 0.75f;

        protected final Lock lifecycleLock = new ReentrantLock();

        /**
         * Flag that records whether caching is enabled for this dictionary. Default
         * starting state is <code>true</code>.
         */
        private boolean isEnabled = true;

        /**
         * Initial capacity of the caches. If this is set to less than one, then
         * the system default is used.
         */
        private int initialCapacity;

        /**
         * Maximum capacity of the caches. If this is set to less than one, then
         * the cache size is unlimited.
         */
        private int maximumCapacity;

        // The caches themselves
        @Nullable
        protected Map<IItemID<?>, IItem<?>> itemCache;
        @Nullable
        protected Map<ISenseKey, IWord> keyCache;
        @Nullable
        protected Map<ISenseKey, ISenseEntry> senseCache;
        @Nullable
        protected Map<ISenseKey, ISenseEntry[]> sensesCache;

        /**
         * Default constructor that initializes the dictionary with caching enabled.
         *
         * @since JWI 2.2.0
         */
        public ItemCache()
        {
            this(DEFAULT_INITIAL_CAPACITY, DEFAULT_MAXIMUM_CAPACITY, true);
        }

        /**
         * Caller can specify both the initial size, maximum size, and the
         * initial state of caching.
         *
         * @param initialCapacity the initial capacity of the cache
         * @param maxCapacity     the maximum capacity of the cache
         * @param enabled         whether the cache starts out enabled
         * @since JWI 2.2.0
         */
        public ItemCache(int initialCapacity, int maxCapacity, boolean enabled)
        {
            setInitialCapacity(initialCapacity);
            setMaximumCapacity(maxCapacity);
            setEnabled(enabled);
        }

        /*
         * (non-Javadoc)
         *
         * @see edu.edu.mit.jwi.data.IHasLifecycle#open()
         */
        public boolean open()
        {
            if (isOpen())
            {
                return true;
            }
            try
            {
                lifecycleLock.lock();
                int capacity = (initialCapacity < 1) ? DEFAULT_INITIAL_CAPACITY : initialCapacity;
                itemCache = this.makeCache(capacity);
                keyCache = this.makeCache(capacity);
                senseCache = this.makeCache(capacity);
                sensesCache = this.makeCache(capacity);
            }
            finally
            {
                lifecycleLock.unlock();
            }
            return true;
        }

        /**
         * Creates the map that backs this cache.
         *
         * @param <K>             the key type
         * @param <V>             the value type
         * @param initialCapacity the initial capacity
         * @return the new map
         * @since JWI 2.2.0
         */
        @NonNull
        protected <K, V> Map<K, V> makeCache(int initialCapacity)
        {
            return new LinkedHashMap<>(initialCapacity, DEFAULT_LOAD_FACTOR, true);
        }

        /*
         * (non-Javadoc)
         *
         * @see edu.edu.mit.jwi.data.IHasLifecycle#isOpen()
         */
        public boolean isOpen()
        {
            return senseCache != null && sensesCache != null;
        }

        /**
         * An internal method for assuring compliance with the dictionary
         * interface that says that methods will throw
         * {@code ObjectClosedException}s if the dictionary has not yet been
         * opened.
         *
         * @throws ObjectClosedException if the dictionary is closed.
         * @since JWI 2.2.0
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
         * @see edu.edu.mit.jwi.data.IClosable#close()
         */
        public void close()
        {
            if (!isOpen())
            {
                return;
            }
            try
            {
                lifecycleLock.lock();
                itemCache = null;
                keyCache = null;
                senseCache = null;
            }
            finally
            {
                lifecycleLock.unlock();
            }
        }

        /*
         * (non-Javadoc)
         *
         * @see edu.mit.jwi.ICachingDictionary.IItemCache#clear()
         */
        public void clear()
        {
            if (itemCache != null)
            {
                itemCache.clear();
            }
            if (keyCache != null)
            {
                keyCache.clear();
            }
            if (senseCache != null)
            {
                senseCache.clear();
            }
            if (sensesCache != null)
            {
                sensesCache.clear();
            }
        }

        /*
         * (non-Javadoc)
         *
         * @see edu.mit.jwi.ICachingDictionary.IItemCache#isEnabled()
         */
        public boolean isEnabled()
        {
            return isEnabled;
        }

        /*
         * (non-Javadoc)
         *
         * @see edu.mit.jwi.ICachingDictionary.IItemCache#setEnabled(boolean)
         */
        public void setEnabled(boolean isEnabled)
        {
            this.isEnabled = isEnabled;
        }

        /**
         * Returns the initial capacity of this cache.
         *
         * @return the initial capacity of this cache.
         * @since JWI 2.2.0
         */
        public int getInitialCapacity()
        {
            return initialCapacity;
        }

        /**
         * Sets the initial capacity of the cache
         *
         * @param capacity the initial capacity
         * @since JWI 2.2.0
         */
        public void setInitialCapacity(int capacity)
        {
            initialCapacity = capacity < 1 ? DEFAULT_INITIAL_CAPACITY : capacity;
        }

        /*
         * (non-Javadoc)
         *
         * @see edu.mit.jwi.ICachingDictionary.IItemCache#getMaximumCapacity()
         */
        public int getMaximumCapacity()
        {
            return maximumCapacity;
        }

        /*
         * (non-Javadoc)
         *
         * @see edu.mit.jwi.ICachingDictionary.IItemCache#setMaximumCapacity(int)
         */
        public void setMaximumCapacity(int capacity)
        {
            int oldCapacity = maximumCapacity;
            maximumCapacity = capacity;
            if (maximumCapacity < 1 || oldCapacity <= maximumCapacity)
            {
                return;
            }
            assert itemCache != null;
            reduceCacheSize(itemCache);
            assert keyCache != null;
            reduceCacheSize(keyCache);
            assert senseCache != null;
            reduceCacheSize(senseCache);
            assert sensesCache != null;
            reduceCacheSize(sensesCache);
        }

        /*
         * (non-Javadoc)
         *
         * @see edu.mit.jwi.ICachingDictionary.IItemCache#size()
         */
        public int size()
        {
            checkOpen();
            assert keyCache != null;
            assert itemCache != null;
            assert senseCache != null;
            assert sensesCache != null;
            return itemCache.size() + keyCache.size() + senseCache.size() + sensesCache.size();
        }

        /*
         * (non-Javadoc)
         *
         * @see edu.mit.jwi.ICachingDictionary.IItemCache#cacheItem(edu.edu.mit.jwi.item.IItem)
         */
        public void cacheItem(@NonNull IItem<?> item)
        {
            checkOpen();
            if (!isEnabled())
            {
                return;
            }

            IItemID<?> id = item.getID();
            assert id != null;
            assert itemCache != null;
            itemCache.put(id, item);
            reduceCacheSize(itemCache);
        }

        /*
         * (non-Javadoc)
         *
         * @see edu.mit.jwi.ICachingDictionary.IItemCache#cacheWordByKey(edu.edu.mit.jwi.item.IWord)
         */
        public void cacheWordByKey(@NonNull IWord word)
        {
            checkOpen();
            if (!isEnabled())
            {
                return;
            }
            assert keyCache != null;
            keyCache.put(word.getSenseKey(), word);
            reduceCacheSize(keyCache);
        }

        /*
         * (non-Javadoc)
         *
         * @see edu.mit.jwi.ICachingDictionary.IItemCache#cacheSenseEntry(edu.edu.mit.jwi.item.ISenseEntry)
         */
        public void cacheSenseEntry(@NonNull ISenseEntry entry)
        {
            checkOpen();
            if (!isEnabled())
            {
                return;
            }
            ISenseKey sk = entry.getSenseKey();
            assert sk != null;
            assert senseCache != null;
            senseCache.put(sk, entry);
            reduceCacheSize(senseCache);
        }

        private final Object cacheLock = new Object();

        /**
         * Brings the map size into line with the specified maximum capacity of
         * this cache.
         *
         * @param cache the map to be trimmed
         * @since JWI 2.2.0
         */
        protected void reduceCacheSize(@NonNull Map<?, ?> cache)
        {
            if (!isOpen() || maximumCapacity < 1 || cache.size() < maximumCapacity)
            {
                return;
            }
            synchronized (cacheLock)
            {
                int remove = cache.size() - maximumCapacity;
                Iterator<?> itr = cache.keySet().iterator();
                for (int i = 0; i <= remove; i++)
                {
                    if (itr.hasNext())
                    {
                        itr.next();
                        itr.remove();
                    }
                }
            }
        }

        /*
         * (non-Javadoc)
         *
         * @see edu.mit.jwi.ICachingDictionary.IItemCache#retrieveItem(edu.edu.mit.jwi.item.IItemID)
         */
        @Nullable
        public <T extends IItem<D>, D extends IItemID<T>> T retrieveItem(D id)
        {
            checkOpen();
            assert itemCache != null;
            //noinspection unchecked
            return (T) itemCache.get(id);
        }

        /*
         * (non-Javadoc)
         *
         * @see edu.mit.jwi.ICachingDictionary.IItemCache#retrieveWord(edu.edu.mit.jwi.item.ISenseKey)
         */
        @Nullable
        public IWord retrieveWord(ISenseKey key)
        {
            checkOpen();
            assert keyCache != null;
            return keyCache.get(key);
        }

        /*
         * (non-Javadoc)
         *
         * @see edu.mit.jwi.ICachingDictionary.IItemCache#retrieveSenseEntry(edu.edu.mit.jwi.item.ISenseKey)
         */
        @Nullable
        public ISenseEntry retrieveSenseEntry(ISenseKey key)
        {
            checkOpen();
            assert senseCache != null;
            return senseCache.get(key);
        }
    }

    /*
     * (non-Javadoc)
     *
     * @see edu.mit.jwi.IDictionary#getWords(java.lang.String, edu.edu.mit.jwi.item.POS, int)
     */
    @NonNull
    public Set<String> getWords(@NonNull String start, @Nullable POS pos, int limit)
    {
        checkOpen();
        assert backing != null;
        return backing.getWords(start, pos, limit);
    }
}
