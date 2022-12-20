package edu.mit.jwi.test;

import org.junit.Assert;
import org.junit.jupiter.api.Test;

import org.junit.jupiter.api.BeforeAll;

import java.io.IOException;

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;

public class Tests_31
{
	private static JWI jwi;

	@BeforeAll
	public static void init() throws IOException
	{
		String wnHome = System.getenv("WNHOME31" /* + File.separator + "dict" */);
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