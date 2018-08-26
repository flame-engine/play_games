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

import android.content.Intent;
import android.net.Uri;
import android.graphics.Bitmap;
import android.graphics.drawable.BitmapDrawable;
import android.graphics.drawable.Drawable;

import com.google.android.gms.common.images.ImageManager;

import com.google.android.gms.auth.api.Auth;
import com.google.android.gms.auth.api.signin.GoogleSignIn;
import com.google.android.gms.auth.api.signin.GoogleSignInAccount;
import com.google.android.gms.auth.api.signin.GoogleSignInClient;
import com.google.android.gms.auth.api.signin.GoogleSignInOptions;
import com.google.android.gms.auth.api.signin.GoogleSignInResult;

import com.google.android.gms.games.Games;
import com.google.android.gms.games.Player;
import com.google.android.gms.games.PlayersClient;
import com.google.android.gms.tasks.OnFailureListener;
import com.google.android.gms.tasks.OnSuccessListener;

public class PlayGamesPlugin implements MethodCallHandler, ActivityResultListener {

  private static final int RC_SIGN_IN = 1;

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
      Intent intent = signInClient.getSignInIntent();
      registrar.activity().startActivityForResult(intent, RC_SIGN_IN);
    } else if (call.method.equals("getHiResImage")) {
      getHiResImage();
    } else if (call.method.equals("getIconImage")) {
      getIconImage();
    } else {
      pendingOperation = null;
      result.notImplemented();
    }
  }

  private void result(Map<String, Object> response) {
    pendingOperation.result.success(response);
    pendingOperation = null;
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
    if (pendingOperation == null || requestCode != RC_SIGN_IN) {
      return false;
    }
    GoogleSignInResult result = Auth.GoogleSignInApi.getSignInResultFromIntent(data);
    if (result.isSuccess()) {
      currentAccount = result.getSignInAccount();
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
          Map<String, Object> errorMap = new HashMap<>();
          errorMap.put("type", "ERROR_2");
          // TODO log e
          errorMap.put("message", e.getMessage());
          result(errorMap);
        }
      });
    } else {
      Map<String, Object> errorMap = new HashMap<>();
      errorMap.put("type", "ERROR");
      String message = result.getStatus().getStatusMessage();
      if (message == null || message.isEmpty()) {
          message = "Unexpected error " + result.getStatus();
      }
      errorMap.put("message", message);
      result(errorMap);
    }
    return true;
  }
}
