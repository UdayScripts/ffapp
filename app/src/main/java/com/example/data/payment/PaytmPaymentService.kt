package com.example.data.payment

import android.content.Context
import android.content.Intent
import android.net.Uri
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody
import org.json.JSONObject
import java.net.URLEncoder
import java.util.concurrent.TimeUnit

data class PaytmTxnStatus(
    val status: String,
    val isSuccess: Boolean,
    val isPending: Boolean,
    val respCode: String,
    val respMsg: String,
    val txnId: String?,
    val bankTxnId: String?,
    val orderId: String,
    val txnAmount: Double?,
    val rawJson: String
)

object PaytmPaymentService {
    private const val DEFAULT_MID = "SzFThC49898719386494"
    const val DEFAULT_UPI_VPA = "paytmqr1aictmo962@paytm"
    const val STATUS_URL = "https://securegw.paytm.in/merchant-status/getTxnStatus"

    private val httpClient by lazy {
        OkHttpClient.Builder()
            .connectTimeout(15, TimeUnit.SECONDS)
            .readTimeout(15, TimeUnit.SECONDS)
            .build()
    }

    /**
     * Generates a unique, alphanumeric order ID under 30 characters.
     */
    fun generateOrderId(): String {
        val timestamp = System.currentTimeMillis()
        val randomSuffix = (1000..9999).random()
        return "FF_${timestamp}_$randomSuffix"
    }

    /**
     * Builds standard UPI payment URI for deeplinking into any UPI app.
     */
    fun buildUpiDeeplink(
        orderId: String,
        amount: Int,
        vpa: String = DEFAULT_UPI_VPA,
        payeeName: String = "eSports Arena"
    ): String {
        val encodedPn = URLEncoder.encode(payeeName, "UTF-8")
        val encodedNote = URLEncoder.encode("Deposit $orderId", "UTF-8")
        return "upi://pay?pa=$vpa&pn=$encodedPn&mc=5499&tr=$orderId&am=$amount&cu=INR&tn=$encodedNote"
    }

    /**
     * Builds URL for generating clean QR code bitmap from the UPI Deeplink.
     */
    fun buildQrCodeUrl(upiDeeplink: String): String {
        val encoded = URLEncoder.encode(upiDeeplink, "UTF-8")
        return "https://api.qrserver.com/v1/create-qr-code/?size=400x400&data=$encoded"
    }

    /**
     * Attempts to launch an installed UPI app (PhonePe, Google Pay, Paytm, BHIM, etc.)
     */
    fun launchUpiPaymentIntent(context: Context, upiDeeplink: String): Boolean {
        return try {
            val intent = Intent(Intent.ACTION_VIEW).apply {
                data = Uri.parse(upiDeeplink)
                addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            }
            val chooser = Intent.createChooser(intent, "Pay via UPI App")
            chooser.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            context.startActivity(chooser)
            true
        } catch (e: Exception) {
            false
        }
    }

    /**
     * Queries Paytm Transaction Status API:
     * Endpoint: https://securegw.paytm.in/merchant-status/getTxnStatus
     * POST body: JsonData={"MID":"...","ORDERID":"..."}
     */
    suspend fun checkTransactionStatus(
        orderId: String,
        mid: String = DEFAULT_MID
    ): Result<PaytmTxnStatus> = withContext(Dispatchers.IO) {
        try {
            val jsonPayload = JSONObject().apply {
                put("MID", mid)
                put("ORDERID", orderId)
            }.toString()

            val encodedPayload = URLEncoder.encode(jsonPayload, "UTF-8")
            val postData = "JsonData=$encodedPayload"

            val requestBody = postData.toRequestBody("application/x-www-form-urlencoded; charset=UTF-8".toMediaType())
            val request = Request.Builder()
                .url(STATUS_URL)
                .post(requestBody)
                .addHeader("Content-Type", "application/x-www-form-urlencoded; charset=UTF-8")
                .build()

            val response = httpClient.newCall(request).execute()
            val raw = response.body?.string() ?: ""

            if (!response.isSuccessful) {
                return@withContext Result.failure(Exception("HTTP error ${response.code}: $raw"))
            }

            val json = JSONObject(raw)
            val respBody = json.optJSONObject("body") ?: json
            val resultInfo = respBody.optJSONObject("resultInfo")

            val status = respBody.optString(
                "STATUS",
                json.optString("STATUS", resultInfo?.optString("resultStatus", "UNKNOWN") ?: "UNKNOWN")
            )
            val respCode = respBody.optString(
                "RESPCODE",
                json.optString("RESPCODE", resultInfo?.optString("resultCode", "") ?: "")
            )
            val respMsg = respBody.optString(
                "RESPMSG",
                json.optString("RESPMSG", resultInfo?.optString("resultMsg", "No status message") ?: "No status message")
            )
            val txnId = when {
                respBody.has("TXNID") -> respBody.optString("TXNID")
                json.has("TXNID") -> json.optString("TXNID")
                respBody.has("txnId") -> respBody.optString("txnId")
                else -> null
            }
            val bankTxnId = when {
                respBody.has("BANKTXNID") -> respBody.optString("BANKTXNID")
                json.has("BANKTXNID") -> json.optString("BANKTXNID")
                respBody.has("bankTxnId") -> respBody.optString("bankTxnId")
                else -> null
            }
            val retOrderId = respBody.optString("ORDERID", json.optString("ORDERID", respBody.optString("orderId", orderId)))
            val amountStr = when {
                respBody.has("TXNAMOUNT") -> respBody.optString("TXNAMOUNT")
                json.has("TXNAMOUNT") -> json.optString("TXNAMOUNT")
                respBody.has("txnAmount") -> respBody.optString("txnAmount")
                else -> null
            }
            val txnAmount = amountStr?.toDoubleOrNull()

            val isSuccess = status.equals("TXN_SUCCESS", ignoreCase = true) ||
                respCode == "01" ||
                status.equals("S", ignoreCase = true)
            val isPending = status.equals("PENDING", ignoreCase = true) ||
                respCode == "295" ||
                respCode == "334" ||
                status.equals("U", ignoreCase = true)

            Result.success(
                PaytmTxnStatus(
                    status = status,
                    isSuccess = isSuccess,
                    isPending = isPending,
                    respCode = respCode,
                    respMsg = respMsg,
                    txnId = txnId,
                    bankTxnId = bankTxnId,
                    orderId = retOrderId,
                    txnAmount = txnAmount,
                    rawJson = raw
                )
            )
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
}
