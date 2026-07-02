package com.example.carpoolclient.auth.storage;

import android.content.Context;
import android.content.SharedPreferences;

import androidx.security.crypto.EncryptedSharedPreferences;
import androidx.security.crypto.MasterKey;

import java.io.IOException;
import java.security.GeneralSecurityException;

public final class SecureTokenStore {
    private static final String PREF_FILE = "secure_tokens";
    private static final String KEY_JWT = "jwt_token";
    private static final String KEY_FCM = "fcm_token";
    private static SecureTokenStore instance;
    private final SharedPreferences prefs;

    public SecureTokenStore(Context context) {
        try {
            MasterKey masterKey = new MasterKey.Builder(context)
                    .setKeyScheme(MasterKey.KeyScheme.AES256_GCM)
                    .build();
            prefs = EncryptedSharedPreferences.create(
                    context,
                    PREF_FILE,
                    masterKey,
                    EncryptedSharedPreferences.PrefKeyEncryptionScheme.AES256_SIV,
                    EncryptedSharedPreferences.PrefValueEncryptionScheme.AES256_GCM
            );
        } catch (GeneralSecurityException | IOException e) {
            throw new IllegalStateException("Unable to initialize secure token storage", e);
        }
    }

    public static synchronized SecureTokenStore getInstance(Context context) {
        if (instance == null) {
            instance = new SecureTokenStore(context.getApplicationContext());
        }
        return instance;
    }

    public void saveJwtToken(String token) {
        prefs.edit().putString(KEY_JWT, token).apply();
    }

    public String getJwtToken() {
        return prefs.getString(KEY_JWT, null);
    }

    public void saveFcmToken(String token) {
        prefs.edit().putString(KEY_FCM, token).apply();
    }

    public String getFcmToken() {
        return prefs.getString(KEY_FCM, null);
    }

    public void clear() {
        prefs.edit().remove(KEY_JWT).remove(KEY_FCM).apply();
    }
}

