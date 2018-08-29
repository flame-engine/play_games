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

Let's assume you already have your Application registered in Google Play Console; that is what allows you to have a Store listing, and publish your app. If you don't, you should be able to easily create one without problems.

That being set up, you need to create the Games Services project. That is a different thing! Access the [GPC](https://play.google.com/apps/publish), login if not already, and select *Game Services* on the left menu:

![1](images/p1.png)

You might need to accept some lots of terms and conditions.

Now you need to create a new game; if this is your first time, you will need to click on the blue button *SET UP GOOGLE PLAY GAMES SERVICES* in the center of the screen.

![2](images/p2.png)

Otherwise, click on the *ADD NEW GAME* blue button on the top right corner:

![3](images/p3.png)

Either way, you will the the following dialog:

![4](images/p4.png)

Fill both the name of the game and the category (you can copy from the Application you already have under *All applications*), and tap *CONTINUE*.

You will be taken to your game page. A few things need to be done here. In order to publish (you can test without Publishing), you need to fill the Game Details page (basically just copying details that you already filled on your Application). You will find out that an orange check in the menu means it's ok for testing, and a green one means it's ok to publish. You can refrain from filling these for now, as long as you follow the Testing procedures in the end. Otherwise, I'd recommend you already fill this so you won't forget anything later on.

## Link your project

This is the most unfriendly and cryptic part, so pay attention!

...

## Steps

The basic steps (need to elaborate)
 * Create the GPGS Project
 * Create the linked android app with proper package name and SHA-1 inside GPGS
 * Create also a webapp and get the id from that (NOT your android linked app)
 * Create at least one achievement to get your games-ids.xml file and place it accordingly
 * Remember to add yourself as tester if not published


