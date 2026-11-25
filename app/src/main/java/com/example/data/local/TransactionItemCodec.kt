package com.example.data.local

import com.example.data.model.TransactionItem
import org.json.JSONArray
import org.json.JSONObject

object TransactionItemCodec {
  fun encode(items: List<TransactionItem>): String {
    val array = JSONArray()
    items.forEach { item ->
      val obj = JSONObject()
      obj.put("productId", item.productId)
      obj.put("productName", item.productName)
      obj.put("unitName", item.unitName)
      obj.put("quantity", item.quantity)
      obj.put("baseUnitsDeducted", item.baseUnitsDeducted)
      obj.put("unitPrice", item.unitPrice)
      obj.put("unitCost", item.unitCost)
      obj.put("subtotal", item.subtotal)
      obj.put("isRetailUnit", item.isRetailUnit)
      obj.put("standardUnitPrice", item.standardUnitPrice)
      array.put(obj)
    }
    return array.toString()
  }

  fun decode(json: String): List<TransactionItem> {
    if (json.isBlank()) return emptyList()
    return runCatching {
        val array = JSONArray(json)
        buildList {
          for (i in 0 until array.length()) {
            val obj = array.getJSONObject(i)
            val parsedUnitPrice = obj.optLong("unitPrice", 0L)
            add(
              TransactionItem(
                productId = obj.optLong("productId", 0L),
                productName = obj.optString("productName", ""),
                unitName = obj.optString("unitName", ""),
                quantity = obj.optDouble("quantity", 0.0),
                baseUnitsDeducted = obj.optDouble("baseUnitsDeducted", 0.0),
                unitPrice = parsedUnitPrice,
                unitCost = obj.optLong("unitCost", 0L),
                subtotal = obj.optLong("subtotal", 0L),
                isRetailUnit = obj.optBoolean("isRetailUnit", false),
                standardUnitPrice = obj.optLong("standardUnitPrice", parsedUnitPrice),
              )
            )
          }
        }
      }
      .getOrDefault(emptyList())
  }
}

object KasbonPaymentCodec {
  fun encode(records: List<com.example.data.model.KasbonPaymentRecord>): String {
    if (records.isEmpty()) return ""
    val array = JSONArray()
    records.forEach { rec ->
      val obj = JSONObject()
      obj.put("amount", rec.amount)
      obj.put("paymentMethod", rec.paymentMethod)
      obj.put("timestamp", rec.timestamp)
      obj.put("recordedBy", rec.recordedBy)
      array.put(obj)
    }
    return array.toString()
  }

  fun decode(json: String): List<com.example.data.model.KasbonPaymentRecord> {
    if (json.isBlank()) return emptyList()
    return runCatching {
        val array = JSONArray(json)
        buildList {
          for (i in 0 until array.length()) {
            val obj = array.getJSONObject(i)
            val amt = obj.optLong("amount", 0L)
            if (amt > 0L) {
              add(
                com.example.data.model.KasbonPaymentRecord(
                  amount = amt,
                  paymentMethod = obj.optString("paymentMethod", "Tunai"),
                  timestamp = obj.optLong("timestamp", 0L),
                  recordedBy = obj.optString("recordedBy", ""),
                )
              )
            }
          }
        }
      }
      .getOrDefault(emptyList())
  }
}

