package com.library.auralms;

import android.annotation.SuppressLint;
import android.app.Activity;
import android.graphics.Bitmap;
import android.os.Bundle;
import android.view.View;
import android.webkit.WebChromeClient;
import android.webkit.WebResourceError;
import android.webkit.WebResourceRequest;
import android.webkit.WebSettings;
import android.webkit.WebView;
import android.webkit.WebViewClient;
import android.widget.TextView;
import android.widget.LinearLayout;
import android.widget.Button;
import android.graphics.Color;
import android.view.Gravity;
import android.view.ViewGroup;

public class MainActivity extends Activity {
    private WebView webView;
    private View errorView;

    @SuppressLint("SetJavaScriptEnabled")
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        String url = BuildConfig.BACKEND_URL;
        if (url.contains("YOUR-AURALMS-SERVICE")) {
            showSetupMessage();
            return;
        }

        webView = new WebView(this);
        WebSettings settings = webView.getSettings();
        settings.setJavaScriptEnabled(true);
        settings.setDomStorageEnabled(true);
        settings.setDatabaseEnabled(true);
        settings.setLoadsImagesAutomatically(true);
        settings.setSupportZoom(false);
        settings.setMixedContentMode(WebSettings.MIXED_CONTENT_NEVER_ALLOW);

        webView.setWebChromeClient(new WebChromeClient());
        webView.setWebViewClient(new WebViewClient() {
            @Override public void onPageStarted(WebView view, String url, Bitmap favicon) {
                if (errorView != null) errorView.setVisibility(View.GONE);
                webView.setVisibility(View.VISIBLE);
            }

            @Override public void onReceivedError(WebView view, WebResourceRequest request, WebResourceError error) {
                if (request.isForMainFrame()) showConnectionError();
            }

            @Override public boolean shouldOverrideUrlLoading(WebView view, WebResourceRequest request) {
                String host = request.getUrl().getHost();
                String backendHost = android.net.Uri.parse(BuildConfig.BACKEND_URL).getHost();
                if (host != null && host.equals(backendHost)) return false;
                return true;
            }
        });
        setContentView(webView);
        webView.loadUrl(url);
    }

    private void showSetupMessage() {
        TextView message = new TextView(this);
        message.setText("AuraLMS is almost ready. Set your deployed backend URL in the Android build configuration, then rebuild the APK.");
        message.setTextSize(18);
        message.setTextColor(Color.rgb(31, 41, 55));
        message.setGravity(Gravity.CENTER);
        message.setPadding(32, 32, 32, 32);
        setContentView(message);
    }

    private void showConnectionError() {
        if (errorView == null) {
            LinearLayout layout = new LinearLayout(this);
            layout.setOrientation(LinearLayout.VERTICAL);
            layout.setGravity(Gravity.CENTER);
            layout.setPadding(32, 32, 32, 32);
            TextView message = new TextView(this);
            message.setText("Can't connect to AuraLMS. Check your internet connection or try again.");
            message.setTextSize(18);
            message.setGravity(Gravity.CENTER);
            Button retry = new Button(this);
            retry.setText("Retry");
            retry.setOnClickListener(v -> webView.reload());
            layout.addView(message, new LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT));
            layout.addView(retry);
            errorView = layout;
            addContentView(errorView, new ViewGroup.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.MATCH_PARENT));
        }
        webView.setVisibility(View.GONE);
        errorView.setVisibility(View.VISIBLE);
    }

    @Override public void onBackPressed() {
        if (webView != null && webView.canGoBack()) webView.goBack();
        else super.onBackPressed();
    }
}
