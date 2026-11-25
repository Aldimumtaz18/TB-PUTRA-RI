package com.example.util

import android.Manifest
import android.annotation.SuppressLint
import android.bluetooth.BluetoothAdapter
import android.bluetooth.BluetoothDevice
import android.bluetooth.BluetoothManager
import android.bluetooth.BluetoothSocket
import android.content.Context
import android.content.pm.PackageManager
import android.os.Build
import androidx.core.content.ContextCompat
import java.io.IOException
import java.util.UUID
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

data class PairedPrinterDevice(
  val name: String,
  val address: String,
)

sealed interface BluetoothPrintersState {
  data object Unavailable : BluetoothPrintersState
  data object PermissionRequired : BluetoothPrintersState
  data object Disabled : BluetoothPrintersState
  data class Ready(val devices: List<PairedPrinterDevice>) : BluetoothPrintersState
}

object BluetoothPrinterHelper {

  private val SPP_UUID: UUID = UUID.fromString("00001101-0000-1000-8000-00805F9B34FB")

  fun hasConnectPermission(context: Context): Boolean {
    return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
      ContextCompat.checkSelfPermission(context, Manifest.permission.BLUETOOTH_CONNECT) ==
        PackageManager.PERMISSION_GRANTED
    } else {
      true
    }
  }

  private fun getAdapter(context: Context): BluetoothAdapter? {
    val manager = context.getSystemService(Context.BLUETOOTH_SERVICE) as? BluetoothManager
    return manager?.adapter
  }

  @SuppressLint("MissingPermission")
  fun getPairedPrintersState(context: Context): BluetoothPrintersState {
    val adapter = getAdapter(context) ?: return BluetoothPrintersState.Unavailable
    if (!hasConnectPermission(context)) {
      return BluetoothPrintersState.PermissionRequired
    }
    if (!adapter.isEnabled) {
      return BluetoothPrintersState.Disabled
    }
    return try {
      val bonded = adapter.bondedDevices.orEmpty()
      val devices =
        bonded
          .map { device ->
            val safeName =
              runCatching { device.name }.getOrNull()?.trim()?.takeIf { it.isNotEmpty() }
                ?: "Perangkat Bluetooth"
            PairedPrinterDevice(
              name = safeName,
              address = device.address.orEmpty(),
            )
          }
          .filter { it.address.isNotBlank() }
          .sortedWith(
            compareByDescending<PairedPrinterDevice> { d ->
              val lower = d.name.lowercase()
              "print" in lower || "pos" in lower || "rpp" in lower || "mtp" in lower || "eppos" in lower || "xprinter" in lower
            }.thenBy { it.name.lowercase() }
          )
      BluetoothPrintersState.Ready(devices)
    } catch (_: SecurityException) {
      BluetoothPrintersState.PermissionRequired
    }
  }

  @SuppressLint("MissingPermission")
  suspend fun printEscPos(
    context: Context,
    deviceAddress: String,
    bytes: ByteArray,
  ): Result<Unit> =
    withContext(Dispatchers.IO) {
      val adapter = getAdapter(context)
        ?: return@withContext Result.failure(IllegalStateException("Perangkat tidak mendukung Bluetooth."))
      if (!hasConnectPermission(context)) {
        return@withContext Result.failure(
          SecurityException("Izin Bluetooth belum diberikan. Aktifkan izin Perangkat Sekitar terlebih dahulu.")
        )
      }
      if (!adapter.isEnabled) {
        return@withContext Result.failure(
          IllegalStateException("Bluetooth sedang mati. Nyalakan Bluetooth terlebih dahulu.")
        )
      }

      val device: BluetoothDevice =
        try {
          adapter.getRemoteDevice(deviceAddress)
        } catch (_: IllegalArgumentException) {
          return@withContext Result.failure(IllegalArgumentException("Alamat printer tidak valid."))
        }

      var socket: BluetoothSocket? = null
      try {
        socket = connectRfcommSocket(device)
        val out = socket.outputStream
        val chunkSize = 512
        var offset = 0
        while (offset < bytes.size) {
          val count = minOf(chunkSize, bytes.size - offset)
          out.write(bytes, offset, count)
          out.flush()
          offset += count
          if (offset < bytes.size) {
            Thread.sleep(25)
          }
        }
        // Allow printer buffer to drain before tearing down RFCOMM socket
        Thread.sleep(300)
        Result.success(Unit)
      } catch (se: SecurityException) {
        Result.failure(
          SecurityException("Akses Bluetooth ditolak. Periksa izin aplikasi di Pengaturan.", se)
        )
      } catch (e: IOException) {
        Result.failure(
          IOException(
            "Gagal terhubung ke printer. Pastikan printer thermal menyala, kertas terpasang, dan berada dekat HP.",
            e,
          )
        )
      } finally {
        runCatching { socket?.close() }
      }
    }

  @SuppressLint("MissingPermission")
  private fun connectRfcommSocket(device: BluetoothDevice): BluetoothSocket {
    val secureSocket = device.createRfcommSocketToServiceRecord(SPP_UUID)
    return try {
      secureSocket.connect()
      secureSocket
    } catch (firstError: IOException) {
      runCatching { secureSocket.close() }
      // Fallback to insecure RFCOMM channel required by many 58mm/80mm portable thermal printers
      val insecureSocket = device.createInsecureRfcommSocketToServiceRecord(SPP_UUID)
      try {
        insecureSocket.connect()
        insecureSocket
      } catch (secondError: IOException) {
        runCatching { insecureSocket.close() }
        throw firstError
      }
    }
  }
}
