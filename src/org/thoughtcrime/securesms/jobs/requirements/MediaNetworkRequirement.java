package org.thoughtcrime.securesms.jobs.requirements;

import android.content.Context;
import android.net.ConnectivityManager;
import android.net.NetworkInfo;
import android.support.annotation.NonNull;
import android.util.Log;

import org.thoughtcrime.securesms.attachments.Attachment;
import org.thoughtcrime.securesms.attachments.AttachmentId;
import org.thoughtcrime.securesms.database.DatabaseFactory;
import org.thoughtcrime.securesms.database.AttachmentDatabase;
import org.thoughtcrime.securesms.util.MediaUtil;
import org.thoughtcrime.securesms.util.ServiceUtil;
import org.thoughtcrime.securesms.util.TextSecurePreferences;
import org.whispersystems.jobqueue.dependencies.ContextDependent;
import org.whispersystems.jobqueue.requirements.Requirement;

import java.util.Collections;
import java.util.Set;

public class MediaNetworkRequirement implements Requirement, ContextDependent {
  private static final long   serialVersionUID = 0L;
  private static final String TAG              = MediaNetworkRequirement.class.getSimpleName();

  private transient Context context;

  private final long messageId;
  private final long partRowId;
  private final long partUniqueId;

  public MediaNetworkRequirement(Context context, long messageId, AttachmentId attachmentId) {
    this.context      = context;
    this.messageId    = messageId;
    this.partRowId    = attachmentId.getRowId();
    this.partUniqueId = attachmentId.getUniqueId();
  }

  @Override
  public void setContext(Context context) {
    this.context = context;
  }

  private static NetworkInfo getNetworkInfo(Context context) {
    return ServiceUtil.getConnectivityManager(context).getActiveNetworkInfo();
  }

  private static boolean isConnectedWifi(Context context) {
    final NetworkInfo info = getNetworkInfo(context);
    return info != null && info.isConnected() && info.getType() == ConnectivityManager.TYPE_WIFI;
  }

  private static boolean isConnectedMobile(Context context) {
    final NetworkInfo info = getNetworkInfo(context);
    return info != null && info.isConnected() && info.getType() == ConnectivityManager.TYPE_MOBILE;
  }

  private static boolean isConnectedRoaming(Context context) {
    final NetworkInfo info = getNetworkInfo(context);
    return info != null && info.isConnected() && info.isRoaming() && info.getType() == ConnectivityManager.TYPE_MOBILE;
  }

  private @NonNull Set<String> getAllowedAutoDownloadTypes() {
    if (isConnectedWifi(context)) {
      return TextSecurePreferences.getWifiMediaDownloadAllowed(context);
    } else if (isConnectedRoaming(context)) {
      return TextSecurePreferences.getRoamingMediaDownloadAllowed(context);
    } else if (isConnectedMobile(context)) {
      return TextSecurePreferences.getMobileMediaDownloadAllowed(context);
    } else {
      return Collections.emptySet();
    }
  }

  public static @NonNull boolean isAllowedToShowAnimatedGiphyPreview(Context context) {
    Set<String> allowedNetworks = TextSecurePreferences.getGiphyMediaDownloadAllowed(context);
    if (isConnectedWifi(context)) {
      return allowedNetworks.contains("wifi");
    } else if (isConnectedRoaming(context)) {
      return allowedNetworks.contains("roaming");
    } else if (isConnectedMobile(context)) {
      return allowedNetworks.contains("mobile");
    } else {
      return false;
    }
  }

  @Override
  public boolean isPresent() {
    final AttachmentId       attachmentId = new AttachmentId(partRowId, partUniqueId);
    final AttachmentDatabase db           = DatabaseFactory.getAttachmentDatabase(context);
    final Attachment         attachment   = db.getAttachment(attachmentId);

    if (attachment == null) {
      Log.w(TAG, "attachment was null, returning vacuous true");
      return true;
    }

    Log.w(TAG, "part transfer progress is " + attachment.getTransferState());
    switch (attachment.getTransferState()) {
    case AttachmentDatabase.TRANSFER_PROGRESS_STARTED:
      return true;
    case AttachmentDatabase.TRANSFER_PROGRESS_AUTO_PENDING:
      final Set<String> allowedTypes = getAllowedAutoDownloadTypes();
      final boolean     isAllowed    = allowedTypes.contains(MediaUtil.getDiscreteMimeType(attachment.getContentType()));

      /// XXX WTF -- This is *hella* gross. A requirement shouldn't have the side effect of
      // *modifying the database* just by calling isPresent().
      if (isAllowed) db.setTransferState(messageId, attachmentId, AttachmentDatabase.TRANSFER_PROGRESS_STARTED);
      return isAllowed;
    default:
      return false;
    }
  }
}
