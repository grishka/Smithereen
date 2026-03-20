package smithereen.text;

import org.jetbrains.annotations.NotNull;

public record FormattedTextSource(@NotNull String source, @NotNull FormattedTextFormat format){
}
