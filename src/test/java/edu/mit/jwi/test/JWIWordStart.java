package edu.mit.jwi.test;

import edu.mit.jwi.item.POS;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.io.OutputStream;
import java.io.PrintStream;
import java.util.List;

public class JWIWordStart {
    private static final boolean VERBOSE = !System.getProperties().containsKey("SILENT");

    private static final PrintStream PS = VERBOSE ? System.out : new PrintStream(new OutputStream() {
        public void write(int b) {
            //DO NOTHING
        }
    });

    private static JWI jwi;

    @BeforeAll
    public static void init() throws IOException {
        String wnHome = System.getProperty("SOURCE");
        jwi = new JWI(wnHome);
    }

    // enum

    @Test
    public void searchStart() {
        String start = System.getProperty("WORD");
        List<String> result = jwi.getDict().getWords(start, POS.VERB);
        System.out.println(result);
    }
}