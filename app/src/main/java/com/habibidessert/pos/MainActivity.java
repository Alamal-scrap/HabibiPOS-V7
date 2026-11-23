package com.habibidessert.pos;

import android.app.Activity;
import android.os.Bundle;
import android.content.Intent;
import android.net.Uri;
import android.webkit.*;
import android.view.View;
import android.widget.Toast;
import android.app.DownloadManager;
import android.content.Context;
import android.os.Environment;
import android.provider.DocumentsContract;
import android.webkit.JavascriptInterface;
import androidx.webkit.WebViewAssetLoader;
import androidx.webkit.WebViewClientCompat;
import android.webkit.WebResourceRequest;
import android.webkit.WebResourceResponse;
import java.io.OutputStream;
import java.nio.charset.StandardCharsets;

public class MainActivity extends Activity {
    private WebView web;
    private ValueCallback<Uri[]> fileCallback;
    private static final int FILE_CHOOSER = 101;
    private static final int BACKUP_SAVE = 102;
    private String pendingBackup;
    private String pendingBackupName;

    @Override public void onCreate(Bundle state) {
        super.onCreate(state);
        getWindow().getDecorView().setSystemUiVisibility(View.SYSTEM_UI_FLAG_FULLSCREEN | View.SYSTEM_UI_FLAG_IMMERSIVE_STICKY);
        web = new WebView(this);
        setContentView(web);
        WebSettings settings = web.getSettings();
        settings.setJavaScriptEnabled(true);
        settings.setDomStorageEnabled(true);
        settings.setAllowFileAccess(false);
        settings.setAllowContentAccess(true);
        settings.setJavaScriptCanOpenWindowsAutomatically(true);
        WebViewAssetLoader loader = new WebViewAssetLoader.Builder()
            .addPathHandler("/assets/", new WebViewAssetLoader.AssetsPathHandler(this))
            .build();
        web.setWebViewClient(new WebViewClientCompat() {
            @Override public WebResourceResponse shouldInterceptRequest(WebView view, WebResourceRequest request) {
                return loader.shouldInterceptRequest(request.getUrl());
            }
            @Override public WebResourceResponse shouldInterceptRequest(WebView view, String url) {
                return loader.shouldInterceptRequest(Uri.parse(url));
            }
        });
        web.addJavascriptInterface(new Object() {
            @JavascriptInterface public void saveBackup(String json, String filename) {
                runOnUiThread(() -> {
                    if (pendingBackup != null) { Toast.makeText(MainActivity.this, "Finish the previous backup first", Toast.LENGTH_LONG).show(); return; }
                    pendingBackup = json;
                    pendingBackupName = filename.replaceAll("[^a-zA-Z0-9._-]", "_");
                    Intent intent = new Intent(Intent.ACTION_CREATE_DOCUMENT);
                    intent.addCategory(Intent.CATEGORY_OPENABLE);
                    intent.setType("application/json");
                    intent.putExtra(Intent.EXTRA_TITLE, pendingBackupName);
                    try { startActivityForResult(intent, BACKUP_SAVE); }
                    catch (Exception e) { pendingBackup = null; Toast.makeText(MainActivity.this, "Cannot open file picker", Toast.LENGTH_LONG).show(); }
                });
            }
        }, "HabibiAndroid");
        web.setWebChromeClient(new WebChromeClient() {
            @Override public boolean onShowFileChooser(WebView view, ValueCallback<Uri[]> callback, FileChooserParams params) {
                if (fileCallback != null) fileCallback.onReceiveValue(null);
                fileCallback = callback;
                try { startActivityForResult(params.createIntent(), FILE_CHOOSER); return true; }
                catch (Exception ex) { fileCallback = null; return false; }
            }
            @Override public boolean onJsConfirm(WebView view, String url, String message, JsResult result) {
                new android.app.AlertDialog.Builder(MainActivity.this).setMessage(message).setPositiveButton("OK",(d,w)->result.confirm()).setNegativeButton("Cancel",(d,w)->result.cancel()).show(); return true;
            }
            @Override public boolean onJsPrompt(WebView view,String url,String message,String defaultValue,JsPromptResult result) {
                android.widget.EditText input=new android.widget.EditText(MainActivity.this);input.setText(defaultValue);
                new android.app.AlertDialog.Builder(MainActivity.this).setTitle(message).setView(input).setPositiveButton("OK",(d,w)->result.confirm(input.getText().toString())).setNegativeButton("Cancel",(d,w)->result.cancel()).show();return true;
            }
        });
        web.setDownloadListener((url, agent, disposition, mime, length) -> {
            try {
                if (url.startsWith("blob:")) { Toast.makeText(this,"Blob downloads require an Android export bridge",Toast.LENGTH_LONG).show(); return; }
                DownloadManager.Request req = new DownloadManager.Request(Uri.parse(url));
                req.setMimeType(mime);
                req.setNotificationVisibility(DownloadManager.Request.VISIBILITY_VISIBLE_NOTIFY_COMPLETED);
                req.setDestinationInExternalPublicDir(Environment.DIRECTORY_DOWNLOADS,"HabibiPOS_Backup.json");
                ((DownloadManager)getSystemService(Context.DOWNLOAD_SERVICE)).enqueue(req);
            } catch(Exception e) { Toast.makeText(this,"Download failed: "+e.getMessage(),Toast.LENGTH_LONG).show(); }
        });
        web.loadUrl("https://appassets.androidplatform.net/assets/index.html");
    }
    @Override protected void onActivityResult(int requestCode,int resultCode,Intent data) {
        super.onActivityResult(requestCode,resultCode,data);
        if (requestCode == BACKUP_SAVE) {
            String backup = pendingBackup;
            pendingBackup = null;
            if (resultCode == RESULT_OK && data != null && data.getData() != null && backup != null) {
                try (OutputStream out = getContentResolver().openOutputStream(data.getData(), "w")) {
                    if (out == null) throw new Exception("Storage destination unavailable");
                    out.write(backup.getBytes(StandardCharsets.UTF_8));
                    out.flush();
                    Toast.makeText(this, "Backup saved successfully", Toast.LENGTH_LONG).show();
                } catch (Exception e) {
                    Toast.makeText(this, "Backup failed: " + e.getMessage(), Toast.LENGTH_LONG).show();
                }
            }
            return;
        }
        if (requestCode==FILE_CHOOSER && fileCallback!=null) {
            fileCallback.onReceiveValue(resultCode==RESULT_OK ? WebChromeClient.FileChooserParams.parseResult(resultCode,data) : null);
            fileCallback=null;
        }
    }
    @Override public void onBackPressed() { if(web.canGoBack()) web.goBack(); else new android.app.AlertDialog.Builder(this).setMessage("Exit Habibi POS?").setPositiveButton("Exit",(d,w)->finish()).setNegativeButton("Stay",null).show(); }
}
