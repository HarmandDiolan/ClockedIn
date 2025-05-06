package com.example.clockedin.utils;

import android.util.Base64;
import android.util.Log;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.security.SecureRandom;

/**
 * Utility class for handling password encryption and verification
 */
public class PasswordUtils {
    private static final String TAG = "PasswordUtils";
    private static final String HASH_ALGORITHM = "SHA-256";
    private static final int SALT_LENGTH = 16; // bytes
    
    /**
     * Generates a secure random salt
     * @return salt as byte array
     */
    public static byte[] generateSalt() {
        SecureRandom random = new SecureRandom();
        byte[] salt = new byte[SALT_LENGTH];
        random.nextBytes(salt);
        return salt;
    }
    
    /**
     * Encrypts a password with salt using SHA-256
     * @param password The plain text password
     * @param salt The salt to use
     * @return The hashed password
     */
    public static byte[] hashPassword(String password, byte[] salt) {
        try {
            MessageDigest digest = MessageDigest.getInstance(HASH_ALGORITHM);
            digest.reset();
            digest.update(salt);
            return digest.digest(password.getBytes(StandardCharsets.UTF_8));
        } catch (NoSuchAlgorithmException e) {
            Log.e(TAG, "Error hashing password: " + e.getMessage(), e);
            throw new RuntimeException("Error hashing password", e);
        }
    }
    
    /**
     * Creates a password entry for the database by combining salt and hashed password
     * @param password Plain text password
     * @return Base64 encoded string containing salt and hashed password
     */
    public static String encryptPassword(String password) {
        byte[] salt = generateSalt();
        byte[] hashedPassword = hashPassword(password, salt);
        
        // Combine salt and password hash into one entry (salt:hash)
        byte[] combined = new byte[salt.length + hashedPassword.length];
        System.arraycopy(salt, 0, combined, 0, salt.length);
        System.arraycopy(hashedPassword, 0, combined, salt.length, hashedPassword.length);
        
        // Convert to Base64 string for storage
        return Base64.encodeToString(combined, Base64.NO_WRAP);
    }
    
    /**
     * Verifies a plain text password against a stored encrypted password
     * @param plainTextPassword Plain text password to verify
     * @param storedPassword Encrypted password from the database
     * @return true if password matches, false otherwise
     */
    public static boolean verifyPassword(String plainTextPassword, String storedPassword) {
        try {
            // Decode the stored password
            byte[] combined = Base64.decode(storedPassword, Base64.NO_WRAP);
            
            // Extract salt (first SALT_LENGTH bytes)
            byte[] salt = new byte[SALT_LENGTH];
            System.arraycopy(combined, 0, salt, 0, salt.length);
            
            // Hash the input password with the same salt
            byte[] hashedPassword = hashPassword(plainTextPassword, salt);
            
            // Compare the stored hash with the generated hash
            int hashOffset = salt.length;
            for (int i = 0; i < hashedPassword.length; i++) {
                if (combined[i + hashOffset] != hashedPassword[i]) {
                    return false;
                }
            }
            return true;
        } catch (Exception e) {
            Log.e(TAG, "Error verifying password: " + e.getMessage(), e);
            return false;
        }
    }
} 