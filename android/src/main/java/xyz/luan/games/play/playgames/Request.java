package xyz.luan.games.play.playgames;

import java.io.ByteArrayOutputStream;
import java.util.Map;
import java.util.HashMap;

import io.flutter.plugin.common.MethodCall;
import io.flutter.plugin.common.MethodChannel;
import io.flutter.plugin.common.MethodChannel.MethodCallHandler;
import io.flutter.plugin.common.MethodChannel.Result;
import io.flutter.plugin.common.PluginRegistry.Registrar;
import io.flutter.plugin.common.PluginRegistry.ActivityResultListener;

import android.content.Context;
import android.content.Intent;
import android.net.Uri;
import android.graphics.Bitmap;
import android.graphics.drawable.BitmapDrawable;
import android.graphics.drawable.Drawable;
import android.util.Log;
import android.support.annotation.NonNull;

import com.google.android.gms.common.images.ImageManager;
import com.google.android.gms.games.AnnotatedData;

import com.google.android.gms.auth.api.Auth;
import com.google.android.gms.auth.api.signin.GoogleSignIn;
import com.google.android.gms.auth.api.signin.GoogleSignInAccount;
import com.google.android.gms.auth.api.signin.GoogleSignInClient;
import com.google.android.gms.auth.api.signin.GoogleSignInOptions;
import com.google.android.gms.auth.api.signin.GoogleSignInResult;
import com.google.android.gms.games.SnapshotsClient;
import com.google.android.gms.games.snapshot.Snapshot;
import com.google.android.gms.games.snapshot.SnapshotMetadata;
import com.google.android.gms.games.snapshot.SnapshotMetadataChange;
import com.google.android.gms.games.LeaderboardsClient;
import com.google.android.gms.games.leaderboard.LeaderboardVariant;
import com.google.android.gms.games.leaderboard.ScoreSubmissionData;
import com.google.android.gms.games.leaderboard.LeaderboardScore;

import com.google.android.gms.games.Games;
import com.google.android.gms.games.GamesClient;
import com.google.android.gms.games.Player;
import com.google.android.gms.games.PlayersClient;
import com.google.android.gms.tasks.OnFailureListener;
import com.google.android.gms.tasks.OnSuccessListener;
import java.io.IOException;
import java.util.Arrays;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.List;
import java.util.ArrayList;
import java.util.Iterator;

public class Request {

    private static final String TAG = Request.class.getCanonicalName();

    private GoogleSignInAccount currentAccount;
    private Registrar registrar;
    private MethodCall call;
    private Result result;
    private PlayGamesPlugin pluginRef;

    public Request(PlayGamesPlugin pluginRef, GoogleSignInAccount currentAccount, Registrar registrar, MethodCall call, Result result) {
        this.pluginRef = pluginRef;
        this.currentAccount = currentAccount;
        this.registrar = registrar;
        this.call = call;
        this.result = result;
    }

    public void handle() {
        if (currentAccount == null) {
            error("ERROR_NOT_SIGNED_IN", "You must be signed in to perform any other action.");
            return;
        }
        if (call.method.equals("getHiResImage")) {
            this.getHiResImage();
        } else if (call.method.equals("getIconImage")) {
            this.getIconImage();
        } else if (call.method.equals("unlockAchievementById")) {
            String id = call.argument("id");
            this.unlockAchievement(id);
        } else if (call.method.equals("unlockAchievementByName")) {
            String name = call.argument("name");
            this.unlockAchievement(getIdByName(name));
        } else if (call.method.equals("incrementAchievementById")) {
            String id = call.argument("id");
            int amount = call.argument("amount");
            this.incrementAchievement(id, amount);
        } else if (call.method.equals("incrementAchievementByName")) {
            String name = call.argument("name");
            int amount = call.argument("amount");
            this.incrementAchievement(getIdByName(name), amount);
        } else if (call.method.equals("setPopupOptions")) {
            boolean show = call.argument("show");
            int gravity = call.argument("gravity");
            this.setPopupOptions(show, gravity);
        } else if (call.method.equals("openSnapshot")) {
            String snapshotName = call.argument("snapshotName");
            this.openSnapshot(snapshotName);
        } else if (call.method.equals("saveSnapshot")) {
            String snapshotName = call.argument("snapshotName");
            String content = call.argument("content");
            Map<String, String> metadata = call.argument("metadata");
            this.saveSnapshot(snapshotName, content, metadata);
        } else if (call.method.equals("resolveSnapshotConflict")) {
            String snapshotName = call.argument("snapshotName");
            String conflictId = call.argument("conflictId");
            String content = call.argument("content");
            Map<String, String> metadata = call.argument("metadata");
            this.resolveSnapshotConflict(snapshotName, conflictId, content, metadata);
        } else if (call.method.equals("submitScoreByName")) {
            String leaderboardName = call.argument("leaderboardName");
            Long score = ((Number) call.argument("score")).longValue();
            this.submitScore(this.getIdByName(leaderboardName), score);
        } else if (call.method.equals("submitScoreById")) {
            String leaderboardId = call.argument("leaderboardId");
            Long score = ((Number) call.argument("score")).longValue();
            this.submitScore(leaderboardId, score);
        } else if (call.method.equals("loadPlayerCenteredScoresByName")) {
            String leaderboardName = call.argument("leaderboardName");
            String timeSpan = call.argument("timeSpan");
            String collectionType = call.argument("collectionType");
            int maxResults = call.argument("maxResults");
            boolean forceReload = call.argument("forceReload");
            this.loadPlayerCenteredScores(this.getIdByName(leaderboardName), timeSpan, collectionType, maxResults, forceReload);
        } else if (call.method.equals("loadPlayerCenteredScoresById")) {
            String leaderboardId = call.argument("leaderboardId");
            String timeSpan = call.argument("timeSpan");
            String collectionType = call.argument("collectionType");
            int maxResults = call.argument("maxResults");
            boolean forceReload = call.argument("forceReload");
            this.loadPlayerCenteredScores(leaderboardId, timeSpan, collectionType, maxResults, forceReload);
        } else if (call.method.equals("loadTopScoresByName")) {
            String leaderboardName = call.argument("leaderboardName");
            String timeSpan = call.argument("timeSpan");
            String collectionType = call.argument("collectionType");
            int maxResults = call.argument("maxResults");
            boolean forceReload = call.argument("forceReload");
            this.loadTopScores(this.getIdByName(leaderboardName), timeSpan, collectionType, maxResults, forceReload);
        } else if (call.method.equals("loadTopScoresById")) {
            String leaderboardId = call.argument("leaderboardId");
            String timeSpan = call.argument("timeSpan");
            String collectionType = call.argument("collectionType");
            int maxResults = call.argument("maxResults");
            boolean forceReload = call.argument("forceReload");
            this.loadTopScores(leaderboardId, timeSpan, collectionType, maxResults, forceReload);
        } else {
            result.notImplemented();
        }
    }

    private String getIdByName(String name) {
        Context ctx = registrar.context();
        int resId = ctx.getResources().getIdentifier(name, "string", ctx.getPackageName());
        return ctx.getString(resId);
    }

    public void unlockAchievement(String id) {
        Games.getAchievementsClient(registrar.activity(), currentAccount).unlockImmediate(id)
                .addOnSuccessListener(new OnSuccessListener<Void>() {
                    @Override
                    public void onSuccess(Void a) {
                        result(true);
                    }
                }).addOnFailureListener(new OnFailureListener() {
                    @Override
                    public void onFailure(@NonNull Exception e) {
                        Log.e(TAG, "Could not unlock achievement", e);
                        result(false);
                    }
                });
    }

    public void incrementAchievement(String id, int amount) {
        Games.getAchievementsClient(registrar.activity(), currentAccount).incrementImmediate(id, amount)
                .addOnSuccessListener(new OnSuccessListener<Boolean>() {
                    @Override
                    public void onSuccess(Boolean b) {
                        result(true);
                    }
                }).addOnFailureListener(new OnFailureListener() {
                    @Override
                    public void onFailure(@NonNull Exception e) {
                        Log.e(TAG, "Could not increment achievement", e);
                        result(false);
                    }
                });
    }

    public void setPopupOptions(boolean show, int gravity) {
        GamesClient gamesClient = Games.getGamesClient(registrar.activity(), currentAccount);
        if (show) {
            gamesClient.setViewForPopups(registrar.view());
            gamesClient.setGravityForPopups(gravity);
        } else {
            gamesClient.setViewForPopups(null);
        }
        result(true);
    }

    private void result(Object response) {
        result.success(response);
    }

    private void error(String type, Throwable e) {
        Log.e(TAG, "Unexpected error on " + type, e);
        error(type, e.getMessage());
    }

    private void error(String type, String message) {
        Map<String, Object> errorMap = new HashMap<>();
        errorMap.put("type", type);
        errorMap.put("message", message);
        result(errorMap);
    }

    public void getHiResImage() {
        PlayersClient playersClient = Games.getPlayersClient(registrar.activity(), currentAccount);
        playersClient.getCurrentPlayer().addOnSuccessListener(new OnSuccessListener<Player>() {
            @Override
            public void onSuccess(Player player) {
                readImage(player.getHiResImageUri());
            }
        });
    }

    public void getIconImage() {
        PlayersClient playersClient = Games.getPlayersClient(registrar.activity(), currentAccount);
        playersClient.getCurrentPlayer().addOnSuccessListener(new OnSuccessListener<Player>() {
            @Override
            public void onSuccess(Player player) {
                readImage(player.getHiResImageUri());
            }
        });
    }

    private void readImage(final Uri uri) {
        final Map<String, Object> data = new HashMap<>();
        if (uri == null) {
            data.put("bytes", null);
            result(data);
        }
        ImageManager manager = ImageManager.create(registrar.context());
        manager.loadImage(new ImageManager.OnImageLoadedListener() {
            @Override
            public void onImageLoaded(Uri uri1, Drawable drawable, boolean isRequestedDrawable) {
                Bitmap bitmap = ((BitmapDrawable) drawable).getBitmap();
                ByteArrayOutputStream stream = new ByteArrayOutputStream();
                bitmap.compress(Bitmap.CompressFormat.JPEG, 100, stream);
                byte[] bytes = stream.toByteArray();
                data.put("bytes", bytes);
                result(data);
            }
        }, uri);
    }

    private OnSuccessListener<SnapshotsClient.DataOrConflict<Snapshot>> generateCallback(final String snapshotName) {
        return new OnSuccessListener<SnapshotsClient.DataOrConflict<Snapshot>>() {
            @Override
            public void onSuccess(SnapshotsClient.DataOrConflict<Snapshot> i) {
                if (i.isConflict()) {
                    try {
                        String conflictId = i.getConflict().getConflictId();
                        Map<String, Object> errorMap = new HashMap<>();
                        errorMap.put("type", "SNAPSHOT_CONFLICT");
                        errorMap.put("message", "There was a conflict while opening snapshot, call resolveConflict to solve it.");
                        errorMap.put("conflictId", conflictId);
                        Snapshot snapshot = i.getConflict().getSnapshot();
                        pluginRef.getLoadedSnapshot().put(snapshotName, snapshot);
                        errorMap.put("local", snapshotToMap(i.getConflict().getConflictingSnapshot()));
                        errorMap.put("server", snapshotToMap(snapshot));
                        result(errorMap);
                    } catch (IOException e) {
                        error("SNAPSHOT_CONTENT_READ_ERROR", e);
                    }
                } else {
                    pluginRef.getLoadedSnapshot().put(snapshotName, i.getData());
                    try {
                        result(snapshotToMap(i.getData()));
                    } catch (IOException e) {
                        error("SNAPSHOT_CONTENT_READ_ERROR", e);
                    }
                }
            }
        };
    }

    public void openSnapshot(String snapshotName) {
        SnapshotsClient snapshotsClient = Games.getSnapshotsClient(this.registrar.activity(), currentAccount);
        snapshotsClient.open(snapshotName, true).addOnSuccessListener(generateCallback(snapshotName)).addOnFailureListener(new OnFailureListener() {
            @Override
            public void onFailure(@NonNull Exception e) {
                System.out.println("----- failure " + e);
                error("SNAPSHOT_FAILURE", e);
            }
        });
    }

    private boolean hasOnlyAllowedKeys(Map<String, ?> map, String... keys) {
        Set<String> _keys = new HashSet<>(Arrays.asList(keys));
        for (String key : map.keySet()) {
            if (!_keys.contains(key)) {
                error("INVALID_METADATA_SET", "You can not set the metadata " + key + "; only " + _keys + " are allowed.");
                return false;
            }
        }
        return true;
    }

    public void saveSnapshot(String snapshotName, String content, Map<String, String> metadata) {
        if (!hasOnlyAllowedKeys(metadata, "description")) {
            return;
        }
        SnapshotsClient snapshotsClient = Games.getSnapshotsClient(this.registrar.activity(), currentAccount);
        Snapshot snapshot = pluginRef.getLoadedSnapshot().get(snapshotName);
        if (snapshot == null) {
            error("SNAPSHOT_NOT_OPENED", "The snapshot with name " + snapshotName + " was not opened before.");
            return;
        }
        snapshot.getSnapshotContents().writeBytes(content.getBytes());
        SnapshotMetadataChange metadataChange = new SnapshotMetadataChange.Builder()
                .setDescription(metadata.get("description"))
                .build();
        snapshotsClient.commitAndClose(snapshot, metadataChange).addOnSuccessListener(new OnSuccessListener<SnapshotMetadata>() {
            @Override
            public void onSuccess(SnapshotMetadata snapshotMetadata) {
                Map<String, Object> result = new HashMap<>();
                result.put("status", true);
                result(result);
            }
        }).addOnFailureListener(new OnFailureListener() {
            @Override
            public void onFailure(@NonNull Exception e) {
                error("SAVE_SNAPSHOT_ERROR", e);
            }
        });
    }

    public void resolveSnapshotConflict(String snapshotName, String conflictId, String content, Map<String, String> metadata) {
        if (!hasOnlyAllowedKeys(metadata, "description")) {
            return;
        }
        SnapshotsClient snapshotsClient = Games.getSnapshotsClient(this.registrar.activity(), currentAccount);
        Snapshot snapshot = pluginRef.getLoadedSnapshot().get(snapshotName);
        if (snapshot == null) {
            error("SNAPSHOT_NOT_OPENED", "The snapshot with name " + snapshotName + " was not opened before.");
            return;
        }
        snapshot.getSnapshotContents().writeBytes(content.getBytes());
        SnapshotMetadataChange metadataChange = new SnapshotMetadataChange.Builder()
                .setDescription(metadata.get("description"))
                .build();

        snapshotsClient.resolveConflict(conflictId, snapshot.getMetadata().getSnapshotId(), metadataChange, snapshot.getSnapshotContents()).addOnSuccessListener(generateCallback(snapshotName)).addOnFailureListener(new OnFailureListener() {
            @Override
            public void onFailure(@NonNull Exception e) {
                error("RESOLVE_SNAPSHOT_CONFLICT_ERROR", e);
            }
        });
    }

    private Map<String, Object> snapshotToMap(Snapshot snapshot) throws IOException {
        String blob = new String(snapshot.getSnapshotContents().readFully());
        Map<String, Object> metadata = new HashMap<>();
        metadata.put("title", snapshot.getMetadata().getTitle());
        metadata.put("description", snapshot.getMetadata().getDescription());
        metadata.put("deviceName", snapshot.getMetadata().getDeviceName());
        metadata.put("snapshotId", snapshot.getMetadata().getSnapshotId());
        metadata.put("uniqueName", snapshot.getMetadata().getUniqueName());
        metadata.put("coverImageAspectRatio", snapshot.getMetadata().getCoverImageAspectRatio());
        metadata.put("coverImageUri", snapshot.getMetadata().getCoverImageUri() == null ? null : snapshot.getMetadata().getCoverImageUri().toString());
        metadata.put("lastModifiedTimestamp", snapshot.getMetadata().getLastModifiedTimestamp());

        Map<String, Object> data = new HashMap<>();
        data.put("content", blob);
        data.put("metadata", metadata);

        return data;
    }

    public void submitScore(String leaderboardId, Long score) {
        LeaderboardsClient leaderboardsClient = Games.getLeaderboardsClient(this.registrar.activity(), currentAccount);
        leaderboardsClient.submitScoreImmediate(leaderboardId, score).addOnSuccessListener(new OnSuccessListener<ScoreSubmissionData>() {
            @Override
            public void onSuccess(ScoreSubmissionData data) {
                Map<String, Object> successMap = new HashMap<>();
                successMap.put("type", "SUCCESS");
                successMap.put("leaderboardId", data.getLeaderboardId());
                successMap.put("playerId", data.getPlayerId());
                successMap.put("scoreResultDaily", resultToMap(data.getScoreResult(LeaderboardVariant.TIME_SPAN_DAILY)));
                successMap.put("scoreResultWeekly", resultToMap(data.getScoreResult(LeaderboardVariant.TIME_SPAN_WEEKLY)));
                successMap.put("scoreResultAllTime", resultToMap(data.getScoreResult(LeaderboardVariant.TIME_SPAN_ALL_TIME)));
                result(successMap);
            }
        }).addOnFailureListener(new OnFailureListener() {
            @Override
            public void onFailure(@NonNull Exception e) {
                System.out.println("----- leaderboard submit failure " + e);
                error("LEADERBOARD_FAILURE", e);
            }
        });
    }

    public void loadPlayerCenteredScores(String leaderboardId, String timeSpan, String collectionType, int maxResults, boolean forceReload) {
        LeaderboardsClient leaderboardsClient = Games.getLeaderboardsClient(this.registrar.activity(), currentAccount);
        leaderboardsClient.loadPlayerCenteredScores(leaderboardId, convertTimeSpan(timeSpan), convertCollection(collectionType), maxResults, forceReload)
            .addOnSuccessListener(scoreSuccessHandler())
            .addOnFailureListener(new OnFailureListener() {
                @Override
                public void onFailure(@NonNull Exception e) {
                    System.out.println("----- leaderboard player centered retrieve failure " + e);
                    error("LEADERBOARD_PLAYER_CENTERED_FAILURE", e);
                }
            }
        );
    }

    public void loadTopScores(String leaderboardId, String timeSpan, String collectionType, int maxResults, boolean forceReload) {
        LeaderboardsClient leaderboardsClient = Games.getLeaderboardsClient(this.registrar.activity(), currentAccount);
        leaderboardsClient.loadTopScores(leaderboardId, convertTimeSpan(timeSpan), convertCollection(collectionType), maxResults, forceReload)
            .addOnSuccessListener(scoreSuccessHandler())
            .addOnFailureListener(new OnFailureListener() {
                @Override
                public void onFailure(@NonNull Exception e) {
                    System.out.println("----- leaderboard top scores retrieve failure " + e);
                    error("LEADERBOARD_TOP_SCORES_FAILURE", e);
                }
            }
        );
    }

    private OnSuccessListener<AnnotatedData<LeaderboardsClient.LeaderboardScores>> scoreSuccessHandler() {
        return new OnSuccessListener<AnnotatedData<LeaderboardsClient.LeaderboardScores>>() {
            @Override
            public void onSuccess(AnnotatedData<LeaderboardsClient.LeaderboardScores> data) {
                LeaderboardsClient.LeaderboardScores scores = data.get();
                Map<String, Object> successMap = new HashMap<>();
                successMap.put("type", "SUCCESS");
                successMap.put("leaderboardDisplayName", scores.getLeaderboard().getDisplayName());
                List<Map<String, Object>> list = new ArrayList<>();
                Iterator<LeaderboardScore> it = scores.getScores().iterator();
                while (it.hasNext()) {
                    list.add(scoreToMap(it.next()));
                }
                successMap.put("scores", list);

                scores.release();
                result(successMap);
            }
        };
    }

    private int convertTimeSpan(String timeSpan) {
        switch (timeSpan) {
            case "TimeSpan.TIME_SPAN_DAILY":
                return LeaderboardVariant.TIME_SPAN_DAILY;
            case "TimeSpan.TIME_SPAN_WEEKLY":
                return LeaderboardVariant.TIME_SPAN_WEEKLY;
            case "TimeSpan.TIME_SPAN_ALL_TIME":
                return LeaderboardVariant.TIME_SPAN_ALL_TIME;
        }
        throw new RuntimeException("Unknown time span...");
    }

    private int convertCollection(String collection) {
        switch (collection) {
            case "CollectionType.COLLECTION_PUBLIC":
                return LeaderboardVariant.COLLECTION_PUBLIC;
            case "CollectionType.COLLECTION_SOCIAL":
                return LeaderboardVariant.COLLECTION_SOCIAL;
        }
        throw new RuntimeException("Unknown time span...");
    }

    private Map<String, Object> resultToMap(ScoreSubmissionData.Result result) {
        Map<String, Object> data = new HashMap<>();
        data.put("formattedScore", result.formattedScore);
        data.put("newBest", result.newBest);
        data.put("rawScore", result.rawScore);
        data.put("scoreTag", result.scoreTag);
        return data;
    }

    private Map<String, Object> scoreToMap(LeaderboardScore score) {
        Map<String, Object> data = new HashMap<>();

        data.put("displayRank", score.getDisplayRank());
        data.put("displayScore", score.getDisplayScore());
        data.put("rank", score.getRank());
        data.put("rawScore", score.getRawScore());
        data.put("scoreTag", score.getScoreTag());
        data.put("timestampMillis", score.getTimestampMillis());
        data.put("scoreHolderDisplayName", score.getScoreHolderDisplayName());

        return data;
    }
}