package com.cisco.cmxtest;

import java.net.MalformedURLException;
import com.cisco.cmx.network.CMXClient;

import android.app.Application;
import android.content.SharedPreferences;
import android.preference.PreferenceManager;

public class TestApp extends Application {

    @Override
    public void onCreate() {
        super.onCreate();

        PreferenceManager.setDefaultValues(this, R.xml.settings, true);

        CMXClient.getInstance().initialize(this);
        CMXClient.getInstance().setConfiguration(getConfiguration());
    }

    public CMXClient.Configuration getConfiguration() {
        SharedPreferences settings = PreferenceManager.getDefaultSharedPreferences(getBaseContext());

        String serverAddress = settings.getString("prefServerAddress", getResources().getString(R.string.default_settings_server_address));

        try {
            CMXClient.Configuration config = new CMXClient.Configuration();
            config.setServerAddress(serverAddress);
            config.setServerPort(getResources().getString(R.string.default_settings_server_port));
            config.setSenderId(getResources().getString(R.string.default_settings_senderid));
            return config;
        }
        catch (MalformedURLException e) {
            e.printStackTrace();
        }
        return null;
    }
}