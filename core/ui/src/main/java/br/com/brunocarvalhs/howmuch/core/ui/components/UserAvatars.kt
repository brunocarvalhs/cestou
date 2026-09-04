package br.com.brunocarvalhs.howmuch.core.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Person
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import br.com.brunocarvalhs.howmuch.core.domain.model.UserProfile
import br.com.brunocarvalhs.howmuch.core.theme.CestouTheme

private const val AVATAR_DEFAULT_SIZE_DP = 28
private val DEFAULT_AVATAR_SIZE = AVATAR_DEFAULT_SIZE_DP.dp
private const val AVATAR_ICON_SIZE_DIVISOR = 1.75f
private const val AVATAR_OVERLAP_DP = -8

/**
 * A single member avatar. Renders initials derived from [profile]'s name when one is available;
 * falls back to a generic person icon when [profile] is null or has no usable name, so the avatar
 * space is never left blank (spec IAA-01 AC7).
 */
@Composable
fun UserAvatar(
    profile: UserProfile?,
    modifier: Modifier = Modifier,
    size: Dp = DEFAULT_AVATAR_SIZE
) {
    val initials = profile?.name.toInitialsOrNull()
    Box(
        modifier = modifier
            .size(size)
            .clip(CircleShape)
            .background(MaterialTheme.colorScheme.surfaceVariant)
            .border(1.dp, MaterialTheme.colorScheme.surface, CircleShape),
        contentAlignment = Alignment.Center
    ) {
        if (initials != null) {
            Text(
                text = initials,
                style = MaterialTheme.typography.labelSmall,
                fontWeight = FontWeight.SemiBold,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        } else {
            Icon(
                imageVector = Icons.Default.Person,
                contentDescription = null,
                modifier = Modifier.size(size / AVATAR_ICON_SIZE_DIVISOR),
                tint = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}

/**
 * Overlapping row of up to [maxVisible] member avatars — visual shell originally introduced in
 * `feature/shopping`'s `ShoppingItem`, extracted here so `feature/cart` can reuse it too. Each
 * entry renders real initials when a resolved [UserProfile] is supplied, or the generic-icon
 * fallback (a `null` element) when it isn't — e.g. `ShoppingItem` doesn't resolve profiles today,
 * it only knows member ids, so it passes a list of `null`s to preserve its existing look.
 */
@Composable
fun UserAvatars(
    profiles: List<UserProfile?>,
    modifier: Modifier = Modifier,
    maxVisible: Int = 2
) {
    Row(modifier = modifier, horizontalArrangement = Arrangement.spacedBy(AVATAR_OVERLAP_DP.dp)) {
        profiles.take(maxVisible).forEach { profile ->
            UserAvatar(profile = profile)
        }
    }
}

private fun String?.toInitialsOrNull(): String? {
    val words = this?.trim()?.split(Regex("\\s+"))?.filter { it.isNotEmpty() }.orEmpty()
    val initials = when {
        words.size >= 2 -> "${words.first().first()}${words[1].first()}"
        words.size == 1 -> words.first().take(1)
        else -> return null
    }
    return initials.uppercase()
}

@Preview(showBackground = true, name = "Has name")
@Composable
private fun UserAvatarHasNamePreview() {
    CestouTheme {
        UserAvatar(profile = UserProfile(id = "1", name = "Bruno Carvalhos"))
    }
}

@Preview(showBackground = true, name = "No name (fallback icon)")
@Composable
private fun UserAvatarNoNamePreview() {
    CestouTheme {
        UserAvatar(profile = UserProfile(id = "1", name = null))
    }
}

@Preview(showBackground = true, name = "Null profile (fallback icon)")
@Composable
private fun UserAvatarNullProfilePreview() {
    CestouTheme {
        UserAvatar(profile = null)
    }
}

@Preview(showBackground = true, name = "Overlapping pair")
@Composable
private fun UserAvatarsPreview() {
    CestouTheme {
        UserAvatars(
            profiles = listOf(
                UserProfile(id = "1", name = "Bruno Carvalhos"),
                UserProfile(id = "2", name = null)
            )
        )
    }
}
