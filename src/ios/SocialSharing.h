#import <Cordova/CDV.h>
#import <MessageUI/MFMailComposeViewController.h>
#import "WXApiManager.h"
#import "WXApiRequestHandler.h"
#import <TencentOpenAPI/QQApiInterface.h>

@interface SocialSharing : CDVPlugin <UIPopoverControllerDelegate, MFMailComposeViewControllerDelegate, UIDocumentInteractionControllerDelegate,WXApiManagerDelegate>

@property (nonatomic, strong) MFMailComposeViewController *globalMailComposer;
@property (retain) UIDocumentInteractionController * documentInteractionController;
@property (retain) NSString * tempStoredFile;
@property (retain) CDVInvokedUrlCommand * command;

- (void)available:(CDVInvokedUrlCommand*)command;
- (void)setIPadPopupCoordinates:(CDVInvokedUrlCommand*)command;
- (void)share:(CDVInvokedUrlCommand*)command;
- (void)canShareVia:(CDVInvokedUrlCommand*)command;
- (void)canShareViaEmail:(CDVInvokedUrlCommand*)command;
- (void)shareVia:(CDVInvokedUrlCommand*)command;
- (void)shareViaTwitter:(CDVInvokedUrlCommand*)command;
- (void)shareViaFacebook:(CDVInvokedUrlCommand*)command;
- (void)shareViaFacebookWithPasteMessageHint:(CDVInvokedUrlCommand*)command;
- (void)shareViaWhatsApp:(CDVInvokedUrlCommand*)command;
- (void)shareViaSMS:(CDVInvokedUrlCommand*)command;
- (void)shareViaEmail:(CDVInvokedUrlCommand*)command;
- (void)shareViaInstagram:(CDVInvokedUrlCommand*)command;
- (void)shareViaQq:(CDVInvokedUrlCommand*)command;
- (void)shareViaWechat:(CDVInvokedUrlCommand*)command;

- (void)saveToPhotoAlbum:(CDVInvokedUrlCommand*)command;

@end
