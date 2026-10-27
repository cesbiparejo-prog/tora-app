package com.dani.tshuva;

import org.junit.*; import static org.junit.Assert.*;

public class LockTest {
    @Test public void format() {
        assertTrue(Lock.validFormat("1234")); assertTrue(Lock.validFormat("123456"));
        assertFalse(Lock.validFormat("123")); assertFalse(Lock.validFormat("1234567")); assertFalse(Lock.validFormat("12a4")); assertFalse(Lock.validFormat(null));
    }
    @Test public void hashing() {
        byte[] s1 = {1,2,3,4,5,6,7,8,9,10,11,12,13,14,15,16}, s2 = {16,15,14,13,12,11,10,9,8,7,6,5,4,3,2,1};
        String h = Lock.hash("2580", s1);
        assertEquals(64, h.length()); assertEquals(h, Lock.hash("2580", s1));
        assertNotEquals(h, Lock.hash("2580", s2)); assertNotEquals(h, Lock.hash("2581", s1));
        assertFalse(h.contains("2580"));
        assertTrue(Lock.slowEquals(h, Lock.hash("2580", s1)));
    }
}
