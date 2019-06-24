SuperFreezZ
===========

[<img src="https://f-droid.org/badge/get-it-on.png" alt="Get it on F-Droid" height="70">](https://f-droid.org/packages/superfreeze.tool.android/)

Android app (beta) making it possible to entirely freeze all background activities of any app.

Greenify can also do this, but it is not Open Source.

SuperFreezZ is not yet another task manager promising to delete 10 GB of data per month or making your device 2x as fast. This is impossible.

Instead, SuperFreezZ is honest about its disadvantages: Freezing daily used apps probably drains your battery a little faster. Also, these apps will take longer to start when you use them the next time: SuperFreezZ will super freeze your apps, it takes about 1-3 seconds to defrost them. Greenify has the same disadvantages, except that the author of Greenify does not warn you about it. So: Just do not overdo it, and SuperFreezZ will be super useful. 

Examples for apps that deserve to be frozen:

 * Untrusted apps (that you do not want to run in the background)
 * Apps you rarely use
 * Annoying apps

Features
--------

 * Optionally works without accessibility service as this slows down the device
 * Can freeze only apps not used for a week (can be configured)
 * Choose a white list (freeze all by standard) or a black list (do not freeze anything by standard)
 * Can freeze apps when the screen goes off
 * Options to freeze system apps and even SuperFreezZ itself
 * Completely open source and free software

Build
-----

The build should succeed out of the box with Android Studio and Gradle. If not, open an issue. Others will probably also have the problem.

Contributing to SuperFreezZ
------------

### Development

If you have a problem, question or an idea, just open an issue!

If you would like to help with developing, have a look at the issues, or think of something that could be improved, and open an issue for it.

Please tell me what you are going to do, to avoid implementing the same thing at the same time :-)

### Translate

You can [translate SuperFreezZ on Weblate](https://hosted.weblate.org/engage/superfreezz/). Current progress:

[![Translation status](https://hosted.weblate.org/widgets/superfreezz/-/multi-auto.svg)](https://hosted.weblate.org/engage/superfreezz/?utm_source=widget)

You can always add other lanuages.

### Donate

Monetary donations are currently not accepted (setting up a Liberapay account was too much effort, so it was given up for the time being).

However, to show me your support, you can [donate to WWF or the Christian Blind Mission and post about it here](https://gitlab.com/SuperFreezZ/SuperFreezZ/issues/18).

Credits
-------

The code to show the app list is from [ApkExtractor](https://f-droid.org/wiki/page/axp.tool.apkextractor).

Robin Naumann made a nice intro. The intro was created using the AppIntro library.

The feature graphic background is from here: https://pixabay.com/photos/thunder-lighting-lightning-cloud-1368797/, the text was added with https://www.norio.be/android-feature-graphic-generator/.

Copying
-------

```
Copyright (c) 2015 axxapy
Copyright (c) 2018, 2019 Hocuri
Copyright (c) 2019 Robin Naumann

SuperFreezZ is free software: You can redistribute it and/or modify
it under the terms of the GNU General Public License as published by
the Free Software Foundation, either version 3 of the License, or
(at your option) any later version.

SuperFreezZ is distributed in the hope that it will be useful,
but WITHOUT ANY WARRANTY; without even the implied warranty of
MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the
GNU General Public License for more details.

You should have received a copy of the GNU General Public License
along with SuperFreezZ. If not, see <https://www.gnu.org/licenses/>.
```

Q/A
---

Q/A:

Q: What is the difference between hibernating and freezing?
A: There is none at all. If you hibernate an app with Greenify it will even be shown as frozen in SuperFreezZ and the other way round.

Q: But the correct spelling would be "SuperFreeze"!
A: I know.

Q: Do you have any intentions to sell freezers?
A: No.