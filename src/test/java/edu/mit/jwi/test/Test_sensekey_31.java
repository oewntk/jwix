package edu.mit.jwi.test;

import org.junit.BeforeClass;
import org.junit.Test;

import java.io.IOException;

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

public class Test_sensekey_31
{
	private static JWI jwi;

	@BeforeClass public static void init() throws IOException
	{
		String wnHome = System.getenv("WNHOME31" /* + File.separator + "dict" */);
		jwi = new JWI(wnHome);
	}

	@Test public void sensekeysLive()
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

	@Test public void senseEntriesLive()
	{
		TestLib.allSenseEntriesAreLive(jwi);
	}

	@Test public void sensekey()
	{
		assertTrue(TestLib.sensekeyFromStringIsLive(jwi, "galore%5:00:00:abundant:00"));
		assertTrue(TestLib.sensekeyFromStringIsLive(jwi, "galore%5:00:00:many:00"));
		assertFalse(TestLib.sensekeyFromStringIsLive(jwi, "galore%5:00:01:many:00"));
		assertFalse(TestLib.sensekeyFromStringIsLive(jwi, "galore%5:00:02:many:00"));

		assertTrue(TestLib.sensekeyFromStringIsLive(jwi, "aborigine%1:18:00::"));
		assertTrue(TestLib.sensekeyFromStringIsLive(jwi, "Aborigine%1:18:00::"));
		assertTrue(TestLib.sensekeyFromStringIsLive(jwi, "aborigine%1:18:01::"));
		assertTrue(TestLib.sensekeyFromStringIsLive(jwi, "Aborigine%1:18:01::"));

		assertTrue(TestLib.sensekeyFromStringIsLive(jwi, "hot%5:00:00:warm:03"));
		assertFalse(TestLib.sensekeyFromStringIsLive(jwi, "hot%5:00:19:warm:03"));
	}
}