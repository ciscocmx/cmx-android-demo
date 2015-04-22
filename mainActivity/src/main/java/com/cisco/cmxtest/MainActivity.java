package com.cisco.cmxtest;

import java.text.DateFormat;
import java.util.Date;

import android.app.Activity;
import android.app.AlertDialog;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.SharedPreferences;
import android.content.SharedPreferences.OnSharedPreferenceChangeListener;
import android.net.ConnectivityManager;
import android.net.NetworkInfo;
import android.net.wifi.WifiInfo;
import android.net.wifi.WifiManager;
import android.os.Bundle;
import android.os.Handler;
import android.os.Message;
import android.preference.PreferenceManager;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.EditText;
import android.widget.ProgressBar;
import android.widget.RatingBar;
import android.widget.RatingBar.OnRatingBarChangeListener;
import android.widget.TextView;
import android.widget.Toast;

import com.cisco.cmx.model.CMXClientLocation;
import com.cisco.cmx.network.CMXClient;
import com.cisco.cmx.network.CMXClientLocationResponseHandler;
import com.cisco.cmx.network.CMXClientRegisteringResponseHandler;
import com.cisco.cmx.ui.CMXClientUi;

public class MainActivity extends Activity implements OnSharedPreferenceChangeListener {

    static final int RESULT_SETTINGS = 1;

    private TextView mServerURL;

    private TextView mNetworkStatus;

    private TextView mWiFiStatus;

    private TextView mRegistrationStatus;

    private TextView mMapLocation;

    private TextView mGeoLocation;

    private TextView mZoneName;

    private TextView mLocationServerTime;

    private TextView mLocationTime;
    
    private TextView mMACADdress;

    private static Handler mHandle;

    private NetworkConnectionCheck mConnectionCheck;

    public static final int LOCATION_UPDATED = 5000;

    private ProgressBar mProgressBar;

    String ratingValue = "";

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        mServerURL = (TextView) findViewById(R.id.txtServerURL);
        mNetworkStatus = (TextView) findViewById(R.id.txtNetworkStatus);
        mWiFiStatus = (TextView) findViewById(R.id.txtWiFiStatus);
        mLocationTime = (TextView) findViewById(R.id.txtLocationTime);
        mMapLocation = (TextView) findViewById(R.id.txtMapLocation);
        mZoneName = (TextView) findViewById(R.id.txtZoneName);
        mLocationServerTime = (TextView) findViewById(R.id.txtServerLocationTime);
        mRegistrationStatus = (TextView) findViewById(R.id.txtRegistrationStatus);

        mRegistrationStatus.setText("Not registered");

        mProgressBar = (ProgressBar) findViewById(R.id.progressBar1);
        mProgressBar.setVisibility(View.INVISIBLE);

        SharedPreferences settings = PreferenceManager.getDefaultSharedPreferences(getBaseContext());
        settings.registerOnSharedPreferenceChangeListener(this);
        
        String serverAddress = settings.getString("prefServerAddress", getResources().getString(R.string.default_settings_server_address));
        mServerURL.setText(serverAddress + " port " + getResources().getString(R.string.default_settings_server_port));

        mConnectionCheck = new NetworkConnectionCheck();
        registerReceiver(mConnectionCheck, new IntentFilter(ConnectivityManager.CONNECTIVITY_ACTION));
        registerReceiver(mConnectionCheck, new IntentFilter(WifiManager.NETWORK_STATE_CHANGED_ACTION));
        registerReceiver(mConnectionCheck, new IntentFilter(WifiManager.WIFI_STATE_CHANGED_ACTION));

        mMACADdress = (TextView) findViewById(R.id.txtMACAddress);
        mMACADdress.setText(getMacAddress());

        mHandle = new Handler() {
            @Override
            public void handleMessage(Message msg) {
            if (msg.what == LOCATION_UPDATED) {
                CMXClientLocation clientLocation = (CMXClientLocation) msg.obj;
                mMapLocation.setText("(" + clientLocation.getMapCoordinate().getX() + " / " + clientLocation.getMapCoordinate().getY() + ")");
                mLocationServerTime.setText(DateFormat.getDateTimeInstance().format(new Date(clientLocation.getLastLocationUpdateTime())));
                mLocationTime.setText(DateFormat.getDateTimeInstance().format(new Date()));

                Log.i("CMX", "X Location: " + clientLocation.getMapCoordinate().getX());
                Log.i("CMX", "Y Location: " + clientLocation.getMapCoordinate().getY());
            }
            super.handleMessage(msg);
            }
        };

        registerAndLoadData();
    }

    private void registerAndLoadData() {
        if (!CMXClient.getInstance().isRegistered()) {
            CMXClient.getInstance().registerClient(new CMXClientRegisteringResponseHandler() {
                @Override
                public void onStart() {
                    super.onStart();
                }

                @Override
                public void onFailure(Throwable error) {
                    mRegistrationStatus.setText("Failed to register");
                }

                @Override
                public void onSuccess() {
                    super.onSuccess();
                    mRegistrationStatus.setText("Registered");
                    startLocationUpdate();
                }
            });
        }
        else {
            mRegistrationStatus.setText("Registered");
            startLocationUpdate();
        }
    }

    private void startLocationUpdate() {
        CMXClient.getInstance().startUserLocationPolling(4, new CMXClientLocationResponseHandler() {
            @Override
            public void onUpdate(CMXClientLocation clientLocation) {
                Message msg = mHandle.obtainMessage();
                msg.what = LOCATION_UPDATED;
                msg.obj = clientLocation;
                mHandle.sendMessage(msg);
            }
        });
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        getMenuInflater().inflate(R.menu.main, menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        switch(item.getItemId()) {
        case R.id.action_settings:
            showSettings();
            break;
        case R.id.action_feedback:
            showFeedback();
            break;
        default:
            return super.onOptionsItemSelected(item);   
        }
        return super.onOptionsItemSelected(item);
    }

    private void showFeedback() {
         AlertDialog.Builder builder = new AlertDialog.Builder(this);
         LayoutInflater inflater =  this.getLayoutInflater();
         View alertView = inflater.inflate(R.layout.feedback, null);
         final EditText feedbackText = (EditText) alertView.findViewById(R.id.txtFeedback);
         final RatingBar rating = (RatingBar) alertView.findViewById(R.id.ratingBar1);
         
         rating.setOnRatingBarChangeListener(new OnRatingBarChangeListener() {
             
            @Override
            public void onRatingChanged(RatingBar ratingBar, float rating, boolean fromUser) {
                ratingValue = String.valueOf(rating);
            }
        });
         builder.setTitle("Feedback");
         builder.setView(alertView);
         builder.setPositiveButton("OK", new DialogInterface.OnClickListener() {
             @Override
             public void onClick(DialogInterface dialog, int id) {
                 Toast abc = Toast.makeText(getApplicationContext(), feedbackText.getText() + " | " + ratingValue, Toast.LENGTH_SHORT);
                 abc.show();
             }
         });
         
         builder.setNegativeButton("Cancel", new DialogInterface.OnClickListener() {
             public void onClick(DialogInterface dialog, int id) {
                 dialog.cancel();
             }
         });
         
         builder.create().show();
         
    }

    @Override
    protected void onStart() {
        super.onStart();
    }

    @Override
    public void onDestroy() {
        super.onDestroy();

        // Unregister preferences listener
        SharedPreferences settings = PreferenceManager.getDefaultSharedPreferences(getBaseContext());
        settings.unregisterOnSharedPreferenceChangeListener(this);

        unregisterReceiver(mConnectionCheck);
        mConnectionCheck = null;

        CMXClient.getInstance().stopUserLocationPolling();
    }

    private void showSettings() {
        Intent i = new Intent(this, SettingsActivity.class);
        this.startActivityForResult(i, RESULT_SETTINGS);
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        switch (resultCode) {
            case RESULT_SETTINGS:
                CMXClient.getInstance().setConfiguration(((TestApp) this.getApplication()).getConfiguration());
        }
    }

    public void showMap(View v) {
        CMXClientUi.getInstance().showMapView();
    }

    @Override
    public void onSharedPreferenceChanged(SharedPreferences sharedPreferences, String key) {
        if (key.equals("prefServerAddress")) {
            String serverAddress = sharedPreferences.getString("prefServerAddress", getResources().getString(R.string.default_settings_server_address));
            String serverText = serverAddress + ":" + getResources().getString(R.string.default_settings_server_port);
            mServerURL.setText(serverText);
        }
    }

    private String getMacAddress() {
        WifiManager wifiMgr = (WifiManager) this.getSystemService(Context.WIFI_SERVICE);
        WifiInfo wifiInfo = wifiMgr.getConnectionInfo();
        return wifiInfo != null ? wifiInfo.getMacAddress() : null;
    }

    private class NetworkConnectionCheck extends BroadcastReceiver {

        @Override
        public void onReceive(Context context, Intent intent) {
            if (intent.getAction().equals(ConnectivityManager.CONNECTIVITY_ACTION)) {
                boolean noConnectivity = intent.getBooleanExtra(ConnectivityManager.EXTRA_NO_CONNECTIVITY, false);
                if (noConnectivity) {
                    mNetworkStatus.setText("Not Connected");
                }
                else {
                    ConnectivityManager cm = (ConnectivityManager) getSystemService(Context.CONNECTIVITY_SERVICE);
                    NetworkInfo network = cm.getActiveNetworkInfo();
                    if (network != null && network.isConnected()) {
                        mNetworkStatus.setText("Connected");
                    }
                }
            }
            else if (intent.getAction().equals(WifiManager.NETWORK_STATE_CHANGED_ACTION)) {
                NetworkInfo networkInfo = intent.getParcelableExtra(WifiManager.EXTRA_NETWORK_INFO);
                if (networkInfo.isConnected()) {
                    mNetworkStatus.setText("Connected");
                }
            }
            else if (intent.getAction().equals(WifiManager.WIFI_STATE_CHANGED_ACTION)) {
                int extraWifiState = intent.getIntExtra(WifiManager.EXTRA_WIFI_STATE, WifiManager.WIFI_STATE_UNKNOWN);

                switch (extraWifiState) {
                    case WifiManager.WIFI_STATE_DISABLED:
                        mWiFiStatus.setText("Wi Fi disabled");
                        break;
                    case WifiManager.WIFI_STATE_ENABLED:
                        mWiFiStatus.setText("Wi Fi Enabled");
                        break;
                }
            }
        }
    }
}
