syntax:
:

    ```xml
    <permission android:description="string resource"
                android:icon="drawable resource"
                android:label="string resource"
                android:name="string"
                android:permissionGroup="string"
                android:protectionLevel=["normal" | "dangerous" |
                                         "signature" | ...] />
    ```

contained in:
:   `https://developer.android.com/guide/topics/manifest/manifest-element`

description:
:   Declares a security permission used to
    limit access to specific components or features of this or other applications.
    For more information about how permissions work, see the [Permissions](https://developer.android.com/guide/topics/manifest/manifest-intro#perms) section in the app manifest overview and [Security tips](https://developer.android.com/guide/topics/security/security).

attributes:
:

    `android:description`

    :   A user-readable description of the permission that is longer and more informative than the label. It might display, for example, to explain the permission to the user when the user is asked to grant the permission to another application.<br />


        This attribute is set as a reference to a string resource.
        Unlike the `label` attribute, it can't be a raw string.

    `android:icon`
    :   A reference to a drawable resource for an icon that represents the
        permission.

    `android:label`

    :   A user-readable name for the permission.<br />


        As a convenience, the label can be directly set
        as a raw string while you're developing the application. However,
        when the application is ready to publish, set it as a
        reference to a string resource, so that it can be localized like other
        strings in the user interface.

    `android:name`
    :   The name to be used in
        code to refer to the permission, such as in a
        `https://developer.android.com/guide/topics/manifest/uses-permission-element` element or the
        `permission` attributes of application components.


        **Note:** The system doesn't let multiple packages declare
        a permission with the same name unless all the packages are signed with the
        same certificate. If a package declares a permission, the system doesn't permit
        the user to install other packages with the same permission name, unless
        those packages are signed with the same certificate as the first package.

        For this reason, Google recommends prefixing permissions with the app's package name,
        using reverse-domain-style naming.
        Follow this prefix with `.permission.` and then a description of the
        capability that the permission represents in upper SNAKE_CASE. For example:
        `com.example.myapp.permission.ENGAGE_HYPERSPACE`.

        Following this recommendation avoids naming collisions and helps clearly identify the owner
        and intention of a custom permission.

    `android:permissionGroup`
    :   Assigns this permission to a group. The value of this attribute is
        the name of the group, which is declared with the
        `https://developer.android.com/guide/topics/manifest/permission-group-element` element in this
        or another application. If this attribute isn't set, the permission
        doesn't belong to a group.

    `android:protectionLevel`

    :
        Characterizes the potential risk implied in the permission and
        indicates the procedure for the system to follow when determining
        whether to grant the permission to an application requesting it.


        Each protection level consists of a base permission type and zero or more
        flags. For example, the `"dangerous"` protection level has no
        flags. In contrast, the protection level `"signature|privileged"`
        is a combination of the `"signature"` base permission type and the
        `"privileged"` flag.


        The following table shows all base permission types. For a list of flags, see
        `https://developer.android.com/reference/android/R.attr#protectionLevel`.

        | Value | Meaning |
        |---|---|
        | `"normal"` | The default value. A lower-risk permission that gives requesting applications access to isolated application-level features with minimal risk to other applications, the system, or the user. The system automatically grants this type of permission to a requesting application at installation, without asking for the user's explicit approval, though the user always has the option to review these permissions before installing. |
        | `"dangerous"` | A higher-risk permission that gives a requesting application access to private user data or control over the device that can negatively impact the user. Because this type of permission introduces potential risk, the system might not automatically grant it to the requesting application. For example, any dangerous permissions requested by an application might be displayed to the user and require confirmation before proceeding, or some other approach might be taken to avoid the user automatically granting the use of such facilities. |
        | `"signature"` | A permission that the system grants only if the requesting application is signed with the same certificate as the application that declared the permission. If the certificates match, the system automatically grants the permission without notifying the user or asking for the user's explicit approval. |
        | `"knownSigner"` | A permission that the system grants only if the requesting application is signed with [an allowed certificate](https://developer.android.com/guide/topics/permissions/defining#grant-signature-permissions). If the requester's certificate is listed, the system automatically grants the permission without notifying the user or asking for the user's explicit approval. |
        | `"signatureOrSystem"` | *Old synonym for `"signature|privileged"`. Deprecated in API level 23.* A permission that the system grants only to applications that are in a dedicated folder on the Android system image *or* that are signed with the same certificate as the application that declared the permission. Avoid using this option, as the `"signature"` protection level is sufficient for most needs and works regardless of where apps are installed. The `"signatureOrSystem"` permission is used for certain special situations where multiple vendors have applications built into a system image and need to share specific features explicitly because they are being built together. |

introduced in:
:   API level 1

see also:
:   `https://developer.android.com/guide/topics/manifest/uses-permission-element`

    `https://developer.android.com/guide/topics/manifest/permission-tree-element`

    `https://developer.android.com/guide/topics/manifest/permission-group-element`