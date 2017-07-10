package com.example.hannes.garage;

import android.content.Context;
import android.media.AudioManager;
import android.media.MediaPlayer;
import android.net.ConnectivityManager;
import android.net.Network;
import android.net.NetworkCapabilities;
import android.os.AsyncTask;
import android.os.Bundle;
import android.os.CountDownTimer;
import android.os.Handler;
import android.provider.Settings;
import android.provider.SyncStateContract;
import android.support.annotation.RawRes;
import android.support.design.widget.FloatingActionButton;
import android.support.design.widget.Snackbar;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.Toolbar;
import android.util.Log;
import android.view.View;
import android.view.Menu;
import android.view.MenuItem;
import android.widget.TextView;

import com.tinkerforge.AlreadyConnectedException;
import com.tinkerforge.BrickletDualRelay;
import com.tinkerforge.IPConnection;
import com.tinkerforge.NetworkException;
import com.tinkerforge.NotConnectedException;
import com.tinkerforge.TimeoutException;


import java.util.prefs.Preferences;

import static android.support.constraint.solver.SolverVariable.Type.CONSTANT;


public class MainActivity extends AppCompatActivity {
    AudioManager am;
    private boolean garageAktuateOk = false;
    private IPConnection ipcon_garage;
    private BrickletDualRelay dr;
    private String TAG;
    private int errorCode = 0;
    private Functions functions = new Functions();
    private TextView tvSecondsToTerminate;
    private CountDownTimer doorTimer;
    private float volume = 0.9f;
    private MediaPlayer mPlayer;
    private AudioManager audioManager;
    int count = 0;
    int maxtries = 5;


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        Toolbar toolbar = (Toolbar) findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);
        ipcon_garage = new IPConnection();
        dr = new BrickletDualRelay(getString(R.string.uid_garage_dual_relay), ipcon_garage);
        tvSecondsToTerminate = (TextView) findViewById(R.id.tvSeconds);
        TAG = "Carage";
        audioManager = (AudioManager) getSystemService(Context.AUDIO_SERVICE);

        new AktuateGarage().execute("");
        doorTimer = new CountDownTimer(30000, 1000) {

            public void onTick(long millisUntilFinished) {
                tvSecondsToTerminate.setText("" + millisUntilFinished / 1000);
            }
            public void onFinish() {
               // PlaySound(R.raw.error_max);
           //     finish();
            }
        }.start();

        Log.i(TAG, "pj_back");
    }



    private class AktuateGarage extends AsyncTask<String, Void, String> {
        @Override
        protected String doInBackground(String... params) {


            while (!garageAktuateOk && count++ <= maxtries) {
                Log.i(TAG, "pj_innerwhile" + count + ":::");
                try {
                    ipcon_garage.connect(getString(R.string.host_Garage), getResources().getInteger(R.integer.port_garage));
                    dr.setMonoflop((short) 1, true, (long) 500);
                    ipcon_garage.disconnect();
                    garageAktuateOk = true;
                } catch (NotConnectedException e) {
                    Log.i(TAG, "pj_notconnectedExc" + count + ":::" + e);
                        errorCode = 10;
                    /*if (count >= 5) {
                        errorCode = 4711;
                    }*/
                    //return null;
                } catch (AlreadyConnectedException a){
                    errorCode = 11;
                    Log.i(TAG, "pj_allreadyconnectedExc" + count + ":::" + a);
                } catch (NetworkException n){
                    Log.i(TAG, "pj_NetworkExcExc" + count + ":::" + n);
                    errorCode = 12;
                } catch (TimeoutException t){
                    Log.i(TAG, "pj_TimeoutExcExc" + count + ":::" + t);
                    errorCode = 13;
                }
            }

            return null;
        }

        @Override
        protected void onPostExecute(String result) {
            Log.i(TAG, "pj_PostExecute:: errorCode: " + errorCode + " garageAktuateOK: " + garageAktuateOk);
            if (count == maxtries) {
                if (errorCode == 10) {
                    PlaySound(R.raw.fehler_nicht_verbunden);

                } else if (errorCode == 11) {
                    PlaySound(R.raw.error_allready_connected);
                    finish();
                } else if (errorCode == 12) {
                    PlaySound(R.raw.fehler_netzwerk_ausnahme);
                    finish();
                } else if (errorCode == 13) {
                    PlaySound(R.raw.fehler_tiemout);
                    finish();
                } else if (garageAktuateOk) {
                    PlaySound(R.raw.garage_oeffnet);
                    finish();
                } else {
                    Log.i(TAG, "pj_da hats was::: ");
                }
            } else if (garageAktuateOk){
                PlaySound(R.raw.garage_oeffnet);
                finish();
            } else if (!garageAktuateOk){
                try {
                    Thread.sleep(2000);
                } catch (Exception e){
                    Log.i(TAG, "pj_da hats was:::: " + e );
                }
            }

        }

        @Override
        protected void onPreExecute() {
        }

        @Override
        protected void onProgressUpdate(Void... values) {
            Log.i(TAG, "pj_ProgressUpdate:: errorCode: " + errorCode + " garageAktuateOK: " + garageAktuateOk);
        }
    }



    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        // Inflate the menu; this adds items to the action bar if it is present.
        getMenuInflater().inflate(R.menu.menu_main, menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        // Handle action bar item clicks here. The action bar will
        // automatically handle clicks on the Home/Up button, so long
        // as you specify a parent activity in AndroidManifest.xml.
        int id = item.getItemId();

        //noinspection SimplifiableIfStatement
        if (id == R.id.action_settings) {
            return true;
        }

        return super.onOptionsItemSelected(item);
    }
    private void PlaySound(int soundFileID){

       /* mPlayer.selectTrack(soundFileID);
        audioManager.setStreamVolume(AudioManager.STREAM_MUSIC, 1000, 0);
        */

        audioManager.setMode(audioManager.MODE_IN_COMMUNICATION);
        audioManager.setBluetoothScoOn(true);
        audioManager.startBluetoothSco();
        audioManager.setSpeakerphoneOn(false);

        mPlayer = MediaPlayer.create(getApplicationContext(), soundFileID);
        mPlayer.start();

        while (mPlayer.isPlaying()){
            try {
                Thread.sleep(1000);
            } catch (Exception e){
                Log.i(TAG, "PJ_Da hats was::: " + e);
            }

        }
        mPlayer.release();
    }
}
