package com.lofo.serenia.service.token;

import com.lofo.serenia.config.SereniaConfig;
import com.lofo.serenia.domain.user.User;
import com.lofo.serenia.domain.user.UserTokenQuota;
import com.lofo.serenia.domain.user.UserTokenUsage;
import com.lofo.serenia.repository.UserTokenQuotaRepository;
import com.lofo.serenia.repository.UserTokenUsageRepository;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import jakarta.transaction.Transactional;
import jakarta.ws.rs.WebApplicationException;
import jakarta.ws.rs.core.Response;
import org.jboss.logging.Logger;

import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

@ApplicationScoped
public class TokenUsageService {

    private static final Logger LOG = Logger.getLogger(TokenUsageService.class);

    private final UserTokenQuotaRepository tokenQuotaRepository;
    private final UserTokenUsageRepository tokenUsageRepository;
    private final SereniaConfig sereniaConfig;

    @Inject
    public TokenUsageService(
            UserTokenQuotaRepository tokenQuotaRepository,
            UserTokenUsageRepository tokenUsageRepository, SereniaConfig sereniaConfig) {
        this.tokenQuotaRepository = tokenQuotaRepository;
        this.tokenUsageRepository = tokenUsageRepository;
        this.sereniaConfig = sereniaConfig;
    }

    @Transactional
    public void initializeUserTokenQuota(User user) {
        LOG.infof("Initializing token quota for user: %s", user.getId());

        UserTokenQuota quota = UserTokenQuota.builder()
                .user(user)
                .inputTokensLimit(sereniaConfig.defaultInputTokensLimit())
                .outputTokensLimit(sereniaConfig.defaultOutputTokensLimit())
                .totalTokensLimit(sereniaConfig.defaultTotalTokensLimit())
                .inputTokensUsed(0L)
                .outputTokensUsed(0L)
                .totalTokensUsed(0L)
                .createdAt(LocalDateTime.now())
                .updatedAt(LocalDateTime.now())
                .build();

        tokenQuotaRepository.persist(quota);
        LOG.infof("Token quota initialized for user: %s", user.getId());
    }

    @Transactional
    public void recordTokenUsage(User user, long inputTokens, long outputTokens, String usageType) {
        LOG.infof("Recording token usage for user %s: input=%d, output=%d, type=%s",
                user.getId(), inputTokens, outputTokens, usageType);

        UserTokenUsage usage = UserTokenUsage.builder()
                .user(user)
                .inputTokens(inputTokens)
                .outputTokens(outputTokens)
                .totalTokens(inputTokens + outputTokens)
                .usageType(usageType)
                .createdAt(LocalDateTime.now())
                .build();

        tokenUsageRepository.persist(usage);
    }

    @Transactional
    public void consumeTokens(UUID userId, long inputTokens, long outputTokens, String usageType) {
        LOG.infof("Consuming tokens for user %s: input=%d, output=%d", userId, inputTokens, outputTokens);

        UserTokenQuota quota = tokenQuotaRepository.findByUserId(userId)
                .orElseThrow(() -> new WebApplicationException(
                        "User token quota not found",
                        Response.Status.NOT_FOUND));

        if (!quota.canConsumeTokens(inputTokens, outputTokens)) {
            LOG.warnf("Token limit exceeded for user %s", userId);
            throw new WebApplicationException(
                    "Token quota exceeded. Remaining - Input: %d, Output: %d, Total: %d"
                            .formatted(quota.getRemainingInputTokens(),
                                      quota.getRemainingOutputTokens(),
                                      quota.getRemainingTotalTokens()),
                    Response.Status.PAYMENT_REQUIRED);
        }

        quota.addInputTokens(inputTokens);
        quota.addOutputTokens(outputTokens);
        tokenQuotaRepository.persist(quota);

        recordTokenUsage(quota.getUser(), inputTokens, outputTokens, usageType);
        LOG.infof("Tokens consumed for user %s - Total used: %d", userId, quota.getTotalTokensUsed());
    }

    public UserTokenQuota getUserTokenQuota(UUID userId) {
        return tokenQuotaRepository.findByUserId(userId)
                .orElseThrow(() -> new WebApplicationException(
                        "User token quota not found",
                        Response.Status.NOT_FOUND));
    }

    public List<UserTokenUsage> getUserTokenUsageByDateRange(UUID userId, LocalDateTime startDate, LocalDateTime endDate) {
        return tokenUsageRepository.findByUserIdAndDateRange(userId, startDate, endDate);
    }


    public long getTotalTokensUsedInPeriod(UUID userId, LocalDateTime startDate, LocalDateTime endDate) {
        List<UserTokenUsage> usages = getUserTokenUsageByDateRange(userId, startDate, endDate);
        return usages.stream()
                .mapToLong(UserTokenUsage::getTotalTokens)
                .sum();
    }
}
