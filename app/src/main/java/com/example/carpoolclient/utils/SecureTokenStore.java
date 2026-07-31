package com.example.carpoolclient.utils;

import android.content.Context;
import android.content.SharedPreferences;
import android.util.Log;

import androidx.security.crypto.EncryptedSharedPreferences;
import androidx.security.crypto.MasterKey;

import java.io.IOException;
import java.security.GeneralSecurityException;

/**
 * A secure storage utility for sensitive tokens such as JWT and FCM tokens.
 * It uses {@link EncryptedSharedPreferences} to ensure that data is encrypted at rest.
 * This class follows the Singleton pattern to provide a single point of access to the encrypted storage.
 */
public final class SecureTokenStore {
    private static final String TAG = "SecureTokenStore";
    private static final String PREF_FILE = "secure_tokens";
    private static final String KEY_JWT = "jwt_token";
    private static final String KEY_FCM = "fcm_token";
    private static final String KEY_FULL_NAME = "full_name";
    private static SecureTokenStore instance;
    private final SharedPreferences prefs;

    /**
     * Initializes the SecureTokenStore with EncryptedSharedPreferences.
     * If initialization fails due to corrupted keys or files, it attempts to clear the
     * storage and retry once.
     *
     * @param context The application context.
     * @throws IllegalStateException If initialization fails critically even after a retry.
     */
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

    /**
     * Creates an instance of {@link EncryptedSharedPreferences} using a {@link MasterKey}.
     *
     * @param context The context used to build the master key and shared preferences.
     * @return A configured SharedPreferences instance with encryption enabled.
     * @throws GeneralSecurityException If there is a security-related error.
     * @throws IOException If there is an I/O error while accessing the preferences file.
     */
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

    /**
     * Returns the singleton instance of SecureTokenStore.
     *
     * @param context The context used to initialize the store if it hasn't been created yet.
     * @return The singleton instance of {@link SecureTokenStore}.
     */
    public static synchronized SecureTokenStore getInstance(Context context) {
        if (instance == null) {
            instance = new SecureTokenStore(context.getApplicationContext());
        }
        return instance;
    }

    /**
     * Securely saves the JWT token to encrypted storage.
     *
     * @param token The JWT token to be stored.
     */
    public void saveJwtToken(String token) {
        prefs.edit().putString(KEY_JWT, token).apply();
    }

    /**
     * Retrieves the stored JWT token.
     *
     * @return The JWT token string, or {@code null} if no token is found.
     */
    public String getJwtToken() {
        return prefs.getString(KEY_JWT, null);
    }

    /**
     * Securely saves the FCM (Firebase Cloud Messaging) token to encrypted storage.
     *
     * @param token The FCM token to be stored.
     */
    public void saveFcmToken(String token) {
        prefs.edit().putString(KEY_FCM, token).apply();
    }

    /**
     * Retrieves the stored FCM token.
     *
     * @return The FCM token string, or {@code null} if no token is found.
     */
    public String getFcmToken() {
        return prefs.getString(KEY_FCM, null);
    }

    /**
     * Securely saves the user's full name to encrypted storage.
     *
     * @param name The full name to be stored.
     */
    public void saveFullName(String name) {
        prefs.edit().putString(KEY_FULL_NAME, name).apply();
    }

    /**
     * Retrieves the stored full name.
     *
     * @return The full name string, or {@code null} if no name is found.
     */
    public String getFullName() {
        return prefs.getString(KEY_FULL_NAME, null);
    }
}
