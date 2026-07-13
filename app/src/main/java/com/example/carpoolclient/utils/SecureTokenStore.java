package com.example.carpoolclient.utils;

import android.content.Context;
import android.content.SharedPreferences;
import android.util.Log;

import androidx.security.crypto.EncryptedSharedPreferences;
import androidx.security.crypto.MasterKey;

import java.io.IOException;
import java.security.GeneralSecurityException;

public final class SecureTokenStore {
    private static final String TAG = "SecureTokenStore";
    private static final String PREF_FILE = "secure_tokens";
    private static final String KEY_JWT = "jwt_token";
    private static final String KEY_FCM = "fcm_token";
    private static SecureTokenStore instance;
    private final SharedPreferences prefs;

    private boolean isVerified;

    public boolean isVerified() {
        return isVerified;
    }

    public void setVerified(boolean verified) {
        isVerified = verified;
    }

    public SecureTokenStore(Context context) {
        SharedPreferences tempPrefs;
        try {
            tempPrefs = createEncryptedPrefs(context);
        } catch (GeneralSecurityException | IOException e) {
            Log.e(TAG, "Error initializing EncryptedSharedPreferences, clearing and retrying", e);
            // Delete the corrupted file or keyset
            context.deleteSharedPreferences(PREF_FILE);
            try {
                tempPrefs = createEncryptedPrefs(context);
            } catch (GeneralSecurityException | IOException ex) {
                Log.e(TAG, "Critical failure initializing EncryptedSharedPreferences", ex);
                throw new IllegalStateException("Unable to initialize secure token storage", ex);
            }
        }
        this.prefs = tempPrefs;
    }

    private SharedPreferences createEncryptedPrefs(Context context) throws GeneralSecurityException, IOException {
        MasterKey masterKey = new MasterKey.Builder(context)
                .setKeyScheme(MasterKey.KeyScheme.AES256_GCM)
                .build();
        return EncryptedSharedPreferences.create(
                context,
                PREF_FILE,
                masterKey,
                EncryptedSharedPreferences.PrefKeyEncryptionScheme.AES256_SIV,
                EncryptedSharedPreferences.PrefValueEncryptionScheme.AES256_GCM
        );
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
