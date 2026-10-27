package com.dani.tshuva;

import android.app.*; import android.content.*; import android.graphics.*; import android.os.*; import android.widget.*;

public class MainActivity extends Base {
    TextView dias, frase;
    @Override protected void onCreate(Bundle b) {
        super.onCreate(b);
        if (Build.VERSION.SDK_INT >= 33) requestPermissions(new String[]{"android.permission.POST_NOTIFICATIONS"}, 2);
        DailyReceiver.schedule(this);
        ScrollView sv = new ScrollView(this); LinearLayout r = Ui.col(this); sv.addView(r);
        TextView t = Ui.text(this, "\u200Fתשובה\u200E (Teshuvá: retorno)", 24); t.setTextColor(Ui.AZUL); t.setTypeface(null, Typeface.BOLD); r.addView(t);
        r.addView(Ui.text(this, "Cada día es un nuevo comienzo. " + Messages.t("חדשים לבקרים", "nuevas cada mañana") + " — Eijá 3:23", 15));

        LinearLayout c = new LinearLayout(this); c.setOrientation(LinearLayout.VERTICAL);
        dias = Ui.text(this, "", 44); dias.setTypeface(null, Typeface.BOLD); dias.setTextColor(Color.WHITE); dias.setGravity(android.view.Gravity.CENTER);
        TextView sub = Ui.text(this, "días en el camino", 16); sub.setTextColor(Color.WHITE); sub.setGravity(android.view.Gravity.CENTER);
        c.addView(dias); c.addView(sub); r.addView(Ui.card(this, c, Ui.AZUL));

        Button sos = Ui.button(this, "🆘  SOS – Tengo un impulso ahora", Color.rgb(198, 40, 40)); sos.setTextSize(19);
        sos.setOnClickListener(v -> startActivity(new Intent(this, SosActivity.class))); r.addView(sos);

        frase = Ui.text(this, "", 16); r.addView(Ui.card(this, frase, Color.rgb(255, 248, 225)));

        Button chat = Ui.button(this, "💬  Hablar con el guía", Ui.AZUL); chat.setOnClickListener(v -> startActivity(new Intent(this, ChatActivity.class))); r.addView(chat);
        Button diario = Ui.button(this, "📓  Mi diario privado", Color.rgb(69, 90, 100)); diario.setOnClickListener(v -> startActivity(new Intent(this, JournalActivity.class))); r.addView(diario);
        Button otra = Ui.button(this, "✨  Otra enseñanza", Ui.ORO); otra.setOnClickListener(v -> mostrar(Messages.random())); r.addView(otra);
        Button reset = Ui.button(this, "↺  Reiniciar contador", Color.rgb(120, 120, 120)); reset.setOnClickListener(v -> reiniciar()); r.addView(reset);
        Button aj = Ui.button(this, "⚙️  Ajustes", Color.rgb(96, 125, 139)); aj.setOnClickListener(v -> startActivity(new Intent(this, SettingsActivity.class))); r.addView(aj);

        TextView nota = Ui.text(this, "Esta app es un apoyo y no reemplaza a un psicólogo, a un rabino ni a un grupo de recuperación. Si estás en peligro, llama a emergencias.", 12);
        nota.setTextColor(Color.GRAY); r.addView(nota);
        setContentView(sv);
        mostrar(Messages.today());
    }
    void mostrar(String[] m) { frase.setText(Messages.format(m)); }
    @Override protected void onResume() { super.onResume(); long d = (System.currentTimeMillis() - Store.cleanSince(this)) / 86400000L; dias.setText(String.valueOf(d)); }

    void reiniciar() {
        new AlertDialog.Builder(this).setTitle("Volver a empezar")
            .setMessage("Una caída no borra el camino recorrido.\n\n\u200Fכי שבע יפול צדיק וקם\u200F\n\u200E(Siete veces cae el justo y se levanta) — Mishlei 24:16\n\nReiniciar no es fracasar: es hacer " + Messages.t("תשובה", "teshuvá: retorno") + " y seguir. ¿Quieres poner el contador en cero y empezar hoy de nuevo?")
            .setPositiveButton("Sí, empiezo de nuevo", (d, w) -> {
                Store.p(this).edit().putLong("since", System.currentTimeMillis()).apply(); onResume();
                new AlertDialog.Builder(this).setTitle("Bien hecho por ser honesto 💛")
                    .setMessage("Respira. No te castigues: la culpa sana dice \"hice algo que no quiero repetir\", la vergüenza dice \"soy malo\" — y eso no es verdad.\n\nPregúntate con calma: ¿qué pasó antes? (¿hambre, enojo, soledad, cansancio, estrés?) ¿Qué puedo cambiar hoy?\n\nSi quieres, cuéntaselo al guía o escríbelo en tu diario.")
                    .setPositiveButton("Hablar con el guía", (d2, w2) -> startActivity(new Intent(this, ChatActivity.class)))
                    .setNegativeButton("Ahora no", null).show();
            }).setNegativeButton("Cancelar", null).show();
    }
}
