package com.teamtodo.app.data;

import android.content.Context;
import android.util.Log;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.Timestamp;
import com.google.firebase.firestore.DocumentReference;
import com.google.firebase.firestore.FieldValue;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.Query;
import com.google.firebase.firestore.QueryDocumentSnapshot;
import org.json.JSONObject;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Calendar;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

public class FirestoreManager {

    private static final String TAG = "FirestoreManager";
    private final Context context;
    private final FirebaseFirestore db;
    private final FirebaseAuth auth;

    // ???汝뷴젆???????????
    public static final String COL_USERS = "users";
    public static final String COL_PROFILES = "profiles";
    public static final String COL_TODOS = "todos";
    public static final String COL_THUMBSUP = "thumbsup";
    public static final String COL_TEAMS = "teams";
    public static final String COL_GOALS = "goals";
    public static final String COL_QUOTES = "quotes";

    public interface Callback<T> { void onResult(T result); }
    public interface VoidCallback { void onDone(); }

    static String buildSampleTeamDocId(String uid) {
        if (uid == null) return "sample-team-anon";
        String safeUid = uid.replaceAll("[^A-Za-z0-9_-]", "");
        if (safeUid.length() > 20) safeUid = safeUid.substring(0, 20);
        if (safeUid.isEmpty()) safeUid = "anon";
        return "sample-team-" + safeUid;
    }

    public FirestoreManager(Context context, FirebaseFirestore db, FirebaseAuth auth) {
        this.context = context;
        this.db = db;
        this.auth = auth;
    }

    private String getUid() {
        FirebaseUser user = auth.getCurrentUser();
        return user != null ? user.getUid() : null;
    }

    // =============================
    // ??????????怨멸텛???
    // =============================
    public void createOrUpdateUser(FirebaseUser user, VoidCallback callback) {
        Map<String, Object> userData = new HashMap<>();
        userData.put("uid", user.getUid());
        userData.put("displayName", user.getDisplayName());
        userData.put("email", user.getEmail());
        userData.put("photoUrl", user.getPhotoUrl() != null ? user.getPhotoUrl().toString() : "");
        userData.put("updatedAt", FieldValue.serverTimestamp());

        db.collection(COL_USERS).document(user.getUid())
            .set(userData, com.google.firebase.firestore.SetOptions.merge())
            .addOnSuccessListener(aVoid -> {
                if (callback != null) callback.onDone();
            })
            .addOnFailureListener(e -> {
                Log.e(TAG, "createOrUpdateUser failed", e);
                if (callback != null) callback.onDone();
            });
    }

    // =============================
    // ?????獄쏅챶留??貫?????????怨멸텛???
    // =============================

    /**
     * ?????ル뒌????饔낅떽???嶺뚮슢梨뜹ㅇ????'(???????獄쏅챶留??貫????' ?耀붾굝?????????붾눀?袁⑸븸亦껋꼷伊???- ownerUid == ??uid
     * ???黎앸럽??筌뚭퍏??????? ??傭?끆?????????????????獄쏅챶留??貫???????獄쏅챶留??泳?뿀??????μ떜媛?걫?????????μ떜媛?걫???
     */
    public void getMyProfiles(Callback<List<Map<String, Object>>> callback) {
        String uid = getUid();
        if (uid == null) { callback.onResult(new ArrayList<>()); return; }

        db.collection(COL_PROFILES)
            .whereEqualTo("ownerUid", uid)
            .get()
            .addOnSuccessListener(snapshots -> {
                List<Map<String, Object>> profiles = new ArrayList<>();
                for (QueryDocumentSnapshot doc : snapshots) {
                    Map<String, Object> profile = doc.getData();
                    profile.put("id", doc.getId());
                    profile.put("isMine", true);  // ???????獄쏅챶留??貫????????????
                    profiles.add(profile);
                }
                callback.onResult(profiles);
            })
            .addOnFailureListener(e -> {
                Log.e(TAG, "getMyProfiles failed", e);
                callback.onResult(new ArrayList<>());
            });
    }

    // ?????곌떽釉붾?궘???饔낅떽???嶺뚮슢梨뜹ㅇ???(getProfiles -> getMyProfiles ???轅붽틓?????
    public void getProfiles(Callback<List<Map<String, Object>>> callback) {
        getMyProfiles(callback);
    }

    /**
     * ?????????獄쏅챶留??貫?????耀붾굝?????????붾눀?袁⑸븸亦껋꼷伊???- ??? ?????ル뒌??????????????耀붾굝???????怨뚮뼺???????? ???轅붽틓????????傭?끆??????猷????????怨멸텛????????獄쏅챶留??貫????
     * ?????獄쏅챶留??貫????????壤굿??Β??????椰?壤???'???? ??????????????
     * ????: {teamId -> [profiles...]}
     */
    public void getTeamMemberProfiles(Callback<List<Map<String, Object>>> callback) {
        String uid = getUid();
        if (uid == null) { callback.onResult(new ArrayList<>()); return; }

        // 1) ??? ?????ル뒌??????????? ?耀붾굝?????????붾눀?袁⑸븸亦껋꼷伊???????⑥ル?????
        db.collection(COL_TEAMS)
            .whereArrayContains("members", uid)
            .get()
            .addOnSuccessListener(teamSnapshots -> {
                if (teamSnapshots.isEmpty()) {
                    callback.onResult(new ArrayList<>());
                    return;
                }

                // 2) ?????????uid ?????곌떽釉붾??(?????轅붽틓?????
                java.util.Set<String> memberUids = new java.util.HashSet<>();
                for (QueryDocumentSnapshot team : teamSnapshots) {
                    List<?> members = (List<?>) team.get("members");
                    if (members != null) {
                        for (Object m : members) {
                            if (m instanceof String && !m.equals(uid)) {
                                memberUids.add((String) m);
                            }
                        }
                    }
                }

                if (memberUids.isEmpty()) {
                    callback.onResult(new ArrayList<>());
                    return;
                }

                // 3) ?????????????獄쏅챶留??貫????????⑥ル?????(isMyAccount=true ???????????????????獄쏅챶留??貫????
                // Firestore whereIn ???????? ???????????ㅻ쿋??10??
                List<String> memberList = new ArrayList<>(memberUids);
                int maxBatch = Math.min(memberList.size(), 10);
                List<String> batchUids = memberList.subList(0, maxBatch);

                db.collection(COL_PROFILES)
                    .whereIn("ownerUid", batchUids)
                    .whereEqualTo("isMyAccount", true)  // ?耀붾굝????????????獄쏅챶留??貫???????獄쏅챶留????
                    .get()
                    .addOnSuccessListener(profileSnapshots -> {
                        List<Map<String, Object>> profiles = new ArrayList<>();
                        java.util.Set<String> addedUids = new java.util.HashSet<>();

                        for (QueryDocumentSnapshot doc : profileSnapshots) {
                            String ownerUid = doc.getString("ownerUid");
                            if (ownerUid != null && !addedUids.contains(ownerUid)) {
                                Map<String, Object> profile = doc.getData();
                                profile.put("id", doc.getId());
                                profile.put("isMine", false);   // ?????????獄쏅챶留??貫????????????
                                profiles.add(profile);
                                addedUids.add(ownerUid);
                            }
                        }
                        callback.onResult(profiles);
                    })
                    .addOnFailureListener(e -> {
                        Log.e(TAG, "getTeamMemberProfiles failed", e);
                        callback.onResult(new ArrayList<>());
                    });
            })
            .addOnFailureListener(e -> {
                Log.e(TAG, "getTeams for profiles failed", e);
                callback.onResult(new ArrayList<>());
            });
    }

    /**
     * ?????獄쏅챶留??貫????????壤굿??Β??????椰?壤??? ???????獄쏅챶留??貫????+ ?????????獄쏅챶留??貫???????? ?????밸븶筌믩끃????
     * result.myProfiles: ???????獄쏅챶留??貫?????耀붾굝?????????붾눀?袁⑸븸亦껋꼷伊???(isMine=true)
     * result.teamProfiles: ?????????獄쏅챶留??貫?????耀붾굝?????????붾눀?袁⑸븸亦껋꼷伊???(isMine=false)
     */
    public void getProfilesForSelection(
        Callback<List<Map<String, Object>>> myCallback,
        Callback<List<Map<String, Object>>> teamCallback
    ) {
        getMyProfiles(myCallback);
        getTeamMemberProfiles(teamCallback);
    }

    public void saveProfile(JSONObject data, Callback<String> callback) {
        String uid = getUid();
        if (uid == null) { callback.onResult(null); return; }

        boolean isMyAccount = data.optBoolean("isMyAccount", false);
        Map<String, Object> profileData = new HashMap<>();
        profileData.put("ownerUid", uid);
        profileData.put("name", data.optString("name", "Profile"));
        profileData.put("avatarType", data.optString("avatarType", "male"));
        profileData.put("avatarIndex", data.optInt("avatarIndex", 0));
        profileData.put("photoUrl", data.optString("photoUrl", ""));
        profileData.put("isMyAccount", isMyAccount);
        profileData.put("themeColor", data.optString("themeColor", "#FFB3C6"));
        profileData.put("updatedAt", FieldValue.serverTimestamp());

        String existingProfileId = data.optString("id", "");
        DocumentReference targetRef;
        if (!existingProfileId.isEmpty()) {
            targetRef = db.collection(COL_PROFILES).document(existingProfileId);
        } else {
            profileData.put("createdAt", FieldValue.serverTimestamp());
            targetRef = db.collection(COL_PROFILES).document();
        }
        String targetProfileId = targetRef.getId();

        if (isMyAccount) {
            db.collection(COL_PROFILES)
                .whereEqualTo("ownerUid", uid)
                .get()
                .addOnSuccessListener(snapshots -> {
                    com.google.firebase.firestore.WriteBatch batch = db.batch();
                    for (QueryDocumentSnapshot doc : snapshots) {
                        if (!doc.getId().equals(targetProfileId)) {
                            batch.update(doc.getReference(), "isMyAccount", false);
                        }
                    }
                    batch.set(targetRef, profileData, com.google.firebase.firestore.SetOptions.merge());
                    batch.commit()
                        .addOnSuccessListener(aVoid -> callback.onResult(targetProfileId))
                        .addOnFailureListener(e -> Log.e(TAG, "saveProfile batch failed", e));
                })
                .addOnFailureListener(e -> Log.e(TAG, "saveProfile prequery failed", e));
        } else {
            targetRef.set(profileData, com.google.firebase.firestore.SetOptions.merge())
                .addOnSuccessListener(aVoid -> callback.onResult(targetProfileId))
                .addOnFailureListener(e -> Log.e(TAG, "saveProfile failed", e));
        }
    }

    public void deleteProfile(String profileId, VoidCallback callback) {
        db.collection(COL_PROFILES).document(profileId)
            .delete()
            .addOnSuccessListener(aVoid -> { if (callback != null) callback.onDone(); })
            .addOnFailureListener(e -> Log.e(TAG, "deleteProfile failed", e));
    }

    // =============================
    // ?????????怨멸텛???(Firestore limit ?????怨멸텛???
    // =============================
    public void getTodos(String profileId, String dateStr, Callback<List<Map<String, Object>>> callback) {
        if (profileId == null || profileId.isEmpty()) {
            callback.onResult(new ArrayList<>()); return;
        }

        // dateStr?????????ㅻ쑄?癲ル슢캉??뮻?????癲ル슢???????????먃?
        if (dateStr == null || dateStr.isEmpty()) {
            dateStr = new SimpleDateFormat("yyyy-MM-dd", Locale.KOREA).format(new Date());
        }
        final String finalDate = dateStr;

        // 1) ?????????????먃??????????????(startDate <= date <= endDate) - ?耀붾굝??????鶯? 1?????????
        // DB ???????ロ렰????????????????ш내?℡ㅇ???????????
        db.collection(COL_TODOS)
            .whereEqualTo("profileId", profileId)
            .whereLessThanOrEqualTo("startDate", finalDate)
            .limit(100)
            .get()
            .addOnSuccessListener(snapshots -> {
                List<Map<String, Object>> todos = new ArrayList<>();
                for (QueryDocumentSnapshot doc : snapshots) {
                    Map<String, Object> todo = doc.getData();
                    todo.put("id", doc.getId());

                    // endDate ?????ш내?℡ㅇ????븐뼐????(Firestore??????????????????????????????Β?ル윲??)
                    String endDate = doc.getString("endDate");
                    if (endDate == null || endDate.isEmpty() || endDate.compareTo(finalDate) >= 0) {
                        // ????椰????????ш내?℡ㅇ????븐뼐????
                        List<?> daysOfWeek = (List<?>) doc.get("daysOfWeek");
                        if (daysOfWeek == null || daysOfWeek.isEmpty() || isMatchingDayOfWeek(finalDate, daysOfWeek)) {
                            todos.add(todo);
                        }
                    }
                }
                callback.onResult(todos);
            })
            .addOnFailureListener(e -> {
                Log.e(TAG, "getTodos failed", e);
                callback.onResult(new ArrayList<>());
            });
    }

    private boolean isMatchingDayOfWeek(String dateStr, List<?> daysOfWeek) {
        try {
            SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd", Locale.KOREA);
            Date date = sdf.parse(dateStr);
            Calendar cal = Calendar.getInstance();
            if (date != null) cal.setTime(date);
            int dayOfWeek = cal.get(Calendar.DAY_OF_WEEK) - 1; // 0=?? 6=??
            for (Object day : daysOfWeek) {
                if (day instanceof Long && ((Long) day).intValue() == dayOfWeek) return true;
                if (day instanceof Integer && (Integer) day == dayOfWeek) return true;
            }
            return false;
        } catch (Exception e) {
            return true;
        }
    }

    public void saveTodo(JSONObject data, Callback<String> callback) {
        String uid = getUid();
        if (uid == null) { callback.onResult(null); return; }

        Map<String, Object> todoData = new HashMap<>();
        todoData.put("profileId", data.optString("profileId"));
        todoData.put("title", data.optString("title", ""));
        todoData.put("startDate", data.optString("startDate", ""));
        todoData.put("endDate", data.optString("endDate", ""));
        todoData.put("startTime", data.optString("startTime", ""));
        todoData.put("endTime", data.optString("endTime", ""));
        todoData.put("points", data.optInt("points", 10));
        todoData.put("isCompleted", data.optBoolean("isCompleted", false));
        todoData.put("parentTodoId", data.optString("parentTodoId", ""));
        todoData.put("ownerUid", uid);
        todoData.put("updatedAt", FieldValue.serverTimestamp());

        // ????椰???????μ떜媛?걫???
        try {
            org.json.JSONArray daysArray = data.optJSONArray("daysOfWeek");
            if (daysArray != null) {
                List<Integer> days = new ArrayList<>();
                for (int i = 0; i < daysArray.length(); i++) days.add(daysArray.getInt(i));
                todoData.put("daysOfWeek", days);
            } else {
                todoData.put("daysOfWeek", new ArrayList<>());
            }
        } catch (Exception e) {
            todoData.put("daysOfWeek", new ArrayList<>());
        }

        String todoId = data.optString("id", "");
        if (!todoId.isEmpty()) {
            db.collection(COL_TODOS).document(todoId)
                .set(todoData, com.google.firebase.firestore.SetOptions.merge())
                .addOnSuccessListener(aVoid -> callback.onResult(todoId))
                .addOnFailureListener(e -> {
                    Log.e(TAG, "saveTodo update failed", e);
                    callback.onResult("");
                });
        } else {
            todoData.put("createdAt", FieldValue.serverTimestamp());
            db.collection(COL_TODOS).add(todoData)
                .addOnSuccessListener(docRef -> callback.onResult(docRef.getId()))
                .addOnFailureListener(e -> {
                    Log.e(TAG, "saveTodo add failed", e);
                    callback.onResult("");
                });
        }
    }

    public void deleteTodo(String todoId, VoidCallback callback) {
        db.collection(COL_TODOS).document(todoId)
            .delete()
            .addOnSuccessListener(aVoid -> { if (callback != null) callback.onDone(); })
            .addOnFailureListener(e -> Log.e(TAG, "deleteTodo failed", e));
    }

    public void completeTodo(String todoId, boolean completed, Callback<Map<String, Object>> callback) {
        Map<String, Object> updates = new HashMap<>();
        updates.put("isCompleted", completed);
        updates.put("completedAt", completed ? FieldValue.serverTimestamp() : null);

        db.collection(COL_TODOS).document(todoId)
            .update(updates)
            .addOnSuccessListener(aVoid -> {
                // ?????????????댁댉??????諛몃마???
                db.collection(COL_TODOS).document(todoId).get()
                    .addOnSuccessListener(doc -> {
                        Map<String, Object> result = new HashMap<>();
                        result.put("id", todoId);
                        result.put("completed", completed);
                        result.put("points", doc.getLong("points") != null ? doc.getLong("points") : 0);
                        callback.onResult(result);
                    });
            })
            .addOnFailureListener(e -> Log.e(TAG, "completeTodo failed", e));
    }

    // =============================
    // ??????????????怨멸텛???
    // =============================
    public void addThumbsUp(JSONObject data, VoidCallback callback) {
        String giverUid = getUid();
        if (giverUid == null) return;

        String todoId = data.optString("todoId");
        String receiverUid = data.optString("receiverUid", ""); // ??????????uid
        String receiverProfileId = data.optString("receiverProfileId", "");
        String comment = data.optString("comment", "");

        // ???????????????關?쒎첎?嫄??怨몄굛????????????????Β?ル윲??
        if (giverUid.equals(receiverUid)) {
            Log.w(TAG, "Cannot thumbsup own todo");
            if (callback != null) callback.onDone();
            return;
        }

        // ?????袁ⓦ걤???ш낄猷????????밸븶??縕ユ쾮?: ???????살퓢??giverUid + todoId
        db.collection(COL_THUMBSUP)
            .whereEqualTo("giverUid", giverUid)
            .whereEqualTo("todoId", todoId)
            .get()
            .addOnSuccessListener(snapshots -> {
                if (!snapshots.isEmpty()) {
                    // ???? ??????????
                    if (callback != null) callback.onDone();
                    return;
                }

                // ????????????癲????????
                Map<String, Object> thumbsData = new HashMap<>();
                thumbsData.put("todoId", todoId);
                thumbsData.put("giverUid", giverUid);
                thumbsData.put("receiverUid", receiverUid);
                thumbsData.put("receiverProfileId", receiverProfileId);
                thumbsData.put("comment", comment);
                thumbsData.put("createdAt", FieldValue.serverTimestamp());

                db.collection(COL_THUMBSUP).add(thumbsData)
                    .addOnSuccessListener(docRef -> {
                        // 1) ??????????????袁ⓦ걤?嶺뚯쉶?????렢? ????giver)?????points +1 (?????곕츥?嶺뚮?爰??????癲?????????곌떽釉붾???????????????
                        addPoints(giverUid, 1);

                        // 2) ??????????????밸븶筌믩끃??? ????receiver)?????thumbsupPoints +2
                        //    (?????곕츥????????????? thumbsupPoints ?????獄쏅챶留???????????饔낅떽???????깃묄???耀붾굝???壤굿?怨쀫ぁ???筌? ?????ル뒌????
                        if (!receiverUid.isEmpty()) {
                            addThumbsupPoints(receiverUid, 2);
                        }

                        if (callback != null) callback.onDone();
                    })
                    .addOnFailureListener(e -> Log.e(TAG, "addThumbsUp failed", e));
            })
            .addOnFailureListener(e -> Log.e(TAG, "addThumbsUp query failed", e));
    }

    public void removeThumbsUp(String todoId, VoidCallback callback) {
        String uid = getUid();
        if (uid == null) return;

        db.collection(COL_THUMBSUP)
            .whereEqualTo("giverUid", uid)
            .whereEqualTo("todoId", todoId)
            .get()
            .addOnSuccessListener(snapshots -> {
                for (QueryDocumentSnapshot doc : snapshots) {
                    doc.getReference().delete();
                }
                if (callback != null) callback.onDone();
            });
    }

    // =============================
    // ?? ?????怨멸텛???
    // =============================
    public void getTeams(Callback<List<Map<String, Object>>> callback) {
        db.collection(COL_TEAMS)
            .limit(50)
            .get()
            .addOnSuccessListener(snapshots -> {
                List<Map<String, Object>> teams = new ArrayList<>();
                for (QueryDocumentSnapshot doc : snapshots) {
                    Map<String, Object> team = doc.getData();
                    team.put("id", doc.getId());
                    teams.add(team);
                }
                callback.onResult(teams);
            })
            .addOnFailureListener(e -> callback.onResult(new ArrayList<>()));
    }

    public void joinTeam(String teamId, VoidCallback callback) {
        String uid = getUid();
        if (uid == null) return;
        db.collection(COL_TEAMS).document(teamId)
            .update("members", FieldValue.arrayUnion(uid))
            .addOnSuccessListener(aVoid -> { if (callback != null) callback.onDone(); });
    }

    public void leaveTeam(String teamId, VoidCallback callback) {
        String uid = getUid();
        if (uid == null) return;
        db.collection(COL_TEAMS).document(teamId)
            .update("members", FieldValue.arrayRemove(uid))
            .addOnSuccessListener(aVoid -> { if (callback != null) callback.onDone(); });
    }

    // =============================
    // ?耀붾굝?????????붾눀???????????怨멸텛???
    // =============================
    public void getGoals(String profileId, Callback<List<Map<String, Object>>> callback) {
        if (profileId == null || profileId.isEmpty()) {
            callback.onResult(new ArrayList<>()); return;
        }

        db.collection(COL_GOALS)
            .whereEqualTo("profileId", profileId)
            .orderBy("createdAt", Query.Direction.DESCENDING)
            .limit(50)
            .get()
            .addOnSuccessListener(snapshots -> {
                List<Map<String, Object>> goals = new ArrayList<>();
                for (QueryDocumentSnapshot doc : snapshots) {
                    Map<String, Object> goal = doc.getData();
                    goal.put("id", doc.getId());
                    goals.add(goal);
                }
                callback.onResult(goals);
            })
            .addOnFailureListener(e -> callback.onResult(new ArrayList<>()));
    }

    public void saveGoal(JSONObject data, Callback<String> callback) {
        String uid = getUid();
        if (uid == null) { callback.onResult(null); return; }

        Map<String, Object> goalData = new HashMap<>();
        goalData.put("profileId", data.optString("profileId"));
        goalData.put("ownerUid", uid);
        goalData.put("title", data.optString("title", ""));
        goalData.put("category", data.optString("category", "???????"));
        goalData.put("period", data.optString("period", "Quarterly"));
        goalData.put("needsProgress", data.optBoolean("needsProgress", false));
        goalData.put("updatedAt", FieldValue.serverTimestamp());

        String goalId = data.optString("id", "");
        if (!goalId.isEmpty()) {
            db.collection(COL_GOALS).document(goalId)
                .set(goalData, com.google.firebase.firestore.SetOptions.merge())
                .addOnSuccessListener(aVoid -> callback.onResult(goalId));
        } else {
            goalData.put("createdAt", FieldValue.serverTimestamp());
            db.collection(COL_GOALS).add(goalData)
                .addOnSuccessListener(docRef -> callback.onResult(docRef.getId()));
        }
    }

    public void deleteGoal(String goalId, VoidCallback callback) {
        db.collection(COL_GOALS).document(goalId)
            .delete()
            .addOnSuccessListener(aVoid -> { if (callback != null) callback.onDone(); });
    }

    // =============================
    // ??????????怨멸텛???
    // =============================

    // ?????곕츥?嶺뚮?爰????????????????????耀붾굝???壤굿?怨쀫ぁ???筌? (?????????獄쏅챶留?? ???????????????밸쫫?????욱룏????
    public void addPoints(String uid, int points) {
        db.collection(COL_USERS).document(uid)
            .update("points", FieldValue.increment(points))
            .addOnFailureListener(e -> {
                // ???癲?????????諛몃만? ???????ㅻ쑄?癲ル슢캉??뮻?????ш끽維뽳쭩??????????
                Map<String, Object> init = new HashMap<>();
                init.put("points", points);
                init.put("thumbsupPoints", 0);
                db.collection(COL_USERS).document(uid)
                    .set(init, com.google.firebase.firestore.SetOptions.merge());
            });
    }

    // ??????????????밸븶筌믩끃??? ??????耀붾굝???壤굿?怨쀫ぁ???筌? (????饔낅떽???????깃묄????饔낅떽?????????????곕츥????????????? thumbsupPoints ?????獄쏅챶留???????????μ떜媛?걫??곷묄??
    public void addThumbsupPoints(String receiverUid, int points) {
        db.collection(COL_USERS).document(receiverUid)
            .update("thumbsupPoints", FieldValue.increment(points))
            .addOnFailureListener(e -> {
                // ???癲?????????諛몃만? ???????ㅻ쑄?癲ル슢캉??뮻?????ш끽維뽳쭩???(?耀붾굝??????鶯????????????????밸븶筌믩끃??????汝뷴젆?琉????
                Map<String, Object> init = new HashMap<>();
                init.put("thumbsupPoints", points);
                db.collection(COL_USERS).document(receiverUid)
                    .set(init, com.google.firebase.firestore.SetOptions.merge());
            });
    }

    // ???????= points + thumbsupPoints
    public void getTotalPoints(String uid, Callback<Integer> callback) {
        db.collection(COL_USERS).document(uid).get()
            .addOnSuccessListener(doc -> {
                int pts = doc.getLong("points") != null ? doc.getLong("points").intValue() : 0;
                int thumbPts = doc.getLong("thumbsupPoints") != null ? doc.getLong("thumbsupPoints").intValue() : 0;
                callback.onResult(pts + thumbPts);
            })
            .addOnFailureListener(e -> callback.onResult(0));
    }

    public void getTodayPoints(String profileId, Callback<Integer> callback) {
        if (profileId == null || profileId.isEmpty()) {
            callback.onResult(0);
            return;
        }
        Calendar cal = Calendar.getInstance(Locale.KOREA);
        cal.set(Calendar.HOUR_OF_DAY, 0);
        cal.set(Calendar.MINUTE, 0);
        cal.set(Calendar.SECOND, 0);
        cal.set(Calendar.MILLISECOND, 0);
        Date start = cal.getTime();
        cal.add(Calendar.DAY_OF_MONTH, 1);
        Date end = cal.getTime();
        Timestamp startTs = new Timestamp(start);
        Timestamp endTs = new Timestamp(end);

        db.collection(COL_TODOS)
            .whereEqualTo("profileId", profileId)
            .whereEqualTo("isCompleted", true)
            .whereGreaterThanOrEqualTo("completedAt", startTs)
            .whereLessThan("completedAt", endTs)
            .limit(100)
            .get()
            .addOnSuccessListener(snapshots -> {
                int totalPoints = 0;
                for (QueryDocumentSnapshot doc : snapshots) {
                    Long pts = doc.getLong("points");
                    if (pts != null) totalPoints += pts;
                }
                callback.onResult(totalPoints);
            })
            .addOnFailureListener(e -> callback.onResult(0));
    }

    public void getThumbsUps(String todoId, Callback<List<Map<String, Object>>> callback) {
        if (todoId == null || todoId.isEmpty()) {
            callback.onResult(new ArrayList<>());
            return;
        }
        db.collection(COL_THUMBSUP)
            .whereEqualTo("todoId", todoId)
            .limit(200)
            .get()
            .addOnSuccessListener(snapshots -> {
                List<Map<String, Object>> list = new ArrayList<>();
                for (QueryDocumentSnapshot doc : snapshots) {
                    Map<String, Object> row = doc.getData();
                    row.put("id", doc.getId());
                    list.add(row);
                }
                callback.onResult(list);
            })
            .addOnFailureListener(e -> callback.onResult(new ArrayList<>()));
    }

    public void seedSampleData(Callback<Boolean> callback) {
        String uid = getUid();
        if (uid == null) {
            if (callback != null) callback.onResult(false);
            return;
        }

        String today = new SimpleDateFormat("yyyy-MM-dd", Locale.KOREA).format(new Date());
        Calendar cal = Calendar.getInstance(Locale.KOREA);
        cal.add(Calendar.DAY_OF_MONTH, 7);
        String nextWeek = new SimpleDateFormat("yyyy-MM-dd", Locale.KOREA).format(cal.getTime());

        getMyProfiles(profiles -> {
            DocumentReference profileRef;
            if (profiles == null || profiles.isEmpty()) {
                profileRef = db.collection(COL_PROFILES).document();
            } else {
                Object existingId = profiles.get(0).get("id");
                String profileId = existingId != null ? String.valueOf(existingId) : "";
                profileRef = profileId.isEmpty()
                    ? db.collection(COL_PROFILES).document()
                    : db.collection(COL_PROFILES).document(profileId);
            }
            String profileId = profileRef.getId();

            com.google.firebase.firestore.WriteBatch batch = db.batch();

            Map<String, Object> profileData = new HashMap<>();
            profileData.put("ownerUid", uid);
            profileData.put("name", "My Profile");
            profileData.put("avatarType", "female");
            profileData.put("avatarIndex", 0);
            profileData.put("isMyAccount", true);
            profileData.put("themeColor", "#EB4770");
            profileData.put("updatedAt", FieldValue.serverTimestamp());
            profileData.put("createdAt", FieldValue.serverTimestamp());
            batch.set(profileRef, profileData, com.google.firebase.firestore.SetOptions.merge());

            DocumentReference todoA = db.collection(COL_TODOS).document();
            DocumentReference todoB = db.collection(COL_TODOS).document();
            DocumentReference parent = db.collection(COL_TODOS).document();
            DocumentReference child1 = db.collection(COL_TODOS).document();
            DocumentReference child2 = db.collection(COL_TODOS).document();

            Map<String, Object> t1 = new HashMap<>();
            t1.put("profileId", profileId);
            t1.put("title", "Morning Meditation");
            t1.put("startDate", today);
            t1.put("endDate", nextWeek);
            t1.put("startTime", "09:00");
            t1.put("endTime", "09:20");
            t1.put("points", 30);
            t1.put("isCompleted", false);
            t1.put("parentTodoId", "");
            t1.put("ownerUid", uid);
            t1.put("daysOfWeek", Arrays.asList(1, 2, 3, 4, 5));
            t1.put("createdAt", FieldValue.serverTimestamp());
            t1.put("updatedAt", FieldValue.serverTimestamp());
            batch.set(todoA, t1, com.google.firebase.firestore.SetOptions.merge());

            Map<String, Object> t2 = new HashMap<>();
            t2.put("profileId", profileId);
            t2.put("title", "2PM Stretching");
            t2.put("startDate", today);
            t2.put("endDate", nextWeek);
            t2.put("startTime", "14:00");
            t2.put("endTime", "14:20");
            t2.put("points", 40);
            t2.put("isCompleted", false);
            t2.put("parentTodoId", "");
            t2.put("ownerUid", uid);
            t2.put("daysOfWeek", Arrays.asList(1, 3, 5));
            t2.put("createdAt", FieldValue.serverTimestamp());
            t2.put("updatedAt", FieldValue.serverTimestamp());
            batch.set(todoB, t2, com.google.firebase.firestore.SetOptions.merge());

            Map<String, Object> p = new HashMap<>();
            p.put("profileId", profileId);
            p.put("title", "Drink 2L Water");
            p.put("startDate", today);
            p.put("endDate", nextWeek);
            p.put("startTime", "10:00");
            p.put("endTime", "21:00");
            p.put("points", 40);
            p.put("isCompleted", false);
            p.put("parentTodoId", "");
            p.put("ownerUid", uid);
            p.put("daysOfWeek", Arrays.asList(0, 1, 2, 3, 4, 5, 6));
            p.put("createdAt", FieldValue.serverTimestamp());
            p.put("updatedAt", FieldValue.serverTimestamp());
            batch.set(parent, p, com.google.firebase.firestore.SetOptions.merge());

            Map<String, Object> c1 = new HashMap<>(p);
            c1.put("title", "500ml ?轅붽틓??????猷몄??(1)");
            c1.put("points", 20);
            c1.put("parentTodoId", parent.getId());
            batch.set(child1, c1, com.google.firebase.firestore.SetOptions.merge());

            Map<String, Object> c2 = new HashMap<>(p);
            c2.put("title", "500ml ?轅붽틓??????猷몄??(2)");
            c2.put("points", 20);
            c2.put("parentTodoId", parent.getId());
            batch.set(child2, c2, com.google.firebase.firestore.SetOptions.merge());

            DocumentReference g1 = db.collection(COL_GOALS).document();
            Map<String, Object> goal1 = new HashMap<>();
            goal1.put("profileId", profileId);
            goal1.put("ownerUid", uid);
            goal1.put("title", "?轅몄뫅?????????룸ℓ???????");
            goal1.put("category", "Health");
            goal1.put("period", "Quarter");
            goal1.put("needsProgress", true);
            goal1.put("createdAt", FieldValue.serverTimestamp());
            goal1.put("updatedAt", FieldValue.serverTimestamp());
            batch.set(g1, goal1, com.google.firebase.firestore.SetOptions.merge());

            DocumentReference g2 = db.collection(COL_GOALS).document();
            Map<String, Object> goal2 = new HashMap<>(goal1);
            goal2.put("title", "Get SQLD");
            goal2.put("category", "Certificate");
            goal2.put("period", "Quarterly");
            batch.set(g2, goal2, com.google.firebase.firestore.SetOptions.merge());

            DocumentReference g3 = db.collection(COL_GOALS).document();
            Map<String, Object> goal3 = new HashMap<>(goal1);
            goal3.put("title", "Save 500k KRW Monthly");
            goal3.put("category", "Finance");
            goal3.put("period", "Yearly");
            batch.set(g3, goal3, com.google.firebase.firestore.SetOptions.merge());

            DocumentReference team1 = db.collection(COL_TEAMS).document(buildSampleTeamDocId(uid));
            Map<String, Object> teamData = new HashMap<>();
            teamData.put("ownerUid", uid);
            teamData.put("name", "???????????濚??");
            teamData.put("description", "?????濚????????????");
            teamData.put("members", Arrays.asList(uid));
            teamData.put("createdAt", FieldValue.serverTimestamp());
            teamData.put("updatedAt", FieldValue.serverTimestamp());
            batch.set(team1, teamData, com.google.firebase.firestore.SetOptions.merge());

            batch.commit()
                .addOnSuccessListener(v -> {
                    if (callback != null) callback.onResult(true);
                })
                .addOnFailureListener(e -> {
                    Log.e(TAG, "seedSampleData failed", e);
                    if (callback != null) callback.onResult(false);
                });
        });
    }

    public void resetMyData(VoidCallback callback) {
        String uid = getUid();
        if (uid == null) {
            if (callback != null) callback.onDone();
            return;
        }

        db.collection(COL_TODOS).whereEqualTo("ownerUid", uid).get()
            .addOnSuccessListener(todoSnap -> {
                com.google.firebase.firestore.WriteBatch batch = db.batch();
                for (QueryDocumentSnapshot d : todoSnap) batch.delete(d.getReference());
                db.collection(COL_GOALS).whereEqualTo("ownerUid", uid).get()
                    .addOnSuccessListener(goalSnap -> {
                        for (QueryDocumentSnapshot d : goalSnap) batch.delete(d.getReference());
                        db.collection(COL_THUMBSUP).whereEqualTo("giverUid", uid).get()
                            .addOnSuccessListener(thumbSnap -> {
                                for (QueryDocumentSnapshot d : thumbSnap) batch.delete(d.getReference());
                                db.collection(COL_PROFILES).whereEqualTo("ownerUid", uid).get()
                                    .addOnSuccessListener(profileSnap -> {
                                        for (QueryDocumentSnapshot d : profileSnap) batch.delete(d.getReference());
                                        batch.commit()
                                            .addOnSuccessListener(v -> {
                                                db.collection(COL_USERS).document(uid)
                                                    .set(new HashMap<String, Object>() {{
                                                        put("points", 0);
                                                        put("thumbsupPoints", 0);
                                                    }}, com.google.firebase.firestore.SetOptions.merge())
                                                    .addOnCompleteListener(t -> {
                                                        if (callback != null) callback.onDone();
                                                    });
                                            })
                                            .addOnFailureListener(e -> {
                                                if (callback != null) callback.onDone();
                                            });
                                    })
                                    .addOnFailureListener(e -> {
                                        if (callback != null) callback.onDone();
                                    });
                            })
                            .addOnFailureListener(e -> {
                                if (callback != null) callback.onDone();
                            });
                    })
                    .addOnFailureListener(e -> {
                        if (callback != null) callback.onDone();
                    });
            })
            .addOnFailureListener(e -> {
                if (callback != null) callback.onDone();
            });
    }

    // =============================
    // ?耀붾굝????????궰?櫻뗫봿????????怨멸텛???
    // =============================
    public void saveLink(JSONObject data, VoidCallback callback) {
        String todoId = data.optString("todoId");
        String url = data.optString("url");
        String title = data.optString("title", url);

        Map<String, Object> linkData = new HashMap<>();
        linkData.put("url", url);
        linkData.put("title", title);

        db.collection(COL_TODOS).document(todoId)
            .update("links", FieldValue.arrayUnion(linkData))
            .addOnSuccessListener(aVoid -> { if (callback != null) callback.onDone(); });
    }

    public void deleteLink(String todoId, String url, VoidCallback callback) {
        db.collection(COL_TODOS).document(todoId).get()
            .addOnSuccessListener(doc -> {
                List<?> links = (List<?>) doc.get("links");
                if (links != null) {
                    for (Object link : links) {
                        if (link instanceof Map) {
                            Map<?, ?> linkMap = (Map<?, ?>) link;
                            if (url.equals(linkMap.get("url"))) {
                                db.collection(COL_TODOS).document(todoId)
                                    .update("links", FieldValue.arrayRemove(link))
                                    .addOnSuccessListener(aVoid -> { if (callback != null) callback.onDone(); });
                                return;
                            }
                        }
                    }
                }
                if (callback != null) callback.onDone();
            });
    }

    // =============================
    // ????癲ル슢??????耀붾굝????癲ル슢??㎖?밤뀋????몄뿴??
    // =============================
    public void getDailyQuote(Callback<Map<String, Object>> callback) {
        // ???????먃? ???????癲??됀??????????耀붾굝????癲ル슢??㎖?밤뀋????몄뿴??????壤굿??Β??(?耀붾굝????????????밸븶筌믩끃?????
        int dayOfYear = Calendar.getInstance().get(Calendar.DAY_OF_YEAR);

        db.collection(COL_QUOTES)
            .get()
            .addOnSuccessListener(snapshots -> {
                List<QueryDocumentSnapshot> docs = new ArrayList<>();
                for (QueryDocumentSnapshot doc : snapshots) docs.add(doc);
                if (!docs.isEmpty()) {
                    QueryDocumentSnapshot doc = docs.get(dayOfYear % docs.size());
                    Map<String, Object> quote = doc.getData();
                    quote.put("id", doc.getId());
                    callback.onResult(quote);
                } else {
                    // ?????????耀붾굝????癲ル슢??㎖?밤뀋????몄뿴??
                    Map<String, Object> defaultQuote = new HashMap<>();
                    defaultQuote.put("text", "????癲ル슢????????耀붾굝梨루땟??????????轅붽틓?????????ル뒌???????????耀붾굝?????????붾눀????????????獄쏅챶留????????μ떜媛?걫?????汝뷴젆?琉???????????곸죩.");
                    defaultQuote.put("author", "TeamTodo");
                    callback.onResult(defaultQuote);
                }
            })
            .addOnFailureListener(e -> {
                Map<String, Object> defaultQuote = new HashMap<>();
                defaultQuote.put("text", "??? ????????????곕츥????? ?耀붾굝???????????????곸죩.");
                defaultQuote.put("author", "TeamTodo");
                callback.onResult(defaultQuote);
            });
    }
}
