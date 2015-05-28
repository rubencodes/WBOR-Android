package com.ruben.wbor.app;

import android.media.AudioManager;
import android.media.MediaPlayer;
import android.os.AsyncTask;
import android.os.Bundle;
import android.support.v7.app.ActionBarActivity;
import android.view.View;
import android.view.animation.Animation;
import android.view.animation.LinearInterpolator;
import android.view.animation.RotateAnimation;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.TextView;

import org.apache.http.HttpEntity;
import org.apache.http.HttpResponse;
import org.apache.http.StatusLine;
import org.apache.http.client.HttpClient;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.impl.client.DefaultHttpClient;
import org.json.JSONException;
import org.json.JSONObject;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.util.Timer;
import java.util.TimerTask;

public class Home extends ActionBarActivity {
    MediaPlayer mediaPlayer = new MediaPlayer();
    Timer getWBORinfo;
    boolean playDisabled = false;
    boolean stopDisabled = true;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        this.setContentView(R.layout.activity_home);

        final TextView songText = (TextView) findViewById(R.id.textView);
        final TextView artistText = (TextView) findViewById(R.id.textView2);
        final Button playButton = (Button) findViewById(R.id.button);
        final Button stopButton = (Button) findViewById(R.id.button2);
        final ImageView record = (ImageView) findViewById(R.id.imageView2);

        playButton.setOnClickListener(new View.OnClickListener() {
            public void onClick(View v) {
                if(!playDisabled) {
                    playDisabled = true;

                    new LoadStream().execute();
                }
            }
        });

        stopButton.setOnClickListener(new View.OnClickListener() {
            public void onClick(View v) {
                if(!stopDisabled) {
                    stopDisabled = true;
                    record.setAnimation(null);
                    playButton.setBackgroundResource(R.drawable.play);
                    stopButton.setBackgroundResource(R.drawable.pause2);

                    mediaPlayer.stop();
                    mediaPlayer.reset();
                    getWBORinfo.cancel();
                    getWBORinfo.purge();

                    songText.setText("");
                    artistText.setText("");
                    playDisabled = false;
                }
            }
        });
    }

    private class LoadStream extends AsyncTask<Void, Void, Void> {
        @Override
        protected void onPreExecute() {
            TextView songText = (TextView) findViewById(R.id.textView);
            TextView artistText = (TextView) findViewById(R.id.textView2);
            Button playButton = (Button) findViewById(R.id.button);
            Button stopButton = (Button) findViewById(R.id.button2);
            ImageView record = (ImageView) findViewById(R.id.imageView2);

            //initializing media player, set to defaults
            songText.setText("Buffering...");
            artistText.setText("");
            RotateAnimation anim = new RotateAnimation(0.0f, 360.0f, Animation.RELATIVE_TO_SELF, 0.5f, Animation.RELATIVE_TO_SELF, 0.5f);

            playButton.setBackgroundResource(R.drawable.play2);
            stopButton.setBackgroundResource(R.drawable.pause);
            anim.setInterpolator(new LinearInterpolator());
            anim.setRepeatCount(Animation.INFINITE); //Repeat animation indefinitely
            anim.setDuration(1200); //Put desired duration per anim cycle here, in milliseconds
            record.startAnimation(anim);
        }

        @Override
        protected Void doInBackground(Void... voids) {
            String url = "http://139.140.232.18:8000/WBOR"; // URL of stream

            mediaPlayer.setAudioStreamType(AudioManager.STREAM_MUSIC);
            mediaPlayer.setOnPreparedListener(new MediaPlayer.OnPreparedListener() {
                @Override
                public void onPrepared(MediaPlayer mediaPlayer) {
                    mediaPlayer.start();
                    getWBORinfo = new Timer();
                    getWBORinfo.scheduleAtFixedRate(new TimerTask() {
                        @Override
                        public void run() {
                            new getWBORFeed().execute();
                        }
                    }, 0, 60000);
                    stopDisabled = false;
                }
            });

            try {
                mediaPlayer.setDataSource(url);
                mediaPlayer.prepare(); // might take long! (for buffering, etc)
            } catch (IOException e) {
                e.printStackTrace();
            }

            return null;
        }

        @Override
        protected void onPostExecute(Void aVoid) {

        }
    }

    private class getWBORFeed extends AsyncTask {
        protected void onPostExecute(Object result) {
            String jsonString = (String) result;
            try {
                JSONObject myJSON = new JSONObject(jsonString);

                String song = myJSON.getString("song_string");
                String artist = myJSON.getString("artist_string");
                String show = myJSON.getString("program_title");

                TextView songText = (TextView) findViewById(R.id.textView);
                TextView artistText = (TextView) findViewById(R.id.textView2);

                songText.setText(song);
                artistText.setText(artist);

            } catch (JSONException e) {
                e.printStackTrace();
            }
        }

        protected String doInBackground(Object[] objects) {
            StringBuilder builder = new StringBuilder();
            HttpClient client = new DefaultHttpClient();
            HttpGet httpGet = new HttpGet("http://wbor-hr.appspot.com/updateinfo");
            try {
                HttpResponse response = client.execute(httpGet);
                StatusLine statusLine = response.getStatusLine();
                int statusCode = statusLine.getStatusCode();
                if (statusCode == 200) {
                    HttpEntity entity = response.getEntity();
                    InputStream content = entity.getContent();
                    BufferedReader reader = new BufferedReader(new InputStreamReader(content));
                    String line;
                    while ((line = reader.readLine()) != null) {
                        builder.append(line);
                    }
                } else return "Network Error";
            } catch (Exception e) {
                return "Network Error";
            }
            return builder.toString();
        }
    }
}
