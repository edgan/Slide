# Notifications
Notifications allow you to know when you have a message in your inbox from
another user.

## Version
You need at least `7.2.0` of `Slide` for either type of notification to work
properly.

Both forms of notification existed pre-fork. `7.2.0` allows them to
work again, and fixes a few bugs.

## Regular
Regular notifications poll the `Reddit` API about your inbox status at the
specified interval, and give you a notification when it sees a new one.

The default interval when it is enabled is 10 minutes. The range is 10 minutes
to 2 hours.

### Setup and requirements
1. Android level notifications have to be enabled for `Slide`.
2. Enable the interval by checking the box in
`Settings | General | Notifications`.
3. Select your desired interval.

## Piggyback
Piggyback notifications let the `Reddit` app get a push notification from
`Reddit`. `Slide` then dismisses `Reddit`'s notification and shows it's own.

### Note
The `Notification read, reply, and control` gives `Slide` complete control over
all notifications on your phone, not just ones from the official `Reddit` app
or `Slide` itself.

### Setup and requirements
Piggyback notifications have a few requirements.

1. The official `Reddit` app has to installed
2. Android level notifications have to be enabled for the official `Reddit` app
3. Notifications have to be enabled in the official `Reddit` app
4. Android level notifications have to be enabled for `Slide`
5. `Notification read, reply, and control` has to be enabled for `Slide`. The
`Enable Notifications` button in `Setting | General | Notifications` will take
you to the Android settings screen where it can be enabled.
6. After `Notification read, reply, and control` is enabled you need to reboot
your device.
