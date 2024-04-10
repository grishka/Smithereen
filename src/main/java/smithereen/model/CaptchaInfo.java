package smithereen.model;

import java.time.Instant;

public record CaptchaInfo(String answer, Instant generatedAt){
}
