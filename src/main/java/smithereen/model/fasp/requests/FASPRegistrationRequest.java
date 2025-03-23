package smithereen.model.fasp.requests;

import java.net.URI;

import smithereen.util.validation.AllFieldsAreRequired;

@AllFieldsAreRequired
public record FASPRegistrationRequest(String name, URI baseUrl, String serverId, String publicKey){
}
