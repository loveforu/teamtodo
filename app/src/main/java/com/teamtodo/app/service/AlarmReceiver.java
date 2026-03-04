package com.teamtodo.app.service;

import android.app.NotificationManager;
import android.app.PendingIntent;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import androidx.core.app.NotificationCompat;
import com.teamtodo.app.R;
import com.teamtodo.app.TeamTodoApp;
import com.teamtodo.app.ui.MainActivity;
import java.util.Random;

public class AlarmReceiver extends BroadcastReceiver {

    @Override
    public void onReceive(Context context, Intent intent) {
        String action = intent.getAction();

        // 부팅 완료 시 알람 재등록 (TODO: SharedPrefs에서 알람 목록 읽어서 재등록)
        if (Intent.ACTION_BOOT_COMPLETED.equals(action)) {
            return;
        }

        String alarmId = intent.getStringExtra("alarmId");
        String title = intent.getStringExtra("title");
        boolean isMorningCall = intent.getBooleanExtra("isMorningCall", false);
        String channelId = intent.getStringExtra("channelId");
        if (channelId == null) channelId = TeamTodoApp.CHANNEL_TODO_ALARM;

        // 앱 열기 인텐트
        Intent openIntent = new Intent(context, MainActivity.class);
        openIntent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TOP);
        PendingIntent pendingIntent = PendingIntent.getActivity(
            context, 0, openIntent, PendingIntent.FLAG_UPDATE_CURRENT | PendingIntent.FLAG_IMMUTABLE);

        String notifTitle = isMorningCall ? "🌅 좋은 아침이에요!" : "⏰ 할 일 알림";
        String notifBody = title != null ? title : "할 일을 확인하세요!";

        NotificationCompat.Builder builder = new NotificationCompat.Builder(context, channelId)
            .setSmallIcon(R.mipmap.ic_launcher)
            .setContentTitle(notifTitle)
            .setContentText(notifBody)
            .setAutoCancel(true)
            .setPriority(isMorningCall ?
                NotificationCompat.PRIORITY_MAX : NotificationCompat.PRIORITY_HIGH)
            .setContentIntent(pendingIntent);

        NotificationManager nm = (NotificationManager) context.getSystemService(Context.NOTIFICATION_SERVICE);
        nm.notify(new Random().nextInt(10000), builder.build());
    }
}
