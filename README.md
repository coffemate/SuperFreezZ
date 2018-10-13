SuperFreezZ
===========

An android app that makes it possible to entirely freeze all background activities of an app.
Currently, the user has to force stop apps by hand, SuperFreezZ only shows the app settings page.

Greenify is another app that can do this, but it is not Open Source.

Any contributions are welcome.

SuperFreezZ is not yet another task manager promising to delete 10GB of data per month or making your device 2x as fast. This is impossible. You should freeze only
* apps that you do not trust (and do not want to run in background) and 
* apps that you use very few.

If you freeze apps that you use daily, the battery of your device will drain faster and these apps will take longer to load.

Features
--------

* Optionally works without accessibility service as this slows down the device

Build
-----

The build should succeed out of the box with Android Studio and Gradle. If not, it is probably my fault, please open an issue then. Others will probably also have this problem then.

Contributing to SuperFreezZ
------------

If you have a problem or a question or an idea or whatever, just open an issue!

If you would like to help, have a look at the issues or think about what could be improved and open an issue for it. Please tell me what you are going to do to avoid that I also implement the same thing at the same time :-)


Copying
-------

```
Copyright (c) 2015 axxapy
Copyright (c) 2018 Hocceruser

SuperFreezZ is free software: you can redistribute it and/or modify
it under the terms of the GNU General Public License as published by
the Free Software Foundation, either version 3 of the License, or
(at your option) any later version.

SuperFreezZ is distributed in the hope that it will be useful,
but WITHOUT ANY WARRANTY; without even the implied warranty of
MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
GNU General Public License for more details.

You should have received a copy of the GNU General Public License
along with SuperFreezZ.  If not, see <http://www.gnu.org/licenses/>.
```

------------------------------------------------------------------

SuperFreezZ contains files additionally distributed under the MIT license. For these files you may choose between GPLv3-or-later (see above) and MIT (see below). These files are:

```
build.gradle
src/superfreeze/tool/android/userInterface/AppsListAdapter.kt
src/superfreeze/tool/android/userInterface/MainActivity.kt
res/layout/list_item.xml
res/layout/activity_main.xml
res/values/strings.xml
res/values/colors.xml
res/values/styles.xml
res/values/attrs.xml
res/values-de/strings.xml
res/xml/searchable.xml
res/menu/main.xml
AndroidManifest.xml
```

```
The MIT License (MIT)

Copyright (c) 2015 axxapy
Copyright (c) 2018 Hocceruser

Permission is hereby granted, free of charge, to any person obtaining a copy
of this software and associated documentation files (the "Software"), to deal
in the Software without restriction, including without limitation the rights
to use, copy, modify, merge, publish, distribute, sublicense, and/or sell
copies of the Software, and to permit persons to whom the Software is
furnished to do so, subject to the following conditions:

The above copyright notice and this permission notice shall be included in all
copies or substantial portions of the Software.

THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR
IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY,
FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE
AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER
LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM,
OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN THE
SOFTWARE.
```