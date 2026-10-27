package com.dani.tshuva;

import android.app.*; import android.content.*; import android.graphics.*; import android.graphics.drawable.*; import android.os.*; import android.speech.*; import android.view.*; import android.widget.*;
import java.util.*; import java.util.concurrent.*;

public class ChatActivity extends Base {
    LinearLayout lista; ScrollView sv; EditText et; Button enviar, mic; TextView estado;
    List<String[]> hist; boolean ocupado; SpeechRecognizer rec; boolean escuchando;
    final ExecutorService ex = Executors.newSingleThreadExecutor();

    @Override protected void onCreate(Bundle b) {
        super.onCreate(b);
        LinearLayout root = new LinearLayout(this); root.setOrientation(LinearLayout.VERTICAL);
        TextView aviso = Ui.text(this, "  Plan gratis de Google: tus mensajes pueden usarse para mejorar sus productos. No reemplaza ayuda profesional.", 11);
        aviso.setBackgroundColor(Color.rgb(255, 243, 224)); aviso.setTextColor(Color.rgb(93, 64, 55)); root.addView(aviso);
        sv = new ScrollView(this); lista = Ui.col(this); sv.addView(lista);
        root.addView(sv, new LinearLayout.LayoutParams(-1, 0, 1));
        estado = Ui.text(this, "", 13); estado.setPadding(Ui.dp(this, 16), 0, 0, 0); estado.setTextColor(Color.GRAY); root.addView(estado);
        LinearLayout bar = new LinearLayout(this); bar.setPadding(Ui.dp(this, 8), Ui.dp(this, 4), Ui.dp(this, 8), Ui.dp(this, 8)); bar.setGravity(Gravity.CENTER_VERTICAL);
        et = new EditText(this); et.setHint("…o escribe aquí (opcional)"); et.setMaxLines(5); bar.addView(et, new LinearLayout.LayoutParams(0, -2, 1));
        enviar = new Button(this); enviar.setText("➤"); enviar.setTextSize(20); bar.addView(enviar, new LinearLayout.LayoutParams(Ui.dp(this, 56), Ui.dp(this, 56)));
        root.addView(bar);
        mic = Ui.button(this, "", Ui.AZUL); mic.setTextSize(22);
        LinearLayout.LayoutParams mlp = new LinearLayout.LayoutParams(-1, Ui.dp(this, 84)); int mg = Ui.dp(this, 12); mlp.setMargins(mg, 0, mg, mg);
        root.addView(mic, mlp); pintarMic(); setContentView(root);
        enviar.setOnClickListener(v -> enviar());
        mic.setOnClickListener(v -> { if (ocupado && !escuchando) return; if (escuchando) detenerYEnviar(); else escuchar(); });

        hist = Store.load(this, "chat");
        if (hist.isEmpty()) burbuja("model", "\u2067שלום\u2069\u200E (Shalom: paz), Dani. Estoy aquí para escucharte, sin juicio. Toca el micrófono, cuéntame cómo te sientes (puedes hacer pausas) y toca Detener: te respondo por escrito.", false);
        for (String[] h : hist) burbuja(h[0], h[1], false);
        if (getIntent().getBooleanExtra("sos", false)) et.setText("Acabo de tener un impulso fuerte y usé el botón SOS. ");
        if (Store.apiKey(this).isEmpty()) estado.setText("Falta tu clave de Gemini: ve a ⋮ › Ajustes. Todo lo demás de la app funciona sin ella.");
    }

    @Override public boolean onCreateOptionsMenu(Menu m) { m.add(0, 1, 0, "Ajustes"); m.add(0, 2, 0, "Borrar historial"); return true; }
    @Override public boolean onOptionsItemSelected(MenuItem i) {
        if (i.getItemId() == 1) startActivity(new Intent(this, SettingsActivity.class));
        if (i.getItemId() == 2) new AlertDialog.Builder(this).setMessage("¿Borrar toda la conversación de este teléfono?")
            .setPositiveButton("Borrar", (d, w) -> { Store.setSinceMemory(this, 0); hist.clear(); Store.save(this, "chat", hist); lista.removeAllViews(); burbuja("model", "Historial borrado. ¿De qué quieres hablar?", false); })
            .setNegativeButton("Cancelar", null).show();
        return true;
    }
    @Override protected void onResume() { super.onResume(); if (!Store.apiKey(this).isEmpty() && !ocupado) estado.setText(""); }

    void burbuja(String rol, String t, boolean anim) {
        TextView tv = Ui.text(this, t, 16); tv.setTextIsSelectable(true);
        int p = Ui.dp(this, 12); tv.setPadding(p, p, p, p);
        GradientDrawable g = new GradientDrawable(); g.setCornerRadius(Ui.dp(this, 16));
        boolean yo = "user".equals(rol);
        g.setColor(yo ? Color.rgb(197, 202, 233) : Color.rgb(255, 248, 225)); tv.setBackground(g);
        LinearLayout.LayoutParams lp = new LinearLayout.LayoutParams(-2, -2); lp.topMargin = Ui.dp(this, 8);
        lp.gravity = yo ? Gravity.END : Gravity.START; if (yo) lp.leftMargin = Ui.dp(this, 40); else lp.rightMargin = Ui.dp(this, 40);
        lista.addView(tv, lp); sv.post(() -> sv.fullScroll(View.FOCUS_DOWN));
    }

    void enviar() {
        if (escuchando) detener();
        String t = et.getText().toString().trim(); if (t.isEmpty() || ocupado) return;
        if (Gemini.isCrisis(t)) Ui.crisisDialog(this);
        String key = Store.apiKey(this);
        if (key.isEmpty()) { estado.setText("Primero agrega tu clave gratuita de Gemini en Ajustes."); startActivity(new Intent(this, SettingsActivity.class)); return; }
        et.setText(""); hist.add(new String[]{"user", t, String.valueOf(System.currentTimeMillis())}); Store.save(this, "chat", hist); burbuja("user", t, true);
        pedir(key);
    }

    void pedir(String key) {
        ocupado = true; enviar.setEnabled(false); estado.setText("El guía está escribiendo…");
        final List<String[]> copia = new ArrayList<>(hist); final String model = Store.model(this);
        final long dias = (System.currentTimeMillis() - Store.cleanSince(this)) / 86400000L;
        final String sys = Gemini.systemWithMemory(Gemini.systemWithProfile(Gemini.SYSTEM_PROMPT, Store.profile(this)), Store.memory(this)) + "\n\nContexto: hoy es " + new java.text.SimpleDateFormat("EEEE d 'de' MMMM 'de' yyyy", new Locale("es")).format(new Date()) + ". Dani lleva " + dias + " días en el contador de su camino.";
        ex.execute(() -> {
            String resp; String err = null;
            try { resp = Gemini.send(key, model, sys, copia); } catch (java.net.UnknownHostException e) { resp = null; err = "Sin conexión a internet. El SOS, el diario y el contador funcionan sin internet."; }
            catch (Exception e) { resp = null; err = e.getMessage() == null ? "Error de conexión" : e.getMessage(); }
            final String r = resp, er = err;
            if (r != null) actualizarMemoria(key, model, copia, r);
            runOnUiThread(() -> {
                if (isFinishing() || isDestroyed()) { if (r != null) { hist.add(new String[]{"model", r, ""}); Store.save(this, "chat", hist); } return; }
                ocupado = false; enviar.setEnabled(true);
                if (r != null) { hist.add(new String[]{"model", r, String.valueOf(System.currentTimeMillis())}); Store.save(this, "chat", hist); burbuja("model", r, true); estado.setText(""); }
                else { estado.setText("⚠️ " + er + "  (toca para reintentar)"); estado.setOnClickListener(v -> { if (!ocupado) pedir(Store.apiKey(this)); }); }
            });
        });
    }

    /** Corre en el hilo de fondo: cada MEMORY_EVERY mensajes pide a la IA que actualice el resumen. */
    void actualizarMemoria(String key, String model, List<String[]> copia, String respuesta) {
        int n = Store.sinceMemory(this) + 2;
        if (n < Gemini.MEMORY_EVERY) { Store.setSinceMemory(this, n); return; }
        try {
            List<String[]> h = new ArrayList<>(copia); h.add(new String[]{"model", respuesta});
            String nueva = Gemini.send(key, model, Gemini.MEMORY_SYSTEM, Gemini.memoryRequest(Store.memory(this), h));
            if (!nueva.trim().isEmpty()) Store.setMemory(this, nueva.trim());
            Store.setSinceMemory(this, 0);
        } catch (Exception e) { Store.setSinceMemory(this, n); }   // se reintenta la próxima vez
    }

    // ---- voz (solo SpeechRecognizer: nunca se graba ni guarda audio) ---- (como en VozTareas) ----
    void escuchar() {
        if (checkSelfPermission("android.permission.RECORD_AUDIO") != 0) { requestPermissions(new String[]{"android.permission.RECORD_AUDIO"}, 1); return; }
        if (!SpeechRecognizer.isRecognitionAvailable(this)) { Toast.makeText(this, "No hay reconocimiento de voz. Instala/activa la app de Google.", Toast.LENGTH_LONG).show(); return; }
        if (rec == null) { rec = SpeechRecognizer.createSpeechRecognizer(this); rec.setRecognitionListener(oyente); }
        escuchando = true; esperandoFinal = false; ultimoParcial = ""; pintarMic();
        getWindow().addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON); iniciar();
    }
    void iniciar() {
        Intent i = new Intent(RecognizerIntent.ACTION_RECOGNIZE_SPEECH)
            .putExtra(RecognizerIntent.EXTRA_LANGUAGE_MODEL, RecognizerIntent.LANGUAGE_MODEL_FREE_FORM)
            .putExtra(RecognizerIntent.EXTRA_LANGUAGE, Locale.getDefault().getLanguage().equals("es") ? Locale.getDefault().toLanguageTag() : "es-ES")
            .putExtra(RecognizerIntent.EXTRA_PARTIAL_RESULTS, true)
            .putExtra(RecognizerIntent.EXTRA_SPEECH_INPUT_COMPLETE_SILENCE_LENGTH_MILLIS, 10000L)
            .putExtra(RecognizerIntent.EXTRA_SPEECH_INPUT_POSSIBLY_COMPLETE_SILENCE_LENGTH_MILLIS, 10000L)
            .putExtra(RecognizerIntent.EXTRA_SPEECH_INPUT_MINIMUM_LENGTH_MILLIS, 60000L);
        try { rec.startListening(i); } catch (Exception e) { reintentar(); }
    }
    void reintentar() { if (escuchando) new Handler(Looper.getMainLooper()).postDelayed(() -> { if (escuchando) { try { rec.cancel(); } catch (Exception ignored) {} iniciar(); } }, 300); }
    boolean esperandoFinal; String ultimoParcial = ""; final Handler h = new Handler(Looper.getMainLooper());
    void pintarMic() {
        android.graphics.drawable.GradientDrawable g = new android.graphics.drawable.GradientDrawable(); g.setCornerRadius(Ui.dp(this, 20));
        g.setColor(escuchando ? Color.rgb(229, 57, 53) : Ui.AZUL); mic.setBackground(g); mic.setTextColor(Color.WHITE);
        mic.setText(escuchando ? "⏹  Detener y enviar" : "🎤  Toca y habla");
    }
    /** Detiene el dictado y, al llegar el texto final, lo envía automáticamente. */
    void detenerYEnviar() {
        esperandoFinal = true; detener(); estado.setText("Procesando lo que dijiste…");
        h.postDelayed(this::finalizar, 2500);   // por si el reconocedor no entrega resultado final
    }
    void finalizar() {
        if (!esperandoFinal) return; esperandoFinal = false;
        if (!ultimoParcial.isEmpty()) { et.setText(unir(et.getText().toString(), ultimoParcial)); ultimoParcial = ""; }
        if (et.getText().toString().trim().isEmpty()) { estado.setText("No te escuché bien. Toca el micrófono e inténtalo de nuevo."); return; }
        estado.setText(""); enviar();
    }
    void detener() {
        escuchando = false; pintarMic();
        getWindow().clearFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON);
        if (rec != null) try { rec.stopListening(); } catch (Exception ignored) {}
        if (!esperandoFinal) estado.setText("");
    }
    static String unir(String a, String b) { a = a.trim(); return a.isEmpty() ? b : a + " " + b; }
    final RecognitionListener oyente = new RecognitionListener() {
        public void onReadyForSpeech(Bundle p) { estado.setText("🔴 Escuchando… (puedes hacer pausas; toca ⏹ al terminar)"); }
        public void onBeginningOfSpeech() {} public void onRmsChanged(float r) {} public void onBufferReceived(byte[] b) {} public void onEndOfSpeech() {} public void onEvent(int t, Bundle b) {}
        public void onPartialResults(Bundle b) { ArrayList<String> r = b.getStringArrayList(SpeechRecognizer.RESULTS_RECOGNITION); if (r != null && !r.isEmpty() && !r.get(0).isEmpty()) { ultimoParcial = r.get(0); if (escuchando) estado.setText("🔴 " + r.get(0)); } }
        public void onResults(Bundle b) {
            ArrayList<String> r = b.getStringArrayList(SpeechRecognizer.RESULTS_RECOGNITION);
            if (r != null && !r.isEmpty() && !r.get(0).isEmpty()) { et.setText(unir(et.getText().toString(), r.get(0))); et.setSelection(et.length()); }
            ultimoParcial = "";
            if (escuchando) iniciar(); else if (esperandoFinal) finalizar();
        }
        public void onError(int e) {
            if (e == SpeechRecognizer.ERROR_INSUFFICIENT_PERMISSIONS) { esperandoFinal = false; detener(); Toast.makeText(ChatActivity.this, "Falta permiso de micrófono", Toast.LENGTH_SHORT).show(); return; }
            if (esperandoFinal) { finalizar(); return; }
            reintentar();
        }
    };
    @Override public void onRequestPermissionsResult(int c, String[] p, int[] g) { if (c == 1 && g.length > 0 && g[0] == 0) escuchar(); }
    @Override protected void onPause() { if (escuchando) { esperandoFinal = false; detener(); } super.onPause(); }
    @Override protected void onDestroy() { if (rec != null) rec.destroy(); ex.shutdown(); super.onDestroy(); }
}
