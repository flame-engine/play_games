# Configuration required for GPGS Sign In

## Concepts: Don't mix the consoles!

Firstly, as stated already, GPGS stands for Google Play Games Services.

Google has three major consoles: Play Console, Cloud Console and Firebase Console.
 * [Google Play Console](https://play.google.com/apps/publish/) is related to publishing games on the Play Store and using the GPGS.
 * [Google Cloud Console](https://console.cloud.google.com) is the regular console for anything Google Cloud related (GAE, GCE, IAM, etc, etc).
 * [Firebase Console](https://console.firebase.google.com) is the console for Firebase related stuff, that are slowly being intertwined with GCP stuff.

 Firebase provides a very good generic authentication service, with lots of providers, including GPGS! But we will **not** be using that. We are going to sign in directly with GPGS, without Firebase. You might be using Firebase for other things, though (you most likely are).

 Don't get those mixed up!

## Create a GPGS project
...

## Steps

The basic steps (need to elaborate)
 * Create the GPGS Project
 * Create the linked android app with proper package name and SHA-1 inside GPGS
 * Create also a webapp and get the id from that (NOT your android linked app)
 * Create at least one achievement to get your games-ids.xml file and place it accordingly
 * Remember to add yourself as tester if not published


