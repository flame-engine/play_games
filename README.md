# play_games

Use Google Play Games Services on your Flutter app; signin, achievements, etc!

**WIP**: This is currently a Work in Progress. Not all features are implemented, and things might change a lot.

## Part 1: Sign In

Call the sign in method to sign in; you must check if the login was successufully.

```dart
    SigninResult result = await PlayGames.signIn();
    if (result.success) {
      await PlayGames.setPopupOptions();
      this.account = result.account;
    } else {
      this.error = result.message;
    }
    this.loading = false;
```

But don't think it will be that easy! Actually, GPGS signin is quite a hassle to do, but the following tutorial should help you walk through this process.

The basic steps (need to elaborate)
 * Create the GPGS Project
 * Create the linked android app with proper package name and SHA-1 inside GPGS
 * Create also a webapp and get the id from that (NOT your android linked app)
 * Create at least one achievement to get your games-ids.xml file and place it accordingly
 * Remember to add yourself as tester if not published