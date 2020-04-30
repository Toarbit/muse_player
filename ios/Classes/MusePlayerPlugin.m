#import "MusePlayerPlugin.h"
#if __has_include(<muse_player/muse_player-Swift.h>)
#import <muse_player/muse_player-Swift.h>
#else
// Support project import fallback if the generated compatibility header
// is not copied when this plugin is created as a library.
// https://forums.swift.org/t/swift-static-libraries-dont-copy-generated-objective-c-header/19816
#import "muse_player-Swift.h"
#endif

@implementation MusePlayerPlugin
+ (void)registerWithRegistrar:(NSObject<FlutterPluginRegistrar>*)registrar {
  [SwiftMusePlayerPlugin registerWithRegistrar:registrar];
}
@end
