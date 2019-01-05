package xyz.luan.games.play.playgames;

import android.content.Intent;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.util.Log;

import com.google.android.gms.auth.api.Auth;
import com.google.android.gms.auth.api.signin.GoogleSignIn;
import com.google.android.gms.auth.api.signin.GoogleSignInAccount;
import com.google.android.gms.auth.api.signin.GoogleSignInClient;
import com.google.android.gms.auth.api.signin.GoogleSignInOptions;
import com.google.android.gms.auth.api.signin.GoogleSignInResult;
import com.google.android.gms.drive.Drive;
import com.google.android.gms.games.Games;
import com.google.android.gms.games.Player;
import com.google.android.gms.games.PlayersClient;
import com.google.android.gms.games.SnapshotsClient;
import com.google.android.gms.games.snapshot.Snapshot;
import com.google.android.gms.games.snapshot.SnapshotMetadata;
import com.google.android.gms.games.snapshot.SnapshotMetadataChange;
import com.google.android.gms.tasks.OnFailureListener;
import com.google.android.gms.tasks.OnSuccessListener;
import com.google.android.gms.tasks.SuccessContinuation;
import com.google.android.gms.tasks.Task;

import java.io.IOException;
import java.util.Arrays;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

import io.flutter.plugin.common.MethodCall;
import io.flutter.plugin.common.MethodChannel;
import io.flutter.plugin.common.MethodChannel.MethodCallHandler;
import io.flutter.plugin.common.MethodChannel.Result;
import io.flutter.plugin.common.PluginRegistry.ActivityResultListener;
import io.flutter.plugin.common.PluginRegistry.Registrar;

public class PlayGamesPlugin implements MethodCallHandler, ActivityResultListener {

    private static final String TAG = PlayGamesPlugin.class.getCanonicalName();
    private static final int RC_SIGN_IN = 9001;
    private static final int RC_ACHIEVEMENT_UI = 9002;

    private Registrar registrar;
    private PendingOperation pendingOperation;
    private GoogleSignInAccount currentAccount;
    private Map<String, Snapshot> loadedSnapshots = new HashMap<>();

    public static void registerWith(Registrar registrar) {
        final MethodChannel channel = new MethodChannel(registrar.messenger(), "play_games");
        channel.setMethodCallHandler(new PlayGamesPlugin(registrar));
    }

    public PlayGamesPlugin(Registrar registrar) {
        this.registrar = registrar;
        this.registrar.addActivityResultListener(this);
    }

    private static class PendingOperation {
        MethodCall call;
        Result result;

        PendingOperation(MethodCall call, Result result) {
            this.call = call;
            this.result = result;
        }
    }

    private boolean getPropOrDefault(MethodCall call, String prop, boolean defaultValue) {
        Object value = call.argument(prop);
        if (value == null || value.toString().isEmpty()) {
            return defaultValue;
        }
        return value.toString().equalsIgnoreCase("true");
    }

    @Override
    public void onMethodCall(MethodCall call, Result result) {
        if (call.method.equals("signIn")) {
            if (pendingOperation != null) {
                throw new IllegalStateException("signIn/showAchievements cannot be used concurrently!");
            }
            pendingOperation = new PendingOperation(call, result);
            boolean requestEmail = getPropOrDefault(call, "requestEmail", true);
            boolean scopeSnapshot = getPropOrDefault(call, "scopeSnapshot", false);
            GoogleSignInOptions.Builder builder = new GoogleSignInOptions.Builder(GoogleSignInOptions.DEFAULT_GAMES_SIGN_IN);
            if (requestEmail) {
                builder.requestEmail();
            }
            if (scopeSnapshot) {
                builder.requestScopes(Drive.SCOPE_APPFOLDER);
            }
            GoogleSignInOptions opts = builder.build();
            GoogleSignInClient signInClient = GoogleSignIn.getClient(registrar.activity(), opts);
            silentSignIn(signInClient);
        } else if (call.method.equals("showAchievements")) {
            if (pendingOperation != null) {
                throw new IllegalStateException("signIn/showAchievements cannot be used concurrently!");
            }
            pendingOperation = new PendingOperation(call, result);
            showAchievements();
        } else if (call.method.equals("openSnapshot")) {
            if (pendingOperation != null) {
                throw new IllegalStateException("signIn/showAchievements cannot be used concurrently!");
            }
            pendingOperation = new PendingOperation(call, result);
            String snapshotName = call.argument("snapshotName");
            openSnapshot(snapshotName);
        } else if (call.method.equals("saveSnapshot")) {
            if (pendingOperation != null) {
                throw new IllegalStateException("signIn/showAchievements cannot be used concurrently!");
            }
            pendingOperation = new PendingOperation(call, result);
            String snapshotName = call.argument("snapshotName");
            String content = call.argument("content");
            Map<String, String> metadata = call.argument("metadata");
            saveSnapshot(snapshotName, content, metadata);
        } else if (call.method.equals("resolveSnapshotConflict")) {
            if (pendingOperation != null) {
                throw new IllegalStateException("signIn/showAchievements cannot be used concurrently!");
            }
            pendingOperation = new PendingOperation(call, result);
            String snapshotName = call.argument("snapshotName");
            String conflictId = call.argument("conflictId");
            String content = call.argument("content");
            Map<String, String> metadata = call.argument("metadata");
            resolveSnapshotConflict(snapshotName, conflictId, content, metadata);
        } else {
            new Request(currentAccount, registrar, call, result).handle();
        }
    }

    private void explicitSignIn(GoogleSignInClient signInClient) {
        Intent intent = signInClient.getSignInIntent();
        registrar.activity().startActivityForResult(intent, RC_SIGN_IN);
    }

    private void silentSignIn(final GoogleSignInClient signInClient) {
        signInClient.silentSignIn().addOnSuccessListener(new OnSuccessListener<GoogleSignInAccount>() {
            @Override
            public void onSuccess(GoogleSignInAccount googleSignInAccount) {
                handleSuccess(googleSignInAccount);
            }
        }).addOnFailureListener(new OnFailureListener() {
            @Override
            public void onFailure(@NonNull Exception e) {
                Log.i(TAG, "Failed to silent signin, trying explicit signin", e);
                explicitSignIn(signInClient);
            }
        });
    }

    private void handleSuccess(GoogleSignInAccount acc) {
        currentAccount = acc;
        PlayersClient playersClient = Games.getPlayersClient(registrar.activity(), currentAccount);
        playersClient.getCurrentPlayer().addOnSuccessListener(new OnSuccessListener<Player>() {
            @Override
            public void onSuccess(Player player) {
                Map<String, Object> successMap = new HashMap<>();
                successMap.put("type", "SUCCESS");
                successMap.put("id", player.getPlayerId());
                successMap.put("email", currentAccount.getEmail());
                successMap.put("displayName", player.getDisplayName());
                successMap.put("hiResImageUri", player.getHiResImageUri().toString());
                successMap.put("iconImageUri", player.getIconImageUri().toString());
                result(successMap);
            }
        }).addOnFailureListener(new OnFailureListener() {
            @Override
            public void onFailure(Exception e) {
                error("ERROR_FETCH_PLAYER_PROFILE", e);
            }
        });
    }

    private void result(Object response) {
        pendingOperation.result.success(response);
        pendingOperation = null;
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

    @Override
    public boolean onActivityResult(int requestCode, int resultCode, Intent data) {
        if (pendingOperation == null) {
            return false;
        }
        if (requestCode == RC_ACHIEVEMENT_UI) {
            Map<String, Object> result = new HashMap<>();
            result.put("closed", true);
            result(result);
            return true;
        } else if (requestCode == RC_SIGN_IN) {
            GoogleSignInResult result = Auth.GoogleSignInApi.getSignInResultFromIntent(data);
            if (result.isSuccess()) {
                handleSuccess(result.getSignInAccount());
            } else {
                String message = result.getStatus().getStatusMessage();
                if (message == null || message.isEmpty()) {
                    message = "Unexpected error " + result.getStatus();
                }
                error("ERROR_SIGNIN", message);
            }
            return true;
        }
        return false;
    }

    public void showAchievements() {
        Games.getAchievementsClient(registrar.activity(), currentAccount).getAchievementsIntent()
                .addOnSuccessListener(new OnSuccessListener<Intent>() {
                    @Override
                    public void onSuccess(Intent intent) {
                        registrar.activity().startActivityForResult(intent, RC_ACHIEVEMENT_UI);
                    }
                }).addOnFailureListener(new OnFailureListener() {
            @Override
            public void onFailure(@NonNull Exception e) {
                error("ERROR_SHOW_ACHIEVEMENTS", e);
            }
        });
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
                        loadedSnapshots.put(snapshotName, snapshot);
                        errorMap.put("local", toMap(i.getConflict().getConflictingSnapshot()));
                        errorMap.put("server", toMap(snapshot));
                        result(errorMap);
                    } catch (IOException e) {
                        error("SNAPSHOT_CONTENT_READ_ERROR", e);
                    }
                } else {
                    loadedSnapshots.put(snapshotName, i.getData());
                    try {
                        result(toMap(i.getData()));
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
        Snapshot snapshot = loadedSnapshots.get(snapshotName);
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
        Snapshot snapshot = loadedSnapshots.get(snapshotName);
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

    private Map<String, Object> toMap(Snapshot snapshot) throws IOException {
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
}
