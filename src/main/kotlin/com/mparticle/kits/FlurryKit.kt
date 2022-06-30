package com.mparticle.kits

import android.content.Context
import android.content.Intent
import android.util.Log
import com.flurry.android.Constants
import com.flurry.android.FlurryAgent
import com.flurry.android.FlurryInstallReceiver
import com.mparticle.MPEvent
import com.mparticle.MParticle
import com.mparticle.MParticle.IdentityType
import com.mparticle.kits.KitIntegration.AttributeListener
import java.util.*

class FlurryKit : KitIntegration(), AttributeListener, KitIntegration.EventListener {
    override fun getName(): String = KIT_NAME

    override fun onKitCreate(
        settings: Map<String, String>,
        context: Context
    ): List<ReportingMessage> {
        var logEnabled = false
        if (MParticle.getInstance()?.environment == MParticle.Environment.Development) {
            logEnabled = true
        }
        FlurryAgent.setReportLocation(
            getSettings().containsKey(INCLUDE_LOCATION) && !getSettings()[INCLUDE_LOCATION].toBoolean()
        )
        getSettings()[API_KEY]?.let {
            FlurryAgent.Builder()
                .withLogEnabled(logEnabled)
                .withLogLevel(Log.VERBOSE)
                .withCaptureUncaughtExceptions(getSettings()[CAPTURE_EXCEPTIONS].toBoolean())
                .build(context, it)
        }
        return emptyList()
    }

    override fun setInstallReferrer(intent: Intent) {
        FlurryInstallReceiver().onReceive(context, intent)
    }

    override fun setUserIdentity(identityType: IdentityType, idIn: String) {
        var id = idIn
        if (identityType == IdentityType.CustomerId) {
            if (!settings.containsKey(HASH_ID) || settings[HASH_ID].toBoolean()) {
                id = KitUtils.hashFnv1a(id.toByteArray()).toString()
            }
            FlurryAgent.setUserId(id)
        }
    }

    override fun removeUserIdentity(identityType: IdentityType) {
        if (identityType == IdentityType.CustomerId) {
            FlurryAgent.setUserId("")
        }
    }

    override fun logout(): List<ReportingMessage> = emptyList()

    override fun setUserAttribute(key: String, value: String) {
        if (key == MParticle.UserAttributes.AGE) {
            try {
                FlurryAgent.setAge(value.toInt())
            } catch (nfe: NumberFormatException) {
            }
        }
        if (key == MParticle.UserAttributes.GENDER) {
            val female = value.lowercase(Locale.getDefault()).equals("female", true)
            FlurryAgent.setGender(if (female) Constants.FEMALE else Constants.MALE)
        }
    }

    override fun setUserAttributeList(s: String, list: List<String>) {}
    override fun supportsAttributeLists(): Boolean = false

    override fun setAllUserAttributes(
        attributes: Map<String, String>,
        attributeLists: Map<String, List<String>>
    ) {
        for ((key, value) in attributes) {
            setUserAttribute(key, value)
        }
    }

    override fun removeUserAttribute(key: String) {
        //there's no way to un-set age/gender
    }

    override fun leaveBreadcrumb(breadcrumb: String): List<ReportingMessage> = emptyList()

    override fun logError(
        message: String,
        errorAttributes: Map<String, String>
    ): List<ReportingMessage> = emptyList()

    override fun logException(
        exception: Exception,
        exceptionAttributes: Map<String, String>,
        message: String
    ): List<ReportingMessage> = emptyList()

    override fun logEvent(event: MPEvent): List<ReportingMessage> {
        if (event.customAttributes == null) {
            FlurryAgent.logEvent(event.eventName)
        } else {
            event.customAttributeStrings?.let { FlurryAgent.logEvent(event.eventName, it) }
        }
        val messageList = LinkedList<ReportingMessage>()
        messageList.add(ReportingMessage.fromEvent(this, event))
        return messageList
    }

    override fun logScreen(
        screenName: String,
        eventAttributes: Map<String, String>
    ): List<ReportingMessage> {
        FlurryAgent.logEvent(screenName, eventAttributes)
        val messageList = LinkedList<ReportingMessage>()
        messageList.add(
            ReportingMessage(
                this,
                ReportingMessage.MessageType.SCREEN_VIEW,
                System.currentTimeMillis(),
                eventAttributes
            )
                .setScreenName(screenName)
        )
        return messageList
    }

    override fun setOptOut(optOutStatus: Boolean): List<ReportingMessage> = emptyList()

    companion object {
        private const val API_KEY = "apiKey"
        private const val HASH_ID = "hashCustomerId"
        private const val CAPTURE_EXCEPTIONS = "captureExceptions"
        private const val INCLUDE_LOCATION = "includeLocation"
        const val KIT_NAME = "Flurry"
    }
}
