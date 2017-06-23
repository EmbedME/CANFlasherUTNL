CANFlasherUTNL
==============

CANFlasherUTNL is a flash utility for NXP LPC11C22 and LPC11C24 devices.
It writes HEX files via the built-in CANOpen bootloader to the target flash 
through an USB-to-CAN interface USBtin.
http://www.fischl.de/can/bootloader/canflasherutnl/

Usage
-----

1. Plug in USBtin (USB-to-CAN interface, http://www.fischl.de/usbtin/)
2. Start this tool, select port and HEX file name
3. Power up LPC with mode pins set for CAN bootloading
4. Press "Upload" to start firmware upload


Build and run
-------------

The frame design was created with Netbeans built-in designer.

CANFlahserUTNL uses USBtinLib to talk to USBtin devices. USBtinLib uses jSSC for
accessing the virtual serial port. Both libraries are included in this source
code as JAR file in folder lib/.

Ant is used to build the application from Java source code. To run the 
program, type
```
ant run
```


Changelog
---------

1.0 (2016-05-14)
First release

1.1 (2017-06-23)
Added GO (code jump) command

License
-------

Copyright (C) 2016-2017  Thomas Fischl (http://www.fischl.de)

This program is free software: you can redistribute it and/or modify
it under the terms of the GNU General Public License as published by
the Free Software Foundation, either version 3 of the License, or
(at your option) any later version.

This program is distributed in the hope that it will be useful,
but WITHOUT ANY WARRANTY; without even the implied warranty of
MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
GNU General Public License for more details.

You should have received a copy of the GNU General Public License
along with this program.  If not, see <http://www.gnu.org/licenses/>.

