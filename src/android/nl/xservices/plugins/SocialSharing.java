package nl.xservices.plugins;

import android.annotation.SuppressLint;
import android.app.Activity;
import android.content.*;
import android.content.pm.ActivityInfo;
import android.content.pm.PackageManager;
import android.content.pm.ResolveInfo;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.os.Environment;
import android.text.Html;
import android.text.TextUtils;
import android.util.Base64;
import android.util.Log;
import android.view.Gravity;
import android.widget.Toast;


import com.sina.weibo.sdk.api.ImageObject;
import com.sina.weibo.sdk.api.TextObject;
import com.sina.weibo.sdk.api.WebpageObject;
import com.sina.weibo.sdk.api.WeiboMessage;
import com.sina.weibo.sdk.api.WeiboMultiMessage;
import com.sina.weibo.sdk.api.share.BaseResponse;
import com.sina.weibo.sdk.api.share.IWeiboHandler;
import com.sina.weibo.sdk.api.share.IWeiboShareAPI;
import com.sina.weibo.sdk.api.share.SendMessageToWeiboRequest;
import com.sina.weibo.sdk.api.share.SendMessageToWeiboResponse;
import com.sina.weibo.sdk.api.share.WeiboShareSDK;
import com.sina.weibo.sdk.auth.AuthInfo;
import com.sina.weibo.sdk.auth.Oauth2AccessToken;
import com.sina.weibo.sdk.auth.WeiboAuthListener;
import com.sina.weibo.sdk.auth.sso.SsoHandler;
import com.sina.weibo.sdk.constant.WBConstants;
import com.sina.weibo.sdk.exception.WeiboException;
import com.sina.weibo.sdk.utils.LogUtil;
import com.tencent.connect.share.QQShare;
import com.tencent.mm.sdk.constants.ConstantsAPI;
import com.tencent.mm.sdk.modelbase.BaseReq;
import com.tencent.mm.sdk.modelbase.BaseResp;
import com.tencent.mm.sdk.modelmsg.SendMessageToWX;
import com.tencent.mm.sdk.modelmsg.WXImageObject;
import com.tencent.mm.sdk.modelmsg.WXMediaMessage;
import com.tencent.mm.sdk.modelmsg.WXTextObject;
import com.tencent.mm.sdk.modelmsg.WXWebpageObject;
import com.tencent.mm.sdk.openapi.IWXAPI;
import com.tencent.mm.sdk.openapi.WXAPIFactory;
import com.tencent.tauth.Tencent;
import com.tencent.tauth.IUiListener;
import com.tencent.tauth.UiError;

import org.apache.cordova.CallbackContext;
import org.apache.cordova.CordovaInterface;
import org.apache.cordova.CordovaPlugin;
import org.apache.cordova.CordovaWebView;
import org.apache.cordova.PluginResult;
import org.apache.cordova.file.Filesystem;
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.io.*;
import java.net.URL;
import java.net.URLConnection;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Timer;
import java.util.TimerTask;
import java.util.regex.Matcher;
import java.util.regex.Pattern;


public class SocialSharing extends CordovaPlugin implements IWeiboHandler.Response{

  private static final String ACTION_AVAILABLE_EVENT = "available";
  private static final String ACTION_SHARE_EVENT = "share";
  private static final String ACTION_CAN_SHARE_VIA = "canShareVia";
  private static final String ACTION_CAN_SHARE_VIA_EMAIL = "canShareViaEmail";
  private static final String ACTION_SHARE_VIA = "shareVia";
  private static final String ACTION_SHARE_VIA_TWITTER_EVENT = "shareViaTwitter";
  private static final String ACTION_SHARE_VIA_FACEBOOK_EVENT = "shareViaFacebook";
  private static final String ACTION_SHARE_VIA_FACEBOOK_WITH_PASTEMESSAGEHINT = "shareViaFacebookWithPasteMessageHint";
  private static final String ACTION_SHARE_VIA_WHATSAPP_EVENT = "shareViaWhatsApp";
  private static final String ACTION_SHARE_VIA_INSTAGRAM_EVENT = "shareViaInstagram";
  private static final String ACTION_SHARE_VIA_SMS_EVENT = "shareViaSMS";
  private static final String ACTION_SHARE_VIA_EMAIL_EVENT = "shareViaEmail";

  private static final String ACTION_SHARE_VIA_WECHAT_EVENT = "shareViaWechat";
  private static final String ACTION_SHARE_VIA_QQ_EVENT = "shareViaQq";
  private static final String ACTION_SHARE_VIA_WEI_BO="shareViaWeiBo";

  private static final String WXAPPID_PROPERTY_KEY = "wechatappid";
  private static final String QQAPPID_PROPERTY_KEY = "qqappid";
  private static final String WEIBOAPPID_PROPERTY_KEY ="weiboappid";
  private static final String WEIBO_URL_PROPERTY_KEY="WEIBORedirectURI";
  private static String QQ_APP_ID="";
  private static String WECHAT_APP_ID="";
  private static String WEIBO_APP_ID="";
  private static String WEIBO_URL="";
  private static String weibo_token="";

  private static final String WECHAT_SHARE_FAVORITE="favorite";
  private static final String WECHAT_SHARE_SESSION="session";
  private static final String WECHAT_SHARE_TIMELINE="timeline";
  private static final String WECHAT_ACTION_PREFIX="iwx_share_";
  private static final int THUMB_SIZE = 150;

  private static final int ACTIVITY_CODE_SEND = 1;
  private static final int ACTIVITY_CODE_SENDVIAEMAIL = 2;
  private static final int ACTIVITY_CODE_SENDVIAWHATSAPP = 3;

  private CallbackContext _callbackContext;

  private String pasteMessage;

  private Tencent mTencent;
  private IWeiboShareAPI weiboShareAPI;

  private abstract class SocialSharingRunnable implements Runnable {
    public CallbackContext callbackContext;
    SocialSharingRunnable(CallbackContext cb) {
      this.callbackContext = cb;
    }
  }
  @Override
  public void initialize(CordovaInterface cordova, CordovaWebView webView) {
    super.initialize(cordova, webView);
    QQ_APP_ID=preferences.getString(QQAPPID_PROPERTY_KEY,"");
    WECHAT_APP_ID=preferences.getString(WXAPPID_PROPERTY_KEY,"");
    WEIBO_APP_ID=preferences.getString(WEIBOAPPID_PROPERTY_KEY,"");
    WEIBO_URL=preferences.getString(WEIBO_URL_PROPERTY_KEY,"");
    if(!QQ_APP_ID.isEmpty()) { mTencent = Tencent.createInstance(QQ_APP_ID, cordova.getActivity().getApplicationContext());}
    if(!WECHAT_APP_ID.isEmpty()){
      weiboShareAPI=WeiboShareSDK.createWeiboAPI(cordova.getActivity().getApplicationContext(),WEIBO_APP_ID);
      weiboShareAPI.registerApp();
    }

    // your init code here
  }
  @Override
  public boolean execute(String action, JSONArray args, CallbackContext callbackContext) throws JSONException {
    this._callbackContext = callbackContext; // only used for onActivityResult
    this.pasteMessage = null;
    if (ACTION_AVAILABLE_EVENT.equals(action)) {
      callbackContext.sendPluginResult(new PluginResult(PluginResult.Status.OK));
      return true;
    } else if (ACTION_SHARE_EVENT.equals(action)) {
      return doSendIntent(callbackContext, args.getString(0), args.getString(1), args.getJSONArray(2), args.getString(3), null, false);
    } else if (ACTION_SHARE_VIA_TWITTER_EVENT.equals(action)) {
      return doSendIntent(callbackContext, args.getString(0), args.getString(1), args.getJSONArray(2), args.getString(3), "twitter", false);
    } else if (ACTION_SHARE_VIA_FACEBOOK_EVENT.equals(action)) {
      return doSendIntent(callbackContext, args.getString(0), args.getString(1), args.getJSONArray(2), args.getString(3), "com.facebook.katana", false);
    } else if (ACTION_SHARE_VIA_FACEBOOK_WITH_PASTEMESSAGEHINT.equals(action)) {
      this.pasteMessage = args.getString(4);
      return doSendIntent(callbackContext, args.getString(0), args.getString(1), args.getJSONArray(2), args.getString(3), "com.facebook.katana", false);
    } else if (ACTION_SHARE_VIA_WHATSAPP_EVENT.equals(action)) {
      if (notEmpty(args.getString(4))) {
        return shareViaWhatsAppDirectly(callbackContext, args.getString(0), args.getString(1), args.getJSONArray(2), args.getString(3), args.getString(4));
      } else {
        return doSendIntent(callbackContext, args.getString(0), args.getString(1), args.getJSONArray(2), args.getString(3), "whatsapp", false);
      }
    } else if (ACTION_SHARE_VIA_INSTAGRAM_EVENT.equals(action)) {
      if (notEmpty(args.getString(0))) {
        copyHintToClipboard(args.getString(0), "Instagram paste message");
      }
      return doSendIntent(callbackContext, args.getString(0), args.getString(1), args.getJSONArray(2), args.getString(3), "instagram", false);
    } else if (ACTION_CAN_SHARE_VIA.equals(action)) {
      return doSendIntent(callbackContext, args.getString(0), args.getString(1), args.getJSONArray(2), args.getString(3), args.getString(4), true);
    } else if (ACTION_CAN_SHARE_VIA_EMAIL.equals(action)) {
      if (isEmailAvailable()) {
        callbackContext.sendPluginResult(new PluginResult(PluginResult.Status.OK));
        return true;
      } else {
        callbackContext.sendPluginResult(new PluginResult(PluginResult.Status.ERROR, "not available"));
        return false;
      }
    } else if (ACTION_SHARE_VIA.equals(action)) {
      return doSendIntent(callbackContext, args.getString(0), args.getString(1), args.getJSONArray(2), args.getString(3), args.getString(4), false);
    } else if (ACTION_SHARE_VIA_SMS_EVENT.equals(action)) {
      return invokeSMSIntent(callbackContext, args.getJSONObject(0), args.getString(1));
    } else if (ACTION_SHARE_VIA_EMAIL_EVENT.equals(action)) {
      return invokeEmailIntent(callbackContext, args.getString(0), args.getString(1), args.getJSONArray(2), args.isNull(3) ? null : args.getJSONArray(3), args.isNull(4) ? null : args.getJSONArray(4), args.isNull(5) ? null : args.getJSONArray(5));
    } else if(ACTION_SHARE_VIA_QQ_EVENT.equals(action)){
      return shareViaQq(callbackContext,args.getString(0), args.getString(1), args.isNull(2)?null:args.getJSONArray(2), args.isNull(3)?null:args.getString(3),args.isNull(4)?null:args.getString(4));
    } else if(ACTION_SHARE_VIA_WECHAT_EVENT.equals(action)){
      return shareViaWechat(callbackContext,args.getString(0), args.getString(1), args.isNull(2)?null:args.getJSONArray(2), args.isNull(3)?null:args.getString(3),args.isNull(4)?null:args.getString(4));
    } else if(ACTION_SHARE_VIA_WEI_BO.equals(action)){
      return shareViaWeiBo(callbackContext,args.getString(0),args.getString(1),args.isNull(2)?null:args.getJSONArray(2), args.isNull(3)?null:args.getString(3));
    } else {
      callbackContext.error("socialSharing." + action + " is not a supported function. Did you mean '" + ACTION_SHARE_EVENT + "'?");
      return false;
    }
  }

  private boolean isEmailAvailable() {
    final Intent intent = new Intent(Intent.ACTION_SENDTO, Uri.fromParts("mailto", "someone@domain.com", null));
    return cordova.getActivity().getPackageManager().queryIntentActivities(intent, 0).size() > 0;
  }

  private boolean invokeEmailIntent(final CallbackContext callbackContext, final String message, final String subject, final JSONArray to, final JSONArray cc, final JSONArray bcc, final JSONArray files) throws JSONException {

    final SocialSharing plugin = this;
    cordova.getThreadPool().execute(new SocialSharingRunnable(callbackContext) {
      public void run() {
        final Intent draft = new Intent(Intent.ACTION_SEND_MULTIPLE);
        if (notEmpty(message)) {
          Pattern htmlPattern = Pattern.compile(".*\\<[^>]+>.*", Pattern.DOTALL);
          if (htmlPattern.matcher(message).matches()) {
            draft.putExtra(android.content.Intent.EXTRA_TEXT, Html.fromHtml(message));
            draft.setType("text/html");
          } else {
            draft.putExtra(android.content.Intent.EXTRA_TEXT, message);
            draft.setType("text/plain");
          }
        }
        if (notEmpty(subject)) {
          draft.putExtra(android.content.Intent.EXTRA_SUBJECT, subject);
        }
        try {
          if (to != null && to.length() > 0) {
            draft.putExtra(android.content.Intent.EXTRA_EMAIL, toStringArray(to));
          }
          if (cc != null && cc.length() > 0) {
            draft.putExtra(android.content.Intent.EXTRA_CC, toStringArray(cc));
          }
          if (bcc != null && bcc.length() > 0) {
            draft.putExtra(android.content.Intent.EXTRA_BCC, toStringArray(bcc));
          }
          if (files.length() > 0) {
            final String dir = getDownloadDir();
            if (dir != null) {
              ArrayList<Uri> fileUris = new ArrayList<Uri>();
              for (int i = 0; i < files.length(); i++) {
                final Uri fileUri = getFileUriAndSetType(draft, dir, files.getString(i), subject, i);
                if (fileUri != null) {
                  fileUris.add(fileUri);
                }
              }
              if (!fileUris.isEmpty()) {
                draft.putExtra(Intent.EXTRA_STREAM, fileUris);
              }
            }
          }
        } catch (Exception e) {
          callbackContext.error(e.getMessage());
        }

        draft.setType("application/octet-stream");
        cordova.startActivityForResult(plugin, Intent.createChooser(draft, "Choose Email App"), ACTIVITY_CODE_SENDVIAEMAIL);
      }
    });

    return true;
  }

  private String getDownloadDir() throws IOException {
    // better check, otherwise it may crash the app
    if (Environment.MEDIA_MOUNTED.equals(Environment.getExternalStorageState())) {
      // we need to use external storage since we need to share to another app
      final String dir = webView.getContext().getExternalFilesDir(null) + "/socialsharing-downloads";
      createOrCleanDir(dir);
      return dir;
    } else {
      return null;
    }
  }

  private boolean doSendIntent(final CallbackContext callbackContext, final String msg, final String subject, final JSONArray files, final String url, final String appPackageName, final boolean peek) {

    final CordovaInterface mycordova = cordova;
    final CordovaPlugin plugin = this;

    cordova.getThreadPool().execute(new SocialSharingRunnable(callbackContext) {
      public void run() {
        String message = msg;
        final boolean hasMultipleAttachments = files.length() > 1;
        final Intent sendIntent = new Intent(hasMultipleAttachments ? Intent.ACTION_SEND_MULTIPLE : Intent.ACTION_SEND);
        sendIntent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_WHEN_TASK_RESET);

        try {
          if (files.length() > 0 && !"".equals(files.getString(0))) {
            final String dir = getDownloadDir();
            if (dir != null) {
              ArrayList<Uri> fileUris = new ArrayList<Uri>();
              Uri fileUri = null;
              for (int i = 0; i < files.length(); i++) {
                fileUri = getFileUriAndSetType(sendIntent, dir, files.getString(i), subject, i);
                if (fileUri != null) {
                  fileUris.add(fileUri);
                }
              }
              if (!fileUris.isEmpty()) {
                if (hasMultipleAttachments) {
                  sendIntent.putExtra(Intent.EXTRA_STREAM, fileUris);
                } else {
                  sendIntent.putExtra(Intent.EXTRA_STREAM, fileUri);
                }
              }
            } else {
              sendIntent.setType("text/plain");
            }
          } else {
            sendIntent.setType("text/plain");
          }
        } catch (Exception e) {
          callbackContext.error(e.getMessage());
        }

        if (notEmpty(subject)) {
          sendIntent.putExtra(Intent.EXTRA_SUBJECT, subject);
        }
        // add the URL to the message, as there seems to be no separate field
        if (notEmpty(url)) {
          if (notEmpty(message)) {
            message += " " + url;
          } else {
            message = url;
          }
        }
        if (notEmpty(message)) {
          sendIntent.putExtra(android.content.Intent.EXTRA_TEXT, message);
          // sometimes required when the user picks share via sms
          if (Build.VERSION.SDK_INT < 21) { // LOLLIPOP
            sendIntent.putExtra("sms_body", message);
          }
        }

        if (appPackageName != null) {
          String packageName = appPackageName;
          String passedActivityName = null;
          if (packageName.contains("/")) {
            String[] items = appPackageName.split("/");
            packageName = items[0];
            passedActivityName = items[1];
          }
          final ActivityInfo activity = getActivity(callbackContext, sendIntent, packageName);
          if (activity != null) {
            if (peek) {
              callbackContext.sendPluginResult(new PluginResult(PluginResult.Status.OK));
            } else {
              sendIntent.addCategory(Intent.CATEGORY_LAUNCHER);
              sendIntent.setComponent(new ComponentName(activity.applicationInfo.packageName,
                      passedActivityName != null ? passedActivityName : activity.name));
              mycordova.startActivityForResult(plugin, sendIntent, 0);

              if (pasteMessage != null) {
                // add a little delay because target app (facebook only atm) needs to be started first
                new Timer().schedule(new TimerTask() {
                  public void run() {
                    cordova.getActivity().runOnUiThread(new Runnable() {
                      public void run() {
                        copyHintToClipboard(msg, pasteMessage);
                        showPasteMessage(pasteMessage);
                      }
                    });
                  }
                }, 2000);
              }
            }
          }
        } else {
          if (peek) {
            callbackContext.sendPluginResult(new PluginResult(PluginResult.Status.OK));
          } else {
            mycordova.startActivityForResult(plugin, Intent.createChooser(sendIntent, null), ACTIVITY_CODE_SEND);
          }
        }
      }
    });
    return true;
  }

  @SuppressLint("NewApi")
  private void copyHintToClipboard(String msg, String label) {
    if (Build.VERSION.SDK_INT < Build.VERSION_CODES.HONEYCOMB) {
      return;
    }
    final ClipboardManager clipboard = (android.content.ClipboardManager) cordova.getActivity().getSystemService(Context.CLIPBOARD_SERVICE);
    final ClipData clip = android.content.ClipData.newPlainText(label, msg);
    clipboard.setPrimaryClip(clip);
  }

  @SuppressLint("NewApi")
  private void showPasteMessage(String label) {
    if (Build.VERSION.SDK_INT < Build.VERSION_CODES.HONEYCOMB) {
      return;
    }
    final Toast toast = Toast.makeText(webView.getContext(), label, Toast.LENGTH_LONG);
    toast.setGravity(Gravity.CENTER_VERTICAL | Gravity.CENTER_HORIZONTAL, 0, 0);
    toast.show();
  }

  private Uri getFileUriAndSetType(Intent sendIntent, String dir, String image, String subject, int nthFile) throws IOException {
    // we're assuming an image, but this can be any filetype you like
    String localImage = image;
    sendIntent.setType("image/*");
    if (image.startsWith("http") || image.startsWith("www/")) {
      String filename = getFileName(image);
      localImage = "file://" + dir + "/" + filename;
      if (image.startsWith("http")) {
        // filename optimisation taken from https://github.com/EddyVerbruggen/SocialSharing-PhoneGap-Plugin/pull/56
        URLConnection connection = new URL(image).openConnection();
        String disposition = connection.getHeaderField("Content-Disposition");
        if (disposition != null) {
          final Pattern dispositionPattern = Pattern.compile("filename=([^;]+)");
          Matcher matcher = dispositionPattern.matcher(disposition);
          if (matcher.find()) {
            filename = matcher.group(1).replaceAll("[^a-zA-Z0-9._-]", "");
            localImage = "file://" + dir + "/" + filename;
          }
        }
        saveFile(getBytes(connection.getInputStream()), dir, filename);
      } else {
        saveFile(getBytes(webView.getContext().getAssets().open(image)), dir, filename);
      }
    } else if (image.startsWith("data:")) {
      // safeguard for https://code.google.com/p/android/issues/detail?id=7901#c43
      if (!image.contains(";base64,")) {
        sendIntent.setType("text/plain");
        return null;
      }
      // image looks like this: data:image/png;base64,R0lGODlhDAA...
      final String encodedImg = image.substring(image.indexOf(";base64,") + 8);
      // correct the intent type if anything else was passed, like a pdf: data:application/pdf;base64,..
      if (!image.contains("data:image/")) {
        sendIntent.setType(image.substring(image.indexOf("data:") + 5, image.indexOf(";base64")));
      }
      // the filename needs a valid extension, so it renders correctly in target apps
      final String imgExtension = image.substring(image.indexOf("/") + 1, image.indexOf(";base64"));
      String fileName;
      // if a subject was passed, use it as the filename
      // filenames must be unique when passing in multiple files [#158]
      if (notEmpty(subject)) {
        fileName = sanitizeFilename(subject) + (nthFile == 0 ? "" : "_" + nthFile) + "." + imgExtension;
      } else {
        fileName = "file" + (nthFile == 0 ? "" : "_" + nthFile) + "." + imgExtension;
      }
      saveFile(Base64.decode(encodedImg, Base64.DEFAULT), dir, fileName);
      localImage = "file://" + dir + "/" + fileName;
    } else if (image.startsWith("df:")) {
      // safeguard for https://code.google.com/p/android/issues/detail?id=7901#c43
      if (!image.contains(";base64,")) {
        sendIntent.setType("text/plain");
        return null;
      }
      // format looks like this :  df:filename.txt;data:image/png;base64,R0lGODlhDAA...
      final String fileName = image.substring(image.indexOf("df:") + 3, image.indexOf(";data:"));
      final String fileType = image.substring(image.indexOf(";data:") + 6, image.indexOf(";base64,"));
      final String encodedImg = image.substring(image.indexOf(";base64,") + 8);
      sendIntent.setType(fileType);
      saveFile(Base64.decode(encodedImg, Base64.DEFAULT), dir, sanitizeFilename(fileName));
      localImage = "file://" + dir + "/" + fileName;
    } else if (!image.startsWith("file://")) {
      throw new IllegalArgumentException("URL_NOT_SUPPORTED");
    }
    return Uri.parse(localImage);
  }

  private boolean shareViaWhatsAppDirectly(final CallbackContext callbackContext, String message, final String subject, final JSONArray files, final String url, final String number) {
    // add the URL to the message, as there seems to be no separate field
    if (notEmpty(url)) {
      if (notEmpty(message)) {
        message += " " + url;
      } else {
        message = url;
      }
    }
    final String shareMessage = message;
    final SocialSharing plugin = this;
    cordova.getThreadPool().execute(new SocialSharingRunnable(callbackContext) {
      public void run() {
        Intent intent = new Intent(Intent.ACTION_SENDTO);
        intent.setData(Uri.parse("smsto:" + number));

        intent.putExtra("sms_body", shareMessage);
        intent.putExtra("sms_subject", subject);
        intent.setPackage("com.whatsapp");

        try {
          if (files.length() > 0 && !"".equals(files.getString(0))) {
            final boolean hasMultipleAttachments = files.length() > 1;
            final String dir = getDownloadDir();
            if (dir != null) {
              ArrayList<Uri> fileUris = new ArrayList<Uri>();
              Uri fileUri = null;
              for (int i = 0; i < files.length(); i++) {
                fileUri = getFileUriAndSetType(intent, dir, files.getString(i), subject, i);
                if (fileUri != null) {
                  fileUris.add(fileUri);
                }
              }
              if (!fileUris.isEmpty()) {
                if (hasMultipleAttachments) {
                  intent.putExtra(Intent.EXTRA_STREAM, fileUris);
                } else {
                  intent.putExtra(Intent.EXTRA_STREAM, fileUri);
                }
              }
            }
          }
        } catch (Exception e) {
          callbackContext.error(e.getMessage());
        }
        try {
          cordova.startActivityForResult(plugin, intent, ACTIVITY_CODE_SENDVIAWHATSAPP);
        } catch (Exception e) {
          callbackContext.error(e.getMessage());
        }
      }
    });
    return true;
  }

  private boolean invokeSMSIntent(final CallbackContext callbackContext, JSONObject options, String p_phonenumbers) {
    final String message = options.optString("message");
    // TODO test this on a real SMS enabled device before releasing it
//    final String subject = options.optString("subject");
//    final String image = options.optString("image");
    final String subject = null; //options.optString("subject");
    final String image = null; // options.optString("image");
    final String phonenumbers = getPhoneNumbersWithManufacturerSpecificSeparators(p_phonenumbers);
    final SocialSharing plugin = this;
    cordova.getThreadPool().execute(new SocialSharingRunnable(callbackContext) {
      public void run() {
        Intent intent;

        if (Build.VERSION.SDK_INT >= 19) { // Build.VERSION_CODES.KITKAT) {
          // passing in no phonenumbers for kitkat may result in an error,
          // but it may also work for some devices, so documentation will need to cover this case
          intent = new Intent(Intent.ACTION_SENDTO);
          intent.setData(Uri.parse("smsto:" + (notEmpty(phonenumbers) ? phonenumbers : "")));
        } else {
          intent = new Intent(Intent.ACTION_VIEW);
          intent.setType("vnd.android-dir/mms-sms");
          if (phonenumbers != null) {
            intent.putExtra("address", phonenumbers);
          }
        }
        intent.putExtra("sms_body", message);
        intent.putExtra("sms_subject", subject);

        try {
          if (image != null && !"".equals(image)) {
            final Uri fileUri = getFileUriAndSetType(intent, getDownloadDir(), image, subject, 0);
            if (fileUri != null) {
              intent.putExtra(Intent.EXTRA_STREAM, fileUri);
            }
          }
          cordova.startActivityForResult(plugin, intent, 0);
        } catch (Exception e) {
          callbackContext.error(e.getMessage());
        }
      }
    });
    return true;
  }

  private static String getPhoneNumbersWithManufacturerSpecificSeparators(String phonenumbers) {
    if (notEmpty(phonenumbers)) {
      char separator;
      if (android.os.Build.MANUFACTURER.equalsIgnoreCase("samsung")) {
        separator = ',';
      } else {
        separator = ';';
      }
      return phonenumbers.replace(';', separator).replace(',', separator);
    }
    return null;
  }

  private ActivityInfo getActivity(final CallbackContext callbackContext, final Intent shareIntent, final String appPackageName) {
    final PackageManager pm = webView.getContext().getPackageManager();
    List<ResolveInfo> activityList = pm.queryIntentActivities(shareIntent, 0);
    for (final ResolveInfo app : activityList) {
      if ((app.activityInfo.packageName).contains(appPackageName)) {
        return app.activityInfo;
      }
    }
    // no matching app found
    callbackContext.sendPluginResult(new PluginResult(PluginResult.Status.ERROR, getShareActivities(activityList)));
    return null;
  }

  private JSONArray getShareActivities(List<ResolveInfo> activityList) {
    List<String> packages = new ArrayList<String>();
    for (final ResolveInfo app : activityList) {
      packages.add(app.activityInfo.packageName);
    }
    return new JSONArray(packages);
  }

  @Override
  public void onActivityResult(int requestCode, int resultCode, Intent intent) {
    super.onActivityResult(requestCode, resultCode, intent);
    if (_callbackContext != null) {
      if (ACTIVITY_CODE_SENDVIAEMAIL == requestCode) {
        _callbackContext.success();
      } else {
        _callbackContext.sendPluginResult(new PluginResult(PluginResult.Status.OK, resultCode == Activity.RESULT_OK));
      }
    }
  }

  private void createOrCleanDir(final String downloadDir) throws IOException {
    final File dir = new File(downloadDir);
    if (!dir.exists()) {
      if (!dir.mkdirs()) {
        throw new IOException("CREATE_DIRS_FAILED");
      }
    } else {
      cleanupOldFiles(dir);
    }
  }

  private static String getFileName(String url) {
    final String pattern = ".*/([^?#]+)?";
    Pattern r = Pattern.compile(pattern);
    Matcher m = r.matcher(url);
    if (m.find()) {
      return m.group(1);
    } else {
      return null;
    }
  }

  private byte[] getBytes(InputStream is) throws IOException {
    ByteArrayOutputStream buffer = new ByteArrayOutputStream();
    int nRead;
    byte[] data = new byte[16384];
    while ((nRead = is.read(data, 0, data.length)) != -1) {
      buffer.write(data, 0, nRead);
    }
    buffer.flush();
    return buffer.toByteArray();
  }

  private void saveFile(byte[] bytes, String dirName, String fileName) throws IOException {
    final File dir = new File(dirName);
    final FileOutputStream fos = new FileOutputStream(new File(dir, fileName));
    fos.write(bytes);
    fos.flush();
    fos.close();
  }

  /**
   * As file.deleteOnExit does not work on Android, we need to delete files manually.
   * Deleting them in onActivityResult is not a good idea, because for example a base64 encoded file
   * will not be available for upload to Facebook (it's deleted before it's uploaded).
   * So the best approach is deleting old files when saving (sharing) a new one.
   */
  private void cleanupOldFiles(File dir) {
    for (File f : dir.listFiles()) {
      //noinspection ResultOfMethodCallIgnored
      f.delete();
    }
  }

  private static boolean notEmpty(String what) {
    return what != null &&
        !"".equals(what) &&
        !"null".equalsIgnoreCase(what);
  }

  private static String[] toStringArray(JSONArray jsonArray) throws JSONException {
    String[] result = new String[jsonArray.length()];
    for (int i = 0; i < jsonArray.length(); i++) {
      result[i] = jsonArray.getString(i);
    }
    return result;
  }

  public static String sanitizeFilename(String name) {
    return name.replaceAll("[:\\\\/*?|<> ]", "_");
  }

  private boolean shareViaQq(final CallbackContext callbackContext,final String message, final String subject, final JSONArray files, final String url ,final String toZone){
    cordova.getActivity().runOnUiThread(new Runnable() {
      public void run() {
        if (mTencent == null) {
          callbackContext.error("init error");
          return;
        }
        final Bundle params = new Bundle();
        try {
          params.putInt(QQShare.SHARE_TO_QQ_KEY_TYPE, QQShare.SHARE_TO_QQ_TYPE_DEFAULT);
          params.putString(QQShare.SHARE_TO_QQ_TITLE, subject);
          params.putString(QQShare.SHARE_TO_QQ_SUMMARY, message);
          params.putString(QQShare.SHARE_TO_QQ_TARGET_URL, url);
          params.putString(QQShare.SHARE_TO_QQ_IMAGE_URL, (files.length() > 0 && !"".equals(files.getString(0)) ? files.getString(0) : ""));
          params.putString(QQShare.SHARE_TO_QQ_APP_NAME, cordova.getActivity().getString(R.string.app_name));
          if (notEmpty(toZone)) {
            params.putInt(QQShare.SHARE_TO_QQ_EXT_INT, QQShare.SHARE_TO_QQ_FLAG_QZONE_AUTO_OPEN);
          }
          mTencent.shareToQQ(cordova.getActivity(), params, new IUiListener() {
            @Override
            public void onCancel() {
              callbackContext.error("分享取消");
            }

            @Override
            public void onComplete(Object response) {
              // TODO Auto-generated method stub
              callbackContext.success();
            }

            @Override
            public void onError(UiError e) {
              // TODO Auto-generated method stub
              //Util.toastMessage(QQShareActivity.this, "onError: " + e.errorMessage, "e");
              callbackContext.error(e.errorMessage);
            }
          });
        } catch (Exception e) {
          callbackContext.error(e.getMessage());
        }
      }
    });
    return true;
  }

  private boolean shareViaWechat(final CallbackContext callbackContext,final String message, final String subject, final JSONArray files, final String url,final String scene){
    cordova.getThreadPool().execute(new Runnable() {
      public void run() {
        final IWXAPI mIWXAPI = WXAPIFactory.createWXAPI(webView.getContext(), WECHAT_APP_ID);
        if (!mIWXAPI.isWXAppInstalled()) {
          callbackContext.error("No wechat found!");
          return;
        }
        SendMessageToWX.Req mReq = new SendMessageToWX.Req();
        if (WECHAT_SHARE_FAVORITE.equals(scene)) {
          mReq.scene = SendMessageToWX.Req.WXSceneFavorite;
        } else if (WECHAT_SHARE_TIMELINE.equals(scene)) {
          mReq.scene = SendMessageToWX.Req.WXSceneTimeline;
        } else {
          mReq.scene = SendMessageToWX.Req.WXSceneSession;
        }
        WXMediaMessage mMessage = new WXMediaMessage();
        mMessage.sdkVer = 1;
        if (subject.getBytes().length > 512) {
          mMessage.title = new String(Arrays.copyOfRange(subject.getBytes(), 0, 511));
        } else {
          mMessage.title = subject;
        }
        if (message.getBytes().length > 1024) {
          mMessage.description = new String(Arrays.copyOfRange(subject.getBytes(), 0, 1023));
        } else {
          mMessage.description = message;
        }
        final String messageAction = buildAction();
        mMessage.messageAction = messageAction;
        try {
          if (notEmpty(url)) {
            mMessage.mediaObject = new WXWebpageObject(url);
            if (files.length() > 0 && !"".equals(files.getString(0))) {
              Bitmap img = get_bitmap_from_url(files.getString(0));
              if (img == null) {
                callbackContext.error("no img found!");
                return;
              }
              mMessage.setThumbImage(Bitmap.createScaledBitmap(img, THUMB_SIZE, THUMB_SIZE, true));
              img.recycle();
            }
          } else if (notEmpty(files.getString(0))) {
            Bitmap img = get_bitmap_from_url(files.getString(0));
            if (img == null) {
              callbackContext.error("no img found!");
              return;
            }
            mMessage.mediaObject = new WXImageObject(img);
            mMessage.setThumbImage(Bitmap.createScaledBitmap(img, THUMB_SIZE, THUMB_SIZE, true));
            img.recycle();

          } else if (notEmpty(message)) {
            mMessage.mediaObject = new WXTextObject(message);
          } else {
            callbackContext.error("no message detect!");
            return;
          }
          mReq.message = mMessage;
          if (!mReq.checkArgs()) {
            callbackContext.error("message arguments error!");
          }
          IntentFilter intentFilter = new IntentFilter();
          intentFilter.addAction(messageAction);
          TencentMMReceiver mmReceiver = new TencentMMReceiver() {
            @Override
            public void onReceive(Context context, Intent intent) {
              if (mIWXAPI.handleIntent(intent, this)) {
                // intent handled as wechat request/response
                return;
              }
            }

            @Override
            public void onReq(BaseReq baseReq) {

            }

            @Override
            public void onResp(BaseResp baseResp) {
              switch (baseResp.getType()) {
                case ConstantsAPI.COMMAND_SENDAUTH:

                  break;

                case ConstantsAPI.COMMAND_SENDMESSAGE_TO_WX:
                  // 处理微信主程序返回的SendMessageToWX.Resp
                  if (baseResp.errCode == SendMessageToWX.Resp.ErrCode.ERR_OK) {
                    callbackContext.success();
                  } else {
                    callbackContext.error(baseResp.errStr);
                  }
                  webView.getContext().unregisterReceiver(this);
                  break;

                default:
                  break;
              }
            }
          };
          webView.getContext().registerReceiver(mmReceiver, intentFilter);
          mIWXAPI.sendReq(mReq);
        } catch (Exception e) {
          callbackContext.error(e.getMessage());
        }
      }
    });
    return true;
  }

  private Bitmap get_bitmap_from_url(String imgUrl){
    try{
      byte[] imgBytes=DownLoadRemoteImg(imgUrl);
      if(imgBytes==null){
        return null;
      }else{
        final Bitmap bitmap=BitmapFactory.decodeByteArray(imgBytes,0,imgBytes.length);
        return bitmap;
      }
    }catch (Exception e){
      return null;
    }
  }

  private byte[] DownLoadRemoteImg(String image) throws IOException {
    // we're assuming an image, but this can be any filetype you like
    if (image.startsWith("http") || image.startsWith("www/")) {
        // filename optimisation taken from https://github.com/EddyVerbruggen/SocialSharing-PhoneGap-Plugin/pull/56
        URLConnection connection = new URL(image).openConnection();
      return getBytes(connection.getInputStream());
    }
    return null;
  }

  private byte[] bitmapToThumb(Bitmap bitmap,int width,int height){
    Bitmap target=Bitmap.createScaledBitmap(bitmap,width,height,true);
    ByteArrayOutputStream baos = new ByteArrayOutputStream();
    target.compress(Bitmap.CompressFormat.JPEG, 80, baos);
    return baos.toByteArray();
  }

  private String buildAction() {
    return WECHAT_ACTION_PREFIX + String.valueOf(System.currentTimeMillis());
  }

  private boolean shareViaWeiBo(final CallbackContext callbackContext,final String message, final String subject, final JSONArray files, final String url){
    final CordovaPlugin sharePlugin=this;
    cordova.getThreadPool().execute(new Runnable() {
      public void run() {
        if(WEIBO_APP_ID.isEmpty() || weiboShareAPI == null) {
          callbackContext.error("weibo app key is not set!");
          return;
        }
        if(!weiboShareAPI.isWeiboAppInstalled() || !weiboShareAPI.isWeiboAppSupportAPI()) {
          callbackContext.error("Please update your weibo app.");
          return;
        }
        WeiboMessage wbMsg = new WeiboMessage();
        if(!url.isEmpty()){
          WebpageObject webObj=new WebpageObject();
          webObj.actionUrl=url;
          webObj.description=message;
          webObj.title=subject;
          webObj.identify=String.valueOf(System.currentTimeMillis());
          if(files.length()>0){
            try{
              String img_url=files.getString(0);
              Bitmap image=get_bitmap_from_url(img_url);
              webObj.thumbData=bitmapToThumb(image,50,50);
            }catch (Exception e){

            }
          }
          wbMsg.mediaObject=webObj;
        }else if(files.length()>0){
          ImageObject imgObj= new ImageObject();
          try{
            String img_url=files.getString(0);
            Bitmap image=get_bitmap_from_url(img_url);
            imgObj.imageData=bitmapToThumb(image,image.getWidth(),image.getHeight());
            imgObj.thumbData=bitmapToThumb(image,50,50);
            imgObj.description=message;
            imgObj.title=subject;
            imgObj.identify=String.valueOf(System.currentTimeMillis());
            wbMsg.mediaObject=imgObj;
          }catch (Exception e){
            TextObject txObj=new TextObject();
            txObj.text=message;
            txObj.identify=String.valueOf(System.currentTimeMillis());
            wbMsg.mediaObject=txObj;
          }
        }else{
          TextObject txObj=new TextObject();
          txObj.text=message;
          txObj.identify=String.valueOf(System.currentTimeMillis());
          wbMsg.mediaObject=txObj;
        }
        SendMessageToWeiboRequest request=new SendMessageToWeiboRequest();
        request.message=wbMsg;
        request.transaction=String.valueOf(System.currentTimeMillis());
        AuthInfo authInfo=new AuthInfo(cordova.getActivity().getApplicationContext(),WEIBO_APP_ID,WEIBO_URL,"all");
        if(!weiboShareAPI.sendRequest(cordova.getActivity(),request,authInfo,weibo_token,new AuthListener())){
          callbackContext.error("error when sending request to weibo app");
        }
      }
    });

    return true;
  }

  class AuthListener implements WeiboAuthListener {

    @Override
    public void onComplete(Bundle values) {
      // 从 Bundle 中解析 Token
      Oauth2AccessToken mAccessToken = Oauth2AccessToken.parseAccessToken(values);
      //从这里获取用户输入的 电话号码信息
      String  phoneNum =  mAccessToken.getPhoneNum();
      if (mAccessToken.isSessionValid()) {
        // 显示 Token
        weibo_token=mAccessToken.getToken();
      } else {
        _callbackContext.error("No access allowed");
      }
    }

    @Override
    public void onCancel() {
      Log.d("SocailSharingViaWeibo", "sharing is canceled");
    }

    @Override
    public void onWeiboException(WeiboException e) {
      Log.d("SocailSharingViaWeibo",e.getMessage());
    }
  }

  /**
   * Called when the activity receives a new intent.
   *
   * @param intent
   */
  @Override
  public void onNewIntent(Intent intent) {
    super.onNewIntent(intent);
    if(intent.getAction().equals(WBConstants.ACTIVITY_REQ_SDK)){
      String appPackage = intent.getStringExtra("_weibo_appPackage");
      String transaction = intent.getStringExtra("_weibo_transaction");
      if(TextUtils.isEmpty(transaction)) {
        LogUtil.e("SocialSharing", "handleWeiboResponse faild intent _weibo_transaction is null");
      }else{
        SendMessageToWeiboResponse data = new SendMessageToWeiboResponse(intent.getExtras());
        this.onResponse(data);
      }
    }
  }

  @Override
  public void onResponse(BaseResponse baseResponse) {
    if(baseResponse.errCode==WBConstants.ErrorCode.ERR_OK){
      _callbackContext.success("成功分享微博");
    }else if(baseResponse.errCode==WBConstants.ErrorCode.ERR_FAIL){
      _callbackContext.error("分享错误");
    } else{
      _callbackContext.error("分享被取消");
    }
  }
}
