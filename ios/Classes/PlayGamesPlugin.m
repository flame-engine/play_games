#import "PlayGamesPlugin.h"
#if __has_include(<play_games/play_games-Swift.h>)
#import <play_games/play_games-Swift.h>
#else
// Support project import fallback if the generated compatibility header
// is not copied when this plugin is created as a library.
// https://forums.swift.org/t/swift-static-libraries-dont-copy-generated-objective-c-header/19816
#import "play_games-Swift.h"
#endif

@implementation PlayGamesPlugin
+ (void)registerWithRegistrar:(NSObject<FlutterPluginRegistrar>*)registrar {
  [SwiftPlayGamesPlugin registerWithRegistrar:registrar];
}
@end
