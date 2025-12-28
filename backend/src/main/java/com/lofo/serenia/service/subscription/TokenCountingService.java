package com.lofo.serenia.service.subscription;
import jakarta.enterprise.context.ApplicationScoped;
/**
 * Service de calcul des tokens consommés.
 * MVP : Utilise strlen (longueur de chaîne) comme approximation.
 */
@ApplicationScoped
public class TokenCountingService {

    /**
     * Calcule le nombre de tokens pour un texte.
     * MVP : 1 caractère = 1 token (approximation grossière mais simple).
     */
    public int countTokens(String content) {
        if (content == null) {
            return 0;
        }
        return content.length();
    }

    /**
     * Calcule le total de tokens pour un échange (entrée + sortie).
     */
    public int countExchangeTokens(String userMessage, String assistantResponse) {
        return countTokens(userMessage) + countTokens(assistantResponse);
    }
}
