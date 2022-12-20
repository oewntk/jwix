package edu.mit.jwi.test;

import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.Test;

import org.junit.jupiter.api.BeforeAll;

import java.io.IOException;

import edu.mit.jwi.data.parse.SenseKeyParser;
import edu.mit.jwi.data.parse.SenseLineParser;
import edu.mit.jwi.data.parse.SensesLineParser;
import edu.mit.jwi.item.ISenseEntry;
import edu.mit.jwi.item.ISenseKey;

import static org.junit.Assert.*;

@Deprecated
@Disabled
public class Test_sensekey_XX_pools
{
	private static JWI jwi;

	@BeforeAll
	public static void init() throws IOException
	{
		String wnHome = System.getenv("WNHOMEXX" /* + File.separator + "dict" */);
		jwi = new JWI(wnHome, JWI.Mode.__XX_POOLS);
	}

	@Test
	public void sensekeysLive()
	{
		try
		{
			TestLib.allSensekeysAreLive(jwi);
		}
		catch (AssertionError ae)
		{
			TestLib.listDeadSensekeys(jwi);
			throw ae;
		}
	}

	@Test
	public void senseEntriesLive()
	{
		TestLib.allSenseEntriesAreLive(jwi);
	}

	@Test
	public void senseEntryPoolsLive()
	{
		TestLib.allSenseEntryPoolsAreLive(jwi);
	}

	@Test
	public void senseEntriesFromPoolsAreLive()
	{
		TestLib.allSenseEntriesFromPoolsAreLive(jwi);
	}

	@Test
	public void sensekey()
	{
//		assertTrue(TestLib.sensekeyFromStringIsLive(jwi, "galore%5:00:01:abundant:00"));
//		assertTrue(TestLib.sensekeyFromStringIsLive(jwi, "galore%5:00:02:many:00"));
		assertTrue(TestLib.sensekeyFromStringIsLive(jwi, "galore%5:00:01:many:00"));
		assertTrue(TestLib.sensekeyFromStringIsLive(jwi, "galore%5:00:02:abundant:00"));
		assertFalse(TestLib.sensekeyFromStringIsLive(jwi, "galore%5:00:00:many:00"));

		assertTrue(TestLib.sensekeyFromStringIsLive(jwi, "aborigine%1:18:00::"));
		assertTrue(TestLib.sensekeyFromStringIsLive(jwi, "Aborigine%1:18:00::"));
		assertFalse(TestLib.sensekeyFromStringIsLive(jwi, "aborigine%1:18:01::"));
		assertFalse(TestLib.sensekeyFromStringIsLive(jwi, "Aborigine%1:18:01::"));

//		assertTrue(TestLib.sensekeyFromStringIsLive(jwi, "hot%5:00:19:warm:03"));
//		assertFalse(TestLib.sensekeyFromStringIsLive(jwi, "hot%5:00:02:warm:03"));
		assertTrue(TestLib.sensekeyFromStringIsLive(jwi, "hot%5:00:02:warm:03"));
		assertFalse(TestLib.sensekeyFromStringIsLive(jwi, "hot%5:00:03:warm:03"));
	}

	@Test
	public void multiplePools()
	{
		jwi.forAllSenseEntryPools((ses) -> {
			assertNotNull(ses);
			if (ses.length > 1)
			{
				ISenseKey sk = ses[0].getSenseKey();
				assert sk != null;
				System.out.printf("● pool:%s%n", sk.toString());
			}
		});
	}

	@Test
	public void multipleSamePools()
	{
		jwi.forAllSenseEntryPools((ses) -> {
			assertNotNull(ses);
			if (ses.length > 1)
			{
				ISenseKey sk = ses[0].getSenseKey();
				assert sk != null;
				int ofs1 = ses[0].getOffset();
				int ofs2 = ses[1].getOffset();
				int sn1 = ses[0].getSenseNumber();
				int sn2 = ses[1].getSenseNumber();
				int tc1 = ses[0].getTagCount();
				int tc2 = ses[1].getTagCount();
				if (ofs1 == ofs2 && sn1 == sn2 && tc1 == tc2)
					System.out.printf("● pool:%s - %d %d %d - %d %d %d%n", sk.toString(), ofs1, sn1, tc1, ofs2, sn2, tc2);
			}
		});
	}

	@Test
	public void multipleDiffPools()
	{
		jwi.forAllSenseEntryPools((ses) -> {
			assertNotNull(ses);
			if (ses.length > 1)
			{
				ISenseKey sk = ses[0].getSenseKey();
				assert sk != null;
				int ofs1 = ses[0].getOffset();
				int ofs2 = ses[1].getOffset();
				int sn1 = ses[0].getSenseNumber();
				int sn2 = ses[1].getSenseNumber();
				int tc1 = ses[0].getTagCount();
				int tc2 = ses[1].getTagCount();
				if (ofs1 != ofs2 || sn1 != sn2 || tc1 != tc2)
					System.out.printf("● pool:%s - %d %d %d - %d %d %d%n", sk.toString(), ofs1, sn1, tc1, ofs2, sn2, tc2);
			}
		});
	}

	@Test
	public void pool()
	{
		String skStr = "aborigine%1:18:00::";
		ISenseKey sk = SenseKeyParser.getInstance().parseLine(skStr);
		assert sk != null;
		assertEquals(sk.toString(), skStr);
		System.out.printf("● sensekey %s%n", sk);
		ISenseEntry[] senseEntries = jwi.getDict().getSenseEntries(sk);
		assert senseEntries != null;
		for (ISenseEntry se : senseEntries)
		{
			System.out.printf(". sense entry %d %d %d%n", se.getOffset(), se.getSenseNumber(), se.getTagCount());
		}
	}

	@Test
	public void parse()
	{
		SenseLineParser singleParser = SenseLineParser.getInstance();
		SensesLineParser multipleParser = SensesLineParser.getInstance();
		String skStr = "aborigine%1:18:00::";
		String e1 = "09628141 0 1";
		String e2 = "09680125 1 0";
		String e3 = "77777777 8 9";
		String line = skStr + ' ' + e1;
		String line2 = line + ' ' + e2;
		String line3 = line2 + ' ' + e3;

		ISenseEntry[] ses = { singleParser.parseLine(line), singleParser.parseLine(line + " "), singleParser.parseLine(line2), singleParser.parseLine(line3), };
		for (ISenseEntry se : ses)
		{
			ISenseKey sk = se.getSenseKey();
			assertNotNull(sk);
			assertEquals(skStr, sk.toString());
			assertEquals(9628141, se.getOffset());
			assertEquals(0, se.getSenseNumber());
			assertEquals(1, se.getTagCount());
		}

		ISenseEntry[] ses1 = multipleParser.parseLine(line);
		ISenseEntry[] ses10 = multipleParser.parseLine(line + " ");
		ISenseEntry[] ses100 = multipleParser.parseLine(line + "    ");
		ISenseEntry[] ses2 = multipleParser.parseLine(line2);
		ISenseEntry[] ses3 = multipleParser.parseLine(line3);
		assertNotNull(ses1);
		assertNotNull(ses10);
		assertNotNull(ses100);
		assertNotNull(ses2);
		assertNotNull(ses3);
		assertEquals(1, ses1.length);
		assertEquals(1, ses10.length);
		assertEquals(1, ses100.length);
		assertEquals(2, ses2.length);
		assertEquals(3, ses3.length);

		for (ISenseEntry[] pool : new ISenseEntry[][] { ses1, ses10, ses100, ses2, ses3 })
		{
			assertNotNull(pool[0]);
			ISenseKey sk = pool[0].getSenseKey();
			assertNotNull(sk);
			assertEquals(skStr, sk.toString());
			assertEquals(9628141, pool[0].getOffset());
			assertEquals(0, pool[0].getSenseNumber());
			assertEquals(1, pool[0].getTagCount());
		}

		for (ISenseEntry[] pool : new ISenseEntry[][] { ses2, ses3 })
		{
			assertNotNull(pool[1]);
			ISenseKey sk = pool[1].getSenseKey();
			assertNotNull(sk);
			assertEquals(skStr, sk.toString());
			assertEquals(9680125, pool[1].getOffset());
			assertEquals(1, pool[1].getSenseNumber());
			assertEquals(0, pool[1].getTagCount());
		}

		for (ISenseEntry[] pool : new ISenseEntry[][] { ses3 })
		{
			assertNotNull(pool[2]);
			ISenseKey sk = pool[2].getSenseKey();
			assertNotNull(sk);
			assertEquals(skStr, sk.toString());
			assertEquals(77777777, pool[2].getOffset());
			assertEquals(8, pool[2].getSenseNumber());
			assertEquals(9, pool[2].getTagCount());
		}
	}
}