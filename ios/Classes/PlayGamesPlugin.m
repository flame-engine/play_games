#import "PlayGamesPlugin.h"
#import <play_games/play_games-Swift.h>

@implementation PlayGamesPlugin
+ (void)registerWithRegistrar:(NSObject<FlutterPluginRegistrar>*)registrar {
  [SwiftPlayGamesPlugin registerWithRegistrar:registrar];
}
@end
