/* ******************************************************************************
 * Java Wordnet Interface Library (JWI) v2.4.0
 * Copyright (c) 2007-2015 Mark A. Finlayson
 *
 * JWI is distributed under the terms of the Creative Commons Attribution 4.0
 * International Public License, which means it may be freely used for all
 * purposes, as long as proper acknowledgment is made.  See the license file
 * included with this distribution for more details.
 *******************************************************************************/

package edu.mit.jwi.item;

import edu.mit.jwi.NonNull;
import edu.mit.jwi.Nullable;

/**
 * Concrete, default implementation of the <code>ISenseKey</code> interface.
 *
 * @author Mark A. Finlayson
 * @version 2.4.0
 * @since JWI 2.1.0
 */
public class SenseKey implements ISenseKey
{
    /**
     * This serial version UID identifies the last version of JWI whose
     * serialized instances of the SenseKey class are compatible with this
     * implementation.
     *
     * @since JWI 2.4.0
     */
    private static final long serialVersionUID = 240;

    // final instance fields
    @NonNull
    private final String lemma;
    private final int lexID;
    @Nullable
    private final POS pos;
    private final boolean isAdjSat;
    @Nullable
    private final ILexFile lexFile;

    // dynamic fields
    private boolean isHeadSet;
    @Nullable
    private String headLemma = null;
    private int headLexID = -1;
    @Nullable
    private String toString;

    /**
     * Constructs a new sense key.
     *
     * @param lemma  the lemma for the sense key
     * @param lexID  the lexical id of the sense key
     * @param synset the synset for the sense key
     * @throws NullPointerException if either the lemma or synset is <code>null</code>
     * @since JWI 2.1.0
     */
    public SenseKey(@NonNull String lemma, int lexID, @NonNull ISynset synset)
    {
        this(lemma, lexID, synset.getPOS(), synset.isAdjectiveSatellite(), synset.getLexicalFile());
    }

    /**
     * Constructs a new sense key.
     *
     * @param lemma       the lemma; may not be <code>null</code>
     * @param lexID       the lexical id
     * @param pos         the part of speech; may not be <code>null</code>
     * @param isAdjSat    <code>true</code> if this represents an adjective satellite;
     *                    <code>false</code> otherwise
     * @param lexFile     the lexical file; may not be <code>null</code>
     * @param originalKey the original key string
     * @throws NullPointerException if the lemma, lexical file, or original key is
     *                              <code>null</code>
     * @since JWI 2.1.0
     */
    public SenseKey(@NonNull String lemma, int lexID, POS pos, boolean isAdjSat, ILexFile lexFile, @Nullable String originalKey)
    {
        this(lemma, lexID, pos, isAdjSat, lexFile);
        if (originalKey == null)
        {
            throw new NullPointerException();
        }
        this.toString = originalKey;
    }

    /**
     * Constructs a new sense key.
     *
     * @param lemma       the lemma; may not be <code>null</code>
     * @param lexID       the lexical id
     * @param pos         the part of speech; may not be <code>null</code>
     * @param lexFile     the lexical file; may not be <code>null</code>
     * @param originalKey the original key string
     * @param headLemma   the head lemma
     * @param headLexID   the head lexical id; ignored if head lemma is null
     * @throws NullPointerException if the lemma, lexical file, or original key is
     *                              <code>null</code>
     * @since JWI 2.1.0
     */
    public SenseKey(@NonNull String lemma, int lexID, POS pos, ILexFile lexFile, @Nullable String headLemma, int headLexID, @Nullable String originalKey)
    {
        this(lemma, lexID, pos, (headLemma != null), lexFile);
        if (headLemma == null)
        {
            isHeadSet = true;
        }
        else
        {
            setHead(headLemma, headLexID);
        }
        if (originalKey == null)
        {
            throw new NullPointerException();
        }
        this.toString = originalKey;
    }

    /**
     * Constructs a new sense key.
     *
     * @param lemma    the lemma; may not be <code>null</code>
     * @param lexID    the lexical id
     * @param pos      the part of speech; may not be <code>null</code>
     * @param isAdjSat <code>true</code> if this is an adjective satellite sense key;
     *                 <code>false</code> otherwise
     * @param lexFile  the lexical file; may not be <code>null</code>
     * @throws NullPointerException if the lemma, part of speech, or lexical file is
     *                              <code>null</code>
     * @since JWI 2.1.0
     */
    public SenseKey(@NonNull String lemma, int lexID, @Nullable POS pos, boolean isAdjSat, @Nullable ILexFile lexFile)
    {
        if (pos == null)
        {
            throw new NullPointerException();
        }
        if (lexFile == null)
        {
            throw new NullPointerException();
        }

        // all sense key lemmas need not be in lower case
        // also checks for null
        this.lemma = lemma; //.toLowerCase();
        this.lexID = lexID;
        this.pos = pos;
        this.isAdjSat = isAdjSat;
        this.lexFile = lexFile;
        this.isHeadSet = !isAdjSat;
    }

    /*
     * (non-Javadoc)
     *
     * @see edu.edu.mit.jwi.item.ISenseKey#getLemma()
     */
    @NonNull
    public String getLemma()
    {
        return lemma;
    }

    /*
     * (non-Javadoc)
     *
     * @see edu.edu.mit.jwi.item.ISenseKey#getLexicalID()
     */
    public int getLexicalID()
    {
        return lexID;
    }

    /*
     * (non-Javadoc)
     *
     * @see edu.edu.mit.jwi.item.IHasPOS#getPOS()
     */
    @Nullable
    public POS getPOS()
    {
        return pos;
    }

    /*
     * (non-Javadoc)
     *
     * @see edu.edu.mit.jwi.item.ISenseKey#getSynsetType()
     */
    public int getSynsetType()
    {
        assert pos != null;
        return isAdjSat ? 5 : pos.getNumber();
    }

    /*
     * (non-Javadoc)
     *
     * @see edu.edu.mit.jwi.item.ISenseKey#isAdjectiveSatellite()
     */
    public boolean isAdjectiveSatellite()
    {
        return isAdjSat;
    }

    /*
     * (non-Javadoc)
     *
     * @see edu.edu.mit.jwi.item.ISenseKey#getLexicalFile()
     */
    @Nullable
    public ILexFile getLexicalFile()
    {
        return lexFile;
    }

    /*
     * (non-Javadoc)
     *
     * @see edu.edu.mit.jwi.item.ISenseKey#setHead(java.lang.String, int)
     */
    public void setHead(@NonNull String headLemma, int headLexID)
    {
        if (!needsHeadSet())
        {
            throw new IllegalStateException();
        }
        Word.checkLexicalID(headLexID);
        if (headLemma.trim().length() == 0)
        {
            throw new IllegalArgumentException();
        }
        this.headLemma = headLemma;
        this.headLexID = headLexID;
        this.isHeadSet = true;
    }

    /*
     * (non-Javadoc)
     *
     * @see edu.edu.mit.jwi.item.ISenseKey#getHeadWord()
     */
    @Nullable
    public String getHeadWord()
    {
        checkHeadSet();
        return headLemma;
    }

    /*
     * (non-Javadoc)
     *
     * @see edu.edu.mit.jwi.item.ISenseKey#getHeadID()
     */
    public int getHeadID()
    {
        checkHeadSet();
        return headLexID;
    }

    /*
     * (non-Javadoc)
     *
     * @see edu.edu.mit.jwi.item.ISenseKey#needsHeadSet()
     */
    public boolean needsHeadSet()
    {
        return !isHeadSet;
    }

    /*
     * (non-Javadoc)
     *
     * @see java.lang.Comparable#compareTo(java.lang.Object)
     */
    public int compareTo(@NonNull ISenseKey key)
    {
        int cmp;

        // first sort alphabetically by lemma
        cmp = this.getLemma().compareTo(key.getLemma());
        if (cmp != 0)
        {
            return cmp;
        }

        // then sort by synset type
        cmp = Float.compare(this.getSynsetType(), key.getSynsetType());
        if (cmp != 0)
        {
            return cmp;
        }

        // then sort by lex_filenum
        ILexFile lf = this.getLexicalFile();
        assert lf != null;
        ILexFile lf2 = key.getLexicalFile();
        assert lf2 != null;
        cmp = Float.compare(lf.getNumber(), lf2.getNumber());
        if (cmp != 0)
        {
            return cmp;
        }

        // then sort by lex_id
        cmp = Float.compare(this.getLexicalID(), key.getLexicalID());
        if (cmp != 0)
        {
            return cmp;
        }

        if (!this.isAdjectiveSatellite() && !key.isAdjectiveSatellite())
        {
            return 0;
        }
        if (!this.isAdjectiveSatellite() & key.isAdjectiveSatellite())
        {
            return -1;
        }
        if (this.isAdjectiveSatellite() & !key.isAdjectiveSatellite())
        {
            return 1;
        }

        // then sort by head_word
        String hw = this.getHeadWord();
        assert hw != null;
        String hw2 = key.getHeadWord();
        assert hw2 != null;
        cmp = hw.compareTo(hw2);
        if (cmp != 0)
        {
            return cmp;
        }

        // finally by head_id
        return Float.compare(this.getHeadID(), key.getHeadID());
    }

    /*
     * (non-Javadoc)
     *
     * @see java.lang.Object#toString()
     */
    @NonNull
    public String toString()
    {
        checkHeadSet();
        if (toString == null)
        {
            toString = toString(this);
        }
        return toString;
    }

    /**
     * Throws an exception if the head is not yet set.
     *
     * @throws IllegalArgumentException if the {@link #needsHeadSet()} method returns
     *                                  <code>true</code>.
     * @since JWI 2.2.0
     */
    protected void checkHeadSet()
    {
        if (needsHeadSet())
        {
            throw new IllegalStateException("Head word and id not yet set");
        }
    }

    /*
     * (non-Javadoc)
     *
     * @see java.lang.Object#hashCode()
     */
    @Override
    public int hashCode()
    {
        final int prime = 31;
        int result = 1;
        result = prime * result + lemma.hashCode();
        result = prime * result + lexID;
        assert pos != null;
        result = prime * result + pos.hashCode();
        assert lexFile != null;
        result = prime * result + lexFile.hashCode();
        result = prime * result + (isAdjSat ? 1231 : 1237);
        if (isAdjSat)
        {
            result = prime * result + (headLemma == null ? 0 : headLemma.hashCode());
            result = prime * result + headLexID;
        }
        return result;
    }

    /*
     * (non-Javadoc)
     *
     * @see java.lang.Object#equals(java.lang.Object)
     */
    @Override
    public boolean equals(@Nullable Object obj)
    {
        if (this == obj)
        {
            return true;
        }
        if (obj == null)
        {
            return false;
        }
        if (!(obj instanceof SenseKey))
        {
            return false;
        }
        final SenseKey other = (SenseKey) obj;
        if (!lemma.equals(other.getLemma()))
        {
            return false;
        }
        if (lexID != other.getLexicalID())
        {
            return false;
        }
        if (pos != other.getPOS())
        {
            return false;
        }
        assert other.getLexicalFile() != null;
        assert lexFile != null;
        if (lexFile.getNumber() != other.getLexicalFile().getNumber())
        {
            return false;
        }
        if (isAdjSat != other.isAdjectiveSatellite())
        {
            return false;
        }
        if (isAdjSat)
        {
            assert headLemma != null;
            if (!headLemma.equals(other.getHeadWord()))
            {
                return false;
            }
            return headLexID == other.getHeadID();
        }
        return true;
    }

    /**
     * Returns a string representation of the specified sense key object.
     *
     * @param key the sense key to be encoded as a string
     * @return the string representation of the sense key
     * @throws NullPointerException if the specified key is <code>null</code>
     * @since JWI 2.1.0
     */
    @NonNull
    public static String toString(@NonNull ISenseKey key)
    {
        ILexFile lf = key.getLexicalFile();
        assert lf != null;

        // figure out appropriate size
        int size = key.getLemma().length() + 10;
        if (key.isAdjectiveSatellite())
        {
            String hw = key.getHeadWord();
            assert hw != null;
            size += hw.length() + 2;
        }

        // allocate builder
        StringBuilder sb = new StringBuilder(size);

        // make string
        sb.append(key.getLemma()); //.toLowerCase());
        sb.append('%');
        sb.append(key.getSynsetType());
        sb.append(':');
        sb.append(LexFile.getLexicalFileNumberString(lf.getNumber()));
        sb.append(':');
        sb.append(Word.getLexicalIDForSenseKey(key.getLexicalID()));
        sb.append(':');
        if (key.isAdjectiveSatellite())
        {
            if (key.needsHeadSet())
            {
                sb.append("??");
            }
            else
            {
                sb.append(key.getHeadWord());
            }
        }
        sb.append(':');
        if (key.isAdjectiveSatellite())
        {
            if (key.needsHeadSet())
            {
                sb.append("??");
            }
            else
            {
                sb.append(Word.getLexicalIDForSenseKey(key.getHeadID()));
            }
        }
        return sb.toString();
    }
}
