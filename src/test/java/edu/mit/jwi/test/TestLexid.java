package edu.mit.jwi.test;

import org.junit.jupiter.api.Test;


public class TestLexid
{
	@Test public void parseCompatLexid()
	{
		String line = "02504828 00 s 01 hot 03 001 & 02504619 a 0000 | (color) bold and intense; \"hot pink\"";
		TestLib.parseDataLineIntoMembers(line).forEach(System.out::println);
	}

	@Test public void parseNonCompatLexid()
	{
		String line = "02504828 00 s 01 hot 13 001 & 02504619 a 0000 | (color) bold and intense; \"hot pink\"";
		TestLib.parseDataLineIntoMembers(line).forEach(System.out::println);
	}
}