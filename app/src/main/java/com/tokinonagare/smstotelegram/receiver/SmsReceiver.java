package com.tokinonagare.smstotelegram.receiver;

import android.content.ContentResolver;
import android.content.Context;
import android.content.ContextWrapper;
import android.content.SharedPreferences;
import android.database.ContentObserver;
import android.database.Cursor;
import android.net.Uri;
import android.os.Handler;
import android.text.TextUtils;

import com.tokinonagare.smstotelegram.model.MessageSend;

/**
 * 获取最新短信内容
 * Created by tokinonagare on 05/10/2016.
 */

public class SmsReceiver extends ContextWrapper {

    private final static Uri SMS_INBOX = Uri.parse("content://sms/");

    private SharedPreferences messagePreference;
    private SharedPreferences.Editor messageEditor;

    public SmsReceiver(Context base) {
        super(base);
    }

    public String getSmsFromPhone() {
        String message = "短信内容";

        // 为缓存作准备
        messagePreference = getSharedPreferences("messageName", Context.MODE_PRIVATE);
        messageEditor = messagePreference.edit();
        // 获取短信内容
        SmsObserver smsObserver = new SmsObserver(smsHandler);
        getContentResolver().registerContentObserver(SMS_INBOX, true, smsObserver);
        ContentResolver cr = getContentResolver();

        // 获取短信：person，发件人；address，电话号码；body，内容；
        String[] projection = new String[] {"person", "address", "body"};

        Cursor cur = cr.query(SMS_INBOX, projection, null, null, "date desc");

        if (cur != null && cur.moveToNext()) {
            String number = cur.getString(cur.getColumnIndex("address"));//手机号
            String name = cur.getString(cur.getColumnIndex("person"));//联系人姓名列表
            String body = cur.getString(cur.getColumnIndex("body"));//短信内容

            // 当通讯录中有匹配名称时显示，否则隐藏
            if (name == null) {
                message = number + " " + body;
            } else {
                message = name + " " + number + " " + body;
            }
        }
        return message;
    }

    private Handler smsHandler = new Handler() {
        //这里可以进行回调的操作
    };

    /**
     * 短信变化监控
     */
    private class SmsObserver extends ContentObserver {

        SmsObserver(Handler handler) {
            super(handler);
        }

        @Override
        public void onChange(boolean selfChange) {
            super.onChange(selfChange);
            String message = getSmsFromPhone();

            // 每当有新短信时，从缓存中获取短信，并确保不重复发送
            String messageCacheKey = "当前短信";
            String messageCache = messagePreference.getString(messageCacheKey, null);

            if (!TextUtils.equals(message, messageCache)) {
                // 发送短信
                MessageSend messageSend = new MessageSend();
                messageSend.sendMessage(message);

                // 缓存当前短信
                messageEditor.putString(messageCacheKey, message);
                messageEditor.apply();
            }
        }
    }
}
