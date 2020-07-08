# Smithereen

Federated, ActivityPub-compatible social network with friends, walls, and (*at some point in the future*) groups.

**At the moment, this is very far from being production-ready. Things may and likely will break.**

## Building/installation

### Running directly on your server

1. Install and configure MySQL
2. Install maven if you don't have it already
3. Build the jar by running `mvn package` and place the one with dependencies at `/opt/smithereen/smithereen.jar`
4. Set up the native library ([libvips](https://github.com/libvips/libvips) and JNI bindings): pick a binary for your OS and CPU from [prebuilt ones](jniPrebuilt) or build your own
5. Fill in the config file, see a commented example [here](examples/config.properties)
6. Create a new MySQL database and initialize it with the [schema](schema.sql) using a command (`mysql -p smithereen < schema.sql`) or any GUI like phpMyAdmin
7. Run `java -jar /opt/smithereen/smithereen.jar /etc/smithereen/config.properties init_admin` to create the first account
8. Log into that account from your web browser, then configure the rest of the server settings from its UI

### Using Docker

Copy [Docker-specific config example](examples/config_docker.properties) to the project root directory as `config.properties` and edit it to set your domain. You can then use `docker-compose` to run both Smithereen an MySQL. You still need to [configure your web server to reverse proxy the port 4567](examples/nginx.conf). Create the first account by running `docker exec -it smithereen_web_1 bash -c ./smithereen-init-admin`.
