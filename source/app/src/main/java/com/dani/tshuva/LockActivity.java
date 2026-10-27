package com.dani.tshuva;

import android.app.*; import android.content.*; import android.graphics.*; import android.hardware.biometrics.*; import android.os.*; import android.text.*; import android.view.*; import android.widget.*;

/** Pantalla de bloqueo: crear PIN, desbloquear (PIN o huella), cambiar PIN y borrado total si se olvida. */
public class LockActivity extends Activity {
    static final int UNLOCK = 0, SETUP = 1, CHANGE = 2;
    int mode, fallos; String primero; TextView titulo, msg; EditText pin; Button ok, huella;

    @Override protected void onCreate(Bundle b) {
        super.onCreate(b); Base.secure(this);
        mode = getIntent().getIntExtra("modo", Lock.hasPin(this) ? UNLOCK : SETUP);
        LinearLayout r = Ui.col(this); r.setGravity(Gravity.CENTER_HORIZONTAL);
        TextView icono = Ui.text(this, "🎚", 48); icono.setGravity(Gravity.CENTER); r.addView(icono);
        titulo = Ui.text(this, "", 22); titulo.setGravity(Gravity.CENTER); titulo.setTypeface(null, Typeface.BOLD); r.addView(titulo);
        msg = Ui.text(this, "", 14); msg.setGravity(Gravity.CENTER); r.addView(msg);
        pin = new EditText(this); pin.setInputType(InputType.TYPE_CLASS_NUMBER | InputType.TYPE_NUMBER_VARIATION_PASSWORD);
        pin.setFilters(new InputFilter[]{new InputFilter.LengthFilter(6)}); pin.setGravity(Gravity.CENTER); pin.setTextSize(28); pin.setHint("••••");
        r.addView(pin, new LinearLayout.LayoutParams(Ui.dp(this, 200), -2));
        pin.setOnEditorActionListener((v, a, e) -> { aceptar(); return true; });
        ok = Ui.button(this, "Aceptar", Ui.AZUL); ok.setOnClickListener(v -> aceptar()); r.addView(ok);
        huella = Ui.button(this, "Usar huella", Color.rgb(69, 90, 100)); huella.setOnClickListener(v -> biometria()); r.addView(huella);
        TextView olvide = Ui.text(this, "¿Olvidaste tu PIN?", 14); olvide.setTextColor(Color.rgb(198, 40, 40)); olvide.setGravity(Gravity.CENTER);
        olvide.setOnClickListener(v -> olvide()); r.addView(olvide);
        setContentView(r);
        pintar(); olvide.setVisibility(mode == UNLOCK ? View.VISIBLE : View.GONE);
        if (mode == UNLOCK && biometriaDisponible()) biometria();
    }
    void pintar() {
        huella.setVisibility(mode == UNLOCK && biometriaDisponible() ? View.VISIBLE : View.GONE);
        if (mode == SETUP && primero == null) { titulo.setText("Crea tu PIN"); msg.setText("Elige un PIN de 4 a 6 dígitos para proteger la app."); }
        else if (primero != null) { titulo.setText("Confirma tu PIN"); msg.setText("Escríbelo otra vez."); }
        else if (mode == CHANGE) { titulo.setText("PIN actual"); msg.setText("Escribe tu PIN actual."); }
        else { titulo.setText("Afinador"); msg.setText("Introduce tu PIN"); }
    }
    boolean biometriaDisponible() {
        if (Build.VERSION.SDK_INT < 28 || !Lock.biometricOn(this)) return false;
        if (Build.VERSION.SDK_INT >= 29) { BiometricManager bm = getSystemService(BiometricManager.class); return bm != null && bm.canAuthenticate() == BiometricManager.BIOMETRIC_SUCCESS; }
        return getPackageManager().hasSystemFeature("android.hardware.fingerprint");
    }
    void biometria() {
        if (Build.VERSION.SDK_INT < 28) return;
        new BiometricPrompt.Builder(this).setTitle("Afinador").setSubtitle("Desbloquea con tu huella")
            .setNegativeButton("Usar PIN", getMainExecutor(), (d, w) -> {}).build()
            .authenticate(new CancellationSignal(), getMainExecutor(), new BiometricPrompt.AuthenticationCallback() {
                @Override public void onAuthenticationSucceeded(BiometricPrompt.AuthenticationResult r) { abrir(); }
            });
    }
    void aceptar() {
        String p = pin.getText().toString(); pin.setText("");
        if (mode == UNLOCK) {
            if (Lock.check(this, p)) abrir();
            else { fallos++; msg.setText("PIN incorrecto" + (fallos >= 5 ? ". Espera 30 segundos." : "")); if (fallos >= 5) { ok.setEnabled(false); new Handler(Looper.getMainLooper()).postDelayed(() -> { ok.setEnabled(true); fallos = 0; msg.setText("Introduce tu PIN"); }, 30000); } }
            return;
        }
        if (mode == CHANGE && primero == null && !"verificado".equals(msg.getTag())) {
            if (Lock.check(this, p)) { msg.setTag("verificado"); titulo.setText("Nuevo PIN"); msg.setText("Elige un PIN nuevo de 4 a 6 dígitos."); }
            else msg.setText("PIN incorrecto");
            return;
        }
        if (primero == null) {
            if (!Lock.validFormat(p)) { msg.setText("El PIN debe tener de 4 a 6 dígitos."); return; }
            primero = p; pintar(); return;
        }
        if (!primero.equals(p)) { primero = null; pintar(); msg.setText("No coinciden. Vuelve a empezar."); if (mode == CHANGE) titulo.setText("Nuevo PIN"); return; }
        Lock.setPin(this, p); Toast.makeText(this, "PIN guardado", Toast.LENGTH_SHORT).show(); abrir();
    }
    void abrir() { Lock.unlocked = true; finish(); overridePendingTransition(0, 0); }
    void olvide() {
        new AlertDialog.Builder(this).setTitle("Restablecer la app")
            .setMessage("No hay forma de recuperar el PIN. Para volver a usar la app hay que BORRAR TODOS SUS DATOS: diario, contador, chat, memoria, perfil, clave de Google y ajustes. Esto no se puede deshacer.")
            .setPositiveButton("Continuar", (d, w) -> new AlertDialog.Builder(this).setTitle("¿Seguro?").setMessage("Se borrará todo definitivamente.")
                .setPositiveButton("Borrar todo", (d2, w2) -> borrarTodo()).setNegativeButton("Cancelar", null).show())
            .setNegativeButton("Cancelar", null).show();
    }
    void borrarTodo() {
        getSystemService(AlarmManager.class).cancel(DailyReceiver.pi(this));
        getSystemService(NotificationManager.class).cancelAll();
        getSystemService(ActivityManager.class).clearApplicationUserData();   // borra todo y cierra la app
    }
    @Override public void onBackPressed() { if (mode == CHANGE) { super.onBackPressed(); return; } finishAffinity(); }
}
