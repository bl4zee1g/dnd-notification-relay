Quickly vibe coded App that lets you set a Discord Webhook URL and checks if your phone is in Do Not Disturb mode. If it is, it's gonna forward all non-silent notifications that aren't from Discord to that Webhook. I made it so it doesn't show in App Drawer, so you need to open it with adb:

```
adb shell am start -n dev.notificationrelay/.MainActivity
```

And from there also grant it access to read notifications.
I just made it so I can see notifications on Discord on my Laptop when in school since my teach gets angry when I pull out my phone.
