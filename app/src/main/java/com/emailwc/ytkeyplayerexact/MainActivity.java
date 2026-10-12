package com.emailwc.ytkeyplayerexact;

import android.annotation.SuppressLint;
import android.os.Bundle;
import android.os.Handler;
import android.webkit.JavascriptInterface;
import android.webkit.WebChromeClient;
import android.webkit.WebSettings;
import android.webkit.WebView;
import android.widget.Button;
import android.widget.EditText;
import android.widget.TextView;
import android.widget.Toast;
import androidx.appcompat.app.AppCompatActivity;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class MainActivity extends AppCompatActivity {

    private WebView webView;
    private EditText etUrl, etStart, etEnd;
    private TextView tvStatus;
    private Button btnLoad, btnPlayPause;
    private Handler handler = new Handler();
    private Runnable loopCheck;
    private boolean isPlaying = true;
    private float startSec = 0f;
    private float endSec = 10f;
    private String currentVideoId = "dQw4w9WgXcQ";

    @SuppressLint({"SetJavaScriptEnabled", "AddJavascriptInterface"})
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        etUrl = findViewById(R.id.etUrl);
        etStart = findViewById(R.id.etStart);
        etEnd = findViewById(R.id.etEnd);
        tvStatus = findViewById(R.id.tvStatus);
        btnLoad = findViewById(R.id.btnLoad);
        btnPlayPause = findViewById(R.id.btnPlayPause);
        webView = findViewById(R.id.webView);

        WebSettings ws = webView.getSettings();
        ws.setJavaScriptEnabled(true);
        ws.setDomStorageEnabled(true);
        ws.setMediaPlaybackRequiresUserGesture(false);
        webView.setWebChromeClient(new WebChromeClient());
        webView.addJavascriptInterface(new JsBridge(), "AndroidBridge");

        btnLoad.setOnClickListener(v -> loadVideo());
        btnPlayPause.setOnClickListener(v -> togglePlayPause());

        loadVideo();
        startLoopChecker();
    }

    private void loadVideo() {
        String input = etUrl.getText().toString().trim();
        String id = extractVideoId(input);
        if (id != null && !id.isEmpty()) currentVideoId = id;

        try {
            startSec = Float.parseFloat(etStart.getText().toString());
            endSec = Float.parseFloat(etEnd.getText().toString());
        } catch (Exception e) {
            Toast.makeText(this, "Invalid start/end", Toast.LENGTH_SHORT).show();
            return;
        }

        if (endSec <= startSec) {
            Toast.makeText(this, "End must be > Start", Toast.LENGTH_SHORT).show();
            return;
        }

        String html = getHtml(currentVideoId, startSec);
        webView.loadDataWithBaseURL("https://www.youtube.com", html, "text/html", "UTF-8", null);
        tvStatus.setText("Status: Loading " + currentVideoId + " [" + startSec + "s -> " + endSec + "s] EXACT LOOP");
    }

    private String getHtml(String videoId, float start) {
        // EXACT player: IFrame API, controls 1, playsinline, enablejsapi, precise seekTo
        return "<!DOCTYPE html><html><head><style>body{margin:0;background:#000;}#player{width:100vw;height:100vh;}</style></head><body>" +
                "<div id='player'></div>" +
                "<script>" +
                "var tag=document.createElement('script');tag.src='https://www.youtube.com/iframe_api';document.head.appendChild(tag);" +
                "var player; var isReady=false;" +
                "function onYouTubeIframeAPIReady(){" +
                " player=new YT.Player('player',{height:'100%',width:'100%',videoId:'" + videoId + "'," +
                " playerVars:{'playsinline':1,'controls':1,'rel':0,'modestbranding':1,'start':" + (int)start + "}," +
                " events:{'onReady':onReady,'onStateChange':onState}});" +
                "}" +
                "function onReady(e){isReady=true; e.target.seekTo(" + start + ",true); e.target.playVideo(); AndroidBridge.onReady();}" +
                "function onState(e){ AndroidBridge.onState(e.data); }" +
                "function getCurrent(){ if(isReady) try{ AndroidBridge.onTime(player.getCurrentTime()); }catch(e){} }" +
                "function seekTo(s){ if(isReady) player.seekTo(s,true); }" +
                "function play(){ if(isReady) player.playVideo(); }" +
                "function pause(){ if(isReady) player.pauseVideo(); }" +
                "setInterval(getCurrent, 50);" +
                "</script></body></html>";
    }

    private void startLoopChecker() {
        loopCheck = new Runnable() {
            @Override public void run() {
                // loop logic is in JS bridge onTime, but keep handler alive
                handler.postDelayed(this, 100);
            }
        };
        handler.post(loopCheck);
    }

    private void togglePlayPause() {
        if (isPlaying) {
            webView.evaluateJavascript("pause();", null);
            btnPlayPause.setText("▶");
            isPlaying = false;
        } else {
            webView.evaluateJavascript("play();", null);
            btnPlayPause.setText("⏸");
            isPlaying = true;
        }
    }

    private String extractVideoId(String input) {
        if (input == null) return null;
        if (input.matches("[a-zA-Z0-9_-]{11}")) return input;
        Pattern p = Pattern.compile("(?:v=|\/embed\/|\/shorts\/|youtu\.be\/)([a-zA-Z0-9_-]{11})");
        Matcher m = p.matcher(input);
        if (m.find()) return m.group(1);
        return input;
    }

    public class JsBridge {
        @JavascriptInterface
        public void onReady() {
            runOnUiThread(() -> tvStatus.setText("Status: PLAYING EXACT [" + startSec + " -> " + endSec + "]"));
        }
        @JavascriptInterface
        public void onState(int state) {
            // 1 = playing, 2 = paused
        }
        @JavascriptInterface
        public void onTime(float current) {
            runOnUiThread(() -> {
                tvStatus.setText(String.format("Time: %.2f / %.2f sec [EXACT LOOP]", current, endSec));
                if (current >= endSec - 0.05f || current < startSec - 0.5f) {
                    webView.evaluateJavascript("seekTo(" + startSec + ");", null);
                }
            });
        }
    }

    @Override
    protected void onDestroy() {
        if (loopCheck != null) handler.removeCallbacks(loopCheck);
        if (webView != null) webView.destroy();
        super.onDestroy();
    }
}