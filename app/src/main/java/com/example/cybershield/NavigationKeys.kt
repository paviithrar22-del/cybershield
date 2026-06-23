package com.example.cybershield

import androidx.navigation3.runtime.NavKey
import kotlinx.serialization.Serializable

@Serializable data object Welcome : NavKey
@Serializable data object Login : NavKey
@Serializable data object Register : NavKey
@Serializable data object ForgotPassword : NavKey
@Serializable data object Intro : NavKey
@Serializable data object SocialSelect : NavKey
@Serializable data object PermissionSetup : NavKey
@Serializable data object UserProfileSetup : NavKey
@Serializable data object Main : NavKey

