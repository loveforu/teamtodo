package com.teamtodo.app.service;

import android.app.AlarmManager;
import android.app.PendingIntent;
import android.content.Context;
import android.content.Intent;
import android.os.Build;
import com.teamtodo.app.TeamTodoApp;

public class TodoAlarmManager {

    private final Context context;
    private final android.app.AlarmManager systemAlarmManager;

    public TodoAlarmManager(Context context) {
        this.context = context;
        this.systemAlarmManager = (android.app.AlarmManager) context.getSystemService(Context.ALARM_SERVICE);
    }

    public void setAlarm(String alarmId, String title, long timeMillis, boolean isMorningCall) {
        Intent intent = new Intent(context, AlarmReceiver.class);
        intent.putExtra("alarmId", alarmId);
        intent.putExtra("title", title);
        intent.putExtra("isMorningCall", isMorningCall);
        intent.putExtra("channelId", isMorningCall ?
            TeamTodoApp.CHANNEL_MORNING_CALL : TeamTodoApp.CHANNEL_TODO_ALARM);

        int requestCode = Math.abs(alarmId.hashCode());
        PendingIntent pendingIntent = PendingIntent.getBroadcast(
            context, requestCode, intent,
            PendingIntent.FLAG_UPDATE_CURRENT | PendingIntent.FLAG_IMMUTABLE);

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            if (systemAlarmManager.canScheduleExactAlarms()) {
                systemAlarmManager.setExactAndAllowWhileIdle(
                    AlarmManager.RTC_WAKEUP, timeMillis, pendingIntent);
            } else {
                systemAlarmManager.setAndAllowWhileIdle(
                    AlarmManager.RTC_WAKEUP, timeMillis, pendingIntent);
            }
        } else {
            systemAlarmManager.setExactAndAllowWhileIdle(
                AlarmManager.RTC_WAKEUP, timeMillis, pendingIntent);
        }
    }

    public void cancelAlarm(String alarmId) {
        Intent intent = new Intent(context, AlarmReceiver.class);
        int requestCode = Math.abs(alarmId.hashCode());
        PendingIntent pendingIntent = PendingIntent.getBroadcast(
            context, requestCode, intent,
            PendingIntent.FLAG_UPDATE_CURRENT | PendingIntent.FLAG_IMMUTABLE);
        systemAlarmManager.cancel(pendingIntent);
    }
}
