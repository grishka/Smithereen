#!/bin/bash

function failWithError {
	echo -e "\033[1;31mError:\033[0m $1" >&2
	exit 1
}

cd $(dirname $0)

echo ""
echo -e " \033[38;5;64m▄██████████████▄"
echo -e " \033[38;5;64m███████\033[48;5;231m  \033[0;38;5;64m███████\033[0m     ▄▄▄ ▖           ▄▄    ▗▄▄                                 "
echo -e " \033[38;5;64m███████\033[48;5;231m  \033[0;38;5;64m███████\033[0m    ▟█  ▀▌           ▀▀ ▗▟  ██                                 "
echo -e " \033[38;5;64m███████\033[48;5;231m  \033[0;38;5;64m███████\033[0m    ▀██▄  ▜█▙▀█▙▀▜█▖▝██▝██▀▘██▞▜█▖▗█▀▜▖▝██▞▜▌▗█▀▜▖ ▟▛▀▙ ▜█▙▀█▙ "
echo -e " \033[38;5;64m███████\033[48;5;231m▀▀\033[0;38;5;64m███████\033[0m      ▝▜█▄▐█▌ ██ ▐█▌ ██ ██  ██ ▐█▌██▄▟█ ██   ██▄▟█▐█▙▄█▌▐█▌ ██ "
echo -e " \033[38;5;64m███████\033[48;5;231m▄▄\033[0;38;5;64m███████\033[0m    ▙▖  █▛▐█▌ ██ ▐█▌ ██ ██▗ ██ ▐█▌▜█▖ ▗ ██   ▜█▖ ▗▝█▙  ▖▐█▌ ██ "
echo -e " \033[38;5;64m ▀▀▀▀▀████▀▀▀▀▀ \033[0m    ▘▝▀▀▀ ▀▀▀▝▀▀▘▀▀▀▝▀▀▘▝▀▘▝▀▀▘▀▀▀ ▀▀▀▘▝▀▀▘   ▀▀▀▘ ▝▀▀▀ ▀▀▀▝▀▀▘"
echo -e " \033[38;5;64m       ▀▀       \033[0m"

if [ "$(uname)" != "Linux" ]; then
	failWithError "This installer script only supports Linux."
fi

arch="$(uname -m)"

if [ "$arch" != "__arch__" ]; then
	failWithError "This bundle is built for __arch__, but your machine is $arch. Please download the correct bundle for your CPU architecture or follow the steps to install Smithereen manually."
fi

if [ "$EUID" -ne 0 ]; then
	echo "Some steps of this installation require root privileges, but this script isn't running as root. Re-running with sudo..."
	sudo $0
	exit
fi

if ! [[ -d /run/systemd/system ]]; then
	failWithError "Your system is not running systemd. As this installer relies on systemd services to run daemons, it's of no use on your system. You will have to install Smithereen manually."
fi

echo ""
echo "Checking for Java..."
if ! [ -x "$(command -v java)" ]; then
	failWithError "You don't have Java installed, or it isn't on your PATH.
Install JRE 21 or newer from one of these providers:
- Eclipse Adoptium: https://adoptium.net/temurin/releases/
- Azul: https://www.azul.com/downloads/
- Amazon Corretto: https://aws.amazon.com/corretto/"
fi

javaVersion="$((java --version 2>&1) | head -n 1 | cut -d " " -f 2)"
echo "Found Java $javaVersion."
javaMajorVersion="$(echo "$javaVersion" | cut -d "." -f 1)"
if [ "$javaMajorVersion" -lt "21" ]; then
	failWithError "Java 21 or newer is required."
fi

mysqlHelpUrl="https://dev.mysql.com/doc/refman/8.4/en/linux-installation.html"
echo "Checking for MySQL..."
if ! [ -x "$(command -v mysql)" ]; then
	failWithError "You don't have MySQL installed. See $mysqlHelpUrl for installation instructions.
If you would like to run the database on a separate server, you will have to install Smithereen manually."
fi
mysqlVersion="$(mysql --version | tr -s " ")"
if [ "$(echo $mysqlVersion | grep MariaDB | wc -l)" -gt "0" ]; then
	failWithError "You have MariaDB instead of MySQL. Unfortunately, some distribution maintainers think that it's a drop-in replacement, but MariaDB is known to be incompatible with Smithereen.
See $mysqlHelpUrl for instructions on how to install MySQL, or, if you need MariaDB for other applications on your server, consider running Smithereen in a container like Docker or LXC."
fi
if [ "$(echo $mysqlVersion | grep Distrib | wc -l)" -gt "0" ]; then
	mysqlActualVersion="$(echo $mysqlVersion | cut -d " " -f 5)"
else
	mysqlActualVersion="$(echo $mysqlVersion | cut -d " " -f 3)"
fi
if [ "$(echo $mysqlActualVersion | cut -d "." -f 1)" -lt "8" ]; then
	failWithError "Your version of MySQL, $mysqlActualVersion, is too old to work with Smithereen. Please update it. See $mysqlHelpUrl for installation instructions."
fi
echo "Found MySQL $mysqlActualVersion."

echo ""
echo "Your server has all prerequisites installed. This script will now ask a few questions to configure your Smithereen installation."
echo ""

while :
do
	read -s -p "MySQL root password (empty if none): " mysqlPassword
	echo ""
	mysqlCommand="mysql -uroot"
	if ! [ -z "$mysqlPassword" ]; then
		export MYSQL_PWD=$mysqlPassword
	fi
	$mysqlCommand -e "SELECT 1;" > /dev/null
	mysqlRes=$?
	if [ $mysqlRes == 0 ]; then break; fi
	echo "Failed to connect to MySQL with this password. Try again. $mysqlRes"
done
while [ -z "$domain" ]; do read -p "Domain name your Smithereen server will use: " domain; done
read -p "Installation location [/opt/smithereen]: " installLocation
if [ -z "$installLocation" ]; then installLocation="/opt/smithereen"; fi

if [[ -d "$installLocation" ]]; then
	if [[ -f "$installLocation/smithereen.jar" ]]; then
		echo "There appears to be an existing installation of Smithereen in $installLocation."
		read -p "Press Enter to run the update script instead."
		./update.sh
		exit 0
	fi
fi

read -p "Database name and new MySQL user name [smithereen]: " dbName
if [ -z "$dbName" ]; then dbName="smithereen"; fi
read -p "Would you like to use an S3 cloud storage service for user-uploaded files (y/n)? [n]: " useS3
if [ "$useS3" == [yY] ]; then
	echo ""
	echo "Make sure you've read the readme and configured your storage service correctly: https://github.com/grishka/Smithereen/blob/master/README.md#using-s3-object-storage"
	echo ""
	read -p "Endpoint for your S3 storage provider - this is where API requests will be sent (leave empty if using Amazon Web Services): " s3Endpoint
	read -p "S3 region (some non-AWS cloud providers don't care): " s3Region
	while [ -z "$s3KeyID" ]; do read -p "S3 key ID: " s3KeyID; done
	while [ -z "$s3SecretKey" ]; do read -s -p "S3 secret key: " s3SecretKey; echo ""; done
	while [ -z "$s3Bucket" ]; do read -p "S3 bucket name: " s3Bucket; done
	if ! [ -z "$s3Endpoint" ]; then
		read -p "Does your storage provider require the bucket name to be put into the hostname instead of the path for API requests (y/n)? [n]: " s3OverridePathStyle
		while [ -z "$s3Hostname" ]; do read -p "Hostname for your S3 storage provider - this will be used to serve files to the outside world: " s3Hostname; done
	fi
	read -p "Location for cached files from remote servers, must be under the web server root directory [/var/www/smithereen]: " webRoot
else
	read -p "Location for user-uploaded files and cached files from remote servers, must be under the web server root directory [/var/www/smithereen]: " webRoot
fi
if [ -z "$webRoot" ]; then webRoot="/var/www/smithereen"; fi

echo ""
echo "This script will now:"
echo "- Copy the necessary files to $installLocation"
echo "- Create a new MySQL database '$dbName' and initialize it"
echo "- Create a MySQL user '$dbName' with a random password for that database"
echo "- Generate the configuration file for Smithereen"
echo "- Create the first Smithereen account that will be the server admin"
echo "- Install systemd services for Smithereen and imgproxy"
read -p "Press Enter to continue..."

echo ""
mkdir -p $installLocation || failWithError "Unable to create directory $installLocation"
mkdir -p $installLocation/nginx_cache/images
mkdir -p $webRoot
mkdir $webRoot/s
mkdir $webRoot/s/uploads
mkdir $webRoot/s/media_cache
chown -R www-data:www-data $webRoot
chown -R www-data:www-data $installLocation/nginx_cache/images
cp -v -R smithereen.jar lib libvips* imgproxy $installLocation

echo "Creating database..."
echo "CREATE DATABASE $dbName;" | $mysqlCommand > /dev/null || failWithError "Unable to create a MySQL database"
mysqlUserPassword="$(base64 < /dev/urandom | head -c 20)"
echo "Creating MySQL user..."
echo "CREATE USER '$dbName'@'localhost' IDENTIFIED BY '$mysqlUserPassword'; GRANT ALL ON $dbName.* TO '$dbName'@'localhost'; FLUSH PRIVILEGES;" | $mysqlCommand > /dev/null || failWithError "Unable to create a MySQL user and grant database permissions"
echo "Initializing database..."
$mysqlCommand -D$dbName < schema.sql > /dev/null || failWithError "Failed to load schema into the database"

echo "Generating config at $installLocation/config.properties..."
configContent="# Database settings
db.host=localhost
db.name=$dbName
db.user=$dbName
db.password=$mysqlUserPassword
# You can limit the number of concurrent database connections, default is 100
#db.max_connections=50

# The domain for your instance. Used for local object URIs in ActivityPub. If running on localhost, must include the port.
domain=$domain"
if [ "$useS3" == [yY] ]; then
	configContent="$configContent

# S3 storage configuration
upload.backend=s3"
	if ! [ -z "$s3Endpoint" ]; then
		configContent="$configContent
upload.s3.endpoint=$s3Endpoint"
	fi
	if ! [ -z "$s3Region" ]; then
		configContent="$configContent
upload.s3.region=$s3Region"
	fi
	configContent="$configContent
upload.s3.key_id=$s3KeyID
upload.s3.secret_key=$s3SecretKey
upload.s3.bucket=$s3Bucket"
	if ! [ -z "$s3Hostname" ]; then
		configContent="$configContent
upload.s3.hostname=$s3Hostname"
	fi
	if [ $s3OverridePathStyle == [yY] ]; then
		configContent="$configContent
upload.s3.override_path_style=true"
	fi
else
	configContent="$configContent

# Filesystem path where user-uploaded files (profile pictures, post media) are stored.
upload.path=$webRoot/s/uploads
# The URL path that corresponds to the above filesystem path.
upload.url_path=/s/uploads"
fi

configContent="$configContent

# Media cache temporarily stores files from other servers.
# Must be under your web server's web root.
media_cache.path=$webRoot/s/media_cache
# And its corresponding URL path.
media_cache.url_path=/s/media_cache
# The maximum size after which the media cache starts deleting oldest files.
# Integer number of bytes or any of K, M, G, T for the corresponding unit
media_cache.max_size=1G
# How big could a single file be before it is hotlinked instead of going through the media cache
media_cache.file_size_limit=50M

# The URL path prefix configured in imgproxy and nginx
imgproxy.url_prefix=/i
# Paths to media directories relative to IMGPROXY_LOCAL_FILESYSTEM_ROOT
imgproxy.local_uploads=/uploads
imgproxy.local_media_cache=/media_cache"

imgproxyKey=$(head -c 2048 /dev/random | shasum -a 256 | cut -d ' ' -f 1)
imgproxySalt=$(head -c 2048 /dev/random | shasum -a 256 | cut -d ' ' -f 1)

configContent="$configContent
imgproxy.key=$imgproxyKey
imgproxy.salt=$imgproxySalt"

echo "$configContent" > "$installLocation/config.properties"
echo "IMGPROXY_KEY=$imgproxyKey
IMGPROXY_SALT=$imgproxySalt
IMGPROXY_PATH_PREFIX=/i
IMGPROXY_LOCAL_FILESYSTEM_ROOT=$webRoot/s" > "$installLocation/imgproxy.env"
if [ "$useS3" == [yY] ]; then
	if [ -z "$s3Hostname" ]; then s3Hostname="s3-$s3Region.amazonaws.com"; fi
	echo "IMGPROXY_ALLOWED_SOURCES=local://,https://$s3Hostname/" >> "$installLocation/imgproxy.env"
else
	echo "IMGPROXY_ALLOWED_SOURCES=local://" >> "$installLocation/imgproxy.env"
fi

java --module-path $installLocation/smithereen.jar:$installLocation/lib -m smithereen.server/smithereen.SmithereenApplication $installLocation/config.properties init_admin || failWithError "Failed to initialize the admin user"

echo ""
echo "Installing systemd services..."
echo "[Unit]
Description=Smithereen social network server
After=network.target mysqld.service

[Service]
Type=simple
ExecStart=$(which java) -Djna.library.path=$installLocation --module-path $installLocation/smithereen.jar:$installLocation/lib -m smithereen.server/smithereen.SmithereenApplication $installLocation/config.properties
Restart=always
User=www-data
Group=www-data

[Install]
WantedBy=multi-user.target" > /etc/systemd/system/smithereen.service
echo "[Unit]
Description=imgproxy for Smithereen
After=network.target

[Service]
Type=simple
WorkingDirectory=$installLocation
ExecStart=$installLocation/imgproxy
EnvironmentFile=$installLocation/imgproxy.env
Restart=always
User=www-data
Group=www-data

[Install]
WantedBy=multi-user.target" > /etc/systemd/system/smithereen_imgproxy.service
systemctl daemon-reload
systemctl enable smithereen
systemctl enable smithereen_imgproxy
echo "Starting services..."
service smithereen start || failWithError "Failed to start the main service"
service smithereen_imgproxy start || failWithError "Failed to start the imgproxy service"

echo ""
echo "Installation is almost complete. A few last steps you'll have to do manually:"
echo "- Configure your web server - see nginx.conf in this directory for an example"
echo "- Set up TLS for your domain $domain, most probably using Certbot"
echo "- Navigate to https://$domain/ in your web browser and log into your admin account. Customize the server settings to your liking and fill in your profile. You will most likely want to set up some way for your server to send email via \"Admin Panel -> Other\"."
echo ""
echo "Welcome to the fediverse!"
