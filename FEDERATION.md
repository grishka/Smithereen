## ActivityPub federation in Smithereen
Here are the non-standard additions to ActivityPub that Smithereen uses in order to federate between servers.

The `sm:` JSON-LD namespace is short for `http://smithereen.software/ns#`; in relevant objects, any custom fields within that namespace are aliased in `@context` and thus appear throughout the object without any namespace.
### Wall posts
People can post on other people's walls. Wall posts are part of the `sm:wall` collection, as per [FEP-400e](https://git.activitypub.dev/ActivityPubDev/Fediverse-Enhancement-Proposals/src/commit/f94077e1514928c2d2ae79d86a5953c93874b73d/feps/fep-400e.md). Addressing **must** include `as:Public` and the wall owner.

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
  "target": {
    "id": "http://smithereen.local:8080/users/4/wall",
    "attributedTo": "http://smithereen.local:8080/users/4",
    "type": "Collection"
  },
  "replies": {
    "id": "http://smithereen.local:8080/posts/110/replies",
    "type": "Collection",
    "first": {
      "next": "http://smithereen.local:8080/posts/110/replies?page=1",
      "partOf": "http://smithereen.local:8080/posts/110/replies",
      "type": "CollectionPage"
    }
  },
  "attributedTo": "http://smithereen.local:8080/users/1",
  "published": "2020-05-21T19:05:00Z",
  "sensitive": false,
  "content": "<p>Test.<\/p>"
}
```
TODO: capability negotiation
#### A note about comments
Since this is modelled after VK, comments aren't supposed to appear in newsfeeds by themselves; they only exist in the context of a top-level post. Thus, comments aren't addressed to anyone's followers. They're addressed to `as:Public`, the top-level post author, the parent comment author, and mentioned users, if any.
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
### Groups
Groups are like users, except they can't follow anything. Groups have walls that work the same way as user walls. Both `Join` and `Follow` activities work for joining groups, as well as `Leave` and `Undo{Follow}` for leaving. Outgoing activities are `Follow` and `Undo{Follow}` in order to maximize the compatibility.

Groups have administrators that are listed in the `attributedTo` field:
```json
  "attributedTo": [
    {
      "type": "Person",
      "title": "Group admins can have user-visible titles",
      "id": "http://smithereen.local:8080/users/1"
    }
  ]
```
These links must point to a `Person` object and will be ignored otherwise.

Any actions of the group administrators are federated as if the group actor itself performed them.
