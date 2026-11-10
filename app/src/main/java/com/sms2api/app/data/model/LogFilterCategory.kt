package com.sms2api.app.data.model

enum class LogFilterCategory(val label: String) {
    ALL("All"),
    MATCHED("Filter Matching"),
    SKIPPED("Filter Not Matching"),
    SUCCESS("Success"),
    PENDING("Pending"),
    FAILED("Failed")
}
