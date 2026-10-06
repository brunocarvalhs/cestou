package br.com.brunocarvalhs.howmuch.core.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
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
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import br.com.brunocarvalhs.howmuch.core.domain.model.UserProfile
import br.com.brunocarvalhs.howmuch.core.theme.CestouTheme
import coil.compose.SubcomposeAsyncImage

private const val AVATAR_DEFAULT_SIZE_DP = 28
private val DEFAULT_AVATAR_SIZE = AVATAR_DEFAULT_SIZE_DP.dp
private const val AVATAR_ICON_SIZE_DIVISOR = 1.75f
private const val AVATAR_OVERLAP_DP = -8
private const val INITIALS_SIZE_RATIO = 0.4f

/**
 * A single member avatar: the photo when there is one, otherwise the initials on a stable color
 * derived from the user id (spec EPA-08), also used when the photo fails to load. A generic person
 * icon is only shown when there is no name at all (e.g. `ShoppingItem`, which does not resolve
 * profiles and passes `null`).
 */
@Composable
fun UserAvatar(
    profile: UserProfile?,
    modifier: Modifier = Modifier,
    size: Dp = DEFAULT_AVATAR_SIZE
) {
    val initials = avatarInitials(profile?.name)
    val (container, content) = avatarColors(profile?.id ?: profile?.name.orEmpty())
    val placeholder: @Composable () -> Unit = {
        AvatarInitials(initials = initials, size = size, container = container, content = content)
    }
    Box(
        modifier = modifier
            .size(size)
            .clip(CircleShape)
            .border(1.dp, MaterialTheme.colorScheme.surface, CircleShape),
        contentAlignment = Alignment.Center
    ) {
        val photoUrl = profile?.photoUrl
        if (photoUrl.isNullOrBlank()) {
            placeholder()
        } else {
            SubcomposeAsyncImage(
                model = photoUrl,
                contentDescription = null,
                contentScale = ContentScale.Crop,
                modifier = Modifier.fillMaxSize(),
                loading = { placeholder() },
                error = { placeholder() }
            )
        }
    }
}

@Composable
private fun AvatarInitials(initials: String?, size: Dp, container: Color, content: Color) {
    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(container),
        contentAlignment = Alignment.Center
    ) {
        if (initials != null) {
            // Sized from the circle, not the user font scale, so it never overflows the avatar.
            val fontSize = with(LocalDensity.current) { (size * INITIALS_SIZE_RATIO).toSp() }
            Text(
                text = initials,
                fontSize = fontSize,
                maxLines = 1,
                fontWeight = FontWeight.SemiBold,
                color = content
            )
        } else {
            Icon(
                imageVector = Icons.Default.Person,
                contentDescription = null,
                modifier = Modifier.size(size / AVATAR_ICON_SIZE_DIVISOR),
                tint = content
            )
        }
    }
}

/** Material container/on-container pairs, which meet text contrast in light and dark themes. */
@Composable
private fun avatarColors(seed: String): Pair<Color, Color> {
    val scheme = MaterialTheme.colorScheme
    val palette = listOf(
        scheme.primaryContainer to scheme.onPrimaryContainer,
        scheme.secondaryContainer to scheme.onSecondaryContainer,
        scheme.tertiaryContainer to scheme.onTertiaryContainer,
    )
    return palette[avatarColorSlot(seed, palette.size)]
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
