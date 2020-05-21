## ActivityPub federation in Smithereen
Here are the non-standard additions to ActivityPub that Smithereen uses in order to federate between servers.

The `sm:` JSON-LD namespace is short for `http://smithereen.software/ns#`; in relevant objects, any custom fields within that namespace are aliased in `@context` and thus appear throughout the object without any namespace.
### Wall posts
People can post on other people's walls. These posts are exposed in the outbox of the wall owner, and could be, of course, fetched via a direct link. Compared to a "regular" post of a user on their own wall, these posts have an additional field `partOf` that points to the outbox of the owner. Addressing **must** include `as:Public` and the wall owner.

Example object:
```json
{
  "@context": [
    "https://www.w3.org/ns/activitystreams",
    {
      "sensitive": "as:sensitive"
    }
  ],
  "type": "Note",
  "id": "http://smithereen.local:8080/posts/110",
  "url": "http://smithereen.local:8080/posts/110",
  "to": [],
  "cc": [
    "https://www.w3.org/ns/activitystreams#Public",
    "http://smithereen.local:8080/users/4"
  ],
  "partOf": "http://smithereen.local:8080/users/4/outbox",
  "attributedTo": "http://smithereen.local:8080/users/1",
  "published": "2020-05-21T19:05:00Z",
  "sensitive": false,
  "content": "<p>Test.<\/p>"
}
```
TODO: capability negotiation
### Friends and Followers
Any bilateral followings are considered friends. Even though all followers are accepted automatically, `Accept{Follow}` is expected from remote servers. Actors from servers running software that allows manually reviewing and accepting followers, e.g. Mastodon, are supported.
### Friend requests
Friend requests are sent as `Offer{Follow}` activities, where the inner `Follow` activity is as if the friend request recepient is following its sender. Semantically, this is "I'm asking you to follow me back". In order for Smithereen to allow sending a friend request to a remote actor, that actor must have `sm:supportsFriendRequests` field set to `true`. A friend request may have a text message attached to it in `content` of the `Offer`.

Accepting a friend request is done by following the sender back, that is, simply sending a `Follow`.

Rejecting a friend request is done by sending a `Reject{Offer{Follow}}` activity.
### Additional profile fields
There are separate fields for first and last names, birth date, and gender, all based on schema.org. Those are `firstName`, `lastName`, `birthDate` and `gender`. `firstName` and `lastName` are respectively aliases to `givenName` and `familyName`. Birth date is in the format `YYYY-MM-DD`. Gender can either be `sc:Male` or `sc:Female`.
### Non-square profile pictures
Smithereen uses non-square profile pictures on the profile page. In order to retain compatibility with everything else, `icon` in the actor still points to a square picture. It's extended with the `image` field that contains the full rectangular one, and `sm:cropRegion` with the coordinates of the square version within the rectangular one. The coordinates are in the order `[x1, y1, x2, y2]`, where (x1, y1) are the top-left corner of the square, and (x2, y2) are the bottom-right. The top-left corner of the rectangle is (0, 0), and the bottom-right one is (1, 1).

Example:
```json
...
  "icon": {
    "type": "Image",
    "url": "http://smithereen.local:8080/s/uploads/avatars/a502dd6ba23da7a899526368b9b58896_xl.jpg",
    "width": 400,
    "height": 400,
    "image": {
      "type": "Image",
      "url": "http://smithereen.local:8080/s/uploads/avatars/a502dd6ba23da7a899526368b9b58896_rxl.jpg",
      "width": 400,
      "height": 565
    },
    "cropRegion": [
      0.0555555559694767,
      0.05295007675886154,
      0.745726466178894,
      0.5416036248207092
    ]
  },
...
```
