# Smithereen

Federated, ActivityPub-compatible social network with friends, walls, and (*at some point in the future*) groups.

**At the moment, this is very far from being production-ready. Things may and likely will break.**

## Building/installation

### Running directly on your server

1. Install and configure MySQL
2. Install maven if you don't have it already
3. Build the jar by running `mvn package`
4. Set up the native library ([libvips](https://github.com/libvips/libvips) and JNI bindings): pick a binary for your OS and CPU from [prebuilt ones](jniPrebuilt) or build your own
5. Fill in the config file, see a commented example [here](examples/config.properties)
6. Create a new MySQL database and initialize it with the [schema](schema.sql) using a command (`mysql -p smithereen < schema.sql`) or any GUI like phpMyAdmin
7. Create an invite code for the first user using this SQL query: `INSERT INTO signup_invitations (signups_remaining) VALUES (1)`
8. (if running on a server) set up a service in your OS to run Smithereen as a daemon (the jar expects one command-line argument — path to the config file, e.g. `java -jar smithereen.jar /etc/smithereen/config.properties`)
9. (if running on a server) set up your web server to proxy everything except `/s/` and `/.well-known/acme-challenge` to 127.0.0.1:4567 or whichever port you set in the config, [example for nginx](examples/nginx.conf)
10. Navigate to either localhost:4567 (if developing) or your server address and register your account, using 00000000000000000000000000000000 (32 zeros) as your invite code
11. Optionally, change `access_level` to 3 in the `accounts` table to grant yourself admin access

### Using Docker

Copy [Docker-specific config example](examples/config_docker.properties) to the project root directory as `config.properties` and edit it to set your domain. You can then use `docker-compose` to run both Smithereen an MySQL. You still need to [configure your web server to reverse proxy the port 4567](examples/nginx.conf).
