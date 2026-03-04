package com.teamtodo.app;

import android.app.Application;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.os.Build;
import com.google.firebase.FirebaseApp;

public class TeamTodoApp extends Application {

    public static final String CHANNEL_TODO_ALARM = "todo_alarm";
    public static final String CHANNEL_MORNING_CALL = "morning_call";
    public static final String CHANNEL_GENERAL = "general";

    @Override
    public void onCreate() {
        super.onCreate();
        FirebaseApp.initializeApp(this);
        createNotificationChannels();
    }

    private void createNotificationChannels() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            NotificationManager nm = getSystemService(NotificationManager.class);

            // 할 일 알람 채널
            NotificationChannel todoChannel = new NotificationChannel(
                CHANNEL_TODO_ALARM, "할 일 알람", NotificationManager.IMPORTANCE_HIGH);
            todoChannel.setDescription("설정한 시간에 할 일을 알려줍니다");
            todoChannel.enableVibration(true);
            nm.createNotificationChannel(todoChannel);

            // 모닝콜 채널
            NotificationChannel morningChannel = new NotificationChannel(
                CHANNEL_MORNING_CALL, "모닝콜", NotificationManager.IMPORTANCE_MAX);
            morningChannel.setDescription("매일 아침 기상 알람입니다");
            morningChannel.enableVibration(true);
            nm.createNotificationChannel(morningChannel);

            // 일반 알림 채널
            NotificationChannel generalChannel = new NotificationChannel(
                CHANNEL_GENERAL, "일반 알림", NotificationManager.IMPORTANCE_DEFAULT);
            generalChannel.setDescription("포인트, 따봉 등 일반 알림입니다");
            nm.createNotificationChannel(generalChannel);
        }
    }
}
