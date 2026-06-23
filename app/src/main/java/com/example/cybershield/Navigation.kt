package com.example.cybershield

import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawingPadding
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.navigation3.runtime.entryProvider
import androidx.navigation3.runtime.rememberNavBackStack
import androidx.navigation3.ui.NavDisplay
import com.example.cybershield.ui.main.MainScreen
import com.example.cybershield.ui.auth.WelcomeScreen
import com.example.cybershield.ui.auth.LoginScreen
import com.example.cybershield.ui.auth.RegisterScreen
import com.example.cybershield.ui.auth.ForgotPasswordScreen
import com.example.cybershield.ui.auth.IntroScreen
import com.example.cybershield.ui.auth.SocialSelectScreen
import com.example.cybershield.ui.auth.PermissionSetupScreen
import com.example.cybershield.ui.auth.UserProfileSetupScreen

@Composable
fun MainNavigation() {
  val backStack = rememberNavBackStack(Main)

  NavDisplay(
    backStack = backStack,
    onBack = { backStack.removeLastOrNull() },
    entryProvider =
      entryProvider {
        entry<Main> {
          MainScreen(onItemClick = { navKey -> backStack.add(navKey) }, modifier = Modifier.safeDrawingPadding().padding(16.dp))
        }
      },
  )
}

@Composable
fun AuthNavigation() {
  val backStack = rememberNavBackStack(Welcome)

  NavDisplay(
    backStack = backStack,
    onBack = { backStack.removeLastOrNull() },
    entryProvider =
      entryProvider {
        entry<Welcome> {
          WelcomeScreen(
            onLoginClick = { backStack.add(Login) },
            onRegisterClick = { backStack.add(Register) }
          )
        }
        entry<Login> {
          LoginScreen(
            onForgotPasswordClick = { backStack.add(ForgotPassword) },
            onRegisterClick = { backStack.add(Register) }
          )
        }
        entry<Register> {
          RegisterScreen(
            onLoginClick = { backStack.add(Login) }
          )
        }
        entry<ForgotPassword> {
          ForgotPasswordScreen(
            onBackToLoginClick = { backStack.removeLastOrNull() }
          )
        }
      },
  )
}

@Composable
fun SetupNavigation(onSetupFinished: () -> Unit) {
  val backStack = rememberNavBackStack(Intro)

  NavDisplay(
    backStack = backStack,
    onBack = { backStack.removeLastOrNull() },
    entryProvider =
      entryProvider {
        entry<Intro> {
          IntroScreen(
            onFinished = { backStack.add(SocialSelect) }
          )
        }
        entry<SocialSelect> {
          SocialSelectScreen(
            onFinished = { backStack.add(PermissionSetup) }
          )
        }
        entry<PermissionSetup> {
          PermissionSetupScreen(
            onFinished = { backStack.add(UserProfileSetup) }
          )
        }
        entry<UserProfileSetup> {
          UserProfileSetupScreen(
            onFinished = onSetupFinished
          )
        }
      },
  )
}

