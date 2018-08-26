package xyz.luan.games.play.playgames;

import java.util.Map;
import java.util.HashMap;

import io.flutter.plugin.common.MethodCall;
import io.flutter.plugin.common.MethodChannel;
import io.flutter.plugin.common.MethodChannel.MethodCallHandler;
import io.flutter.plugin.common.MethodChannel.Result;
import io.flutter.plugin.common.PluginRegistry.Registrar;
import io.flutter.plugin.common.PluginRegistry.ActivityResultListener;

import android.content.Intent;
import com.google.android.gms.auth.api.Auth;
import com.google.android.gms.auth.api.signin.GoogleSignIn;
import com.google.android.gms.auth.api.signin.GoogleSignInAccount;
import com.google.android.gms.auth.api.signin.GoogleSignInClient;
import com.google.android.gms.auth.api.signin.GoogleSignInOptions;
import com.google.android.gms.auth.api.signin.GoogleSignInResult;

public class PlayGamesPlugin implements MethodCallHandler, ActivityResultListener {

  private static final int RC_SIGN_IN = 1;

  private Registrar registrar;
  private PendingOperation pendingOperation;

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
    } else {
      pendingOperation = null;
      result.notImplemented();
    }
  }

  private void result(Map<String, String> response) {
    pendingOperation.result.success(response);
    pendingOperation = null;
  }

  @Override
  public boolean onActivityResult(int requestCode, int resultCode, Intent data) {
    if (pendingOperation == null || requestCode != RC_SIGN_IN) {
      return false;
    }
    GoogleSignInResult result = Auth.GoogleSignInApi.getSignInResultFromIntent(data);
    if (result.isSuccess()) {
      GoogleSignInAccount signedInAccount = result.getSignInAccount();
      Map<String, String> successMap = new HashMap<>();
      successMap.put("type", "SUCCESS");
      successMap.put("id", signedInAccount.getId());
      successMap.put("email", signedInAccount.getEmail());
      successMap.put("displayName", signedInAccount.getDisplayName());
      successMap.put("avatar", signedInAccount.getPhotoUrl() == null ? null : signedInAccount.getPhotoUrl().toString());
      result(successMap);
    } else {
      Map<String, String> errorMap = new HashMap<>();
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
