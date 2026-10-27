package com.dani.tshuva;

import android.app.*; import android.content.*; import android.os.*; import android.view.*;

/** Actividad base: oculta contenido en recientes, título discreto y bloqueo con PIN/huella. */
public class Base extends Activity {
    static int started; static long backgroundSince;
    static void secure(Activity a) {
        a.getWindow().setFlags(WindowManager.LayoutParams.FLAG_SECURE, WindowManager.LayoutParams.FLAG_SECURE);
        a.setTaskDescription(new ActivityManager.TaskDescription("Afinador"));
    }
    @Override protected void onCreate(Bundle b) { super.onCreate(b); secure(this); }
    @Override protected void onStart() {
        super.onStart();
        if (started == 0 && backgroundSince > 0 && System.currentTimeMillis() - backgroundSince > Lock.TIMEOUT_MS) Lock.unlocked = false;
        started++;
    }
    @Override protected void onResume() {
        super.onResume();
        if (!Lock.unlocked) startActivity(new Intent(this, LockActivity.class).addFlags(Intent.FLAG_ACTIVITY_NO_ANIMATION));
    }
    @Override protected void onStop() { super.onStop(); started--; if (started <= 0) { started = 0; backgroundSince = System.currentTimeMillis(); } }
}
