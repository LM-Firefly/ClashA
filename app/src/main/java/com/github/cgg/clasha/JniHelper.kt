/* Shadowsocks - A shadowsocks client for Android
 * Copyright (C) 2012 <max.c.lv@gmail.com>
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program.  If not, see <http://www.gnu.org/licenses/>.
 *
 *
 *                            ___====-_  _-====___
 *                      _--^^^#####//      \\#####^^^--_
 *                   _-^##########// (    ) \\##########^-_
 *                  -############//  |\^^/|  \\############-
 *                _/############//   (@::@)   \\############\_
 *               /#############((     \\//     ))#############\
 *              -###############\\    (oo)    //###############-
 *             -#################\\  / VV \  //#################-
 *            -###################\\/      \//###################-
 *           _#/|##########/\######(   /\   )######/\##########|\#_
 *           |/ |#/\#/\#/\/  \#/\##\  |  |  /##/\#/  \/\#/\#/\#| \|
 *           `  |/  V  V  `   V  \#\| |  | |/#/  V   '  V  V  \|  '
 *              `   `  `      `   / | |  | | \   '      '  '   '
 *                               (  | |  | |  )
 *                              __\ | |  | | /__
 *                             (vvv(VVV)(VVV)vvv)
 *
 *                              HERE BE DRAGONS
 *
 */

package com.github.cgg.clasha

import android.os.Build
import android.system.ErrnoException

object JniHelper {
    init {
        System.loadLibrary("jni-helper")
    }

    @Deprecated // Use Process.destroy() since API 24
    @Throws(Exception::class)
    fun sigtermCompat(@NonNull process: Process) {
        if (Build.VERSION.SDK_INT >= 24) throw UnsupportedOperationException("Never call this method in OpenJDK!")
        val errno = sigterm(process)
        if (errno != 0) throw ErrnoException("kill", errno)
    }

    @Deprecated // only implemented for before API 24
    @Throws(Exception::class)
    fun waitForCompat(@NonNull process: Process, millis: Long): Boolean {
        if (Build.VERSION.SDK_INT >= 24) throw UnsupportedOperationException("Never call this method in OpenJDK!")
        val mutex = getExitValueMutex(process)
        synchronized(mutex) {
            if (getExitValue(process) == null) mutex.wait(millis)
            return getExitValue(process) != null
        }
    }

    external fun sigkill(pid: Int): Int
    private external fun sigterm(process: Process): Int
    private external fun getExitValue(process: Process): Integer?
    private external fun getExitValueMutex(process: Process): Object
    external fun sendFd(fd: Int, @NonNull path: String): Int
    external fun close(fd: Int)
}
