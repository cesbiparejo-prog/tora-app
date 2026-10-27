package com.dani.tshuva;

import android.content.*; import java.security.*; import java.security.spec.*; import javax.crypto.*; import javax.crypto.spec.*;

/** PIN guardado como PBKDF2-SHA256 con sal aleatoria. Nunca se guarda el PIN en claro. */
public class Lock {
    static final long TIMEOUT_MS = 60_000;
    static boolean unlocked;
    static final int ITER = 20000;
    static SharedPreferences p(Context c) { return c.getSharedPreferences("candado", Context.MODE_PRIVATE); }
    static boolean hasPin(Context c) { return p(c).contains("hash"); }
    static boolean biometricOn(Context c) { return p(c).getBoolean("huella", true); }
    public static boolean validFormat(String pin) { return pin != null && pin.matches("\\d{4,6}"); }

    public static String hash(String pin, byte[] salt) {
        try {
            SecretKeyFactory f = SecretKeyFactory.getInstance("PBKDF2WithHmacSHA256");
            byte[] h = f.generateSecret(new PBEKeySpec(pin.toCharArray(), salt, ITER, 256)).getEncoded();
            return hex(h);
        } catch (GeneralSecurityException e) { throw new RuntimeException(e); }
    }
    public static String hex(byte[] b) { StringBuilder s = new StringBuilder(); for (byte x : b) s.append(String.format("%02x", x)); return s.toString(); }
    static byte[] unhex(String s) { byte[] b = new byte[s.length() / 2]; for (int i = 0; i < b.length; i++) b[i] = (byte) Integer.parseInt(s.substring(2 * i, 2 * i + 2), 16); return b; }
    public static boolean slowEquals(String a, String b) { return MessageDigest.isEqual(a.getBytes(), b.getBytes()); }

    static void setPin(Context c, String pin) {
        byte[] salt = new byte[16]; new SecureRandom().nextBytes(salt);
        p(c).edit().putString("salt", hex(salt)).putString("hash", hash(pin, salt)).apply();
    }
    static boolean check(Context c, String pin) {
        if (!hasPin(c) || !validFormat(pin)) return false;
        return slowEquals(hash(pin, unhex(p(c).getString("salt", ""))), p(c).getString("hash", ""));
    }
}
