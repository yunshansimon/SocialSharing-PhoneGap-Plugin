#import <Cordova/CDV.h>
#import <MessageUI/MFMailComposeViewController.h>
#import "WXApiManager.h"
#import "WXApiRequestHandler.h"
#import <TencentOpenAPI/QQApiInterface.h>
#import <TencentOpenAPI/TencentOAuth.h>
#import <TencentOpenAPI/TencentApiInterface.h>
#import "WeiboSDK.h"

@interface SocialSharing : CDVPlugin <UIPopoverControllerDelegate, MFMailComposeViewControllerDelegate, UIDocumentInteractionControllerDelegate,WXApiManagerDelegate,QQApiInterfaceDelegate,TencentSessionDelegate,WeiboSDKDelegate>

@property (nonatomic, strong) MFMailComposeViewController *globalMailComposer;
@property (retain) UIDocumentInteractionController * documentInteractionController;
@property (retain) NSString * tempStoredFile;
@property (retain) CDVInvokedUrlCommand * command;

- (void)handleOpenURL:(NSNotification *)notification;
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
- (void)shareViaWeiBo:(CDVInvokedUrlCommand*)command;

- (void)saveToPhotoAlbum:(CDVInvokedUrlCommand*)command;
- (void)onReq:(QQBaseReq *)req;

/**
 处理来至QQ的响应
 */
- (void)onResp:(QQBaseResp *)resp;

/**
 处理QQ在线状态的回调
 */
- (void)isOnlineResponse:(NSDictionary *)response;
- (void)tencentDidLogout;


/**
 * 登录成功后的回调
 */
- (void)tencentDidLogin;

/**
 * 登录失败后的回调
 * \param cancelled 代表用户是否主动退出登录
 */
- (void)tencentDidNotLogin:(BOOL)cancelled;

/**
 * 登录时网络有问题的回调
 */
- (void)tencentDidNotNetWork;




@end
