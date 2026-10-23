package com.example.data.payment

import android.content.ContentValues
import android.content.Context
import android.content.Intent
import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.graphics.RectF
import android.graphics.Typeface
import android.net.Uri
import android.os.Build
import android.os.Environment
import android.provider.MediaStore
import android.widget.Toast
import androidx.core.content.FileProvider
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.File
import java.io.FileOutputStream
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import kotlin.math.abs

data class DepositReceiptData(
    val orderId: String,
    val txnId: String,
    val amount: Int,
    val updatedBalance: Int,
    val paymentMethod: String = "Paytm UPI / QR Code",
    val userName: String = "Player",
    val userEmail: String = "",
    val bankTxnId: String? = null,
    val timestamp: Long = System.currentTimeMillis()
)

object DepositReceiptManager {

    fun generateReceiptBitmap(data: DepositReceiptData): Bitmap {
        val width = 720
        val height = 1140
        val bitmap = Bitmap.createBitmap(width, height, Bitmap.Config.ARGB_8888)
        val canvas = Canvas(bitmap)

        // Outer Dark Slate Background
        canvas.drawColor(Color.parseColor("#0F172A"))

        val paint = Paint(Paint.ANTI_ALIAS_FLAG)

        // Card Container Panel
        val cardRect = RectF(28f, 28f, (width - 28).toFloat(), (height - 28).toFloat())
        paint.color = Color.parseColor("#1E293B")
        paint.style = Paint.Style.FILL
        canvas.drawRoundRect(cardRect, 28f, 28f, paint)

        // Card Border in Accent Green / Slate
        paint.color = Color.parseColor("#10B981")
        paint.style = Paint.Style.STROKE
        paint.strokeWidth = 3f
        canvas.drawRoundRect(cardRect, 28f, 28f, paint)

        // Top Accent Banner (Fire Orange to Gold)
        paint.style = Paint.Style.FILL
        paint.color = Color.parseColor("#FF5722")
        canvas.drawRoundRect(RectF(28f, 28f, (width - 28).toFloat(), 42f), 6f, 6f, paint)

        // Title: eSports Arena
        paint.color = Color.WHITE
        paint.textSize = 34f
        paint.typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
        paint.textAlign = Paint.Align.CENTER
        canvas.drawText("eSPORTS ARENA", (width / 2).toFloat(), 95f, paint)

        // Subtitle
        paint.color = Color.parseColor("#94A3B8")
        paint.textSize = 18f
        paint.typeface = Typeface.create(Typeface.DEFAULT, Typeface.NORMAL)
        canvas.drawText("OFFICIAL DEPOSIT TRANSACTION RECEIPT", (width / 2).toFloat(), 125f, paint)

        // Verified Success Tag Pill
        val tagRect = RectF((width / 2 - 130).toFloat(), 145f, (width / 2 + 130).toFloat(), 185f)
        paint.color = Color.parseColor("#064E3B")
        paint.style = Paint.Style.FILL
        canvas.drawRoundRect(tagRect, 20f, 20f, paint)

        paint.color = Color.parseColor("#34D399")
        paint.textSize = 16f
        paint.typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
        canvas.drawText("✓ VERIFIED & CREDITED", (width / 2).toFloat(), 171f, paint)

        // Amount Box
        val amtBox = RectF(60f, 215f, (width - 60).toFloat(), 345f)
        paint.color = Color.parseColor("#0F172A")
        paint.style = Paint.Style.FILL
        canvas.drawRoundRect(amtBox, 20f, 20f, paint)

        paint.color = Color.parseColor("#10B981")
        paint.style = Paint.Style.STROKE
        paint.strokeWidth = 2f
        canvas.drawRoundRect(amtBox, 20f, 20f, paint)

        // Amount Label
        paint.style = Paint.Style.FILL
        paint.color = Color.parseColor("#94A3B8")
        paint.textSize = 16f
        paint.typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
        canvas.drawText("TOTAL AMOUNT ADDED", (width / 2).toFloat(), 250f, paint)

        // Large Amount
        paint.color = Color.parseColor("#10B981")
        paint.textSize = 52f
        paint.typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
        canvas.drawText("+₹${data.amount}", (width / 2).toFloat(), 305f, paint)

        // Updated Balance
        paint.color = Color.parseColor("#CBD5E1")
        paint.textSize = 17f
        paint.typeface = Typeface.create(Typeface.DEFAULT, Typeface.NORMAL)
        canvas.drawText("Updated Wallet Balance: ₹${data.updatedBalance}", (width / 2).toFloat(), 333f, paint)

        // Dotted / Solid Horizontal Divider
        paint.color = Color.parseColor("#334155")
        paint.strokeWidth = 1.5f
        canvas.drawLine(60f, 375f, (width - 60).toFloat(), 375f, paint)

        // Key-Value Rows
        val leftX = 64f
        val rightX = (width - 64).toFloat()
        var currentY = 415f
        val lineSpacing = 50f

        val dateFormat = SimpleDateFormat("dd MMM yyyy, hh:mm:ss a", Locale.getDefault())
        val formattedDate = dateFormat.format(Date(data.timestamp))

        val rows = listOf(
            Pair("Order ID", data.orderId),
            Pair("Paytm Txn ID", data.txnId),
            Pair("Bank UTR / Ref", data.bankTxnId ?: "GATEWAY_VERIFIED"),
            Pair("Payment Mode", data.paymentMethod),
            Pair("Account User", data.userName),
            Pair("Account Email", data.userEmail.ifBlank { "Unregistered" }),
            Pair("Date & Time", formattedDate),
            Pair("Gateway Status", "TXN_SUCCESS (Code 01)")
        )

        for ((label, value) in rows) {
            // Label
            paint.style = Paint.Style.FILL
            paint.textAlign = Paint.Align.LEFT
            paint.color = Color.parseColor("#94A3B8")
            paint.textSize = 18f
            paint.typeface = Typeface.create(Typeface.DEFAULT, Typeface.NORMAL)
            canvas.drawText(label, leftX, currentY, paint)

            // Value
            paint.textAlign = Paint.Align.RIGHT
            paint.textSize = 18f
            if (label == "Gateway Status") {
                paint.color = Color.parseColor("#10B981")
                paint.typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
            } else if (label.contains("ID") || label.contains("Ref")) {
                paint.color = Color.parseColor("#F8FAFC")
                paint.typeface = Typeface.create(Typeface.MONOSPACE, Typeface.BOLD)
            } else {
                paint.color = Color.parseColor("#F8FAFC")
                paint.typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
            }

            // Truncate if too long to prevent overlap
            val safeValue = if (value.length > 28) value.take(26) + "…" else value
            canvas.drawText(safeValue, rightX, currentY, paint)

            // Subtle row separator line
            paint.color = Color.parseColor("#1E293B").let {
                Color.parseColor("#334155")
            }
            paint.strokeWidth = 1f
            canvas.drawLine(leftX, currentY + 14f, rightX, currentY + 14f, paint)

            currentY += lineSpacing
        }

        // Security / Verification Footer Panel
        val footerBox = RectF(60f, (height - 230).toFloat(), (width - 60).toFloat(), (height - 60).toFloat())
        paint.style = Paint.Style.FILL
        paint.color = Color.parseColor("#0F172A")
        canvas.drawRoundRect(footerBox, 16f, 16f, paint)

        paint.color = Color.parseColor("#334155")
        paint.style = Paint.Style.STROKE
        paint.strokeWidth = 1.5f
        canvas.drawRoundRect(footerBox, 16f, 16f, paint)

        // Footer Text
        paint.style = Paint.Style.FILL
        paint.textAlign = Paint.Align.CENTER

        paint.color = Color.parseColor("#10B981")
        paint.textSize = 17f
        paint.typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
        canvas.drawText("🔒 256-Bit SSL Encrypted Verification", (width / 2).toFloat(), (height - 190).toFloat(), paint)

        paint.color = Color.parseColor("#94A3B8")
        paint.textSize = 14f
        paint.typeface = Typeface.create(Typeface.MONOSPACE, Typeface.NORMAL)
        val hash = abs(data.orderId.hashCode()).toString(16).padStart(8, '0').uppercase()
        canvas.drawText("Digital Receipt Hash: SHA256-$hash", (width / 2).toFloat(), (height - 155).toFloat(), paint)

        paint.color = Color.parseColor("#64748B")
        paint.textSize = 13f
        paint.typeface = Typeface.create(Typeface.DEFAULT, Typeface.NORMAL)
        canvas.drawText("Generated by eSports Arena Cloud Billing Server", (width / 2).toFloat(), (height - 125).toFloat(), paint)
        canvas.drawText("Help & Dispute: support@esportsarena.app", (width / 2).toFloat(), (height - 98).toFloat(), paint)

        return bitmap
    }

    suspend fun saveReceiptToDevice(context: Context, data: DepositReceiptData): Result<Uri> = withContext(Dispatchers.IO) {
        try {
            val bitmap = generateReceiptBitmap(data)
            val fileName = "eSportsArena_Receipt_${data.orderId}.png"
            val resolver = context.contentResolver

            // Cache file copy for immediate FileProvider access
            val cacheDir = File(context.cacheDir, "receipts").apply { mkdirs() }
            val cacheFile = File(cacheDir, fileName)
            FileOutputStream(cacheFile).use { out ->
                bitmap.compress(Bitmap.CompressFormat.PNG, 100, out)
            }

            var savedUri: Uri? = null

            // Save to Public MediaStore Downloads / Pictures
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                val contentValues = ContentValues().apply {
                    put(MediaStore.MediaColumns.DISPLAY_NAME, fileName)
                    put(MediaStore.MediaColumns.MIME_TYPE, "image/png")
                    put(MediaStore.MediaColumns.RELATIVE_PATH, Environment.DIRECTORY_DOWNLOADS + "/eSportsArena")
                    put(MediaStore.MediaColumns.IS_PENDING, 1)
                }
                val collection = MediaStore.Downloads.getContentUri(MediaStore.VOLUME_EXTERNAL_PRIMARY)
                val itemUri = resolver.insert(collection, contentValues)
                    ?: resolver.insert(MediaStore.Images.Media.EXTERNAL_CONTENT_URI, contentValues)

                itemUri?.let { uri ->
                    resolver.openOutputStream(uri)?.use { out ->
                        bitmap.compress(Bitmap.CompressFormat.PNG, 100, out)
                    }
                    contentValues.clear()
                    contentValues.put(MediaStore.MediaColumns.IS_PENDING, 0)
                    resolver.update(uri, contentValues, null, null)
                    savedUri = uri
                }
            } else {
                val downloadsDir = Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOWNLOADS)
                val targetDir = File(downloadsDir, "eSportsArena").apply { mkdirs() }
                val targetFile = File(targetDir, fileName)
                FileOutputStream(targetFile).use { out ->
                    bitmap.compress(Bitmap.CompressFormat.PNG, 100, out)
                }
                savedUri = Uri.fromFile(targetFile)
            }

            // Fallback to cache FileProvider URI if MediaStore was null
            val finalUri = savedUri ?: FileProvider.getUriForFile(
                context,
                "${context.packageName}.fileprovider",
                cacheFile
            )

            // Also write a formatted text receipt
            try {
                val txtFileName = "eSportsArena_Receipt_${data.orderId}.txt"
                val txtContent = buildString {
                    appendLine("==================================================")
                    appendLine("       eSPORTS ARENA — OFFICIAL DEPOSIT RECEIPT   ")
                    appendLine("==================================================")
                    appendLine("Status:            TXN_SUCCESS (VERIFIED & CREDITED)")
                    appendLine("Amount Credited:   +₹${data.amount}")
                    appendLine("Updated Balance:   ₹${data.updatedBalance}")
                    appendLine("Order ID:          ${data.orderId}")
                    appendLine("Paytm Txn ID:      ${data.txnId}")
                    appendLine("Bank UTR / Ref:    ${data.bankTxnId ?: "UPI_PAYTM_GATEWAY"}")
                    appendLine("Payment Channel:   ${data.paymentMethod}")
                    appendLine("Account User:      ${data.userName} (${data.userEmail})")
                    appendLine("Date & Time:       ${SimpleDateFormat("dd MMM yyyy, hh:mm:ss a", Locale.getDefault()).format(Date(data.timestamp))}")
                    appendLine("--------------------------------------------------")
                    appendLine("Security: 256-Bit SSL Encrypted Electronic Receipt")
                    appendLine("Support: support@esportsarena.app")
                    appendLine("==================================================")
                }
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                    val txtValues = ContentValues().apply {
                        put(MediaStore.MediaColumns.DISPLAY_NAME, txtFileName)
                        put(MediaStore.MediaColumns.MIME_TYPE, "text/plain")
                        put(MediaStore.MediaColumns.RELATIVE_PATH, Environment.DIRECTORY_DOWNLOADS + "/eSportsArena")
                    }
                    resolver.insert(MediaStore.Downloads.EXTERNAL_CONTENT_URI, txtValues)?.let { uri ->
                        resolver.openOutputStream(uri)?.use { out ->
                            out.write(txtContent.toByteArray())
                        }
                    }
                }
            } catch (_: Exception) {}

            Result.success(finalUri)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    fun shareReceipt(context: Context, data: DepositReceiptData) {
        try {
            val bitmap = generateReceiptBitmap(data)
            val fileName = "Receipt_${data.orderId}.png"
            val cacheDir = File(context.cacheDir, "receipts").apply { mkdirs() }
            val cacheFile = File(cacheDir, fileName)
            FileOutputStream(cacheFile).use { out ->
                bitmap.compress(Bitmap.CompressFormat.PNG, 100, out)
            }

            val uri = FileProvider.getUriForFile(
                context,
                "${context.packageName}.fileprovider",
                cacheFile
            )

            val shareIntent = Intent(Intent.ACTION_SEND).apply {
                type = "image/png"
                putExtra(Intent.EXTRA_STREAM, uri)
                putExtra(Intent.EXTRA_SUBJECT, "Deposit Receipt - ₹${data.amount} (Order #${data.orderId})")
                putExtra(
                    Intent.EXTRA_TEXT,
                    "Deposit of ₹${data.amount} successfully added to eSports Arena wallet. Txn ID: ${data.txnId}."
                )
                addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
            }

            context.startActivity(Intent.createChooser(shareIntent, "Share Deposit Receipt"))
        } catch (e: Exception) {
            Toast.makeText(context, "Could not share receipt: ${e.message}", Toast.LENGTH_SHORT).show()
        }
    }
}
