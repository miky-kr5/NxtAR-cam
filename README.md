NxtAR: A generic software architecture for Augmented Reality based mobile robot control.
========================================================================================

Camera module
-------------

### Abstract ###

NxtAR is a generic software architecture for the development of Augmented Reality games
and applications centered around mobile robot control. This is a reference implementation
with support for [LEGO Mindstorms NXT][1] mobile robots.

### Module description ###

The camera module is a reference implementation of the image capture module of the NxtAR architecture for
the Android (>=3.0) operating system. This module acts as a bridge between the [robot control][2] and [core][3] modules of
the architecture, capturing images to be processed by the core module and forwarding control instructions to
the robot module.

### Module installation and usage. ###

Install the NxtAR-cam_XXXXXX.apk file on your device. Detailed usage instructions can be found in the [NxtAR Android backend module][4]
documentation.

 [1]: http://www.lego.com/en-us/mindstorms/?domainredir=mindstorms.lego.com
 [2]: https://github.com/sagge-miky/NxtAR-bot
 [3]: https://github.com/sagge-miky/NxtAR-core
 [4]: https://github.com/sagge-miky/NxtAR-android
