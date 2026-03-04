package com.teamtodo.app.ui;

import android.annotation.SuppressLint;
import android.app.Activity;
import android.content.SharedPreferences;
import android.graphics.Color;
import android.content.Intent;
import android.os.Bundle;
import android.util.Log;
import android.webkit.JavascriptInterface;
import android.webkit.WebChromeClient;
import android.webkit.WebResourceRequest;
import android.webkit.RenderProcessGoneDetail;
import android.webkit.WebSettings;
import android.webkit.WebView;
import android.webkit.WebViewClient;
import android.widget.Toast;
import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.appcompat.app.AppCompatActivity;
import com.google.android.gms.auth.api.signin.GoogleSignIn;
import com.google.android.gms.auth.api.signin.GoogleSignInAccount;
import com.google.android.gms.auth.api.signin.GoogleSignInClient;
import com.google.android.gms.auth.api.signin.GoogleSignInOptions;
import com.google.android.gms.tasks.Task;
import com.google.firebase.auth.AuthCredential;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.auth.GoogleAuthProvider;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.gson.Gson;
import com.teamtodo.app.R;
import com.teamtodo.app.data.FirestoreManager;
import com.teamtodo.app.service.TodoAlarmManager;
import org.json.JSONObject;
import java.util.HashMap;
import java.util.Map;

public class MainActivity extends AppCompatActivity {
    private static final String TAG = "MainActivity";
    private static final String PREFS_NAME = "teamtodo_prefs";
    private static final String KEY_THEME_COLOR = "themeColor";
    private static final String DEFAULT_THEME_COLOR = "#EB4770";

    private WebView webView;
    private FirebaseAuth mAuth;
    private FirebaseFirestore db;
    private GoogleSignInClient googleSignInClient;
    private FirestoreManager firestoreManager;
    private TodoAlarmManager alarmManager;
    private String currentScreen = "profile_select";
    private String lastLoadedScreen = null;
    private String pendingSharedUrl = null;
    private final Gson gson = new Gson();

    private final ActivityResultLauncher<Intent> googleSignInLauncher =
        registerForActivityResult(new ActivityResultContracts.StartActivityForResult(), result -> {
            if (result.getResultCode() == Activity.RESULT_OK) {
                Task<GoogleSignInAccount> task = GoogleSignIn.getSignedInAccountFromIntent(result.getData());
                handleSignInResult(task);
            } else {
                sendToWebView("onLoginError", "{\"error\":\"로그인이 취소되었습니다\"}");
            }
        });

    @SuppressLint("SetJavaScriptEnabled")
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        mAuth = FirebaseAuth.getInstance();
        db = FirebaseFirestore.getInstance();
        firestoreManager = new FirestoreManager(this, db, mAuth);
        alarmManager = new TodoAlarmManager(this);

        // Google Sign-In 설정
        GoogleSignInOptions gso = new GoogleSignInOptions.Builder(GoogleSignInOptions.DEFAULT_SIGN_IN)
            .requestIdToken(getString(R.string.default_web_client_id))
            .requestEmail()
            .requestProfile()
            .build();
        googleSignInClient = GoogleSignIn.getClient(this, gso);

        // Intent에서 화면 정보 가져오기
        currentScreen = getIntent().getStringExtra("screen");
        if (currentScreen == null) currentScreen = "profile_select";
        pendingSharedUrl = getIntent().getStringExtra("sharedUrl");

        applySavedThemeColor();
        setupWebView();
        loadScreen(currentScreen);
    }

    private String normalizeHexColor(String input) {
        if (input == null) return DEFAULT_THEME_COLOR;
        String raw = input.trim();
        if (raw.isEmpty()) return DEFAULT_THEME_COLOR;
        if (!raw.startsWith("#")) raw = "#" + raw;
        if (!raw.matches("^#[0-9a-fA-F]{6}$")) return DEFAULT_THEME_COLOR;
        return raw.toUpperCase();
    }

    private void applySystemBarThemeColor(String colorHex) {
        String hex = normalizeHexColor(colorHex);
        try {
            int color = Color.parseColor(hex);
            runOnUiThread(() -> {
                getWindow().setStatusBarColor(color);
                getWindow().setNavigationBarColor(color);
            });
        } catch (Exception ignored) {
        }
    }

    private void applySavedThemeColor() {
        SharedPreferences prefs = getSharedPreferences(PREFS_NAME, MODE_PRIVATE);
        String saved = prefs.getString(KEY_THEME_COLOR, DEFAULT_THEME_COLOR);
        applySystemBarThemeColor(saved);
    }

    @SuppressLint("SetJavaScriptEnabled")
    private void setupWebView() {
        webView = findViewById(R.id.webView);
        WebSettings settings = webView.getSettings();
        settings.setJavaScriptEnabled(true);
        settings.setDomStorageEnabled(true);
        settings.setDatabaseEnabled(true);
        settings.setAllowFileAccess(true);
        settings.setAllowContentAccess(true);
        settings.setLoadWithOverviewMode(true);
        settings.setUseWideViewPort(true);
        settings.setMediaPlaybackRequiresUserGesture(false);
        settings.setCacheMode(WebSettings.LOAD_DEFAULT);
        settings.setMixedContentMode(WebSettings.MIXED_CONTENT_ALWAYS_ALLOW);

        webView.setWebChromeClient(new WebChromeClient());
        webView.setWebViewClient(new WebViewClient() {
            @Override
            public boolean shouldOverrideUrlLoading(WebView view, WebResourceRequest request) {
                String url = request.getUrl().toString();
                if (url.startsWith("teamtodo://")) {
                    handleScheme(url);
                    return true;
                }
                if (url.startsWith("http://") || url.startsWith("https://")) {
                    Intent intent = new Intent(Intent.ACTION_VIEW, android.net.Uri.parse(url));
                    startActivity(intent);
                    return true;
                }
                return false;
            }

            @Override
            public void onPageFinished(WebView view, String url) {
                super.onPageFinished(view, url);
                injectBridgeScript();
                if (pendingSharedUrl != null) {
                    String escapedUrl = pendingSharedUrl.replace("'", "\\'");
                    runOnUiThread(() -> webView.evaluateJavascript(
                        "if(window.onSharedUrl) window.onSharedUrl('" + escapedUrl + "');", null));
                    pendingSharedUrl = null;
                }
            }

            @Override
            public boolean onRenderProcessGone(WebView view, RenderProcessGoneDetail detail) {
                Log.e(TAG, "WebView renderer gone. didCrash=" + detail.didCrash());
                recoverFromWebViewCrash();
                return true;
            }
        });

        // JavaScript Interface 등록
        webView.addJavascriptInterface(new AndroidBridge(), "AndroidBridge");
    }

    private void injectBridgeScript() {
        String script = "javascript:(function() {" +
            "window.teamtodo = window.teamtodo || {};" +
            "window.teamtodo.call = function(method, data) {" +
            "  AndroidBridge.call(method, data ? JSON.stringify(data) : '{}');" +
            "};" +
            "if(window.onBridgeReady) window.onBridgeReady();" +
            "})();";
        webView.evaluateJavascript(script, null);
    }

    public void loadScreen(String screenName) {
        loadScreen(screenName, false);
    }

    public void loadScreen(String screenName, boolean forceReload) {
        if (screenName == null || screenName.trim().isEmpty()) return;
        if (!forceReload && screenName.equals(lastLoadedScreen)) return;
        currentScreen = screenName;
        lastLoadedScreen = screenName;
        String htmlFile = "file:///android_asset/html/" + screenName + ".html";
        runOnUiThread(() -> webView.loadUrl(htmlFile));
    }

    public void sendToWebView(String eventName, String jsonData) {
        String payload = (jsonData == null || jsonData.trim().isEmpty()) ? "null" : jsonData;
        String safeEventName = org.json.JSONObject.quote(eventName);
        String script =
            "javascript:(function(){" +
            "try{" +
            "if(window.onNativeEvent) window.onNativeEvent(" + safeEventName + "," + payload + ");" +
            "}catch(e){console.error('onNativeEvent failed',e);}" +
            "})();";
        runOnUiThread(() -> webView.evaluateJavascript(script, null));
    }

    @SuppressLint("SetJavaScriptEnabled")
    private void recoverFromWebViewCrash() {
        runOnUiThread(() -> {
            try {
                if (webView != null) {
                    webView.destroy();
                }
            } catch (Exception ignored) { }

            setContentView(R.layout.activity_main);
            setupWebView();
            loadScreen(currentScreen != null ? currentScreen : "profile_select");
            Toast.makeText(this, "화면을 복구했습니다.", Toast.LENGTH_SHORT).show();
        });
    }

    private void handleScheme(String url) {
        // teamtodo://navigate?screen=todo_main
        android.net.Uri uri = android.net.Uri.parse(url);
        String host = uri.getHost();
        if ("navigate".equals(host)) {
            String screen = uri.getQueryParameter("screen");
            if (screen != null) loadScreen(screen);
        } else if ("login".equals(host)) {
            startGoogleSignIn();
        } else if ("logout".equals(host)) {
            signOut();
        }
    }

    private void startGoogleSignIn() {
        Intent signInIntent = googleSignInClient.getSignInIntent();
        googleSignInLauncher.launch(signInIntent);
    }

    private void handleSignInResult(Task<GoogleSignInAccount> completedTask) {
        try {
            GoogleSignInAccount account = completedTask.getResult(com.google.android.gms.common.api.ApiException.class);
            AuthCredential credential = GoogleAuthProvider.getCredential(account.getIdToken(), null);
            mAuth.signInWithCredential(credential).addOnCompleteListener(this, task -> {
                if (task.isSuccessful()) {
                    FirebaseUser user = mAuth.getCurrentUser();
                    if (user != null) {
                        Map<String, Object> userData = new HashMap<>();
                        userData.put("uid", user.getUid());
                        userData.put("displayName", user.getDisplayName());
                        userData.put("email", user.getEmail());
                        userData.put("photoUrl", user.getPhotoUrl() != null ? user.getPhotoUrl().toString() : "");
                        // Firestore 쓰기 실패와 무관하게 로그인 성공 전환은 즉시 보장한다.
                        sendToWebView("onLoginSuccess", gson.toJson(userData));
                        loadScreen("profile_select", true);
                        firestoreManager.createOrUpdateUser(user, null);
                    }
                } else {
                    sendToWebView("onLoginError", "{\"error\":\"인증에 실패했습니다\"}");
                }
            });
        } catch (Exception e) {
            sendToWebView("onLoginError", "{\"error\":\"" + e.getMessage() + "\"}");
        }
    }

    private void signOut() {
        mAuth.signOut();
        googleSignInClient.signOut().addOnCompleteListener(task -> {
            loadScreen("login");
        });
    }

    @Override
    public void onBackPressed() {
        if (webView.canGoBack()) {
            webView.goBack();
        } else {
            super.onBackPressed();
        }
    }

    // ==============================
    // JavaScript Bridge Interface
    // ==============================
    class AndroidBridge {

        @JavascriptInterface
        public void call(String method, String jsonData) {
            try {
                JSONObject data = new JSONObject(jsonData);
                switch (method) {
                    // === 네비게이션 ===
                    case "navigate":
                        String screen = data.optString("screen", "todo_main");
                        String targetScreen = screen;
                        boolean forceReload = false;
                        String sharedUrlParam = "";
                        if (screen.contains("?")) {
                            android.net.Uri parsed = android.net.Uri.parse("teamtodo://local/" + screen);
                            String path = parsed.getPath();
                            targetScreen = (path != null && path.length() > 1) ? path.substring(1) : screen.split("\\?")[0];
                            String sUrl = parsed.getQueryParameter("sharedUrl");
                            if (sUrl != null) {
                                sharedUrlParam = sUrl;
                            }
                            forceReload = true;
                        }
                        // 타인 프로필 선택 시 friend_todo로 이동 (네이티브에서 파라미터 전달)
                        if (targetScreen.startsWith("friend_todo") && data.has("targetProfileId")) {
                            String targetProfileId = data.optString("targetProfileId");
                            String targetOwnerUid  = data.optString("targetOwnerUid", "");
                            String targetName      = data.optString("targetName", "");
                            // localStorage에 대상 프로필 저장 후 화면 로드
                            String initScript = "javascript:" +
                                "localStorage.setItem('targetProfileId','" + targetProfileId + "');" +
                                "localStorage.setItem('targetOwnerUid','" + targetOwnerUid + "');" +
                                "localStorage.setItem('targetProfileName','" + targetName.replace("'","\\'") + "');";
                            runOnUiThread(() -> webView.evaluateJavascript(initScript, null));
                        }
                        if ("url_share".equals(targetScreen) && !sharedUrlParam.isEmpty()) {
                            final String escaped = sharedUrlParam.replace("'", "\\'");
                            String setScript = "javascript:localStorage.setItem('sharedUrlPending','" + escaped + "');";
                            runOnUiThread(() -> webView.evaluateJavascript(setScript, null));
                        }
                        loadScreen(targetScreen, forceReload);
                        break;
                    case "setThemeColor":
                        String colorHex = normalizeHexColor(data.optString("color", DEFAULT_THEME_COLOR));
                        getSharedPreferences(PREFS_NAME, MODE_PRIVATE)
                            .edit()
                            .putString(KEY_THEME_COLOR, colorHex)
                            .apply();
                        applySystemBarThemeColor(colorHex);
                        sendToWebView("onThemeColorApplied", "{\"ok\":true}");
                        break;

                    // === 인증 ===
                    case "googleLogin":
                        runOnUiThread(MainActivity.this::startGoogleSignIn);
                        break;
                    case "logout":
                        runOnUiThread(MainActivity.this::signOut);
                        break;
                    case "getCurrentUser":
                        FirebaseUser user = mAuth.getCurrentUser();
                        if (user != null) {
                            Map<String, Object> ud = new HashMap<>();
                            ud.put("uid", user.getUid());
                            ud.put("displayName", user.getDisplayName());
                            ud.put("email", user.getEmail());
                            ud.put("photoUrl", user.getPhotoUrl() != null ? user.getPhotoUrl().toString() : "");
                            sendToWebView("onCurrentUser", gson.toJson(ud));
                        } else {
                            sendToWebView("onCurrentUser", "null");
                        }
                        break;

                    // === Firestore - 할 일 ===
                    case "getTodos":
                        firestoreManager.getTodos(
                            data.optString("profileId"),
                            data.optString("date"),
                            todos -> sendToWebView("onTodos", gson.toJson(todos))
                        );
                        break;
                    case "saveTodo":
                        firestoreManager.saveTodo(data, todoId ->
                            sendToWebView("onTodoSaved", "{\"id\":\"" + todoId + "\"}"));
                        break;
                    case "deleteTodo":
                        firestoreManager.deleteTodo(data.optString("id"), () ->
                            sendToWebView("onTodoDeleted", "{\"id\":\"" + data.optString("id") + "\"}"));
                        break;
                    case "completeTodo":
                        firestoreManager.completeTodo(
                            data.optString("id"),
                            data.optBoolean("completed"),
                            result -> sendToWebView("onTodoCompleted", gson.toJson(result))
                        );
                        break;

                    // === Firestore - 프로필 ===
                    // 내 프로필만 (내 ownerUid 기준)
                    case "getProfiles":
                    case "getMyProfiles":
                        firestoreManager.getMyProfiles(
                            profiles -> sendToWebView("onProfiles", gson.toJson(profiles))
                        );
                        break;

                    // 팀원 프로필 (isMyAccount=true 기준)
                    case "getTeamMemberProfiles":
                        firestoreManager.getTeamMemberProfiles(
                            profiles -> sendToWebView("onTeamMemberProfiles", gson.toJson(profiles))
                        );
                        break;

                    // 프로필 선택 화면용: 내 프로필 + 팀원 프로필 동시 요청
                    case "getProfilesForSelection":
                        firestoreManager.getProfilesForSelection(
                            myProfiles  -> sendToWebView("onMyProfiles",   gson.toJson(myProfiles)),
                            teamProfiles -> sendToWebView("onTeamProfiles", gson.toJson(teamProfiles))
                        );
                        break;

                    case "saveProfile":
                        firestoreManager.saveProfile(data, profileId ->
                            sendToWebView("onProfileSaved", "{\"id\":\"" + profileId + "\"}"));
                        break;
                    case "deleteProfile":
                        firestoreManager.deleteProfile(data.optString("id"), () ->
                            sendToWebView("onProfileDeleted", "{}"));
                        break;

                    // === Firestore - 팀 ===
                    case "getTeams":
                        firestoreManager.getTeams(
                            teams -> sendToWebView("onTeams", gson.toJson(teams))
                        );
                        break;
                    case "joinTeam":
                        firestoreManager.joinTeam(data.optString("teamId"), () ->
                            sendToWebView("onTeamJoined", "{}"));
                        break;
                    case "leaveTeam":
                        firestoreManager.leaveTeam(data.optString("teamId"), () ->
                            sendToWebView("onTeamLeft", "{}"));
                        break;

                    // === Firestore - 따봉 ===
                    case "addThumbsUp":
                        firestoreManager.addThumbsUp(data, () ->
                            sendToWebView("onThumbsUpAdded", "{}"));
                        break;
                    case "removeThumbsUp":
                        firestoreManager.removeThumbsUp(
                            data.optString("todoId"),
                            () -> sendToWebView("onThumbsUpRemoved", "{}"));
                        break;

                    // === Firestore - 목표 ===
                    case "getGoals":
                        firestoreManager.getGoals(
                            data.optString("profileId"),
                            goals -> sendToWebView("onGoals", gson.toJson(goals))
                        );
                        break;
                    case "saveGoal":
                        firestoreManager.saveGoal(data, goalId ->
                            sendToWebView("onGoalSaved", "{\"id\":\"" + goalId + "\"}"));
                        break;
                    case "deleteGoal":
                        firestoreManager.deleteGoal(data.optString("id"), () ->
                            sendToWebView("onGoalDeleted", "{}"));
                        break;

                    // === 알람 ===
                    case "setAlarm":
                        alarmManager.setAlarm(
                            data.optString("id"),
                            data.optString("title"),
                            data.optLong("timeMillis"),
                            data.optBoolean("isMorningCall", false)
                        );
                        sendToWebView("onAlarmSet", "{}");
                        break;
                    case "cancelAlarm":
                        alarmManager.cancelAlarm(data.optString("id"));
                        sendToWebView("onAlarmCancelled", "{}");
                        break;

                    // === 포인트 ===
                    case "getPoints":
                        firestoreManager.getTodayPoints(
                            data.optString("profileId"),
                            points -> sendToWebView("onPoints", "{\"points\":" + points + "}")
                        );
                        break;

                    // === 오늘의 명언 ===
                    case "getDailyQuote":
                        firestoreManager.getDailyQuote(
                            quote -> sendToWebView("onDailyQuote", gson.toJson(quote))
                        );
                        break;

                    // === URL 링크 관리 ===
                    case "saveLink":
                        firestoreManager.saveLink(data, () ->
                            sendToWebView("onLinkSaved", "{}"));
                        break;
                    case "deleteLink":
                        firestoreManager.deleteLink(
                            data.optString("todoId"),
                            data.optString("url"),
                            () -> sendToWebView("onLinkDeleted", "{}"));
                        break;
                    case "getThumbsUps":
                        firestoreManager.getThumbsUps(
                            data.optString("todoId"),
                            rows -> sendToWebView("onThumbsUps", gson.toJson(rows))
                        );
                        break;
                    case "seedSampleData":
                        firestoreManager.seedSampleData(ok -> {
                            if (Boolean.TRUE.equals(ok)) {
                                sendToWebView("onSampleSeeded", "{}");
                            } else {
                                sendToWebView("onError", "{\"error\":\"seedSampleDataFailed\"}");
                            }
                        });
                        break;
                    case "resetMyData":
                        firestoreManager.resetMyData(() -> sendToWebView("onDataReset", "{}"));
                        break;

                    default:
                        sendToWebView("onError", "{\"error\":\"Unknown method: " + method + "\"}");
                }
            } catch (Exception e) {
                sendToWebView("onError", "{\"error\":\"" + e.getMessage() + "\"}");
            }
        }
    }
}
