package com.mymetroui.launcher.ui

import com.mymetroui.launcher.data.notif.NotificationRepository
import com.mymetroui.launcher.ui.actioncenter.ActionCenterController
import com.mymetroui.launcher.ui.actioncenter.FakeBrightness
import com.mymetroui.launcher.ui.actioncenter.FakeTorch
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.UnconfinedTestDispatcher

@OptIn(ExperimentalCoroutinesApi::class)
fun testController(repo: NotificationRepository = NotificationRepository()) = ActionCenterController(
    repo, FakeTorch(), FakeBrightness(), { false }, CoroutineScope(UnconfinedTestDispatcher()),
)
