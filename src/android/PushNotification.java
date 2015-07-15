package com.cmpsoft.mobile.plugin.pushnotification;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.SharedPreferences;
import android.content.SharedPreferences.Editor;
import android.preference.PreferenceManager;

import com.baidu.android.pushservice.PushConstants;
import com.baidu.android.pushservice.PushManager;

import org.apache.cordova.CallbackContext;
import org.apache.cordova.CordovaPlugin;
import org.apache.cordova.PluginResult;
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

public class PushNotification extends CordovaPlugin {
  private BroadcastReceiver receiver = null;
  private CallbackContext pushCallbackContext = null;

  public static final String ACTION_RESPONSE = "bccsclient.action.RESPONSE";
  public static final String RESPONSE_METHOD = "method";
  public static final String RESPONSE_CONTENT = "content";
  public static final String RESPONSE_ERRCODE = "errcode";

  public boolean execute(String action, JSONArray args, CallbackContext callbackContext) throws JSONException {
    if (action.equals("init")) {

      this.pushCallbackContext = callbackContext;
      super.initialize(cordova, webView);
      IntentFilter intentFilter = new IntentFilter();
      intentFilter.addAction(PushConstants.ACTION_RECEIVE);
      if (this.receiver == null) {
        this.receiver = new BroadcastReceiver() {
          @Override
          public void onReceive(Context context, Intent intent) {
            if (intent.getAction().equals(PushConstants.ACTION_RECEIVE)) {
              sendPushInfo(context, intent);
            }
          }
        };
        cordova.getActivity().registerReceiver(this.receiver, intentFilter);
      }

      PluginResult pluginResult = new PluginResult(PluginResult.Status.NO_RESULT);
      pluginResult.setKeepCallback(true);
      callbackContext.sendPluginResult(pluginResult);
      PushManager.startWork(cordova.getActivity().getApplicationContext(), 0, args.getString(0));
      return true;
    } else if (action.equals("getInfo")) {
      JSONObject r = new JSONObject();
      SharedPreferences sp = PreferenceManager
        .getDefaultSharedPreferences(cordova.getActivity());
      r.put("app_id", sp.getString("app_id", ""));
      r.put("channel_id", sp.getString("channel_id", ""));
      r.put("client_id", sp.getString("user_id", ""));
      callbackContext.success(r);
      return true;
    }
    return false;
  }

  private void sendPushInfo(Context context, Intent intent) {
    String content = "";
    JSONObject info = null;
    if (intent.getByteArrayExtra(PushConstants.EXTRA_CONTENT) != null) {
      content = new String(intent.getByteArrayExtra(PushConstants.EXTRA_CONTENT));
      try {
        info = (JSONObject) new JSONObject(content).get("response_params");
        info.put("deviceType", 3);

        SharedPreferences sp = PreferenceManager.getDefaultSharedPreferences(cordova.getActivity());
        Editor editor = sp.edit();
        editor.putString("app_id", info.getString("app_id"));
        editor.putString("channel_id", info.getString("channel_id"));
        editor.putString("user_id", info.getString("user_id"));

        editor.commit();
      } catch (JSONException e) {
        e.printStackTrace();
      }
    }
    if (this.pushCallbackContext != null && info != null) {
      PluginResult result = new PluginResult(PluginResult.Status.OK, info);
      result.setKeepCallback(false);
      this.pushCallbackContext.sendPluginResult(result);
    }
    if (this.receiver != null) {
      try {
        this.cordova.getActivity().unregisterReceiver(this.receiver);
        this.receiver = null;
      } catch (Exception e) {
        //
      }
    }
  }
}
