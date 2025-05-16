<p align="center"><img src="/img/logo_text.svg" alt="Smithereen"/></p>

Federated, ActivityPub-compatible social network with friends, walls, and groups.

If you have any questions or feedback, there's a [Telegram chat](https://t.me/SmithereenProject) you can join.

[![Crowdin](https://badges.crowdin.net/smithereen/localized.svg)](https://crowdin.com/project/smithereen)

## Building/installation

### Running directly on your server

1. Install and configure MySQL
2. Install maven and JDK >=21 if you don't have it already
3. Build the jar by running `mvn package -DskipTests=true` and place the one with dependencies at `/opt/smithereen/smithereen.jar`
4. Set up the image processing native library ([libvips](https://github.com/libvips/libvips)): run `java LibVipsDownloader.java` to automatically download a prebuilt one from [here](https://github.com/lovell/sharp-libvips). If you already have libvips installed on your system, you may skip this step, but be aware that not all libvips builds include all the features Smithereen needs.
5. Install and configure [imgproxy](https://docs.imgproxy.net/GETTING_STARTED)
6. Fill in the config file, see a commented example [here](examples/config.properties)
	- You can use either the local file system (default) or an S3-compatible object storage service for user-uploaded media files.
7. Create a new MySQL database and initialize it with the [schema](schema.sql) using a command (`mysql -p smithereen < schema.sql`) or any GUI like phpMyAdmin
8. Configure and start the daemon: assuming your distribution uses systemd, copy [the service file](examples/smithereen.service) to /etc/systemd/system, then run `systemctl daemon-reload` and `service smithereen start`
9. Run `java -jar /opt/smithereen/smithereen.jar /etc/smithereen/config.properties init_admin` to create the first account
10. Log into that account from your web browser, then configure the rest of the server settings from its UI

### Using Docker

Copy [Docker-specific config example](examples/config_docker.properties) to the project root directory as `config.properties` and edit it to set your domain. Also edit `docker-compose.yml` to add your imgproxy secrets. You can then use `docker-compose` to run Smithereen, MySQL, and imgproxy. You still need to [configure your web server to reverse proxy the port 4567](examples/nginx.conf). Create the first account by running `docker-compose exec web bash -c ./smithereen-init-admin`.

### Using S3 object storage

Smithereen supports S3-compatible object storage for user-uploaded media files (but not media file cache for files downloaded from other servers).

To enable S3 storage, set `upload.backend=s3` in your `config.properties`. Configure other properties depending on your cloud provider:
- `upload.s3.region`: the region to use, `us-east-1` by default. Required for AWS, but some other cloud providers accept arbitrary values here.
- `upload.s3.endpoint`: the S3 endpoint, `s3.<region>.amazonaws.com` by default. Required if **not** using AWS.
- `upload.s3.key_id` and `upload.s3.secret_key`: your credentials for request authentication.
- `upload.s3.bucket`: the name of your bucket.
- `upload.s3.override_path_style`: if `upload.s3.endpoint` is set, set this to `true` if your cloud provider requires 
putting the bucket name into the hostname instead of in the path for API requests, like `<bucket>.<endpoint>`.
- `upload.s3.protocol`: `https` by default, can be set to `http`.

The following properties control the public URLs for clients to read the files from your S3 bucket. These are used for imgproxy and given out to clients directly when they click "Open original" in the image viewer, and will be used for non-image (e.g. video) attachments in a future Smithereen version:
- `upload.s3.hostname`: defaults to `s3-<region>.amazonaws.com`. Needs to be set if not using AWS and `upload.s3.alias_host` is not set.
- `upload.s3.alias_host`: can be used instead of `upload.s3.hostname` if you don't want your bucket name to be visible. Requires that you have a CDN or a reverse proxy in front of the storage provider.
  - If this is set, the bucket name is **not** included in the generated URLs. The URLs will have the form of `<protocol>://<alias_host>/<object>`.
  - If this is **not** set, the generated URLs will be of the form `<protocol>://<hostname>/<bucket>/<object>`.

You will need to configure your bucket to allow anonymous read access to objects, but not allow directory listing. [Refer to Mastodon documentation on how to do this on different cloud providers.](https://docs.joinmastodon.org/admin/optional/object-storage/#minio)

You will also need to configure imgproxy to allow it to access your S3 storage:
```
IMGPROXY_ALLOWED_SOURCES=local://,https://<your S3 hostname or alias host>/
```
[Make sure to include a trailing slash in the URL.](https://docs.imgproxy.net/configuration/options#IMGPROXY_ALLOWED_SOURCES)

## Contributing

If you would like to help translate Smithereen into your language, please [do so on Crowdin](https://crowdin.com/project/smithereen). If your language isn't listed there, please ask to have it added.

If you would like to add a feature, please ask first before starting to work on it! This project is mostly "open-source but not open-contribution" because I (@grishka) have a rather specific vision of what it should be — an **old-school social network** that one primarily uses to stay up to date on the lives of people they know IRL and get to know these people better. An important part of that vision is to limit people's exposure to out-of-network content (created by those they don't have any connections with) as much as possible. The intended way to explore the network is by going along the social graph or (coming in the future) search for people. In particular, the following features will definitely **not** be added:

- **Hashtags.** The whole idea behind them is to be able to easily see what *people you don't know* post about some topics or global events. They thus serve as an easy gateway to out-of-network content. They encourage the creation of content that is more appealing to the global audience rather than one's followers. Account-scoped hashtags (`#tag@username`) may be added in the future though.
- **Public feeds** (local and federated). For the same reason, but also so that Smithereen servers don't become communities. They should just be the infrastructure that the network runs on.
- **Global full-text post search.** Same as hashtags.
- **Public pages** and **posting on behalf of a group or an event.** Logo accounts are bad. Logos shouldn't say things. Their existence erodes the humanness of the whole experience because you get these "higher beings" that aren't human and don't *feel* human. The intentional lack of easily obtainable logo accounts (no one is stopping one from creating a fake user profile for an organization, of course) serves to encourage authentic human interactions.
- **Custom emojis.** Many fediverse servers have these, but very few people use them in practice. And when they do, it almost always looks too colorful and distracting. Especially when they're animated.
- **Non-quote reposts**, like on Mastodon. Smithereen fully supports *displaying* these in order to be compatible with the majority of fediverse software, but the inability to *create* them is intentional. Separate comment threads for each repost reduce user confusion, invite more meaningful conversations and simplify the synchronization of these comments between servers. Combined with Smithereen's privacy settings and the ability of the post author to delete any comments on their posts, this completely eliminates all the abuse potential that, for example, quote tweets on Twitter present.
- **Animated profile pictures** as well as those with alpha transparency. Many fediverse servers allow these, but again, Smithereen is explicitly for authentic human interactions. So, it is built with the expectation that people will use their own photo, taken with a camera, as their profile picture, just like on Facebook or VKontakte.

If you disagree, feel free to fork this project, but please change the name if you do.

## Federating with Smithereen

Smithereen supports various features not found in most other ActivityPub server software. [See the federation document](/FEDERATION.md) if you would like to implement these ActivityPub extensions in your project.
