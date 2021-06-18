package com.oracle.truffle.st.test;

import java.io.InputStream;
import java.util.Scanner;

public class TestUtils {
    public static String readResourceAsString(InputStream res) {
        return new Scanner(res, "UTF-8").useDelimiter("\\A").next();
    }
}
