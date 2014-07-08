#android-vlc-remote

This is a fork of the project [android-vlc-remote](https://code.google.com/p/android-vlc-remote/) created by Peter Baldwin. It allows you to control media playback of VideoLAN Client (VLC) from your Android-powered mobile device.

##Requirements
This fork has increased the minimum API version on which it will run. It now requires Ice Cream Sandwich (API 14). Any pull requests are welcome to increase compatibility.

This has been tested with VLC 1.1, VLC 2.0.x, VLC 2.1.x and VLC 2.2.0 nightlies.

##Install
There is an install guide available at http://samicemalone.co.uk/remote-for-vlc/install.html.
You can also find a list of FAQ's at http://samicemalone.co.uk/remote-for-vlc/faq.html.

##Changes

* UI
    * Display bottom action bar to smaller screens for cropping, subtitle track, fullscreen, audio track and aspect ratio.
    * Added preferences to allow the action bar buttons to be changed
    * Playlist loading progress
    * Layout changes
    * Preference to hide the DVD tab for smaller screens
    * Updated some icons
    * Added a ViewPager to provide swipeable tabs for phone layouts
    * Added preference for showing playback controls in the notification drawer
* Media Filename Detection
    * An attempt is made to detect the media information from the filename (TV and Movies) for the current playing item.
    * Preference to detect media information for each item in the playlist
* Advanced Controls
    * Added next/previous chapter buttons for large enough screens (required VLC 2.0+)
    * Added preset preferences for audio/subtitle delays that can be toggled
* VLC Servers
    * Added an icon to distinguish which server is current being used.
    * Added ability to give servers nicknames instead of displaying ip/host:port
    * Added preference to display the current server in the action bar.
    * Added icons to distinguish between normal servers, servers that require authentication and servers for which access is forbidden
    * Servers can now be edited via the context menu.
    * Servers no longer show username/password in the title. Can still be viewed when editing.
    * Added ability to test server connection before adding
    * Added support for VLC 2.1+ http interface which uses HTTP basic authentication instead of an access control list (.hosts).
    * Prompt for password if server requires authentication.
* Browse Library
    * Fix for VLC 2.x not showing the parent (..) entry in the list on Windows.
    * Normalise file paths to avoid displaying any parent entries e.g. /path/../to/another/../path
    * If a directory is accessed without the correct permissions (or other error), send the user back to the previous directory instead of back home.
    * Added preference to display directories sorted before files.
    * Added support for libraries which allows multiple directories to be viewed as a library. This is similar to Windows Libraries. This can be useful when your media is spanned across multiple drives.

##Media Filename Detection
Filenames are matched against various regular expressions to detect the media information. The regular expressions are far from perfect and any improvements are welcome.
###TV Episodes
The TV show name, season number and episode number will try to be detected, as will the episode name if it is present. If the episode name is not present, the filename will be displayed instead.
###Movies
The movie name, year, source and quality of the movie will try to be determined.

##Screenshots
Tablet and landscape screenshots are located in the screenshots/ directory.
![Playback Controls](https://raw.github.com/samicemalone/android-vlc-remote/master/screenshots/HTC.Desire.ICS.png)
![Settings](https://raw.github.com/samicemalone/android-vlc-remote/master/screenshots/Settings.Galaxy.Nexus.JB.png)

##VLC Changes
The HTTP interface has a few different API versions which can lead to different behaviour on different VLC versions.

* VLC 1.1.x outputs file metadata for each track in the playlist where as VLC 2.0.x and 2.1.x do not.
* VLC 2.1 appears to dropping access control lists (.hosts) file in favour of a password.

##VLC Bugs
* Files with the same name in the playlist will have the `"current=current"` attribute set in playlist.xml. This is due to the HTTP interface comparing the current track by name instead of by item id. This should be fixed for 2.1. [[Source]](http://mailman.videolan.org/pipermail/vlc-commits/2013-April/019895.html)
* ~~I have been unable to get album art (over http) to work using Windows. Album art worked fine in Linux. It could possibly be related to [this message](http://lists.w3.org/Archives/Public/www-archive/2011Oct/0022.html).~~
  * UPDATE - I sent in a patch to fix this [issue](https://trac.videolan.org/vlc/ticket/7607). It should be available on VLC 2.2 nightlies and included in VLC 2.1.3

##License
android-vlc-remote is distributed under the GNU GPL version 3 or later.
Please read the file LICENSE for more details.