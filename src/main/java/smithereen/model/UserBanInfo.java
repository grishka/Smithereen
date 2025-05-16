package smithereen.model;

import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.time.Instant;

public record UserBanInfo(@NotNull Instant bannedAt, @Nullable Instant expiresAt, @Nullable String message, boolean requirePasswordChange, int moderatorID, int reportID){
	/**
	 * The number of days after a user's ban when their account is deleted automatically.
	 */
	public static int ACCOUNT_DELETION_DAYS=30;
}
