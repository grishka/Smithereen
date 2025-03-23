package smithereen.model.fasp.responses;

import java.net.URI;

public record FASPRegistrationResponse(String faspId, String publicKey, URI registrationCompletionUri){
}
