package com.ssoftwares.voicerecorderapp;

import android.Manifest;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.media.MediaPlayer;
import android.media.MediaRecorder;
import android.net.Uri;
import android.os.Build;
import android.os.Environment;
import android.os.Handler;
import android.os.SystemClock;

import android.os.Bundle;

import android.transition.TransitionManager;
import android.util.Base64;
import android.util.Log;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.widget.Chronometer;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.SeekBar;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.RequiresApi;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.Toolbar;
import androidx.core.content.ContextCompat;

import com.android.volley.AuthFailureError;
import com.android.volley.Request;
import com.android.volley.RequestQueue;
import com.android.volley.Response;
import com.android.volley.VolleyError;
import com.android.volley.toolbox.JsonObjectRequest;
import com.android.volley.toolbox.Volley;
import com.google.gson.Gson;

import com.ssoftwares.voicerecorderapp.models.Assistant;
import com.ssoftwares.voicerecorderapp.models.Audio;
import com.ssoftwares.voicerecorderapp.models.Config;
import com.ssoftwares.voicerecorderapp.models.SpeechData;
import com.ssoftwares.voicerecorderapp.models.SpeechResult;

import org.json.JSONException;
import org.json.JSONObject;

import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;

import cafe.adriel.androidaudioconverter.AndroidAudioConverter;
import cafe.adriel.androidaudioconverter.callback.IConvertCallback;
import cafe.adriel.androidaudioconverter.model.AudioFormat;

public class VoiceTaskActivity extends AppCompatActivity implements View.OnClickListener {

    private Toolbar toolbar;
    private Chronometer chronometer;
    private ImageView imageViewRecord, imageViewPlay, imageViewStop;
    private SeekBar seekBar;
    private LinearLayout linearLayoutRecorder, linearLayoutPlay;
    private MediaRecorder mRecorder;
    private MediaPlayer mPlayer;
    private String fileName = null;
    private int lastProgress = 0;
    private Handler mHandler = new Handler();
    private int RECORD_AUDIO_REQUEST_CODE = 123;
    private boolean isPlaying = false;
    TextView mText;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_voice_task);

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            getPermissionToRecordAudio();
        }

        initViews();

    }

    private void initViews() {

        /** setting up the toolbar  **/
        toolbar = (Toolbar) findViewById(R.id.toolbar);
        toolbar.setTitle("Voice Recorder");
        toolbar.setTitleTextColor(getResources().getColor(android.R.color.black));
        setSupportActionBar(toolbar);

        linearLayoutRecorder = (LinearLayout) findViewById(R.id.linearLayoutRecorder);
        chronometer = (Chronometer) findViewById(R.id.chronometerTimer);
        chronometer.setBase(SystemClock.elapsedRealtime());
        imageViewRecord = (ImageView) findViewById(R.id.imageViewRecord);
        imageViewStop = (ImageView) findViewById(R.id.imageViewStop);
        imageViewPlay = (ImageView) findViewById(R.id.imageViewPlay);
        linearLayoutPlay = (LinearLayout) findViewById(R.id.linearLayoutPlay);
        seekBar = (SeekBar) findViewById(R.id.seekBar);
        mText = findViewById(R.id.textViewMessage);

        imageViewRecord.setOnClickListener(this);
        imageViewStop.setOnClickListener(this);
        imageViewPlay.setOnClickListener(this);

    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {

        MenuInflater inflater = getMenuInflater();
        inflater.inflate(R.menu.list_menu, menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {

        switch (item.getItemId()) {
            case R.id.item_list:
                gotoRecodingListActivity();
                return true;
            default:
                return super.onOptionsItemSelected(item);

        }

    }

    private void gotoRecodingListActivity() {
        Intent intent = new Intent(this, RecordingListActivity.class);
        intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
        startActivity(intent);
    }

    @Override
    public void onClick(View view) {

        if (view == imageViewRecord) {
            prepareforRecording();
            startRecording();
        } else if (view == imageViewStop) {
            prepareforStop();
            stopRecording();
        } else if (view == imageViewPlay) {
            if (!isPlaying && fileName != null) {
                isPlaying = true;
                startPlaying();
            } else {
                isPlaying = false;
                stopPlaying();
            }
        }

    }

    private void prepareforStop() {
        TransitionManager.beginDelayedTransition(linearLayoutRecorder);
        imageViewRecord.setVisibility(View.VISIBLE);
        imageViewStop.setVisibility(View.GONE);
        linearLayoutPlay.setVisibility(View.VISIBLE);
    }


    private void prepareforRecording() {
        TransitionManager.beginDelayedTransition(linearLayoutRecorder);
        imageViewRecord.setVisibility(View.GONE);
        imageViewStop.setVisibility(View.VISIBLE);
        linearLayoutPlay.setVisibility(View.GONE);
    }

    private void stopPlaying() {
        try {
            mPlayer.release();
        } catch (Exception e) {
            e.printStackTrace();
        }
        mPlayer = null;
        //showing the play button
        imageViewPlay.setImageResource(R.drawable.ic_play);
        chronometer.stop();

    }

    private void startRecording() {
        mRecorder = new MediaRecorder();
        mRecorder.setAudioSource(MediaRecorder.AudioSource.MIC);
        mRecorder.setOutputFormat(MediaRecorder.OutputFormat.DEFAULT);
        mRecorder.setAudioSamplingRate(16000);
        File root = Environment.getExternalStorageDirectory();
        File file = new File(root.getAbsolutePath() + "/Task_Recording/Audios");
        if (!file.exists()) {
            file.mkdirs();
        }
        fileName = Objects.requireNonNull(getExternalCacheDir()).getAbsolutePath();
        fileName += System.currentTimeMillis() + ".mp3";

//        fileName = root.getAbsolutePath() + "/Task_Recording/Audios/"
//                + String.valueOf(System.currentTimeMillis() + ".mp3");
        Log.d("filename", fileName);
        mRecorder.setOutputFile(fileName);
        mRecorder.setAudioEncoder(MediaRecorder.AudioEncoder.AMR_NB);

        try {
            mRecorder.prepare();
            mRecorder.start();
        } catch (IOException e) {
            e.printStackTrace();
        }
        lastProgress = 0;
        seekBar.setProgress(0);
        // making the imageview a stop button
        //starting the chronometer
        chronometer.setBase(SystemClock.elapsedRealtime());
        chronometer.start();
    }

    public void getInputString(String base64Audio) {
        String url = "https://speech.googleapis.com/v1/speech:recognize?key=AIzaSyAJBoW5YjuPGG7bJgZFVvlNcPtmX7ENwXQ";
        final SpeechData speechData = new SpeechData();
        Audio audio = new Audio();
        audio.setContent(base64Audio.replace("\n", ""));
        Config config = new Config();
        config.setEncoding("FLAC");
        config.setLanguageCode("en-IN");
        config.setSampleRateHertz(8000);
        speechData.setAudio(audio);
        speechData.setConfig(config);
        Gson gson = new Gson();
        String jsonString = gson.toJson(speechData);
        Log.d(TAG, "getInputString: " + jsonString);
        JSONObject jsonObject = null;
        try {
            jsonObject = new JSONObject(jsonString);
        } catch (JSONException e) {
            e.printStackTrace();
        }
        RequestQueue requestQueue = Volley.newRequestQueue(this);
        JsonObjectRequest jsonObjectRequest = new JsonObjectRequest(Request.Method.POST,
                url, jsonObject, new Response.Listener<JSONObject>() {
            @Override
            public void onResponse(JSONObject response) {
                Log.d(TAG, "onResponse: " + response);
                Gson gson = new Gson();
                SpeechResult speechResult = gson.fromJson(response.toString(), SpeechResult.class);
                if (speechResult.getResults() != null) {
                    if (speechResult.getResults().size() > 0) {
                        if (speechResult.getResults().get(0).getAlternatives().size() > 0) {
                            mText.setText("" + speechResult.getResults().get(0).getAlternatives().get(0).getTranscript());

                        }
                    }
                }
                volleyGet(speechResult.getResults().get(0).getAlternatives().get(0).getTranscript());
            }
        }, new Response.ErrorListener() {
            @Override
            public void onErrorResponse(VolleyError error) {
                error.printStackTrace();
            }
        }
        ) {
            @Override
            protected Map<String, String> getParams() throws AuthFailureError {
                return super.getParams();
            }
        };

        requestQueue.add(jsonObjectRequest);
    }

    public void volleyGet(String data) {
        String url = "https://api.wit.ai/message?v=20201218&q=" + data;
        List<String> jsonResponses = new ArrayList<>();


        RequestQueue requestQueue = Volley.newRequestQueue(this);
        JsonObjectRequest jsonObjectRequest = new JsonObjectRequest(Request.Method.GET,
                url, null, new Response.Listener<JSONObject>() {
            @Override
            public void onResponse(JSONObject response) {
                Log.d(TAG, "onResponse: " + response.toString());
                Gson gson = new Gson();
                Assistant assistant = gson.fromJson(response.toString(), Assistant.class);
                Log.d(TAG, "onResponse: " + assistant);
                String finalString = "Original Text" + assistant.getText();
                mText.setText("Original Text -> " + assistant.getText());

                if (assistant.getEntities().getWit$contactContact() != null &&
                        assistant.getEntities().getWit$contactContact().size() > 0) {
                    for (int i = 0; i < assistant.getEntities().getWit$contactContact().size(); i++) {
                        finalString = finalString + "\n" +
                                "Person Name -> " + i + 1 + " " + assistant.getEntities().getWit$contactContact().get(i).getValue();

                    }

                }
                if (assistant.getEntities().getWit$datetimeDatetime() != null && assistant.getEntities().getWit$datetimeDatetime().size() > 0) {
                    for (int i = 0; i < assistant.getEntities().getWit$datetimeDatetime().size(); i++) {
                        finalString = finalString + "\n" +
                                "Deadline -> " + i + 1 + " " + assistant.getEntities().getWit$datetimeDatetime().get(i).getTo().getValue();

                    }
                }
                if (assistant.getEntities().getWit$messageSubjectMessageSubject() != null && assistant.getEntities().getWit$messageSubjectMessageSubject().size() > 0) {
                    for (int i = 0; i < assistant.getEntities().getWit$messageSubjectMessageSubject().size(); i++) {
                        finalString = finalString + "\n" +
                                "Task Tittle -> " + i + 1 + " " + assistant.getEntities().getWit$messageSubjectMessageSubject().get(i).getValue();

                    }
                }
                mText.setText("" + finalString);
            }
        }, new Response.ErrorListener() {
            @Override
            public void onErrorResponse(VolleyError error) {
                error.printStackTrace();
            }
        }
        ) {
            @Override
            public Map<String, String> getHeaders() throws AuthFailureError {
                HashMap<String, String> headers = new HashMap<String, String>();
                headers.put("Authorization", " Bearer HRACZ2IKPLT3TYL5IGO5X5LWXVAQDY5U");
                return headers;

            }
        };

        requestQueue.add(jsonObjectRequest);

    }


    private void stopRecording() {

        try {
            mRecorder.stop();
            mRecorder.release();
        } catch (Exception e) {
            e.printStackTrace();
        }
        mRecorder = null;
        //starting the chronometer
        chronometer.stop();
        chronometer.setBase(SystemClock.elapsedRealtime());
        //showing the play button
        Toast.makeText(this, "Recording saved successfully.", Toast.LENGTH_SHORT).show();
        Uri uri = Uri.fromFile(new File(fileName));


        startPlaying();
        IConvertCallback callback = new IConvertCallback() {
            @Override
            public void onSuccess(File convertedFile) {
                // So fast? Love it!
                Log.d(TAG, "onSuccess: " + convertedFile.getAbsolutePath());
                encodeAudio(convertedFile.getAbsolutePath());
            }

            @Override
            public void onFailure(Exception error) {
                // Oops! Something went wrong
            }
        };
        AndroidAudioConverter.with(this)
                // Your current audio file
                .setFile(new File(fileName))

                // Your desired audio format
                .setFormat(AudioFormat.FLAC)

                // An callback to know when conversion is finished
                .setCallback(callback)

                // Start conversion
                .convert();

    }

    private static final String TAG = "MainActivity";

    private void encodeAudio(String selectedPath) {

        byte[] audioBytes;
        try {

            // Just to check file size.. Its is correct i-e; Not Zero
            File audioFile = new File(selectedPath);
            long fileSize = audioFile.length();

            ByteArrayOutputStream baos = new ByteArrayOutputStream();
            FileInputStream fis = new FileInputStream(new File(selectedPath));
            byte[] buf = new byte[1024];
            int n;
            while (-1 != (n = fis.read(buf)))
                baos.write(buf, 0, n);
            audioBytes = baos.toByteArray();

            // Here goes the Base64 string
            String _audioBase64 = android.util.Base64.encodeToString(audioBytes, Base64.DEFAULT);
            Log.d(TAG, "encodeAudio:Final " + _audioBase64);
            getInputString(_audioBase64);
        } catch (Exception e) {
            Log.d(TAG, "encodeAudio: " + e);
        }

    }


    private void startPlaying() {
        mPlayer = new MediaPlayer();
        try {
            mPlayer.setDataSource(fileName);
            mPlayer.prepare();
            mPlayer.start();
        } catch (IOException e) {
            Log.e("LOG_TAG", "prepare() failed");
        }
        //making the imageview pause button
        imageViewPlay.setImageResource(R.drawable.ic_pause);

        seekBar.setProgress(lastProgress);
        mPlayer.seekTo(lastProgress);
        seekBar.setMax(mPlayer.getDuration());
        seekUpdation();
        chronometer.start();


        mPlayer.setOnCompletionListener(new MediaPlayer.OnCompletionListener() {
            @Override
            public void onCompletion(MediaPlayer mp) {
                imageViewPlay.setImageResource(R.drawable.ic_play);
                isPlaying = false;
                chronometer.stop();
            }
        });


        seekBar.setOnSeekBarChangeListener(new SeekBar.OnSeekBarChangeListener() {
            @Override
            public void onProgressChanged(SeekBar seekBar, int progress, boolean fromUser) {
                if (mPlayer != null && fromUser) {
                    mPlayer.seekTo(progress);
                    chronometer.setBase(SystemClock.elapsedRealtime() - mPlayer.getCurrentPosition());
                    lastProgress = progress;
                }
            }

            @Override
            public void onStartTrackingTouch(SeekBar seekBar) {

            }

            @Override
            public void onStopTrackingTouch(SeekBar seekBar) {

            }
        });
    }

    Runnable runnable = new Runnable() {
        @Override
        public void run() {
            seekUpdation();
        }
    };

    private void seekUpdation() {
        if (mPlayer != null) {
            int mCurrentPosition = mPlayer.getCurrentPosition();
            seekBar.setProgress(mCurrentPosition);
            lastProgress = mCurrentPosition;
        }
        mHandler.postDelayed(runnable, 100);
    }


    @RequiresApi(api = Build.VERSION_CODES.M)
    public void getPermissionToRecordAudio() {
        // 1) Use the support library version ContextCompat.checkSelfPermission(...) to avoid
        // checking the build version since Context.checkSelfPermission(...) is only available
        // in Marshmallow
        // 2) Always check for permission (even if permission has already been granted)
        // since the user can revoke permissions at any time through Settings
        if (ContextCompat.checkSelfPermission(this, Manifest.permission.RECORD_AUDIO) != PackageManager.PERMISSION_GRANTED
                || ContextCompat.checkSelfPermission(this, Manifest.permission.READ_EXTERNAL_STORAGE) != PackageManager.PERMISSION_GRANTED
                || ContextCompat.checkSelfPermission(this, Manifest.permission.WRITE_EXTERNAL_STORAGE) != PackageManager.PERMISSION_GRANTED) {

            // The permission is NOT already granted.
            // Check if the user has been asked about this permission already and denied
            // it. If so, we want to give more explanation about why the permission is needed.
            // Fire off an async request to actually get the permission
            // This will show the standard permission request dialog UI
            requestPermissions(new String[]{Manifest.permission.READ_EXTERNAL_STORAGE, Manifest.permission.RECORD_AUDIO, Manifest.permission.WRITE_EXTERNAL_STORAGE},
                    RECORD_AUDIO_REQUEST_CODE);

        }
    }

    // Callback with the request from calling requestPermissions(...)
    @RequiresApi(api = Build.VERSION_CODES.M)
    @Override
    public void onRequestPermissionsResult(int requestCode,
                                           @NonNull String permissions[],
                                           @NonNull int[] grantResults) {
        // Make sure it's our original READ_CONTACTS request
        if (requestCode == RECORD_AUDIO_REQUEST_CODE) {
            if (grantResults.length == 3 &&
                    grantResults[0] == PackageManager.PERMISSION_GRANTED
                    && grantResults[1] == PackageManager.PERMISSION_GRANTED
                    && grantResults[2] == PackageManager.PERMISSION_GRANTED) {

                //Toast.makeText(this, "Record Audio permission granted", Toast.LENGTH_SHORT).show();

            } else {
                Toast.makeText(this, "You must give permissions to use this app. App is exiting.", Toast.LENGTH_SHORT).show();
                finishAffinity();
            }
        }

    }


}
