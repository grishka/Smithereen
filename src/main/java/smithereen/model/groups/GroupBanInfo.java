package smithereen.model.groups;

import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.time.Instant;

public record GroupBanInfo(@NotNull Instant bannedAt, @Nullable String message, int moderatorID, int reportID, boolean suspendedOnRemoteServer){
	public static int GROUP_DELETION_DAYS=30;

	public GroupBanInfo withRemoteSuspensionStatus(boolean suspended){
		return new GroupBanInfo(bannedAt, message, moderatorID, reportID, suspended);
	}
}
