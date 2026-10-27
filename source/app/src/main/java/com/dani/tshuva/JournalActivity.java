package com.dani.tshuva;

import android.app.*; import android.graphics.*; import android.os.*; import android.widget.*;
import java.text.*; import java.util.*;

public class JournalActivity extends Base {
    LinearLayout lista; EditText et; List<String[]> notas;
    @Override protected void onCreate(Bundle b) {
        super.onCreate(b);
        ScrollView sv = new ScrollView(this); LinearLayout r = Ui.col(this); sv.addView(r);
        r.addView(Ui.text(this, "Solo se guarda en este teléfono. Escribe lo que sientes, tus disparadores, tus victorias.", 14));
        et = new EditText(this); et.setHint("¿Cómo estás hoy? ¿Qué sentiste? ¿Qué te ayudó?"); et.setMinLines(4); et.setGravity(android.view.Gravity.TOP); r.addView(et);
        Button g = Ui.button(this, "Guardar entrada", Ui.AZUL); g.setOnClickListener(v -> guardar()); r.addView(g);
        lista = new LinearLayout(this); lista.setOrientation(LinearLayout.VERTICAL); r.addView(lista);
        setContentView(sv); notas = Store.load(this, "diario"); pintar();
    }
    void guardar() {
        String t = et.getText().toString().trim(); if (t.isEmpty()) return;
        String f = new SimpleDateFormat("EEEE d 'de' MMMM yyyy, HH:mm", new Locale("es")).format(new Date());
        notas.add(0, new String[]{f, t}); Store.save(this, "diario", notas); et.setText(""); pintar();
        Toast.makeText(this, "Guardado", Toast.LENGTH_SHORT).show();
    }
    void pintar() {
        lista.removeAllViews();
        for (int k = 0; k < notas.size(); k++) {
            final int idx = k; String[] n = notas.get(k);
            TextView t = Ui.text(this, n[0] + "\n\n" + n[1], 15);
            android.view.View c = Ui.card(this, t, Color.rgb(236, 239, 241));
            c.setOnLongClickListener(v -> { new AlertDialog.Builder(this).setMessage("¿Borrar esta entrada?")
                .setPositiveButton("Borrar", (d, w) -> { notas.remove(idx); Store.save(this, "diario", notas); pintar(); })
                .setNegativeButton("Cancelar", null).show(); return true; });
            lista.addView(c);
        }
        if (!notas.isEmpty()) lista.addView(Ui.text(this, "(Mantén pulsada una entrada para borrarla)", 12));
    }
}
