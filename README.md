# Smithereen

Federated, ActivityPub-compatible social network with friends, walls, and (*at some point in the future*) groups.

**At the moment, this is very far from being production-ready. Things may and likely will break.**

## Building/installation

This will be easier when (if?) the project reaches a usable state.

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

## Docker image

Smithereen can be built as Docker image using Dockerfile provided by this repository. Dockerfile handles building JNI libraries as well. You can use it with Docker Compose for easy deployments. Example `docker-compose.yml` file:

```yaml
version: '3'

services:
  web:
    build: .
    ports:
      # Container port is hard-coded and cannot be changed
      # without modifying Dockerfile.
      # host_port:container_port
      - '4567:4567'
    volumes:
      # config.properties path is hard-coded and cannot be
      # changed without modifying Dockerfile (see below).
      # host_path:container_path
      - ./config.properties:/usr/local/etc/config.properties
      - ./cache:/var/cache/smithereen/cache
      - ./media_cache:/var/cache/smithereen/media
    restart: always
  mysql:
    # MySQL 5 is required because schema.sql dump
    # isn't compatible with new versions of MySQL
    image: mysql:5
    environment:
      # You can use $ENVIRONMENT_VARS with .env
      MYSQL_ROOT_PASSWORD: smithereen
      MYSQL_DATABASE: smithereen
      # If you are afraid of using root account, you can
      # use variables before to create new superuser
      MYSQL_USER: $DOCKER_MYSQL_USER
      MYSQL_PASSWORD: $DOCKER_MYSQL_PASSWORD
    volumes:
      # Here we mount Docker volume to MySQL DB path
      # so they will be saved between restarts
      - mysql-data:/var/lib/mysql
      # docker-entrypoint.sh automatically imports SQLs
      # from /docker-entrypoint-initdb.d directory
      # to $MYSQL_DATABASE database (created automatically)
      - ./schema.sql:/docker-entrypoint-initdb.d/schema.sql
    restart: always

volumes:
  # MySQL persistence
  mysql-data:
```

`config.properties`:

```properties
...
# This is required to properly expose container port to host machine!
server.ip=0.0.0.0

# Make sure that these paths are the same in docker-compose.yml too
upload.path=/var/cache/smithereen/cache
media_cache.path=/var/cache/smithereen/media
...
```

You can skip steps 2, 3, 4, 8 in installation instructions if you use Dockerfile. Once you deploy it, you can install Nginx to your host machine (or as Docker container too) and configure it to proxy requests to `127.0.0.1:4567` like [here](examples/nginx.conf).
