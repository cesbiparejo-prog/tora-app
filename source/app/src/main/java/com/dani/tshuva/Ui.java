package com.dani.tshuva;

import android.app.*; import android.graphics.*; import android.graphics.drawable.*; import android.view.*; import android.widget.*;

class Ui {
    static final int AZUL = Color.rgb(26, 35, 126), ORO = Color.rgb(255, 193, 7);
    static int dp(Activity a, int v) { return (int) (v * a.getResources().getDisplayMetrics().density + 0.5f); }
    static LinearLayout col(Activity a) { LinearLayout l = new LinearLayout(a); l.setOrientation(LinearLayout.VERTICAL); int p = dp(a, 16); l.setPadding(p, p, p, p); return l; }
    static TextView text(Activity a, String s, float sp) { TextView t = new TextView(a); t.setText(s); t.setTextSize(sp); t.setTextColor(Color.rgb(33, 33, 33)); t.setPadding(0, dp(a, 6), 0, dp(a, 6)); return t; }
    static Button button(Activity a, String s, int color) {
        Button b = new Button(a); b.setText(s); b.setAllCaps(false); b.setTextSize(17); b.setTextColor(color == ORO ? Color.BLACK : Color.WHITE);
        GradientDrawable g = new GradientDrawable(); g.setColor(color); g.setCornerRadius(dp(a, 14)); b.setBackground(g);
        LinearLayout.LayoutParams lp = new LinearLayout.LayoutParams(-1, dp(a, 56)); lp.topMargin = dp(a, 10); b.setLayoutParams(lp); return b;
    }
    static View card(Activity a, View inner, int bg) {
        LinearLayout c = col(a); GradientDrawable g = new GradientDrawable(); g.setColor(bg); g.setCornerRadius(dp(a, 16)); c.setBackground(g);
        c.addView(inner); LinearLayout.LayoutParams lp = new LinearLayout.LayoutParams(-1, -2); lp.topMargin = dp(a, 10); c.setLayoutParams(lp); return c;
    }
    static void crisisDialog(Activity a) {
        new AlertDialog.Builder(a).setTitle("Tu vida vale muchísimo 💛")
            .setMessage("Si estás pensando en hacerte daño o en quitarte la vida, por favor no te quedes solo con esto.\n\n• Llama ahora a los servicios de emergencia de tu país (112 en Europa y muchos países, 911 en América, 101 en Israel).\n• Llama a una línea de prevención del suicidio de tu país (en Israel: ERAN 1201; en España: 024; en Argentina: 135; en EE. UU.: 988).\n• Escribe o llama a alguien de confianza: un familiar, un amigo, tu rabino.\n\nNo eres una carga. Pedir ayuda es un acto de fuerza.")
            .setPositiveButton("Llamar a emergencias", (d, w) -> { try { a.startActivity(new android.content.Intent(android.content.Intent.ACTION_DIAL, android.net.Uri.parse("tel:112"))); } catch (Exception ignored) {} })
            .setNegativeButton("Cerrar", null).show();
    }
}
