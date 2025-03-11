package com.example.mentalhealthdiary;

import android.app.Application;
import com.example.mentalhealthdiary.config.RemoteConfig;

public class MentalHealthApp extends Application {
    @Override
    public void onCreate() {
        super.onCreate();
        RemoteConfig.init(this);
    }
} 