# Smithereen

[![Crowdin](https://badges.crowdin.net/smithereen/localized.svg)](https://crowdin.com/project/smithereen)

Federated, ActivityPub-compatible social network with friends, walls, and groups.

If you have any questions or feedback, there's a [Telegram chat](https://t.me/SmithereenProject) you can join.

## Building/installation

### Running directly on your server

1. Install and configure MySQL
2. Install maven and JDK >=21 if you don't have it already
3. Build the jar by running `mvn package -DskipTests=true` and place the one with dependencies at `/opt/smithereen/smithereen.jar`
4. Set up the image processing native library ([libvips](https://github.com/libvips/libvips)): run `java LibVipsDownloader.java` to automatically download a prebuilt one from [here](https://github.com/lovell/sharp-libvips). If you already have libvips installed on your system, you may skip this step, but be aware that not all libvips builds include all the features Smithereen needs.
5. Install and configure [imgproxy](https://docs.imgproxy.net/GETTING_STARTED)
6. Fill in the config file, see a commented example [here](examples/config.properties)
7. Create a new MySQL database and initialize it with the [schema](schema.sql) using a command (`mysql -p smithereen < schema.sql`) or any GUI like phpMyAdmin
8. Configure and start the daemon: assuming your distribution uses systemd, copy [the service file](examples/smithereen.service) to /etc/systemd/system, then run `systemctl daemon-reload` and `service smithereen start`
9. Run `java -jar /opt/smithereen/smithereen.jar /etc/smithereen/config.properties init_admin` to create the first account
10. Log into that account from your web browser, then configure the rest of the server settings from its UI

### Using Docker

Copy [Docker-specific config example](examples/config_docker.properties) to the project root directory as `config.properties` and edit it to set your domain. Also edit `docker-compose.yml` to add your imgproxy secrets. You can then use `docker-compose` to run Smithereen, MySQL, and imgproxy. You still need to [configure your web server to reverse proxy the port 4567](examples/nginx.conf). Create the first account by running `docker-compose exec web bash -c ./smithereen-init-admin`.

## Contributing

If you would like to help translate Smithereen into your language, please [do so on Crowdin](https://crowdin.com/project/smithereen).
