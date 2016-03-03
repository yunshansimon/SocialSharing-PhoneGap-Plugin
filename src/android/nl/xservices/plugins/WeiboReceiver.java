package nl.xservices.plugins;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;

import com.sina.weibo.sdk.api.share.BaseResponse;
import com.sina.weibo.sdk.api.share.IWeiboHandler;

/**
 * Created by caoyang on 16/3/3.
 */
public class WeiboReceiver extends BroadcastReceiver implements IWeiboHandler.Response {
    @Override
    public void onReceive(Context context, Intent intent) {

    }

    @Override
    public void onResponse(BaseResponse baseResponse) {

    }
}
