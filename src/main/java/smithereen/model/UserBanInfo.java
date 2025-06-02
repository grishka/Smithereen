package smithereen.model;

import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.time.Instant;

public record UserBanInfo(@NotNull Instant bannedAt, @Nullable Instant expiresAt, @Nullable String message, boolean requirePasswordChange, int moderatorID, int reportID, boolean suspendedOnRemoteServer){
	/**
	 * The number of days after a user's ban when their account is deleted automatically.
	 */
	public static int ACCOUNT_DELETION_DAYS=30;

	public UserBanInfo withRemoteSuspensionStatus(boolean suspended){
		return new UserBanInfo(bannedAt, expiresAt, message, requirePasswordChange, moderatorID, reportID, suspended);
	}
}
