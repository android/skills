<br />


Applicable XR devices This guidance helps you build experiences for these types of XR devices. [Learn about XR device types →](https://developer.android.com/develop/xr/devices) ![](https://developer.android.com/static/images/develop/xr/ai-glasses-icon.svg) AI Glasses [](https://developer.android.com/develop/xr/devices#ai-glasses) [Learn about XR device types →](https://developer.android.com/develop/xr/devices)

<br />

You can specify a different [`PendingIntent`](https://developer.android.com/reference/kotlin/android/app/PendingIntent) that is invoked when a
notification is tapped *on the glasses display* . This `PendingIntent` is
distinct from the default intent set for the phone using
[`setContentIntent`](https://developer.android.com/reference/kotlin/androidx/core/app/NotificationCompat.Builder#setContentIntent(android.app.PendingIntent)). For example, when the user taps on the notification on
the display AI glasses, a specific glasses activity launches on the display AI
glasses.

To add glasses-specific behaviors, use [`ProjectedExtender`](https://developer.android.com/reference/kotlin/androidx/core/app/NotificationCompat.ProjectedExtender). This API lets
you to customize the notification's behavior on the glasses without affecting
how it appears on the phone.

### Kotlin

    // Intent to fire when tapped on the phone
    val phoneIntent = Intent(this, MyPhoneActivity::class.java)
    val phonePendingIntent = PendingIntent.getActivity(
        this, 0, phoneIntent, PendingIntent.FLAG_IMMUTABLE
    )

    // Intent to fire when tapped on the glasses display
    val glassesIntent = Intent(this, MyGlassesActivity::class.java)
    val glassesPendingIntent = PendingIntent.getActivity(
        this, 1, glassesIntent, PendingIntent.FLAG_IMMUTABLE
    )

    // Create the base notification
    val builder = NotificationCompat.Builder(this, CHANNEL_ID).apply {
        setSmallIcon(R.drawable.ic_notification)
        setContentTitle("Navigation in Progress")
        setContentText("Tap to see details")
        // Default intent for phone
        setContentIntent(phonePendingIntent)

        // Create and apply the glasses extender
        val projectedExtender = ProjectedExtender().setContentIntent(glassesPendingIntent)

        extend(projectedExtender)
    }

    // Issue the notification
    notificationManager.notify(NOTIFICATION_ID, builder.build())

### Java

    // Intent to fire when tapped on the phone
    Intent phoneIntent = new Intent(this, MyPhoneActivity.class);
    PendingIntent phonePendingIntent =
        PendingIntent.getActivity(this, 0, phoneIntent, PendingIntent.FLAG_IMMUTABLE);

    // Intent to fire when tapped on the glasses display
    Intent glassesIntent = new Intent(this, MyGlassesActivity.class);
    PendingIntent glassesPendingIntent =
        PendingIntent.getActivity(this, 1, glassesIntent, PendingIntent.FLAG_IMMUTABLE);

    // Create the base notification
    NotificationCompat.Builder builder = new NotificationCompat.Builder(this, CHANNEL_ID)
        .setSmallIcon(R.drawable.ic_notification)
        .setContentTitle("New Update")
        .setContentText("Something important happened.")
        // Default intent for phone
        .setContentIntent(phonePendingIntent);

    // Create and apply the Glasses extender
    ProjectedExtender projectedExtender = new ProjectedExtender()
        // glasses-specific intent
        .setContentIntent(glassesPendingIntent);
    builder.extend(projectedExtender);

    // Issue the notification
    notificationManager.notify(NOTIFICATION_ID, builder.build());