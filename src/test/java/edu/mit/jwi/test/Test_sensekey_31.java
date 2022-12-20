package edu.mit.jwi.test;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

import org.junit.jupiter.api.BeforeAll;

import java.io.IOException;

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

public class Test_sensekey_31
{
	private static JWI jwi;

	@BeforeAll
	public static void init() throws IOException
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
		Assertions.assertTrue(TestLib.sensekeyFromStringIsLive(jwi, "galore%5:00:00:abundant:00"));
		Assertions.assertTrue(TestLib.sensekeyFromStringIsLive(jwi, "galore%5:00:00:many:00"));

		Assertions.assertTrue(TestLib.sensekeyFromStringIsLive(jwi, "aborigine%1:18:00::"));
		Assertions.assertTrue(TestLib.sensekeyFromStringIsLive(jwi, "aborigine%1:18:01::"));
		Assertions.assertTrue(TestLib.sensekeyFromStringIsLive(jwi, "Aborigine%1:18:00::"));
		Assertions.assertTrue(TestLib.sensekeyFromStringIsLive(jwi, "Aborigine%1:18:01::"));

		Assertions.assertTrue(TestLib.sensekeyFromStringIsLive(jwi, "hot%3:00:01::"));
		Assertions.assertTrue(TestLib.sensekeyFromStringIsLive(jwi, "hot%3:00:02::"));
		Assertions.assertTrue(TestLib.sensekeyFromStringIsLive(jwi, "hot%5:00:00:active:01"));
		Assertions.assertTrue(TestLib.sensekeyFromStringIsLive(jwi, "hot%5:00:00:charged:00"));
		Assertions.assertTrue(TestLib.sensekeyFromStringIsLive(jwi, "hot%5:00:00:eager:00"));
		Assertions.assertTrue(TestLib.sensekeyFromStringIsLive(jwi, "hot%5:00:00:fast:01"));
		Assertions.assertTrue(TestLib.sensekeyFromStringIsLive(jwi, "hot%5:00:00:fresh:01"));
		Assertions.assertTrue(TestLib.sensekeyFromStringIsLive(jwi, "hot%5:00:00:good:01"));
		Assertions.assertTrue(TestLib.sensekeyFromStringIsLive(jwi, "hot%5:00:00:illegal:00"));
		Assertions.assertTrue(TestLib.sensekeyFromStringIsLive(jwi, "hot%5:00:00:lucky:00"));
		Assertions.assertTrue(TestLib.sensekeyFromStringIsLive(jwi, "hot%5:00:00:near:00"));
		Assertions.assertTrue(TestLib.sensekeyFromStringIsLive(jwi, "hot%5:00:00:new:00"));
		Assertions.assertTrue(TestLib.sensekeyFromStringIsLive(jwi, "hot%5:00:00:popular:00"));
		Assertions.assertTrue(TestLib.sensekeyFromStringIsLive(jwi, "hot%5:00:00:radioactive:00"));
		Assertions.assertTrue(TestLib.sensekeyFromStringIsLive(jwi, "hot%5:00:00:sexy:00"));
		Assertions.assertTrue(TestLib.sensekeyFromStringIsLive(jwi, "hot%5:00:00:skilled:00"));
		Assertions.assertTrue(TestLib.sensekeyFromStringIsLive(jwi, "hot%5:00:00:tasty:00"));
		Assertions.assertTrue(TestLib.sensekeyFromStringIsLive(jwi, "hot%5:00:00:unpleasant:00"));
		Assertions.assertTrue(TestLib.sensekeyFromStringIsLive(jwi, "hot%5:00:00:violent:00"));
		Assertions.assertTrue(TestLib.sensekeyFromStringIsLive(jwi, "hot%5:00:00:wanted:00"));
	}
}