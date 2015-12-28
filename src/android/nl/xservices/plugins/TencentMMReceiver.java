package nl.xservices.plugins;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;

import com.tencent.mm.sdk.modelbase.BaseReq;
import com.tencent.mm.sdk.modelbase.BaseResp;
import com.tencent.mm.sdk.openapi.IWXAPIEventHandler;

/**
 * Created by caoyang on 15/12/24.
 */
public class TencentMMReceiver extends BroadcastReceiver implements IWXAPIEventHandler {
    @Override
    public void onReceive(Context context, Intent intent) {

    }

    @Override
    public void onReq(BaseReq baseReq) {

    }

    @Override
    public void onResp(BaseResp baseResp) {

    }
}
