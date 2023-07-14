package edu.mit.jwi.test;

import edu.mit.jwi.item.POS;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.io.OutputStream;
import java.io.PrintStream;
import java.util.List;

public class JWIWordStartTests
{
    private static final boolean VERBOSE = !System.getProperties().containsKey("SILENT");

    private static final PrintStream PS = VERBOSE ? System.out : new PrintStream(new OutputStream()
    {
        public void write(int b)
        {
            //DO NOTHING
        }
    });

    private static JWI jwi;
    final String start = System.getProperty("TARGET");
    final String scope = System.getProperty("TARGETSCOPE");
    final POS pos = POS.valueOf(scope);

    @BeforeAll
    public static void init() throws IOException
    {
        String wnHome = System.getProperty("SOURCE");
        jwi = new JWI(wnHome);
    }

    // enum

    @Test
    public void searchStart()
    {
        List<String> result = jwi.getDict().getWords(start, pos);
        PS.println(start);
        PS.println(result);
    }
}