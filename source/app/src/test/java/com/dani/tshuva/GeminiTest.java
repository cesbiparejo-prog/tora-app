package com.dani.tshuva;

import org.json.*; import org.junit.*; import java.util.*;
import static org.junit.Assert.*;

public class GeminiTest {
    @Test public void url() {
        assertEquals("https://generativelanguage.googleapis.com/v1beta/models/gemini-3.8-flash:generateContent", Gemini.url(""));
        assertEquals("https://generativelanguage.googleapis.com/v1beta/models/gemini-2.0-flash:generateContent", Gemini.url("models/gemini-2.0-flash"));
    }
    @Test public void keyInUrl() {
        assertEquals("https://generativelanguage.googleapis.com/v1beta/models/gemini-2.5-flash:generateContent?key=AQ.Ab8RN-x_y", Gemini.urlWithKey("gemini-2.5-flash", " AQ.Ab8RN-x_y "));
        assertEquals("https://generativelanguage.googleapis.com/v1beta/models/gemini-3.8-flash:generateContent?key=AIzaFAKE123", Gemini.urlWithKey("", "AIzaFAKE123"));
        String u = Gemini.urlWithKey("gemini-2.5-flash", "a+b/c=d&e f");
        assertTrue(u.endsWith("?key=a%2Bb%2Fc%3Dd%26e+f"));
    }
    static final String OK = "{\"candidates\":[{\"content\":{\"parts\":[{\"text\":\"Hola Dani\"}]}}]}";
    List<String> calls = new ArrayList<>();
    @Before public void setUp() { Gemini.backoffMs = 0; calls.clear(); }
    @After public void tearDown() { Gemini.http = Gemini::realPost; Gemini.backoffMs = 800; }
    static String modelOf(String url) { return url.substring(url.indexOf("models/") + 7, url.indexOf(":generateContent")); }
    List<String[]> one() { List<String[]> h = new ArrayList<>(); h.add(new String[]{"user", "hola"}); return h; }

    @Test public void chainOrder() {
        assertEquals(Arrays.asList("gemini-3.8-flash", "gemini-3.5-flash", "gemini-3.7-flash", "gemini-3.6-flash", "gemini-flash-latest", "gemini-3.5-flash-lite", "gemini-3.1-flash-lite"), Gemini.chain(""));
        List<String> c = Gemini.chain("models/mi-modelo"); assertEquals("mi-modelo", c.get(0)); assertEquals(8, c.size());
        assertEquals(7, Gemini.chain("gemini-3.5-flash").size());
        assertEquals("gemini-3.8-flash", Store.migrate("gemini-2.5-flash"));
        assertEquals("gemini-3.8-flash", Store.migrate(" "));
        assertEquals("gemini-3.5-flash", Store.migrate("gemini-3.5-flash"));
    }
    @Test public void fallbackOn503And404() throws Exception {
        Gemini.http = (url, header, body) -> {
            calls.add(modelOf(url) + (header != null ? "+H" : ""));
            assertTrue(header != null || url.contains("?key=AQ.test"));
            String m = modelOf(url);
            if (m.equals("gemini-2.5-flash")) return new Object[]{404, "{}"};
            if (m.equals("gemini-3.5-flash")) return new Object[]{200, OK};
            return new Object[]{503, "{}"};
        };
        assertEquals("Hola Dani", Gemini.send("AQ.test", "gemini-2.5-flash", "S", one()));
        assertEquals(Arrays.asList("gemini-2.5-flash", "gemini-2.5-flash+H", "gemini-3.8-flash", "gemini-3.5-flash"), calls);
    }
    @Test public void allFailSaturated() {
        Gemini.http = (url, header, body) -> { calls.add(url); return new Object[]{url.contains("lite") ? 500 : 503, "{}"}; };
        try { Gemini.send("k", "", "S", one()); fail(); } catch (Exception e) { assertEquals(Gemini.SATURADO, e.getMessage()); }
        assertEquals(7, calls.size());
    }
    @Test public void onlyQuota() {
        Gemini.http = (url, header, body) -> new Object[]{429, "{}"};
        try { Gemini.send("k", "", "S", one()); fail(); } catch (Exception e) { assertTrue(e.getMessage().contains("límite gratuito")); }
    }
    @Test public void badKeyStops() {
        Gemini.http = (url, header, body) -> { calls.add(url); return new Object[]{400, "{\"error\":{\"message\":\"API key not valid\"}}"}; };
        try { Gemini.send("k", "", "S", one()); fail(); } catch (Exception e) { assertTrue(e.getMessage().contains("API key not valid")); }
        assertEquals(1, calls.size());
    }
    @Test public void body() throws Exception {
        List<String[]> h = new ArrayList<>();
        h.add(new String[]{"model", "Shalom"});
        for (int i = 0; i < 30; i++) h.add(new String[]{i % 2 == 0 ? "user" : "model", "m\"sg" + i});
        JSONObject o = new JSONObject(Gemini.buildBody("SYS ñ", h));
        assertEquals("SYS ñ", o.getJSONObject("system_instruction").getJSONArray("parts").getJSONObject(0).getString("text"));
        JSONArray c = o.getJSONArray("contents");
        assertTrue(c.length() <= Gemini.MAX_TURNS);
        assertEquals("user", c.getJSONObject(0).getString("role"));
        assertEquals("m\"sg29", c.getJSONObject(c.length() - 1).getJSONArray("parts").getJSONObject(0).getString("text"));
        assertEquals(4, o.getJSONArray("safetySettings").length());
    }
    @Test public void parse() throws Exception {
        String r = "{\"candidates\":[{\"content\":{\"role\":\"model\",\"parts\":[{\"text\":\"pensando\",\"thought\":true},{\"text\":\"Hola Dani\"}]},\"finishReason\":\"STOP\"}]}";
        assertEquals("Hola Dani", Gemini.parse(r));
        try { Gemini.parse("{\"promptFeedback\":{\"blockReason\":\"SAFETY\"}}"); fail(); } catch (JSONException e) { assertTrue(e.getMessage().contains("SAFETY")); }
    }
    @Test public void memory() throws Exception {
        List<String[]> h = new ArrayList<>(); h.add(new String[]{"user", "Me cuesta de noche"}); h.add(new String[]{"model", "Te entiendo"});
        List<String[]> req = Gemini.memoryRequest("- Disparador: soledad", h);
        assertEquals(1, req.size()); assertEquals("user", req.get(0)[0]);
        assertTrue(req.get(0)[1].contains("- Disparador: soledad")); assertTrue(req.get(0)[1].contains("Dani: Me cuesta de noche")); assertTrue(req.get(0)[1].contains("Guía: Te entiendo"));
        JSONObject o = new JSONObject(Gemini.buildBody(Gemini.MEMORY_SYSTEM, req)); assertEquals(1, o.getJSONArray("contents").length());
        assertTrue(Gemini.systemWithMemory("BASE", "- dato").contains("MEMORIA DE LARGO PLAZO"));
        assertEquals("BASE", Gemini.systemWithMemory("BASE", " "));
    }
    @Test public void profile() {
        String s = Gemini.systemWithMemory(Gemini.systemWithProfile(Gemini.SYSTEM_PROMPT, "Músico de 35"), "- dato");
        assertTrue(s.contains("SOBRE DANI")); assertTrue(s.contains("Músico de 35")); assertTrue(s.contains("TIPP")); assertTrue(s.contains("Melajim II 3:15"));
        assertEquals("BASE", Gemini.systemWithProfile("BASE", ""));
        assertTrue(Messages.ALL.length >= 80);
    }
    @Test public void crisis() { assertTrue(Gemini.isCrisis("A veces pienso en Suicidarme")); assertFalse(Gemini.isCrisis("hoy estuve bien")); }
    @Test public void messages() { assertTrue(Messages.ALL.length >= 60); for (String[] m : Messages.ALL) { assertEquals(4, m.length); assertTrue(m[0].matches("^@?[\\u0590-\\u05FF].*")); } }
}
