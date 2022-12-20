package edu.mit.jwi.test;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

import org.junit.jupiter.api.BeforeAll;

import java.io.IOException;

public class TestsWn30
{
	private static JWI jwi;

	@BeforeAll
	public static void init() throws IOException
	{
		String wnHome = System.getenv("WNHOME30" /* + File.separator + "dict" */);
		jwi = new JWI(wnHome);
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
		jwi.forAllSenses(Assertions::assertNotNull);
	}

	@Test public void allSynsetsAreNonNull()
	{
		jwi.forAllSynsets(Assertions::assertNotNull);
	}

	@Test public void allSenseEntriesAreNonNull()
	{
		jwi.forAllSenseEntries(Assertions::assertNotNull);
	}

	@Test public void allLemmasAreNonNull()
	{
		jwi.forAllLemmas((l) -> {
			Assertions.assertNotNull(l);
			Assertions.assertFalse(l.isEmpty());
		});
	}

	@Test public void allSensekeysAreNonNull()
	{
		jwi.forAllSensekeys(Assertions::assertNotNull);
	}

	@Test public void allSynsetRelationsAreNonNull()
	{
		jwi.forAllSynsetRelations(Assertions::assertNotNull);
	}

	@Test public void allSenseRelationsAreNonNull()
	{
		jwi.forAllSenseRelations(Assertions::assertNotNull);
	}

	// enum live

	@Test public void allSensekeysAreLive()
	{
		TestLib.allSensekeysAreLive(jwi);
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
}