/*
 * Copyright (C) 2014 The ParanoidAndroid Project
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package com.android.systemui.statusbar.policy.paranoid;

import android.app.INotificationManager;
import android.app.Notification;
import android.content.ComponentName;
import android.content.Context;
import android.content.pm.PackageManager.NameNotFoundException;
import android.content.res.Configuration;
import android.content.res.Resources;
import android.graphics.drawable.BitmapDrawable;
import android.graphics.drawable.Drawable;
import android.os.Bundle;
import android.os.RemoteException;
import android.os.ServiceManager;
import android.os.UserHandle;
import android.service.notification.INotificationListener;
import android.service.notification.StatusBarNotification;
import android.util.AttributeSet;
import android.util.Log;
import android.widget.FrameLayout;
import android.widget.RemoteViews;

public class CustomNotificationListener extends FrameLayout {

    private INotificationManager mNotificationManager;
    private INotificationListenerWrapper mNotificationListener;

    /**
     * Simple class that listens to changes in notifications
     */
    private class INotificationListenerWrapper extends INotificationListener.Stub {
        @Override
        public void onNotificationPosted(final StatusBarNotification sbn) {
            // show notification
        }
        @Override
        public void onNotificationRemoved(final StatusBarNotification sbn) {
            // remove notification
        }
    }

    public CustomNotificationListener(Context context) {
        this(context, null);
    }

    public CustomNotificationListener(Context context, AttributeSet attrs) {
        super(context, attrs);

        mNotificationManager = INotificationManager.Stub.asInterface(
                ServiceManager.getService(Context.NOTIFICATION_SERVICE));
        mNotificationListener = new INotificationListenerWrapper();
    }

    @Override protected void onAttachedToWindow() {
        super.onAttachedToWindow();
        registerNotificationListener();
    }

    @Override
    protected void onDetachedFromWindow() {
        super.onDetachedFromWindow();
        unregisterNotificationListener();
    }

    @Override
    protected void onConfigurationChanged(Configuration newConfig) {
        // update
    }

    private void registerNotificationListener() {
        ComponentName cn = new ComponentName(mContext, getClass().getName());
        try {
            mNotificationManager.registerListener(mNotificationListener, cn, UserHandle.USER_ALL);
        } catch (RemoteException e) {
            // failed to register
        }
    }

    private void unregisterNotificationListener() {
        if (mNotificationListener !=  null) {
            try {
                mNotificationManager.unregisterListener(mNotificationListener, UserHandle.USER_ALL);
            } catch (RemoteException e) {
                // ... sad panda
            }
        }
    }

    private StatusBarNotification getNextNotification() {
        try {
            StatusBarNotification[] sbns = mNotificationManager
                    .getActiveNotificationsFromListener(mNotificationListener);
            if (sbns == null) return null;
            for (int i = sbns.length - 1; i >= 0; i--) {
                if (sbns[i] == null)
                    continue;
                if (isBlacklisted(sbns[i])) {
                    return sbns[i];
                }
            }
        } catch (RemoteException e) {
        }

        return null;
    }

    private StatusBarNotification getPreviousNotification() {
        try {
            StatusBarNotification[] sbns = mNotificationManager
                    .getActiveNotificationsFromListener(mNotificationListener);
            if (sbns == null) return null;
            for (int i = 0; i <= sbns.length - 1; i++) {
                if (sbns[i] == null) continue;
                if (!isBlacklisted(sbns[i])) return sbns[i];
            }
        } catch (RemoteException e) {
        }

        return null;
    }

    /*TODO isPackageAllowedForHalo doesn't actually exist and should be renamed*/
    private boolean isBlacklisted(StatusBarNotification sbn) {
        boolean allowed = false;
        /*try {
            allowed = !mNotificationManager.isPackageAllowedForHalo(sbn.getPackageName());
        } catch (android.os.RemoteException ex) {
                // System is dead
		}*/
        return allowed;
    }

    private RemoteViews getRemoteView(StatusBarNotification sbn) {
        final Notification notification = sbn.getNotification();
        boolean useBigContent = notification.bigContentView != null;
        
        RemoteViews rv = useBigContent
                                ? notification.bigContentView
                                : notification.contentView;

        return rv;
    }

    private Drawable getIcon(final StatusBarNotification sbn) {
        Drawable notificationIcon = null;
        try {
            Context pkgContext = mContext.createPackageContext(sbn.getPackageName(),
                                                                Context.CONTEXT_RESTRICTED);
            notificationIcon = pkgContext.getResources()
                                                .getDrawable(sbn.getNotification().icon);
        } catch (NameNotFoundException nnfe) {
            /*notificationIcon = mContext.getResources()
                                                .getDrawable(R.drawable.*******);*/
        } catch (Resources.NotFoundException nfe) {
            /*notificationIcon = mContext.getResources()
                                                .getDrawable(R.drawable.*******);*/
        }
        return notificationIcon;
    }

    private Drawable getLargeIcon(final StatusBarNotification sbn) {
        Drawable largeIcon = null;
        try {
            Context pkgContext = mContext.createPackageContext(sbn.getPackageName(),
                                Context.CONTEXT_RESTRICTED);
            largeIcon = new BitmapDrawable(mContext.getResources(), sbn.getNotification().largeIcon);
        } catch (NameNotFoundException ex) {
            /*largeIcon = mContext.getResources()
                                                .getDrawable(R.drawable.*******);*/
        } catch (Resources.NotFoundException e) {
            /*largeIcon = mContext.getResources()
                                                .getDrawable(R.drawable.*******);*/
        }
        return largeIcon;
    }

    private String getTickerText(StatusBarNotification sbn) {
        final Notification notification = sbn.getNotification();
        CharSequence tickerText = notification.tickerText;

        if (tickerText == null) {
            Bundle extras = notification.extras;
            if (extras != null) {
                tickerText = extras.getCharSequence(Notification.EXTRA_TITLE, null);
            }
        }
        return tickerText.toString();
    }

    private String getSummaryText(StatusBarNotification sbn) {
        final Notification notification = sbn.getNotification();
        Bundle extras = notification.extras;
        String summaryText = null;

        if (extras != null) {
            summaryText = extras.getString(Notification.EXTRA_SUMMARY_TEXT);
        }
        return summaryText;
    }
}
