/*
 * SPDX-FileCopyrightText: 2021 Maxim Leshchenko <cnmaks90@gmail.com>
 *
 * SPDX-License-Identifier: GPL-2.0-only OR GPL-3.0-only OR LicenseRef-KDE-Accepted-GPL
 */

package org.kde.kdeconnect.ui.about

import androidx.annotation.DrawableRes
import androidx.annotation.StringRes

data class AboutData(@StringRes val name: Int,
                     @DrawableRes val icon: Int,
                     val versionName: String,
                     val bugURL: String? = null,
                     val releasesURL: String? = null,
                     val sourceCodeURL: String? = null,
                     val kdeConnectSourceURL: String,
                     val kdeDonateURL: String? = null,
                     @StringRes val kdeConnectAuthorsFooterText: Int? = null,
                     val mainContributor: AboutPerson,
                     val kdeConnectAuthors: List<AboutPerson> = mutableListOf()
)
