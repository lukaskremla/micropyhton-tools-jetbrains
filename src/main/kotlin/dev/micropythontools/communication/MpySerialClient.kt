/*
 * Copyright 2000-2024 JetBrains s.r.o.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package dev.micropythontools.communication

import com.intellij.openapi.diagnostic.thisLogger
import jssc.SerialPort
import jssc.SerialPort.FLOWCONTROL_NONE
import jssc.SerialPortEventListener
import jssc.SerialPortException
import java.io.IOException
import java.nio.charset.StandardCharsets

/**
 * @author elmot
 */
class MpySerialClient(private val comm: MpyComm) : MpyClient {
    private val port = SerialPort(comm.connectionParameters.portName)
    override fun send(string: String) {
        this@MpySerialClient.thisLogger().debug("< $string")
        port.writeString(string)
    }

    override fun hasPendingData(): Boolean = port.inputBufferBytesCount > 0

    override fun close() = closeBlocking()

    private val listener = SerialPortEventListener { event ->
        if (event.eventType and SerialPort.MASK_RXCHAR != 0) {
            val count = event.eventValue
            val s = port.readBytes(count).toString(StandardCharsets.UTF_8)
            comm.dataReceived(s)
            this@MpySerialClient.thisLogger().debug("> $s")
        }
    }

    @Throws(IOException::class)
    override suspend fun connect(progressIndicatorText: String): MpySerialClient {
        try {
            port.openPort()
            port.addEventListener(listener, SerialPort.MASK_RXCHAR)
            port.setParams(
                SerialPort.BAUDRATE_115200,
                SerialPort.DATABITS_8,
                SerialPort.STOPBITS_1,
                SerialPort.PARITY_NONE
            )
            port.flowControlMode = FLOWCONTROL_NONE
            comm.state = State.CONNECTED
            return this
        } catch (e: SerialPortException) {
            throw IOException("${e.port.portName}: ${e.exceptionType}")
        }
    }

    override fun closeBlocking() {
        port.closePort()
    }

    override fun sendPing() = Unit

    override val isConnected: Boolean
        get() = port.isOpened
}