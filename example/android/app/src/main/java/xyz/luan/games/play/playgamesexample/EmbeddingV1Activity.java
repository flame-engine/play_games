package xyz.luan.games.play.playgamesexample;

import android.os.Bundle;
import io.flutter.app.FlutterActivity;
import xyz.luan.games.play.playgames.PlayGamesPlugin;

public class EmbeddingV1Activity extends FlutterActivity {
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        PlayGamesPlugin.registerWith(registrarFor("xyz.luan.games.play.playgames.PlayGamesPlugin"));
    }
}
