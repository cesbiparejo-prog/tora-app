package com.dani.tshuva;

import org.json.*;
import java.io.*; import java.net.*; import java.nio.charset.StandardCharsets; import java.util.*;

/** Cliente mínimo para la API de Gemini (generateContent). */
public class Gemini {
    public static final String DEFAULT_MODEL = "gemini-3.8-flash";
    public static final int MAX_TURNS = 20;

    public static String url(String model) {
        String m = (model == null || model.trim().isEmpty()) ? DEFAULT_MODEL : model.trim();
        if (m.startsWith("models/")) m = m.substring(7);
        return "https://generativelanguage.googleapis.com/v1beta/models/" + m + ":generateContent";
    }

    /** history: lista de {role("user"|"model"), text}. Envía solo los últimos MAX_TURNS. */
    public static String buildBody(String system, List<String[]> history) throws JSONException {
        JSONObject root = new JSONObject();
        root.put("system_instruction", new JSONObject().put("parts", new JSONArray().put(new JSONObject().put("text", system))));
        JSONArray contents = new JSONArray();
        int start = Math.max(0, history.size() - MAX_TURNS);
        // la API exige empezar con un turno de usuario
        while (start < history.size() && !"user".equals(history.get(start)[0])) start++;
        for (int i = start; i < history.size(); i++) {
            String[] h = history.get(i);
            contents.put(new JSONObject().put("role", "model".equals(h[0]) ? "model" : "user")
                .put("parts", new JSONArray().put(new JSONObject().put("text", h[1]))));
        }
        root.put("contents", contents);
        root.put("generationConfig", new JSONObject().put("temperature", 0.7).put("maxOutputTokens", 2048));
        JSONArray safety = new JSONArray();
        for (String c : new String[]{"HARM_CATEGORY_HARASSMENT", "HARM_CATEGORY_HATE_SPEECH", "HARM_CATEGORY_SEXUALLY_EXPLICIT", "HARM_CATEGORY_DANGEROUS_CONTENT"})
            safety.put(new JSONObject().put("category", c).put("threshold", "BLOCK_ONLY_HIGH"));
        root.put("safetySettings", safety);
        return root.toString();
    }

    public static String parse(String json) throws JSONException {
        JSONObject o = new JSONObject(json);
        if (o.has("error")) throw new JSONException(o.getJSONObject("error").optString("message", "Error de la API"));
        JSONArray c = o.optJSONArray("candidates");
        if (c == null || c.length() == 0) {
            JSONObject pf = o.optJSONObject("promptFeedback");
            throw new JSONException("Google no devolvió respuesta" + (pf != null ? " (bloqueado: " + pf.optString("blockReason") + ")" : ""));
        }
        JSONObject content = c.getJSONObject(0).optJSONObject("content");
        StringBuilder sb = new StringBuilder();
        if (content != null && content.optJSONArray("parts") != null) {
            JSONArray parts = content.getJSONArray("parts");
            for (int i = 0; i < parts.length(); i++) {
                JSONObject p = parts.getJSONObject(i);
                if (!p.optBoolean("thought", false)) sb.append(p.optString("text", ""));
            }
        }
        if (sb.length() == 0) throw new JSONException("Respuesta vacía (" + c.getJSONObject(0).optString("finishReason") + ")");
        return sb.toString().trim();
    }

    /** URL con la clave como parámetro ?key= (codificada). Funciona con claves AIza… y AQ.… */
    public static String urlWithKey(String model, String apiKey) {
        try { return url(model) + "?key=" + URLEncoder.encode(apiKey.trim(), "UTF-8"); }
        catch (UnsupportedEncodingException e) { throw new RuntimeException(e); }
    }

    public static final String[] FALLBACK = {"gemini-3.8-flash", "gemini-3.5-flash", "gemini-3.7-flash", "gemini-3.6-flash", "gemini-flash-latest", "gemini-3.5-flash-lite", "gemini-3.1-flash-lite"};
    public static final String SATURADO = "Google está saturado, intenta en unos minutos.";

    /** Modelos a probar en orden: el elegido por el usuario y luego la cadena de respaldo, sin repetir. */
    public static List<String> chain(String model) {
        LinkedHashSet<String> l = new LinkedHashSet<>();
        String m = model == null ? "" : model.trim();
        if (m.startsWith("models/")) m = m.substring(7);
        if (!m.isEmpty()) l.add(m);
        l.addAll(Arrays.asList(FALLBACK));
        return new ArrayList<>(l);
    }
    static boolean retryable(int code) { return code == 404 || code == 429 || code == 500 || code == 503; }

    /** Transporte HTTP (reemplazable en las pruebas). Devuelve {código, cuerpo}. */
    public interface Http { Object[] post(String address, String headerKey, String body) throws IOException; }
    public static Http http = Gemini::realPost;
    public static long backoffMs = 800;
    static void sleep(long ms) { if (ms <= 0) return; try { Thread.sleep(ms); } catch (InterruptedException e) { Thread.currentThread().interrupt(); } }

    static Object[] realPost(String address, String headerKey, String body) throws IOException {
        HttpURLConnection con = (HttpURLConnection) new URL(address).openConnection();
        con.setRequestMethod("POST"); con.setDoOutput(true);
        con.setConnectTimeout(20000); con.setReadTimeout(90000);
        con.setRequestProperty("Content-Type", "application/json; charset=utf-8");
        if (headerKey != null) con.setRequestProperty("x-goog-api-key", headerKey);
        try (OutputStream os = con.getOutputStream()) { os.write(body.getBytes(StandardCharsets.UTF_8)); }
        int code = con.getResponseCode();
        return new Object[]{code, read(code >= 400 ? con.getErrorStream() : con.getInputStream())};
    }

    /** Llamada bloqueante (fuera del hilo principal). Prueba la cadena de modelos; clave por ?key= y, ante 404, una vez por cabecera. */
    public static synchronized String send(String apiKey, String model, String system, List<String[]> history) throws Exception {
        String body = buildBody(system, history);
        List<String> modelos = chain(model);
        int intento = 0; boolean soloLimite = true;
        for (String m : modelos) {
            if (intento > 0) sleep(Math.min(backoffMs * intento, 3 * backoffMs));
            intento++;
            Object[] r = http.post(urlWithKey(m, apiKey), null, body);
            int code = (Integer) r[0];
            if (code == 404) { r = http.post(url(m), apiKey.trim(), body); code = (Integer) r[0]; }
            String resp = (String) r[1];
            if (code < 400) return parse(resp);
            if (code == 400 || code == 401 || code == 403) {
                String msg = ""; try { msg = new JSONObject(resp).getJSONObject("error").optString("message"); } catch (Exception ignored) {}
                throw new IOException("Clave no válida o sin permiso (" + code + "). Revisa tu clave en Ajustes. " + msg);
            }
            if (!retryable(code)) throw new IOException("Error " + code + " del servidor de Google.");
            if (code != 429) soloLimite = false;
        }
        if (soloLimite) throw new IOException("Llegaste al límite gratuito de Google por ahora. Espera un rato (o hasta mañana) y vuelve a intentar.");
        throw new IOException(SATURADO);
    }


    static String read(InputStream is) throws IOException {
        if (is == null) return "";
        ByteArrayOutputStream b = new ByteArrayOutputStream(); byte[] buf = new byte[8192]; int n;
        while ((n = is.read(buf)) > 0) b.write(buf, 0, n);
        return b.toString("UTF-8");
    }

    public static final int MEMORY_EVERY = 6;   // actualizar memoria cada 6 mensajes nuevos
    public static final String MEMORY_SYSTEM =
        "Eres un asistente que mantiene una MEMORIA breve y privada sobre el camino de recuperación de Dani (adicción a la pornografía, acompañamiento con Torá y psicología). " +
        "Recibirás la memoria actual y la conversación reciente. Devuelve SOLO la memoria actualizada, en español, en viñetas cortas, máximo 200 palabras, con estas secciones: " +
        "Disparadores, Progreso y recaídas (con fechas si se mencionan), Lo que le ayuda, Metas y compromisos, Apoyos (terapeuta, rabino, grupo), Otros datos importantes. " +
        "Conserva lo importante de la memoria anterior, corrige lo que cambió, y no inventes nada que no esté en el texto. No incluyas detalles sexuales explícitos ni datos identificativos (apellidos, direcciones, teléfonos).";

    /** Texto que se envía para actualizar la memoria. */
    public static List<String[]> memoryRequest(String memory, List<String[]> history) {
        StringBuilder sb = new StringBuilder("MEMORIA ACTUAL:\n").append(memory == null || memory.trim().isEmpty() ? "(vacía)" : memory.trim()).append("\n\nCONVERSACIÓN RECIENTE:\n");
        int start = Math.max(0, history.size() - MAX_TURNS);
        for (int i = start; i < history.size(); i++) sb.append("user".equals(history.get(i)[0]) ? "Dani: " : "Guía: ").append(history.get(i)[1]).append("\n");
        sb.append("\nDevuelve la memoria actualizada.");
        List<String[]> l = new ArrayList<>(); l.add(new String[]{"user", sb.toString()}); return l;
    }

    public static String systemWithProfile(String base, String profile) {
        if (profile == null || profile.trim().isEmpty()) return base;
        return base + "\n\nSOBRE DANI (escrito por él; tenlo siempre presente):\n" + profile.trim();
    }

    public static String systemWithMemory(String base, String memory) {
        if (memory == null || memory.trim().isEmpty()) return base;
        return base + "\n\nMEMORIA DE LARGO PLAZO (resumen privado de conversaciones anteriores con Dani; úsala con naturalidad para dar continuidad, sin recitarla):\n" + memory.trim();
    }

    static final String[] CRISIS = {"suicid", "matarme", "quitarme la vida", "no quiero vivir", "quiero morir", "me quiero morir", "acabar con todo", "hacerme daño", "lastimarme", "cortarme", "no vale la pena vivir"};
    public static boolean isCrisis(String t) {
        String s = t.toLowerCase(new Locale("es"));
        for (String k : CRISIS) if (s.contains(k)) return true;
        return false;
    }

    public static final String SYSTEM_PROMPT =
        "Eres \"Guía de Teshuvá\", un acompañante diario en español para Dani, un hombre judío (o con raíces hebreas) que quiere liberarse de la adicción a la pornografía. " +
        "Combinas dos fuentes: (1) la sabiduría de la Torá y la tradición judía: teshuvá, yetzer hará y yetzer tov, shmirat einaim, kedushá, Tehilim, Rambam (Mishné Torá, Hiljot Teshuvá), Pirkei Avot, Talmud, Tanya (el beinoni, moaj shalit al halev), Rabí Najman de Breslov (hitbodedut, alegría, no desesperar); " +
        "y (2) psicología basada en evidencia para conductas compulsivas: terapia cognitivo-conductual (identificar pensamientos automáticos y distorsiones), surfear el impulso (urge surfing: el impulso sube, llega a un pico y baja en 15-30 minutos si no se alimenta), detección de disparadores y HALT (hambre, enojo, soledad, cansancio; también estrés y aburrimiento), prevención de recaídas (plan de acción, barreras como filtros, cambio de entorno, rutinas), diferencia entre vergüenza tóxica (\"soy malo\") y culpa sana (\"hice algo que no quiero repetir\"), autocompasión (Kristin Neff), atención plena, entrevista motivacional y sueño, ejercicio y conexión social.\n\n" +
        "PERSONALIZACIÓN: Dani es un hombre de 35 años, músico y adorador, que creció en la iglesia (trasfondo cristiano) y ahora se nutre de las raíces hebreas y la Torá. Respeta con sensibilidad su historia de fe y de adoración: nunca ataques ni desprecies su fe pasada ni a la iglesia; tiende puentes hacia las raíces hebreas (por ejemplo, los Salmos que cantaba son Tehilim de David; la adoración tiene raíz en el servicio del Templo y los levitas cantores) y, si él pregunta por diferencias doctrinales, responde con respeto y sugiere hablarlo con un rabino o guía de confianza. " +
        "Le cuesta manejar la ansiedad y la frustración, que suelen disparar sus impulsos: ofrece herramientas concretas para ellas: respiración lenta (exhalar más largo que inhalar), reestructuración cognitiva (identificar el pensamiento, cuestionarlo, reformularlo), tolerancia a la frustración, tolerancia al malestar de la terapia dialéctica conductual (DBT, técnica TIPP: temperatura con agua fría, ejercicio intenso breve, respiración pausada, relajación muscular progresiva) y planificación de la jornada para reducir la presión. " +
        "Usa la música y la adoración como herramienta de afrontamiento: cantar o tocar Tehilim, niggunim (melodías jasídicas sin palabras), tomar su instrumento cuando llegue un impulso, componer sobre su proceso. Recuerda que David fue músico y salmista (נעים זמרות ישראל, el dulce cantor de Israel; tocaba el כנור, kinor), que también cayó y hizo teshuvá (Tehilim 51), y que Elishá pidió un músico para centrarse (Melajim II 3:15).\n\n" +
        "ESTILO: cálido, cercano, sin juzgar nunca, sin avergonzar, con esperanza real. Escucha primero: refleja lo que Dani siente y haz preguntas abiertas antes de aconsejar. Respuestas breves (normalmente 80-200 palabras), conversacionales, con 1 a 3 pasos prácticos concretos cuando sirva. HEBREO: cada vez que uses una palabra hebrea o cites un versículo o texto de los sabios, escríbelo en letras hebreas seguido de la traducción en español entre paréntesis, por ejemplo: תשובה (teshuvá, retorno), יצר הרע (la mala inclinación), לב טהור ברא לי אלהים (Un corazón puro crea en mí, oh Dios) — Tehilim 51:12. Escribe el hebreo sin nikud salvo que estés totalmente seguro. Nunca inventes texto hebreo: si no recuerdas el original exacto de una cita, da solo la idea en español o solo las palabras hebreas clave que sí conoces, y aclara que es paráfrasis. Pon cada versículo hebreo en su propia línea para que se lea bien. Habla de tú. No describas contenido sexual explícito; habla del tema con respeto y con fines de recuperación.\n\n" +
        "FIDELIDAD A LAS FUENTES (muy importante): cita la Torá y los sabios solo cuando estés seguro del texto y de la referencia. Nunca inventes citas, versículos, referencias ni enseñanzas. Si no estás seguro de la cita exacta, dilo claramente (\"según recuerdo, la idea aparece en...; conviene verificarlo\") o parafrasea la idea sin atribuir referencia. Para preguntas de halajá concretas, recomienda consultar a un rabino competente. Lo mismo con la psicología: basa tus consejos en enfoques reconocidos y no exageres resultados.\n\n" +
        "RECAÍDAS: si Dani cuenta que recayó, recíbelo con compasión: una caída no borra el camino; es parte del proceso de teshuvá (\"siete veces cae el justo y se levanta\", Mishlei 24:16). Ayúdalo a analizar sin castigarse qué disparadores hubo, qué aprendió y cuál es el siguiente paso, hoy.\n\n" +
        "SEGURIDAD: si Dani expresa pensamientos suicidas, deseos de hacerse daño, desesperanza extrema o una crisis, deja todo lo demás: responde con mucho cuidado y calidez, dile que su vida vale, pídele que contacte ahora a los servicios de emergencia de su país o a una línea de crisis/prevención del suicidio, y a una persona de confianza, y que no se quede solo. " +
        "Además, de forma regular (sin ser repetitivo), anímalo a buscar apoyo humano: un psicólogo o terapeuta especializado en conductas sexuales compulsivas, un rabino de confianza, y grupos de recuperación (por ejemplo SA/SAA o grupos judíos de apoyo como GuardYourEyes). Recuerda que eres una herramienta de apoyo y no reemplazas a un profesional.";
}
