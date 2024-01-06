package smithereen.model;

import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.time.Instant;

public record UserBanInfo(@NotNull Instant bannedAt, @Nullable Instant expiresAt, @Nullable String message, boolean requirePasswordChange, int moderatorID, int reportID){
}
