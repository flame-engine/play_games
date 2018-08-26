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

public class PlayGamesPlugin implements MethodCallHandler, ActivityResultListener {

  private static final String TAG = PlayGamesPlugin.class.getCanonicalName();
  private static final int RC_SIGN_IN = 9001;
  private static final int RC_ACHIEVEMENT_UI = 9002;

  private Registrar registrar;
  private PendingOperation pendingOperation;
  private GoogleSignInAccount currentAccount;

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

  @Override
  public void onMethodCall(MethodCall call, Result result) {
    if (pendingOperation != null) {
      throw new IllegalStateException("Cannot be used concurrently!");
    }
    pendingOperation = new PendingOperation(call, result);
    if (call.method.equals("signIn")) {
      GoogleSignInOptions opts = new GoogleSignInOptions.Builder(GoogleSignInOptions.DEFAULT_GAMES_SIGN_IN).requestEmail().build();
      GoogleSignInClient signInClient = GoogleSignIn.getClient(registrar.activity(), opts);
      silentSignIn(signInClient);
    } else if (call.method.equals("getHiResImage")) {
      getHiResImage();
    } else if (call.method.equals("getIconImage")) {
      getIconImage();
    } else if (call.method.equals("showAchievements")) {
      showAchievements();
    } else if (call.method.equals("unlockAchievementById")) {
      String id = call.argument("id");
      unlockAchievement(id);
    } else if (call.method.equals("unlockAchievementByName")) {
      String name = call.argument("name");
      unlockAchievement(getIdByName(name));
    } else if (call.method.equals("incrementAchievementById")) {
      String id = call.argument("id");
      int amount = call.argument("amount");
      incrementAchievement(id, amount);
    } else if (call.method.equals("incrementAchievementByName")) {
      String name = call.argument("name");
      int amount = call.argument("amount");
      incrementAchievement(getIdByName(name), amount);
    } else if (call.method.equals("setPopupOptions")) {
      boolean show = call.argument("show");
      int gravity = call.argument("gravity");
      setPopupOptions(show, gravity);
    } else {
      pendingOperation = null;
      result.notImplemented();
    }
  }

  private String getIdByName(String name) {
    Context ctx = registrar.conte`xt();
    int resId = ctx.getResources().getIdentifier(name, "string", ctx.getPackageName());
    return ctx.getString(resId);
  }

  private void unlockAchievement(String id) {
    Games.getAchievementsClient(registrar.activity(), currentAccount).unlockImmediate(id).addOnSuccessListener(new OnSuccessListener<Void>() {
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

  private void incrementAchievement(String id, int amount) {
    Games.getAchievementsClient(registrar.activity(), currentAccount).incrementImmediate(id, amount).addOnSuccessListener(new OnSuccessListener<Boolean>() {
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

  private void showAchievements() {
    Games.getAchievementsClient(registrar.activity(), currentAccount)
      .getAchievementsIntent()
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

  private void setPopupOptions(boolean show, int gravity) {
    GamesClient gamesClient = Games.getGamesClient(registrar.activity(), currentAccount);
    if (show) {
      gamesClient.setViewForPopups(registrar.view());
      gamesClient.setGravityForPopups(gravity);
    } else {
      gamesClient.setViewForPopups(null);
    }
    result(true);
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

  private void getHiResImage() {
    PlayersClient playersClient = Games.getPlayersClient(registrar.activity(), currentAccount);
    playersClient.getCurrentPlayer().addOnSuccessListener(new OnSuccessListener<Player>() {
      @Override
      public void onSuccess(Player player) {
        readImage(player.getHiResImageUri());
      }
    });
  }

  private void getIconImage() {
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
}
