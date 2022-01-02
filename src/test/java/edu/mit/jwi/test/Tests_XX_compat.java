package edu.mit.jwi.test;

import edu.mit.jwi.item.POS;
import org.junit.Assert;
import org.junit.BeforeClass;
import org.junit.Test;

import java.io.IOException;

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;

public class Tests_XX_compat
{
	private static JWI jwi;

	@BeforeClass public static void init() throws IOException
	{
		String wnHome = System.getenv("WNHOMEXX_compat" /* + File.separator + "dict" */);
		jwi = new JWI(wnHome, JWI.Mode.XX);
	}

	// enum

	@Test public void allSenses()
	{
		jwi.forAllSenses(null);
	}

	@Test public void allSynsets()
	{
		jwi.forAllSynsets(null);
	}

	@Test public void allSenseEntries()
	{
		jwi.forAllSenseEntries(null);
	}

	// enum non null

	@Test public void allSensesAreNonNull()
	{
		jwi.forAllSenses(Assert::assertNotNull);
	}

	@Test public void allSynsetsAreNonNull()
	{
		jwi.forAllSynsets(Assert::assertNotNull);
	}

	@Test public void allSenseEntriesAreNonNull()
	{
		jwi.forAllSenseEntries(Assert::assertNotNull);
	}

	@Test public void allLemmasAreNonNull()
	{
		jwi.forAllLemmas((l) -> {
			assertNotNull(l);
			assertFalse(l.isEmpty());
		});
	}

	@Test public void allSensekeysAreNonNull()
	{
		jwi.forAllSensekeys(Assert::assertNotNull);
	}

	@Test public void allSynsetRelationsAreNonNull()
	{
		jwi.forAllSynsetRelations(Assert::assertNotNull);
	}

	@Test public void allSenseRelationsAreNonNull()
	{
		jwi.forAllSenseRelations(Assert::assertNotNull);
	}

	// enum live

	@Test public void allSensekeysAreLive()
	{
		try
		{
			TestLib.allSensekeysAreLive(jwi);
		}
		catch(AssertionError ae)
		{
			TestLib.listDeadSensekeys(jwi);
			throw ae;
		}
	}

	@Test public void allSenseEntriesAreLive()
	{
		TestLib.allSenseEntriesAreLive(jwi);
	}

	// others

	// the test involves new is_caused_by
	@Test public void extraRelations()
	{
		jwi.walk("spread");
	}

	// the test involves Young (n) and adj
	@Test public void cased()
	{
		jwi.walk("young");
	}

	// the test involves new is_caused_by
	@Test public void cased2()
	{
		jwi.walk("aborigine");
	}

	// the test involves adj
	@Test public void adjSat()
	{
		jwi.walk("small");
	}

	// the test involves galore (a)
	@Test public void adjMarker()
	{
		jwi.walk("galore");
	}

	// the test involves a frameless entry
	@Test public void frameless()
	{
		jwi.getDict().getIndexWord("fangirl", POS.VERB);
		jwi.walk("fangirl", POS.VERB);
	}
}