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
import com.google.android.gms.tasks.OnFailureListener;
import com.google.android.gms.tasks.OnSuccessListener;
import com.google.android.gms.tasks.SuccessContinuation;
import com.google.android.gms.tasks.Task;
import com.google.android.gms.games.snapshot.Snapshot;

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

    private void startTransaction(MethodCall call, Result result) {
        if (pendingOperation != null) {
            throw new IllegalStateException("signIn/showAchievements/saved games/snapshots cannot be used concurrently!");
        }
        pendingOperation = new PendingOperation(call, result);
    }

    @Override
    public void onMethodCall(MethodCall call, Result result) {
        if (call.method.equals("signIn")) {
            startTransaction(call, result);
            boolean requestEmail = getPropOrDefault(call, "requestEmail", true);
            boolean scopeSnapshot = getPropOrDefault(call, "scopeSnapshot", false);
            try {
                signIn(requestEmail, scopeSnapshot);
            } catch (Exception ex) {
                pendingOperation = null;
                throw ex;
            }
        } else if (call.method.equals("showAchievements")) {
            startTransaction(call, result);
            try {
                showAchievements();
            } catch (Exception ex) {
                pendingOperation = null;
                throw ex;
            }
        } else {
            new Request(this, currentAccount, registrar, call, result).handle();
        }
    }

    private void signIn(boolean requestEmail, boolean scopeSnapshot) {
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

    public Map<String, Snapshot> getLoadedSnapshot() {
        return this.loadedSnapshots;
    }
}
