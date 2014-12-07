package com.group15.toq_o.present;

import android.app.Application;
import com.group15.toq_o.present.Presentation.Presentation;

import java.util.HashMap;
import java.util.Date;

/**
 * Created by weili on 11/16/14.
 */
public class PresentApplication extends Application {

    private HashMap<String,Presentation> presentationHashMap;
    private Date driveAccessDate;

    @Override
    public void onCreate() {
        super.onCreate();
        reset();
    }

    private void reset() {
        presentationHashMap = new HashMap<String, Presentation>();
        driveAccessDate = new Date();
    }

    protected HashMap<String, Presentation> getPresentationHashMap() {
        return presentationHashMap;
    }

    protected void addToPresentationHashMap(String key, Presentation value) { presentationHashMap.put(key, value); }

    protected Date getDriveAccessDate() {
        return driveAccessDate;
    }

    protected void setDriveAccessDate(Date date) {
        this.driveAccessDate = date;
    }
}
