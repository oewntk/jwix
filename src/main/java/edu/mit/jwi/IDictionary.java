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
import edu.mit.jwi.data.IHasCharset;
import edu.mit.jwi.data.IHasLifecycle;
import edu.mit.jwi.data.compare.ILineComparator;
import edu.mit.jwi.item.*;
import edu.mit.jwi.morph.IStemmer;

import java.nio.charset.Charset;
import java.util.Iterator;
import java.util.Set;

/**
 * Objects that implement this interface are intended as the main entry point to
 * accessing Wordnet data. The dictionary must be opened by calling
 * {@code open()} before it is used, otherwise its methods throw an
 * {@link IllegalStateException}.
 *
 * @author Mark A. Finlayson
 * @version 2.4.0
 * @since JWI 1.0
 */
public interface IDictionary extends IHasVersion, IHasLifecycle, IHasCharset
{
    /**
     * Sets the character set associated with this dictionary. The character set
     * may be <code>null</code>.
     *
     * @param charset the possibly <code>null</code> character set to use when
     *                decoding files.
     * @since JWI 2.3.4
     */
    void setCharset(@Nullable Charset charset);

    /**
     * Sets the comparator associated with this content type in this dictionary.
     * The comparator may be <code>null</code> in which case it is reset.
     *
     * @param contentTypeKey the <code>non-null</code> content type for which
     *                       the comparator is to be set.
     * @param comparator     the possibly <code>null</code> comparator set to use when
     *                       decoding files.
     * @throws IllegalStateException if the provider is currently open
     * @since JWI 2.4.1
     */
    void setComparator(ContentTypeKey contentTypeKey, @Nullable ILineComparator comparator);

    /**
     * Sets pattern attached to content type key, that source files have to
     * match to be selected.
     * This gives selection a first opportunity before falling back on standard data
     * type selection.
     *
     * @param contentTypeKey the <code>non-null</code> content type key for which
     *                       the matcher is to be set.
     * @param pattern        regexp pattern
     * @since JWI 2.4.1
     */
    void setSourceMatcher(ContentTypeKey contentTypeKey, @Nullable String pattern);

    /**
     * This method is identical to <code>getIndexWord(IIndexWordID)</code> and
     * is provided as a convenience.
     *
     * @param lemma the lemma for the index word requested; may not be
     *              <code>null</code>, empty, or all whitespace
     * @param pos   the part of speech; may not be <code>null</code>
     * @return the index word corresponding to the specified lemma and part of
     * speech, or <code>null</code> if none is found
     * @throws NullPointerException     if either argument is <code>null</code>
     * @throws IllegalArgumentException if the specified lemma is empty or all whitespace
     * @since JWI 1.0
     */
    @Nullable
    IIndexWord getIndexWord(String lemma, POS pos);

    /**
     * Retrieves the specified index word object from the database. If the
     * specified lemma/part of speech combination is not found, returns
     * {@code null}.
     * <p>
     * <i>Note:</i> This call does no stemming on the specified lemma, it is
     * taken as specified. That is, if you submit the word "dogs", it will
     * search for "dogs", not "dog"; in the standard Wordnet distribution, there
     * is no entry for "dogs" and therefore the call will return
     * <code>null</code>. This is in contrast to the Wordnet API provided by
     * Princeton. If you want your searches to capture morphological variation,
     * use the descendants of the {@link IStemmer} class.
     *
     * @param id the id of the index word to search for; may not be
     *           <code>null</code>
     * @return the index word, if found; <code>null</code> otherwise
     * @throws NullPointerException if the argument is <code>null</code>
     * @since JWI 1.0
     */
    @Nullable
    IIndexWord getIndexWord(IIndexWordID id);

    /**
     * Returns an iterator that will iterate over all index words of the
     * specified part of speech.
     *
     * @param pos the part of speech over which to iterate; may not be
     *            <code>null</code>
     * @return an iterator that will iterate over all index words of the
     * specified part of speech
     * @throws NullPointerException if the argument is <code>null</code>
     * @since JWI 1.0
     */
    Iterator<IIndexWord> getIndexWordIterator(POS pos);

    /**
     * Retrieves the word with the specified id from the database. If the
     * specified word is not found, returns {@code null}
     *
     * @param id the id of the word to search for; may not be <code>null</code>
     * @return the word, if found; <code>null</code> otherwise
     * @throws NullPointerException if the argument is <code>null</code>
     * @since JWI 1.0
     */
    @Nullable
    IWord getWord(IWordID id);

    /**
     * Retrieves the word with the specified sense key from the database. If the
     * specified word is not found, returns {@code null}
     *
     * @param key the sense key of the word to search for; may not be
     *            <code>null</code>
     * @return the word, if found; <code>null</code> otherwise
     * @throws NullPointerException if the argument is <code>null</code>
     * @since JWI 1.0
     */
    @Nullable
    IWord getWord(ISenseKey key);

    /**
     * Retrieves the synset with the specified id from the database. If the
     * specified synset is not found, returns {@code null}
     *
     * @param id the id of the synset to search for; may not be
     *           <code>null</code>
     * @return the synset, if found; <code>null</code> otherwise
     * @throws NullPointerException if the argument is <code>null</code>
     * @since JWI 1.0
     */
    @Nullable
    ISynset getSynset(ISynsetID id);

    /**
     * Returns an iterator that will iterate over all synsets of the specified
     * part of speech.
     *
     * @param pos the part of speech over which to iterate; may not be
     *            <code>null</code>
     * @return an iterator that will iterate over all synsets of the specified
     * part of speech
     * @throws NullPointerException if the argument is <code>null</code>
     * @since JWI 1.0
     */
    Iterator<ISynset> getSynsetIterator(POS pos);

    /**
     * Retrieves the sense entry for the specified sense key from the database.
     * If the specified sense key has no associated sense entry, returns
     * {@code null}
     *
     * @param key the sense key of the entry to search for; may not be
     *            <code>null</code>
     * @return the entry, if found; <code>null</code> otherwise
     * @throws NullPointerException if the argument is <code>null</code>
     * @since JWI 1.0
     */
    @Nullable
    ISenseEntry getSenseEntry(ISenseKey key);

    /**
     * Returns an iterator that will iterate over all sense entries in the
     * dictionary.
     *
     * @return an iterator that will iterate over all sense entries
     * @since JWI 1.0
     */
    Iterator<ISenseEntry> getSenseEntryIterator();

    /**
     * Retrieves the exception entry for the specified surface form and part of
     * speech from the database. If the specified surface form/ part of speech
     * pair has no associated exception entry, returns {@code null}
     *
     * @param surfaceForm the surface form to be looked up; may not be <code>null</code>
     *                    , empty, or all whitespace
     * @param pos         the part of speech; may not be <code>null</code>
     * @return the entry, if found; <code>null</code> otherwise
     * @throws NullPointerException     if either argument is <code>null</code>
     * @throws IllegalArgumentException if the specified surface form is empty or all whitespace
     * @since JWI 1.0
     */
    @Nullable
    IExceptionEntry getExceptionEntry(String surfaceForm, POS pos);

    /**
     * Retrieves the exception entry for the specified id from the database. If
     * the specified id is not found, returns <code>null</code>
     *
     * @param id the exception entry id of the entry to search for; may not be
     *           <code>null</code>
     * @return the exception entry for the specified id
     * @since JWI 1.1
     */
    @Nullable
    IExceptionEntry getExceptionEntry(IExceptionEntryID id);

    /**
     * Returns an iterator that will iterate over all exception entries of the
     * specified part of speech.
     *
     * @param pos the part of speech over which to iterate; may not be
     *            <code>null</code>
     * @return an iterator that will iterate over all exception entries of the
     * specified part of speech
     * @throws NullPointerException if the argument is <code>null</code>
     * @since JWI 1.0
     */
    Iterator<IExceptionEntry> getExceptionEntryIterator(POS pos);

    /**
     * Returns list of lemmas that have the given start.
     *
     * @param start start of lemmas searched for; may not be
     *              <code>null</code>
     * @param pos   the part of speech over which to iterate; may be
     *              <code>null</code>, in which case it ignores pos
     * @param limit maximum number of results, 0 for no limit
     * @return a set of lemmas in dictionary that have given start
     * @throws NullPointerException if the argument is <code>null</code>
     * @since JWIX 2.4.0.4
     */
    @NonNull
    Set<String> getWords(@NonNull String start, @Nullable POS pos, int limit);
}
