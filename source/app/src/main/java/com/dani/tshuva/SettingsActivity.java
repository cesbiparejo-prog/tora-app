package com.dani.tshuva;

import android.app.*; import android.content.*; import android.graphics.*; import android.net.*; import android.os.*; import android.text.*; import android.widget.*;

public class SettingsActivity extends Base {
    EditText key, model, mem, perfil; Button hora; CheckBox notif; int h, m;
    @Override protected void onCreate(Bundle b) {
        super.onCreate(b);
        ScrollView sv = new ScrollView(this); LinearLayout r = Ui.col(this); sv.addView(r);
        r.addView(Ui.text(this, "Clave de Google Gemini (API key)", 17));
        key = new EditText(this); key.setHint("Pega aquí tu clave (empieza por AIza o AQ.)"); key.setSingleLine();
        key.setInputType(InputType.TYPE_CLASS_TEXT | InputType.TYPE_TEXT_VARIATION_PASSWORD); key.setText(Store.apiKey(this)); r.addView(key);
        CheckBox ver = new CheckBox(this); ver.setText("Mostrar clave"); r.addView(ver);
        ver.setOnCheckedChangeListener((v, on) -> key.setInputType(InputType.TYPE_CLASS_TEXT | (on ? InputType.TYPE_TEXT_VARIATION_VISIBLE_PASSWORD : InputType.TYPE_TEXT_VARIATION_PASSWORD)));
        Button obt = Ui.button(this, "Obtener clave gratis (Google AI Studio)", Color.rgb(96, 125, 139));
        obt.setOnClickListener(v -> { try { startActivity(new Intent(Intent.ACTION_VIEW, Uri.parse("https://aistudio.google.com/apikey"))); } catch (Exception ignored) {} }); r.addView(obt);
        r.addView(Ui.text(this, "Modelo de IA", 17));
        model = new EditText(this); model.setSingleLine(); model.setText(Store.model(this)); r.addView(model);
        r.addView(Ui.text(this, "Por defecto: " + Gemini.DEFAULT_MODEL + ". Si Google está saturado, la app prueba sola otros modelos Flash gratuitos.", 12));

        notif = new CheckBox(this); notif.setText("Aliento diario (notificación)"); notif.setChecked(Store.notifOn(this)); r.addView(notif);
        h = Store.hour(this); m = Store.minute(this);
        hora = Ui.button(this, "", Ui.ORO); hora.setOnClickListener(v -> new TimePickerDialog(this, (tp, hh, mm) -> { h = hh; m = mm; pintarHora(); }, h, m, true).show()); r.addView(hora); pintarHora();

        Button g = Ui.button(this, "Guardar", Ui.AZUL); g.setOnClickListener(v -> guardar()); r.addView(g);
        Button prueba = Ui.button(this, "Probar notificación ahora", Color.rgb(120, 120, 120));
        prueba.setOnClickListener(v -> new DailyReceiver().onReceive(this, new Intent())); r.addView(prueba);

        r.addView(Ui.text(this, "🙋 Sobre mí", 17));
        r.addView(Ui.text(this, "Lo que quieres que el guía sepa siempre de ti. Se envía con cada mensaje.", 12));
        perfil = new EditText(this); perfil.setMinLines(3); perfil.setGravity(android.view.Gravity.TOP); perfil.setTextSize(14); perfil.setText(Store.profile(this)); r.addView(perfil);

        r.addView(Ui.text(this, "🧠 Memoria del guía", 17));
        r.addView(Ui.text(this, "Resumen que la IA escribe sobre tu camino (disparadores, progreso, lo que te ayuda). Se guarda solo aquí y se envía a Google con cada mensaje para dar continuidad. Puedes corregirlo o borrarlo.", 12));
        mem = new EditText(this); mem.setMinLines(3); mem.setGravity(android.view.Gravity.TOP); mem.setTextSize(14);
        mem.setHint("(Todavía vacía: se crea después de algunos mensajes)"); mem.setText(Store.memory(this)); r.addView(mem);
        Button borrarMem = Ui.button(this, "Borrar memoria", Color.rgb(198, 40, 40));
        borrarMem.setOnClickListener(v -> new AlertDialog.Builder(this).setMessage("¿Borrar la memoria del guía? El historial del chat no se borra.")
            .setPositiveButton("Borrar", (d, w) -> { Store.p(this).edit().putBoolean("memSeed", true).apply(); Store.setMemory(this, ""); Store.setSinceMemory(this, 0); mem.setText(""); Toast.makeText(this, "Memoria borrada", Toast.LENGTH_SHORT).show(); })
            .setNegativeButton("Cancelar", null).show());
        r.addView(borrarMem);

        r.addView(Ui.text(this, "🔐 Bloqueo", 17));
        Button cambiar = Ui.button(this, "Cambiar PIN", Color.rgb(69, 90, 100));
        cambiar.setOnClickListener(v -> startActivity(new Intent(this, LockActivity.class).putExtra("modo", LockActivity.CHANGE))); r.addView(cambiar);
        CheckBox huella = new CheckBox(this); huella.setText("Desbloquear con huella (si el teléfono la tiene)"); huella.setChecked(Lock.biometricOn(this));
        huella.setOnCheckedChangeListener((v, on) -> Lock.p(this).edit().putBoolean("huella", on).apply()); r.addView(huella);
        r.addView(Ui.text(this, "La app aparece como «Afinador», se bloquea al abrirla o tras 1 minuto fuera, y no muestra su contenido en la pantalla de apps recientes ni en las notificaciones.", 12));

        TextView priv = Ui.text(this, "🔒 Privacidad\n\nTu diario, tu contador, tu historial de chat, la memoria del guía y «Sobre mí» se guardan solo en este teléfono. La voz se convierte en texto con el reconocimiento de voz del teléfono; la app nunca graba ni guarda audio. Los mensajes del chat (junto con «Sobre mí» y la memoria del guía) se envían a Google (Gemini) para obtener la respuesta. Con el plan GRATUITO de Google, Google puede usar esas conversaciones (y revisores humanos pueden leerlas) para mejorar sus productos. No escribas datos que te identifiquen (nombre completo, dirección, etc.). Si prefieres que no se usen, activa la facturación en Google AI Studio (plan pago).\n\n⚕️ Esta app es un apoyo diario y NO reemplaza a un psicólogo, a un médico, a un rabino ni a un grupo de recuperación. En una emergencia, llama a los servicios de emergencia.", 13);
        r.addView(Ui.card(this, priv, Color.rgb(255, 243, 224)));
        setContentView(sv);
    }
    void pintarHora() { hora.setText(String.format("⏰ Hora del aliento: %02d:%02d", h, m)); }
    void guardar() {
        Store.p(this).edit().putString("key", key.getText().toString().trim()).putString("model", model.getText().toString().trim().isEmpty() ? Gemini.DEFAULT_MODEL : model.getText().toString().trim())
            .putInt("hour", h).putInt("min", m).putBoolean("notif", notif.isChecked()).putString("memoria", mem.getText().toString().trim()).putBoolean("memSeed", true).putString("perfil", perfil.getText().toString().trim()).apply();
        DailyReceiver.schedule(this); Toast.makeText(this, "Guardado", Toast.LENGTH_SHORT).show(); finish();
    }
}
