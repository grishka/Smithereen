package smithereen.model.viewmodel;

import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.net.URI;

public record AudioAttachmentViewModel(@NotNull String audioID, @NotNull String formattedDuration, long durationSeconds, @NotNull String artist, @NotNull String title, @Nullable URI url){
}
