package com.example.hannes.garage;

import android.content.Context;
import android.media.AudioManager;
import android.media.MediaPlayer;
import android.os.AsyncTask;
import android.os.Bundle;
import android.os.CountDownTimer;
import android.provider.MediaStore;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.Toolbar;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;
import android.widget.TextView;
import android.widget.Toast;

import com.tinkerforge.AlreadyConnectedException;
import com.tinkerforge.BrickletDualRelay;
import com.tinkerforge.IPConnection;
import com.tinkerforge.NetworkException;
import com.tinkerforge.NotConnectedException;
import com.tinkerforge.TimeoutException;

public class MainActivity extends AppCompatActivity {
    //AudioManager am;
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
    private int count = 0;
    private int maxtries = 5;
    private boolean finishing = false;



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
                finishing = true;
                PlaySound(R.raw.error_max, MainActivity.this);

                finishAndRemoveTask();
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
                if (!garageAktuateOk && count <= maxtries){
                    try {
                        Thread.sleep(2000);
                    } catch (Exception e) {
                        Log.i(TAG, "pj_Thread_Sleep" + count + ":::" + e);
                    }
                }
            }
            return null;
        }

        @Override
        protected void onPostExecute(String result) {
            Log.i(TAG, "pj_PostExecute:: errorCode: " + errorCode + " garageAktuateOK: " + garageAktuateOk);
            if (count == maxtries) {
                if (errorCode == 10) {
                    finishing = true;
                    PlaySound(R.raw.fehler_nicht_verbunden, MainActivity.this);
                } else if (errorCode == 11) {
                    finishing = true;
                    PlaySound(R.raw.error_allready_connected, MainActivity.this);
                } else if (errorCode == 12) {
                    finishing = true;
                    PlaySound(R.raw.fehler_netzwerk_ausnahme, MainActivity.this);
                } else if (errorCode == 13) {
                    finishing = true;
                    PlaySound(R.raw.fehler_tiemout, MainActivity.this);
                } else if (garageAktuateOk) {
                    finishing = true;
                    PlaySound(R.raw.garage_oeffnet, MainActivity.this);
                } else {
                    Log.i(TAG, "pj_da hats was::: ");
                }
            } else if (garageAktuateOk){
                PlaySound(R.raw.garage_oeffnet, MainActivity.this);
                finishing = true;
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
    private void PlaySound(int soundFileID, final Context context){

        boolean gotFocus = requestAudioFocusForMyApp();

        if(gotFocus) {
            audioManager.setMode(0);
            audioManager.setBluetoothScoOn(true);
            audioManager.startBluetoothSco();

            if (audioManager.isMusicActive()){
                audioManager.setMode(AudioManager.AUDIOFOCUS_GAIN_TRANSIENT_MAY_DUCK);
            } else {
                audioManager.setMode(AudioManager.MODE_IN_COMMUNICATION);
                audioManager.setStreamVolume(AudioManager.MODE_IN_COMMUNICATION, audioManager.getStreamMaxVolume(AudioManager.MODE_IN_COMMUNICATION), 0);
            }

            mPlayer = MediaPlayer.create(getApplicationContext(), soundFileID);
            mPlayer.setOnCompletionListener(new MediaPlayer.OnCompletionListener(){
                public void onCompletion(MediaPlayer player){

                    audioManager.setMode(AudioManager.MODE_NORMAL);
                    audioManager.abandonAudioFocus(null);

                  //  mPlayer.reset();
                   // mPlayer.release();
                    if (finishing){
                        try {
                            Thread.sleep(2000);
                        } catch (Exception e) {
                            Log.i(TAG, "pj_da hats was::: " + e);
                        }
                        doorTimer.cancel();
                        finishAndRemoveTask();
                    }

                }
            });

            if(!audioManager.isMusicActive()) {
                try {
                    Thread.sleep(2000); // warte 2 Sekunden, ansonsten ist der TelefonCall noch nicht bereit und die Ausgabe wird verschluckt
                } catch (Exception e) {
                    Log.i(TAG, "pj_da hats was::: " + e);
                }
            }
            mPlayer.start();
        }
    }
    private boolean requestAudioFocusForMyApp() {
        int result = audioManager.requestAudioFocus(null, AudioManager.MODE_IN_COMMUNICATION, AudioManager.AUDIOFOCUS_GAIN_TRANSIENT);

        if (result == AudioManager.AUDIOFOCUS_REQUEST_GRANTED) {
            Log.d("AudioFocus", "Audio focus received");
            return true;
        } else {
            Log.d("AudioFocus", "Audio focus NOT received");
            return false;
        }
    }
}
