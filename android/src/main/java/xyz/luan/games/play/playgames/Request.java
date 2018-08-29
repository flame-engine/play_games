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

import com.google.android.gms.auth.api.Auth;
import com.google.android.gms.auth.api.signin.GoogleSignIn;
import com.google.android.gms.auth.api.signin.GoogleSignInAccount;
import com.google.android.gms.auth.api.signin.GoogleSignInClient;
import com.google.android.gms.auth.api.signin.GoogleSignInOptions;
import com.google.android.gms.auth.api.signin.GoogleSignInResult;

import com.google.android.gms.games.Games;
import com.google.android.gms.games.GamesClient;
import com.google.android.gms.games.Player;
import com.google.android.gms.games.PlayersClient;
import com.google.android.gms.tasks.OnFailureListener;
import com.google.android.gms.tasks.OnSuccessListener;

public class Request {

    private static final String TAG = Request.class.getCanonicalName();

    private GoogleSignInAccount currentAccount;
    private Registrar registrar;
    private MethodCall call;
    private Result result;

    public Request(GoogleSignInAccount currentAccount, Registrar registrar, MethodCall call, Result result) {
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
}