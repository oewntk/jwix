package edu.mit.jwi.test;

import org.junit.Assert;
import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.Test;

import org.junit.jupiter.api.BeforeAll;

import java.io.IOException;

import edu.mit.jwi.item.POS;

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;

@Deprecated
@Disabled
public class Tests_XX_legacy
{
	private static JWI jwi;

	@BeforeAll
	public static void init() throws IOException
	{
		String wnHome = System.getenv("WNHOMEXX" /* + File.separator + "dict" */);
		jwi = new JWI(wnHome, JWI.Mode.__XX_LEGACY);
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

	// not all, for instance
	// generated Young%1:18:07:: does not have an entry in index.sense.legacy (young:1:18:06 is the legacy
	// sense aka as loretta_young%1:18:00::
	public void allSensekeysAreLive()
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