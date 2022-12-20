package edu.mit.jwi.test;

import org.junit.jupiter.api.Test;

import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Disabled;

import java.io.IOException;
import java.util.List;

import edu.mit.jwi.item.*;

import static org.junit.Assert.assertTrue;

@Deprecated
@Disabled
public class Test_nonascii_XX
{
	private static JWI jwi;

	@BeforeAll
	public static void init() throws IOException
	{
		String wnHome = System.getenv("WNHOMEXX_contrib" /* + File.separator + "dict" */);
		jwi = new JWI(wnHome);
	}

	@Test public void nonascii()
	{
		final IIndexWord idx = jwi.getDict().getIndexWord("Wałęsa", POS.NOUN);
		assert idx != null;
		final List<IWordID> senseids = idx.getWordIDs();
		for (final IWordID senseid : senseids) // synset id, sense number, and lemma
		{
			// sense
			final IWord sense = jwi.getDict().getWord(senseid);
			assert sense != null;
			System.out.println("● sense = " + sense.toString() + " sensekey=" + sense.getSenseKey());

			// synset
			final ISynsetID synsetid = senseid.getSynsetID();
			final ISynset synset = jwi.getDict().getSynset(synsetid);
			assert synset != null;
			final String members = JWI.getMembers(synset);
			System.out.println("● synset = " + members + synset.getGloss());
			assertTrue(members.contains("Wałęsa"));
			assertTrue(members.contains("Lech_Wałęsa"));
			assertTrue(members.contains("Walesa"));
			assertTrue(members.contains("Lech_Walesa"));
		}
	}
}