
package com.emailwc.ytkeyplayerexact;
import android.annotation.SuppressLint;
import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.view.KeyEvent;
import android.webkit.JavascriptInterface;
import android.webkit.WebChromeClient;
import android.webkit.WebSettings;
import android.webkit.WebView;
import android.webkit.WebViewClient;
import androidx.appcompat.app.AppCompatActivity;
public class MainActivity extends AppCompatActivity {
    WebView webView;
    @SuppressLint("SetJavaScriptEnabled")
    @Override protected void onCreate(Bundle savedInstanceState){
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        webView=findViewById(R.id.webview);
        WebSettings s=webView.getSettings();
        s.setJavaScriptEnabled(true);
        s.setDomStorageEnabled(true);
        s.setMediaPlaybackRequiresUserGesture(false);
        s.setMixedContentMode(WebSettings.MIXED_CONTENT_ALWAYS_ALLOW);
        s.setAllowFileAccess(true);
        s.setAllowContentAccess(true);
        s.setAllowFileAccessFromFileURLs(true);
        s.setAllowUniversalAccessFromFileURLs(true);
        s.setCacheMode(WebSettings.LOAD_DEFAULT);
        s.setUserAgentString("Mozilla/5.0 (Linux; Android 11; BRAVIA 4K GB) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/124.0.0.0 Safari/537.36");
        webView.setWebChromeClient(new WebChromeClient());
        webView.setWebViewClient(new WebViewClient(){
            @Override
            public boolean shouldOverrideUrlLoading(WebView view, String url){
                if(url.contains("youtube.com/watch")){
                    try{
                        Intent i=new Intent(Intent.ACTION_VIEW, Uri.parse(url));
                        i.setPackage("com.google.android.youtube.tv");
                        startActivity(i);
                        return true;
                    }catch(Exception e){}
                }
                return false;
            }
        });
        webView.addJavascriptInterface(new Object(){
            @JavascriptInterface public void openVlcFavourites(){
                String p="/storage/emulated/0/BWJam Songs";
                try{
                    Intent i=new Intent(Intent.ACTION_VIEW);
                    i.setPackage("org.videolan.vlc");
                    i.setDataAndType(Uri.parse("file://"+p),"resource/folder");
                    i.putExtra("extra_path",p);
                    startActivity(i);return;
                }catch(Exception e){}
                try{
                    Intent l=getPackageManager().getLaunchIntentForPackage("org.videolan.vlc");
                    if(l!=null){startActivity(l);return;}
                }catch(Exception e){}
                try{
                    Intent f=new Intent(Intent.ACTION_VIEW);
                    f.setDataAndType(Uri.parse("file://"+p),"*/*");
                    startActivity(f);
                }catch(Exception ex){
                    startActivity(new Intent(Intent.ACTION_VIEW,Uri.parse("file:///storage/emulated/0/")));
                }
            }
            @JavascriptInterface public String getFavPath(){return "/storage/emulated/0/BWJam Songs";}
            @JavascriptInterface public void openInYouTube(String videoId){
                try{
                    Intent i=new Intent(Intent.ACTION_VIEW, Uri.parse("vnd.youtube:"+videoId));
                    i.setPackage("com.google.android.youtube.tv");
                    startActivity(i);
                }catch(Exception e){
                    try{
                        startActivity(new Intent(Intent.ACTION_VIEW, Uri.parse("https://www.youtube.com/watch?v="+videoId)));
                    }catch(Exception ex){}
                }
            }
        },"Android");
        webView.setOnKeyListener((v,kc,ev)->{
            if(ev.getAction()==KeyEvent.ACTION_DOWN){
                if(kc==KeyEvent.KEYCODE_DPAD_LEFT){webView.evaluateJavascript("if(window.seekTimeline)seekTimeline(-5);",null);return true;}
                if(kc==KeyEvent.KEYCODE_DPAD_RIGHT){webView.evaluateJavascript("if(window.seekTimeline)seekTimeline(5);",null);return true;}
                if(kc==KeyEvent.KEYCODE_DPAD_CENTER||kc==KeyEvent.KEYCODE_ENTER){webView.evaluateJavascript("if(window.toggleTimelineSelect)toggleTimelineSelect();",null);return true;}
            }return false;
        });
        webView.loadUrl("file:///android_asset/console.html");
    }
    @Override public void onBackPressed(){if(webView.canGoBack())webView.goBack();else super.onBackPressed();}
}
