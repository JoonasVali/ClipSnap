package com.github.joonasvali.bookreaderai.util;

/**
 * Utility class for model-related operations and family detection.
 * This class provides methods to determine model families and capabilities.
 */
public class ModelUtils {
    
    /**
     * Checks if the given model belongs to the GPT-5 family.
     * This method is designed to be easily extensible for future GPT-5 variants.
     * 
     * @param modelName the model name to check
     * @return true if the model belongs to the GPT-5 family, false otherwise
     */
    public static boolean isGPT5Family(String modelName) {
        if (modelName == null) {
            return false;
        }
        
        // Check for exact matches and future variants
        return modelName.equals("GPT-5") || 
               modelName.startsWith("GPT-5-") ||  // For future variants like GPT-5-turbo, GPT-5-vision, etc.
               modelName.equals("gpt-5") ||      // API model name
               modelName.startsWith("gpt-5-");   // API model variants
    }
    
    /**
     * Checks if the given model belongs to the GPT-4 family.
     * 
     * @param modelName the model name to check
     * @return true if the model belongs to the GPT-4 family, false otherwise
     */
    public static boolean isGPT4Family(String modelName) {
        if (modelName == null) {
            return false;
        }
        
        return modelName.equals("GPT-4o") || 
               modelName.startsWith("GPT-4") ||
               modelName.equals("chatgpt-4o-latest") ||
               modelName.startsWith("gpt-4");
    }
    
    /**
     * Checks if the given model requires whole image processing (no slicing or scaling).
     * Currently, this includes GPT-5 family models, but can be extended for other models
     * that have similar capabilities.
     * 
     * @param modelName the model name to check
     * @return true if the model should process whole images, false otherwise
     */
    public static boolean requiresWholeImageProcessing(String modelName) {
        return isGPT5Family(modelName);
    }
    
    /**
     * Checks if the given model supports multiple samples (n > 1).
     * Some models like GPT-5 may only support n=1.
     * 
     * @param modelName the model name to check
     * @return true if the model supports multiple samples, false otherwise
     */
    public static boolean supportsMultipleSamples(String modelName) {
        return !isGPT5Family(modelName);
    }
}
