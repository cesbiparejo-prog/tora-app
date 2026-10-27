package com.dani.tshuva;

import android.app.*; import android.content.*; import android.graphics.*; import android.os.*; import android.widget.*;

public class SosActivity extends Base {
    static final String[][] PASOS = {
        {"Para y respira", "Aleja la pantalla. Inhala por la nariz 4 segundos… sostén 4… exhala por la boca 6. Repite 5 veces.\n\nEl impulso es una ola: sube, llega a un pico y baja. No tienes que obedecerla."},
        {"Surfea la ola", "Observa el impulso sin pelear: ¿dónde lo sientes en el cuerpo? ¿Calor, tensión, inquietud? Ponle un número del 1 al 10.\n\nSolo míralo, como un surfista mira la ola. En 15–30 minutos baja si no lo alimentas."},
        {"Si es ansiedad o frustración", "Usa TIPP (de la terapia DBT):\n• Temperatura: agua fría en la cara 30 segundos.\n• Intenso ejercicio: 1 minuto de sentadillas o saltos.\n• Pausada respiración: exhala más largo que inhalas.\n• Progresiva relajación: tensa y suelta hombros, manos, piernas.\n\nLuego reformula: '¿Qué me estoy diciendo? ¿Es 100% cierto? ¿Qué le diría a un amigo?' La frustración es incómoda, pero la puedes tolerar sin escapar."},
        {"Aterriza (5-4-3-2-1)", "Nombra 5 cosas que ves, 4 que puedes tocar, 3 que oyes, 2 que hueles y 1 que saboreas.\n\nPregúntate HALT: ¿tengo hambre, enojo, soledad o cansancio? ¿Qué necesito de verdad ahora?"},
        {"\u2067תהלים\u2069\u200E (Tehilim: Salmos)", "\u200Fלב טהור ברא לי אלהים, ורוח נכון חדש בקרבי\u200F\n\u200E(Crea en mí, oh Dios, un corazón puro, y renueva un espíritu recto dentro de mí.) — Tehilim 51:12\n\n\u200Fאשא עיני אל ההרים, מאין יבא עזרי. עזרי מעם ה', עשה שמים וארץ\u200F\n\u200E(Alzaré mis ojos a los montes: ¿de dónde vendrá mi ayuda? Mi ayuda viene de Hashem, Hacedor de los cielos y la tierra.) — Tehilim 121:1-2\n\n\u200Fכי שבע יפול צדיק וקם\u200F\n\u200E(Siete veces cae el justo y se levanta.) — Mishlei 24:16\n\nDilo despacio, en voz alta si puedes."},
        {"Toma tu instrumento o canta un salmo", "Como David con su \u2067כנור\u2069\u200E (kinor: arpa), deja que la música te devuelva la calma. Toca 10 minutos o canta despacio un salmo o un \u2067ניגון\u2069\u200E (niggun: melodía).\n\n\u200Fועתה קחו לי מנגן\u200F\n\u200E(Y ahora tráiganme un músico.) — Melajim II 3:15\n\nCantar te obliga a respirar lento: el impulso pierde fuerza."},
        {"Muévete", "Cambia de lugar ya: sal de la habitación, lávate la cara con agua fría, haz 20 sentadillas o sal a caminar. Llama o escribe a alguien.\n\n\u200Fאיזהו גבור? הכובש את יצרו\u200F\n\u200E(¿Quién es fuerte? El que domina su inclinación.) — Pirkei Avot 4:1\n\nLo estás haciendo ahora mismo."}
    };
    int i = 0; TextView titulo, cuerpo, timer; Button sig; CountDownTimer ct;

    @Override protected void onCreate(Bundle b) {
        super.onCreate(b);
        ScrollView sv = new ScrollView(this); LinearLayout r = Ui.col(this); sv.addView(r);
        titulo = Ui.text(this, "", 24); titulo.setTypeface(null, Typeface.BOLD); titulo.setTextColor(Ui.AZUL);
        cuerpo = Ui.text(this, "", 19); timer = Ui.text(this, "", 16); timer.setTextColor(Color.GRAY);
        r.addView(titulo); r.addView(cuerpo); r.addView(timer);
        sig = Ui.button(this, "Siguiente ➜", Ui.AZUL); sig.setOnClickListener(v -> siguiente()); r.addView(sig);
        Button chat = Ui.button(this, "💬 Hablar con el guía ahora", Color.rgb(69, 90, 100));
        chat.setOnClickListener(v -> abrirChat()); r.addView(chat);
        setContentView(sv); getWindow().addFlags(android.view.WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON);
        mostrar();
    }
    void mostrar() {
        titulo.setText((i + 1) + "/" + PASOS.length + " · " + PASOS[i][0]); cuerpo.setText(PASOS[i][1]);
        if (ct != null) ct.cancel();
        ct = new CountDownTimer(60000, 1000) {
            public void onTick(long ms) { timer.setText("Quédate aquí con este paso: " + (ms / 1000) + " s"); }
            public void onFinish() { timer.setText("Cuando estés listo, sigue."); }
        }.start();
        sig.setText(i == PASOS.length - 1 ? "Terminé ✔ — hablar con el guía" : "Siguiente ➜");
    }
    void siguiente() { if (i < PASOS.length - 1) { i++; mostrar(); } else abrirChat(); }
    void abrirChat() {
        startActivity(new Intent(this, ChatActivity.class).putExtra("sos", true)); finish();
    }
    @Override protected void onDestroy() { if (ct != null) ct.cancel(); super.onDestroy(); }
}
