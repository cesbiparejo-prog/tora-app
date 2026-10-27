package com.dani.tshuva;

import android.app.*; import android.content.*; import java.util.*;

public class DailyReceiver extends BroadcastReceiver {
    static final String CH = "recordatorio";
    static PendingIntent pi(Context c) { return PendingIntent.getBroadcast(c, 1, new Intent(c, DailyReceiver.class), PendingIntent.FLAG_IMMUTABLE | PendingIntent.FLAG_UPDATE_CURRENT); }

    public static void schedule(Context c) {
        AlarmManager am = c.getSystemService(AlarmManager.class);
        am.cancel(pi(c));
        if (!Store.notifOn(c)) return;
        Calendar t = Calendar.getInstance();
        t.set(Calendar.HOUR_OF_DAY, Store.hour(c)); t.set(Calendar.MINUTE, Store.minute(c)); t.set(Calendar.SECOND, 0); t.set(Calendar.MILLISECOND, 0);
        if (t.getTimeInMillis() <= System.currentTimeMillis() + 1000) t.add(Calendar.DAY_OF_YEAR, 1);
        am.setAndAllowWhileIdle(AlarmManager.RTC_WAKEUP, t.getTimeInMillis(), pi(c));
    }

    @Override public void onReceive(Context c, Intent i) {
        NotificationManager nm = c.getSystemService(NotificationManager.class);
        nm.deleteNotificationChannel("aliento");
        nm.createNotificationChannel(new NotificationChannel(CH, "Recordatorios", NotificationManager.IMPORTANCE_DEFAULT));
        PendingIntent open = PendingIntent.getActivity(c, 0, new Intent(c, MainActivity.class), PendingIntent.FLAG_IMMUTABLE);
        Notification n = new Notification.Builder(c, CH).setSmallIcon(R.drawable.ic_notif)
            .setContentTitle("Afinador").setContentText("Tienes un recordatorio")
            .setVisibility(Notification.VISIBILITY_PRIVATE).setContentIntent(open).setAutoCancel(true).build();
        try { nm.notify(7, n); } catch (SecurityException ignored) {}
        schedule(c);
    }
}
